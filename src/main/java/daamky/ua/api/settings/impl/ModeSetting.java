package daamky.ua.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.SettingType;

public class ModeSetting extends Setting<String> {
   private final List<String> modes;
   private final boolean renderDescription;
   private Predicate<String> visibleMode = mode -> true;

   public ModeSetting(String name, String description, String defaultValue, String... modes) {
      this(name, description, true, defaultValue, modes);
   }

   public ModeSetting(String name, String description, boolean renderDescription, String defaultValue, String... modes) {
      super(name, description, defaultValue, SettingType.MODE);
      this.modes = new ArrayList<>(List.of(modes));
      this.renderDescription = renderDescription;
      this.setValue(defaultValue);
   }

   public List<String> getModes() {
      return Collections.unmodifiableList(this.modes);
   }

   /** Replaces the available options of a dynamic mode setting. */
   public void replaceModes(String... newModes) {
      String previous = this.getValue();
      this.modes.clear();
      if (newModes != null) {
         for (String mode : newModes) {
            if (mode != null && !mode.isBlank() && !this.modes.contains(mode)) {
               this.modes.add(mode);
            }
         }
      }
      if (this.modes.isEmpty()) {
         this.modes.add("Не выбрано");
      }
      this.setValue(this.modes.contains(previous) ? previous : this.modes.getFirst());
   }

   public List<String> getVisibleModes() {
      List<String> visibleModes = new ArrayList<>();

      for (String mode : this.modes) {
         if (this.isModeVisible(mode)) {
            visibleModes.add(mode);
         }
      }

      return visibleModes;
   }

   public boolean isModeVisible(String mode) {
      return this.visibleMode.test(mode);
   }

   public ModeSetting visibleModesWhen(Predicate<String> visibleMode) {
      this.visibleMode = visibleMode == null ? mode -> true : visibleMode;
      return this;
   }

   public boolean shouldRenderDescription() {
      return this.renderDescription;
   }

   public boolean is(String mode) {
      if (mode == null) {
         return false;
      } else {
         for (String availableMode : this.modes) {
            if (availableMode.equalsIgnoreCase(mode)) {
               return this.getValue().equals(availableMode);
            }
         }

         return false;
      }
   }

   public boolean isSelected(String mode) {
      return this.is(mode);
   }

   @Override
   public JsonElement toJson() {
      return new JsonPrimitive(this.getValue());
   }

   @Override
   public void fromJson(JsonElement element) {
      if (element != null && element.isJsonPrimitive()) {
         this.setValue(element.getAsString());
      }
   }

   protected String normalize(String value) {
      if (value != null && !this.modes.isEmpty()) {
         for (String mode : this.modes) {
            if (mode.equalsIgnoreCase(value)) {
               return mode;
            }
         }

         return this.modes.getFirst();
      } else {
         return this.modes.isEmpty() ? "" : this.modes.getFirst();
      }
   }
}
