package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NeuroAimPointFeatures {
   private static final NeuroAimPointFeatures EMPTY = new NeuroAimPointFeatures(0.0, 0.0, 0.5, 0.0, 0.0);
   private final double multiPointEnabled;
   private final double aimOffsetX;
   private final double aimOffsetY;
   private final double aimOffsetZ;
   private final double aimOffsetLength;

   private NeuroAimPointFeatures(double multiPointEnabled, double aimOffsetX, double aimOffsetY, double aimOffsetZ, double aimOffsetLength) {
      this.multiPointEnabled = multiPointEnabled;
      this.aimOffsetX = aimOffsetX;
      this.aimOffsetY = aimOffsetY;
      this.aimOffsetZ = aimOffsetZ;
      this.aimOffsetLength = aimOffsetLength;
   }

   public static NeuroAimPointFeatures from(LivingEntity target, AABB referenceBox, Vec3 aimPoint, boolean multiPointEnabled) {
      if (target != null && aimPoint != null) {
         AABB box = referenceBox != null ? referenceBox : target.getBoundingBox();
         if (box == null) {
            return EMPTY;
         } else {
            Vec3 center = box.getCenter();
            double halfX = Math.max(box.getXsize() * 0.5, 1.0E-6);
            double halfZ = Math.max(box.getZsize() * 0.5, 1.0E-6);
            double height = Math.max(box.getYsize(), 1.0E-6);
            double offsetX = Mth.clamp((aimPoint.x - center.x) / halfX, -2.0, 2.0);
            double offsetY = Mth.clamp((aimPoint.y - box.minY) / height, -1.0, 2.0);
            double offsetZ = Mth.clamp((aimPoint.z - center.z) / halfZ, -2.0, 2.0);
            double offsetLength = Mth.clamp(Math.sqrt(offsetX * offsetX + offsetZ * offsetZ), 0.0, 3.0);
            return new NeuroAimPointFeatures(multiPointEnabled ? 1.0 : 0.0, offsetX, offsetY, offsetZ, offsetLength);
         }
      } else {
         return EMPTY;
      }
   }

   public double multiPointEnabled() {
      return this.multiPointEnabled;
   }

   public double aimOffsetX() {
      return this.aimOffsetX;
   }

   public double aimOffsetY() {
      return this.aimOffsetY;
   }

   public double aimOffsetZ() {
      return this.aimOffsetZ;
   }

   public double aimOffsetLength() {
      return this.aimOffsetLength;
   }
}
