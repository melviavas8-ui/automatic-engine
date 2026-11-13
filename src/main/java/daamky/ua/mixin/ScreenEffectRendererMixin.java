package daamky.ua.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.NoRender;

@Mixin({InGameOverlayRenderer.class})
public abstract class ScreenEffectRendererMixin {
   public ScreenEffectRendererMixin() {
   }

   @Inject(
      method = {"renderFireOverlay"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private static void daamky$renderFire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
      if (NoRender.isActive("Fire")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderInWallOverlay"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private static void daamky$renderBlockOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      if (NoRender.isActive("Block Overlay") || NoRender.isActive("Lava")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"setFloatingItem"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$displayItemActivation(ItemStack floatingItem, Random random, CallbackInfo ci) {
      if (floatingItem != null && floatingItem.isOf(Items.TOTEM_OF_UNDYING) && NoRender.isActive("Totem Animation")) {
         ci.cancel();
      }
   }
}
