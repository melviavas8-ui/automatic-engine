package daamky.ua.api.events.impl;

import net.minecraft.entity.player.PlayerEntity;
import daamky.ua.api.events.CancellableEvent;

public final class JumpEvent extends CancellableEvent {
   private final PlayerEntity player;
   private double motionMultiplier = 1.0;

   public JumpEvent(PlayerEntity player) {
      this.player = player;
   }

   public PlayerEntity getPlayer() {
      return this.player;
   }

   public double getMotionMultiplier() {
      return this.motionMultiplier;
   }

   public void setMotionMultiplier(double motionMultiplier) {
      this.motionMultiplier = Math.max(0.0, motionMultiplier);
   }
}
