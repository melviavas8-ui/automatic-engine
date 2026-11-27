package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.access.SeeInvisibleRenderStateAccessor;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.player.NameProtect;
import daamky.ua.api.module.impl.visual.Chams;
import daamky.ua.api.module.impl.visual.ESP;
import daamky.ua.api.module.impl.visual.KillEffect;
import daamky.ua.api.module.impl.visual.SeeInvisible;
import daamky.ua.api.module.impl.visual.frageffect.LegacyGlassRenderer;
import daamky.ua.utils.render.world.blockoverlay.ChamsRenderTypes;
import daamky.ua.utils.render.world.legacy.LegacySoulRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   @Unique
   private static final int daamky_WHITE_RGB = 16777215;

   public LivingEntityRendererMixin() {
   }

   @Shadow
   public abstract Identifier getTextureLocation(S var1);



   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"},
      at = {@At("TAIL")},
      require = 0
   )
   private void updateRenderStateHook(LivingEntity entity, S state, float tickDelta, CallbackInfo ci) {
      SeeInvisibleRenderStateAccessor accessor = (SeeInvisibleRenderStateAccessor)state;
      Chams chams = Chams.getInstance();
      if (chams != null && chams.shouldRender(entity)) {
         accessor.daamky$setChamsColor(chams.getColor(entity));
         state.isInvisible = false;
         state.isInvisibleToPlayer = false;
      } else {
         accessor.daamky$setChamsColor(0);
      }

      if (KillEffect.isLegacySoulFake(entity)) {
         state.isInvisible = false;
         state.isInvisibleToPlayer = false;
      } else if (SeeInvisible.shouldRenderInvisible() && entity.isInvisible()) {
         state.isInvisible = false;
         state.isInvisibleToPlayer = false;
         accessor.daamky$setSeeInvisibleAlpha(SeeInvisible.getAlpha());
      } else {
         accessor.daamky$setSeeInvisibleAlpha(1.0F);
      }

      if (ESP.shouldHideVanillaName(entity)) {
         state.nameTag = null;
         if (state instanceof AvatarRenderState avatarState) {
            avatarState.scoreText = null;
         }
      }

      state.nameTag = NameProtect.protectOwnNameTag(entity, state.nameTag);
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && entity == client.player && !(client.screen instanceof AbstractContainerScreen)) {
         AngleConnection controller = AngleConnection.INSTANCE;
         if (controller.getFakeAngle() != null) {
            float prevHeadYaw = controller.getPreviousFakeRotation().getYaw();
            float currHeadYaw = controller.getFakeRotation().getYaw();
            float prevPitch = controller.getPreviousFakeRotation().getPitch();
            float currPitch = controller.getFakeRotation().getPitch();
            float prevBodyYaw = controller.getPreviousFakeBodyYaw();
            float currBodyYaw = controller.getFakeBodyYaw();
            float headYaw = Mth.rotLerp(tickDelta, prevHeadYaw, currHeadYaw);
            float pitch = Mth.clamp(Mth.lerp(tickDelta, prevPitch, currPitch), -90.0F, 90.0F);
            float bodyYaw = Mth.rotLerp(tickDelta, prevBodyYaw, currBodyYaw);
            float maxHeadRotation = 52.0F;
            float headBodyDiff = Mth.wrapDegrees(headYaw - bodyYaw);
            if (Math.abs(headBodyDiff) > maxHeadRotation) {
               bodyYaw = headYaw - (float)Mth.sign((double)headBodyDiff) * maxHeadRotation;
            }

            state.bodyRot = bodyYaw;
            state.yRot = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -maxHeadRotation, maxHeadRotation);
            state.xRot = pitch;
         } else {
            if (controller.getCurrentAngle() != null) {
               float headYaw = Mth.rotLerp(tickDelta, controller.getPreviousRotation().getYaw(), controller.getRotation().getYaw());
               float pitch = Mth.lerp(tickDelta, controller.getPreviousRotation().getPitch(), controller.getRotation().getPitch());
               float maxHeadRotation = 52.0F;
               state.yRot = Mth.clamp(Mth.wrapDegrees(headYaw - state.bodyRot), -maxHeadRotation, maxHeadRotation);
               state.xRot = Mth.clamp(pitch, -90.0F, 90.0F);
            }
         }
      }
   }

   @Inject(
      method = {"getRenderType"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$useTranslucentRenderType(S state, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
      SeeInvisibleRenderStateAccessor accessor = (SeeInvisibleRenderStateAccessor)state;
      if (!glowing) {
         if (state instanceof AvatarRenderState avatarState && KillEffect.isLegacySoulFake(avatarState.id)) {
            cir.setReturnValue(LegacySoulRenderTypes.entity(this.getTextureLocation(state)));
            return;
         }

         if (accessor.daamky$shouldRenderSeeInvisible() || accessor.daamky$shouldRenderChams()) {
            if (accessor.daamky$shouldRenderChams()) {
               Chams chams = Chams.getInstance();
               if (chams != null && chams.isGlassStyle()) {
                  cir.setReturnValue(LegacySoulRenderTypes.glassEntity(this.getTextureLocation(state)));
               } else {
                  cir.setReturnValue(ChamsRenderTypes.entity(this.getTextureLocation(state), chams == null || chams.isThroughWalls()));
               }
            } else {
               cir.setReturnValue(RenderTypes.itemEntityTranslucentCull(this.getTextureLocation(state)));
            }
         }
      }
   }

   @Inject(
      method = {"getModelTint"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$applySeeInvisibleAlpha(S state, CallbackInfoReturnable<Integer> cir) {
      SeeInvisibleRenderStateAccessor accessor = (SeeInvisibleRenderStateAccessor)state;
      if (state instanceof AvatarRenderState avatarState && KillEffect.isLegacySoulFake(avatarState.id)) {
         float progress = KillEffect.getLegacySoulProgress(avatarState.id);
         int alpha = Mth.clamp(Math.round((1.0F - progress) * 255.0F), 0, 255);
         cir.setReturnValue(alpha << 24 | 16777215);
         return;
      }

      if (!accessor.daamky$shouldRenderSeeInvisible()) {
         if (accessor.daamky$shouldRenderChams()) {
            cir.setReturnValue(accessor.daamky$getChamsColor());
         }
      } else {
         int alpha = Mth.clamp(Math.round(accessor.daamky$getSeeInvisibleAlpha() * 255.0F), 0, 255);
         cir.setReturnValue(alpha << 24 | 16777215);
      }
   }
}
