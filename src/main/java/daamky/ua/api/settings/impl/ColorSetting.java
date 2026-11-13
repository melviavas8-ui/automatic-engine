package daamky.ua.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.SettingType;

public class ColorSetting extends Setting<Color> {
   public ColorSetting(String name, String description, Color defaultValue) {
      super(name, description, defaultValue, SettingType.COLOR);
   }

   @Override
   public JsonElement toJson() {
      Color color = this.getValue();
      JsonObject object = new JsonObject();
      object.addProperty("red", color.getRed());
      object.addProperty("green", color.getGreen());
      object.addProperty("blue", color.getBlue());
      object.addProperty("alpha", color.getAlpha());
      object.addProperty("rgba", color.getRGB());
      return object;
   }

   @Override
   public void fromJson(JsonElement element) {
      if (element != null && element.isJsonObject()) {
         JsonObject object = element.getAsJsonObject();
         if (object.has("rgba")) {
            this.setValue(new Color(object.get("rgba").getAsInt(), true));
         } else {
            int red = object.has("red") ? object.get("red").getAsInt() : this.getValue().getRed();
            int green = object.has("green") ? object.get("green").getAsInt() : this.getValue().getGreen();
            int blue = object.has("blue") ? object.get("blue").getAsInt() : this.getValue().getBlue();
            int alpha = object.has("alpha") ? object.get("alpha").getAsInt() : this.getValue().getAlpha();
            this.setValue(new Color(this.clamp(red), this.clamp(green), this.clamp(blue), this.clamp(alpha)));
         }
      }
   }

   protected Color normalize(Color value) {
      return value == null ? new Color(255, 255, 255, 255) : value;
   }

   private int clamp(int value) {
      return Math.max(0, Math.min(255, value));
   }
}
