package daamky.ua.api.module.impl.combat.maxdamage;

import daamky.ua.IMinecraft;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.player.PlayerInventoryUtil;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MaxDamage implements IMinecraft {
   private MaxDamage() {
   }

   public static boolean runAttack(Runnable attack, String firstItemType, String secondItemType) {
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         Item firstItem = getItemByType(firstItemType);
         Item secondItem = getItemByType(secondItemType);
         if (firstItem != Items.AIR && secondItem != Items.AIR && firstItem != secondItem) {
            if (mc.player.getOffhandItem().getItem() != firstItem) {
               return false;
            } else {
               int secondSlot = findConfiguredSlot(secondItem);
               if (secondSlot == -1) {
                  return false;
               } else {
                  PlayerInventoryUtil.swapWithBypassGrim(() -> {
                     swapWithOffhand(secondSlot);
                     attack.run();
                     swapWithOffhand(secondSlot);
                  });
                  return true;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void reset() {
   }

   private static void swapWithOffhand(int slot) {
      InventoryTask.clickSlot(InventoryUtils.wrapSlot(slot), 40, ClickType.SWAP, false);
   }

   private static Item getItemByType(String itemType) {
      return switch (itemType) {
         case "Totem" -> Items.TOTEM_OF_UNDYING;
         case "Head" -> Items.PLAYER_HEAD;
         case "GApple" -> Items.GOLDEN_APPLE;
         case "Shield" -> Items.SHIELD;
         default -> Items.AIR;
      };
   }

   private static int findConfiguredSlot(Item item) {
      if (item != Items.AIR && mc.player != null) {
         int foilFallback = -1;

         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
               if (!stack.hasFoil()) {
                  return i;
               }

               if (foilFallback == -1) {
                  foilFallback = i;
               }
            }
         }

         return foilFallback;
      } else {
         return -1;
      }
   }
}
