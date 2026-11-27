package daamky.ua.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import daamky.ua.mixin.accessor.AvatarRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;

public final class PlayerHandRenderer {
   private PlayerHandRenderer() {
   }

   public static void renderRightHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light) {
      renderRightHand(Minecraft.getInstance().player, poseStack, submitNodeCollector, light);
   }

   public static void renderLeftHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light) {
      renderLeftHand(Minecraft.getInstance().player, poseStack, submitNodeCollector, light);
   }

   public static void renderRightHand(AbstractClientPlayer player, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light) {
      renderHand(player, poseStack, submitNodeCollector, light, true);
   }

   public static void renderLeftHand(AbstractClientPlayer player, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light) {
      renderHand(player, poseStack, submitNodeCollector, light, false);
   }

   public static void renderHand(
      AbstractClientPlayer player, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, boolean rightHand
   ) {
      if (player != null && poseStack != null && submitNodeCollector != null) {
         Minecraft client = Minecraft.getInstance();
         AvatarRenderer<AbstractClientPlayer> renderer = client.getEntityRenderDispatcher().getPlayerRenderer(player);
         Identifier skinTexture = player.getSkin().body().texturePath();
         boolean slim = player.getSkin().model() == PlayerModelType.SLIM;
         renderHand(renderer, poseStack, submitNodeCollector, light, skinTexture, slim, rightHand);
      }
   }

   public static void renderHand(
      AvatarRenderer<?> renderer,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
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
