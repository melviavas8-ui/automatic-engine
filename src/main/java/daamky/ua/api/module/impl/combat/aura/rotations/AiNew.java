package daamky.ua.api.module.impl.combat.aura.rotations;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.util.MathUtils;

public class AiNew extends RotateConstructor {
   private float animProgress = 1.0F;
   private float startYaw;
   private float startPitch;
   private float lastTargetYaw;
   private float lastTargetPitch;
   private final AtomicLong время = new AtomicLong(0L);

   public AiNew() {
      super("AiNew");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta));
      if (rotationDifference < 1.0E-6F) {
         rotationDifference = 1.0F;
      }

      AuraModule aura = AuraModule.getInstance();
      boolean атака = this.lolikbypass(0);
      if (aura != null && aura.isEnabled() && AuraModule.target != null && entity != null) {
         boolean var36 = true;
      } else {
         boolean var10000 = false;
      }

      float playerPitch1 = MinecraftClient.getInstance().player.getPitch();
      float playerYaw = MinecraftClient.getInstance().player.getYaw();
      if (AuraModule.target == null) {
         if (this.время.get() == 0L) {
            this.время.set(System.currentTimeMillis());
         }

         if (System.currentTimeMillis() - this.время.get() < 500L) {
            float pitch = (float)(0.0 * Math.cos((double)((float)System.currentTimeMillis() / 2.0F)));
            float yaw = (float)(0.0 * Math.sin((double)((float)System.currentTimeMillis() / 3.0F)));
            Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(currentAngle.getYaw() + yaw);
            moveAngle.setPitch(MathHelper.clamp(currentAngle.getPitch() + pitch, -90.0F, 90.0F));
            return moveAngle;
         }
      } else {
         this.время.set(0L);
      }

      float yawDeltaToTarget = MathHelper.wrapDegrees(targetAngle.getYaw() - currentAngle.getYaw());
      float pitchDeltaToTarget = targetAngle.getPitch() - currentAngle.getPitch();
      float yawDeltaFromPlayer = MathHelper.wrapDegrees(playerYaw - currentAngle.getYaw());
      float yawSpeed = атака ? MathUtils.getRandom(55.0F, 100.0F) : MathUtils.getRandom(20.0F, 45.0F);
      float pitchSpeed = атака ? 12.0F : 3.0F;
      float moveYawToTarget = MathHelper.clamp(yawDeltaToTarget, -yawSpeed, yawSpeed);
      float movePitchToTarget = MathHelper.clamp(pitchDeltaToTarget, -pitchSpeed, pitchSpeed);
      float playerYawInfluence = MathHelper.clamp(yawDeltaFromPlayer * 0.18F, -3.0F, 3.0F);
      float randomYaw = MathUtils.getRandom(-0.8F, 0.8F);
      boolean aimed = Math.abs(yawDeltaToTarget) < 2.0F && Math.abs(pitchDeltaToTarget) < 2.0F;
      float shakeYaw = 0.0F;
      float shakePitch = 0.0F;
      if (AuraModule.target != null && !атака && !aimed) {
         shakeYaw = (float)(1.5 * Math.sin((double)System.currentTimeMillis() / 140.0));
         shakePitch = (float)(0.35F * Math.cos((double)System.currentTimeMillis() / 160.0));
      }

      float finalYaw = currentAngle.getYaw() + moveYawToTarget + playerYawInfluence + randomYaw + shakeYaw;
      float finalPitch = currentAngle.getPitch() + movePitchToTarget + shakePitch;
      Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
      moveAngle.setYaw(MathHelper.wrapDegrees(finalYaw));
      moveAngle.setPitch(MathHelper.clamp(finalPitch, -90.0F, 90.0F));
      float playerPitchInfluence = 1.0F;
      if (MinecraftClient.getInstance().player != null) {
         float playerPitch = MinecraftClient.getInstance().player.getPitch();
         float blendedPitch = MathHelper.lerp(1.0F, moveAngle.getPitch(), playerPitch);
         moveAngle.setPitch(MathHelper.clamp(blendedPitch, -90.0F, 90.0F));
         return this.animate(currentAngle, moveAngle.getYaw(), moveAngle.getPitch(), MathUtils.getRandom(0.1F, 0.4F));
      } else {
         return currentAngle;
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

   private boolean lolikbypass(int ticks) {
      AuraModule aura = AuraModule.getInstance();
      if (aura == null) {
         return false;
      } else {
         try {
            StrikeManager handler = aura.getAttackHandler();
            return handler == null ? false : AuraModule.target != null && handler.canAttack(aura.getConfig(), ticks);
         } catch (Throwable var4) {
            return false;
         }
      }
   }

   private float easeOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow((double)(x - 1.0F), 3.0) + c1 * (float)Math.pow((double)(x - 1.0F), 2.0);
   }

   private float randomLerp(float min, float max) {
      return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
   }

   @Override
   public Vec3d randomValue() {
      return new Vec3d(0.0, 0.0, 0.0);
   }
}
