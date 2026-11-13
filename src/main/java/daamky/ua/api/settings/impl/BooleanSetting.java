package daamky.ua.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.SettingType;

public class BooleanSetting extends Setting<Boolean> {
   public BooleanSetting(String name, String description, boolean defaultValue) {
      super(name, description, defaultValue, SettingType.BOOLEAN);
   }

   @Override
   public JsonElement toJson() {
      return new JsonPrimitive(this.getValue());
   }

   @Override
   public void fromJson(JsonElement element) {
      if (element != null && element.isJsonPrimitive()) {
         this.setValue(Boolean.valueOf(element.getAsBoolean()));
      }
   }
}
