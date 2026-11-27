package daamky.ua.api.module.impl.visual.particles;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.WorldVertex;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

final class ParticleRenderer {
   private final Set<RenderType> usedRenderTypes = new HashSet<>();

   ParticleRenderer() {
   }

   void clear() {
      this.usedRenderTypes.clear();
   }

   void flush(BufferSource provider) {
      for (RenderType type : this.usedRenderTypes) {
         provider.endBatch(type);
      }

      this.usedRenderTypes.clear();
   }

   void drawTexture(
      PoseStack stack,
      BufferSource provider,
      Identifier texture,
      Vec3 pos,
      Vec3 cameraPos,
      Quaternionf cameraRotation,
      float size,
      float rotation,
      int color,
      boolean bloom
   ) {
      RenderType renderType = bloom ? ClientPipelines.WORLD_PARTICLES_GLOW.apply(texture) : RenderTypes.entityTranslucent(texture);
      this.renderBillboard(stack, provider, renderType, pos, cameraPos, cameraRotation, size, rotation, color);
   }

   void drawBox(Vec3 renderPos, float scale, int color) {
      float boxScale = scale / 4.5F;
      AABB box = new AABB(
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
      PoseStack stack,
      BufferSource provider,
      RenderType renderType,
      Vec3 pos,
      Vec3 cameraPos,
      Quaternionf cameraRotation,
      float size,
      float rotation,
      int color
   ) {
      if (!(size <= 0.001F) && ColorUtil.getAlpha(color) > 0) {
         this.usedRenderTypes.add(renderType);
         VertexConsumer consumer = provider.getBuffer(renderType);
         stack.pushPose();
         stack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
         stack.mulPose(cameraRotation);
         stack.mulPose(Axis.ZP.rotationDegrees(rotation));
         Pose pose = stack.last();
         float half = size / 2.0F;
         WorldVertex.textured(consumer, pose, -half, -half, 0.0F, 0.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, half, -half, 0.0F, 1.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, half, half, 0.0F, 1.0F, 1.0F, color);
         WorldVertex.textured(consumer, pose, -half, half, 0.0F, 0.0F, 1.0F, color);
         stack.popPose();
      }
   }
}
