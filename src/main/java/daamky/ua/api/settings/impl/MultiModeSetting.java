package daamky.ua.api.settings.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.SettingType;

public class MultiModeSetting extends Setting<Set<String>> {
   private final List<String> modes;
   private final boolean renderDescription;

   public MultiModeSetting(String name, String description, String[] modes, String... selected) {
      this(name, description, true, modes, selected);
   }

   public MultiModeSetting(String name, String description, boolean renderDescription, String[] modes, String... selected) {
      super(name, description, new LinkedHashSet<>(Arrays.asList(selected)), SettingType.MULTI_MODE);
      this.modes = List.of(modes);
      this.renderDescription = renderDescription;
      this.setValue(new LinkedHashSet<>(Arrays.asList(selected)));
   }

   public List<String> getModes() {
      return this.modes;
   }

   public boolean shouldRenderDescription() {
      return this.renderDescription;
   }

   public boolean isSelected(String mode) {
      return this.getValue().contains(this.normalizeMode(mode));
   }

   public void setSelected(String mode, boolean selected) {
      Set<String> next = new LinkedHashSet<>(this.getValue());
      String normalized = this.normalizeMode(mode);
      if (selected) {
         next.add(normalized);
      } else {
         next.remove(normalized);
      }

      this.setValue(next);
   }

   public int selectedCount() {
      return this.getValue().size();
   }

   @Override
   public JsonElement toJson() {
      JsonArray array = new JsonArray();

      for (String value : this.getValue()) {
         array.add(value);
      }

      return array;
   }

   @Override
   public void fromJson(JsonElement element) {
      if (element != null && element.isJsonArray()) {
         Set<String> next = new LinkedHashSet<>();

         for (JsonElement item : element.getAsJsonArray()) {
            if (item.isJsonPrimitive()) {
               next.add(this.normalizeMode(item.getAsString()));
            }
         }

         this.setValue(next);
      }
   }

   protected Set<String> normalize(Set<String> value) {
      Set<String> normalized = new LinkedHashSet<>();
      if (value == null) {
         return normalized;
      } else {
         for (String mode : value) {
            String normalizedMode = this.normalizeMode(mode);
            if (!normalizedMode.isEmpty()) {
               normalized.add(normalizedMode);
            }
         }

         return normalized;
      }
   }

   private String normalizeMode(String value) {
      if (value == null) {
         return "";
      } else {
         for (String mode : this.modes) {
            if (mode.equalsIgnoreCase(value)) {
               return mode;
            }
         }

         return "";
      }
   }
}
