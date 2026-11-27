package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.module.impl.visual.ESP;
import daamky.ua.api.module.impl.visual.KillEffect;
import daamky.ua.utils.render.hands.ShaderHandMaterialRenderer;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;

@Mixin({AvatarRenderer.class})
public abstract class AvatarRendererMixin {
   private static final Identifier daamky_LEGACY_SOUL_TEXTURE = Identifier.tryParse("daamky:textures/effects/legacy_soul.png");

   public AvatarRendererMixin() {
   }

   @ModifyArg(
      method = {"renderHand"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
      ),
      index = 2,
      require = 0
   )
   private RenderType daamky$useShaderHandMaterial(RenderType original) {
      return ShaderHandMaterialRenderer.armLayer(original);
   }

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V"},
      at = {@At("TAIL")},
      require = 0
   )
   private void daamky$hideEspAvatarLabels(Avatar entity, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
      if (ESP.shouldHideVanillaName(entity)) {
         ESP.captureServerHealth(entity, state.scoreText);
         state.nameTag = null;
         state.scoreText = null;
      }

      if (KillEffect.isLegacySoulFake(entity)) {
         state.showHat = false;
         state.showJacket = false;
         state.showLeftPants = false;
         state.showRightPants = false;
         state.showLeftSleeve = false;
         state.showRightSleeve = false;
         state.showCape = false;
         state.showExtraEars = false;
         state.headEquipment = ItemStack.EMPTY;
         state.chestEquipment = ItemStack.EMPTY;
         state.legsEquipment = ItemStack.EMPTY;
         state.feetEquipment = ItemStack.EMPTY;
         state.rightHandItemStack = ItemStack.EMPTY;
         state.leftHandItemStack = ItemStack.EMPTY;
         state.rightArmPose = ArmPose.EMPTY;
         state.leftArmPose = ArmPose.EMPTY;
         state.isUsingItem = false;
      }
   }

   @Inject(
      method = {"shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Z"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$disableSoulLayers(AvatarRenderState state, CallbackInfoReturnable<Boolean> cir) {
      if (KillEffect.isLegacySoulFake(state.id)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$overrideSoulTexture(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
      if (daamky_LEGACY_SOUL_TEXTURE != null && KillEffect.isLegacySoulFake(state.id)) {
         cir.setReturnValue(daamky_LEGACY_SOUL_TEXTURE);
      }
   }
}
