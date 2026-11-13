package daamky.ua.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.SettingType;

public class ButtonSetting extends Setting<Runnable> {
   public ButtonSetting(String name, String description, Runnable action) {
      super(name, description, action, SettingType.BUTTON, false);
   }

   public void press() {
      if (this.getValue() != null) {
         this.getValue().run();
      }
   }

   @Override
   public JsonElement toJson() {
      return JsonNull.INSTANCE;
   }

   @Override
   public void fromJson(JsonElement element) {
   }
}
