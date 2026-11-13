package daamky.ua.utils.render.world.targetesp;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class ChainTargetEspRenderer {
   private static final Identifier CHAIN_TEXTURE = Identifier.of("daamky", "textures/features/targetesp/chain.png");

   private ChainTargetEspRenderer() {
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.CHAIN_ESP.apply(CHAIN_TEXTURE));
      float sizeScale = 1.0F;
      float speedScale = 1.0F;
      float animationTime = ((float)context.target().age + context.partialTicks()) * speedScale;
      float impact = TargetEspMath.easeOutCubic(context.chainImpactProgress());
      float baseRadius = context.target().getWidth() * 1.35F * sizeScale;
      float bandHeight = Math.max(0.9F, context.target().getHeight() * 0.54F * sizeScale);
      float compressedRadius = baseRadius * (1.0F - impact * 0.18F);
      float primaryBandHeight = bandHeight * (1.0F - impact * 0.08F);
      float secondaryBandHeight = bandHeight * 0.8F * (1.0F - impact * 0.12F);
      float roll = MathHelper.sin((double)(animationTime * 0.22F)) * 18.0F;
      float pitch = MathHelper.cos((double)(animationTime * 0.18F)) * 18.0F;
      float spin = animationTime * 5.0F;
      int startColor = ColorUtil.tintForDamage(context.primaryColor(), context.hurtProgress());
      int endColor = ColorUtil.tintForDamage(context.secondaryColor(), context.hurtProgress());
      stack.push();
      stack.translate(0.0F, context.target().getHeight() * 0.52F, 0.0F);
      renderChainBand(stack, consumer, compressedRadius * 0.9F, primaryBandHeight, roll, pitch, spin, context.alpha(), startColor, endColor, 0.0F, speedScale);
      renderChainBand(
         stack,
         consumer,
         compressedRadius * 0.98F,
         secondaryBandHeight,
         -roll,
         -pitch,
         -spin * 0.92F,
         context.alpha() * 0.92F,
         endColor,
         startColor,
         0.38F,
         speedScale
      );
      stack.pop();
   }

   private static void renderChainBand(
      MatrixStack stack,
      VertexConsumer consumer,
      float radius,
      float bandHeight,
      float roll,
      float pitch,
      float spin,
      float alpha,
      int startColor,
      int endColor,
      float uvOffset,
      float speedScale
   ) {
      stack.push();
      stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
      Entry entry = stack.peek();
      float bottomY = -bandHeight * 0.5F;
      float topY = bandHeight * 0.5F;
      int segments = 64;
      float uvRepeats = 4.0F;

      for (int i = 0; i < segments; i++) {
         float progress0 = (float)i / (float)segments;
         float progress1 = (float)(i + 1) / (float)segments;
         float angle0 = progress0 * 360.0F + spin;
         float angle1 = progress1 * 360.0F + spin;
         float rad0 = angle0 * (float) (Math.PI / 180.0);
         float rad1 = angle1 * (float) (Math.PI / 180.0);
         float x0 = MathHelper.sin((double)rad0) * radius;
         float z0 = MathHelper.cos((double)rad0) * radius;
         float x1 = MathHelper.sin((double)rad1) * radius;
         float z1 = MathHelper.cos((double)rad1) * radius;
         float wobble0 = MathHelper.sin((double)(progress0 * (float) (Math.PI * 2) + spin * 0.05F)) * 0.06F * speedScale;
         float wobble1 = MathHelper.sin((double)(progress1 * (float) (Math.PI * 2) + spin * 0.05F)) * 0.06F * speedScale;
         int color0 = ColorUtil.scaleAlpha(ColorUtil.lerpColor(startColor, endColor, progress0), alpha);
         int color1 = ColorUtil.scaleAlpha(ColorUtil.lerpColor(startColor, endColor, progress1), alpha);
         float u0 = progress0 * uvRepeats + uvOffset;
         float u1 = progress1 * uvRepeats + uvOffset;
         consumer.vertex(entry, x0, bottomY + wobble0, z0).texture(u0, 0.0F).color(color0);
         consumer.vertex(entry, x1, bottomY + wobble1, z1).texture(u1, 0.0F).color(color1);
         consumer.vertex(entry, x1, topY + wobble1, z1).texture(u1, 1.0F).color(color1);
         consumer.vertex(entry, x0, topY + wobble0, z0).texture(u0, 1.0F).color(color0);
      }

      stack.pop();
   }

   public static void endBatch(Immediate provider) {
      provider.draw(ClientPipelines.CHAIN_ESP.apply(CHAIN_TEXTURE));
   }
}
