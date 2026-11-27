package daamky.ua.utils.render.world.targetesp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class GhostTargetEspRenderer {
   private static final Identifier GHOST_TEXTURE = Identifier.fromNamespaceAndPath("daamky", "textures/particles/ghost-glow.png");

   private GhostTargetEspRenderer() {
   }

   public static void render(PoseStack stack, BufferSource provider, TargetEspRenderContext context) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.GHOSTS_ESP.apply(GHOST_TEXTURE));
      stack.pushPose();
      stack.translate(0.0F, context.target().getBbHeight() * 0.6F, 0.0F);
      particle(stack, consumer, context, GhostTargetEspRenderer.TransformationType.FIRST);
      particle(stack, consumer, context, GhostTargetEspRenderer.TransformationType.SECOND);
      particle(stack, consumer, context, GhostTargetEspRenderer.TransformationType.THIRD);
      stack.popPose();
   }

   private static void particle(
      PoseStack stack, VertexConsumer consumer, TargetEspRenderContext context, GhostTargetEspRenderer.TransformationType transformation
   ) {
      double radius = 0.8F;
      double distance = 20.0;
      float particleSize = 1.0F;
      int alphaFactor = 1;
      int particleCount = Math.max(4, Math.round(10.0F * context.alpha()));

      for (int i = 0; i < particleCount; i++) {
         stack.pushPose();
         double angle = 0.2 * ((double)context.frameTimeMs() * 0.55 - (double)i * distance) / 40.0;
         double sin = Math.sin(angle) * radius;
         double cos = Math.cos(angle) * radius;
         Vec3 trans = transformation.make(sin, cos);
         stack.translate(trans.x, trans.y, trans.z);
         stack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
         float spinRotation = (float)context.frameTimeMs() * 0.1F - (float)i * 10.0F;
         stack.mulPose(Axis.ZP.rotationDegrees(spinRotation));
         stack.translate(particleSize / 2.0F, particleSize / 2.0F, 0.0F);
         int currentAlpha = (int)((float)(255 - i * alphaFactor) * context.alpha());
         int startColor = ColorUtil.withAlpha(context.primaryColor(), currentAlpha);
         int endColor = ColorUtil.withAlpha(context.secondaryColor(), currentAlpha);
         Pose entry = stack.last();
         consumer.addVertex(entry, 0.0F, -particleSize, 0.0F).setUv(0.0F, 0.0F).setColor(endColor);
         consumer.addVertex(entry, -particleSize, -particleSize, 0.0F).setUv(0.0F, 1.0F).setColor(endColor);
         consumer.addVertex(entry, -particleSize, 0.0F, 0.0F).setUv(1.0F, 1.0F).setColor(startColor);
         consumer.addVertex(entry, 0.0F, 0.0F, 0.0F).setUv(1.0F, 0.0F).setColor(startColor);
         stack.popPose();
      }
   }

   public static void endBatch(BufferSource provider) {
      provider.endBatch(ClientPipelines.GHOSTS_ESP.apply(GHOST_TEXTURE));
   }

   private static enum TransformationType {
      FIRST {
         @Override
         Vec3 make(double sin, double cos) {
            return new Vec3(sin, cos, -cos);
         }
      },
      SECOND {
         @Override
         Vec3 make(double sin, double cos) {
            return new Vec3(-sin, sin, -cos);
         }
      },
      THIRD {
         @Override
         Vec3 make(double sin, double cos) {
            return new Vec3(-sin, -sin, cos);
         }
      };

      private TransformationType() {
      }

      abstract Vec3 make(double var1, double var3);
   }
}
