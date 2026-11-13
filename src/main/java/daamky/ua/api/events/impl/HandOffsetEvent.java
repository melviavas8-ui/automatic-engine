package daamky.ua.api.events.impl;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import daamky.ua.api.events.CancellableEvent;

public final class HandOffsetEvent extends CancellableEvent {
   private final MatrixStack matrices;
   private final ItemStack stack;
   private final Hand hand;
   private float scale = 1.0F;

   public HandOffsetEvent(MatrixStack matrices, ItemStack stack, Hand hand) {
      this.matrices = matrices;
      this.stack = stack;
      this.hand = hand;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public Hand getHand() {
      return this.hand;
   }

   public float getScale() {
      return this.scale;
   }

   public void setScale(float scale) {
      this.scale = scale;
   }
}
