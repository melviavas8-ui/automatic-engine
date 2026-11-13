package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.attack.StrikerConstructor;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.target.RaycastAngle;
import daamky.ua.api.module.impl.combat.aura.util.MathUtils;

public class Matrix extends RotateConstructor {
   private float animatedPitchStep;
   private float animProgress = 1.0F;
   private float startYaw;
   private float startPitch;
   private float lastTargetYaw;
   private float lastTargetPitch;

   public Matrix() {
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
                  boolean rayTrace = RaycastAngle.rayTrace(currentAngle.toVector(), (double)(aura.attackDistance() - 0.5F), config.getBox());
                  boolean outOfAttackRange = client.player.distanceTo(entity) > aura.attackDistance();
                  float playerHead = client.player.getPitch() + currentAngle.getPitch();
                  if (!rayTrace && !outOfAttackRange) {
                     float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 180.0F;
                     float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 180.0F;
                     float yawJit = (float)(8.0 * Math.cos((double)System.currentTimeMillis() / 70.0));
                     float pitchJit = (float)(6.0 * Math.sin((double)System.currentTimeMillis() / 70.0));
                     return this.animate(
                        currentAngle,
                        currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw) + yawJit,
                        currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch) + pitchJit,
                        MathUtils.getRandom(0.5F, 0.75F)
                     );
                  } else {
                     return currentAngle;
                  }
               }
            }
         }
      }
   }

   private Angle animate(Angle from, float targetYaw, float targetPitch, float speed) {
      float targetChangedYaw = Math.abs(MathHelper.wrapDegrees(targetYaw - this.lastTargetYaw));
      float targetChangedPitch = Math.abs(targetPitch - this.lastTargetPitch);
      if (this.animProgress >= 1.0F || targetChangedYaw > 6.0F || targetChangedPitch > 6.0F) {
         this.startYaw = from.getYaw();
         this.startPitch = from.getPitch();
         this.lastTargetYaw = targetYaw;
         this.lastTargetPitch = targetPitch;
         this.animProgress = 0.0F;
      }

      this.animProgress = MathHelper.clamp(this.animProgress + speed, 0.0F, 1.0F);
      float eased = this.easeOutBack(this.animProgress);
      float yawDelta = MathHelper.wrapDegrees(this.lastTargetYaw - this.startYaw);
      float pitchDelta = this.lastTargetPitch - this.startPitch;
      float yaw = this.startYaw + yawDelta * eased;
      float pitch = MathHelper.clamp(this.startPitch + pitchDelta * eased, -89.0F, 89.0F);
      return new Angle(yaw, pitch);
   }

   private float easeOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow((double)(x - 1.0F), 3.0) + c1 * (float)Math.pow((double)(x - 1.0F), 2.0);
   }

   @Override
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }
}
