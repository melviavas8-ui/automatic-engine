package daamky.ua.api.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import daamky.ua.api.events.CancellableEvent;
import net.minecraft.world.InteractionHand;

public final class HandAnimationEvent extends CancellableEvent {
   private final PoseStack matrices;
   private final InteractionHand hand;
   private final float swingProgress;

   public HandAnimationEvent(PoseStack matrices, InteractionHand hand, float swingProgress) {
      this.matrices = matrices;
      this.hand = hand;
      this.swingProgress = swingProgress;
   }

   public PoseStack getMatrices() {
      return this.matrices;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public float getSwingProgress() {
      return this.swingProgress;
   }
}
