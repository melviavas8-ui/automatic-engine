package daamky.ua.utils.inventory.lookup;

import net.minecraft.item.ItemStack;

@FunctionalInterface
public interface ItemSearcher {
   boolean matches(ItemStack var1);
}
