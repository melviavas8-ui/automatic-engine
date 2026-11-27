package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;
import net.minecraft.world.inventory.ClickType;

public final class ClickSlotEvent extends CancellableEvent {
   private final int windowId;
   private final int slotId;
   private final int button;
   private final ClickType actionType;

   public ClickSlotEvent(int windowId, int slotId, int button, ClickType actionType) {
      this.windowId = windowId;
      this.slotId = slotId;
      this.button = button;
      this.actionType = actionType;
   }

   public int getWindowId() {
      return this.windowId;
   }

   public int getSlotId() {
      return this.slotId;
   }

   public int getButton() {
      return this.button;
   }

   public ClickType getActionType() {
      return this.actionType;
   }
}
