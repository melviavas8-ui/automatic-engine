package daamky.ua.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.utils.render.hands.GlassHandsRenderer;
import daamky.ua.utils.render.hands.HandsFlameRenderer;
import daamky.ua.utils.render.hands.ShaderHandMaterialRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

@Mixin({GameRenderer.class})
public abstract class GameRendererHandsMixin {
   @Shadow
   @Final
   private FeatureRenderDispatcher featureRenderDispatcher;
   @Shadow
   @Final
   private RenderBuffers renderBuffers;

   public GameRendererHandsMixin() {
   }

   @Inject(
      method = {"renderItemInHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"
      )},
      require = 0
   )
   private void daamky$captureHandsScene(float partialTicks, boolean renderBlockOutline, Matrix4f projectionMatrix, CallbackInfo ci) {
      GlassHandsRenderer.captureBeforeHandRender();
      HandsFlameRenderer.captureBeforeHandRender();
      ShaderHandMaterialRenderer.beginFrame();
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderScreenEffect(ZFLnet/minecraft/client/renderer/SubmitNodeCollector;)V"
      )},
      require = 0
   )
   private void daamky$renderHandsFlameBeforeScreenEffects(DeltaTracker deltaTracker, CallbackInfo ci) {
      if (GlassHandsRenderer.hasCapturedHands() || HandsFlameRenderer.hasCapturedHands()) {
         this.featureRenderDispatcher.renderAllFeatures();
         this.renderBuffers.bufferSource().endBatch();
         GlassHandsRenderer.renderCapturedGlass();
         HandsFlameRenderer.renderCapturedHandsFlame();
      }
   }
}
