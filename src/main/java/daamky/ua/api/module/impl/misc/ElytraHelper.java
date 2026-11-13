package daamky.ua.api.module.impl.misc;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import daamky.ua.api.drag.impl.Notifications;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.HotBarScrollEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.mixin.accessor.ItemCooldownInstanceAccessor;
import daamky.ua.mixin.accessor.ItemCooldownsAccessor;
import daamky.ua.utils.inventory.InventoryFlowManager;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.bind.InventoryBindHelper;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.sounds.SoundManager;

public final class ElytraHelper extends Module {
   private static final int HOTBAR_SELECT_DELAY_TICKS = 0;
   private static final int HOTBAR_RESTORE_DELAY_TICKS = 0;
   private static final int INVENTORY_SELECT_DELAY_TICKS = 0;
   private static final int INVENTORY_RESTORE_DELAY_TICKS = 0;
   private static final List<Item> CHESTPLATES = List.of(
      Items.NETHERITE_CHESTPLATE,
      Items.DIAMOND_CHESTPLATE,
      Items.CHAINMAIL_CHESTPLATE,
      Items.IRON_CHESTPLATE,
      Items.GOLDEN_CHESTPLATE,
      Items.LEATHER_CHESTPLATE
   );
   private final BindSetting swapBind = this.register(new BindSetting("Swap Bind", "Swap elytra/chestplate bind.", KeyBind.NONE));
   private final BindSetting fireworkBind = this.register(new BindSetting("Firework Bind", "Use firework bind.", KeyBind.NONE));
   private final BooleanSetting autoTakeoff = this.register(new BooleanSetting("Auto Takeoff", "Automatically toggles jump for elytra takeoff.", false));
   private final BooleanSetting autoFirework = this.register(new BooleanSetting("Auto Firework", "Automatically uses firework after takeoff.", false));
   private final StopWatch autoFireworkTimer = new StopWatch();
   private final StopWatch fireworkUseTimer = new StopWatch();
   private boolean autoFireworkArmed;
   private boolean lastSwapPressed;
   private boolean lastFireworkPressed;
   private ElytraHelper.FireworkPhase fireworkPhase = ElytraHelper.FireworkPhase.IDLE;
   private int fireworkPhaseTicks;
   private int previousHotbarSlot = -1;
   private int restoreInventorySlot = -1;

   public ElytraHelper() {
      super("Elytra Helper", "Helps with elytra swapping and fireworks.", ModuleCategory.MISC);
      this.autoFirework.visibleWhen(this.autoTakeoff::getValue);
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   @SubscribeEvent
   private void onTick(TickEvent.Pre event) {
      if (mc.player != null && mc.world != null) {
         this.updateFireworkUse();
         boolean swapPressed = InventoryBindHelper.isHeld(mc, this.swapBind);
         boolean fireworkPressed = InventoryBindHelper.isHeld(mc, this.fireworkBind);
         if (mc.currentScreen != null) {
            this.lastSwapPressed = swapPressed;
            this.lastFireworkPressed = fireworkPressed;
         } else {
            if (!this.lastSwapPressed && swapPressed && !this.isBusy()) {
               this.startArmorSwap();
            }

            if (!this.lastFireworkPressed && fireworkPressed && !this.isBusy()) {
               this.useFirework();
            }

            this.processAutoFirework();
            this.lastSwapPressed = swapPressed;
            this.lastFireworkPressed = fireworkPressed;
         }
      } else {
         this.resetState();
      }
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      if (mc.player != null) {
         if (this.autoTakeoff.getValue() && InventoryUtils.isElytraEquipped() && InventoryUtils.isElytraUsable()) {
            event.setJumping(mc.player.age % 2 == 0);
            if (!mc.player.isGliding()) {
               this.autoFireworkTimer.reset();
            }
         }
      }
   }

   @SubscribeEvent
   private void onScroll(HotBarScrollEvent event) {
      if (mc.player != null && mc.world != null && mc.currentScreen == null && !this.isBusy()) {
         if (InventoryBindHelper.matchesScroll(event, this.swapBind)) {
            event.setCancelled(true);
            this.startArmorSwap();
         }

         if (InventoryBindHelper.matchesScroll(event, this.fireworkBind)) {
            event.setCancelled(true);
            this.useFirework();
         }
      }
   }

   private void startArmorSwap() {
      Slot slot = this.findChestSwapSlot();
      boolean swappingToElytra = !InventoryUtils.isElytraEquipped();
      String itemName = swappingToElytra ? "Элитра" : "Нагрудиник";
      if (slot == null) {
         this.notifyItemMissing(itemName);
      } else {
         InventoryTask.moveItem(slot, 6, this.shouldQueueSwap(), true);
         this.notifyItemUsed(itemName, false);
         if (swappingToElytra) {
            this.armAutoFirework();
         } else {
            this.autoFireworkArmed = false;
         }
      }
   }

   private Slot findChestSwapSlot() {
      return InventoryUtils.isElytraEquipped() ? InventoryTask.getSlot(CHESTPLATES) : InventoryTask.getSlot(Items.ELYTRA);
   }

   private void notifyItemUsed(String itemName, boolean sound) {
      this.notifyElytraHelper(itemName + " использован", sound);
   }

   private void notifyItemMissing(String itemName) {
      this.notifyElytraHelper(itemName + " Не найдено", true);
   }

   private void notifyItemCooldown(String itemName, ItemStack stack) {
      this.notifyElytraHelper(itemName + "будет доступен через" + this.formatCooldownDurationTicks(this.cooldownRemainingTicks(stack)), true);
   }

   private void notifyElytraHelper(String text, boolean sound) {
      Notifications.push("Elytra Helper", text, 4000L, sound ? SoundManager.NOTIFICATION : null);
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

   private void useFirework() {
      if (mc.player != null && InventoryUtils.isElytraEquipped()) {
         if (this.fireworkUseTimer.finished(1.0)) {
            ItemStack fireworkStack = Items.FIREWORK_ROCKET.getDefaultStack();
            if (mc.player.getItemCooldownManager().isCoolingDown(fireworkStack)) {
               this.notifyItemCooldown("Фейерверк", fireworkStack);
            } else {
               this.previousHotbarSlot = mc.player.getInventory().getSelectedSlot();
               this.restoreInventorySlot = -1;
               if (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) {
                  this.startFireworkPhase(ElytraHelper.FireworkPhase.USE, 0);
               } else {
                  int hotbarSlot = InventoryTask.findHotbarSlot(Items.FIREWORK_ROCKET);
                  if (hotbarSlot != -1) {
                     InventoryUtils.syncSelectedHotbarSlot(hotbarSlot);
                     this.startFireworkPhase(ElytraHelper.FireworkPhase.WAIT_SELECTED, 0);
                  } else {
                     int inventorySlot = InventoryTask.findInventorySlot(Items.FIREWORK_ROCKET);
                     if (inventorySlot != -1) {
                        this.restoreInventorySlot = inventorySlot;
                        this.queueFireworkSwap(inventorySlot, this.previousHotbarSlot);
                        this.startFireworkPhase(ElytraHelper.FireworkPhase.WAIT_SWAP, 0);
                     } else {
                        this.notifyItemMissing("Фейерверк");
                     }
                  }
               }
            }
         }
      }
   }

   private void updateFireworkUse() {
      if (this.fireworkPhase != ElytraHelper.FireworkPhase.IDLE) {
         if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) {
            this.resetFireworkUse();
         } else if (this.fireworkPhaseTicks > 0) {
            this.fireworkPhaseTicks--;
         } else {
            switch (this.fireworkPhase) {
               case WAIT_SWAP:
                  if (!InventoryFlowManager.isIdle()) {
                     return;
                  }

                  this.startFireworkPhase(ElytraHelper.FireworkPhase.WAIT_SELECTED, 0);
                  break;
               case WAIT_SELECTED:
                  this.startFireworkPhase(ElytraHelper.FireworkPhase.USE, 0);
                  break;
               case USE:
                  if (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) {
                     mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                     mc.player.swingHand(Hand.MAIN_HAND);
                     this.notifyItemUsed("Фейерверк", false);
                     this.fireworkUseTimer.reset();
                  }

                  this.startFireworkPhase(ElytraHelper.FireworkPhase.RESTORE, this.restoreInventorySlot != -1 ? 0 : 0);
                  break;
               case RESTORE:
                  if (this.restoreInventorySlot != -1) {
                     this.queueFireworkSwap(this.restoreInventorySlot, this.previousHotbarSlot);
                     this.startFireworkPhase(ElytraHelper.FireworkPhase.WAIT_RESTORE, 0);
                  } else if (this.previousHotbarSlot >= 0 && this.previousHotbarSlot <= 8) {
                     InventoryUtils.syncSelectedHotbarSlot(this.previousHotbarSlot);
                     this.resetFireworkUse();
                  } else {
                     this.resetFireworkUse();
                  }
                  break;
               case WAIT_RESTORE:
                  if (!InventoryFlowManager.isIdle()) {
                     return;
                  }

                  this.resetFireworkUse();
                  break;
               default:
                  this.resetFireworkUse();
            }
         }
      }
   }

   private void startFireworkPhase(ElytraHelper.FireworkPhase phase, int ticks) {
      this.fireworkPhase = phase;
      this.fireworkPhaseTicks = Math.max(0, ticks);
   }

   private void queueFireworkSwap(int inventorySlot, int hotbarSlot) {
      InventoryFlowManager.addTask(() -> InventoryTask.swap(inventorySlot, hotbarSlot, false));
   }

   private void resetFireworkUse() {
      this.fireworkPhase = ElytraHelper.FireworkPhase.IDLE;
      this.fireworkPhaseTicks = 0;
      this.previousHotbarSlot = -1;
      this.restoreInventorySlot = -1;
   }

   private void processAutoFirework() {
      if (mc.player != null) {
         if (!this.autoTakeoff.getValue() || !this.autoFirework.getValue()) {
            this.autoFireworkArmed = false;
         } else if (this.autoFireworkArmed && mc.currentScreen == null && !this.isBusy()) {
            if (InventoryUtils.isElytraEquipped() && mc.player.isGliding() && this.autoFireworkTimer.finished(75.0)) {
               this.useFirework();
               this.autoFireworkArmed = false;
               this.autoFireworkTimer.reset();
            }
         }
      }
   }

   private void armAutoFirework() {
      this.autoFireworkArmed = this.autoTakeoff.getValue() && this.autoFirework.getValue();
      this.autoFireworkTimer.reset();
   }

   private boolean shouldQueueSwap() {
      return !InventoryTask.isMoveMode("ReallyWorld");
   }

   private boolean isBusy() {
      return !InventoryFlowManager.script.isFinished()
         || !InventoryFlowManager.postScript.isFinished()
         || !InventoryTask.isSwapAndUseIdle()
         || this.fireworkPhase != ElytraHelper.FireworkPhase.IDLE;
   }

   private void resetState() {
      this.autoFireworkArmed = false;
      this.lastSwapPressed = false;
      this.lastFireworkPressed = false;
      this.autoFireworkTimer.reset();
      this.fireworkUseTimer.reset();
      this.resetFireworkUse();
   }

   private static enum FireworkPhase {
      IDLE,
      WAIT_SWAP,
      WAIT_SELECTED,
      USE,
      RESTORE,
      WAIT_RESTORE;

      private FireworkPhase() {
      }
   }
}
