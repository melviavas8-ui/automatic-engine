package daamky.ua.api.module.impl.player;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.utils.inventory.InventoryFlowManager;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AutoUse extends Module {
   private static final int PRE_USE_STOP_TICKS = 3;
   private static final int USE_START_TIMEOUT_TICKS = 4;
   private final MultiModeSetting targets = this.register(
      new MultiModeSetting("Targets", "Items to use automatically.", new String[]{"Eat", "Invisible"}, "Eat", "Invisible")
   );
   private Slot restoreSlot;
   private Slot pendingSlot;
   private boolean stopping;
   private boolean using;
   private boolean useStarted;
   private int stopTicks;
   private int useStartTicks;

   public AutoUse() {
      super("Auto Use", "Automatically eats food and drinks an invisibility potion.", ModuleCategory.PLAYER);
   }

   @Override
   protected void onEnable() {
      this.resetState();
   }

   @Override
   protected void onDisable() {
      this.finishUsing(true);
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      if (this.stopping || this.using) {
         event.inputNone();
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      Minecraft client = event.getClient();
      if (client.player == null || client.level == null || client.options == null) {
         this.finishUsing(false);
      } else if (this.stopping) {
         this.updateStopping();
      } else if (this.using) {
         this.enforceFullStop();
         this.updateUsing(client);
      } else if (!this.targets.isSelected("Eat")
         || !client.player.getFoodData().needsFood()
         || !this.prepareUsing(InventoryTask.getFoodMaxSaturationSlot())) {
         if (this.targets.isSelected("Invisible") && !PlayerInteractionHelper.isPotionActive(MobEffects.INVISIBILITY)) {
            this.prepareUsing(InventoryTask.getPotion(MobEffects.INVISIBILITY));
         }
      }
   }

   private boolean prepareUsing(Slot slot) {
      if (this.validSlot(slot) && InventoryFlowManager.isIdle()) {
         this.pendingSlot = slot;
         this.stopping = true;
         this.stopTicks = 0;
         this.enforceFullStop();
         return true;
      } else {
         return false;
      }
   }

   private void updateStopping() {
      this.enforceFullStop();
      this.stopTicks++;
      if (this.stopTicks >= 3) {
         Slot slot = this.pendingSlot;
         this.pendingSlot = null;
         this.stopping = false;
         if (!this.startUsing(slot)) {
            this.resetState();
            InventoryFlowManager.updateMoveKeys();
         }
      }
   }

   private boolean startUsing(Slot slot) {
      if (mc.player != null && mc.options != null && slot != null) {
         ItemStack stack = slot.getItem();
         if (!stack.isEmpty() && !mc.player.getCooldowns().isOnCooldown(stack)) {
            ItemStack offhand = mc.player.getOffhandItem();
            if (!ItemStack.isSameItemSameComponents(offhand, stack)) {
               InventoryTask.swapHand(slot, InteractionHand.OFF_HAND, false, true);
               this.restoreSlot = slot;
            } else {
               this.restoreSlot = null;
            }

            this.using = true;
            this.useStarted = false;
            this.useStartTicks = 0;
            this.enforceFullStop();
            mc.options.keyUse.setDown(true);
            PlayerInteractionHelper.interactItem(InteractionHand.OFF_HAND);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void updateUsing(Minecraft client) {
      this.useStartTicks++;
      if (client.player.isUsingItem()) {
         this.useStarted = true;
         client.options.keyUse.setDown(true);
      } else if (!this.useStarted && this.useStartTicks < 4) {
         client.options.keyUse.setDown(true);
      } else {
         this.finishUsing(true);
      }
   }

   private boolean validSlot(Slot slot) {
      if (mc.player != null && slot != null) {
         ItemStack stack = slot.getItem();
         return !stack.isEmpty() && !mc.player.getCooldowns().isOnCooldown(stack);
      } else {
         return false;
      }
   }

   private void enforceFullStop() {
      if (mc.options != null) {
         mc.options.keyUp.setDown(false);
         mc.options.keyDown.setDown(false);
         mc.options.keyLeft.setDown(false);
         mc.options.keyRight.setDown(false);
         mc.options.keyJump.setDown(false);
         mc.options.keyShift.setDown(false);
         mc.options.keySprint.setDown(false);
      }

      if (mc.player != null) {
         mc.player.setSprinting(false);
         mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
      }
   }

   private void finishUsing(boolean restoreItem) {
      if (mc.options != null) {
         mc.options.keyUse.setDown(false);
      }

      if (restoreItem && this.restoreSlot != null && mc.player != null) {
         InventoryTask.swapHand(this.restoreSlot, InteractionHand.OFF_HAND, false, true);
      }

      this.resetState();
      InventoryFlowManager.updateMoveKeys();
   }

   private void resetState() {
      this.restoreSlot = null;
      this.pendingSlot = null;
      this.stopping = false;
      this.using = false;
      this.useStarted = false;
      this.stopTicks = 0;
      this.useStartTicks = 0;
   }
}
