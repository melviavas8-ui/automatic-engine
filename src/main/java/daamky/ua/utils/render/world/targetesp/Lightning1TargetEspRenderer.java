package daamky.ua.utils.render.world.targetesp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class Lightning1TargetEspRenderer {
   private static final int STROKE_COUNT = 3;
   private static final int SEGMENTS = 30;
   private static final long[] STROKE_PERIOD = new long[]{230L, 310L, 270L};

   private Lightning1TargetEspRenderer() {
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context, float glow) {
      LivingEntity target = context.target();
      if (target != null) {
         float height = Math.max(0.6F, target.getHeight());
         float width = Math.max(0.3F, target.getWidth());
         float scale = MathHelper.clamp(height / 1.8F, 0.55F, 2.2F);
         float radius = Math.max(0.5F, width * 0.92F);
         Vec3d cameraLocal = MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos().subtract(target.getLerpedPos(context.partialTicks()));
         int tint = tintColor(context);
         long now = context.frameTimeMs();
         List<Lightning1TargetEspRenderer.Stroke> strokes = new ArrayList<>();

         for (int index = 0; index < 3; index++) {
            long period = STROKE_PERIOD[index];
            long strike = Math.floorDiv(now, period) + (long)index * 31L;
            float phase = (float)Math.floorMod(now, period) / (float)period;
            int seed = (int)(strike * 2654435761L) + index * 7919;
            float life = strokeLife(phase) * (0.55F + 0.45F * hash(seed + 4711));
            if (!(life <= 0.02F)) {
               float alpha = life * context.alpha();
               if (!(alpha <= 0.01F)) {
                  strokes.add(buildStroke(seed, false, height, radius, scale, tint, alpha, now));
                  if (hash(seed + 8821) > 0.34F) {
                     strokes.add(buildStroke(seed, true, height, radius, scale, tint, alpha * 0.72F, now));
                  }
               }
            }
         }

         if (!strokes.isEmpty()) {
            if (glow > 0.01F) {
               VertexConsumer consumer = provider.getBuffer(ClientPipelines.TARGET_LIGHTNING_GLOW);

               for (Lightning1TargetEspRenderer.Stroke stroke : strokes) {
                  emitGlow(stack, consumer, stroke, cameraLocal, 3.2F + glow * 1.8F, Math.min(1.0F, 0.16F * glow));
               }

               provider.draw(ClientPipelines.TARGET_LIGHTNING_GLOW);
            }

            VertexConsumer consumer = provider.getBuffer(ClientPipelines.TARGET_LIGHTNING);

            for (Lightning1TargetEspRenderer.Stroke stroke : strokes) {
               emitRibbon(stack, consumer, stroke, cameraLocal);
            }

            provider.draw(ClientPipelines.TARGET_LIGHTNING);
         }
      }
   }

   public static void endBatch(Immediate provider) {
      provider.draw(ClientPipelines.TARGET_LIGHTNING_GLOW);
      provider.draw(ClientPipelines.TARGET_LIGHTNING);
   }

   private static Lightning1TargetEspRenderer.Stroke buildStroke(
      int seed, boolean branch, float height, float radius, float scale, int tint, float alpha, long now
   ) {
      float startAngle = hash(seed + 17) * (float) (Math.PI * 2);
      float span = (1.1F + hash(seed + 53) * 4.2F) * (hash(seed + 71) > 0.5F ? 1.0F : -1.0F);
      float lowY = height * (0.05F + hash(seed + 97) * 0.25F);
      float highY = height * (0.7F + hash(seed + 131) * 0.42F);
      float swirl = (hash(seed + 199) - 0.5F) * 1.4F;
      float branchStart = 0.3F + hash(seed + 233) * 0.34F;
      float branchSpan = 0.2F + hash(seed + 281) * 0.18F;
      float branchAngle = (hash(seed + 313) - 0.5F) * 2.6F;
      float branchLift = (hash(seed + 349) - 0.5F) * 1.5F;
      Vec3d[] points = new Vec3d[31];
      float[] widths = new float[31];
      int[] colors = new int[31];
      float baseWidth = 0.042F * scale * (0.75F + alpha * 0.55F) * (branch ? 0.62F : 1.0F);

      for (int i = 0; i <= 30; i++) {
         float t = (float)i / 30.0F;
         float pathT = branch ? branchStart + t * branchSpan * 0.5F : t;
         Vec3d point = pathPoint(seed, pathT, startAngle, span, swirl, lowY, highY, radius, scale);
         if (branch) {
            float pull = (float)Math.pow((double)t, 1.35) * 0.55F * scale;
            float angle = startAngle + span * pathT + branchAngle;
            point = point.add(Math.cos((double)angle) * (double)pull, (double)(branchLift * pull), Math.sin((double)angle) * (double)pull);
         }

         points[i] = point;
         float taper = (float)Math.pow(Math.sin((double)t * Math.PI), 0.45);
         widths[i] = baseWidth * (0.3F + 0.7F * taper);
         float travel = 0.68F + 0.32F * (float)Math.sin((double)t * 17.0 - (double)now * 0.013 + (double)seed);
         int vertexAlpha = Math.round(MathHelper.clamp(alpha * travel, 0.0F, 1.0F) * 255.0F);
         colors[i] = ColorUtil.withAlpha(tint, vertexAlpha);
      }

      return new Lightning1TargetEspRenderer.Stroke(points, widths, colors);
   }

   private static void emitRibbon(MatrixStack stack, VertexConsumer consumer, Lightning1TargetEspRenderer.Stroke stroke, Vec3d cameraLocal) {
      TargetEspRibbon.ribbon(stack, consumer, stroke.points(), stroke.widths(), stroke.colors(), cameraLocal);
   }

   private static void emitGlow(
      MatrixStack stack, VertexConsumer consumer, Lightning1TargetEspRenderer.Stroke stroke, Vec3d cameraLocal, float widthScale, float alphaScale
   ) {
      TargetEspRibbon.sprites(stack, consumer, stroke.points(), stroke.widths(), stroke.colors(), cameraLocal, widthScale, alphaScale);
   }

   private static Vec3d pathPoint(int seed, float t, float startAngle, float span, float swirl, float lowY, float highY, float radius, float scale) {
      float angle = startAngle + span * t + swirl * (float)Math.sin((double)t * Math.PI * 2.0);
      float y = lowY + (highY - lowY) * t;
      float r = radius * (0.82F + 0.3F * (float)Math.sin((double)t * Math.PI));
      r += fractal(seed, t, 3.1F) * 0.21F * scale;
      y += fractal(seed + 53, t, 3.7F) * 0.17F * scale;
      angle += fractal(seed + 29, t, 2.9F) * 0.24F;
      return new Vec3d(Math.cos((double)angle) * (double)r, (double)y, Math.sin((double)angle) * (double)r);
   }

   private static float fractal(int seed, float t, float frequency) {
      return noise(seed, t * frequency) + noise(seed + 911, t * frequency * 2.3F) * 0.5F + noise(seed + 337, t * frequency * 5.1F) * 0.25F;
   }

   private static float strokeLife(float phase) {
      float attack = MathHelper.clamp(phase / 0.07F, 0.0F, 1.0F);
      float decay = (float)Math.pow((double)(1.0F - phase), 1.4);
      return attack * decay;
   }

   private static int tintColor(TargetEspRenderContext context) {
      float hurt = MathHelper.clamp(context.hurtProgress(), 0.0F, 1.0F);
      return hurt <= 0.001F ? context.primaryColor() : lerpColor(context.primaryColor(), context.secondaryColor(), hurt * 0.85F);
   }

   private static int lerpColor(int from, int to, float progress) {
      return ColorUtil.rgba(
         Math.round(MathHelper.lerp(progress, (float)ColorUtil.getRed(from), (float)ColorUtil.getRed(to))),
         Math.round(MathHelper.lerp(progress, (float)ColorUtil.getGreen(from), (float)ColorUtil.getGreen(to))),
         Math.round(MathHelper.lerp(progress, (float)ColorUtil.getBlue(from), (float)ColorUtil.getBlue(to))),
         255
      );
   }

   private static float noise(int seed, float x) {
      int cell = MathHelper.floor(x);
      float local = x - (float)cell;
      local = local * local * (3.0F - 2.0F * local);
      float a = hash(seed * 73856093 + cell);
      float b = hash(seed * 73856093 + cell + 1);
      return (a + (b - a) * local) * 2.0F - 1.0F;
   }

   private static float hash(int value) {
      int x = value * -1640531535;
      x ^= x >>> 15;
      x *= -2048144789;
      x ^= x >>> 13;
      x *= -1028477387;
      x ^= x >>> 16;
      return (float)(x >>> 8) / 1.6777216E7F;
   }

   private static record Stroke(Vec3d[] points, float[] widths, int[] colors) {
   }
}
