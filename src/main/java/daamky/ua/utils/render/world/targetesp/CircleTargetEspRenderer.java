package daamky.ua.utils.render.world.targetesp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import daamky.ua.utils.render.WorldVertex;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class CircleTargetEspRenderer {
   private static final Identifier GLOW_TEXTURE = Identifier.of("daamky", "textures/particles/glow.png");
   private static final int RING_SEGMENTS = 160;
   private static final int MAX_PARTICLES = 1200;
   private static final float RING_SPEED_FRAME_SCALE = 2.0F;
   private static final List<CircleTargetEspRenderer.RingParticle> PARTICLES = new ArrayList<>();
   private static final Random RANDOM = new Random();
   private static float angle;
   private static float prevAngle;
   private static float impactProgress;
   private static long lastUpdateMs;
   private static int lastTargetId = Integer.MIN_VALUE;
   private static int prevHurtTime;

   private CircleTargetEspRenderer() {
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context, CircleTargetEspRenderer.Settings settings) {
      update(context, settings);
      float ringAngle = MathHelper.lerp(context.partialTicks(), prevAngle, angle);
      float animationScale = 0.72F + 0.28F * context.alpha();
      float radius = (context.target().getWidth() * 0.86F + 0.18F) * settings.size() * animationScale;
      float y = context.target().getHeight() * verticalProgress(ringAngle);
      renderRing(stack, provider, context, settings, y, radius, ringAngle, context.alpha());
      if (settings.particles()) {
         renderParticles(stack, provider, context, settings);
      }
   }

   public static void endBatch(Immediate provider) {
      provider.draw(ClientPipelines.WORLD_PARTICLES_QUADS);
      provider.draw(ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_TEXTURE));
   }

   public static void reset() {
      PARTICLES.clear();
      angle = 0.0F;
      prevAngle = 0.0F;
      impactProgress = 0.0F;
      lastUpdateMs = 0L;
      lastTargetId = Integer.MIN_VALUE;
      prevHurtTime = 0;
   }

   private static void update(TargetEspRenderContext context, CircleTargetEspRenderer.Settings settings) {
      if (context.target().getId() != lastTargetId) {
         PARTICLES.clear();
         lastTargetId = context.target().getId();
         lastUpdateMs = 0L;
         impactProgress = 0.0F;
         prevHurtTime = 0;
      }

      long now = context.frameTimeMs();
      float deltaTicks = lastUpdateMs == 0L ? 1.0F : MathHelper.clamp((float)(now - lastUpdateMs) / 16.666666F, 0.25F, 3.0F);
      lastUpdateMs = now;
      prevAngle = angle;
      angle = angle + Math.max(0.1F, settings.speed()) * 2.0F * deltaTicks;
      updateImpactAnimation(context, settings, deltaTicks);
      updateParticles(deltaTicks);
      if (!settings.particles()) {
         PARTICLES.clear();
      } else if (!(context.alpha() <= 0.03F) && PARTICLES.size() < 1200) {
         int spawnCount = (int)((float)(8 + RANDOM.nextInt(7)) * Math.max(0.15F, context.alpha()));

         for (int i = 0; i < spawnCount && PARTICLES.size() < 1200; i++) {
            spawnParticle(context, settings);
         }
      }
   }

   private static void updateImpactAnimation(TargetEspRenderContext context, CircleTargetEspRenderer.Settings settings, float deltaTicks) {
      if (!settings.redOnImpact()) {
         impactProgress = 0.0F;
         prevHurtTime = 0;
      } else {
         float fadeIn = settings.impactFadeIn() * deltaTicks;
         float fadeOut = settings.impactFadeOut() * deltaTicks;
         float maxIntensity = settings.impactIntensity();
         int currentHurtTime = context.target().hurtTime;
         if (currentHurtTime <= prevHurtTime && (currentHurtTime <= 0 || prevHurtTime != 0)) {
            if (currentHurtTime > 0) {
               impactProgress = Math.min(maxIntensity, impactProgress + fadeIn * 0.5F);
            } else {
               impactProgress = Math.max(0.0F, impactProgress - fadeOut);
            }
         } else {
            impactProgress = Math.min(maxIntensity, impactProgress + fadeIn);
         }

         prevHurtTime = currentHurtTime;
      }
   }

   private static void updateParticles(float deltaTicks) {
      Iterator<CircleTargetEspRenderer.RingParticle> iterator = PARTICLES.iterator();

      while (iterator.hasNext()) {
         CircleTargetEspRenderer.RingParticle particle = iterator.next();
         particle.prevPos = particle.pos;
         particle.pos = particle.pos.add(particle.velocity.multiply((double)deltaTicks));
         double friction = Math.pow(0.96, (double)deltaTicks);
         particle.velocity = new Vec3d(particle.velocity.x * friction, particle.velocity.y - 0.0022 * (double)deltaTicks, particle.velocity.z * friction);
         particle.age += deltaTicks;
         if (particle.age >= particle.maxAge || particle.pos.y < particle.groundY) {
            iterator.remove();
         }
      }
   }

   private static void spawnParticle(TargetEspRenderContext context, CircleTargetEspRenderer.Settings settings) {
      float animationScale = 0.72F + 0.28F * context.alpha();
      float radius = (context.target().getWidth() * 0.86F + 0.18F) * settings.size() * animationScale;
      float verticalProgress = verticalProgress(angle);
      double centerX = context.targetPos().x;
      double minY = context.targetPos().y + 0.03;
      double maxY = context.targetPos().y + (double)context.target().getHeight() - 0.03;
      double centerY = minY + (maxY - minY) * (double)verticalProgress;
      double centerZ = context.targetPos().z;
      double particleAngle = RANDOM.nextDouble() * (float) (Math.PI * 2);
      double spread = (double)radius * (0.88 + RANDOM.nextDouble() * 0.22);
      float particleHeight = settings.particleHeight();
      double heightLimit = (double)(context.target().getHeight() * particleHeight);
      double y;
      if (particleHeight >= 0.98F) {
         y = minY + RANDOM.nextDouble() * (maxY - minY);
      } else {
         double halfRange = heightLimit * 0.5;
         y = centerY + (RANDOM.nextDouble() - 0.5) * heightLimit;
         double bottom = Math.max(minY, centerY - halfRange);
         double top = Math.min(maxY, centerY + halfRange);
         if (top > bottom) {
            y = Math.max(bottom, Math.min(top, y));
         }
      }

      y += (RANDOM.nextDouble() - 0.5) * 0.025;
      y = Math.max(minY, Math.min(maxY, y));
      Vec3d pos = new Vec3d(centerX + Math.cos(particleAngle) * spread, y, centerZ + Math.sin(particleAngle) * spread);
      Vec3d outward = new Vec3d(Math.cos(particleAngle), 0.0, Math.sin(particleAngle));
      Vec3d velocity = outward.multiply((RANDOM.nextDouble() - 0.5) * 0.01).add(0.0, -8.0E-4 - RANDOM.nextDouble() * 0.006, 0.0);
      float maxAge = Math.max(1.0F, (float)(34 + RANDOM.nextInt(28)) * settings.particleLife());
      float size = (0.0065F + RANDOM.nextFloat() * 0.014F) * settings.size() * animationScale;
      PARTICLES.add(new CircleTargetEspRenderer.RingParticle(pos, velocity, minY - 0.08, maxAge, size, RANDOM.nextInt(360)));
   }

   private static void renderRing(
      MatrixStack stack,
      Immediate provider,
      TargetEspRenderContext context,
      CircleTargetEspRenderer.Settings settings,
      float y,
      float radius,
      float moving,
      float alpha
   ) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.WORLD_PARTICLES_QUADS);
      renderRingBand(stack, consumer, context, settings, y, radius, 0.055F, moving, alpha * 0.18F);
      renderRingBand(stack, consumer, context, settings, y, radius, 0.014F, moving, alpha * 0.92F);
   }

   private static void renderRingBand(
      MatrixStack stack,
      VertexConsumer consumer,
      TargetEspRenderContext context,
      CircleTargetEspRenderer.Settings settings,
      float y,
      float radius,
      float thickness,
      float moving,
      float alpha
   ) {
      if (!(alpha <= 0.01F) && !(radius <= 0.01F)) {
         Entry pose = stack.peek();
         float halfThickness = thickness * 0.5F;
         float innerRadius = Math.max(0.01F, radius - halfThickness);
         float outerRadius = radius + halfThickness;

         for (int i = 0; i < 160; i++) {
            float progress0 = (float)i / 160.0F;
            float progress1 = (float)(i + 1) / 160.0F;
            float degrees0 = progress0 * 360.0F;
            float degrees1 = progress1 * 360.0F;
            float radians0 = degrees0 * (float) (Math.PI / 180.0);
            float radians1 = degrees1 * (float) (Math.PI / 180.0);
            float sin0 = MathHelper.sin((double)radians0);
            float cos0 = MathHelper.cos((double)radians0);
            float sin1 = MathHelper.sin((double)radians1);
            float cos1 = MathHelper.cos((double)radians1);
            int color0 = circleColor(context, settings, (int)(degrees0 + moving), alpha);
            int color1 = circleColor(context, settings, (int)(degrees1 + moving), alpha);
            consumer.vertex(pose, cos0 * outerRadius, y, sin0 * outerRadius).color(color0);
            consumer.vertex(pose, cos1 * outerRadius, y, sin1 * outerRadius).color(color1);
            consumer.vertex(pose, cos1 * innerRadius, y, sin1 * innerRadius).color(color1);
            consumer.vertex(pose, cos0 * innerRadius, y, sin0 * innerRadius).color(color0);
         }
      }
   }

   private static void renderParticles(MatrixStack stack, Immediate provider, TargetEspRenderContext context, CircleTargetEspRenderer.Settings settings) {
      RenderLayer renderType = ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_TEXTURE);
      VertexConsumer consumer = provider.getBuffer(renderType);

      for (CircleTargetEspRenderer.RingParticle particle : PARTICLES) {
         float life = 1.0F - particle.age / Math.max(1.0F, particle.maxAge);
         if (!(life <= 0.0F)) {
            Vec3d pos = new Vec3d(
               MathHelper.lerp((double)context.partialTicks(), particle.prevPos.x, particle.pos.x),
               MathHelper.lerp((double)context.partialTicks(), particle.prevPos.y, particle.pos.y),
               MathHelper.lerp((double)context.partialTicks(), particle.prevPos.z, particle.pos.z)
            );
            float pulse = 0.72F + 0.28F * MathHelper.sin((double)((particle.age + (float)particle.seed) * 0.7F));
            float particleAlpha = context.alpha() * life * pulse * 0.82F;
            float size = particle.size * (0.75F + life * 0.45F);
            appendParticle(stack, consumer, context, pos, size * 5.4F, circleColor(context, settings, particle.seed, particleAlpha * 0.24F));
            appendParticle(stack, consumer, context, pos, size, circleColor(context, settings, particle.seed, particleAlpha));
         }
      }
   }

   private static void appendParticle(MatrixStack stack, VertexConsumer consumer, TargetEspRenderContext context, Vec3d pos, float size, int color) {
      if (!(size <= 0.001F) && ColorUtil.getAlpha(color) > 0) {
         stack.push();
         stack.translate(pos.x - context.targetPos().x, pos.y - context.targetPos().y, pos.z - context.targetPos().z);
         stack.multiply(context.cameraRotation());
         stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
         Entry pose = stack.peek();
         float half = size * 0.5F;
         WorldVertex.textured(consumer, pose, -half, -half, 0.0F, 0.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, half, -half, 0.0F, 1.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, half, half, 0.0F, 1.0F, 1.0F, color);
         WorldVertex.textured(consumer, pose, -half, half, 0.0F, 0.0F, 1.0F, color);
         stack.pop();
      }
   }

   private static int circleColor(TargetEspRenderContext context, CircleTargetEspRenderer.Settings settings, int hueOffset, float alpha) {
      int primary = context.primaryColor();
      int secondary = context.secondaryColor();
      float blend = 0.5F + 0.5F * MathHelper.sin((double)((float)hueOffset * (float) (Math.PI / 180.0)));
      int base = ColorUtil.lerpColor(primary, secondary, blend);
      if (settings.redOnImpact() && impactProgress > 0.0F) {
         float impact = TargetEspMath.easeOutCubic(impactProgress);
         int impactColor = ColorUtil.rgba(255, 32, 32, ColorUtil.getAlpha(base));
         base = ColorUtil.lerpColor(base, impactColor, impact);
      }

      return ColorUtil.multAlpha(base, alpha);
   }

   private static float verticalProgress(float value) {
      return (MathHelper.sin((double)(value * (float) (Math.PI / 180.0))) + 1.0F) * 0.5F;
   }

   private static final class RingParticle {
      private Vec3d prevPos;
      private Vec3d pos;
      private Vec3d velocity;
      private final double groundY;
      private final float maxAge;
      private final float size;
      private final int seed;
      private float age;

      private RingParticle(Vec3d pos, Vec3d velocity, double groundY, float maxAge, float size, int seed) {
         this.prevPos = pos;
         this.pos = pos;
         this.velocity = velocity;
         this.groundY = groundY;
         this.maxAge = maxAge;
         this.size = size;
         this.seed = seed;
      }
   }

   public static record Settings(
      float size,
      float speed,
      boolean particles,
      float particleHeight,
      float particleLife,
      boolean redOnImpact,
      float impactFadeIn,
      float impactFadeOut,
      float impactIntensity
   ) {
   }
}
