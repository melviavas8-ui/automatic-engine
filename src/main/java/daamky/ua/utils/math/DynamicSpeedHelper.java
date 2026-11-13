package daamky.ua.utils.math;

import lombok.Generated;
import daamky.ua.api.module.impl.combat.aura.Angle;

public final class DynamicSpeedHelper {
   public static float compute(float min, float max, Angle delta) {
      float rotationDifference = (float)Math.hypot((double)Math.abs(delta.getYaw()), (double)Math.abs(delta.getPitch()));
      float factor = 1.0F - Math.min(1.0F, rotationDifference / 180.0F);
      return min + factor * (max - min);
   }

   @Generated
   private DynamicSpeedHelper() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
