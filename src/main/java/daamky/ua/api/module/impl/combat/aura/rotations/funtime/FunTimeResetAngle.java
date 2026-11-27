package daamky.ua.api.module.impl.combat.aura.rotations.funtime;

import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class FunTimeResetAngle extends RotateConstructor {
   public static final FunTimeResetAngle INSTANCE = new FunTimeResetAngle();

   private FunTimeResetAngle() {
      super("FunTimeReset");
   }

   @Override
   public Angle limitAngleChange(Angle current, Angle target, Vec3 point, Entity entity) {
      Angle delta = MathAngle.calculateDelta(current, target);
      float difference = (float)Math.hypot((double)Math.abs(delta.getYaw()), (double)Math.abs(delta.getPitch()));
      float factor = 1.0F - Math.min(1.0F, difference / 180.0F);
      float yawSpeed = 15.0F + factor * 45.0F;
      float pitchSpeed = 25.0F;
      AuraModule aura = AuraModule.getInstance();
      if (aura != null && !aura.getAttackHandler().getAttackTimer().finished(700.0)) {
         yawSpeed = 15.0F;
         pitchSpeed = 5.0F;
      }

      return new Angle(
         current.getYaw() + Mth.clamp(delta.getYaw(), -yawSpeed, yawSpeed),
         Mth.clamp(current.getPitch() + Mth.clamp(delta.getPitch(), -pitchSpeed, pitchSpeed), -90.0F, 90.0F)
      );
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}
