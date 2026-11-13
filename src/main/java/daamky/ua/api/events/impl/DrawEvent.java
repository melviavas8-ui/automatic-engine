package daamky.ua.api.events.impl;

import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.events.Event;

public record DrawEvent(DrawContext graphics, float partialTicks, DrawEvent.Layer layer) implements Event {
   public DrawEvent(DrawContext graphics, float partialTicks) {
      this(graphics, partialTicks, DrawEvent.Layer.GAME);
   }

   public DrawContext getGraphics() {
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
