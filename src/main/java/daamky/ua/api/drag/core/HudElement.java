package daamky.ua.api.drag.core;

import net.minecraft.client.gui.Click;

public interface HudElement {
   void render();

   default boolean mouseClicked(Click event, boolean doubled) {
      return false;
   }

   default String elementName() {
      return this.getClass().getSimpleName();
   }
}
