package daamky.ua.utils.render.world.targetesp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import daamky.ua.utils.render.WorldVertex;

final class TargetEspQuadRenderer {
   private TargetEspQuadRenderer() {
   }

   static void billboard(MatrixStack stack, Immediate provider, Identifier texture, float y, float scale, float rotation, int c1, int c2, int c3, int c4) {
      VertexConsumer consumer = provider.getBuffer(RenderLayers.entityTranslucent(texture));
      Quaternionf cameraRotation = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
      stack.push();
      stack.translate(0.0F, y, 0.0F);
      stack.multiply(cameraRotation);
      if (rotation != 0.0F) {
         stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
      }

      stack.scale(scale, scale, 1.0F);
      Entry pose = stack.peek();
      WorldVertex.textured(consumer, pose, -1.0F, -1.0F, 0.0F, 0.0F, 0.0F, c1);
      WorldVertex.textured(consumer, pose, -1.0F, 1.0F, 0.0F, 0.0F, 1.0F, c2);
      WorldVertex.textured(consumer, pose, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, c3);
      WorldVertex.textured(consumer, pose, 1.0F, -1.0F, 0.0F, 1.0F, 0.0F, c4);
      stack.pop();
   }

   static void end(Immediate provider, Identifier texture) {
      provider.draw(RenderLayers.entityTranslucent(texture));
   }
}
