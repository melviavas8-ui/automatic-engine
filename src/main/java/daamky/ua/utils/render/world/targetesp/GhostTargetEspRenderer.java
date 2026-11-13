package daamky.ua.utils.render.world.targetesp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class GhostTargetEspRenderer {
   private static final Identifier GHOST_TEXTURE = Identifier.of("daamky", "textures/particles/ghost-glow.png");

   private GhostTargetEspRenderer() {
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.GHOSTS_ESP.apply(GHOST_TEXTURE));
      stack.push();
      stack.translate(0.0F, context.target().getHeight() * 0.6F, 0.0F);
      particle(stack, consumer, context, GhostTargetEspRenderer.TransformationType.FIRST);
      particle(stack, consumer, context, GhostTargetEspRenderer.TransformationType.SECOND);
      particle(stack, consumer, context, GhostTargetEspRenderer.TransformationType.THIRD);
      stack.pop();
   }

   private static void particle(
      MatrixStack stack, VertexConsumer consumer, TargetEspRenderContext context, GhostTargetEspRenderer.TransformationType transformation
   ) {
      double radius = 0.8F;
      double distance = 20.0;
      float particleSize = 1.0F;
      int alphaFactor = 1;
      int particleCount = Math.max(4, Math.round(10.0F * context.alpha()));

      for (int i = 0; i < particleCount; i++) {
         stack.push();
         double angle = 0.2 * ((double)context.frameTimeMs() * 0.55 - (double)i * distance) / 40.0;
         double sin = Math.sin(angle) * radius;
         double cos = Math.cos(angle) * radius;
         Vec3d trans = transformation.make(sin, cos);
         stack.translate(trans.x, trans.y, trans.z);
         stack.multiply(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());
         float spinRotation = (float)context.frameTimeMs() * 0.1F - (float)i * 10.0F;
         stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spinRotation));
         stack.translate(particleSize / 2.0F, particleSize / 2.0F, 0.0F);
         int currentAlpha = (int)((float)(255 - i * alphaFactor) * context.alpha());
         int startColor = ColorUtil.withAlpha(context.primaryColor(), currentAlpha);
         int endColor = ColorUtil.withAlpha(context.secondaryColor(), currentAlpha);
         Entry entry = stack.peek();
         consumer.vertex(entry, 0.0F, -particleSize, 0.0F).texture(0.0F, 0.0F).color(endColor);
         consumer.vertex(entry, -particleSize, -particleSize, 0.0F).texture(0.0F, 1.0F).color(endColor);
         consumer.vertex(entry, -particleSize, 0.0F, 0.0F).texture(1.0F, 1.0F).color(startColor);
         consumer.vertex(entry, 0.0F, 0.0F, 0.0F).texture(1.0F, 0.0F).color(startColor);
         stack.pop();
      }
   }

   public static void endBatch(Immediate provider) {
      provider.draw(ClientPipelines.GHOSTS_ESP.apply(GHOST_TEXTURE));
   }

   private static enum TransformationType {
      FIRST {
         @Override
         Vec3d make(double sin, double cos) {
            return new Vec3d(sin, cos, -cos);
         }
      },
      SECOND {
         @Override
         Vec3d make(double sin, double cos) {
            return new Vec3d(-sin, sin, -cos);
         }
      },
      THIRD {
         @Override
         Vec3d make(double sin, double cos) {
            return new Vec3d(-sin, -sin, cos);
         }
      };

      private TransformationType() {
      }

      abstract Vec3d make(double var1, double var3);
   }
}
