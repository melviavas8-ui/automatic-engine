package daamky.ua.api.events.impl;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import daamky.ua.api.events.Event;

public final class ItemRendererEvent implements Event {
   private AbstractClientPlayerEntity player;
   private ItemStack stack;
   private Hand hand;

   public ItemRendererEvent(AbstractClientPlayerEntity player, ItemStack stack, Hand hand) {
      this.player = player;
      this.stack = stack;
      this.hand = hand;
   }

   public AbstractClientPlayerEntity getPlayer() {
      return this.player;
   }

   public void setPlayer(AbstractClientPlayerEntity player) {
      this.player = player;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public void setStack(ItemStack stack) {
      this.stack = stack;
   }

   public Hand getHand() {
      return this.hand;
   }

   public void setHand(Hand hand) {
      this.hand = hand;
   }
}
