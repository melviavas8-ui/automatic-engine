package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.util.MathUtils;

public final class ReallyWorld extends RotateConstructor {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private float animatedYawStep;
   private float animatedPitchStep;
   private float animProgress = 1.0F;
   private float startYaw;
   private float startPitch;
   private float lastTargetYaw;
   private float lastTargetPitch;

   public ReallyWorld() {
      super("ReallyWorld");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      AuraModule aura = AuraModule.getInstance();
      Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = this.safeRotationDifference(yawDelta, pitchDelta);
      if (aura != null && aura.isEnabled() && AuraModule.target != null && entity != null) {
         float yawJit = (float)(1.0 * Math.cos((double)System.currentTimeMillis() / 21.0));
         float pitchJit = (float)(1.0 * Math.sin((double)System.currentTimeMillis() / 21.0));
         float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 360.0F;
         float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 360.0F;
         float fyaw = currentAngle.getYaw() + Math.clamp(yawDelta, -straightLineYaw, straightLineYaw);
         float fpitch = MathHelper.clamp(currentAngle.getPitch() + Math.clamp(pitchDelta, -straightLinePitch, straightLinePitch), -89.0F, 89.0F);
         return this.animate(currentAngle, fyaw + yawJit, fpitch + pitchJit, MathUtils.getRandom(15.0F, 16.0F));
      } else {
         return this.limitPlain(currentAngle, yawDelta, pitchDelta, rotationDifference);
      }
   }

   private Angle limitPlain(Angle currentAngle, float yawDelta, float pitchDelta, float rotationDifference) {
      float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 360.0F;
      float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 360.0F;
      float yaw = currentAngle.getYaw() + Math.clamp(yawDelta, -straightLineYaw, straightLineYaw);
      float pitch = MathHelper.clamp(currentAngle.getPitch() + Math.clamp(pitchDelta, -straightLinePitch, straightLinePitch), -89.0F, 89.0F);
      this.animProgress = 1.0F;
      this.lastTargetYaw = yaw;
      this.lastTargetPitch = pitch;
      return new Angle(yaw, pitch);
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
      return new Vec3d(0.2F, 0.0, 5.0);
   }
}
