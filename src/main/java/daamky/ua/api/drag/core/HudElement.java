package daamky.ua.api.drag.core;

import net.minecraft.client.input.MouseButtonEvent;

public interface HudElement {
   void render();

   default boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      return false;
   }

   default String elementName() {
      return this.getClass().getSimpleName();
   }
}
