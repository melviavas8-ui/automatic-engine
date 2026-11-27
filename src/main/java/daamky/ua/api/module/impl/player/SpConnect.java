package daamky.ua.api.module.impl.player;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
   public void onTick(Minecraft client) {
      if (this.disableQueued) {
         this.setEnabled(false);
      } else if (client.player != null && client.level != null) {
         boolean hasCompassNow = this.hasCompass();
         if (this.hadCompass && !hasCompassNow) {
            this.setEnabled(false);
         } else {
            this.hadCompass = hasCompassNow;
            if (this.compassCooldown > 0) {
               this.compassCooldown--;
            } else {
               Screen screen = client.screen;
               if (screen instanceof ContainerScreen containerScreen) {
                  boolean foundTarget = false;

                  for (Slot slot : ((ChestMenu)containerScreen.getMenu()).slots) {
                     ItemStack stack = slot.getItem();
                     if (stack.is(Items.NETHERITE_SWORD)) {
                        InventoryTask.clickSlot(slot.index, 0, ClickType.PICKUP, false);
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
         for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            if (mc.player.getInventory().getItem(i).is(Items.COMPASS)) {
               return true;
            }
         }

         return mc.player.getOffhandItem().is(Items.COMPASS);
      }
   }

   private boolean hasEmerald() {
      if (mc.player == null) {
         return false;
      } else {
         for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            if (mc.player.getInventory().getItem(i).is(Items.EMERALD)) {
               return true;
            }
         }

         return mc.player.getOffhandItem().is(Items.EMERALD);
      }
   }

   private void selectCompass() {
      if (mc.player != null && mc.gameMode != null) {
         int hotbarSlot = InventoryTask.getHotbarSlotId(slot -> mc.player.getInventory().getItem(slot).is(Items.COMPASS));
         if (hotbarSlot != -1) {
            InventoryUtils.syncSelectedHotbarSlot(hotbarSlot);
            this.useCompass(InteractionHand.MAIN_HAND);
         } else if (mc.player.getOffhandItem().is(Items.COMPASS)) {
            this.useCompass(InteractionHand.OFF_HAND);
         } else {
            InventoryTask.selectCompass();
            if (mc.player.getMainHandItem().is(Items.COMPASS)) {
               this.useCompass(InteractionHand.MAIN_HAND);
            }
         }
      }
   }

   private void useCompass(InteractionHand hand) {
      if (mc.player != null && mc.gameMode != null) {
         mc.gameMode.useItem(mc.player, hand);
         mc.player.swing(hand);
      }
   }

   private boolean isOnSpookyTime() {
      if (Network.isSpookyTime()) {
         return true;
      } else if (mc.getConnection() != null && mc.getConnection().getServerData() != null) {
         String address = mc.getConnection().getServerData().ip;
         return address != null && address.toLowerCase(Locale.ROOT).contains("spookytime");
      } else {
         return false;
      }
   }
}
