package daamky.ua.api.module.impl.visual.particles;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.WorldVertex;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

final class ParticleRenderer {
   private final Set<RenderLayer> usedRenderTypes = new HashSet<>();

   ParticleRenderer() {
   }

   void clear() {
      this.usedRenderTypes.clear();
   }

   void flush(Immediate provider) {
      for (RenderLayer type : this.usedRenderTypes) {
         provider.draw(type);
      }

      this.usedRenderTypes.clear();
   }

   void drawTexture(
      MatrixStack stack,
      Immediate provider,
      Identifier texture,
      Vec3d pos,
      Vec3d cameraPos,
      Quaternionf cameraRotation,
      float size,
      float rotation,
      int color,
      boolean bloom
   ) {
      RenderLayer renderType = bloom ? ClientPipelines.WORLD_PARTICLES_GLOW.apply(texture) : RenderLayers.entityTranslucent(texture);
      this.renderBillboard(stack, provider, renderType, pos, cameraPos, cameraRotation, size, rotation, color);
   }

   void drawBox(Vec3d renderPos, float scale, int color) {
      float boxScale = scale / 4.5F;
      Box box = new Box(
         renderPos.x - (double)boxScale,
         renderPos.y - (double)boxScale,
         renderPos.z - (double)boxScale,
         renderPos.x + (double)boxScale,
         renderPos.y + (double)boxScale,
         renderPos.z + (double)boxScale
      );
      Render3D.drawBox(box, color, 0.1F, true, true, true);
   }

   private void renderBillboard(
      MatrixStack stack,
      Immediate provider,
      RenderLayer renderType,
      Vec3d pos,
      Vec3d cameraPos,
      Quaternionf cameraRotation,
      float size,
      float rotation,
      int color
   ) {
      if (!(size <= 0.001F) && ColorUtil.getAlpha(color) > 0) {
         this.usedRenderTypes.add(renderType);
         VertexConsumer consumer = provider.getBuffer(renderType);
         stack.push();
         stack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
         stack.multiply(cameraRotation);
         stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
         Entry pose = stack.peek();
         float half = size / 2.0F;
         WorldVertex.textured(consumer, pose, -half, -half, 0.0F, 0.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, half, -half, 0.0F, 1.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, half, half, 0.0F, 1.0F, 1.0F, color);
         WorldVertex.textured(consumer, pose, -half, half, 0.0F, 0.0F, 1.0F, color);
         stack.pop();
      }
   }
}
