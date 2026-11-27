package daamky.ua.api.module.impl.combat.aura.rotations;

import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SnapAngle extends RotateConstructor {
   public SnapAngle() {
      super("Snap");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      AuraModule aura = AuraModule.getInstance();
      Minecraft client = Minecraft.getInstance();
      if (aura != null && client.player != null) {
         StrikeManager attackHandler = aura.getAttackHandlerRaw();
         if (entity != null && targetAngle != null && attackHandler != null && aura.getConfig() != null && attackHandler.canAttack(aura.getConfig(), 1)) {
            return this.step(currentAngle, targetAngle, aura.getSpeedValue().getFloat() * 1.5F);
         } else {
            Angle playerViewAngle = new Angle(client.player.getYRot(), client.player.getXRot());
            float speed = attackHandler != null && !attackHandler.getAttackTimer().finished(aura.getTimeOnTick().getValue())
               ? 0.0F
               : aura.getSpeedValue().getFloat() / 1.5F;
            return this.step(currentAngle, playerViewAngle, speed);
         }
      } else {
         return currentAngle;
      }
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }

   private Angle step(Angle currentAngle, Angle targetAngle, float speed) {
      Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawDelta = delta.getYaw();
      float pitchDelta = delta.getPitch();
      float difference = (float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta));
      if (difference < 1.0E-4F) {
         difference = 1.0E-4F;
      }

      float straightLineYaw = Math.abs(yawDelta / difference) * speed;
      float straightLinePitch = Math.abs(pitchDelta / difference) * speed;
      return new Angle(
         currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
         currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
      );
   }
}
