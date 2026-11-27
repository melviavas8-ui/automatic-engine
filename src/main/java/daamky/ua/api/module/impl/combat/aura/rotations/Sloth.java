package daamky.ua.api.module.impl.combat.aura.rotations;

import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class Sloth extends RotateConstructor {
   public Sloth() {
      super("Sloth");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
      if (entity == null) {
         return new Angle(
            currentAngle.getYaw() + Mth.clamp(delta.getYaw(), -45.0F, 45.0F),
            Mth.clamp(currentAngle.getPitch() + Mth.clamp(delta.getPitch(), -25.0F, 25.0F), -89.0F, 89.0F)
         );
      } else {
         float yawSpeed = 70.0F;
         float pitchSpeed = 15.0F;
         float yawjt = 64.0F * (float)Math.cos((double)System.currentTimeMillis() / 29.0);
         float yawjt2 = 12.0F * (float)Math.cos((double)System.currentTimeMillis() / 29.0);
         AuraModule aura = AuraModule.getInstance();
         Minecraft mc = Minecraft.getInstance();
         if (aura != null && aura.canAttack(1)) {
            yawjt = 0.0F;
            yawjt2 = 0.0F;
         }

         return new Angle(
            currentAngle.getYaw() + Mth.clamp(delta.getYaw(), -yawSpeed, yawSpeed) + yawjt,
            Mth.clamp(currentAngle.getPitch() + Mth.clamp(delta.getPitch(), -pitchSpeed, pitchSpeed), -90.0F, 90.0F) + yawjt2
         );
      }
   }

   @Override
   public Vec3 randomValue() {
      return new Vec3(0.2F, 0.0, 5.0);
   }
}
