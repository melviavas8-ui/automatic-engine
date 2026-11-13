package daamky.ua.api.module.impl.combat.macetarget.prediction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.macetarget.state.MaceState;

public class TargetPredictor {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static final int MIN_SAMPLES = 2;
   private static final double SMOOTHING = 0.6;
   private Vec3d lastPosition;
   private Vec3d smoothedVelocity = Vec3d.ZERO;
   private long lastUpdateTime;
   private int sampleCount;

   public TargetPredictor() {
   }

   public void update(LivingEntity target) {
      if (target == null) {
         this.reset();
      } else {
         Vec3d currentPos = target.getEntityPos();
         long currentTime = System.currentTimeMillis();
         if (this.lastPosition != null && this.lastUpdateTime > 0L) {
            long deltaTime = currentTime - this.lastUpdateTime;
            if (deltaTime > 0L && deltaTime < 500L) {
               Vec3d velocity = currentPos.subtract(this.lastPosition);
               this.smoothedVelocity = this.smoothedVelocity.multiply(0.6).add(velocity.multiply(0.4));
               this.sampleCount++;
            }
         }

         this.lastPosition = currentPos;
         this.lastUpdateTime = currentTime;
      }
   }

   public Vec3d getPredictedPosition(LivingEntity target, MaceState.Stage stage) {
      if (target != null && MC.player != null) {
         Vec3d currentPos = target.getEntityPos();
         if (this.sampleCount >= 2 && !(this.smoothedVelocity.horizontalLengthSquared() < 1.0E-4)) {
            double distance = (double)MC.player.distanceTo(target);
            double playerSpeed = MC.player.getVelocity().length();
            double targetSpeed = this.smoothedVelocity.horizontalLength();

            double ticksToReach = switch (stage) {
               case FLYING_UP -> (Math.abs(MC.player.getY() - target.getY()) + distance) / Math.max(playerSpeed * 20.0, 1.0) * 1.2;
               case TARGETTING -> distance / Math.max(playerSpeed * 20.0, 0.8);
               case ATTACKING -> distance / Math.max(playerSpeed * 20.0, 1.5) * 0.8;
               default -> distance / 2.0;
            };
            ticksToReach = Math.min(ticksToReach, 40.0);
            double leadMultiplier = targetSpeed > 0.4 ? 1.5 : (targetSpeed > 0.2 ? 1.3 : 1.0);
            return currentPos.add(this.smoothedVelocity.multiply(ticksToReach * leadMultiplier));
         } else {
            return currentPos;
         }
      } else {
         return Vec3d.ZERO;
      }
   }

   public boolean isMoving() {
      return this.smoothedVelocity.horizontalLengthSquared() > 0.001;
   }

   public void reset() {
      this.lastPosition = null;
      this.smoothedVelocity = Vec3d.ZERO;
      this.lastUpdateTime = 0L;
      this.sampleCount = 0;
   }
}
