package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.rotations.funtime.FTMathUtil;
import daamky.ua.api.module.impl.combat.aura.target.RaycastAngle;
import daamky.ua.api.module.impl.combat.aura.util.MathUtils;
import daamky.ua.api.module.impl.combat.aura.utils.Animator;

public final class SPAngle extends RotateConstructor {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private float animProgress = 1.0F;
   private float startYaw;
   private float startPitch;
   private float lastTargetYaw;
   private float lastTargetPitch;

   public SPAngle() {
      super("SPAngle");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d aimPoint, Entity entity) {
      if (currentAngle != null && targetAngle != null) {
         AuraModule aura = AuraModule.getInstance();
         if (aura == null) {
            Object var10000 = null;
         } else {
            aura.getAttackHandlerRaw();
         }

         Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
         float yawDelta = delta.getYaw();
         float pitchDelta = delta.getPitch();
         float difference = this.safeRotationDifference(yawDelta, pitchDelta);
         float yawSpeed = MathUtils.getRandom(22.0F, 45.0F);
         float pitchSpeed = MathUtils.getRandom(5.0F, 15.0F);
         float yawJitter = FTMathUtil.smoothRandom(-2.0F, 2.0F, 12.0F);
         float pitchJitter = FTMathUtil.smoothRandom(-2.0F, 2.0F, 12.0F);
         if (entity != null && aura != null && RaycastAngle.rayTrace((double)aura.attackDistance(), entity.getBoundingBox())) {
            boolean var21 = true;
         } else {
            boolean var20 = false;
         }

         float maxYawStep = Math.abs(yawDelta / difference) * yawSpeed;
         float maxPitchStep = Math.abs(pitchDelta / difference) * pitchSpeed;
         float targetYaw = currentAngle.getYaw() + MathHelper.clamp(yawDelta, -maxYawStep, maxYawStep) + yawJitter;
         float targetPitch = MathHelper.clamp(currentAngle.getPitch() + MathHelper.clamp(pitchDelta, -maxPitchStep, maxPitchStep) + pitchJitter, -89.0F, 89.0F);
         return Animator.spooky(2.0F, SPAngle.class).apply(currentAngle, targetYaw, targetPitch);
      } else {
         return currentAngle;
      }
   }

   @Override
   public Vec3d randomValue() {
      return new Vec3d(0.2F, 0.0, 5.0);
   }
}
