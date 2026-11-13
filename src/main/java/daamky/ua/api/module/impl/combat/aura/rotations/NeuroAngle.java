package daamky.ua.api.module.impl.combat.aura.rotations;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.util.MathUtils;

public final class NeuroAngle extends RotateConstructor {
   public static final NeuroAngle INSTANCE = new NeuroAngle();
   private static final double DEFAULT_SPEED = 155.0;
   private static final double DEFAULT_SMOOTHNESS = 6.0;
   private static final double MIN_YAW_MICRO = 0.01;
   private static final double MIN_PITCH_MICRO = 0.007;
   private static final long PROFILE_SWITCH_MIN_MS = 120L;
   private static final long PROFILE_SWITCH_MAX_MS = 300L;
   private static final long PROFILE_SETTLE_MS = 140L;
   private static final int STEP_HISTORY_SIZE = 4;
   private Entity lastEntity;
   private long targetAcquiredAtMs;
   private long lastUpdateAtMs;
   private long nextProfileSwitchAtMs;
   private double previousYawStep;
   private double previousPitchStep;
   private double yawVelocity;
   private double pitchVelocity;
   private double yawWavePhase;
   private double pitchWavePhase;
   private double yawWaveSpeed;
   private double pitchWaveSpeed;
   private double profileResponse;
   private double profileStiffness;
   private double profileYawLead;
   private double profilePitchLag;
   private double profileOvershoot;
   private double profileMicroScale;
   private double profileDampingBias;
   private double profileStepResponseBias;
   private double profileStepLimitScale;
   private Angle lastTargetAngle;
   private NeuroAngle.RotationPattern activePattern;
   private final double[] yawStepHistory = new double[4];
   private final double[] pitchStepHistory = new double[4];

   private NeuroAngle() {
      super("Neuro");
      this.randomizeProfile(true, 0L);
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      if (currentAngle != null && targetAngle != null) {
         long now = System.currentTimeMillis();
         if (entity != this.lastEntity) {
            this.lastEntity = entity;
            this.targetAcquiredAtMs = now;
            this.resetMotion();
         }

         this.updateProfileState(now, entity != null);
         double tickScale = this.computeTickScale(now);
         double targetMotion = this.computeTargetMotion(targetAngle);
         double distance = this.computeDistanceToTarget(entity);
         double targetSpeed = this.computeTargetSpeed(entity);
         double inputYawVelocity = this.yawVelocity;
         double inputPitchVelocity = this.pitchVelocity;
         double inputPreviousYawStep = this.previousYawStep;
         double inputPreviousPitchStep = this.previousPitchStep;
         double[] yawHistorySnapshot = (double[])this.yawStepHistory.clone();
         double[] pitchHistorySnapshot = (double[])this.pitchStepHistory.clone();
         Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
         double yawDelta = (double)delta.getYaw();
         double pitchDelta = (double)delta.getPitch();
         double deltaLength = Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
         AuraModule aura = AuraModule.getInstance();
         StrikeManager attackHandler = aura != null ? aura.getAttackHandlerRaw() : null;
         double speed = aura != null ? 9.0 : 155.0;
         double smoothnessValue = aura != null ? aura.getAimAssistSmoothness().getValue() : 6.0;
         double smoothness = MathHelper.clamp((smoothnessValue - 1.0) / 9.0, 0.0, 1.0);
         double warmup = MathHelper.clamp((double)(now - this.targetAcquiredAtMs) / 150.0, 0.0, 1.0);
         double angleResponse = this.easeOutCubic(MathHelper.clamp(deltaLength / 60.0, 0.0, 1.0));
         double distanceResponse = this.computeDistanceResponse(aura, entity);
         boolean attackWindow = attackHandler != null && !attackHandler.getAttackTimer().finished(160.0);
         double sampleShift = MathHelper.clamp(targetMotion / 18.0, 0.0, 1.0);
         NeuroAimPointFeatures aimPointFeatures = entity instanceof LivingEntity livingTarget
            ? NeuroAimPointFeatures.from(livingTarget, livingTarget.getBoundingBox(), vec3d, aura != null && aura.getOptions().isSelected("Multi Points"))
            : NeuroAimPointFeatures.from(null, null, null, false);
         if (deltaLength < 0.03) {
            double restDamping = Math.pow(0.42, tickScale);
            this.yawVelocity *= restDamping;
            this.pitchVelocity *= restDamping;
            this.logDatasetSample(
               aura,
               entity,
               now,
               currentAngle,
               targetAngle,
               yawDelta,
               pitchDelta,
               deltaLength,
               distance,
               distanceResponse,
               targetMotion,
               targetSpeed,
               inputYawVelocity,
               inputPitchVelocity,
               attackWindow,
               tickScale,
               smoothness,
               warmup,
               angleResponse,
               sampleShift,
               inputPreviousYawStep,
               inputPreviousPitchStep,
               yawHistorySnapshot,
               pitchHistorySnapshot,
               aimPointFeatures.multiPointEnabled(),
               aimPointFeatures.aimOffsetX(),
               aimPointFeatures.aimOffsetY(),
               aimPointFeatures.aimOffsetZ(),
               aimPointFeatures.aimOffsetLength(),
               0.0,
               0.0,
               0.0,
               0.0
            );
            return currentAngle;
         } else {
            double targetStability = 1.0 - sampleShift * 0.28;
            double responseScale = (0.72 + angleResponse * 0.44 + distanceResponse * 0.18 + this.profileResponse) * targetStability;
            double smoothPenalty = 1.0 - smoothness * 0.42;
            double baseStiffness = (1.0 + speed * 1.0) * (0.42 + warmup * 0.58) * responseScale * smoothPenalty * this.profileStiffness * tickScale;
            double yawStiffness = baseStiffness * (0.96 + angleResponse * 0.44 + this.profileYawLead);
            double pitchStiffness = baseStiffness * (0.72 + angleResponse * 0.22 - this.profilePitchLag);
            yawStiffness *= this.activePattern.yawStiffnessScale;
            pitchStiffness *= this.activePattern.pitchStiffnessScale;
            double damping = MathHelper.clamp(0.78 - smoothness * 0.055 + distanceResponse * 0.03 - sampleShift * 0.035 + this.profileDampingBias, 0.58, 0.88);
            double velocityDamping = Math.pow(damping, tickScale);
            double maxYawSpeed = (0.76 + speed * 0.62) * (0.56 + angleResponse * 0.82 + distanceResponse * 0.16 + this.profileYawLead * 0.25) * tickScale;
            double maxPitchSpeed = (0.36 + speed * 0.36) * (0.6 + angleResponse * 0.54 + distanceResponse * 0.12 - this.profilePitchLag * 0.12) * tickScale;
            maxYawSpeed *= this.activePattern.yawLimitScale;
            maxPitchSpeed *= this.activePattern.pitchLimitScale;
            if (attackWindow) {
               yawStiffness *= 1.08;
               pitchStiffness *= 1.03;
               maxYawSpeed *= 1.1;
               maxPitchSpeed *= 1.05;
            }

            if (deltaLength > 35.0) {
               double entryBoost = MathHelper.clamp((deltaLength - 35.0) / 55.0, 0.0, 1.0);
               yawStiffness *= 1.0 + entryBoost * 0.36;
               pitchStiffness *= 1.0 + entryBoost * 0.18;
               maxYawSpeed *= 1.0 + entryBoost * 0.24;
               maxPitchSpeed *= 1.0 + entryBoost * 0.16;
            }

            if (this.previousYawStep != 0.0 && Math.signum(this.previousYawStep) != Math.signum(yawDelta)) {
               this.yawVelocity *= 0.86;
            }

            if (this.previousPitchStep != 0.0 && Math.signum(this.previousPitchStep) != Math.signum(pitchDelta)) {
               this.pitchVelocity *= 0.9;
            }

            this.yawVelocity += yawDelta * yawStiffness;
            this.pitchVelocity += pitchDelta * pitchStiffness;
            this.yawVelocity *= velocityDamping;
            this.pitchVelocity *= velocityDamping;
            if (deltaLength > 22.0 && Math.signum(yawDelta) == Math.signum(this.yawVelocity)) {
               this.yawVelocity = this.yawVelocity + Math.signum(yawDelta) * (0.025 + angleResponse * 0.06) * this.profileOvershoot * tickScale;
            }

            if (deltaLength > 18.0 && Math.signum(pitchDelta) == Math.signum(this.pitchVelocity)) {
               this.pitchVelocity = this.pitchVelocity + Math.signum(pitchDelta) * (0.01 + angleResponse * 0.025) * this.profileOvershoot * tickScale;
            }

            this.yawVelocity = MathHelper.clamp(this.yawVelocity, -maxYawSpeed, maxYawSpeed);
            this.pitchVelocity = MathHelper.clamp(this.pitchVelocity, -maxPitchSpeed, maxPitchSpeed);
            if (Math.abs(yawDelta) < 1.15) {
               this.yawVelocity *= 0.58;
            }

            if (Math.abs(pitchDelta) < 1.1) {
               this.pitchVelocity *= 0.56;
            }

            this.yawWavePhase = this.yawWavePhase + this.yawWaveSpeed * tickScale;
            this.pitchWavePhase = this.pitchWavePhase + this.pitchWaveSpeed * tickScale;
            double microScale = (1.0 - smoothness * 0.76) * (0.24 + angleResponse * 0.52) * this.profileMicroScale * this.activePattern.microScaleBias;
            double sensitivityStep = MathUtils.computeGcd();
            double yawMicro = Math.max(0.01, sensitivityStep * 0.2) * microScale * (0.82 + sampleShift * 0.2);
            double pitchMicro = Math.max(0.007, sensitivityStep * 0.16) * microScale * 0.78 * MathHelper.clamp(Math.abs(pitchDelta) / 4.5, 0.16, 1.0);
            double yawWave = Math.sin(this.yawWavePhase * this.activePattern.waveShape) * yawMicro * this.activePattern.waveAmplitude;
            double pitchWave = Math.cos(this.pitchWavePhase * this.activePattern.waveShape * 0.92 + this.activePattern.waveOffset)
               * pitchMicro
               * this.activePattern.waveAmplitude;
            double yawStep = this.yawVelocity + yawWave;
            double pitchStep = this.pitchVelocity + pitchWave;
            if (deltaLength < 2.4) {
               yawStep *= 0.86;
               pitchStep *= 0.78;
            }

            if (deltaLength < 7.5) {
               yawStep *= this.activePattern.nearTargetFriction;
               pitchStep *= this.activePattern.nearTargetFriction;
            }

            double stepResponse = MathHelper.clamp(
               0.3 + angleResponse * 0.26 + distanceResponse * 0.1 + warmup * 0.1 - smoothness * 0.14 + this.profileStepResponseBias, 0.1, 0.84
            );
            yawStep = this.smoothStep(this.previousYawStep, yawStep, stepResponse);
            pitchStep = this.smoothStep(this.previousPitchStep, pitchStep, stepResponse);
            double yawStepLimit = (10.18 + speed * 10.2) * (10.72 + angleResponse * 10.28 + distanceResponse * 0.16) * tickScale * this.profileStepLimitScale;
            double pitchStepLimit = (0.11 + speed * 0.15) * (0.72 + angleResponse * 0.22 + distanceResponse * 0.1) * tickScale * this.profileStepLimitScale;
            yawStep = this.clampStep(yawStep, this.previousYawStep, yawStepLimit);
            pitchStep = this.clampStep(pitchStep, this.previousPitchStep, pitchStepLimit);
            yawStep = this.clampToRemaining(yawStep, yawDelta);
            pitchStep = this.clampToRemaining(pitchStep, pitchDelta);
            NeuroRuntimeModel.InferenceContext inferenceContext = new NeuroRuntimeModel.InferenceContext(
               yawDelta,
               pitchDelta,
               deltaLength,
               distance,
               distanceResponse,
               targetMotion,
               targetSpeed,
               inputYawVelocity,
               inputPitchVelocity,
               attackWindow,
               tickScale,
               smoothness,
               warmup,
               angleResponse,
               sampleShift,
               inputPreviousYawStep,
               inputPreviousPitchStep,
               aimPointFeatures.multiPointEnabled(),
               aimPointFeatures.aimOffsetX(),
               aimPointFeatures.aimOffsetY(),
               aimPointFeatures.aimOffsetZ(),
               aimPointFeatures.aimOffsetLength(),
               yawStep,
               pitchStep,
               yawMicro,
               pitchMicro
            );
            NeuroRuntimeModel.Selection selection = NeuroHumanRuntimeModel.getInstance().predict(aura, inferenceContext);
            if (!selection.modelApplied()) {
               selection = NeuroRuntimeModel.getInstance().selectStep(aura, inferenceContext);
            }

            double appliedYawStep = selection.yawStep();
            double appliedPitchStep = selection.pitchStep();
            if (Math.abs(appliedYawStep) < 0.0025 && Math.abs(appliedPitchStep) < 0.0025) {
               appliedYawStep = 0.0;
               appliedPitchStep = 0.0;
               this.previousYawStep = appliedYawStep;
               this.previousPitchStep = appliedPitchStep;
               this.pushStepHistory(this.yawStepHistory, appliedYawStep);
               this.pushStepHistory(this.pitchStepHistory, appliedPitchStep);
               this.logDatasetSample(
                  aura,
                  entity,
                  now,
                  currentAngle,
                  targetAngle,
                  yawDelta,
                  pitchDelta,
                  deltaLength,
                  distance,
                  distanceResponse,
                  targetMotion,
                  targetSpeed,
                  inputYawVelocity,
                  inputPitchVelocity,
                  attackWindow,
                  tickScale,
                  smoothness,
                  warmup,
                  angleResponse,
                  sampleShift,
                  inputPreviousYawStep,
                  inputPreviousPitchStep,
                  yawHistorySnapshot,
                  pitchHistorySnapshot,
                  aimPointFeatures.multiPointEnabled(),
                  aimPointFeatures.aimOffsetX(),
                  aimPointFeatures.aimOffsetY(),
                  aimPointFeatures.aimOffsetZ(),
                  aimPointFeatures.aimOffsetLength(),
                  yawStep,
                  pitchStep,
                  appliedYawStep,
                  appliedPitchStep
               );
               return currentAngle;
            } else {
               this.previousYawStep = appliedYawStep;
               this.previousPitchStep = appliedPitchStep;
               this.pushStepHistory(this.yawStepHistory, appliedYawStep);
               this.pushStepHistory(this.pitchStepHistory, appliedPitchStep);
               this.logDatasetSample(
                  aura,
                  entity,
                  now,
                  currentAngle,
                  targetAngle,
                  yawDelta,
                  pitchDelta,
                  deltaLength,
                  distance,
                  distanceResponse,
                  targetMotion,
                  targetSpeed,
                  inputYawVelocity,
                  inputPitchVelocity,
                  attackWindow,
                  tickScale,
                  smoothness,
                  warmup,
                  angleResponse,
                  sampleShift,
                  inputPreviousYawStep,
                  inputPreviousPitchStep,
                  yawHistorySnapshot,
                  pitchHistorySnapshot,
                  aimPointFeatures.multiPointEnabled(),
                  aimPointFeatures.aimOffsetX(),
                  aimPointFeatures.aimOffsetY(),
                  aimPointFeatures.aimOffsetZ(),
                  aimPointFeatures.aimOffsetLength(),
                  yawStep,
                  pitchStep,
                  appliedYawStep,
                  appliedPitchStep
               );
               return new Angle(
                  currentAngle.getYaw() + (float)appliedYawStep, MathHelper.clamp(currentAngle.getPitch() + (float)appliedPitchStep, -90.0F, 90.0F)
               );
            }
         }
      } else {
         return currentAngle;
      }
   }

   @Override
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }

   private double computeDistanceResponse(AuraModule aura, Entity entity) {
      if (aura != null && entity != null) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player == null) {
            return 0.0;
         } else {
            double holdDistance = Math.max(0.001, (double)aura.finalDistance());
            double currentDistance = (double)client.player.distanceTo(entity);
            return MathHelper.clamp(currentDistance / holdDistance, 0.0, 1.0);
         }
      } else {
         return 0.0;
      }
   }

   private double computeDistanceToTarget(Entity entity) {
      MinecraftClient client = MinecraftClient.getInstance();
      return entity != null && client.player != null ? (double)client.player.distanceTo(entity) : 0.0;
   }

   private double computeTargetSpeed(Entity entity) {
      return entity == null ? 0.0 : entity.getVelocity().length();
   }

   private void resetMotion() {
      this.previousYawStep = 0.0;
      this.previousPitchStep = 0.0;
      this.yawVelocity = 0.0;
      this.pitchVelocity = 0.0;
      this.lastUpdateAtMs = 0L;
      this.nextProfileSwitchAtMs = 0L;
      this.lastTargetAngle = null;
      Arrays.fill(this.yawStepHistory, 0.0);
      Arrays.fill(this.pitchStepHistory, 0.0);
      this.randomizeProfile(true, System.currentTimeMillis());
   }

   private void updateProfileState(long now, boolean hasTarget) {
      if (hasTarget) {
         if (now - this.targetAcquiredAtMs >= 140L) {
            if (this.nextProfileSwitchAtMs == 0L || now >= this.nextProfileSwitchAtMs) {
               this.yawVelocity *= 0.68;
               this.pitchVelocity *= 0.68;
               this.previousYawStep *= 0.6;
               this.previousPitchStep *= 0.6;
               this.randomizeProfile(false, now);
            }
         }
      }
   }

   private void randomizeProfile(boolean allowSamePattern, long now) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      NeuroAngle.RotationPattern[] patterns = NeuroAngle.RotationPattern.values();
      NeuroAngle.RotationPattern nextPattern = patterns[random.nextInt(patterns.length)];
      if (!allowSamePattern && patterns.length > 1) {
         while (nextPattern == this.activePattern) {
            nextPattern = patterns[random.nextInt(patterns.length)];
         }
      }

      this.activePattern = nextPattern;
      this.yawWavePhase = random.nextDouble(0.0, Math.PI * 2);
      this.pitchWavePhase = random.nextDouble(0.0, Math.PI * 2);
      this.yawWaveSpeed = random.nextDouble(this.activePattern.minYawWaveSpeed, this.activePattern.maxYawWaveSpeed);
      this.pitchWaveSpeed = random.nextDouble(this.activePattern.minPitchWaveSpeed, this.activePattern.maxPitchWaveSpeed);
      this.profileResponse = random.nextDouble(this.activePattern.minResponse, this.activePattern.maxResponse);
      this.profileStiffness = random.nextDouble(this.activePattern.minStiffness, this.activePattern.maxStiffness);
      this.profileYawLead = random.nextDouble(this.activePattern.minYawLead, this.activePattern.maxYawLead);
      this.profilePitchLag = random.nextDouble(this.activePattern.minPitchLag, this.activePattern.maxPitchLag);
      this.profileOvershoot = random.nextDouble(this.activePattern.minOvershoot, this.activePattern.maxOvershoot);
      this.profileMicroScale = random.nextDouble(this.activePattern.minMicroScale, this.activePattern.maxMicroScale);
      this.profileDampingBias = random.nextDouble(this.activePattern.minDampingBias, this.activePattern.maxDampingBias);
      this.profileStepResponseBias = random.nextDouble(this.activePattern.minStepResponseBias, this.activePattern.maxStepResponseBias);
      this.profileStepLimitScale = random.nextDouble(this.activePattern.minStepLimitScale, this.activePattern.maxStepLimitScale);
      this.nextProfileSwitchAtMs = now + random.nextLong(120L, 301L);
   }

   private double clampStep(double value, double previousValue, double maxDelta) {
      double delta = value - previousValue;
      return Math.abs(delta) > maxDelta ? previousValue + Math.signum(delta) * maxDelta : value;
   }

   private double smoothStep(double previousValue, double targetValue, double response) {
      return previousValue + (targetValue - previousValue) * MathHelper.clamp(response, 0.05, 1.0);
   }

   private double clampToRemaining(double step, double remaining) {
      if (Math.abs(remaining) < 1.0E-6) {
         return 0.0;
      } else if (Math.signum(step) != Math.signum(remaining)) {
         return 0.0;
      } else {
         return Math.abs(step) > Math.abs(remaining) ? remaining : step;
      }
   }

   private void pushStepHistory(double[] history, double value) {
      for (int index = history.length - 1; index > 0; index--) {
         history[index] = history[index - 1];
      }

      history[0] = value;
   }

   private double computeTickScale(long now) {
      if (this.lastUpdateAtMs == 0L) {
         this.lastUpdateAtMs = now;
         return 1.0;
      } else {
         double tickScale = MathHelper.clamp((double)(now - this.lastUpdateAtMs) / 16.666666666666668, 0.55, 2.0);
         this.lastUpdateAtMs = now;
         return tickScale;
      }
   }

   private double computeTargetMotion(Angle targetAngle) {
      if (this.lastTargetAngle != null && targetAngle != null) {
         double motion = Math.hypot(
            (double)MathHelper.wrapDegrees(targetAngle.getYaw() - this.lastTargetAngle.getYaw()),
            (double)(targetAngle.getPitch() - this.lastTargetAngle.getPitch())
         );
         this.lastTargetAngle = targetAngle;
         return motion;
      } else {
         this.lastTargetAngle = targetAngle;
         return 0.0;
      }
   }

   private double easeOutCubic(double value) {
      double clamped = MathHelper.clamp(value, 0.0, 1.0);
      double inverse = 1.0 - clamped;
      return 1.0 - inverse * inverse * inverse;
   }

   private void logDatasetSample(
      AuraModule aura,
      Entity entity,
      long timestampMs,
      Angle currentAngle,
      Angle targetAngle,
      double yawDelta,
      double pitchDelta,
      double deltaLength,
      double distance,
      double distanceResponse,
      double targetMotion,
      double targetSpeed,
      double inputYawVelocity,
      double inputPitchVelocity,
      boolean attackWindow,
      double tickScale,
      double smoothness,
      double warmup,
      double angleResponse,
      double sampleShift,
      double previousYawStepSnapshot,
      double previousPitchStepSnapshot,
      double[] yawHistorySnapshot,
      double[] pitchHistorySnapshot,
      double multiPointEnabled,
      double aimOffsetX,
      double aimOffsetY,
      double aimOffsetZ,
      double aimOffsetLength,
      double rawYawStep,
      double rawPitchStep,
      double appliedYawStep,
      double appliedPitchStep
   ) {
      if (aura != null && aura.isNeuroDatasetCaptureEnabled() && entity instanceof LivingEntity livingTarget) {
         NeuroTrainingCollector.getInstance()
            .captureDecision(
               aura,
               livingTarget,
               aura.getNeuroDatasetFormat().getValue(),
               this.activePattern != null ? this.activePattern.name() : "UNKNOWN",
               timestampMs,
               (double)currentAngle.getYaw(),
               (double)currentAngle.getPitch(),
               (double)targetAngle.getYaw(),
               (double)targetAngle.getPitch(),
               yawDelta,
               pitchDelta,
               deltaLength,
               distance,
               distanceResponse,
               targetMotion,
               targetSpeed,
               inputYawVelocity,
               inputPitchVelocity,
               attackWindow,
               tickScale,
               smoothness,
               warmup,
               angleResponse,
               sampleShift,
               previousYawStepSnapshot,
               previousPitchStepSnapshot,
               yawHistorySnapshot,
               pitchHistorySnapshot,
               multiPointEnabled,
               aimOffsetX,
               aimOffsetY,
               aimOffsetZ,
               aimOffsetLength,
               rawYawStep,
               rawPitchStep,
               appliedYawStep,
               appliedPitchStep
            );
      }
   }

   private static enum RotationPattern {
      GLIDE(
         0.05,
         0.11,
         0.04,
         0.09,
         -0.05,
         0.01,
         0.88,
         0.98,
         0.01,
         0.08,
         0.05,
         0.11,
         0.78,
         0.96,
         0.78,
         0.96,
         0.015,
         0.045,
         -0.05,
         -0.01,
         0.84,
         0.94,
         0.88,
         0.84,
         0.74,
         0.72,
         0.72,
         0.85,
         0.15,
         0.78,
         0.68
      ),
      SWAY(
         0.08,
         0.18,
         0.06,
         0.15,
         -0.02,
         0.04,
         0.92,
         1.04,
         0.08,
         0.18,
         0.03,
         0.08,
         0.9,
         1.08,
         1.0,
         1.22,
         -0.01,
         0.015,
         -0.01,
         0.03,
         0.96,
         1.06,
         0.98,
         0.94,
         0.92,
         0.88,
         1.55,
         1.45,
         0.65,
         1.35,
         0.86
      ),
      SHARP(
         0.1,
         0.22,
         0.07,
         0.17,
         0.01,
         0.07,
         1.02,
         1.12,
         0.1,
         0.2,
         0.01,
         0.06,
         1.05,
         1.26,
         0.72,
         0.92,
         -0.04,
         -0.015,
         0.03,
         0.06,
         1.06,
         1.14,
         1.18,
         1.08,
         1.28,
         1.18,
         0.52,
         1.9,
         -0.25,
         0.72,
         1.04
      );

      private final double minYawWaveSpeed;
      private final double maxYawWaveSpeed;
      private final double minPitchWaveSpeed;
      private final double maxPitchWaveSpeed;
      private final double minResponse;
      private final double maxResponse;
      private final double minStiffness;
      private final double maxStiffness;
      private final double minYawLead;
      private final double maxYawLead;
      private final double minPitchLag;
      private final double maxPitchLag;
      private final double minOvershoot;
      private final double maxOvershoot;
      private final double minMicroScale;
      private final double maxMicroScale;
      private final double minDampingBias;
      private final double maxDampingBias;
      private final double minStepResponseBias;
      private final double maxStepResponseBias;
      private final double minStepLimitScale;
      private final double maxStepLimitScale;
      private final double yawStiffnessScale;
      private final double pitchStiffnessScale;
      private final double yawLimitScale;
      private final double pitchLimitScale;
      private final double waveAmplitude;
      private final double waveShape;
      private final double waveOffset;
      private final double microScaleBias;
      private final double nearTargetFriction;

      private RotationPattern(
         double minYawWaveSpeed,
         double maxYawWaveSpeed,
         double minPitchWaveSpeed,
         double maxPitchWaveSpeed,
         double minResponse,
         double maxResponse,
         double minStiffness,
         double maxStiffness,
         double minYawLead,
         double maxYawLead,
         double minPitchLag,
         double maxPitchLag,
         double minOvershoot,
         double maxOvershoot,
         double minMicroScale,
         double maxMicroScale,
         double minDampingBias,
         double maxDampingBias,
         double minStepResponseBias,
         double maxStepResponseBias,
         double minStepLimitScale,
         double maxStepLimitScale,
         double yawStiffnessScale,
         double pitchStiffnessScale,
         double yawLimitScale,
         double pitchLimitScale,
         double waveAmplitude,
         double waveShape,
         double waveOffset,
         double microScaleBias,
         double nearTargetFriction
      ) {
         this.minYawWaveSpeed = minYawWaveSpeed;
         this.maxYawWaveSpeed = maxYawWaveSpeed;
         this.minPitchWaveSpeed = minPitchWaveSpeed;
         this.maxPitchWaveSpeed = maxPitchWaveSpeed;
         this.minResponse = minResponse;
         this.maxResponse = maxResponse;
         this.minStiffness = minStiffness;
         this.maxStiffness = maxStiffness;
         this.minYawLead = minYawLead;
         this.maxYawLead = maxYawLead;
         this.minPitchLag = minPitchLag;
         this.maxPitchLag = maxPitchLag;
         this.minOvershoot = minOvershoot;
         this.maxOvershoot = maxOvershoot;
         this.minMicroScale = minMicroScale;
         this.maxMicroScale = maxMicroScale;
         this.minDampingBias = minDampingBias;
         this.maxDampingBias = maxDampingBias;
         this.minStepResponseBias = minStepResponseBias;
         this.maxStepResponseBias = maxStepResponseBias;
         this.minStepLimitScale = minStepLimitScale;
         this.maxStepLimitScale = maxStepLimitScale;
         this.yawStiffnessScale = yawStiffnessScale;
         this.pitchStiffnessScale = pitchStiffnessScale;
         this.yawLimitScale = yawLimitScale;
         this.pitchLimitScale = pitchLimitScale;
         this.waveAmplitude = waveAmplitude;
         this.waveShape = waveShape;
         this.waveOffset = waveOffset;
         this.microScaleBias = microScaleBias;
         this.nearTargetFriction = nearTargetFriction;
      }
   }
}
