package daamky.ua.api.settings.impl;

import com.google.gson.JsonElement;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.SettingType;
import daamky.ua.api.settings.bind.KeyBind;

public class BindSetting extends Setting<KeyBind> {
   public BindSetting(String name, String description, KeyBind defaultValue) {
      this(name, description, defaultValue, true);
   }

   public BindSetting(String name, String description, KeyBind defaultValue, boolean persistent) {
      super(name, description, defaultValue, SettingType.BIND, persistent);
   }

   @Override
   public JsonElement toJson() {
      return this.getValue().toJson();
   }

   @Override
   public void fromJson(JsonElement element) {
      this.setValue(KeyBind.fromJson(element));
   }

   protected KeyBind normalize(KeyBind value) {
      return value == null ? KeyBind.NONE : value;
   }
}
