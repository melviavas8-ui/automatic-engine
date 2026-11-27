package daamky.ua.api.module.impl.player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
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
   private ClickType dragActionType;

   public ItemScroller() {
      super("ItemScroller", "Item Scroller", ModuleCategory.PLAYER);
   }

   @SubscribeEvent
   private void onHandledScreen(HandledScreenEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.gameMode != null) {
         Slot hoverSlot = event.getSlotHover();
         ClickType actionType = getHoverActionType(client);
         if (isShiftDown(client) && !client.options.keySprint.isDown() && actionType != null) {
            int containerId = client.player.containerMenu.containerId;
            if (this.dragContainerId != containerId || this.dragActionType != actionType) {
               this.draggedSlots.clear();
               this.dragContainerId = containerId;
               this.dragActionType = actionType;
            }

            if (hoverSlot != null && hoverSlot.hasItem()) {
               int slotId = InventoryTask.getMenuSlotId(hoverSlot);
               if (slotId != -1 && this.draggedSlots.add(slotId)) {
                  clickSlot(client, slotId, actionType == ClickType.THROW ? 1 : 0, actionType);
               }
            }
         } else {
            this.resetDrag();
         }
      }
   }

   @SubscribeEvent
   private void onClickSlot(ClickSlotEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (!this.bulkScrollInProgress && client.player != null && client.gameMode != null) {
         if (event.getWindowId() == client.player.containerMenu.containerId) {
            if (isShiftDown(client) && client.options.keySprint.isDown()) {
               ClickType actionType = event.getActionType();
               if (actionType == ClickType.QUICK_MOVE || actionType == ClickType.THROW) {
                  int slotId = event.getSlotId();
                  if (slotId >= 0 && slotId < client.player.containerMenu.slots.size()) {
                     Slot slot = client.player.containerMenu.getSlot(slotId);
                     if (slot.hasItem()) {
                        Item item = slot.getItem().getItem();
                        if (item != null) {
                           List<Integer> matchingSlotIds = IntStream.range(0, client.player.containerMenu.slots.size())
                              .filter(index -> index != slotId)
                              .filter(index -> {
                                 Slot scrolledSlot = client.player.containerMenu.getSlot(index);
                                 return scrolledSlot.hasItem() && scrolledSlot.getItem().getItem().equals(item) && scrolledSlot.container == slot.container;
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

   private static ClickType getHoverActionType(Minecraft client) {
      long handle = client.getWindow().handle();
      if (client.options.keyDrop.isDown() || GLFW.glfwGetKey(handle, 81) == 1) {
         return ClickType.THROW;
      } else {
         return !client.options.keyAttack.isDown() && GLFW.glfwGetMouseButton(handle, 0) != 1 ? null : ClickType.QUICK_MOVE;
      }
   }

   private static boolean isShiftDown(Minecraft client) {
      long handle = client.getWindow().handle();
      return client.options.keyShift.isDown() || GLFW.glfwGetKey(handle, 340) == 1 || GLFW.glfwGetKey(handle, 344) == 1;
   }

   private static void clickSlot(Minecraft client, int slotId, int button, ClickType actionType) {
      client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slotId, button, actionType, client.player);
   }
}
