package daamky.ua.utils.render;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class WorldVertex {
   private static final int FULL_BRIGHT = 15728880;

   private WorldVertex() {
   }

   public static void textured(VertexConsumer consumer, Pose pose, float x, float y, float z, float u, float v, int color) {
      consumer.addVertex(pose, x, y, z).setUv(u, v).setColor(color).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0F, 0.0F, 1.0F);
   }
}
