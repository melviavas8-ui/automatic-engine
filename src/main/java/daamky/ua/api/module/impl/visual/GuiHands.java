package daamky.ua.api.module.impl.visual;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;

public class GuiHands extends Module {
   private static GuiHands instance;

   public GuiHands() {
      super("Gui Hands", "Renders first-person hands while ClickGUI is open.", ModuleCategory.VISUAL);
      instance = this;
   }

   public static boolean shouldRender() {
      return instance != null && instance.isEnabled();
   }
}
