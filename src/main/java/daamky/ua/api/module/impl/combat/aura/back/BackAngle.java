package daamky.ua.api.module.impl.combat.aura.back;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

public class BackAngle extends RotateConstructor {
   private final float speed;

   public BackAngle(float speed) {
      super("UseBack");
      this.speed = MathHelper.clamp(speed, 0.05F, 1.0F);
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawStep = delta.getYaw() * this.speed;
      float pitchStep = delta.getPitch() * this.speed;
      if (Math.abs(yawStep) < 0.01F) {
         yawStep = delta.getYaw();
      }

      if (Math.abs(pitchStep) < 0.01F) {
         pitchStep = delta.getPitch();
      }

      return new Angle(currentAngle.getYaw() + yawStep, currentAngle.getPitch() + pitchStep);
   }

   @Override
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }
}
