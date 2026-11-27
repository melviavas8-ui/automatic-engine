package daamky.ua.api.module.impl.combat.aura.target;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import daamky.ua.IMinecraft;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.AutoSwap;
import daamky.ua.api.module.impl.combat.HitBoxModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;

public class MultiPoint implements IMinecraft {
   private static final int DATASET_SWITCH_MIN_HITS = 2;
   private static final int DATASET_SWITCH_MAX_HITS = 3;
   private static final int NEURO_DATASET_SWITCH_MIN_HITS = 1;
   private static final int NEURO_DATASET_SWITCH_MAX_HITS = 2;
   private static final int POINT_SWITCH_MIN_TICKS = 4;
   private static final int POINT_SWITCH_MAX_TICKS = 6;
   private static final int NEURO_POINT_SWITCH_MIN_TICKS = 2;
   private static final int NEURO_POINT_SWITCH_MAX_TICKS = 3;
   private static final int TELEPORT_TOP_CANDIDATES = 4;
   private static final int NEURO_TOP_CANDIDATES = 7;
   private static final double MAX_OFFSET_LENGTH = 0.5;
   private static final double NEURO_MIN_POINT_DISTANCE = 0.025;
   private static final MultiPoint.PointPreset DEFAULT_PRESET = buildRandomDefaultPreset();
   private static final MultiPoint.PointPreset SPOOKY_PRESET = buildSpookyPreset();
   private static final MultiPoint.PointDataset[] DATASETS = new MultiPoint.PointDataset[]{
      new MultiPoint.PointDataset(0.14, 0.94, 0.7, 6, 0.24, true, 0.25, 0.0, 0.12, 0.16, 0.78),
      new MultiPoint.PointDataset(0.24, 0.98, 0.84, 10, 0.32, true, 0.2, 0.01, 0.16, 0.2, 0.66),
      new MultiPoint.PointDataset(0.1, 0.86, 0.54, 8, 0.38, true, 0.28, 0.02, 0.18, 0.24, 0.62),
      new MultiPoint.PointDataset(0.34, 0.99, 0.9, 12, 0.28, true, 0.18, 0.0, 0.14, 0.18, 0.7),
      new MultiPoint.PointDataset(0.42, 0.96, 0.78, 7, 0.18, false, 0.35, 0.03, 0.08, 0.1, 0.74),
      new MultiPoint.PointDataset(0.18, 0.74, 0.58, 9, 0.44, true, 0.22, 0.01, 0.22, 0.26, 0.58),
      new MultiPoint.PointDataset(0.52, 1.0, 0.92, 11, 0.2, true, 0.42, 0.0, 0.1, 0.14, 0.72),
      new MultiPoint.PointDataset(0.06, 0.68, 0.42, 7, 0.34, true, 0.3, 0.04, 0.2, 0.22, 0.6),
      new MultiPoint.PointDataset(0.3, 0.88, 0.64, 13, 0.46, true, 0.16, 0.02, 0.26, 0.28, 0.52),
      new MultiPoint.PointDataset(0.6, 0.99, 0.86, 6, 0.4, true, 0.24, 0.0, 0.18, 0.2, 0.64),
      new MultiPoint.PointDataset(0.12, 0.98, 0.76, 15, 0.12, false, 0.12, 0.06, 0.14, 0.12, 0.8),
      new MultiPoint.PointDataset(0.2, 0.9, 0.7, 5, 0.5, true, 0.26, 0.0, 0.3, 0.3, 0.48),
      new MultiPoint.PointDataset(0.36, 0.82, 0.72, 8, 0.26, true, 0.38, 0.02, 0.12, 0.18, 0.68),
      new MultiPoint.PointDataset(0.08, 0.52, 0.36, 6, 0.42, true, 0.32, 0.03, 0.24, 0.24, 0.56),
      new MultiPoint.PointDataset(0.7, 1.0, 0.94, 9, 0.3, true, 0.34, 0.01, 0.16, 0.16, 0.76),
      new MultiPoint.PointDataset(0.16, 0.92, 0.62, 12, 0.56, true, 0.18, 0.01, 0.34, 0.32, 0.44),
      new MultiPoint.PointDataset(0.28, 0.94, 0.82, 10, 0.08, false, 0.48, 0.08, 0.06, 0.08, 0.86),
      new MultiPoint.PointDataset(0.04, 0.98, 0.5, 16, 0.36, true, 0.1, 0.03, 0.28, 0.26, 0.5),
      new MultiPoint.PointDataset(0.48, 0.98, 0.88, 14, 0.52, true, 0.2, 0.0, 0.32, 0.34, 0.46),
      new MultiPoint.PointDataset(0.22, 0.78, 0.66, 4, 0.16, false, 0.55, 0.1, 0.04, 0.06, 0.9),
      new MultiPoint.PointDataset(0.1, 1.0, 0.8, 18, 0.48, true, 0.14, 0.02, 0.36, 0.36, 0.42),
      new MultiPoint.PointDataset(0.4, 0.9, 0.56, 9, 0.6, true, 0.24, 0.0, 0.4, 0.38, 0.4),
      new MultiPoint.PointDataset(0.58, 0.98, 0.74, 7, 0.24, true, 0.3, 0.04, 0.18, 0.18, 0.66)
   };
   final Random random = new SecureRandom();
   Vec3 offset = Vec3.ZERO;
   int datasetIndex = 0;
   int lockedCandidateIndex = -1;
   Vec3 lockedPointOffset = null;
   Vec3 smoothedPoint = null;
   long lastSmoothedPointTick = -1L;
   int lockedDatasetIndex = -1;
   int lockedEntityId = -1;
   long nextPointSwitchTick = -1L;
   int lastAttackCount = 0;
   int nextDatasetSwitchHitCount = -1;

   public MultiPoint() {
   }

   private static MultiPoint.PointPreset buildSpookyPreset() {
      List<MultiPoint.AimPoint> points = new ArrayList<>();
      double[] centerHeights = new double[]{0.36, 0.44, 0.52, 0.6, 0.68, 0.72};

      for (double y : centerHeights) {
         points.add(new MultiPoint.AimPoint(0.0, y, 0.0));
      }

      for (double y : new double[]{0.42, 0.54, 0.66}) {
         addCardinalPoints(points, y, 0.12);
         addCardinalPoints(points, y, 0.2);
         addDiagonalPoints(points, y, 0.14);
      }

      for (double y : new double[]{0.48, 0.6}) {
         addCardinalPoints(points, y, 0.16);
         addCardinalPoints(points, y, 0.28);
         addCardinalPoints(points, y, 0.38);
         addDiagonalPoints(points, y, 0.18);
         addDiagonalPoints(points, y, 0.28);
      }

      for (double y : new double[]{0.36, 0.52}) {
         addCardinalPoints(points, y, 0.18);
         addCardinalPoints(points, y, 0.32);
         addDiagonalPoints(points, y, 0.22);
         addDiagonalPoints(points, y, 0.32);
      }

      return new MultiPoint.PointPreset(points.toArray(MultiPoint.AimPoint[]::new));
   }

   private static MultiPoint.PointPreset buildRandomDefaultPreset() {
      List<MultiPoint.AimPoint> points = new ArrayList<>();

      for (double y : new double[]{0.38, 0.5, 0.62, 0.74, 0.84}) {
         points.add(new MultiPoint.AimPoint(0.0, y, 0.0));
         addCardinalPoints(points, y, 0.45);
         addDiagonalPoints(points, y, 0.35);
      }

      return new MultiPoint.PointPreset(points.toArray(MultiPoint.AimPoint[]::new));
   }

   private static void addCardinalPoints(List<MultiPoint.AimPoint> p, double y, double r) {
      p.add(new MultiPoint.AimPoint(r, y, 0.0));
      p.add(new MultiPoint.AimPoint(-r, y, 0.0));
      p.add(new MultiPoint.AimPoint(0.0, y, r));
      p.add(new MultiPoint.AimPoint(0.0, y, -r));
   }

   private static void addDiagonalPoints(List<MultiPoint.AimPoint> p, double y, double r) {
      p.add(new MultiPoint.AimPoint(r, y, r));
      p.add(new MultiPoint.AimPoint(r, y, -r));
      p.add(new MultiPoint.AimPoint(-r, y, r));
      p.add(new MultiPoint.AimPoint(-r, y, -r));
   }

   public Tuple<Vec3, AABB> computeVector(LivingEntity entity, float maxDistance, Angle initialAngle, Vec3 velocity, boolean ignoreWalls) {
      if (this.isFunTimeMode()) {
         Tuple<List<Vec3>, AABB> cp = this.generateFunTimeCandidatePoints(entity, maxDistance, ignoreWalls);
         Vec3 best = this.findTeleportVector(entity, (List<Vec3>)cp.getA(), (AABB)cp.getB(), initialAngle, this.currentDataset());
         this.offset = Vec3.ZERO;
         Vec3 fallback = entity.getEyePosition();
         return new Tuple((best == null ? fallback : best).add(this.offset), (AABB)cp.getB());
      } else {
         MultiPoint.PointDataset dataset = this.currentDataset();
         Tuple<List<Vec3>, AABB> cp = this.isNeuroMode()
            ? this.generateNeuroCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset(), dataset)
            : this.generatePresetCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset());
         Vec3 best = this.findTeleportVector(entity, (List<Vec3>)cp.getA(), (AABB)cp.getB(), initialAngle, dataset);
         Vec3 fallback = entity.getEyePosition();
         return new Tuple(best == null ? fallback : best, (AABB)cp.getB());
      }
   }

   public Tuple<List<Vec3>, AABB> generateCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      if (this.isFunTimeMode()) {
         return this.generateFunTimeCandidatePoints(entity, maxDistance, ignoreWalls);
      } else {
         return this.isNeuroMode()
            ? this.generateNeuroCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset(), this.currentDataset())
            : this.generatePresetCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset());
      }
   }

   public boolean hasValidPoint(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      if (mc.player == null) {
         return false;
      } else if (this.isFunTimeMode()) {
         return !((List)this.generateFunTimeCandidatePoints(entity, maxDistance, ignoreWalls).getA()).isEmpty();
      } else {
         return this.isNeuroMode()
            ? !((List)this.generateNeuroCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset(), this.currentDataset()).getA()).isEmpty()
            : !((List)this.generatePresetCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset()).getA()).isEmpty();
      }
   }

   public static Vec3 distance(Entity entity, float min, float max) {
      double dist = mc.player.getPosition(1.0F).distanceTo(entity.getPosition(1.0F));
      double normalized = Mth.clamp((dist - (double)min) / (double)(max - min), 0.0, 1.0);
      double targetY = entity.getY() + (double)entity.getBbHeight() * normalized;
      return new Vec3(entity.getX(), targetY, entity.getZ());
   }

   private Tuple<List<Vec3>, AABB> generateFunTimeCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      AABB box = this.targetBox(entity);
      double rawStep = box.getYsize() / 10.0;
      double stepY = rawStep <= 0.0 ? 0.1 : rawStep;
      List<Vec3> list = new ArrayList<>();
      Vec3 center = box.getCenter();
      if (!this.isEntityInReach(mc.player.getEyePosition(), box, maxDistance)) {
         return new Tuple(list, box);
      } else {
         for (double y = box.minY; y <= box.maxY + 1.0E-4; y += stepY) {
            Vec3 point = new Vec3(center.x, y, center.z);
            if (this.hasLineOfSight(mc.player.getEyePosition(), point, ignoreWalls)) {
               list.add(point);
            }
         }

         return new Tuple(list, box);
      }
   }

   private Tuple<List<Vec3>, AABB> generatePresetCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls, MultiPoint.PointPreset preset) {
      AABB box = this.targetBox(entity);
      Vec3 center = box.getCenter();
      double height = Math.max(box.getYsize(), 0.1);
      double halfX = box.getXsize() * 0.5;
      double halfZ = box.getZsize() * 0.5;
      List<Vec3> list = new ArrayList<>();
      Vec3 eye = mc.player.getEyePosition();
      if (!this.isEntityInReach(eye, box, maxDistance)) {
         return new Tuple(list, box);
      } else {
         for (MultiPoint.AimPoint ap : preset.points) {
            Vec3 point = new Vec3(center.x + halfX * ap.x, box.minY + height * ap.y, center.z + halfZ * ap.z);
            this.addValidPoint(list, eye, point, ignoreWalls);
         }

         return new Tuple(list, box);
      }
   }

   private Tuple<List<Vec3>, AABB> generateNeuroCandidatePoints(
      LivingEntity entity, float maxDistance, boolean ignoreWalls, MultiPoint.PointPreset preset, MultiPoint.PointDataset dataset
   ) {
      Tuple<List<Vec3>, AABB> datasetPoints = this.generateDatasetCandidatePoints(entity, maxDistance, ignoreWalls, dataset);
      Tuple<List<Vec3>, AABB> presetPoints = this.generatePresetCandidatePoints(entity, maxDistance, ignoreWalls, preset);
      List<Vec3> merged = new ArrayList<>(((List)datasetPoints.getA()).size() + ((List)presetPoints.getA()).size());

      for (Vec3 point : (List<Vec3>)datasetPoints.getA()) {
         this.addUniquePoint(merged, point, 0.025);
      }

      for (Vec3 point : (List<Vec3>)presetPoints.getA()) {
         this.addUniquePoint(merged, point, 0.025);
      }

      return new Tuple(merged, (AABB)datasetPoints.getB());
   }

   private Tuple<List<Vec3>, AABB> generateDatasetCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls, MultiPoint.PointDataset dataset) {
      AABB box = this.targetBox(entity);
      Vec3 center = box.getCenter();
      double height = Math.max(box.getYsize(), 0.1);
      double minY = box.minY + height * dataset.minHeight;
      double maxY = box.minY + height * dataset.maxHeight;
      double stepY = Math.max((maxY - minY) / (double)Math.max(dataset.verticalSamples, 1), 0.05);
      double lr = Math.min(box.getXsize(), box.getZsize()) * dataset.lateralRadius;
      List<Vec3> list = new ArrayList<>();
      Vec3 eye = mc.player.getEyePosition();
      if (!this.isEntityInReach(eye, box, maxDistance)) {
         return new Tuple(list, box);
      } else {
         for (double y = minY; y <= maxY + 1.0E-4; y += stepY) {
            this.addValidPoint(list, eye, new Vec3(center.x, y, center.z), ignoreWalls);
            if (!(lr <= 0.0)) {
               this.addValidPoint(list, eye, new Vec3(center.x + lr, y, center.z), ignoreWalls);
               this.addValidPoint(list, eye, new Vec3(center.x - lr, y, center.z), ignoreWalls);
               this.addValidPoint(list, eye, new Vec3(center.x, y, center.z + lr), ignoreWalls);
               this.addValidPoint(list, eye, new Vec3(center.x, y, center.z - lr), ignoreWalls);
               if (dataset.diagonalPoints) {
                  double d = lr * 0.70710678118;
                  this.addValidPoint(list, eye, new Vec3(center.x + d, y, center.z + d), ignoreWalls);
                  this.addValidPoint(list, eye, new Vec3(center.x + d, y, center.z - d), ignoreWalls);
                  this.addValidPoint(list, eye, new Vec3(center.x - d, y, center.z + d), ignoreWalls);
                  this.addValidPoint(list, eye, new Vec3(center.x - d, y, center.z - d), ignoreWalls);
               }
            }
         }

         return new Tuple(list, box);
      }
   }

   private Vec3 findTeleportVector(LivingEntity entity, List<Vec3> candidates, AABB box, Angle initialAngle, MultiPoint.PointDataset dataset) {
      if (candidates.isEmpty()) {
         this.resetLockedPoint();
         return null;
      } else {
         long gameTick = mc.level == null ? 0L : mc.level.getGameTime();
         Vec3 boxCenter = box.getCenter();
         int currentIndex = this.findMatchingCandidateIndex(candidates, boxCenter, this.lockedPointOffset);
         boolean entityChanged = this.lockedEntityId != entity.getId();
         boolean shouldSwitch = this.nextPointSwitchTick < 0L || gameTick >= this.nextPointSwitchTick;
         if (!entityChanged && this.lockedDatasetIndex == this.datasetIndex && currentIndex >= 0 && !shouldSwitch) {
            this.lockedCandidateIndex = currentIndex;
         } else {
            Vec3 previousPoint = currentIndex < 0 ? null : candidates.get(currentIndex);
            this.lockedCandidateIndex = this.findTeleportVectorIndex(candidates, box, initialAngle, dataset, currentIndex, previousPoint);
            this.lockedPointOffset = candidates.get(this.lockedCandidateIndex).subtract(boxCenter);
            this.lockedDatasetIndex = this.datasetIndex;
            this.lockedEntityId = entity.getId();
            this.nextPointSwitchTick = gameTick + (long)this.randomPointHoldTicks();
            if (entityChanged) {
               this.smoothedPoint = null;
               this.lastSmoothedPointTick = -1L;
            }
         }

         return candidates.get(this.lockedCandidateIndex);
      }
   }

   private int findTeleportVectorIndex(
      List<Vec3> candidates, AABB box, Angle initialAngle, MultiPoint.PointDataset dataset, int previousIndex, Vec3 previousPoint
   ) {
      List<MultiPoint.PointScore> scores = new ArrayList<>();
      Vec3 eye = mc.player.getEyePosition();

      for (int i = 0; i < candidates.size(); i++) {
         scores.add(new MultiPoint.PointScore(i, this.calculatePointScore(eye, candidates.get(i), box, initialAngle, dataset)));
      }

      scores.sort(Comparator.comparingDouble(MultiPoint.PointScore::score));
      int limit = this.isNeuroMode() ? Math.min(7, scores.size()) : scores.size();
      if (limit <= 1) {
         return scores.get(0).index();
      } else {
         List<Integer> selectable = new ArrayList<>();

         for (int i = 0; i < limit; i++) {
            int idx = scores.get(i).index();
            if (idx != previousIndex && (previousPoint == null || !(previousPoint.distanceTo(candidates.get(idx)) < 0.08))) {
               selectable.add(idx);
            }
         }

         if (selectable.isEmpty()) {
            for (int ix = 0; ix < limit; ix++) {
               int idx = scores.get(ix).index();
               if (idx != previousIndex) {
                  selectable.add(idx);
               }
            }
         }

         if (selectable.isEmpty()) {
            return scores.get(0).index();
         } else {
            if (!this.isNeuroMode() && previousPoint != null && selectable.size() > 1) {
               double furthestDistance = 0.0;

               for (int idx : selectable) {
                  furthestDistance = Math.max(furthestDistance, previousPoint.distanceTo(candidates.get(idx)));
               }

               double minimumVisibleMove = furthestDistance * 0.65;
               selectable.removeIf(idxx -> previousPoint.distanceTo(candidates.get(idxx)) < minimumVisibleMove);
            }

            return this.isNeuroMode() ? this.weightedCandidatePick(selectable, scores) : selectable.get(this.random.nextInt(selectable.size()));
         }
      }
   }

   private int findMatchingCandidateIndex(List<Vec3> candidates, Vec3 boxCenter, Vec3 pointOffset) {
      if (pointOffset == null) {
         return -1;
      } else {
         for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).subtract(boxCenter).distanceToSqr(pointOffset) <= 1.0E-6) {
               return i;
            }
         }

         return -1;
      }
   }

   private Vec3 smoothPoint(Vec3 targetPoint, long gameTick) {
      if (this.smoothedPoint == null) {
         this.smoothedPoint = targetPoint;
      } else if (gameTick > this.lastSmoothedPointTick) {
         long elapsedTicks = this.lastSmoothedPointTick < 0L ? 1L : gameTick - this.lastSmoothedPointTick;
         double alpha = 1.0 - Math.pow(0.55, (double)elapsedTicks);
         this.smoothedPoint = this.smoothedPoint.lerp(targetPoint, alpha);
      }

      this.lastSmoothedPointTick = gameTick;
      return this.smoothedPoint;
   }

   private double calculatePointScore(Vec3 eye, Vec3 point, AABB box, Angle initialAngle, MultiPoint.PointDataset dataset) {
      double rotDiff = this.calculateRotationDifference(eye, point, initialAngle);
      double height = Math.max(box.getYsize(), 0.1);
      double heightFactor = (point.y - box.minY) / height;
      double heightPenalty = Math.abs(heightFactor - dataset.preferredHeight) * dataset.heightBias;
      double lateral = Math.hypot(point.x - box.getCenter().x, point.z - box.getCenter().z);
      double lateralPenalty = lateral * dataset.lateralBias;
      double noise = this.random.nextDouble() * dataset.randomScore;
      return rotDiff + heightPenalty + lateralPenalty + noise;
   }

   private double calculateRotationDifference(Vec3 eye, Vec3 point, Angle initialAngle) {
      Angle target = MathAngle.fromVec3d(point.subtract(eye));
      Angle delta = MathAngle.calculateDelta(initialAngle, target);
      return Math.hypot((double)delta.getYaw(), (double)delta.getPitch());
   }

   private void addValidPoint(List<Vec3> list, Vec3 eye, Vec3 point, boolean ignoreWalls) {
      if (this.hasLineOfSight(eye, point, ignoreWalls)) {
         list.add(point);
      }
   }

   private void addUniquePoint(List<Vec3> list, Vec3 point, double minDistance) {
      for (Vec3 existing : list) {
         if (existing.distanceToSqr(point) <= minDistance * minDistance) {
            return;
         }
      }

      list.add(point);
   }

   private boolean isValidPoint(Vec3 eye, Vec3 point, float maxDistance, boolean ignoreWalls) {
      return eye.distanceToSqr(point) > (double)maxDistance * (double)maxDistance ? false : this.hasLineOfSight(eye, point, ignoreWalls);
   }

   private boolean isEntityInReach(Vec3 eye, AABB box, float maxDistance) {
      Vec3 closest = new Vec3(
         Mth.clamp(eye.x, box.minX, box.maxX), Mth.clamp(eye.y, box.minY, box.maxY), Mth.clamp(eye.z, box.minZ, box.maxZ)
      );
      return eye.distanceToSqr(closest) <= (double)maxDistance * (double)maxDistance;
   }

   private AABB targetBox(LivingEntity entity) {
      AutoSwap autoSwap = AutoSwap.getInstance();
      return autoSwap != null && autoSwap.shouldUseVanillaAuraHitbox() ? HitBoxModule.withoutExpansion(entity::getBoundingBox) : entity.getBoundingBox();
   }

   private boolean hasLineOfSight(Vec3 eye, Vec3 point, boolean ignoreWalls) {
      if (ignoreWalls) {
         return true;
      } else {
         HitResult result = RaycastAngle.raycast(eye, point, Block.COLLIDER);
         return result == null || result.getType() != Type.BLOCK;
      }
   }

   private Vec3 clampOffset(Vec3 value) {
      double len = value.length();
      return !(len <= 0.5) && len != 0.0 ? value.scale(0.5 / len) : value;
   }

   private boolean isFunTimeMode() {
      AuraModule aura = AuraModule.getInstance();
      return aura != null && aura.isEnabled() && aura.getMode().is("FunTime");
   }

   private boolean isNeuroMode() {
      AuraModule aura = AuraModule.getInstance();
      return aura != null && aura.isEnabled() && aura.getMode().is("Neuro");
   }

   private MultiPoint.PointPreset currentPreset() {
      AuraModule aura = AuraModule.getInstance();
      return aura != null && aura.isEnabled() && aura.getMode().is("SpookyTime") ? SPOOKY_PRESET : DEFAULT_PRESET;
   }

   private MultiPoint.PointDataset currentDataset() {
      AuraModule aura = AuraModule.getInstance();
      StrikeManager handler = aura == null ? null : aura.getAttackHandlerRaw();
      int attackCount = handler == null ? this.lastAttackCount : handler.getCount();
      if (this.nextDatasetSwitchHitCount >= 0 && attackCount >= this.lastAttackCount) {
         this.lastAttackCount = attackCount;
         if (attackCount < this.nextDatasetSwitchHitCount) {
            return DATASETS[this.datasetIndex];
         } else {
            int step = DATASETS.length > 1 ? 1 + this.random.nextInt(DATASETS.length - 1) : 1;
            this.datasetIndex = (this.datasetIndex + step) % DATASETS.length;
            this.resetLockedPoint();
            this.nextDatasetSwitchHitCount = attackCount + this.randomHitsUntilSwitch();
            return DATASETS[this.datasetIndex];
         }
      } else {
         this.lastAttackCount = attackCount;
         this.nextDatasetSwitchHitCount = attackCount + this.randomHitsUntilSwitch();
         return DATASETS[this.datasetIndex];
      }
   }

   private void resetLockedPoint() {
      this.lockedCandidateIndex = -1;
      this.lockedPointOffset = null;
      this.smoothedPoint = null;
      this.lastSmoothedPointTick = -1L;
      this.lockedDatasetIndex = -1;
      this.lockedEntityId = -1;
      this.nextPointSwitchTick = -1L;
   }

   private int randomPointHoldTicks() {
      return this.isNeuroMode() ? 2 + this.random.nextInt(2) : 4 + this.random.nextInt(3);
   }

   private int randomHitsUntilSwitch() {
      return this.isNeuroMode() ? 1 + this.random.nextInt(2) : 2 + this.random.nextInt(2);
   }

   private int weightedCandidatePick(List<Integer> selectable, List<MultiPoint.PointScore> scores) {
      int totalWeight = 0;
      int[] weights = new int[selectable.size()];

      for (int i = 0; i < selectable.size(); i++) {
         int index = selectable.get(i);
         int rank = this.findRank(scores, index);
         weights[i] = Math.max(1, 8 - rank);
         totalWeight += weights[i];
      }

      int roll = this.random.nextInt(totalWeight);
      int cursor = 0;

      for (int i = 0; i < selectable.size(); i++) {
         cursor += weights[i];
         if (roll < cursor) {
            return selectable.get(i);
         }
      }

      return selectable.get(0);
   }

   private int findRank(List<MultiPoint.PointScore> scores, int candidateIndex) {
      for (int i = 0; i < scores.size(); i++) {
         if (scores.get(i).index() == candidateIndex) {
            return i + 1;
         }
      }

      return scores.size();
   }

   private static class AimPoint {
      final double x;
      final double y;
      final double z;

      AimPoint(double x, double y, double z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }

   private static class PointDataset {
      final double minHeight;
      final double maxHeight;
      final double preferredHeight;
      final int verticalSamples;
      final double lateralRadius;
      final boolean diagonalPoints;
      final double heightBias;
      final double lateralBias;
      final double randomScore;
      final double offsetScale;
      final double offsetRetention;

      PointDataset(
         double minHeight,
         double maxHeight,
         double preferredHeight,
         int verticalSamples,
         double lateralRadius,
         boolean diagonalPoints,
         double heightBias,
         double lateralBias,
         double randomScore,
         double offsetScale,
         double offsetRetention
      ) {
         this.minHeight = minHeight;
         this.maxHeight = maxHeight;
         this.preferredHeight = preferredHeight;
         this.verticalSamples = verticalSamples;
         this.lateralRadius = lateralRadius;
         this.diagonalPoints = diagonalPoints;
         this.heightBias = heightBias;
         this.lateralBias = lateralBias;
         this.randomScore = randomScore;
         this.offsetScale = offsetScale;
         this.offsetRetention = offsetRetention;
      }
   }

   private static class PointPreset {
      final MultiPoint.AimPoint[] points;

      PointPreset(MultiPoint.AimPoint... points) {
         this.points = points;
      }
   }

   private static record PointScore(int index, double score) {
   }
}
