package daamky.ua.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.SettingType;

public class NumberSetting extends Setting<Double> {
   private final double min;
   private final double max;
   private final double step;

   public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
      super(name, description, defaultValue, SettingType.NUMBER);
      this.min = min;
      this.max = max;
      this.step = step <= 0.0 ? 0.1 : step;
      this.setValue(Double.valueOf(defaultValue));
   }

   public double getMin() {
      return this.min;
   }

   public double getMax() {
      return this.max;
   }

   public double getStep() {
      return this.step;
   }

   public float getFloat() {
      return this.getValue().floatValue();
   }

   @Override
   public JsonElement toJson() {
      return new JsonPrimitive(this.getValue());
   }

   @Override
   public void fromJson(JsonElement element) {
      if (element != null) {
         if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("value")) {
               this.setValue(Double.valueOf(object.get("value").getAsDouble()));
            }
         } else {
            if (element.isJsonPrimitive()) {
               this.setValue(Double.valueOf(element.getAsDouble()));
            }
         }
      }
   }

   protected Double normalize(Double value) {
      double safeValue = value == null ? this.min : value;
      double clamped = Math.max(this.min, Math.min(this.max, safeValue));
      return (double)Math.round(clamped / this.step) * this.step;
   }
}
