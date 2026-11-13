package daamky.ua.api.events.impl;

import net.minecraft.item.ItemStack;
import daamky.ua.api.events.Event;

public final class HeldItemUpdateEvent implements Event {
   private ItemStack mainHand;
   private ItemStack offHand;

   public HeldItemUpdateEvent(ItemStack mainHand, ItemStack offHand) {
      this.mainHand = mainHand;
      this.offHand = offHand;
   }

   public ItemStack getMainHand() {
      return this.mainHand;
   }

   public void setMainHand(ItemStack mainHand) {
      this.mainHand = mainHand;
   }

   public ItemStack getOffHand() {
      return this.offHand;
   }

   public void setOffHand(ItemStack offHand) {
      this.offHand = offHand;
   }
}
