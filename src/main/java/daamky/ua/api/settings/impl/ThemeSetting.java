package daamky.ua.api.settings.impl;

import daamky.ua.api.theme.ClientTheme;
import daamky.ua.api.theme.ThemeManager;

public final class ThemeSetting extends ModeSetting {
   public ThemeSetting() {
      super("Client Theme", "Changes the interface color theme.", false, ThemeManager.current().name(), ThemeManager.names().toArray(String[]::new));
      this.addListener((setting, oldValue, newValue) -> ThemeManager.select(newValue));
      ThemeManager.select(this.getValue());
   }

   /** Called after the client style changed: swaps the list to the new style's themes and keeps its own selected theme. */
   public void refreshForStyle() {
      String wanted = ThemeManager.current().name();
      this.replaceModes(ThemeManager.names().toArray(String[]::new));
      this.setValue(wanted);
      ThemeManager.select(wanted);
   }

   public ClientTheme selectedTheme() {
      return ThemeManager.find(this.getValue());
   }

   @Override
   protected String normalize(String value) {
      ClientTheme theme = ThemeManager.find(value);
      return theme.name().equalsIgnoreCase(value) ? theme.name() : ThemeManager.current().name();
   }
}
