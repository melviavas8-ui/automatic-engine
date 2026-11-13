package daamky.ua.api.module.impl.player;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.network.Network;

public final class SpConnect extends Module {
   private static final int OPEN_COMPASS_DELAY_TICKS = 5;
   private static final int RETRY_DELAY_TICKS = 6;
   private boolean hadCompass;
   private boolean disableQueued;
   private int compassCooldown;

   public SpConnect() {
      super("SpookyJoiner", "SpookyJoiner", ModuleCategory.PLAYER);
   }

   @Override
   protected void onEnable() {
      this.hadCompass = this.hasCompass();
      this.compassCooldown = 0;
      this.disableQueued = !this.isOnSpookyTime();
   }

   @Override
   protected void onDisable() {
      this.hadCompass = false;
      this.disableQueued = false;
      this.compassCooldown = 0;
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (this.disableQueued) {
         this.setEnabled(false);
      } else if (client.player != null && client.world != null) {
         boolean hasCompassNow = this.hasCompass();
         if (this.hadCompass && !hasCompassNow) {
            this.setEnabled(false);
         } else {
            this.hadCompass = hasCompassNow;
            if (this.compassCooldown > 0) {
               this.compassCooldown--;
            } else {
               Screen screen = client.currentScreen;
               if (screen instanceof GenericContainerScreen containerScreen) {
                  boolean foundTarget = false;

                  for (Slot slot : ((GenericContainerScreenHandler)containerScreen.getScreenHandler()).slots) {
                     ItemStack stack = slot.getStack();
                     if (stack.isOf(Items.NETHERITE_SWORD)) {
                        InventoryTask.clickSlot(slot.id, 0, SlotActionType.PICKUP, false);
                        this.compassCooldown = 6;
                        foundTarget = true;
                        break;
                     }
                  }

                  if (!foundTarget) {
                     InventoryTask.closeScreen(false);
                     this.compassCooldown = 6;
                  }
               } else if (screen != null) {
                  InventoryTask.closeScreen(false);
                  this.compassCooldown = 6;
               } else {
                  this.selectCompass();
                  this.compassCooldown = 5;
                  if (this.hasEmerald()) {
                     this.setEnabled(false);
                  }
               }
            }
         }
      }
   }

   private boolean hasCompass() {
      if (mc.player == null) {
         return false;
      } else {
         for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.COMPASS)) {
               return true;
            }
         }

         return mc.player.getOffHandStack().isOf(Items.COMPASS);
      }
   }

   private boolean hasEmerald() {
      if (mc.player == null) {
         return false;
      } else {
         for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.EMERALD)) {
               return true;
            }
         }

         return mc.player.getOffHandStack().isOf(Items.EMERALD);
      }
   }

   private void selectCompass() {
      if (mc.player != null && mc.interactionManager != null) {
         int hotbarSlot = InventoryTask.getHotbarSlotId(slot -> mc.player.getInventory().getStack(slot).isOf(Items.COMPASS));
         if (hotbarSlot != -1) {
            InventoryUtils.syncSelectedHotbarSlot(hotbarSlot);
            this.useCompass(Hand.MAIN_HAND);
         } else if (mc.player.getOffHandStack().isOf(Items.COMPASS)) {
            this.useCompass(Hand.OFF_HAND);
         } else {
            InventoryTask.selectCompass();
            if (mc.player.getMainHandStack().isOf(Items.COMPASS)) {
               this.useCompass(Hand.MAIN_HAND);
            }
         }
      }
   }

   private void useCompass(Hand hand) {
      if (mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.interactItem(mc.player, hand);
         mc.player.swingHand(hand);
      }
   }

   private boolean isOnSpookyTime() {
      if (Network.isSpookyTime()) {
         return true;
      } else if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null) {
         String address = mc.getNetworkHandler().getServerInfo().address;
         return address != null && address.toLowerCase(Locale.ROOT).contains("spookytime");
      } else {
         return false;
      }
   }
}
