package daamky.ua.api.module.impl.combat.aura.rotations;

import java.security.SecureRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.target.Vector;

public class LegitSnapAngle extends RotateConstructor {
   public LegitSnapAngle() {
      super("LegitSnap");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      if (entity != null && AuraModule.getInstance().mode.isSelected("Snap")) {
         Vec3d aimPoint = Vector.hitbox(entity, 1.0F, 1.0F, 1.0F, 2.0F);
         targetAngle = MathAngle.calculateAngle(aimPoint);
      }

      StrikeManager attackHandler = AuraModule.getInstance().getAttackHandler();
      AuraModule aura = AuraModule.getInstance();
      Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta));
      if (entity != null && attackHandler.canAttack(aura.getConfig(), 0)) {
         boolean var20 = true;
      } else {
         boolean var10000 = false;
      }

      float speed = 1.0F;
      float maxRotation = 180.0F;
      float lineYaw = Math.abs(yawDelta / rotationDifference) * maxRotation;
      float linePitch = Math.abs(pitchDelta / rotationDifference) * maxRotation;
      float moveYaw = Math.clamp(yawDelta, -lineYaw, lineYaw);
      float movePitch = Math.clamp(pitchDelta, -linePitch, linePitch);
      Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
      moveAngle.setYaw(MathHelper.lerp(speed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
      moveAngle.setPitch(MathHelper.lerp(speed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch));
      return new Angle(moveAngle.getYaw(), moveAngle.getPitch());
   }

   private float randomLerp(float min, float max) {
      return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
   }

   @Override
   public Vec3d randomValue() {
      return new Vec3d(0.0, 0.0, 0.0);
   }
}
