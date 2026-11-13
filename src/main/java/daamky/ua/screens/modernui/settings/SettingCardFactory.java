package daamky.ua.screens.modernui.settings;

import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ButtonSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.api.settings.impl.ThemeSetting;

final class SettingCardFactory {
   private SettingCardFactory() {
   }

   static SettingCardComponent<?> create(Setting<?> setting) {
      if (setting instanceof ThemeSetting themeSetting) {
         return new ThemeSettingCard(themeSetting);
      } else if (setting instanceof ModeSetting modeSetting) {
         return new ModeSettingCard(modeSetting);
      } else if (setting instanceof MultiModeSetting multiModeSetting) {
         return new MultiSettingCard(multiModeSetting);
      } else if (setting instanceof NumberSetting numberSetting) {
         return new SliderSettingCard(numberSetting);
      } else if (setting instanceof BooleanSetting booleanSetting) {
         return new BooleanSettingCard(booleanSetting);
      } else if (setting instanceof ColorSetting colorSetting) {
         return new ColorSettingCard(colorSetting);
      } else if (setting instanceof BindSetting bindSetting) {
         return new BindSettingCard(bindSetting);
      } else if (setting instanceof StringSetting stringSetting) {
         return new TextSettingCard(stringSetting);
      } else {
         return setting instanceof ButtonSetting buttonSetting ? new ButtonSettingCard(buttonSetting) : null;
      }
   }
}
