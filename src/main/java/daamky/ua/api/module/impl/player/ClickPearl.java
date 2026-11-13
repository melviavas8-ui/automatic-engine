package daamky.ua.api.module.impl.player;

import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import daamky.ua.api.drag.impl.Notifications;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.mixin.accessor.ItemCooldownInstanceAccessor;
import daamky.ua.mixin.accessor.ItemCooldownsAccessor;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.sounds.SoundManager;

public final class ClickPearl extends Module {
   private static final long PRE_SWAP_DELAY_MS = 35L;
   private static final long PRE_USE_DELAY_MS = 35L;
   private static final long PRE_RESTORE_DELAY_MS = 35L;
   private static final String PEARL_NAME = "Эндер Перл";
   private static ClickPearl instance;
   private final BindSetting keySetting = this.register(new BindSetting("Кнопка", "Кнопка для броска перки.", KeyBind.keyboard(-1)));
   private final ModeSetting swapMode = this.register(new ModeSetting("Метод свапа", "Способ переключения на жемчуг.", "SpookyTime", "SpookyTime", "FunTime"));
   private boolean throwPearl;
   private boolean lastBindDown;
   private long lastUseTime;
   private long actionTimer;
   private int previousSlot = -1;
   private int pendingHotbarSlot = -1;
   private int pendingInventorySlotId = -1;
   private boolean keysOverridden;
   private boolean wasForwardPressed;
   private boolean wasBackPressed;
   private boolean wasLeftPressed;
   private boolean wasRightPressed;
   private boolean wasJumpPressed;
   private ClickPearl.ThrowState throwState = ClickPearl.ThrowState.IDLE;
   private ClickPearl.ThrowMode throwMode = ClickPearl.ThrowMode.NONE;
   private boolean funTimeRecovery;
   private int recoveryInventorySlotId = -1;
   private int recoveryHotbarSlot = -1;
   private Item recoveryHotbarItem;
   private int recoveryAttempts;
   private long recoveryStartedAt;
   private long recoveryLastAttemptAt;

   public ClickPearl() {
      super("ClickPearl", "Click Pearl", ModuleCategory.PLAYER);
      instance = this;
   }

   public static boolean shouldBypassInventoryMove() {
      return instance != null && instance.throwMode == ClickPearl.ThrowMode.INVENTORY && instance.throwState != ClickPearl.ThrowState.IDLE;
   }

   @Override
   protected void onDisable() {
      this.throwPearl = false;
      this.lastUseTime = 0L;
      this.lastBindDown = false;
      this.restoreMovement();
      this.resetThrowState();
      this.clearFunTimeRecovery();
   }

   @SubscribeEvent
   private void onPreTick(TickEvent.Pre event) {
      MinecraftClient client = event.getClient();
      if (mc.player != null && mc.world != null) {
         this.updateBindState(client);
         this.processFunTimeRecovery();
         if (this.throwState != ClickPearl.ThrowState.IDLE) {
            this.maintainMovementStop();
            this.processThrow();
         } else if (this.throwPearl && mc.currentScreen == null) {
            if (System.currentTimeMillis() - this.lastUseTime >= 200L) {
               this.throwPearl = false;
               ItemStack pearlStack = this.cooldownPearlStack();
               if (mc.player.getItemCooldownManager().isCoolingDown(pearlStack)) {
                  this.notifyItemCooldown("Эндер Перл", pearlStack);
               } else if (mc.player.getOffHandStack().getItem() == Items.ENDER_PEARL) {
                  this.startThrow(ClickPearl.ThrowMode.OFFHAND, -1, -1);
               } else {
                  int hotbarSlot = this.findPearlInHotbar();
                  if (hotbarSlot != -1) {
                     this.startThrow(ClickPearl.ThrowMode.HOTBAR, hotbarSlot, -1);
                     this.notifyItemUsed("Эндер Перл", false);
                  } else {
                     int inventorySlot = this.findPearlInInventory();
                     if (inventorySlot != -1) {
                        this.startThrow(ClickPearl.ThrowMode.INVENTORY, -1, InventoryUtils.wrapSlot(inventorySlot));
                     } else {
                        this.notifyItemMissing("Эндер Перл");
                     }
                  }
               }
            }
         }
      } else {
         this.resetThrowState();
         this.clearFunTimeRecovery();
      }
   }

   private void updateBindState(MinecraftClient client) {
      if (client.getWindow() != null && client.currentScreen == null) {
         boolean bindDown = this.keySetting.getValue().isDown(client.getWindow().getHandle());
         if (bindDown && !this.lastBindDown) {
            this.throwPearl = true;
         }

         this.lastBindDown = bindDown;
      } else {
         this.lastBindDown = false;
      }
   }

   private void startThrow(ClickPearl.ThrowMode mode, int hotbarSlot, int inventorySlotId) {
      this.throwMode = mode;
      this.previousSlot = mc.player.getInventory().getSelectedSlot();
      this.pendingHotbarSlot = hotbarSlot;
      this.pendingInventorySlotId = inventorySlotId;
      this.wasForwardPressed = mc.options.forwardKey.isPressed();
      this.wasBackPressed = mc.options.backKey.isPressed();
      this.wasLeftPressed = mc.options.leftKey.isPressed();
      this.wasRightPressed = mc.options.rightKey.isPressed();
      this.wasJumpPressed = mc.options.jumpKey.isPressed();
      this.keysOverridden = true;
      this.maintainMovementStop();
      if (mc.player.isSprinting()) {
         mc.player.setSprinting(false);
      }

      this.throwState = ClickPearl.ThrowState.WAIT_BEFORE_SWAP;
      this.actionTimer = System.currentTimeMillis();
   }

   private int findPearlInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
            return i;
         }
      }

      return -1;
   }

   private int findPearlInInventory() {
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
            return i;
         }
      }

      return -1;
   }

   private ItemStack cooldownPearlStack() {
      if (mc.player == null) {
         return Items.ENDER_PEARL.getDefaultStack();
      } else if (mc.player.getOffHandStack().isOf(Items.ENDER_PEARL)) {
         return mc.player.getOffHandStack();
      } else {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.ENDER_PEARL)) {
               return stack;
            }
         }

         return Items.ENDER_PEARL.getDefaultStack();
      }
   }

   private void processThrow() {
      if (this.swapMode.is("FunTime")) {
         this.processFunTimeThrow();
      } else {
         switch (this.throwState) {
            case WAIT_BEFORE_SWAP:
               if (System.currentTimeMillis() - this.actionTimer < 35L) {
                  return;
               }

               switch (this.throwMode) {
                  case INVENTORY:
                     this.startInventoryPearlSwap();
                  case OFFHAND:
                  case HOTBAR:
                     this.throwState = ClickPearl.ThrowState.WAIT_BEFORE_USE;
                     this.actionTimer = System.currentTimeMillis();
                     return;
                  default:
                     this.resetThrowState();
                     return;
               }
            case WAIT_BEFORE_USE:
               if (System.currentTimeMillis() - this.actionTimer < 35L) {
                  return;
               }

               switch (this.throwMode) {
                  case OFFHAND:
                     this.useOffhandPearl();
                     break;
                  case HOTBAR:
                     this.startHotbarPearlUse();
                     break;
                  case INVENTORY:
                     this.useInventoryPearl();
                     break;
                  default:
                     this.resetThrowState();
                     return;
               }

               this.lastUseTime = System.currentTimeMillis();
               this.throwState = ClickPearl.ThrowState.WAIT_BEFORE_RESTORE;
               this.actionTimer = System.currentTimeMillis();
               break;
            case WAIT_BEFORE_RESTORE:
               if (System.currentTimeMillis() - this.actionTimer < 35L) {
                  return;
               }

               switch (this.throwMode) {
                  case HOTBAR:
                     this.finishHotbarPearlUse();
                     break;
                  case INVENTORY:
                     this.finishInventoryPearlUse();
               }

               this.restoreMovement();
               this.resetThrowState();
         }
      }
   }

   private void processFunTimeThrow() {
      switch (this.throwState) {
         case WAIT_BEFORE_SWAP:
            switch (this.throwMode) {
               case OFFHAND:
                  break;
               case HOTBAR:
                  if (this.pendingHotbarSlot != this.previousSlot) {
                     InventoryUtils.syncSelectedHotbarSlot(this.pendingHotbarSlot);
                  }
                  break;
               case INVENTORY:
                  this.recoveryHotbarItem = mc.player.getInventory().getStack(this.previousSlot).getItem();
                  this.startInventoryPearlSwap();
                  break;
               default:
                  this.restoreMovement();
                  this.resetThrowState();
                  return;
            }

            this.throwState = ClickPearl.ThrowState.WAIT_BEFORE_USE;
            break;
         case WAIT_BEFORE_USE:
            switch (this.throwMode) {
               case OFFHAND:
                  this.useOffhandPearl();
                  break;
               case HOTBAR:
               case INVENTORY:
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  mc.player.swingHand(Hand.MAIN_HAND);
                  break;
               default:
                  this.restoreMovement();
                  this.resetThrowState();
                  return;
            }

            this.lastUseTime = System.currentTimeMillis();
            this.throwState = ClickPearl.ThrowState.WAIT_BEFORE_RESTORE;
            break;
         case WAIT_BEFORE_RESTORE:
            if (this.throwMode == ClickPearl.ThrowMode.HOTBAR) {
               this.finishHotbarPearlUse();
            } else if (this.throwMode == ClickPearl.ThrowMode.INVENTORY) {
               this.finishInventoryPearlUse();
               this.throwState = ClickPearl.ThrowState.WAIT_BEFORE_CLOSE;
               return;
            }

            this.restoreMovement();
            this.resetThrowState();
            break;
         case WAIT_BEFORE_CLOSE:
            this.startFunTimeRecovery();
            InventoryUtils.closeScreen();
            this.restoreMovement();
            this.resetThrowState();
      }
   }

   private void startFunTimeRecovery() {
      if (this.pendingInventorySlotId != -1 && this.previousSlot != -1 && this.recoveryHotbarItem != null) {
         this.funTimeRecovery = true;
         this.recoveryInventorySlotId = this.pendingInventorySlotId;
         this.recoveryHotbarSlot = this.previousSlot;
         this.recoveryAttempts = 3;
         this.recoveryStartedAt = System.currentTimeMillis();
         this.recoveryLastAttemptAt = this.recoveryStartedAt;
      }
   }

   private void processFunTimeRecovery() {
      if (this.funTimeRecovery && mc.player != null && mc.interactionManager != null) {
         long now = System.currentTimeMillis();
         if (this.recoveryAttempts > 0
            && now - this.recoveryStartedAt < 900L
            && this.recoveryHotbarSlot >= 0
            && this.recoveryHotbarSlot < 9
            && this.recoveryInventorySlotId >= 0
            && this.recoveryInventorySlotId < mc.player.currentScreenHandler.slots.size()) {
            Item hotbarItem = mc.player.getInventory().getStack(this.recoveryHotbarSlot).getItem();
            Item inventoryItem = mc.player.currentScreenHandler.getSlot(this.recoveryInventorySlotId).getStack().getItem();
            if (hotbarItem == this.recoveryHotbarItem) {
               this.clearFunTimeRecovery();
            } else if (inventoryItem == this.recoveryHotbarItem && now - this.recoveryLastAttemptAt >= 150L) {
               InventoryUtils.click(this.recoveryInventorySlotId, this.recoveryHotbarSlot, SlotActionType.SWAP);
               this.recoveryAttempts--;
               this.recoveryLastAttemptAt = now;
            }
         } else {
            this.clearFunTimeRecovery();
         }
      }
   }

   private void clearFunTimeRecovery() {
      this.funTimeRecovery = false;
      this.recoveryInventorySlotId = -1;
      this.recoveryHotbarSlot = -1;
      this.recoveryHotbarItem = null;
      this.recoveryAttempts = 0;
      this.recoveryStartedAt = 0L;
      this.recoveryLastAttemptAt = 0L;
   }

   private void useOffhandPearl() {
      mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
      mc.player.swingHand(Hand.OFF_HAND);
   }

   private void startHotbarPearlUse() {
      this.maintainMovementStop();
      if (this.pendingHotbarSlot != this.previousSlot) {
         InventoryUtils.syncSelectedHotbarSlot(this.pendingHotbarSlot);
      }

      mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   private void finishHotbarPearlUse() {
      this.maintainMovementStop();
      if (this.pendingHotbarSlot != this.previousSlot && this.previousSlot != -1) {
         InventoryUtils.syncSelectedHotbarSlot(this.previousSlot);
      }
   }

   private void startInventoryPearlSwap() {
      if (this.pendingInventorySlotId != -1 && this.previousSlot != -1) {
         this.maintainMovementStop();
         InventoryUtils.click(this.pendingInventorySlotId, this.previousSlot, SlotActionType.SWAP);
      }
   }

   private void notifyItemUsed(String itemName, boolean sound) {
      this.notifyElytraHelper(itemName + " использован", sound);
   }

   private void notifyItemMissing(String itemName) {
      this.notifyElytraHelper(itemName + " Не найдено", true);
   }

   private void notifyItemCooldown(String itemName, ItemStack stack) {
      this.notifyElytraHelper(itemName + " будет доступен через " + this.formatCooldownDurationTicks(this.cooldownRemainingTicks(stack)), true);
   }

   private String formatCooldownDurationTicks(int ticks) {
      if (ticks < 0) {
         return "?";
      } else {
         int totalSeconds = Math.max(1, (int)Math.ceil((double)ticks / 20.0));
         int minutes = totalSeconds / 60;
         int seconds = totalSeconds % 60;
         return minutes > 0 ? minutes + ":" + String.format(Locale.US, "%02d", seconds) : totalSeconds + "s";
      }
   }

   private int cooldownRemainingTicks(ItemStack stack) {
      if (mc.player != null && stack != null && !stack.isEmpty()) {
         ItemCooldownManager cooldowns = mc.player.getItemCooldownManager();

         try {
            Identifier group = cooldowns.getGroup(stack);
            Map<Identifier, ?> activeCooldowns = ((ItemCooldownsAccessor)cooldowns).daamky$getCooldowns();
            if (activeCooldowns.get(group) instanceof ItemCooldownInstanceAccessor accessor) {
               return Math.max(0, accessor.daamky$getEndTime() - ((ItemCooldownsAccessor)cooldowns).daamky$getTickCount());
            }
         } catch (RuntimeException var7) {
         }

         float progress = cooldowns.getCooldownProgress(stack, 0.0F);
         return progress <= 0.0F ? -1 : Math.max(1, Math.round(progress * 20.0F));
      } else {
         return -1;
      }
   }

   private void notifyElytraHelper(String text, boolean sound) {
      Notifications.push("Click Pearl", text, 4000L, sound ? SoundManager.NOTIFICATION : null);
   }

   private void useInventoryPearl() {
      if (this.pendingInventorySlotId != -1 && this.previousSlot != -1) {
         this.maintainMovementStop();
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private void finishInventoryPearlUse() {
      if (this.pendingInventorySlotId != -1 && this.previousSlot != -1) {
         this.maintainMovementStop();
         InventoryUtils.click(this.pendingInventorySlotId, this.previousSlot, SlotActionType.SWAP);
         InventoryUtils.syncSelectedHotbarSlot(this.previousSlot);
      }
   }

   private void maintainMovementStop() {
      mc.options.forwardKey.setPressed(false);
      mc.options.backKey.setPressed(false);
      mc.options.leftKey.setPressed(false);
      mc.options.rightKey.setPressed(false);
      mc.options.jumpKey.setPressed(false);
      mc.options.sprintKey.setPressed(false);
      if (mc.player.isSprinting()) {
         mc.player.setSprinting(false);
      }
   }

   private void restoreMovement() {
      if (this.keysOverridden) {
         mc.options.forwardKey.setPressed(this.wasForwardPressed);
         mc.options.backKey.setPressed(this.wasBackPressed);
         mc.options.leftKey.setPressed(this.wasLeftPressed);
         mc.options.rightKey.setPressed(this.wasRightPressed);
         mc.options.jumpKey.setPressed(this.wasJumpPressed);
         this.keysOverridden = false;
      }
   }

   private void resetThrowState() {
      this.throwState = ClickPearl.ThrowState.IDLE;
      this.throwMode = ClickPearl.ThrowMode.NONE;
      this.actionTimer = 0L;
      this.previousSlot = -1;
      this.pendingHotbarSlot = -1;
      this.pendingInventorySlotId = -1;
      this.keysOverridden = false;
   }

   private static enum ThrowMode {
      NONE,
      OFFHAND,
      HOTBAR,
      INVENTORY;

      private ThrowMode() {
      }
   }

   private static enum ThrowState {
      IDLE,
      WAIT_BEFORE_SWAP,
      WAIT_BEFORE_USE,
      WAIT_BEFORE_RESTORE,
      WAIT_BEFORE_CLOSE;

      private ThrowState() {
      }
   }
}
