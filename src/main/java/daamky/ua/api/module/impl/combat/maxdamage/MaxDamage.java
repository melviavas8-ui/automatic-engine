package daamky.ua.api.module.impl.combat.maxdamage;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import daamky.ua.IMinecraft;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.player.PlayerInventoryUtil;

public final class MaxDamage implements IMinecraft {
   private MaxDamage() {
   }

   public static boolean runAttack(Runnable attack, String firstItemType, String secondItemType) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         Item firstItem = getItemByType(firstItemType);
         Item secondItem = getItemByType(secondItemType);
         if (firstItem != Items.AIR && secondItem != Items.AIR && firstItem != secondItem) {
            if (mc.player.getOffHandStack().getItem() != firstItem) {
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
      InventoryTask.clickSlot(InventoryUtils.wrapSlot(slot), 40, SlotActionType.SWAP, false);
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
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
               if (!stack.hasGlint()) {
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
