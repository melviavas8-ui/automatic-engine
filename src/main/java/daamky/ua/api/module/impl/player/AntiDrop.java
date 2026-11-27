package daamky.ua.api.module.impl.player;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class AntiDrop extends Module {
   private static AntiDrop instance;
   BooleanSetting shul = this.register(new BooleanSetting("Only with loot", "", true));

   public AntiDrop() {
      super("Anti Drop", "Prevents shulker boxes from being dropped.", ModuleCategory.PLAYER);
      instance = this;
   }

   public static boolean shouldBlock(ItemStack stack) {
      AntiDrop module = instance;
      if (module == null || !module.isEnabled() || !isShulker(stack)) {
         return false;
      } else {
         return !module.shul.getValue() ? true : hasLoot(stack);
      }
   }

   private static boolean isShulker(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
   }

   private static boolean hasLoot(ItemStack stack) {
      ItemContainerContents contents = (ItemContainerContents)stack.get(DataComponents.CONTAINER);
      return contents != null && contents.nonEmptyStream().findAny().isPresent();
   }
}
