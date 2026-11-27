package daamky.ua.api.module.impl.combat.aura.impl;

import daamky.ua.api.module.impl.combat.aura.Angle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public abstract class RotateConstructor {
   protected static final float MIN_ROTATION_DIFFERENCE = 1.0E-4F;
   private final String name;

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle) {
      return this.limitAngleChange(currentAngle, targetAngle, null, null);
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d) {
      return this.limitAngleChange(currentAngle, targetAngle, vec3d, null);
   }

   public abstract Angle limitAngleChange(Angle var1, Angle var2, Vec3 var3, Entity var4);

   public abstract Vec3 randomValue();

   protected float safeRotationDifference(float yawDelta, float pitchDelta) {
      return Math.max((float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta)), 1.0E-4F);
   }

   public String getName() {
      return this.name;
   }

   public RotateConstructor(String name) {
      this.name = name;
   }
}
