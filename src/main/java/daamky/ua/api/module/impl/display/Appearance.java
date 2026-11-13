package daamky.ua.api.module.impl.display;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.GuiTypeSetting;
import daamky.ua.api.settings.impl.ThemeSetting;

public final class Appearance extends Module {
   private final ThemeSetting theme = this.register(new ThemeSetting());
   private final GuiTypeSetting type = this.register(new GuiTypeSetting());

   public Appearance() {
      super("Appearance", "Client interface themes.", ModuleCategory.DISPLAY);
   }

   public ThemeSetting theme() {
      return this.theme;
   }

   public GuiTypeSetting guiType() {
      return this.type;
   }
}
