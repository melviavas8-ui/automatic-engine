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
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

public class Ai extends RotateConstructor {
   private long smoothbackShakeStartMs = -1L;
   private final AtomicLong время = new AtomicLong(0L);

   public Ai() {
      super("Ai");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta));
      AuraModule aura = AuraModule.getInstance();
      boolean атака = this.lolikbypass(0);
      boolean hasTarget = aura.isEnabled() && AuraModule.target != null && entity != null;
      if (AuraModule.target == null) {
         if (this.время.get() == 0L) {
            this.время.set(System.currentTimeMillis());
         }

         float pitch = (float)(10.0 * Math.cos((double)((float)System.currentTimeMillis() / 21.0F)));
         float yaw = (float)(55.0 * Math.sin((double)((float)System.currentTimeMillis() / 35.0F)));
         Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
         moveAngle.setYaw(currentAngle.getYaw() + yaw);
         moveAngle.setPitch(MathHelper.clamp(currentAngle.getPitch() + pitch, -90.0F, 90.0F));
         return moveAngle;
      } else {
         this.время.set(0L);
         float pitch = 0.0F;
         if (AuraModule.target != null && !атака) {
            pitch = (float)(25.0 * Math.cos((double)System.currentTimeMillis() / 90.0));
         }

         float yaw = 0.0F;
         if (AuraModule.target != null && !атака) {
            yaw = (float)(55.0 * Math.sin((double)System.currentTimeMillis() / 10.0));
         }

         float скорост = атака ? 0.87F : (this.lolikbypass(0) ? 1.0F : 0.77F);
         if (атака && !hasTarget) {
            скорост = 0.67F;
         }

         float lineYaw = Math.abs(yawDelta / rotationDifference) * 180.0F;
         float linePitch = Math.abs(pitchDelta) * 180.0F;
         float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
         float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
         float targetYaw = currentAngle.getYaw() + moveYaw;
         Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
         moveAngle.setYaw(MathHelper.lerp(Math.clamp(this.randomLerp(скорост, скорост + 0.2F), 0.0F, 1.0F), currentAngle.getYaw(), targetYaw) + yaw);
         float calculatedPitch = MathHelper.lerp(
               Math.clamp(this.randomLerp(скорост, скорост + 0.2F), 0.0F, 1.0F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch
            )
            + pitch;
         float clampedPitch = MathHelper.clamp(calculatedPitch, -90.0F, 90.0F);
         if (aura.isEnabled() && AuraModule.target != null) {
            this.smoothbackShakeStartMs = -1L;
         } else {
            if (this.smoothbackShakeStartMs < 0L) {
               this.smoothbackShakeStartMs = System.currentTimeMillis();
            }

            float shakeFade = 1.0F - Math.clamp((float)(System.currentTimeMillis() - this.smoothbackShakeStartMs) / 1000.0F, 0.0F, 1.0F);
            yaw *= shakeFade;
            pitch *= shakeFade;
         }

         moveAngle.setPitch(clampedPitch);
         return moveAngle;
      }
   }

   private boolean lolikbypass(int ticks) {
      AuraModule aura = AuraModule.getInstance();
      MinecraftClient client = MinecraftClient.getInstance();
      if (aura != null && AuraModule.target != null && client.player != null) {
         return client.player.distanceTo(AuraModule.target) > aura.attackDistance() ? false : aura.canAttack(ticks);
      } else {
         return false;
      }
   }

   private float randomLerp(float min, float max) {
      return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
   }

   @Override
   public Vec3d randomValue() {
      return new Vec3d(0.0, 0.0, 0.0);
   }
}
