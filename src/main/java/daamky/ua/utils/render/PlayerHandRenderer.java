package daamky.ua.utils.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.util.Identifier;
import daamky.ua.mixin.accessor.AvatarRendererAccessor;

public final class PlayerHandRenderer {
   private PlayerHandRenderer() {
   }

   public static void renderRightHand(MatrixStack poseStack, OrderedRenderCommandQueue submitNodeCollector, int light) {
      renderRightHand(MinecraftClient.getInstance().player, poseStack, submitNodeCollector, light);
   }

   public static void renderLeftHand(MatrixStack poseStack, OrderedRenderCommandQueue submitNodeCollector, int light) {
      renderLeftHand(MinecraftClient.getInstance().player, poseStack, submitNodeCollector, light);
   }

   public static void renderRightHand(AbstractClientPlayerEntity player, MatrixStack poseStack, OrderedRenderCommandQueue submitNodeCollector, int light) {
      renderHand(player, poseStack, submitNodeCollector, light, true);
   }

   public static void renderLeftHand(AbstractClientPlayerEntity player, MatrixStack poseStack, OrderedRenderCommandQueue submitNodeCollector, int light) {
      renderHand(player, poseStack, submitNodeCollector, light, false);
   }

   public static void renderHand(
      AbstractClientPlayerEntity player, MatrixStack poseStack, OrderedRenderCommandQueue submitNodeCollector, int light, boolean rightHand
   ) {
      if (player != null && poseStack != null && submitNodeCollector != null) {
         MinecraftClient client = MinecraftClient.getInstance();
         PlayerEntityRenderer<AbstractClientPlayerEntity> renderer = client.getEntityRenderDispatcher().getPlayerRenderer(player);
         Identifier skinTexture = player.getSkin().body().texturePath();
         boolean slim = player.getSkin().model() == PlayerSkinType.SLIM;
         renderHand(renderer, poseStack, submitNodeCollector, light, skinTexture, slim, rightHand);
      }
   }

   public static void renderHand(
      PlayerEntityRenderer<?> renderer,
      MatrixStack poseStack,
      OrderedRenderCommandQueue submitNodeCollector,
      int light,
      Identifier skinTexture,
      boolean slim,
      boolean rightHand
   ) {
      if (renderer != null && poseStack != null && submitNodeCollector != null && skinTexture != null) {
         AvatarRendererAccessor accessor = (AvatarRendererAccessor)renderer;
         if (rightHand) {
            accessor.daamky$renderRightHand(poseStack, submitNodeCollector, light, skinTexture, slim);
         } else {
            accessor.daamky$renderLeftHand(poseStack, submitNodeCollector, light, skinTexture, slim);
         }
      }
   }
}
