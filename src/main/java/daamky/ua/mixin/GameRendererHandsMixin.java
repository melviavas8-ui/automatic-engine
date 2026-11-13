package daamky.ua.mixin;

import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.command.RenderDispatcher;
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

@Mixin({GameRenderer.class})
public abstract class GameRendererHandsMixin {
   @Shadow
   @Final
   private RenderDispatcher entityRenderDispatcher;
   @Shadow
   @Final
   private BufferBuilderStorage buffers;

   public GameRendererHandsMixin() {
   }

   @Inject(
      method = {"renderHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"
      )},
      require = 0
   )
   private void daamky$captureHandsScene(float partialTicks, boolean renderBlockOutline, Matrix4f projectionMatrix, CallbackInfo ci) {
      GlassHandsRenderer.captureBeforeHandRender();
      HandsFlameRenderer.captureBeforeHandRender();
      ShaderHandMaterialRenderer.beginFrame();
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/InGameOverlayRenderer;renderOverlays(ZFLnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V"
      )},
      require = 0
   )
   private void daamky$renderHandsFlameBeforeScreenEffects(RenderTickCounter deltaTracker, CallbackInfo ci) {
      if (GlassHandsRenderer.hasCapturedHands() || HandsFlameRenderer.hasCapturedHands()) {
         this.entityRenderDispatcher.render();
         this.buffers.getEntityVertexConsumers().draw();
         GlassHandsRenderer.renderCapturedGlass();
         HandsFlameRenderer.renderCapturedHandsFlame();
      }
   }
}
