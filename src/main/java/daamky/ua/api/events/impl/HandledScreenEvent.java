package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

public final class HandledScreenEvent implements Event {
   private final GuiGraphics graphics;
   private final Slot slotHover;
   private final int imageWidth;
   private final int imageHeight;

   public HandledScreenEvent(Slot slotHover, int imageWidth, int imageHeight) {
      this(null, slotHover, imageWidth, imageHeight);
   }

   public HandledScreenEvent(GuiGraphics graphics, Slot slotHover, int imageWidth, int imageHeight) {
      this.graphics = graphics;
      this.slotHover = slotHover;
      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;
   }

   public GuiGraphics getGraphics() {
      return this.graphics;
   }

   public Slot getSlotHover() {
      return this.slotHover;
   }

   public int getImageWidth() {
      return this.imageWidth;
   }

   public int getImageHeight() {
      return this.imageHeight;
   }
}
