package daamky.ua.api.module.impl.display;

import daamky.gui.UiStyle;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.GuiTypeSetting;
import daamky.ua.api.settings.impl.StyleSetting;
import daamky.ua.api.settings.impl.ThemeSetting;

public final class Appearance extends Module {
   private final StyleSetting style = this.register(new StyleSetting());
   private final ThemeSetting theme = this.register(new ThemeSetting());
   private final GuiTypeSetting type = this.register(new GuiTypeSetting());

   public Appearance() {
      super("Appearance", "Client interface themes.", ModuleCategory.DISPLAY);
      UiStyle.addChangeListener(() -> {
         if (!UiStyle.current().equals(this.style.getValue())) {
            this.style.setValue(UiStyle.current());
         }

         this.theme.refreshForStyle();
         this.type.refreshForStyle();
      });
   }

   public StyleSetting style() {
      return this.style;
   }

   public ThemeSetting theme() {
      return this.theme;
   }

   public GuiTypeSetting guiType() {
      return this.type;
   }
}
