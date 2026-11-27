package daamky.ua.utils.render.world.targetesp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class ChainTargetEspRenderer {
   private static final Identifier CHAIN_TEXTURE = Identifier.fromNamespaceAndPath("daamky", "textures/features/targetesp/chain.png");

   private ChainTargetEspRenderer() {
   }

   public static void render(PoseStack stack, BufferSource provider, TargetEspRenderContext context) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.CHAIN_ESP.apply(CHAIN_TEXTURE));
      float sizeScale = 1.0F;
      float speedScale = 1.0F;
      float animationTime = ((float)context.target().tickCount + context.partialTicks()) * speedScale;
      float impact = TargetEspMath.easeOutCubic(context.chainImpactProgress());
      float baseRadius = context.target().getBbWidth() * 1.35F * sizeScale;
      float bandHeight = Math.max(0.9F, context.target().getBbHeight() * 0.54F * sizeScale);
      float compressedRadius = baseRadius * (1.0F - impact * 0.18F);
      float primaryBandHeight = bandHeight * (1.0F - impact * 0.08F);
      float secondaryBandHeight = bandHeight * 0.8F * (1.0F - impact * 0.12F);
      float roll = Mth.sin((double)(animationTime * 0.22F)) * 18.0F;
      float pitch = Mth.cos((double)(animationTime * 0.18F)) * 18.0F;
      float spin = animationTime * 5.0F;
      int startColor = ColorUtil.tintForDamage(context.primaryColor(), context.hurtProgress());
      int endColor = ColorUtil.tintForDamage(context.secondaryColor(), context.hurtProgress());
      stack.pushPose();
      stack.translate(0.0F, context.target().getBbHeight() * 0.52F, 0.0F);
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
      stack.popPose();
   }

   private static void renderChainBand(
      PoseStack stack,
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
      stack.pushPose();
      stack.mulPose(Axis.ZP.rotationDegrees(roll));
      stack.mulPose(Axis.XP.rotationDegrees(pitch));
      Pose entry = stack.last();
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
         float x0 = Mth.sin((double)rad0) * radius;
         float z0 = Mth.cos((double)rad0) * radius;
         float x1 = Mth.sin((double)rad1) * radius;
         float z1 = Mth.cos((double)rad1) * radius;
         float wobble0 = Mth.sin((double)(progress0 * (float) (Math.PI * 2) + spin * 0.05F)) * 0.06F * speedScale;
         float wobble1 = Mth.sin((double)(progress1 * (float) (Math.PI * 2) + spin * 0.05F)) * 0.06F * speedScale;
         int color0 = ColorUtil.scaleAlpha(ColorUtil.lerpColor(startColor, endColor, progress0), alpha);
         int color1 = ColorUtil.scaleAlpha(ColorUtil.lerpColor(startColor, endColor, progress1), alpha);
         float u0 = progress0 * uvRepeats + uvOffset;
         float u1 = progress1 * uvRepeats + uvOffset;
         consumer.addVertex(entry, x0, bottomY + wobble0, z0).setUv(u0, 0.0F).setColor(color0);
         consumer.addVertex(entry, x1, bottomY + wobble1, z1).setUv(u1, 0.0F).setColor(color1);
         consumer.addVertex(entry, x1, topY + wobble1, z1).setUv(u1, 1.0F).setColor(color1);
         consumer.addVertex(entry, x0, topY + wobble0, z0).setUv(u0, 1.0F).setColor(color0);
      }

      stack.popPose();
   }

   public static void endBatch(BufferSource provider) {
      provider.endBatch(ClientPipelines.CHAIN_ESP.apply(CHAIN_TEXTURE));
   }
}
