package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

public final class Sloth extends RotateConstructor {
   public Sloth() {
      super("Sloth");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
      if (entity == null) {
         return new Angle(
            currentAngle.getYaw() + MathHelper.clamp(delta.getYaw(), -45.0F, 45.0F),
            MathHelper.clamp(currentAngle.getPitch() + MathHelper.clamp(delta.getPitch(), -25.0F, 25.0F), -89.0F, 89.0F)
         );
      } else {
         float yawSpeed = 70.0F;
         float pitchSpeed = 15.0F;
         float yawjt = 64.0F * (float)Math.cos((double)System.currentTimeMillis() / 29.0);
         float yawjt2 = 12.0F * (float)Math.cos((double)System.currentTimeMillis() / 29.0);
         AuraModule aura = AuraModule.getInstance();
         MinecraftClient mc = MinecraftClient.getInstance();
         if (aura != null && aura.canAttack(1)) {
            yawjt = 0.0F;
            yawjt2 = 0.0F;
         }

         return new Angle(
            currentAngle.getYaw() + MathHelper.clamp(delta.getYaw(), -yawSpeed, yawSpeed) + yawjt,
            MathHelper.clamp(currentAngle.getPitch() + MathHelper.clamp(delta.getPitch(), -pitchSpeed, pitchSpeed), -90.0F, 90.0F) + yawjt2
         );
      }
   }

   @Override
   public Vec3d randomValue() {
      return new Vec3d(0.2F, 0.0, 5.0);
   }
}
