package daamky.ua.api.module.impl.combat.aura;

import daamky.ua.api.module.impl.combat.aura.util.MathUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Angle {
   public static final Angle DEFAULT = new Angle(0.0F, 0.0F);
   private float yaw;
   private float pitch;

   public Angle(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = Mth.clamp(pitch, -90.0F, 90.0F);
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = Mth.clamp(pitch, -90.0F, 90.0F);
   }

   public Angle adjustSensitivity() {
      double gcd = MathUtils.computeGcd();
      Angle previous = AngleConnection.INSTANCE.getServerAngle();
      return new Angle(
         this.adjustAxis(this.yaw, previous.getYaw(), gcd, true), Mth.clamp(this.adjustAxis(this.pitch, previous.getPitch(), gcd, false), -90.0F, 90.0F)
      );
   }

   public Angle random(float radius) {
      return new Angle(this.yaw + MathUtils.getRandom(-radius, radius), this.pitch + MathUtils.getRandom(-radius, radius));
   }

   public Angle addYaw(float yaw) {
      return new Angle(this.yaw + yaw, this.pitch);
   }

   public Angle addPitch(float pitch) {
      this.pitch = Mth.clamp(this.pitch + pitch, -90.0F, 90.0F);
      return this;
   }

   public Angle of(Angle angle) {
      return new Angle(angle.getYaw(), angle.getPitch());
   }

   public Vec3 toVector() {
      float pitchRadians = this.pitch * (float) (Math.PI / 180.0);
      float yawRadians = -this.yaw * (float) (Math.PI / 180.0);
      float yawCos = Mth.cos((double)yawRadians);
      float yawSin = Mth.sin((double)yawRadians);
      float pitchCos = Mth.cos((double)pitchRadians);
      float pitchSin = Mth.sin((double)pitchRadians);
      return new Vec3((double)(yawSin * pitchCos), (double)(-pitchSin), (double)(yawCos * pitchCos));
   }

   private float adjustAxis(float axisValue, float previousValue, double gcd, boolean wrap) {
      if (gcd <= 0.0) {
         return wrap ? Mth.wrapDegrees(axisValue) : axisValue;
      } else {
         float delta = wrap ? Mth.wrapDegrees(axisValue - previousValue) : axisValue - previousValue;
         return previousValue + (float)Math.round((double)delta / gcd) * (float)gcd;
      }
   }

   @Override
   public String toString() {
      return "Angle{yaw=" + this.yaw + ", pitch=" + this.pitch + "}";
   }

   public static final class VecRotation {
      private final Angle angle;
      private final Vec3 vec;

      public VecRotation(Angle angle, Vec3 vec) {
         this.angle = angle;
         this.vec = vec;
      }

      public Angle getAngle() {
         return this.angle;
      }

      public Vec3 getVec() {
         return this.vec;
      }
   }
}
