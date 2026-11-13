package daamky.ua.api.module.impl.combat.macetarget.attack;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.lookup.InventoryUtils;

public class AttackHandler {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private boolean pendingAttack;
   private boolean shouldDisableAfterAttack;
   private boolean swappedToMace;
   private int previousSlot = -1;
   private boolean restoreQueued;

   public AttackHandler() {
   }

   public boolean isPendingAttack() {
      return this.pendingAttack;
   }

   public void setPendingAttack(boolean pendingAttack) {
      this.pendingAttack = pendingAttack;
   }

   public boolean isShouldDisableAfterAttack() {
      return this.shouldDisableAfterAttack;
   }

   public void setShouldDisableAfterAttack(boolean shouldDisableAfterAttack) {
      this.shouldDisableAfterAttack = shouldDisableAfterAttack;
   }

   public void performAttack(LivingEntity target) {
      if (MC.player != null && MC.interactionManager != null && target != null && this.prepareMaceSwap()) {
         MC.interactionManager.attackEntity(MC.player, target);
         this.queueRestoreAfterAttack();
      }
   }

   public boolean prepareMaceSwap() {
      if (MC.player == null) {
         return false;
      } else {
         int maceSlot = InventoryUtils.findHotbarItem(Items.MACE);
         if (maceSlot == -1) {
            return false;
         } else {
            int selectedSlot = MC.player.getInventory().getSelectedSlot();
            if (selectedSlot == maceSlot) {
               return true;
            } else {
               if (!this.swappedToMace) {
                  this.previousSlot = selectedSlot;
                  this.swappedToMace = true;
               }

               InventoryTask.switchTo(maceSlot);
               return true;
            }
         }
      }
   }

   public void processLoop() {
      if (this.restoreQueued) {
         this.restorePreviousSlot();
      }
   }

   public void queueRestoreAfterAttack() {
      if (this.swappedToMace && this.previousSlot != -1) {
         this.restoreQueued = true;
      }
   }

   public boolean isSwappedToMace() {
      return this.swappedToMace;
   }

   public boolean hasMaceInHotbar() {
      return InventoryUtils.findHotbarItem(Items.MACE) != -1;
   }

   public void reset() {
      this.restorePreviousSlot();
      this.pendingAttack = false;
      this.shouldDisableAfterAttack = false;
   }

   private void restorePreviousSlot() {
      if (MC.player != null && this.swappedToMace && this.previousSlot >= 0 && this.previousSlot <= 8) {
         InventoryTask.switchTo(this.previousSlot);
      }

      this.swappedToMace = false;
      this.previousSlot = -1;
      this.restoreQueued = false;
   }
}
