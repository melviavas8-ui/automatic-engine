package daamky.ua.api.events.impl;

import net.minecraft.client.gui.screen.Screen;
import daamky.ua.api.events.CancellableEvent;

public final class CloseScreenEvent extends CancellableEvent {
   private final Screen screen;

   public CloseScreenEvent(Screen screen) {
      this.screen = screen;
   }

   public Screen getScreen() {
      return this.screen;
   }
}
