package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;
import net.minecraft.world.entity.player.Player;

public final class JumpEvent extends CancellableEvent {
   private final Player player;
   private double motionMultiplier = 1.0;

   public JumpEvent(Player player) {
      this.player = player;
   }

   public Player getPlayer() {
      return this.player;
   }

   public double getMotionMultiplier() {
      return this.motionMultiplier;
   }

   public void setMotionMultiplier(double motionMultiplier) {
      this.motionMultiplier = Math.max(0.0, motionMultiplier);
   }
}
