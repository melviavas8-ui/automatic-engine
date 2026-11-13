package daamky.ua.api.events.impl;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import daamky.ua.api.events.CancellableEvent;

public final class HandAnimationEvent extends CancellableEvent {
   private final MatrixStack matrices;
   private final Hand hand;
   private final float swingProgress;

   public HandAnimationEvent(MatrixStack matrices, Hand hand, float swingProgress) {
      this.matrices = matrices;
      this.hand = hand;
      this.swingProgress = swingProgress;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public Hand getHand() {
      return this.hand;
   }

   public float getSwingProgress() {
      return this.swingProgress;
   }
}
