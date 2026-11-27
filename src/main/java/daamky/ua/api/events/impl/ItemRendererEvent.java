package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class ItemRendererEvent implements Event {
   private AbstractClientPlayer player;
   private ItemStack stack;
   private InteractionHand hand;

   public ItemRendererEvent(AbstractClientPlayer player, ItemStack stack, InteractionHand hand) {
      this.player = player;
      this.stack = stack;
      this.hand = hand;
   }

   public AbstractClientPlayer getPlayer() {
      return this.player;
   }

   public void setPlayer(AbstractClientPlayer player) {
      this.player = player;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public void setStack(ItemStack stack) {
      this.stack = stack;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public void setHand(InteractionHand hand) {
      this.hand = hand;
   }
}
