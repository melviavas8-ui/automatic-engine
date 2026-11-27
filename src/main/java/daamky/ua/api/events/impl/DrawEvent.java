package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;
import net.minecraft.client.gui.GuiGraphics;

public record DrawEvent(GuiGraphics graphics, float partialTicks, DrawEvent.Layer layer) implements Event {
   public DrawEvent(GuiGraphics graphics, float partialTicks) {
      this(graphics, partialTicks, DrawEvent.Layer.GAME);
   }

   public GuiGraphics getGraphics() {
      return this.graphics;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }

   public DrawEvent.Layer getLayer() {
      return this.layer;
   }

   public boolean isScreenBackgroundLayer() {
      return this.layer == DrawEvent.Layer.SCREEN_BACKGROUND;
   }

   public static enum Layer {
      GAME,
      SCREEN_BACKGROUND,
      CHAT_BACKGROUND,
      CHAT_OVERLAY;

      private Layer() {
      }
   }
}
