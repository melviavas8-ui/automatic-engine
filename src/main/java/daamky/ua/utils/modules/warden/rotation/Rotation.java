package daamky.ua.utils.modules.warden.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class Rotation {
   public float floatValue;
   public float floatValue2;

   public Rotation(Entity entity) {
      this.floatValue = entity.getYRot();
      this.floatValue2 = entity.getXRot();
   }

   public Rotation(float yaw, float pitch) {
      this.floatValue = yaw;
      this.floatValue2 = pitch;
   }

   public float measure(Rotation other) {
      float deltaYaw = Mth.wrapDegrees(other.floatValue - this.floatValue);
      float deltaPitch = other.floatValue2 - this.floatValue2;
      return (float)Math.hypot((double)Math.abs(deltaYaw), (double)Math.abs(deltaPitch));
   }

   public double measure2(Rotation other) {
      double deltaYaw = (double)Mth.wrapDegrees(other.floatValue - this.floatValue);
      double deltaPitch = (double)Mth.wrapDegrees(other.floatValue2 - this.floatValue2);
      return Math.hypot(deltaYaw, deltaPitch);
   }
}
