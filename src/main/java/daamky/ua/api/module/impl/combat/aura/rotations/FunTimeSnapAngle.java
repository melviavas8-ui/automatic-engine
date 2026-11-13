package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.rotations.funtime.FTMathUtil;
import daamky.ua.api.module.impl.combat.aura.util.MathUtils;
import daamky.ua.api.module.impl.combat.aura.utils.Animator;

public final class FunTimeSnapAngle extends RotateConstructor {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private long smoothbackShakeStartMs = -1L;

   public FunTimeSnapAngle() {
      super("FunTimeSnapAngle");
   }

   @Override
   public Angle limitAngleChange(Angle current, Angle target, Vec3d aimPoint, Entity entity) {
      AuraModule aura = AuraModule.getInstance();
      if (aura != null && this.mc.player != null) {
         StrikeManager attackHandler = aura.getAttackHandlerRaw();
         boolean canAttack = attackHandler != null && aura.getConfig() != null && attackHandler.canAttack(aura.getConfig(), 4);
         if (aura.isEnabled() && AuraModule.target != null && entity != null && canAttack) {
            this.smoothbackShakeStartMs = -1L;
            Angle delta = MathAngle.calculateDelta(current, target);
            float yawDelta = delta.getYaw();
            float pitchDelta = delta.getPitch();
            float rotationDifference = this.safeRotationDifference(yawDelta, pitchDelta);
            float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 110.0F;
            float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 110.0F;
            float desiredYaw = MathHelper.lerp(0.55F, current.getYaw(), current.getYaw() + MathHelper.clamp(yawDelta, -straightLineYaw, straightLineYaw));
            float desiredPitch = MathHelper.lerp(
               0.55F, current.getPitch(), current.getPitch() + MathHelper.clamp(pitchDelta, -straightLinePitch, straightLinePitch)
            );
            return this.animate(current, desiredYaw, desiredPitch);
         } else {
            Angle playerView = new Angle(this.mc.player.getYaw(), this.mc.player.getPitch());
            Angle delta = MathAngle.calculateDelta(current, playerView);
            float yawDelta = delta.getYaw();
            float pitchDelta = delta.getPitch();
            float rotationDifference = this.safeRotationDifference(yawDelta, pitchDelta);
            long now = System.currentTimeMillis();
            float yawShake = (float)((double)FTMathUtil.smoothRandom(-12.0F, 12.0F, 14.0F) * Math.sin((double)now / 70.0));
            float pitchShake = (float)((double)FTMathUtil.smoothRandom(-6.0F, 6.0F, 14.0F) * Math.cos((double)now / 60.0));
            if (aura.isEnabled() && AuraModule.target != null) {
               this.smoothbackShakeStartMs = -1L;
            } else {
               if (this.smoothbackShakeStartMs < 0L) {
                  this.smoothbackShakeStartMs = now;
               }

               float shakeFade = 1.0F - MathHelper.clamp((float)(now - this.smoothbackShakeStartMs) / 1000.0F, 0.0F, 1.0F);
               yawShake *= shakeFade;
               pitchShake *= shakeFade;
            }

            boolean attackCooldownFinished = attackHandler == null || attackHandler.getAttackTimer().finished(535.0);
            float speed = attackCooldownFinished ? 45.0F : 0.0F;
            float straightLineYaw = Math.abs(yawDelta / rotationDifference) * speed;
            float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * speed;
            float desiredYaw = MathHelper.lerp(
               0.85F, current.getYaw(), current.getYaw() + MathHelper.clamp(yawDelta, -straightLineYaw, straightLineYaw) + yawShake
            );
            float desiredPitch = MathHelper.lerp(
               0.85F, current.getPitch(), current.getPitch() + MathHelper.clamp(pitchDelta, -straightLinePitch, straightLinePitch) + pitchShake
            );
            return this.animate(current, desiredYaw, desiredPitch);
         }
      } else {
         return current;
      }
   }

   private Angle animate(Angle current, float targetYaw, float targetPitch) {
      return Animator.back(MathUtils.getRandom(6.0F, 7.0F), FunTimeSnapAngle.class).apply(current, targetYaw, MathHelper.clamp(targetPitch, -90.0F, 90.0F));
   }

   @Override
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }
}
