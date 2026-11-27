package daamky.ua.api.module.impl.combat.macetarget.prediction;

import daamky.ua.api.module.impl.combat.macetarget.state.MaceState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class TargetPredictor {
   private static final Minecraft MC = Minecraft.getInstance();
   private static final int MIN_SAMPLES = 2;
   private static final double SMOOTHING = 0.6;
   private Vec3 lastPosition;
   private Vec3 smoothedVelocity = Vec3.ZERO;
   private long lastUpdateTime;
   private int sampleCount;

   public TargetPredictor() {
   }

   public void update(LivingEntity target) {
      if (target == null) {
         this.reset();
      } else {
         Vec3 currentPos = target.position();
         long currentTime = System.currentTimeMillis();
         if (this.lastPosition != null && this.lastUpdateTime > 0L) {
            long deltaTime = currentTime - this.lastUpdateTime;
            if (deltaTime > 0L && deltaTime < 500L) {
               Vec3 velocity = currentPos.subtract(this.lastPosition);
               this.smoothedVelocity = this.smoothedVelocity.scale(0.6).add(velocity.scale(0.4));
               this.sampleCount++;
            }
         }

         this.lastPosition = currentPos;
         this.lastUpdateTime = currentTime;
      }
   }

   public Vec3 getPredictedPosition(LivingEntity target, MaceState.Stage stage) {
      if (target != null && MC.player != null) {
         Vec3 currentPos = target.position();
         if (this.sampleCount >= 2 && !(this.smoothedVelocity.horizontalDistanceSqr() < 1.0E-4)) {
            double distance = (double)MC.player.distanceTo(target);
            double playerSpeed = MC.player.getDeltaMovement().length();
            double targetSpeed = this.smoothedVelocity.horizontalDistance();

            double ticksToReach = switch (stage) {
               case FLYING_UP -> (Math.abs(MC.player.getY() - target.getY()) + distance) / Math.max(playerSpeed * 20.0, 1.0) * 1.2;
               case TARGETTING -> distance / Math.max(playerSpeed * 20.0, 0.8);
               case ATTACKING -> distance / Math.max(playerSpeed * 20.0, 1.5) * 0.8;
               default -> distance / 2.0;
            };
            ticksToReach = Math.min(ticksToReach, 40.0);
            double leadMultiplier = targetSpeed > 0.4 ? 1.5 : (targetSpeed > 0.2 ? 1.3 : 1.0);
            return currentPos.add(this.smoothedVelocity.scale(ticksToReach * leadMultiplier));
         } else {
            return currentPos;
         }
      } else {
         return Vec3.ZERO;
      }
   }

   public boolean isMoving() {
      return this.smoothedVelocity.horizontalDistanceSqr() > 0.001;
   }

   public void reset() {
      this.lastPosition = null;
      this.smoothedVelocity = Vec3.ZERO;
      this.lastUpdateTime = 0L;
      this.sampleCount = 0;
   }
}
