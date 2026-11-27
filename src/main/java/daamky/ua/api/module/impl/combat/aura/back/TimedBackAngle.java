package daamky.ua.api.module.impl.combat.aura.back;

import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class TimedBackAngle extends RotateConstructor {
   private final long durationNanos;
   private long startNanos;
   private Angle startAngle;

   public TimedBackAngle(long durationMillis) {
      super("TimedBack");
      this.durationNanos = Math.max(1L, durationMillis) * 1000000L;
   }

   @Override
   public Angle limitAngleChange(Angle current, Angle target, Vec3 aimPoint, Entity entity) {
      if (current != null && target != null) {
         if (this.startAngle == null) {
            this.startAngle = new Angle(current.getYaw(), current.getPitch());
            this.startNanos = System.nanoTime();
         }

         float progress = Mth.clamp((float)(System.nanoTime() - this.startNanos) / (float)this.durationNanos, 0.0F, 1.0F);
         float smooth = progress * progress * (3.0F - 2.0F * progress);
         float yawDelta = Mth.wrapDegrees(target.getYaw() - this.startAngle.getYaw());
         float pitchDelta = target.getPitch() - this.startAngle.getPitch();
         return new Angle(this.startAngle.getYaw() + yawDelta * smooth, this.startAngle.getPitch() + pitchDelta * smooth);
      } else {
         return current;
      }
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}
