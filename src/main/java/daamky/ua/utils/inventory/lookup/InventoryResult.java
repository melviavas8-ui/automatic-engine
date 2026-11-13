package daamky.ua.utils.inventory.lookup;

import net.minecraft.item.ItemStack;

public record InventoryResult(int slot, boolean found, ItemStack stack) {
   private static final InventoryResult NOT_FOUND = new InventoryResult(-1, false, ItemStack.EMPTY);

   public static InventoryResult notFound() {
      return NOT_FOUND;
   }

   public static InventoryResult of(int slot, ItemStack stack) {
      return new InventoryResult(slot, true, stack);
   }

   public boolean isHotbar() {
      return this.slot >= 0 && this.slot < 9;
   }

   public int toScreenSlot() {
      return this.slot < 9 ? this.slot + 36 : this.slot;
   }
}
