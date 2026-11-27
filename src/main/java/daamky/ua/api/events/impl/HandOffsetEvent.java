package daamky.ua.api.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import daamky.ua.api.events.CancellableEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class HandOffsetEvent extends CancellableEvent {
   private final PoseStack matrices;
   private final ItemStack stack;
   private final InteractionHand hand;
   private float scale = 1.0F;

   public HandOffsetEvent(PoseStack matrices, ItemStack stack, InteractionHand hand) {
      this.matrices = matrices;
      this.stack = stack;
      this.hand = hand;
   }

   public PoseStack getMatrices() {
      return this.matrices;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public float getScale() {
      return this.scale;
   }

   public void setScale(float scale) {
      this.scale = scale;
   }
}
