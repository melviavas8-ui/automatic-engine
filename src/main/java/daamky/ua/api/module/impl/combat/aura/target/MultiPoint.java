package daamky.ua.api.module.impl.combat.aura.target;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.ShapeType;
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
   Vec3d offset = Vec3d.ZERO;
   int datasetIndex = 0;
   int lockedCandidateIndex = -1;
   Vec3d lockedPointOffset = null;
   Vec3d smoothedPoint = null;
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

   public Pair<Vec3d, Box> computeVector(LivingEntity entity, float maxDistance, Angle initialAngle, Vec3d velocity, boolean ignoreWalls) {
      if (this.isFunTimeMode()) {
         Pair<List<Vec3d>, Box> cp = this.generateFunTimeCandidatePoints(entity, maxDistance, ignoreWalls);
         Vec3d best = this.findTeleportVector(entity, (List<Vec3d>)cp.getLeft(), (Box)cp.getRight(), initialAngle, this.currentDataset());
         this.offset = Vec3d.ZERO;
         Vec3d fallback = entity.getEyePos();
         return new Pair((best == null ? fallback : best).add(this.offset), (Box)cp.getRight());
      } else {
         MultiPoint.PointDataset dataset = this.currentDataset();
         Pair<List<Vec3d>, Box> cp = this.isNeuroMode()
            ? this.generateNeuroCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset(), dataset)
            : this.generatePresetCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset());
         Vec3d best = this.findTeleportVector(entity, (List<Vec3d>)cp.getLeft(), (Box)cp.getRight(), initialAngle, dataset);
         Vec3d fallback = entity.getEyePos();
         return new Pair(best == null ? fallback : best, (Box)cp.getRight());
      }
   }

   public Pair<List<Vec3d>, Box> generateCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
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
         return !((List)this.generateFunTimeCandidatePoints(entity, maxDistance, ignoreWalls).getLeft()).isEmpty();
      } else {
         return this.isNeuroMode()
            ? !((List)this.generateNeuroCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset(), this.currentDataset()).getLeft()).isEmpty()
            : !((List)this.generatePresetCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset()).getLeft()).isEmpty();
      }
   }

   public static Vec3d distance(Entity entity, float min, float max) {
      double dist = mc.player.getLerpedPos(1.0F).distanceTo(entity.getLerpedPos(1.0F));
      double normalized = MathHelper.clamp((dist - (double)min) / (double)(max - min), 0.0, 1.0);
      double targetY = entity.getY() + (double)entity.getHeight() * normalized;
      return new Vec3d(entity.getX(), targetY, entity.getZ());
   }

   private Pair<List<Vec3d>, Box> generateFunTimeCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Box box = this.targetBox(entity);
      double rawStep = box.getLengthY() / 10.0;
      double stepY = rawStep <= 0.0 ? 0.1 : rawStep;
      List<Vec3d> list = new ArrayList<>();
      Vec3d center = box.getCenter();
      if (!this.isEntityInReach(mc.player.getEyePos(), box, maxDistance)) {
         return new Pair(list, box);
      } else {
         for (double y = box.minY; y <= box.maxY + 1.0E-4; y += stepY) {
            Vec3d point = new Vec3d(center.x, y, center.z);
            if (this.hasLineOfSight(mc.player.getEyePos(), point, ignoreWalls)) {
               list.add(point);
            }
         }

         return new Pair(list, box);
      }
   }

   private Pair<List<Vec3d>, Box> generatePresetCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls, MultiPoint.PointPreset preset) {
      Box box = this.targetBox(entity);
      Vec3d center = box.getCenter();
      double height = Math.max(box.getLengthY(), 0.1);
      double halfX = box.getLengthX() * 0.5;
      double halfZ = box.getLengthZ() * 0.5;
      List<Vec3d> list = new ArrayList<>();
      Vec3d eye = mc.player.getEyePos();
      if (!this.isEntityInReach(eye, box, maxDistance)) {
         return new Pair(list, box);
      } else {
         for (MultiPoint.AimPoint ap : preset.points) {
            Vec3d point = new Vec3d(center.x + halfX * ap.x, box.minY + height * ap.y, center.z + halfZ * ap.z);
            this.addValidPoint(list, eye, point, ignoreWalls);
         }

         return new Pair(list, box);
      }
   }

   private Pair<List<Vec3d>, Box> generateNeuroCandidatePoints(
      LivingEntity entity, float maxDistance, boolean ignoreWalls, MultiPoint.PointPreset preset, MultiPoint.PointDataset dataset
   ) {
      Pair<List<Vec3d>, Box> datasetPoints = this.generateDatasetCandidatePoints(entity, maxDistance, ignoreWalls, dataset);
      Pair<List<Vec3d>, Box> presetPoints = this.generatePresetCandidatePoints(entity, maxDistance, ignoreWalls, preset);
      List<Vec3d> merged = new ArrayList<>(((List)datasetPoints.getLeft()).size() + ((List)presetPoints.getLeft()).size());

      for (Vec3d point : (List<Vec3d>)datasetPoints.getLeft()) {
         this.addUniquePoint(merged, point, 0.025);
      }

      for (Vec3d point : (List<Vec3d>)presetPoints.getLeft()) {
         this.addUniquePoint(merged, point, 0.025);
      }

      return new Pair(merged, (Box)datasetPoints.getRight());
   }

   private Pair<List<Vec3d>, Box> generateDatasetCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls, MultiPoint.PointDataset dataset) {
      Box box = this.targetBox(entity);
      Vec3d center = box.getCenter();
      double height = Math.max(box.getLengthY(), 0.1);
      double minY = box.minY + height * dataset.minHeight;
      double maxY = box.minY + height * dataset.maxHeight;
      double stepY = Math.max((maxY - minY) / (double)Math.max(dataset.verticalSamples, 1), 0.05);
      double lr = Math.min(box.getLengthX(), box.getLengthZ()) * dataset.lateralRadius;
      List<Vec3d> list = new ArrayList<>();
      Vec3d eye = mc.player.getEyePos();
      if (!this.isEntityInReach(eye, box, maxDistance)) {
         return new Pair(list, box);
      } else {
         for (double y = minY; y <= maxY + 1.0E-4; y += stepY) {
            this.addValidPoint(list, eye, new Vec3d(center.x, y, center.z), ignoreWalls);
            if (!(lr <= 0.0)) {
               this.addValidPoint(list, eye, new Vec3d(center.x + lr, y, center.z), ignoreWalls);
               this.addValidPoint(list, eye, new Vec3d(center.x - lr, y, center.z), ignoreWalls);
               this.addValidPoint(list, eye, new Vec3d(center.x, y, center.z + lr), ignoreWalls);
               this.addValidPoint(list, eye, new Vec3d(center.x, y, center.z - lr), ignoreWalls);
               if (dataset.diagonalPoints) {
                  double d = lr * 0.70710678118;
                  this.addValidPoint(list, eye, new Vec3d(center.x + d, y, center.z + d), ignoreWalls);
                  this.addValidPoint(list, eye, new Vec3d(center.x + d, y, center.z - d), ignoreWalls);
                  this.addValidPoint(list, eye, new Vec3d(center.x - d, y, center.z + d), ignoreWalls);
                  this.addValidPoint(list, eye, new Vec3d(center.x - d, y, center.z - d), ignoreWalls);
               }
            }
         }

         return new Pair(list, box);
      }
   }

   private Vec3d findTeleportVector(LivingEntity entity, List<Vec3d> candidates, Box box, Angle initialAngle, MultiPoint.PointDataset dataset) {
      if (candidates.isEmpty()) {
         this.resetLockedPoint();
         return null;
      } else {
         long gameTick = mc.world == null ? 0L : mc.world.getTime();
         Vec3d boxCenter = box.getCenter();
         int currentIndex = this.findMatchingCandidateIndex(candidates, boxCenter, this.lockedPointOffset);
         boolean entityChanged = this.lockedEntityId != entity.getId();
         boolean shouldSwitch = this.nextPointSwitchTick < 0L || gameTick >= this.nextPointSwitchTick;
         if (!entityChanged && this.lockedDatasetIndex == this.datasetIndex && currentIndex >= 0 && !shouldSwitch) {
            this.lockedCandidateIndex = currentIndex;
         } else {
            Vec3d previousPoint = currentIndex < 0 ? null : candidates.get(currentIndex);
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
      List<Vec3d> candidates, Box box, Angle initialAngle, MultiPoint.PointDataset dataset, int previousIndex, Vec3d previousPoint
   ) {
      List<MultiPoint.PointScore> scores = new ArrayList<>();
      Vec3d eye = mc.player.getEyePos();

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

   private int findMatchingCandidateIndex(List<Vec3d> candidates, Vec3d boxCenter, Vec3d pointOffset) {
      if (pointOffset == null) {
         return -1;
      } else {
         for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).subtract(boxCenter).squaredDistanceTo(pointOffset) <= 1.0E-6) {
               return i;
            }
         }

         return -1;
      }
   }

   private Vec3d smoothPoint(Vec3d targetPoint, long gameTick) {
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

   private double calculatePointScore(Vec3d eye, Vec3d point, Box box, Angle initialAngle, MultiPoint.PointDataset dataset) {
      double rotDiff = this.calculateRotationDifference(eye, point, initialAngle);
      double height = Math.max(box.getLengthY(), 0.1);
      double heightFactor = (point.y - box.minY) / height;
      double heightPenalty = Math.abs(heightFactor - dataset.preferredHeight) * dataset.heightBias;
      double lateral = Math.hypot(point.x - box.getCenter().x, point.z - box.getCenter().z);
      double lateralPenalty = lateral * dataset.lateralBias;
      double noise = this.random.nextDouble() * dataset.randomScore;
      return rotDiff + heightPenalty + lateralPenalty + noise;
   }

   private double calculateRotationDifference(Vec3d eye, Vec3d point, Angle initialAngle) {
      Angle target = MathAngle.fromVec3d(point.subtract(eye));
      Angle delta = MathAngle.calculateDelta(initialAngle, target);
      return Math.hypot((double)delta.getYaw(), (double)delta.getPitch());
   }

   private void addValidPoint(List<Vec3d> list, Vec3d eye, Vec3d point, boolean ignoreWalls) {
      if (this.hasLineOfSight(eye, point, ignoreWalls)) {
         list.add(point);
      }
   }

   private void addUniquePoint(List<Vec3d> list, Vec3d point, double minDistance) {
      for (Vec3d existing : list) {
         if (existing.squaredDistanceTo(point) <= minDistance * minDistance) {
            return;
         }
      }

      list.add(point);
   }

   private boolean isValidPoint(Vec3d eye, Vec3d point, float maxDistance, boolean ignoreWalls) {
      return eye.squaredDistanceTo(point) > (double)maxDistance * (double)maxDistance ? false : this.hasLineOfSight(eye, point, ignoreWalls);
   }

   private boolean isEntityInReach(Vec3d eye, Box box, float maxDistance) {
      Vec3d closest = new Vec3d(
         MathHelper.clamp(eye.x, box.minX, box.maxX), MathHelper.clamp(eye.y, box.minY, box.maxY), MathHelper.clamp(eye.z, box.minZ, box.maxZ)
      );
      return eye.squaredDistanceTo(closest) <= (double)maxDistance * (double)maxDistance;
   }

   private Box targetBox(LivingEntity entity) {
      AutoSwap autoSwap = AutoSwap.getInstance();
      return autoSwap != null && autoSwap.shouldUseVanillaAuraHitbox() ? HitBoxModule.withoutExpansion(entity::getBoundingBox) : entity.getBoundingBox();
   }

   private boolean hasLineOfSight(Vec3d eye, Vec3d point, boolean ignoreWalls) {
      if (ignoreWalls) {
         return true;
      } else {
         HitResult result = RaycastAngle.raycast(eye, point, ShapeType.COLLIDER);
         return result == null || result.getType() != Type.BLOCK;
      }
   }

   private Vec3d clampOffset(Vec3d value) {
      double len = value.length();
      return !(len <= 0.5) && len != 0.0 ? value.multiply(0.5 / len) : value;
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
