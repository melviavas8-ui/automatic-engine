package daamky.ua.api.module.impl.player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.ClickSlotEvent;
import daamky.ua.api.events.impl.HandledScreenEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.utils.inventory.InventoryTask;

public final class ItemScroller extends Module {
   private final Set<Integer> draggedSlots = new HashSet<>();
   private boolean bulkScrollInProgress;
   private int dragContainerId = -1;
   private SlotActionType dragActionType;

   public ItemScroller() {
      super("ItemScroller", "Item Scroller", ModuleCategory.PLAYER);
   }

   @SubscribeEvent
   private void onHandledScreen(HandledScreenEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.interactionManager != null) {
         Slot hoverSlot = event.getSlotHover();
         SlotActionType actionType = getHoverActionType(client);
         if (isShiftDown(client) && !client.options.sprintKey.isPressed() && actionType != null) {
            int containerId = client.player.currentScreenHandler.syncId;
            if (this.dragContainerId != containerId || this.dragActionType != actionType) {
               this.draggedSlots.clear();
               this.dragContainerId = containerId;
               this.dragActionType = actionType;
            }

            if (hoverSlot != null && hoverSlot.hasStack()) {
               int slotId = InventoryTask.getMenuSlotId(hoverSlot);
               if (slotId != -1 && this.draggedSlots.add(slotId)) {
                  clickSlot(client, slotId, actionType == SlotActionType.THROW ? 1 : 0, actionType);
               }
            }
         } else {
            this.resetDrag();
         }
      }
   }

   @SubscribeEvent
   private void onClickSlot(ClickSlotEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (!this.bulkScrollInProgress && client.player != null && client.interactionManager != null) {
         if (event.getWindowId() == client.player.currentScreenHandler.syncId) {
            if (isShiftDown(client) && client.options.sprintKey.isPressed()) {
               SlotActionType actionType = event.getActionType();
               if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.THROW) {
                  int slotId = event.getSlotId();
                  if (slotId >= 0 && slotId < client.player.currentScreenHandler.slots.size()) {
                     Slot slot = client.player.currentScreenHandler.getSlot(slotId);
                     if (slot.hasStack()) {
                        Item item = slot.getStack().getItem();
                        if (item != null) {
                           List<Integer> matchingSlotIds = IntStream.range(0, client.player.currentScreenHandler.slots.size())
                              .filter(index -> index != slotId)
                              .filter(index -> {
                                 Slot scrolledSlot = client.player.currentScreenHandler.getSlot(index);
                                 return scrolledSlot.hasStack() && scrolledSlot.getStack().getItem().equals(item) && scrolledSlot.inventory == slot.inventory;
                              })
                              .boxed()
                              .toList();
                           if (matchingSlotIds.isEmpty()) {
                              return;
                           }

                           event.setCancelled(true);
                           this.bulkScrollInProgress = true;

                           try {
                              clickSlot(client, slotId, event.getButton(), actionType);

                              for (int matchingSlotId : matchingSlotIds) {
                                 clickSlot(client, matchingSlotId, event.getButton(), actionType);
                              }
                           } finally {
                              this.bulkScrollInProgress = false;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void resetDrag() {
      this.draggedSlots.clear();
      this.dragContainerId = -1;
      this.dragActionType = null;
   }

   private static SlotActionType getHoverActionType(MinecraftClient client) {
      long handle = client.getWindow().getHandle();
      if (client.options.dropKey.isPressed() || GLFW.glfwGetKey(handle, 81) == 1) {
         return SlotActionType.THROW;
      } else {
         return !client.options.attackKey.isPressed() && GLFW.glfwGetMouseButton(handle, 0) != 1 ? null : SlotActionType.QUICK_MOVE;
      }
   }

   private static boolean isShiftDown(MinecraftClient client) {
      long handle = client.getWindow().getHandle();
      return client.options.sneakKey.isPressed() || GLFW.glfwGetKey(handle, 340) == 1 || GLFW.glfwGetKey(handle, 344) == 1;
   }

   private static void clickSlot(MinecraftClient client, int slotId, int button, SlotActionType actionType) {
      client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slotId, button, actionType, client.player);
   }
}
