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

public final class MLSAC extends RotateConstructor {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private float animProgress = 1.0F;
   private float startYaw;
   private float startPitch;
   private float lastTargetYaw;
   private float lastTargetPitch;

   public MLSAC() {
      super("MLSAC");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      AuraModule aura = AuraModule.getInstance();
      if (aura != null && mc.player != null) {
         if (aura == null) {
            Object var10000 = null;
         } else {
            aura.getAttackHandlerRaw();
         }

         Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
         float yawDelta = angleDelta.getYaw();
         float pitchDelta = angleDelta.getPitch();
         float rotationDifference = (float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta));
         float on1 = 180.0F;
         float on2 = 5.0F;
         float yawJit = (float)((double)MathUtils.getRandom(5.0F, 35.0F) * Math.cos((double)System.currentTimeMillis() / 90.0));
         float pitchJit = (float)((double)MathUtils.getRandom(5.0F, 15.0F) * Math.sin((double)System.currentTimeMillis() / 90.0));
         float stryaw = Math.abs(yawDelta / rotationDifference) * on1;
         float strpitch = Math.abs(pitchDelta / rotationDifference) * on2;
         float fyaw = currentAngle.getYaw() + Math.clamp(yawDelta, -stryaw, stryaw);
         float fpitch = MathHelper.clamp(currentAngle.getPitch() + Math.clamp(pitchDelta, -strpitch, strpitch), -89.0F, 89.0F);
         return this.dempilator(currentAngle, fyaw + yawJit, fpitch + pitchJit, MathUtils.getRandom(0.1F, 0.4F));
      } else {
         return targetAngle;
      }
   }

   private Angle dempilator(Angle from, float targetYaw, float targetPitch, float speed) {
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
