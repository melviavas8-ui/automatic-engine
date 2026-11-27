package daamky.ua.utils.render.world.targetesp;

import org.joml.Quaternionf;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import daamky.ua.utils.render.WorldVertex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

final class TargetEspQuadRenderer {
   private TargetEspQuadRenderer() {
   }

   static void billboard(PoseStack stack, BufferSource provider, Identifier texture, float y, float scale, float rotation, int c1, int c2, int c3, int c4) {
      VertexConsumer consumer = provider.getBuffer(RenderTypes.entityTranslucent(texture));
      Quaternionf cameraRotation = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
      stack.pushPose();
      stack.translate(0.0F, y, 0.0F);
      stack.mulPose(cameraRotation);
      if (rotation != 0.0F) {
         stack.mulPose(Axis.ZP.rotationDegrees(rotation));
      }

      stack.scale(scale, scale, 1.0F);
      Pose pose = stack.last();
      WorldVertex.textured(consumer, pose, -1.0F, -1.0F, 0.0F, 0.0F, 0.0F, c1);
      WorldVertex.textured(consumer, pose, -1.0F, 1.0F, 0.0F, 0.0F, 1.0F, c2);
      WorldVertex.textured(consumer, pose, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, c3);
      WorldVertex.textured(consumer, pose, 1.0F, -1.0F, 0.0F, 1.0F, 0.0F, c4);
      stack.popPose();
   }

   static void end(BufferSource provider, Identifier texture) {
      provider.endBatch(RenderTypes.entityTranslucent(texture));
   }
}
