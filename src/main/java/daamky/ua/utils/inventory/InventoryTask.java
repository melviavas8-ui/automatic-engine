package daamky.ua.utils.inventory;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import daamky.ua.IMinecraft;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.utils.chat.ChatMessage;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.inventory.script.Script;

public final class InventoryTask implements IMinecraft {
   private static final Script SWAP_AND_USE_SCRIPT = new Script();
   private static final Script HOTBAR_USE_SCRIPT = new Script();

   private InventoryTask() {
   }

   public static void moveItem(Slot from, int to) {
      if (from != null) {
         moveItem(getMenuSlotId(from), to, false, false);
      }
   }

   public static void moveItem(Slot from, int to, boolean task) {
      if (from != null) {
         moveItem(getMenuSlotId(from), to, task, false);
      }
   }

   public static void moveItem(Slot from, int to, boolean task, boolean updateInventory) {
      if (from != null) {
         moveItem(getMenuSlotId(from), to, task, updateInventory);
      }
   }

   public static void moveItem(int from, int to, boolean task, boolean updateInventory) {
      if (from != to && from != -1) {
         int count = Math.toIntExact(slots().count()) - 10;
         if (from >= count && count == 36) {
            Runnable action = () -> clickSlot(to, from - count, SlotActionType.SWAP, false);
            if (task) {
               InventoryFlowManager.addTask(action);
            } else {
               action.run();
            }
         } else {
            if (task) {
               InventoryFlowManager.addTask(() -> moveItem(from, to, updateInventory));
            } else {
               moveItem(from, to, updateInventory);
            }
         }
      }
   }

   public static void moveItem(int from, int to, boolean updateInventory) {
      clickSlot(from, 0, SlotActionType.SWAP, false);
      clickSlot(to, 0, SlotActionType.SWAP, false);
      clickSlot(from, 0, SlotActionType.SWAP, false);
      if (updateInventory) {
         updateSlots();
      }
   }

   public static void swapHand(Slot slot, Hand hand, boolean task) {
      swapHand(slot, hand, task, false);
   }

   public static void swapHand(Slot slot, Hand hand, boolean task, boolean updateInventory) {
      int slotId = getMenuSlotId(slot);
      if (slot != null && slotId != -1 && mc.player != null) {
         int button = hand == Hand.MAIN_HAND ? mc.player.getInventory().getSelectedSlot() : 40;
         Runnable action = () -> swap(slotId, button, updateInventory);
         if (task) {
            InventoryFlowManager.addTask(action);
         } else {
            action.run();
         }
      }
   }

   public static void swap(int slotId, int button, boolean updateInventory) {
      clickSlot(slotId, button, SlotActionType.SWAP, false);
      if (updateInventory && "FunTime".equals(getSelectedMode())) {
         updateSlots();
      }
   }

   public static void swapAndUse(Slot slot, String text, boolean task) {
      if (slot == null) {
         ChatMessage.brandmessage(text + " - не найден!");
      } else {
         Runnable action = () -> swapAndUse(slot);
         if (task) {
            InventoryFlowManager.addTask(action);
         } else {
            action.run();
         }
      }
   }

   public static void swapAndUse(Item item, boolean task) {
      swapAndUse(item, task, null);
   }

   public static void swapAndUse(Item item, boolean task, Angle angle) {
      Slot slot = getSlot(item);
      if (slot == null) {
         ChatMessage.brandmessage(item.getName(item.getDefaultStack()).getString() + " - не найден!");
      } else {
         Runnable action = () -> swapAndUse(slot, angle);
         if (task) {
            InventoryFlowManager.addTask(action);
         } else {
            action.run();
         }
      }
   }

   public static void updateSwapAndUseScript() {
      SWAP_AND_USE_SCRIPT.update();
      HOTBAR_USE_SCRIPT.update();
   }

   public static boolean isSwapAndUseIdle() {
      return SWAP_AND_USE_SCRIPT.isFinished() && HOTBAR_USE_SCRIPT.isFinished();
   }

   public static void swapAndUse(Slot slot) {
      swapAndUse(slot, null);
   }

   public static void swapAndUse(Slot slot, Angle angle) {
      String var4 = getSelectedMode();
      int delay1;
      int delay2;
      switch (var4) {
         case "HolyWorld":
            delay1 = 2;
            delay2 = 1;
            break;
         case "FunTime":
            delay1 = 5;
            delay2 = 1;
            break;
         default:
            delay1 = 0;
            delay2 = 0;
      }

      SWAP_AND_USE_SCRIPT.cleanup()
         .addTickStep(0, () -> swapHand(slot, Hand.MAIN_HAND, false))
         .addTickStep(delay1, () -> PlayerInteractionHelper.interactItem(Hand.MAIN_HAND, angle))
         .addTickStep(delay2, () -> swapHand(slot, Hand.MAIN_HAND, false, true));
   }

   public static void useHotbarItem(int hotbarSlot, int previousSlot, boolean instant) {
      if (mc.player != null) {
         String mode = getSelectedMode();

         int useDelay = switch (mode) {
            case "HolyWorld" -> instant ? 1 : 3;
            case "FunTime", "SpookyTime", "CopyTime" -> instant ? 1 : 2;
            default -> instant ? 0 : 1;
         };
         int restoreDelay = instant ? 1 : 2;
         useHotbarItem(hotbarSlot, previousSlot, useDelay, restoreDelay);
      }
   }

   public static void useHotbarItem(int hotbarSlot, int previousSlot, int useDelay, int restoreDelay) {
      if (mc.player != null && mc.interactionManager != null && hotbarSlot >= 0 && hotbarSlot <= 8) {
         int safeUseDelay = Math.max(0, useDelay);
         int safeRestoreDelay = Math.max(1, restoreDelay);
         HOTBAR_USE_SCRIPT.cleanup().addTickStep(0, () -> switchTo(hotbarSlot)).addTickStep(safeUseDelay, () -> {
            if (mc.player != null) {
               PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
               mc.player.swingHand(Hand.MAIN_HAND);
            }
         }).addTickStep(safeUseDelay + safeRestoreDelay, () -> {
            if (previousSlot != hotbarSlot) {
               switchTo(previousSlot);
            }
         });
      }
   }

   public static void updateSlots() {
      if (mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.PICKUP_ALL, mc.player);
      }
   }

   public static void closeScreen(boolean packet) {
      if (mc.player != null) {
         if (packet) {
            if (mc.getNetworkHandler() != null) {
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            }
         } else {
            mc.player.closeHandledScreen();
         }
      }
   }

   public static void clickSlot(int id, int button, SlotActionType type) {
      if (id != -1 && mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, button, type, mc.player);
      }
   }

   public static void switchTo(int slot) {
      if (mc.player != null && slot >= 0 && slot <= 8) {
         InventoryUtils.syncSelectedHotbarSlot(slot);
      }
   }

   public static void swapSlot(Slot targetSlot) {
      swapSlot(targetSlot, false);
   }

   public static void swapSlot(Slot targetSlot, boolean task) {
      int slotId = getMenuSlotId(targetSlot);
      if (targetSlot != null && slotId != -1) {
         int hotbarSlot = slotId >= 36 && slotId <= 44 ? slotId - 36 : targetSlot.id;
         Runnable action = () -> switchTo(hotbarSlot);
         if (task) {
            InventoryFlowManager.addTask(action);
         } else {
            action.run();
         }
      }
   }

   public static void swapSlot(Item item) {
      swapSlot(getSlot(item));
   }

   public static void swapSlot(Item item, boolean task) {
      swapSlot(getSlot(item), task);
   }

   public static void clickSlot(Slot slot, int button, SlotActionType ClickType, boolean silent) {
      if (slot != null) {
         clickSlot(getMenuSlotId(slot), button, ClickType, silent);
      }
   }

   public static void clickSlot(int slotId, int buttonId, SlotActionType ClickType, boolean silent) {
      clickSlot(mc.player.currentScreenHandler.syncId, slotId, buttonId, ClickType, silent, false);
   }

   public static void clickSlot(int slotId, int buttonId, SlotActionType ClickType, boolean silent, boolean task) {
      clickSlot(mc.player.currentScreenHandler.syncId, slotId, buttonId, ClickType, silent, task);
   }

   public static void clickSlot(int windowId, int slotId, int buttonId, SlotActionType ClickType, boolean silent) {
      clickSlot(windowId, slotId, buttonId, ClickType, silent, false);
   }

   public static void clickSlot(int windowId, int slotId, int buttonId, SlotActionType ClickType, boolean silent, boolean task) {
      Runnable action = () -> {
         if (mc.interactionManager != null && mc.player != null) {
            mc.interactionManager.clickSlot(windowId, slotId, buttonId, ClickType, mc.player);
            if (silent) {
               mc.player.currentScreenHandler.onSlotClick(slotId, buttonId, ClickType, mc.player);
            }
         }
      };
      if (task) {
         InventoryFlowManager.addTask(action);
      } else {
         action.run();
      }
   }

   public static Slot getSlot(Item item) {
      return getSlot(item, slot -> true);
   }

   public static Slot getSlot(Item item, Predicate<Slot> filter) {
      return getSlot(item, Comparator.comparingInt(slot -> 0), filter);
   }

   public static Slot getSlot(Predicate<Slot> filter) {
      return slots().filter(filter).findFirst().orElse(null);
   }

   public static Slot getSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
      return slots().filter(filter).max(comparator).orElse(null);
   }

   public static Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
      return slots().filter(slot -> slot.getStack().getItem().equals(item)).filter(filter).max(comparator).orElse(null);
   }

   public static Slot getFoodMaxSaturationSlot() {
      return slots()
         .filter(slot -> {
            FoodComponent food = (FoodComponent)slot.getStack().get(DataComponentTypes.FOOD);
            return food != null && !food.canAlwaysEat();
         })
         .max(
            Comparator.comparingDouble(
               slot -> (double)((FoodComponent)Objects.requireNonNull((FoodComponent)slot.getStack().get(DataComponentTypes.FOOD))).saturation()
            )
         )
         .orElse(null);
   }

   public static Slot getSlot(List<Item> items) {
      return slots().filter(slot -> items.contains(slot.getStack().getItem())).findFirst().orElse(null);
   }

   public static Slot getPotion(RegistryEntry<StatusEffect> effect) {
      return slots()
         .filter(
            slot -> {
               PotionContentsComponent component = (PotionContentsComponent)slot.getStack().get(DataComponentTypes.POTION_CONTENTS);
               return component == null
                  ? false
                  : StreamSupport.<StatusEffectInstance>stream(component.getEffects().spliterator(), false)
                     .anyMatch(instance -> instance.getEffectType().equals(effect));
            }
         )
         .findFirst()
         .orElse(null);
   }

   public static Slot getPotionFromCategory(StatusEffectCategory category) {
      return slots()
         .filter(
            slot -> {
               ItemStack stack = slot.getStack();
               PotionContentsComponent component = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
               if (stack.getItem().equals(Items.SPLASH_POTION) && component != null) {
                  StatusEffectCategory inverse = category == StatusEffectCategory.BENEFICIAL ? StatusEffectCategory.HARMFUL : StatusEffectCategory.BENEFICIAL;
                  long matching = StreamSupport.<StatusEffectInstance>stream(component.getEffects().spliterator(), false)
                     .filter(effect -> ((StatusEffect)effect.getEffectType().value()).getCategory() == category)
                     .count();
                  long opposite = StreamSupport.<StatusEffectInstance>stream(component.getEffects().spliterator(), false)
                     .filter(effect -> ((StatusEffect)effect.getEffectType().value()).getCategory() == inverse)
                     .count();
                  return matching >= opposite;
               } else {
                  return false;
               }
            }
         )
         .findFirst()
         .orElse(null);
   }

   public static int getInventoryCount(Item item) {
      return mc.player == null
         ? 0
         : IntStream.range(0, mc.player.getInventory().size())
            .filter(i -> mc.player.getInventory().getStack(i).getItem().equals(item))
            .map(i -> mc.player.getInventory().getStack(i).getCount())
            .sum();
   }

   public static int findFreeHotbarSlot() {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
               return i;
            }
         }

         return 8;
      }
   }

   public static int findHotbarSlot(Item item) {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
               return i;
            }
         }

         return -1;
      }
   }

   public static int findInventorySlot(Item item) {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
               return i;
            }
         }

         return -1;
      }
   }

   public static void swapSlots(int fromSlot, int toSlot) {
      if (mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, fromSlot, toSlot, SlotActionType.SWAP, mc.player);
      }
   }

   public static void swapSlots(int fromSlot, int toSlot, boolean task) {
      if (mc.interactionManager != null && mc.player != null) {
         Runnable action = () -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, fromSlot, toSlot, SlotActionType.SWAP, mc.player);
         if (task) {
            InventoryFlowManager.addTask(action);
         } else {
            action.run();
         }
      }
   }

   public static Slot slotByIndex(int index) {
      if (mc.player == null) {
         return null;
      } else {
         return index >= 0 && index < mc.player.currentScreenHandler.slots.size() ? mc.player.currentScreenHandler.getSlot(index) : null;
      }
   }

   public static void useItemFromSlotMOMENTALNO(int slot) {
      if (mc.player != null && mc.interactionManager != null) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (!stack.isEmpty() && !mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            int previousSlot = mc.player.getInventory().getSelectedSlot();
            InventoryUtils.syncSelectedHotbarSlot(slot);
            PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            InventoryUtils.syncSelectedHotbarSlot(previousSlot);
         }
      }
   }

   public static int getHotbarItems(List<Item> items) {
      return mc.player == null ? -1 : IntStream.range(0, 9).filter(i -> items.contains(mc.player.getInventory().getStack(i).getItem())).findFirst().orElse(-1);
   }

   public static int getHotbarSlotId(IntPredicate filter) {
      return IntStream.range(0, 9).filter(filter).findFirst().orElse(-1);
   }

   public static int getCount(Predicate<Slot> filter) {
      return slots().filter(filter).mapToInt(slot -> slot.getStack().getCount()).sum();
   }

   public static Slot mainHandSlot() {
      if (mc.player == null) {
         return null;
      } else {
         List<Slot> slots = slots().toList();
         int count = slots.size();
         int offset = count == 46 ? 10 : 9;
         int index = count - offset + mc.player.getInventory().getSelectedSlot();
         return index >= 0 && index < slots.size() ? slots.get(index) : null;
      }
   }

   public static boolean isServerScreen() {
      return slots().toList().size() != 46;
   }

   public static Stream<Slot> slots() {
      return mc.player == null ? Stream.empty() : mc.player.currentScreenHandler.slots.stream();
   }

   public static void selectCompass() {
      Slot slot = getSlot(Items.COMPASS);
      if (slot != null && mc.player != null) {
         int slotId = getMenuSlotId(slot);
         if (slotId >= 36 && slotId <= 44) {
            InventoryUtils.syncSelectedHotbarSlot(slotId - 36);
         } else {
            InventoryUtils.syncSelectedHotbarSlot(0);
            swapHand(slot, Hand.MAIN_HAND, false, true);
         }
      }
   }

   public static String getCleanName(Text text) {
      if (text == null) {
         return "";
      } else {
         String name = text.getString();
         return name == null ? "" : name.replaceAll("(?i)§[0-9A-FK-OR]", "").toLowerCase();
      }
   }

   public static String getMoveMode() {
      return getSelectedMode();
   }

   public static boolean isMoveMode(String mode) {
      return mode != null && mode.equals(getSelectedMode());
   }

   public static int getMenuSlotId(Slot slot) {
      if (slot != null && mc.player != null && mc.player.currentScreenHandler != null) {
         List<Slot> slots = mc.player.currentScreenHandler.slots;

         for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) == slot) {
               return i;
            }
         }

         return slot.id;
      } else {
         return -1;
      }
   }

   private static String getSelectedMode() {
      try {
         return "???????";
      } catch (Throwable var1) {
         return "Обычный";
      }
   }

   private static String normalizeModeName(String mode) {
      if (mode == null) {
         return "Обычный";
      } else {
         return switch (mode) {
            case "ФанТайм", "Фантайм" -> "FunTime";
            case "СпукиТайм" -> "SpookyTime";
            case "ХолиВорлд" -> "HolyWorld";
            case "РиллиВорлд" -> "ReallyWorld";
            case "КопиТайм" -> "CopyTime";
            default -> mode;
         };
      }
   }
}
