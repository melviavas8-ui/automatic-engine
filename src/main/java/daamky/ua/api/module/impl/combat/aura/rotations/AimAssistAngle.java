package daamky.ua.api.module.impl.combat.aura.rotations;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

public final class AimAssistAngle extends RotateConstructor {
   public static final AimAssistAngle INSTANCE = new AimAssistAngle();
   private Entity lastEntity;
   private long targetAcquiredAtMs;
   private double previousYawStep;
   private double previousPitchStep;
   private double yawVelocity;
   private double pitchVelocity;
   private double yawWavePhase;
   private double pitchWavePhase;
   private double yawWaveSpeed;
   private double pitchWaveSpeed;
   private double speedMultiplier;
   private static LivingEntity syncedTarget;
   private LivingEntity target;
   private long lastFrameNs;
   private long lastRenderAimNs;
   private double aimYawVelocity;
   private double aimPitchVelocity;
   private double heightNoise;
   private long nextHeightNoiseUpdateMs;
   private double currentPointX;
   private double currentPointY;
   private double currentPointZ;
   private double nextPointX;
   private double nextPointY;
   private double nextPointZ;
   private long nextPointChangeAtMs;
   private double pointLerpSpeed;
   private RegistryKey<World> worldKey;
   MinecraftClient client = MinecraftClient.getInstance();

   public AimAssistAngle() {
      super("AimAssist");
      this.randomizeProfile();
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      if (this.client != null && this.client.player != null && this.client.world != null) {
         RegistryKey<World> currentWorldKey = this.client.world.getRegistryKey();
         if (this.worldKey == null) {
            this.worldKey = currentWorldKey;
         } else if (!this.worldKey.equals(currentWorldKey)) {
            this.resetMotionState();
            this.worldKey = currentWorldKey;
            return currentAngle;
         }

         long frameNs = System.nanoTime();
         double frameScale = this.lastFrameNs > 0L ? (double)(frameNs - this.lastFrameNs) / 6.0E7 : 1.0;
         frameScale = MathHelper.clamp(frameScale, 0.05, 3.0);
         this.lastFrameNs = frameNs;
         long nowMs = System.currentTimeMillis();
         this.updateHumanization(nowMs, frameScale);
         return entity instanceof LivingEntity livingTarget ? this.rotateToTarget(currentAngle, this.client, livingTarget, frameScale, nowMs) : currentAngle;
      } else {
         this.resetState();
         return currentAngle;
      }
   }

   private Angle rotateToTarget(Angle currentAngle, MinecraftClient client, LivingEntity selectedTarget, double frameScale, long nowMs) {
      Vec3d eyePos = client.player.getEyePos();
      Vec3d targetPoint = this.getAimPoint(selectedTarget);
      Vec3d delta = targetPoint.subtract(eyePos);
      double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      if (horizontal < 1.0E-4) {
         return currentAngle;
      } else {
         float currentYaw = currentAngle.getYaw();
         float currentPitch = currentAngle.getPitch();
         double targetYaw = Math.toDegrees(Math.atan2(-delta.x, delta.z));
         double targetPitch = MathHelper.clamp(-Math.toDegrees(Math.atan2(delta.y, horizontal)), -90.0, 90.0);
         double yawDiff = MathHelper.wrapDegrees(targetYaw - (double)currentYaw);
         double pitchDiff = 0.0;
         double response = MathHelper.clamp(0.5, 0.08, 1.0);
         double maxYawStep = Math.max(0.35, 10.0 * frameScale * this.speedMultiplier);
         double maxPitchStep = 0.0;
         double followStrength = MathHelper.clamp(0.18 * frameScale + response * 0.45, 0.08, 1.0);
         this.aimYawVelocity = this.smoothStep(this.aimYawVelocity, yawDiff * response, followStrength);
         this.aimPitchVelocity = this.smoothStep(this.aimPitchVelocity, pitchDiff * response, followStrength);
         double yawStep = MathHelper.clamp(this.aimYawVelocity, -maxYawStep, maxYawStep);
         double pitchStep = MathHelper.clamp(this.aimPitchVelocity, -maxPitchStep, maxPitchStep);
         double secondsOnTarget = (double)(nowMs - this.targetAcquiredAtMs) / 1000.0;
         yawStep += Math.sin(this.yawWavePhase + secondsOnTarget * this.yawWaveSpeed * 6.0) * 0.08 * frameScale;
         pitchStep += Math.cos(this.pitchWavePhase + secondsOnTarget * this.pitchWaveSpeed * 6.0) * 0.05 * frameScale;
         yawStep = this.clampToRemaining(this.clampStep(yawStep, this.previousYawStep, maxYawStep * 0.9), yawDiff);
         pitchStep = this.clampToRemaining(this.clampStep(pitchStep, this.previousPitchStep, Math.max(maxPitchStep * 0.9, 0.1)), pitchDiff);
         if (Math.abs(yawDiff) < 0.02) {
            yawStep = yawDiff;
         }

         if (Math.abs(pitchDiff) < 0.02) {
            pitchStep = pitchDiff;
         }

         this.previousYawStep = yawStep;
         this.previousPitchStep = pitchStep;
         float newYaw = currentYaw + (float)yawStep;
         float newPitch = MathHelper.clamp(currentPitch + (float)pitchStep, -90.0F, 90.0F);
         if (!Float.isNaN(newYaw) && !Float.isNaN(newPitch)) {
            this.previousYawStep = yawStep;
            this.previousPitchStep = pitchStep;
            return new Angle(newYaw, newPitch);
         } else {
            return currentAngle;
         }
      }
   }

   private Vec3d getAimPoint(Entity entity) {
      double heightFraction = -0.3F + this.heightNoise;
      double offsetX = 0.0;
      double offsetZ = 0.0;
      double horizontalRadius = (double)entity.getWidth() * 0.5;
      offsetX = this.currentPointX * horizontalRadius;
      offsetZ = this.currentPointZ * horizontalRadius;
      heightFraction += this.currentPointY;
      return entity.getEntityPos().add(offsetX, (double)entity.getHeight() * MathHelper.clamp(heightFraction, 0.05, 0.95), offsetZ);
   }

   private double getAngleTo(Vec3d eyePos, Vec3d lookVec, Entity entity) {
      Vec3d targetVec = this.getAimPoint(entity).subtract(eyePos);
      double length = targetVec.length();
      if (length < 0.001) {
         return 0.0;
      } else {
         targetVec = targetVec.multiply(1.0 / length);
         double dot = lookVec.x * targetVec.x + lookVec.y * targetVec.y + lookVec.z * targetVec.z;
         return Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));
      }
   }

   private void updateHumanization(long nowMs, double frameScale) {
      if (nowMs >= this.nextHeightNoiseUpdateMs) {
         this.heightNoise = ThreadLocalRandom.current().nextDouble(-0.02, 0.02);
         this.nextHeightNoiseUpdateMs = nowMs + ThreadLocalRandom.current().nextLong(180L, 450L);
      }

      this.currentPointX = this.currentPointY = this.currentPointZ = 0.0;
      this.nextPointX = this.nextPointY = this.nextPointZ = 0.0;
      if (nowMs >= this.nextPointChangeAtMs) {
         this.randomizePoint(nowMs);
      }

      double lerpFactor = MathHelper.clamp(this.pointLerpSpeed * frameScale, 0.01, 1.0);
      this.currentPointX = lerp(this.currentPointX, this.nextPointX, lerpFactor);
      this.currentPointY = lerp(this.currentPointY, this.nextPointY, lerpFactor);
      this.currentPointZ = lerp(this.currentPointZ, this.nextPointZ, lerpFactor);
   }

   private static double lerp(double from, double to, double progress) {
      return from + (to - from) * MathHelper.clamp(progress, 0.0, 1.0);
   }

   private void resetState() {
      this.worldKey = null;
      this.resetMotionState();
   }

   private void resetMotionState() {
      this.target = null;
      syncedTarget = null;
      this.targetAcquiredAtMs = 0L;
      this.lastFrameNs = 0L;
      this.lastRenderAimNs = 0L;
      this.previousYawStep = 0.0;
      this.previousPitchStep = 0.0;
      this.aimYawVelocity = 0.0;
      this.aimPitchVelocity = 0.0;
      this.currentPointX = 0.0;
      this.currentPointY = 0.0;
      this.currentPointZ = 0.0;
      this.nextPointX = 0.0;
      this.nextPointY = 0.0;
      this.nextPointZ = 0.0;
      this.nextPointChangeAtMs = 0L;
      this.randomizeProfile(System.currentTimeMillis());
   }

   private void randomizeProfile(long nowMs) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      this.yawWavePhase = random.nextDouble(Math.PI * 2);
      this.pitchWavePhase = random.nextDouble(Math.PI * 2);
      this.yawWaveSpeed = random.nextDouble(0.08, 0.25);
      this.pitchWaveSpeed = random.nextDouble(0.06, 0.2);
      this.speedMultiplier = random.nextDouble(0.75, 1.25);
      this.heightNoise = random.nextDouble(-0.02, 0.02);
      this.nextHeightNoiseUpdateMs = nowMs + random.nextLong(180L, 450L);
      this.pointLerpSpeed = random.nextDouble(0.008, 0.025);
      this.randomizePoint(nowMs);
   }

   private void randomizePoint(long nowMs) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      this.nextPointX = random.nextDouble(-0.35, 0.35);
      this.nextPointY = random.nextDouble(-0.12, 0.12);
      this.nextPointZ = random.nextDouble(-0.35, 0.35);
      this.nextPointChangeAtMs = nowMs + random.nextLong(600L, 1900L);
      this.pointLerpSpeed = random.nextDouble(0.008, 0.025);
   }

   @Override
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }

   private void randomizeProfile() {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      this.yawWavePhase = random.nextDouble(Math.PI * 2);
      this.pitchWavePhase = random.nextDouble(Math.PI * 2);
      this.yawWaveSpeed = random.nextDouble(0.08, 0.25);
      this.pitchWaveSpeed = random.nextDouble(0.06, 0.2);
      this.speedMultiplier = random.nextDouble(0.75, 1.25);
   }

   private double clampStep(double value, double previousValue, double maxDelta) {
      double delta = value - previousValue;
      return Math.abs(delta) > maxDelta ? previousValue + Math.signum(delta) * maxDelta : value;
   }

   private double smoothStep(double previousValue, double targetValue, double response) {
      double clampedResponse = MathHelper.clamp(response, 0.05, 1.0);
      return previousValue + (targetValue - previousValue) * clampedResponse;
   }

   private double clampToRemaining(double step, double remaining) {
      return Math.abs(step) > Math.abs(remaining) ? remaining : step;
   }

   public void reset() {
      this.lastEntity = null;
      this.resetState();
   }
}
