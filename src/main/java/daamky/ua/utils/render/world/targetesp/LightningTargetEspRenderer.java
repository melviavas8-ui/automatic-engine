package daamky.ua.utils.render.world.targetesp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import daamky.ua.utils.render.WorldVertex;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class LightningTargetEspRenderer {
   private static final int MAX_BOLTS = 88;
   private static final int INITIAL_BOLTS = 18;
   private static final int SPAWN_PER_TICK = 3;
   private static final long SPAWN_INTERVAL_MS = 30L;
   private static final int LIGHTNING_DEPTH = 4;
   private static final long HIT_FLASH_DURATION_MS = 350L;
   private static final float CORE_WIDTH = 1.35F;
   private static final float AURA_WIDTH = 3.6F;
   private static final Identifier GLOW_TEXTURE = Identifier.of("daamky", "textures/particles/glow.png");
   private static final int WHITE = ColorUtil.rgba(255, 255, 255, 255);
   private static final int RED = ColorUtil.rgba(255, 59, 48, 255);
   private static final Random RANDOM = new Random();
   private static final LightningTargetEspRenderer.Bolt[] BOLTS = new LightningTargetEspRenderer.Bolt[88];
   private static int boltCount;
   private static long lastSpawnMs;
   private static long lastHitMs = -350L;
   private static int lastTargetId = Integer.MIN_VALUE;
   private static int previousHurtTime;

   private LightningTargetEspRenderer() {
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context, boolean redOnHit) {
      if (context.target() != null && !(context.alpha() <= 0.01F)) {
         if (context.target().getId() != lastTargetId) {
            reset();
            lastTargetId = context.target().getId();
         }

         long now = context.frameTimeMs();
         if (redOnHit && context.target().hurtTime > previousHurtTime) {
            lastHitMs = now;
         }

         previousHurtTime = context.target().hurtTime;
         int spawnCount = lastSpawnMs == 0L ? 18 : 3;
         if ((lastSpawnMs == 0L || now - lastSpawnMs >= 30L) && boltCount < 88) {
            for (int i = 0; i < spawnCount && boltCount < 88; i++) {
               BOLTS[boltCount++] = spawnBolt(context);
            }

            lastSpawnMs = now;
         }

         float hitT = 0.0F;
         if (redOnHit) {
            long sinceHit = now - lastHitMs;
            if (sinceHit < 350L) {
               hitT = 1.0F - (float)sinceHit / 350.0F;
            }
         }

         LightningTargetEspRenderer.RenderableBolt[] renderableBolts = new LightningTargetEspRenderer.RenderableBolt[boltCount];
         int renderableCount = 0;

         for (int i = 0; i < boltCount; i++) {
            LightningTargetEspRenderer.Bolt bolt = BOLTS[i];
            if (bolt != null && now - bolt.spawnTimeMs <= bolt.lifetimeMs) {
               float life = (float)(now - bolt.spawnTimeMs) / (float)bolt.lifetimeMs;
               float fadeIn = smoothstep(0.0F, 0.12F, life);
               float fadeOut = 1.0F - smoothstep(0.58F, 1.0F, life);
               float pulse = (float)Math.sin((double)now * 0.018 + bolt.pulsePhase);
               float secondaryPulse = (float)Math.sin((double)now * 0.007 + bolt.pulsePhase * 1.73);
               float flicker = 0.88F + pulse * 0.08F + secondaryPulse * 0.04F;
               float fade = fadeIn * fadeOut;
               float alpha = context.alpha() * fade * flicker;
               if (!(alpha <= 0.02F)) {
                  int glowRgb = ColorUtil.lerpColor(context.primaryColor() | 0xFF000000, RED, hitT);
                  int coreRgb = ColorUtil.lerpColor(WHITE, RED, hitT);
                  int glowColor = ColorUtil.multAlpha(glowRgb, alpha * 0.55F);
                  int coreColor = ColorUtil.multAlpha(coreRgb, alpha);
                  renderableBolts[renderableCount++] = new LightningTargetEspRenderer.RenderableBolt(bolt, glowColor, coreColor);
               }
            } else {
               BOLTS[i] = BOLTS[--boltCount];
               BOLTS[boltCount] = null;
               i--;
            }
         }

         if (renderableCount > 0) {
            RenderLayer glowType = ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_TEXTURE);
            VertexConsumer glowConsumer = provider.getBuffer(glowType);

            for (int ix = 0; ix < renderableCount; ix++) {
               LightningTargetEspRenderer.RenderableBolt renderable = renderableBolts[ix];
               renderGlowBlobs(stack, glowConsumer, context, renderable.bolt, renderable.glowColor, now);
            }

            provider.draw(glowType);
            VertexConsumer lineConsumer = provider.getBuffer(ClientPipelines.WORLD_PARTICLES_LINES);

            for (int ix = 0; ix < renderableCount; ix++) {
               LightningTargetEspRenderer.RenderableBolt renderable = renderableBolts[ix];
               renderPath(stack, lineConsumer, renderable.bolt, renderable.glowColor, 3.6F, now);
               renderPath(stack, lineConsumer, renderable.bolt, renderable.coreColor, 1.35F, now);
            }

            provider.draw(ClientPipelines.WORLD_PARTICLES_LINES);
         }
      } else {
         reset();
      }
   }

   public static void reset() {
      for (int i = 0; i < boltCount; i++) {
         BOLTS[i] = null;
      }

      boltCount = 0;
      lastSpawnMs = 0L;
      lastHitMs = -350L;
      lastTargetId = Integer.MIN_VALUE;
      previousHurtTime = 0;
   }

   private static LightningTargetEspRenderer.Bolt spawnBolt(TargetEspRenderContext context) {
      double width = (double)context.target().getWidth();
      double height = (double)context.target().getHeight();
      double surfaceRadius = width * 0.5 + 0.05;
      double angle = RANDOM.nextDouble() * Math.PI * 2.0;
      double y = height * (0.12 + RANDOM.nextDouble() * 0.76);
      Vec3d start = new Vec3d(Math.cos(angle) * surfaceRadius, y, Math.sin(angle) * surfaceRadius);
      double length = 0.32 + RANDOM.nextDouble() * 0.38;
      double direction = RANDOM.nextBoolean() ? 1.0 : -1.0;
      Vec3d tangent = new Vec3d(-Math.sin(angle) * direction, 0.0, Math.cos(angle) * direction);
      Vec3d radial = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
      double radialLean = (RANDOM.nextDouble() - 0.5) * 0.22;
      double verticalLean = (RANDOM.nextDouble() - 0.5) * 0.055;
      Vec3d end = start.add(tangent.multiply(length)).add(radial.multiply(length * radialLean)).add(0.0, verticalLean, 0.0);
      LightningTargetEspRenderer.Bolt bolt = new LightningTargetEspRenderer.Bolt();
      bolt.points = generateLightningPath(start, end, 4, length * 0.24);
      bolt.spawnTimeMs = context.frameTimeMs();
      bolt.lifetimeMs = 620L + (long)RANDOM.nextInt(480);
      bolt.orbitSpeed = direction * (1.05 + RANDOM.nextDouble() * 0.75);
      bolt.flowSpeed = 4.5 + RANDOM.nextDouble() * 3.0;
      bolt.swingSpeed = 1.4 + RANDOM.nextDouble() * 0.9;
      bolt.bobAmount = 0.01 + RANDOM.nextDouble() * 0.018;
      bolt.pulsePhase = RANDOM.nextDouble() * Math.PI * 2.0;
      bolt.glowSize = 0.13F + RANDOM.nextFloat() * 0.065F;
      return bolt;
   }

   private static List<Vec3d> generateLightningPath(Vec3d start, Vec3d end, int depth, double maxOffset) {
      if (depth <= 0) {
         List<Vec3d> result = new ArrayList<>(2);
         result.add(start);
         result.add(end);
         return result;
      } else {
         Vec3d dir = end.subtract(start);
         Vec3d mid = start.add(dir.multiply(0.5));
         Vec3d horizontalDir = new Vec3d(dir.x, 0.0, dir.z);
         Vec3d perp = horizontalDir.lengthSquared() < 1.0E-6 ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(-horizontalDir.z, 0.0, horizontalDir.x).normalize();
         double horizontalOffset = (RANDOM.nextDouble() - 0.5) * 2.0 * maxOffset;
         double verticalOffset = (RANDOM.nextDouble() - 0.5) * maxOffset * 0.22;
         Vec3d displacedMid = mid.add(perp.multiply(horizontalOffset)).add(0.0, verticalOffset, 0.0);
         List<Vec3d> left = generateLightningPath(start, displacedMid, depth - 1, maxOffset * 0.5);
         List<Vec3d> right = generateLightningPath(displacedMid, end, depth - 1, maxOffset * 0.5);
         left.remove(left.size() - 1);
         left.addAll(right);
         return left;
      }
   }

   private static void renderPath(MatrixStack stack, VertexConsumer consumer, LightningTargetEspRenderer.Bolt bolt, int color, float width, long now) {
      Entry pose = stack.peek();

      for (int i = 0; i < bolt.points.size() - 1; i++) {
         Vec3d start = animatedPoint(bolt, bolt.points.get(i), i, now);
         Vec3d end = animatedPoint(bolt, bolt.points.get(i + 1), i + 1, now);
         consumer.vertex(pose, (float)start.x, (float)start.y, (float)start.z).color(color).lineWidth(width);
         consumer.vertex(pose, (float)end.x, (float)end.y, (float)end.z).color(color).lineWidth(width);
      }
   }

   private static void renderGlowBlobs(
      MatrixStack stack, VertexConsumer consumer, TargetEspRenderContext context, LightningTargetEspRenderer.Bolt bolt, int color, long now
   ) {
      float pulse = 1.0F + (float)Math.sin((double)now * 0.012 + bolt.pulsePhase) * 0.12F;

      for (int i = 0; i < bolt.points.size(); i++) {
         Vec3d point = animatedPoint(bolt, bolt.points.get(i), i, now);
         float pointVariation = 0.9F + 0.1F * (float)Math.sin((double)i * 1.83 + bolt.pulsePhase);
         float size = bolt.glowSize * pulse * pointVariation;
         stack.push();
         stack.translate(point.x, point.y, point.z);
         stack.multiply(context.cameraRotation());
         Entry pose = stack.peek();
         float half = size * 0.5F;
         WorldVertex.textured(consumer, pose, -half, -half, 0.0F, 0.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, half, -half, 0.0F, 1.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, half, half, 0.0F, 1.0F, 1.0F, color);
         WorldVertex.textured(consumer, pose, -half, half, 0.0F, 0.0F, 1.0F, color);
         stack.pop();
      }
   }

   private static Vec3d animatedPoint(LightningTargetEspRenderer.Bolt bolt, Vec3d point, int pointIndex, long now) {
      double elapsedSeconds = (double)(now - bolt.spawnTimeMs) / 1000.0;
      double flowPhase = elapsedSeconds * bolt.flowSpeed + bolt.pulsePhase + (double)pointIndex * 0.48;
      double flowingWave = Math.sin(flowPhase);
      double horizontalSway = Math.sin(elapsedSeconds * bolt.swingSpeed + bolt.pulsePhase) * 0.12;
      double angle = elapsedSeconds * bolt.orbitSpeed + horizontalSway + flowingWave * 0.045;
      double sin = Math.sin(angle);
      double cos = Math.cos(angle);
      double radiusPulse = 1.0 + flowingWave * 0.04;
      double x = (point.x * cos - point.z * sin) * radiusPulse;
      double z = (point.x * sin + point.z * cos) * radiusPulse;
      double crawlingWave = Math.cos(flowPhase * 0.72) * bolt.bobAmount;
      return new Vec3d(x, point.y + crawlingWave, z);
   }

   private static float smoothstep(float edge0, float edge1, float value) {
      float t = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
      return t * t * (3.0F - 2.0F * t);
   }

   private static final class Bolt {
      private List<Vec3d> points;
      private long spawnTimeMs;
      private long lifetimeMs;
      private double orbitSpeed;
      private double flowSpeed;
      private double swingSpeed;
      private double bobAmount;
      private double pulsePhase;
      private float glowSize;

      private Bolt() {
      }
   }

   private static record RenderableBolt(LightningTargetEspRenderer.Bolt bolt, int glowColor, int coreColor) {
   }
}
