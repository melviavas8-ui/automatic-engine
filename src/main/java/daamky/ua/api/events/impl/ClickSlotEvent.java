package daamky.ua.api.events.impl;

import net.minecraft.screen.slot.SlotActionType;
import daamky.ua.api.events.CancellableEvent;

public final class ClickSlotEvent extends CancellableEvent {
   private final int windowId;
   private final int slotId;
   private final int button;
   private final SlotActionType actionType;

   public ClickSlotEvent(int windowId, int slotId, int button, SlotActionType actionType) {
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

   public SlotActionType getActionType() {
      return this.actionType;
   }
}
