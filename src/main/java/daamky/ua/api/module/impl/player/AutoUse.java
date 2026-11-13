package daamky.ua.api.module.impl.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.utils.inventory.InventoryFlowManager;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;

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
      MinecraftClient client = event.getClient();
      if (client.player == null || client.world == null || client.options == null) {
         this.finishUsing(false);
      } else if (this.stopping) {
         this.updateStopping();
      } else if (this.using) {
         this.enforceFullStop();
         this.updateUsing(client);
      } else if (!this.targets.isSelected("Eat")
         || !client.player.getHungerManager().isNotFull()
         || !this.prepareUsing(InventoryTask.getFoodMaxSaturationSlot())) {
         if (this.targets.isSelected("Invisible") && !PlayerInteractionHelper.isPotionActive(StatusEffects.INVISIBILITY)) {
            this.prepareUsing(InventoryTask.getPotion(StatusEffects.INVISIBILITY));
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
         ItemStack stack = slot.getStack();
         if (!stack.isEmpty() && !mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            ItemStack offhand = mc.player.getOffHandStack();
            if (!ItemStack.areItemsAndComponentsEqual(offhand, stack)) {
               InventoryTask.swapHand(slot, Hand.OFF_HAND, false, true);
               this.restoreSlot = slot;
            } else {
               this.restoreSlot = null;
            }

            this.using = true;
            this.useStarted = false;
            this.useStartTicks = 0;
            this.enforceFullStop();
            mc.options.useKey.setPressed(true);
            PlayerInteractionHelper.interactItem(Hand.OFF_HAND);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void updateUsing(MinecraftClient client) {
      this.useStartTicks++;
      if (client.player.isUsingItem()) {
         this.useStarted = true;
         client.options.useKey.setPressed(true);
      } else if (!this.useStarted && this.useStartTicks < 4) {
         client.options.useKey.setPressed(true);
      } else {
         this.finishUsing(true);
      }
   }

   private boolean validSlot(Slot slot) {
      if (mc.player != null && slot != null) {
         ItemStack stack = slot.getStack();
         return !stack.isEmpty() && !mc.player.getItemCooldownManager().isCoolingDown(stack);
      } else {
         return false;
      }
   }

   private void enforceFullStop() {
      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.sneakKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
      }

      if (mc.player != null) {
         mc.player.setSprinting(false);
         mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
      }
   }

   private void finishUsing(boolean restoreItem) {
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

      if (restoreItem && this.restoreSlot != null && mc.player != null) {
         InventoryTask.swapHand(this.restoreSlot, Hand.OFF_HAND, false, true);
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
