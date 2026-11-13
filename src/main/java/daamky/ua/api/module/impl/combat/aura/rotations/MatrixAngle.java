package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.attack.StrikerConstructor;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.target.RaycastAngle;
import daamky.ua.api.module.impl.combat.aura.util.MathUtils;

public class MatrixAngle extends RotateConstructor {
   public MatrixAngle() {
      super("Matrix");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      if (entity == null) {
         return currentAngle;
      } else {
         AuraModule aura = AuraModule.getInstance();
         if (aura == null) {
            return currentAngle;
         } else {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
               return currentAngle;
            } else {
               StrikeManager attackHandler = aura.getAttackHandlerRaw();
               if (attackHandler == null) {
                  return currentAngle;
               } else {
                  StrikerConstructor.AttackPerpetratorConfigurable config = aura.getConfig();
                  Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
                  float yawDelta = angleDelta.getYaw();
                  float pitchDelta = angleDelta.getPitch();
                  float rotationDifference = (float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta));
                  if (rotationDifference < 1.0E-4F) {
                     rotationDifference = 1.0E-4F;
                  }

                  boolean shouldAttack = attackHandler.canAttack(config, 2);
                  boolean shouldAttack2 = attackHandler.canAttack(config, 0);
                  boolean shouldAttack3 = attackHandler.canAttack(config, 5);
                  boolean rayTrace = RaycastAngle.rayTrace(currentAngle.toVector(), (double)(aura.attackDistance() - 0.25F), config.getBox());
                  boolean outOfAttackRange = client.player.distanceTo(entity) > aura.attackDistance();
                  if (!rayTrace && !outOfAttackRange) {
                     float straightLineYaw = Math.abs(yawDelta / rotationDifference) * (shouldAttack2 ? 100.0F : MathUtils.getRandom(25.0F, 50.0F));
                     float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * (shouldAttack2 ? 100.0F : MathUtils.getRandom(5.0F, 25.0F));
                     if (!attackHandler.getAttackTimer().finished((double)MathUtils.getRandom(50.0F, 100.0F))) {
                        straightLinePitch = 0.0F;
                        straightLineYaw = 0.0F;
                     }

                     float jitterY = shouldAttack3 ? (!rayTrace && shouldAttack ? 0.0F : MathUtils.getRandom(0.0F, 0.0F)) : 0.0F;
                     float jitterX = shouldAttack3 ? (!rayTrace && shouldAttack ? 0.0F : MathUtils.getRandom(0.0F, 0.0F)) : 0.0F;
                     return new Angle(
                        currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw) + jitterX,
                        currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch) + jitterY
                     );
                  } else {
                     return currentAngle;
                  }
               }
            }
         }
      }
   }

   @Override
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }
}
