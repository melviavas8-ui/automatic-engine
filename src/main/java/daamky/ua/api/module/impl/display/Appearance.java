package daamky.ua.api.module.impl.display;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.settings.impl.GuiTypeSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.ThemeSetting;

public final class Appearance extends Module {
   private final ThemeSetting theme = this.register(new ThemeSetting());
   private final GuiTypeSetting type = this.register(new GuiTypeSetting());

   private final ModeSetting language = this.register(
      new ModeSetting("Language", "Client interface language.", ClientLanguage.current().title(), "Українська", "Русский", "English")
   );

   public Appearance() {
      super("Appearance", "Client interface themes.", ModuleCategory.DISPLAY);
      this.language.addListener((setting, oldValue, newValue) -> {
         for (ClientLanguage.Lang lang : ClientLanguage.Lang.values()) {
            if (lang.title().equals(newValue)) {
               ClientLanguage.set(lang);
            }
         }
      });
      ClientLanguage.addChangeListener(() -> {
         String title = ClientLanguage.current().title();
         if (!title.equals(this.language.getValue())) {
            this.language.setValue(title);
         }
      });
   }

   public ThemeSetting theme() {
      return this.theme;
   }

   public GuiTypeSetting guiType() {
      return this.type;
   }
}
