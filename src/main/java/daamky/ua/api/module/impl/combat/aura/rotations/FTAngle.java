package daamky.ua.api.module.impl.combat.aura.rotations;

import java.util.Random;
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

public class FTAngle extends RotateConstructor {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Random random;
   private final float baseYawStep;
   private final float basePitchStep;
   private final float jitterBase;
   private int fatigue;
   private long lockUntil;
   private float accumulatedErrorYaw;
   private float accumulatedErrorPitch;
   private int variant;

   public FTAngle() {
      super("FTAngle");
      long seed = System.nanoTime() * 31L ^ System.currentTimeMillis() ^ (long)this.mc.hashCode();
      this.random = new Random(seed);
      this.baseYawStep = 34.4F + this.random.nextFloat() * 15.0F;
      this.basePitchStep = 2.8F + this.random.nextFloat() * 5.2F;
      this.jitterBase = 0.12F + this.random.nextFloat() * 1.45F;
      this.variant = this.random.nextInt(5);
   }

   @Override
   public Angle limitAngleChange(Angle currentTurns, Angle targetTurns, Vec3d vec3d, Entity entity) {
      if (this.mc.player == null) {
         return targetTurns;
      } else {
         long now = System.currentTimeMillis();
         if (now < this.lockUntil) {
            return currentTurns;
         } else {
            if (this.random.nextDouble() < 0.055) {
               this.variant = this.random.nextInt(5);
            }

            Angle delta = MathAngle.calculateDelta(currentTurns, targetTurns);
            float yawStep = MathHelper.clamp(delta.getYaw(), -this.baseYawStep, this.baseYawStep);
            float pitchStep = MathHelper.clamp(delta.getPitch(), -this.basePitchStep, this.basePitchStep);
            float distance = entity == null ? Float.MAX_VALUE : this.mc.player.distanceTo(entity);
            if (distance < 4.5F) {
               yawStep *= 0.62F;
               pitchStep *= 0.71F;
            }

            if (this.random.nextDouble() < (this.variant == 4 ? 0.19 : 0.08)) {
               yawStep += (this.random.nextFloat() - 0.5F) * 7.5F;
               pitchStep += (this.random.nextFloat() - 0.5F) * 5.2F;
               this.fatigue = 13 + this.random.nextInt(18);
            } else if (this.fatigue > 0) {
               yawStep *= 0.58F;
               pitchStep *= 0.61F;
               this.fatigue--;
            }

            float nextYaw = currentTurns.getYaw() + yawStep;
            float nextPitch = currentTurns.getPitch() + pitchStep;
            this.accumulatedErrorYaw = this.accumulatedErrorYaw * 0.73F + (this.random.nextFloat() - 0.5F) * 0.95F;
            this.accumulatedErrorPitch = this.accumulatedErrorPitch * 0.71F + (this.random.nextFloat() - 0.5F) * 0.72F;
            nextYaw += this.accumulatedErrorYaw;
            nextPitch += this.accumulatedErrorPitch;
            nextYaw += (float)(
               Math.sin((double)now / 23.7) * 0.62
                  + Math.sin((double)now / 71.0) * 0.48
                  + Math.cos((double)now / 124.0) * 0.33
                  + Math.sin((double)now / 189.0) * 0.21
                  + this.random.nextGaussian() * (double)this.jitterBase
            );
            nextPitch += (float)(
               Math.cos((double)now / 38.4) * 0.51 + Math.sin((double)now / 103.0) * 0.29 + this.random.nextGaussian() * (double)(this.jitterBase * 0.85F)
            );
            if (this.variant == 1) {
               nextYaw = currentTurns.getYaw() + (nextYaw - currentTurns.getYaw()) * 0.58F;
            }

            float gcd = (float)MathUtils.computeGcd();
            if (gcd > 0.0F) {
               nextYaw = (float)Math.round(nextYaw / gcd) * gcd;
               nextPitch = (float)Math.round(nextPitch / gcd) * gcd;
            }

            nextPitch = MathHelper.clamp(nextPitch, -89.0F, 89.0F);
            if (this.canAttack(entity)) {
               this.lockUntil = now + 132L + (long)this.random.nextInt(148);
            }

            return new Angle(nextYaw, nextPitch);
         }
      }
   }

   private boolean canAttack(Entity entity) {
      AuraModule aura = AuraModule.getInstance();
      if (entity != null && aura != null && aura.isEnabled() && AuraModule.target != null) {
         StrikeManager attackHandler = aura.getAttackHandlerRaw();
         return attackHandler != null && attackHandler.canAttack(aura.getConfig(), 3);
      } else {
         return false;
      }
   }

   public void lock(int milliseconds) {
      this.lockUntil = System.currentTimeMillis() + (long)milliseconds + (long)this.random.nextInt(110);
   }

   public boolean locked() {
      return System.currentTimeMillis() < this.lockUntil;
   }

   @Override
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }
}
