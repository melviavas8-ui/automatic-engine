package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;
import net.minecraft.client.gui.screens.Screen;

public final class CloseScreenEvent extends CancellableEvent {
   private final Screen screen;

   public CloseScreenEvent(Screen screen) {
      this.screen = screen;
   }

   public Screen getScreen() {
      return this.screen;
   }
}
