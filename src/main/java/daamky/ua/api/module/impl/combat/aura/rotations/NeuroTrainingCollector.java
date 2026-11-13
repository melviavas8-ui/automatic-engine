package daamky.ua.api.module.impl.combat.aura.rotations;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import daamky.ua.api.module.impl.combat.AuraModule;

public final class NeuroTrainingCollector {
   private static final NeuroTrainingCollector INSTANCE = new NeuroTrainingCollector();
   private static final int MAX_DECISION_AGE_TICKS = 12;
   private static final int HIT_EVALUATION_TICKS = 4;
   private final Map<Integer, NeuroTrainingCollector.DecisionRecord> latestDecisionByTarget = new HashMap<>();
   private final Map<Long, NeuroTrainingCollector.PendingAttack> pendingAttacks = new HashMap<>();
   private long nextDecisionId = 1L;
   private int currentTick;

   private NeuroTrainingCollector() {
   }

   public static NeuroTrainingCollector getInstance() {
      return INSTANCE;
   }

   public synchronized void tick(MinecraftClient client) {
      this.currentTick++;
      Iterator<Entry<Long, NeuroTrainingCollector.PendingAttack>> iterator = this.pendingAttacks.entrySet().iterator();

      while (iterator.hasNext()) {
         NeuroTrainingCollector.PendingAttack pending = iterator.next().getValue();
         LivingEntity target = pending.target;
         int elapsedTicks = Math.max(0, this.currentTick - pending.attackTick);
         if (target == null || !target.isAlive()) {
            this.finalizePending(pending, false, elapsedTicks);
            iterator.remove();
         } else if (this.hasRegisteredHit(target, pending.hurtTimeBeforeAttack)) {
            this.finalizePending(pending, true, elapsedTicks);
            iterator.remove();
         } else if (elapsedTicks >= 4) {
            this.finalizePending(pending, false, elapsedTicks);
            iterator.remove();
         }
      }

      this.latestDecisionByTarget.entrySet().removeIf(entry -> this.currentTick - entry.getValue().tickIndex > 12);
   }

   public synchronized void reset() {
      NeuroDatasetLogger.getInstance().flush();
      this.latestDecisionByTarget.clear();
      this.pendingAttacks.clear();
      this.currentTick = 0;
   }

   public synchronized void captureDecision(
      AuraModule aura,
      LivingEntity target,
      String formatName,
      String pattern,
      long timestampMs,
      double currentYaw,
      double currentPitch,
      double targetYaw,
      double targetPitch,
      double yawDelta,
      double pitchDelta,
      double deltaLength,
      double distance,
      double distanceRatio,
      double targetMotion,
      double targetSpeed,
      double yawVelocity,
      double pitchVelocity,
      boolean attackWindow,
      double tickScale,
      double smoothness,
      double warmup,
      double angleResponse,
      double sampleShift,
      double previousYawStep,
      double previousPitchStep,
      double[] yawHistory,
      double[] pitchHistory,
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
      if (this.isCaptureActive(aura) && target != null) {
         long decisionId = this.nextDecisionId++;
         this.latestDecisionByTarget
            .put(
               target.getId(),
               new NeuroTrainingCollector.DecisionRecord(
                  decisionId,
                  this.currentTick,
                  timestampMs,
                  target.getId(),
                  target.getUuid(),
                  target.getType().toString(),
                  formatName,
                  pattern,
                  currentYaw,
                  currentPitch,
                  targetYaw,
                  targetPitch,
                  yawDelta,
                  pitchDelta,
                  deltaLength,
                  distance,
                  distanceRatio,
                  targetMotion,
                  targetSpeed,
                  yawVelocity,
                  pitchVelocity,
                  attackWindow,
                  tickScale,
                  smoothness,
                  warmup,
                  angleResponse,
                  sampleShift,
                  previousYawStep,
                  previousPitchStep,
                  yawHistory,
                  pitchHistory,
                  multiPointEnabled,
                  aimOffsetX,
                  aimOffsetY,
                  aimOffsetZ,
                  aimOffsetLength,
                  rawYawStep,
                  rawPitchStep,
                  appliedYawStep,
                  appliedPitchStep
               )
            );
      }
   }

   public synchronized void recordAttack(AuraModule aura, LivingEntity target, boolean raytraceSuccess) {
      if (this.isCaptureActive(aura) && target != null) {
         NeuroTrainingCollector.DecisionRecord decision = this.latestDecisionByTarget.get(target.getId());
         if (decision != null && this.currentTick - decision.tickIndex <= 12) {
            if (!this.pendingAttacks.containsKey(decision.decisionId)) {
               NeuroTrainingCollector.PendingAttack pending = new NeuroTrainingCollector.PendingAttack(
                  decision, target, this.currentTick, raytraceSuccess, target.hurtTime
               );
               if (this.hasRegisteredHit(target, pending.hurtTimeBeforeAttack)) {
                  this.finalizePending(pending, true, 0);
               } else {
                  this.pendingAttacks.put(decision.decisionId, pending);
               }
            }
         }
      }
   }

   private boolean isCaptureActive(AuraModule aura) {
      return aura != null && aura.isEnabled() && aura.getMode().is("Neuro") && aura.isNeuroDatasetCaptureEnabled();
   }

   private boolean hasRegisteredHit(LivingEntity target, int hurtTimeBeforeAttack) {
      return target.hurtTime > hurtTimeBeforeAttack || target.hurtTime >= 9;
   }

   private void finalizePending(NeuroTrainingCollector.PendingAttack pending, boolean hitSuccess, int timeToHitTicks) {
      NeuroTrainingCollector.DecisionRecord decision = pending.decision;
      double jerkScore = this.computeJerkScore(decision);
      double signFlipScore = this.computeSignFlipScore(decision);
      double clampGapScore = this.computeClampGapScore(decision);
      double suspicionScore = MathHelper.clamp(jerkScore * 0.55 + signFlipScore * 0.25 + clampGapScore * 0.2, 0.0, 1.0);
      int decisionToAttackTicks = Math.max(0, pending.attackTick - decision.tickIndex);
      double reward = this.computeReward(hitSuccess, pending.raytraceSuccess, timeToHitTicks, decisionToAttackTicks, suspicionScore);
      NeuroDatasetLogger.Sample sample = new NeuroDatasetLogger.Sample(
         decision.decisionId,
         decision.timestampMs,
         decision.targetId,
         decision.targetUuid.toString(),
         decision.targetType,
         decision.pattern,
         decision.currentYaw,
         decision.currentPitch,
         decision.targetYaw,
         decision.targetPitch,
         decision.yawDelta,
         decision.pitchDelta,
         decision.deltaLength,
         decision.distance,
         decision.distanceRatio,
         decision.targetMotion,
         decision.targetSpeed,
         decision.yawVelocity,
         decision.pitchVelocity,
         decision.attackWindow,
         decision.tickScale,
         decision.smoothness,
         decision.warmup,
         decision.angleResponse,
         decision.sampleShift,
         decision.previousYawStep,
         decision.previousPitchStep,
         decision.yawHistory,
         decision.pitchHistory,
         decision.multiPointEnabled,
         decision.aimOffsetX,
         decision.aimOffsetY,
         decision.aimOffsetZ,
         decision.aimOffsetLength,
         decision.rawYawStep,
         decision.rawPitchStep,
         decision.appliedYawStep,
         decision.appliedPitchStep,
         pending.raytraceSuccess,
         hitSuccess,
         decisionToAttackTicks,
         timeToHitTicks,
         jerkScore,
         signFlipScore,
         clampGapScore,
         suspicionScore,
         reward
      );
      NeuroDatasetLogger.getInstance().logSample(sample, decision.formatName);
   }

   private double computeReward(boolean hitSuccess, boolean raytraceSuccess, int timeToHitTicks, int decisionToAttackTicks, double suspicionScore) {
      double reward = hitSuccess ? 1.0 : -0.35;
      reward += raytraceSuccess ? 0.18 : -0.18;
      reward -= (double)Math.min(timeToHitTicks, 4) * 0.06;
      reward -= (double)Math.min(decisionToAttackTicks, 12) * 0.025;
      return reward - suspicionScore * 0.45;
   }

   private double computeJerkScore(NeuroTrainingCollector.DecisionRecord decision) {
      double jerk = Math.abs(decision.appliedYawStep - decision.previousYawStep) + Math.abs(decision.appliedPitchStep - decision.previousPitchStep);
      double normalizer = Math.max(1.0, Math.abs(decision.yawDelta) + Math.abs(decision.pitchDelta));
      return MathHelper.clamp(jerk / normalizer, 0.0, 1.0);
   }

   private double computeSignFlipScore(NeuroTrainingCollector.DecisionRecord decision) {
      int flips = 0;
      if (this.hasSignFlip(decision.previousYawStep, decision.appliedYawStep)) {
         flips++;
      }

      if (this.hasSignFlip(decision.previousPitchStep, decision.appliedPitchStep)) {
         flips++;
      }

      return (double)flips / 2.0;
   }

   private boolean hasSignFlip(double previous, double current) {
      return previous != 0.0 && current != 0.0 && Math.signum(previous) != Math.signum(current);
   }

   private double computeClampGapScore(NeuroTrainingCollector.DecisionRecord decision) {
      double clampGap = Math.abs(decision.rawYawStep - decision.appliedYawStep) + Math.abs(decision.rawPitchStep - decision.appliedPitchStep);
      double normalizer = Math.max(0.25, Math.abs(decision.rawYawStep) + Math.abs(decision.rawPitchStep));
      return MathHelper.clamp(clampGap / normalizer, 0.0, 1.0);
   }

   private static final class DecisionRecord {
      private final long decisionId;
      private final int tickIndex;
      private final long timestampMs;
      private final int targetId;
      private final UUID targetUuid;
      private final String targetType;
      private final String formatName;
      private final String pattern;
      private final double currentYaw;
      private final double currentPitch;
      private final double targetYaw;
      private final double targetPitch;
      private final double yawDelta;
      private final double pitchDelta;
      private final double deltaLength;
      private final double distance;
      private final double distanceRatio;
      private final double targetMotion;
      private final double targetSpeed;
      private final double yawVelocity;
      private final double pitchVelocity;
      private final boolean attackWindow;
      private final double tickScale;
      private final double smoothness;
      private final double warmup;
      private final double angleResponse;
      private final double sampleShift;
      private final double previousYawStep;
      private final double previousPitchStep;
      private final double[] yawHistory;
      private final double[] pitchHistory;
      private final double multiPointEnabled;
      private final double aimOffsetX;
      private final double aimOffsetY;
      private final double aimOffsetZ;
      private final double aimOffsetLength;
      private final double rawYawStep;
      private final double rawPitchStep;
      private final double appliedYawStep;
      private final double appliedPitchStep;

      private DecisionRecord(
         long decisionId,
         int tickIndex,
         long timestampMs,
         int targetId,
         UUID targetUuid,
         String targetType,
         String formatName,
         String pattern,
         double currentYaw,
         double currentPitch,
         double targetYaw,
         double targetPitch,
         double yawDelta,
         double pitchDelta,
         double deltaLength,
         double distance,
         double distanceRatio,
         double targetMotion,
         double targetSpeed,
         double yawVelocity,
         double pitchVelocity,
         boolean attackWindow,
         double tickScale,
         double smoothness,
         double warmup,
         double angleResponse,
         double sampleShift,
         double previousYawStep,
         double previousPitchStep,
         double[] yawHistory,
         double[] pitchHistory,
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
         this.decisionId = decisionId;
         this.tickIndex = tickIndex;
         this.timestampMs = timestampMs;
         this.targetId = targetId;
         this.targetUuid = targetUuid;
         this.targetType = targetType;
         this.formatName = formatName;
         this.pattern = pattern;
         this.currentYaw = currentYaw;
         this.currentPitch = currentPitch;
         this.targetYaw = targetYaw;
         this.targetPitch = targetPitch;
         this.yawDelta = yawDelta;
         this.pitchDelta = pitchDelta;
         this.deltaLength = deltaLength;
         this.distance = distance;
         this.distanceRatio = distanceRatio;
         this.targetMotion = targetMotion;
         this.targetSpeed = targetSpeed;
         this.yawVelocity = yawVelocity;
         this.pitchVelocity = pitchVelocity;
         this.attackWindow = attackWindow;
         this.tickScale = tickScale;
         this.smoothness = smoothness;
         this.warmup = warmup;
         this.angleResponse = angleResponse;
         this.sampleShift = sampleShift;
         this.previousYawStep = previousYawStep;
         this.previousPitchStep = previousPitchStep;
         this.yawHistory = yawHistory == null ? new double[0] : (double[])yawHistory.clone();
         this.pitchHistory = pitchHistory == null ? new double[0] : (double[])pitchHistory.clone();
         this.multiPointEnabled = multiPointEnabled;
         this.aimOffsetX = aimOffsetX;
         this.aimOffsetY = aimOffsetY;
         this.aimOffsetZ = aimOffsetZ;
         this.aimOffsetLength = aimOffsetLength;
         this.rawYawStep = rawYawStep;
         this.rawPitchStep = rawPitchStep;
         this.appliedYawStep = appliedYawStep;
         this.appliedPitchStep = appliedPitchStep;
      }
   }

   private static final class PendingAttack {
      private final NeuroTrainingCollector.DecisionRecord decision;
      private final LivingEntity target;
      private final int attackTick;
      private final boolean raytraceSuccess;
      private final int hurtTimeBeforeAttack;

      private PendingAttack(
         NeuroTrainingCollector.DecisionRecord decision, LivingEntity target, int attackTick, boolean raytraceSuccess, int hurtTimeBeforeAttack
      ) {
         this.decision = decision;
         this.target = target;
         this.attackTick = attackTick;
         this.raytraceSuccess = raytraceSuccess;
         this.hurtTimeBeforeAttack = hurtTimeBeforeAttack;
      }
   }
}
