package daamky.ua.api.module.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.events.types.EventPriority;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.autoswaputil.AutoSwapItemSelectScreen;
import daamky.ua.api.module.impl.combat.autoswaputil.AutoSwapWheelScreen;
import daamky.ua.api.module.impl.combat.maxdamage.MaxDamageAttribute;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.utils.inventory.InventoryFlowManager;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.swap.SwapExecutor;
import daamky.ua.utils.inventory.swap.SwapSettings;

public class AutoSwap extends Module {
   private static final long AURA_STOP_DELAY_MS = 0L;
   private static final long AURA_ATTACK_RESUME_DELAY_MS = 300L;
   private static AutoSwap instance;
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Swap mode.", "Double", "Double", "Triple"));
   private final ModeSetting firstItem = this.register(new ModeSetting("First Item", "First offhand item.", "Totem", "Totem", "Head", "GApple", "Shield"));
   private final ModeSetting secondItem = this.register(new ModeSetting("Second Item", "Second offhand item.", "Totem", "Totem", "Head", "GApple", "Shield"));
   private final BindSetting swapBind = this.register(new BindSetting("Swap Bind", "Uses AutoSwap without toggling the module.", KeyBind.NONE));
   private final BooleanSetting autoDamage = new BooleanSetting("Auto Damage", "Swaps to max-damage item by conditions.", false);
   private final MultiModeSetting autoDamageConditions = this.register(
      new MultiModeSetting("Conditions", "Auto damage conditions.", new String[]{"Target Low HP", "Target No Sword"}, "Target Low HP")
   );
   private final NumberSetting lowHpThreshold = this.register(new NumberSetting("HP Threshold", "Low target HP threshold.", 10.0, 1.0, 20.0, 1.0));
   private final StringSetting tripleSlotOneId = this.hiddenText("TripleSlotOneId");
   private final StringSetting tripleSlotOneName = this.hiddenText("TripleSlotOneName");
   private final StringSetting tripleSlotTwoId = this.hiddenText("TripleSlotTwoId");
   private final StringSetting tripleSlotTwoName = this.hiddenText("TripleSlotTwoName");
   private final StringSetting tripleSlotThreeId = this.hiddenText("TripleSlotThreeId");
   private final StringSetting tripleSlotThreeName = this.hiddenText("TripleSlotThreeName");
   private final SwapExecutor autoDamageSwapExecutor = new SwapExecutor();
   private final AutoSwap.WheelSlotItem[] wheelSlots = new AutoSwap.WheelSlotItem[3];
   private ItemStack savedOffhandItem = ItemStack.EMPTY;
   private boolean autoDamageActive;
   private boolean swapBindWasDown;
   private boolean autoDamageMovementLocked;
   private boolean auraMovementLocked;
   private boolean auraSwapPending;
   private boolean auraInputStopped;
   private long auraInputStoppedAtMs;
   private Slot auraHitSlot;
   private Runnable auraAttack;
   private long auraAttackBlockedUntilMs;
   private Text lastSwapDisplayName;
   private long lastSwapTimeMs;

   public AutoSwap() {
      super("AutoSwap", "Quickly swaps combat offhand items.", ModuleCategory.COMBAT);
      instance = this;
      this.autoDamageConditions.visibleWhen(this.autoDamage::getValue);
      this.lowHpThreshold.visibleWhen(() -> this.autoDamage.getValue() && this.autoDamageConditions.isSelected("Target Low HP"));
      this.firstItem.visibleWhen(() -> this.mode.is("Double") || this.mode.is("MaxDamage"));
      this.secondItem.visibleWhen(() -> this.mode.is("Double") || this.mode.is("MaxDamage"));
      this.syncWheelSlotsFromSettings();
   }

   public static AutoSwap getInstance() {
      return instance;
   }

   @Override
   public void onTick(MinecraftClient client) {
      this.syncWheelSlotsFromSettings();
      if (client.player != null && client.world != null && client.getWindow() != null) {
         if (this.autoDamageSwapExecutor.isRunning()) {
            this.holdAutoDamageStop();
            this.autoDamageSwapExecutor.tick();
         }

         boolean bindDown = this.swapBind.getValue().isDown(client.getWindow().getHandle());
         if (client.currentScreen != null) {
            this.swapBindWasDown = bindDown;
         } else if (this.mode.is("MaxDamage")) {
            this.swapBindWasDown = bindDown;
         } else {
            if (bindDown && !this.swapBindWasDown) {
               this.useSwap();
            }

            this.swapBindWasDown = bindDown;
            if (this.autoDamage.getValue()) {
               this.handleAutoDamage();
            }
         }
      } else {
         this.cancelAutoDamageSwap();
      }
   }

   @Override
   protected void onDisable() {
      this.cancelAutoDamageSwap();
      this.autoDamageActive = false;
      this.savedOffhandItem = ItemStack.EMPTY;
      this.lastSwapDisplayName = null;
      this.lastSwapTimeMs = 0L;
      this.auraAttackBlockedUntilMs = 0L;
      this.swapBindWasDown = false;
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   private void onInput(InputEvent event) {
      if (this.shouldBlockAutoDamageInput()) {
         event.inputNone();
         this.holdAutoDamageStop();
      } else {
         if (this.shouldBlockAuraInput()) {
            event.setDirectionalLow(false, false, false, false);
            event.setJumping(false);
            event.setSprinting(false);
            this.holdAuraStop();
            if (this.auraSwapPending && !this.auraInputStopped) {
               this.auraInputStopped = true;
               this.auraInputStoppedAtMs = System.currentTimeMillis();
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   private void onRotationUpdate(RotationUpdateEvent event) {
      if (this.shouldBlockAutoDamageInput()) {
         this.holdAutoDamageStop();
      }

      if (this.shouldBlockAuraInput()) {
         this.holdAuraStop();
      }

      if (event.isPre() && this.auraSwapPending && this.auraInputStopped && System.currentTimeMillis() - this.auraInputStoppedAtMs >= 0L) {
         this.runPendingAuraSwap();
      }
   }

   private void useSwap() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null && mc.currentScreen == null) {
         if (!this.mode.is("MaxDamage")) {
            if (this.mode.isSelected("Triple")) {
               mc.setScreen(new AutoSwapWheelScreen(this));
            } else {
               Slot first = this.findConfiguredSlot(this.getItemByType(this.firstItem.getValue()));
               Slot second = this.findConfiguredSlot(this.getItemByType(this.secondItem.getValue()));
               Slot validSlot = first != null && mc.player.getOffHandStack().getItem() != first.getStack().getItem() ? first : second;
               if (validSlot != null) {
                  this.swapToSlot(validSlot);
               }
            }
         }
      }
   }

   private void handleAutoDamage() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null && mc.currentScreen == null) {
         if (!this.autoDamageSwapExecutor.isRunning()) {
            LivingEntity target = AuraModule.target;
            boolean shouldSwapToDamage = false;
            if (target != null && target.isAlive()) {
               boolean lowHpCondition;
               boolean var10000;
               label64: {
                  lowHpCondition = this.autoDamageConditions.isSelected("Target Low HP")
                     && target.getHealth() <= this.lowHpThreshold.getFloat()
                     && target.getHealth() < mc.player.getHealth();
                  if (this.autoDamageConditions.isSelected("Target No Sword")
                     && target instanceof PlayerEntity player
                     && this.getAttackDamage(player.getMainHandStack()) <= 0.0) {
                     var10000 = true;
                     break label64;
                  }

                  var10000 = false;
               }

               boolean noSwordCondition = var10000;
               shouldSwapToDamage = this.autoDamageConditions.getValue().isEmpty() || lowHpCondition || noSwordCondition;
            }

            if (shouldSwapToDamage && !this.autoDamageActive) {
               this.savedOffhandItem = mc.player.getOffHandStack().copy();
               ItemStack bestDamageItem = this.findBestDamageItem();
               if (!bestDamageItem.isEmpty()) {
                  Slot damageSlot = this.findSlotByStack(bestDamageItem);
                  if (damageSlot != null) {
                     this.executeAutoDamageSwap(damageSlot);
                     this.autoDamageActive = true;
                  }
               }
            } else {
               if (!shouldSwapToDamage && this.autoDamageActive) {
                  if (!this.savedOffhandItem.isEmpty()) {
                     Slot restoreSlot = this.findSlotByStack(this.savedOffhandItem);
                     if (restoreSlot != null) {
                        this.executeAutoDamageSwap(restoreSlot);
                     }
                  }

                  this.autoDamageActive = false;
                  this.savedOffhandItem = ItemStack.EMPTY;
               }
            }
         }
      }
   }

   private ItemStack findBestDamageItem() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return ItemStack.EMPTY;
      } else {
         List<ItemStack> damageItems = new ArrayList<>();
         PlayerInventory inventory = mc.player.getInventory();

         for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && (stack.getItem() == Items.PLAYER_HEAD || stack.getItem() == Items.TOTEM_OF_UNDYING) && this.getAttackDamage(stack) > 0.0) {
               damageItems.add(stack);
            }
         }

         return damageItems.stream().max(Comparator.comparingDouble(this::getAttackDamage)).orElse(ItemStack.EMPTY);
      }
   }

   private double getAttackDamage(ItemStack stack) {
      AttributeModifiersComponent modifiers = (AttributeModifiersComponent)stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (modifiers == null) {
         return 0.0;
      } else {
         double totalDamage = 0.0;

         for (Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == EntityAttributes.ATTACK_DAMAGE.value()) {
               totalDamage += entry.modifier().value();
            }
         }

         return totalDamage;
      }
   }

   private void swapToItemStack(ItemStack targetStack) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.currentScreen == null) {
         if (targetStack != null && !targetStack.isEmpty()) {
            Slot foundSlot = this.findSlotByStack(targetStack);
            if (foundSlot != null) {
               this.swapToSlot(foundSlot);
            }
         }
      }
   }

   private void swapToSlot(Slot slot) {
      this.swapToSlot(slot, true);
   }

   private void swapToSlot(Slot slot, boolean task) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.currentScreen == null && mc.player != null && slot != null && slot.hasStack() && this.isValidInventorySlot(slot)) {
         ItemStack swappedTo = slot.getStack().copy();
         mc.player.setSprinting(false);
         this.blockAuraAttack();
         if (task) {
            InventoryFlowManager.addTask(() -> {
               this.blockAuraAttack();
               InventoryTask.swapHand(slot, Hand.OFF_HAND, false, true);
            });
         } else {
            InventoryTask.swapHand(slot, Hand.OFF_HAND, false, true);
         }

         this.triggerSwapToast(swappedTo);
      }
   }

   private void blockAuraAttack() {
      this.auraAttackBlockedUntilMs = Math.max(this.auraAttackBlockedUntilMs, System.currentTimeMillis() + 300L);
   }

   private void executeAutoDamageSwap(Slot slot) {
      this.lockAutoDamageMovement();
      this.autoDamageSwapExecutor
         .execute(
            () -> this.swapToSlot(slot, false),
            SwapSettings.defaults()
               .closeInventory(false)
               .preStopDelay(0, 0)
               .waitStopDelay(250, 350)
               .preSwapDelay(120, 180)
               .postSwapDelay(80, 140)
               .resumeDelay(120, 180),
            this::unlockAutoDamageMovement
         );
   }

   private void cancelAutoDamageSwap() {
      this.autoDamageSwapExecutor.cancel();
      this.clearPendingAuraSwap();
      this.unlockAutoDamageMovement();
      this.unlockAuraMovement();
   }

   private void lockAutoDamageMovement() {
      this.autoDamageMovementLocked = true;
      InventoryFlowManager.disableMoveKeys();
      this.holdAutoDamageStop();
   }

   private void unlockAutoDamageMovement() {
      if (this.autoDamageMovementLocked) {
         this.autoDamageMovementLocked = false;
         InventoryFlowManager.restoreMoveKeysOnly();
      }
   }

   private void holdAutoDamageStop() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.options != null) {
         InventoryFlowManager.disableMoveKeys();
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.sneakKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         if (mc.player != null) {
            mc.player.setSprinting(false);
         }
      }
   }

   private void lockAuraMovement() {
      this.auraMovementLocked = true;
      this.holdAuraStop();
   }

   private void unlockAuraMovement() {
      if (this.auraMovementLocked) {
         this.auraMovementLocked = false;
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.options != null && mc.getWindow() != null) {
            InventoryFlowManager.restoreMoveKeysOnly();
         }
      }
   }

   private void holdAuraStop() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.options != null) {
         mc.options.sprintKey.setPressed(false);
         if (mc.player != null) {
            mc.player.setSprinting(false);
         }
      }
   }

   public void swapBeforeAttack() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null && mc.currentScreen == null) {
         Slot first = this.findConfiguredSlot(this.getItemByType(this.firstItem.getValue()));
         Slot second = this.findConfiguredSlot(this.getItemByType(this.secondItem.getValue()));
         Slot validSlot = first != null && mc.player.getOffHandStack().getItem() != first.getStack().getItem() ? first : second;
         if (validSlot != null) {
            this.swapToSlot(validSlot);
         }
      }
   }

   public boolean runMaxDamageAuraAttack(Runnable attack) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (this.isEnabled()
         && this.mode.is("MaxDamage")
         && attack != null
         && mc.player != null
         && mc.world != null
         && (mc.currentScreen == null || mc.currentScreen instanceof InventoryScreen)) {
         if (this.auraSwapPending) {
            this.holdAuraStop();
            return true;
         } else if (MaxDamageAttribute.isKaratelTalisman(mc.player.getOffHandStack())) {
            attack.run();
            return true;
         } else {
            Slot hitSlot = InventoryTask.getSlot(
               (Predicate<Slot>)(slot -> this.isValidInventorySlot(slot) && MaxDamageAttribute.isKaratelTalisman(slot.getStack()))
            );
            if (hitSlot == null || !hitSlot.hasStack()) {
               return false;
            } else if (mc.currentScreen instanceof InventoryScreen) {
               InventoryTask.swapHand(hitSlot, Hand.OFF_HAND, false, true);

               try {
                  attack.run();
               } finally {
                  InventoryTask.swapHand(hitSlot, Hand.OFF_HAND, false, true);
               }

               return true;
            } else {
               this.lockAuraMovement();
               this.auraSwapPending = true;
               this.auraInputStopped = true;
               this.auraInputStoppedAtMs = System.currentTimeMillis();
               this.auraHitSlot = hitSlot;
               this.auraAttack = attack;
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void runPendingAuraSwap() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null && mc.currentScreen == null && this.auraHitSlot != null && this.auraAttack != null) {
         try {
            this.holdAuraStop();
            this.swapToSlot(this.auraHitSlot, false);
            this.holdAuraStop();
            this.auraAttack.run();
            this.holdAuraStop();
            InventoryTask.swapHand(this.auraHitSlot, Hand.OFF_HAND, false, true);
         } finally {
            this.clearPendingAuraSwap();
            this.unlockAuraMovement();
         }
      } else {
         this.clearPendingAuraSwap();
         this.unlockAuraMovement();
      }
   }

   private void clearPendingAuraSwap() {
      this.auraSwapPending = false;
      this.auraInputStopped = false;
      this.auraInputStoppedAtMs = 0L;
      this.auraHitSlot = null;
      this.auraAttack = null;
   }

   private Slot findSlotByStack(ItemStack targetStack) {
      return InventoryTask.slots()
         .filter(this::isValidInventorySlot)
         .filter(slot -> slot.getStack().getItem() == targetStack.getItem())
         .filter(slot -> slot.getStack().getName().getString().equals(targetStack.getName().getString()))
         .findFirst()
         .or(() -> InventoryTask.slots().filter(this::isValidInventorySlot).filter(slot -> slot.getStack().getItem() == targetStack.getItem()).findFirst())
         .orElse(null);
   }

   private Slot findConfiguredSlot(Item item) {
      return InventoryTask.getSlot(item, Comparator.comparing(slot -> slot.getStack().hasGlint()), this::isValidInventorySlot);
   }

   private boolean isValidInventorySlot(Slot slot) {
      int slotId = InventoryTask.getMenuSlotId(slot);
      return slotId != 45 && slotId != 46;
   }

   private void triggerSwapToast(ItemStack swappedTo) {
      if (swappedTo != null && !swappedTo.isEmpty()) {
         this.lastSwapTimeMs = System.currentTimeMillis();
         this.lastSwapDisplayName = swappedTo.getName();
      }
   }

   public Item getItemByType(String itemType) {
      return switch (itemType) {
         case "Totem" -> Items.TOTEM_OF_UNDYING;
         case "Head" -> Items.PLAYER_HEAD;
         case "GApple" -> Items.GOLDEN_APPLE;
         case "Shield" -> Items.SHIELD;
         default -> Items.AIR;
      };
   }

   public void setWheelSlotItem(int index, Item item, String itemName) {
      if (index >= 0 && index < this.wheelSlots.length) {
         if (item != null && item != Items.AIR) {
            String normalizedName = itemName == null ? "" : itemName;
            this.wheelSlots[index] = new AutoSwap.WheelSlotItem(item, normalizedName);
            this.updateWheelSlotSettings(index, Registries.ITEM.getId(item).toString(), normalizedName);
         } else {
            this.wheelSlots[index] = null;
            this.updateWheelSlotSettings(index, "", "");
         }
      }
   }

   public void startSelectingItem(int wheelSlotIndex) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         mc.setScreen(new AutoSwapItemSelectScreen(this, wheelSlotIndex));
      }
   }

   public ItemStack getWheelSlotStack(int index) {
      this.syncWheelSlotsFromSettings();
      if (index >= 0 && index < this.wheelSlots.length) {
         AutoSwap.WheelSlotItem slotItem = this.wheelSlots[index];
         if (slotItem != null && slotItem.item() != null && slotItem.item() != Items.AIR) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
               return ItemStack.EMPTY;
            } else {
               PlayerInventory inventory = mc.player.getInventory();

               for (int i = 0; i < inventory.size(); i++) {
                  ItemStack stack = inventory.getStack(i);
                  if (!stack.isEmpty() && stack.getItem() == slotItem.item() && stack.getName().getString().equals(slotItem.itemName())) {
                     return stack;
                  }
               }

               if (slotItem.itemName().isEmpty()) {
                  for (int ix = 0; ix < inventory.size(); ix++) {
                     ItemStack stack = inventory.getStack(ix);
                     if (!stack.isEmpty() && stack.getItem() == slotItem.item()) {
                        return stack;
                     }
                  }
               }

               return ItemStack.EMPTY;
            }
         } else {
            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public void startSwapToItemStack(ItemStack stack) {
      this.swapToItemStack(stack);
   }

   private void syncWheelSlotsFromSettings() {
      this.syncWheelSlot(0, this.tripleSlotOneId, this.tripleSlotOneName);
      this.syncWheelSlot(1, this.tripleSlotTwoId, this.tripleSlotTwoName);
      this.syncWheelSlot(2, this.tripleSlotThreeId, this.tripleSlotThreeName);
   }

   private void syncWheelSlot(int index, StringSetting idSetting, StringSetting nameSetting) {
      String itemId = this.normalizeSettingText(idSetting);
      String itemName = this.normalizeSettingText(nameSetting);
      if (itemId.isEmpty()) {
         this.wheelSlots[index] = null;
      } else {
         Identifier identifier = Identifier.tryParse(itemId);
         if (identifier != null && Registries.ITEM.containsId(identifier)) {
            Item item = (Item)Registries.ITEM.get(identifier);
            AutoSwap.WheelSlotItem current = this.wheelSlots[index];
            if (current == null || current.item() != item || !current.itemName().equals(itemName)) {
               this.wheelSlots[index] = new AutoSwap.WheelSlotItem(item, itemName);
            }
         } else {
            this.wheelSlots[index] = null;
         }
      }
   }

   private void updateWheelSlotSettings(int index, String itemId, String itemName) {
      this.wheelSlotIdSetting(index).setValue(itemId);
      this.wheelSlotNameSetting(index).setValue(itemName);
   }

   private StringSetting wheelSlotIdSetting(int index) {
      return switch (index) {
         case 0 -> this.tripleSlotOneId;
         case 1 -> this.tripleSlotTwoId;
         case 2 -> this.tripleSlotThreeId;
         default -> throw new IllegalArgumentException("Invalid wheel slot index: " + index);
      };
   }

   private StringSetting wheelSlotNameSetting(int index) {
      return switch (index) {
         case 0 -> this.tripleSlotOneName;
         case 1 -> this.tripleSlotTwoName;
         case 2 -> this.tripleSlotThreeName;
         default -> throw new IllegalArgumentException("Invalid wheel slot index: " + index);
      };
   }

   private String normalizeSettingText(StringSetting setting) {
      return setting.getValue() == null ? "" : setting.getValue().trim();
   }

   public boolean isAutoDamageEnabled() {
      return this.autoDamage.getValue();
   }

   public boolean isMaxDamageModeActive() {
      return this.isEnabled() && this.mode.is("MaxDamage");
   }

   public boolean shouldUseVanillaAuraHitbox() {
      return this.isMaxDamageModeActive()
         || this.isEnabled()
            && this.autoDamage.getValue()
            && (this.autoDamageActive || this.autoDamageMovementLocked || this.autoDamageSwapExecutor.isRunning() || this.autoDamageSwapExecutor.isBlocking());
   }

   public boolean shouldBlockAutoDamageInput() {
      return this.isEnabled() && (this.autoDamageMovementLocked || this.autoDamageSwapExecutor.isBlocking());
   }

   public boolean shouldBlockAuraInput() {
      return this.isEnabled() && (this.auraMovementLocked || this.auraSwapPending);
   }

   public boolean shouldBlockAuraAttack() {
      return this.isEnabled()
         && (
            this.auraSwapPending
               || this.autoDamageMovementLocked
               || this.autoDamageSwapExecutor.isRunning()
               || this.autoDamageSwapExecutor.isBlocking()
               || System.currentTimeMillis() < this.auraAttackBlockedUntilMs
         );
   }

   private StringSetting hiddenText(String name) {
      StringSetting setting = new StringSetting(name, "", "", 256);
      setting.visibleWhen(() -> false);
      return this.register(setting);
   }

   private static record WheelSlotItem(Item item, String itemName) {
   }
}
