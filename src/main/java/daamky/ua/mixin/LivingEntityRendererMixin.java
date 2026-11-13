package daamky.ua.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   @Unique
   private static final int daamky_WHITE_RGB = 16777215;

   public LivingEntityRendererMixin() {
   }

   @Shadow
   public abstract Identifier getTexture(S var1);



   @Inject(
      method = {"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"},
      at = {@At("TAIL")},
      require = 0
   )
   private void updateRenderStateHook(LivingEntity entity, S state, float tickDelta, CallbackInfo ci) {
      SeeInvisibleRenderStateAccessor accessor = (SeeInvisibleRenderStateAccessor)state;
      Chams chams = Chams.getInstance();
      if (chams != null && chams.shouldRender(entity)) {
         accessor.daamky$setChamsColor(chams.getColor(entity));
         state.invisible = false;
         state.invisibleToPlayer = false;
      } else {
         accessor.daamky$setChamsColor(0);
      }

      if (KillEffect.isLegacySoulFake(entity)) {
         state.invisible = false;
         state.invisibleToPlayer = false;
      } else if (SeeInvisible.shouldRenderInvisible() && entity.isInvisible()) {
         state.invisible = true;
         state.invisibleToPlayer = false;
         accessor.daamky$setSeeInvisibleAlpha(SeeInvisible.getAlpha());
      } else {
         accessor.daamky$setSeeInvisibleAlpha(1.0F);
      }

      if (ESP.shouldHideVanillaName(entity)) {
         state.displayName = null;
         if (state instanceof PlayerEntityRenderState avatarState) {
            avatarState.playerName = null;
         }
      }

      state.displayName = NameProtect.protectOwnNameTag(entity, state.displayName);
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && entity == client.player && !(client.currentScreen instanceof HandledScreen)) {
         AngleConnection controller = AngleConnection.INSTANCE;
         if (controller.getFakeAngle() != null) {
            float prevHeadYaw = controller.getPreviousFakeRotation().getYaw();
            float currHeadYaw = controller.getFakeRotation().getYaw();
            float prevPitch = controller.getPreviousFakeRotation().getPitch();
            float currPitch = controller.getFakeRotation().getPitch();
            float prevBodyYaw = controller.getPreviousFakeBodyYaw();
            float currBodyYaw = controller.getFakeBodyYaw();
            float headYaw = MathHelper.lerpAngleDegrees(tickDelta, prevHeadYaw, currHeadYaw);
            float pitch = MathHelper.clamp(MathHelper.lerp(tickDelta, prevPitch, currPitch), -90.0F, 90.0F);
            float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, prevBodyYaw, currBodyYaw);
            float maxHeadRotation = 52.0F;
            float headBodyDiff = MathHelper.wrapDegrees(headYaw - bodyYaw);
            if (Math.abs(headBodyDiff) > maxHeadRotation) {
               bodyYaw = headYaw - (float)MathHelper.sign((double)headBodyDiff) * maxHeadRotation;
            }

            state.bodyYaw = bodyYaw;
            state.relativeHeadYaw = MathHelper.clamp(MathHelper.wrapDegrees(headYaw - bodyYaw), -maxHeadRotation, maxHeadRotation);
            state.pitch = pitch;
         } else {
            if (controller.getCurrentAngle() != null) {
               float headYaw = MathHelper.lerpAngleDegrees(tickDelta, controller.getPreviousRotation().getYaw(), controller.getRotation().getYaw());
               float pitch = MathHelper.lerp(tickDelta, controller.getPreviousRotation().getPitch(), controller.getRotation().getPitch());
               float maxHeadRotation = 52.0F;
               state.relativeHeadYaw = MathHelper.clamp(MathHelper.wrapDegrees(headYaw - state.bodyYaw), -maxHeadRotation, maxHeadRotation);
               state.pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
            }
         }
      }
   }

   @Inject(
      method = {"getRenderLayer"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$useTranslucentRenderType(S state, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderLayer> cir) {
      SeeInvisibleRenderStateAccessor accessor = (SeeInvisibleRenderStateAccessor)state;
      if (!glowing) {
         if (state instanceof PlayerEntityRenderState avatarState && KillEffect.isLegacySoulFake(avatarState.id)) {
            cir.setReturnValue(LegacySoulRenderTypes.entity(this.getTexture(state)));
            return;
         }

         if (accessor.daamky$shouldRenderSeeInvisible() || accessor.daamky$shouldRenderChams()) {
            if (accessor.daamky$shouldRenderChams()) {
               Chams chams = Chams.getInstance();
               if (chams != null && chams.isGlassStyle()) {
                  cir.setReturnValue(LegacySoulRenderTypes.glassEntity(this.getTexture(state)));
               } else {
                  cir.setReturnValue(ChamsRenderTypes.entity(this.getTexture(state), chams == null || chams.isThroughWalls()));
               }
            } else {
               cir.setReturnValue(RenderLayers.itemEntityTranslucentCull(this.getTexture(state)));
            }
         }
      }
   }

   @Inject(
      method = {"getMixColor"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$applySeeInvisibleAlpha(S state, CallbackInfoReturnable<Integer> cir) {
      SeeInvisibleRenderStateAccessor accessor = (SeeInvisibleRenderStateAccessor)state;
      if (state instanceof PlayerEntityRenderState avatarState && KillEffect.isLegacySoulFake(avatarState.id)) {
         float progress = KillEffect.getLegacySoulProgress(avatarState.id);
         int alpha = MathHelper.clamp(Math.round((1.0F - progress) * 255.0F), 0, 255);
         cir.setReturnValue(alpha << 24 | 16777215);
         return;
      }

      if (!accessor.daamky$shouldRenderSeeInvisible()) {
         if (accessor.daamky$shouldRenderChams()) {
            cir.setReturnValue(accessor.daamky$getChamsColor());
         }
      } else {
         int alpha = MathHelper.clamp(Math.round(accessor.daamky$getSeeInvisibleAlpha() * 255.0F), 0, 255);
         cir.setReturnValue(alpha << 24 | 16777215);
      }
   }
}
