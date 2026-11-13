package daamky.ua.api.settings;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import daamky.ua.api.settings.exception.SettingException;

public abstract class Setting<T> {
   private final String name;
   private final String description;
   private final T defaultValue;
   private final SettingType type;
   private final boolean persistent;
   private final List<SettingChangeListener<T>> listeners = new ArrayList<>();
   private BooleanSupplier visible = () -> true;
   private T value;

   protected Setting(String name, String description, T defaultValue, SettingType type) {
      this(name, description, defaultValue, type, true);
   }

   protected Setting(String name, String description, T defaultValue, SettingType type, boolean persistent) {
      if (name != null && !name.isBlank()) {
         this.name = name;
         this.description = description == null ? "" : description;
         this.defaultValue = defaultValue;
         this.value = defaultValue;
         this.type = type;
         this.persistent = persistent;
      } else {
         throw new SettingException("Setting name cannot be empty");
      }
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public T getValue() {
      return this.value;
   }

   public T getDefaultValue() {
      return this.defaultValue;
   }

   public SettingType getType() {
      return this.type;
   }

   public boolean isPersistent() {
      return this.persistent;
   }

   public boolean isVisible() {
      return this.visible.getAsBoolean();
   }

   public Setting<T> visibleWhen(BooleanSupplier visible) {
      this.visible = visible == null ? () -> true : visible;
      return this;
   }

   public Setting<T> visible(BooleanSupplier visible) {
      return this.visibleWhen(visible);
   }

   public Setting<T> setVisible(BooleanSupplier visible) {
      return this.visibleWhen(visible);
   }

   public Setting<T> setVisible(boolean visible) {
      return this.visibleWhen(() -> visible);
   }

   public void setValue(T value) {
      T normalized = this.normalize(value);
      T oldValue = this.value;
      if (!Objects.equals(oldValue, normalized)) {
         this.value = normalized;
         this.notifyListeners(oldValue, normalized);
      }
   }

   public void reset() {
      this.setValue(this.defaultValue);
   }

   public void addListener(SettingChangeListener<T> listener) {
      if (listener != null) {
         this.listeners.add(listener);
      }
   }

   public abstract JsonElement toJson();

   public abstract void fromJson(JsonElement var1);

   protected T normalize(T value) {
      return value;
   }

   private void notifyListeners(T oldValue, T newValue) {
      for (SettingChangeListener<T> listener : this.listeners) {
         listener.onChanged(this, oldValue, newValue);
      }
   }
}
