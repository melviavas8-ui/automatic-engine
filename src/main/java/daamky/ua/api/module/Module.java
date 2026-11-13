package daamky.ua.api.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import daamky.ua.IMinecraft;
import daamky.ua.api.drag.impl.Notifications;
import daamky.ua.api.events.bus.EventBus;
import daamky.ua.api.events.impl.ModuleToggleEvent;
import daamky.ua.api.module.exception.ModuleException;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.manager.Manager;

public abstract class Module implements IMinecraft {
   private String name;
   private String description;
   private ModuleCategory category;
   protected final List<Setting<?>> settings = new ArrayList<>();
   private final List<Setting<?>> settingsView = Collections.unmodifiableList(this.settings);
   private final List<ModuleStateListener> listeners = new ArrayList<>();
   private final BindSetting bindSetting = new BindSetting("Bind", "Module key bind.", KeyBind.NONE, false);
   private EventBus eventBus;
   private boolean enabled;
   private boolean voiceActivated;
   private boolean voiceTogglePending;
   private boolean hidden;
   private boolean starred;

   protected Module() {
      this.bindSetting.addListener((changedSetting, oldValue, newValue) -> this.notifyChanged());
   }

   protected Module(String name, String description, ModuleCategory category) {
      this();
      this.configure(name, description, category);
   }

   final void configure(String name, String description, ModuleCategory category) {
      if (name != null && !name.isBlank()) {
         this.name = name;
         this.description = description == null ? "" : description;
         this.category = category == null ? ModuleCategory.MISC : category;
      } else {
         throw new ModuleException("Module name cannot be empty");
      }
   }

   public final String getName() {
      return this.name;
   }

   public final String getDescription() {
      return this.description;
   }

   public final ModuleCategory getCategory() {
      return this.category;
   }

   public final List<Setting<?>> getSettings() {
      return this.settingsView;
   }

   public final BindSetting getBindSetting() {
      return this.bindSetting;
   }

   public final KeyBind getBind() {
      return this.bindSetting.getValue();
   }

   public final void setBind(KeyBind bind) {
      this.bindSetting.setValue(bind);
   }

   public final boolean isEnabled() {
      return this.enabled;
   }

   public boolean shouldRestoreEnabledState() {
      return true;
   }

   public final boolean isVoiceActivated() {
      return this.enabled && this.voiceActivated;
   }

   public final void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.voiceActivated = enabled && this.voiceTogglePending;
         this.enabled = enabled;
         if (enabled) {
            if (this.eventBus != null) {
               this.eventBus.register(this);
            }

            this.onEnable();
         } else {
            this.onDisable();
            if (this.eventBus != null) {
               this.eventBus.unregister(this);
            }
         }

         Manager.postEvent(new ModuleToggleEvent(this, enabled));
         Notifications.push("Notification", "Модуль " + this.name + (enabled ? " включён" : " выключён"));
         this.notifyChanged();
      }
   }

   public final void toggle() {
      this.setEnabled(!this.enabled);
   }

   public final void toggleByVoice() {
      this.voiceTogglePending = true;

      try {
         this.setEnabled(!this.enabled);
      } finally {
         this.voiceTogglePending = false;
      }
   }

   public final boolean isHidden() {
      return this.hidden;
   }

   public final void setHidden(boolean hidden) {
      if (this.hidden != hidden) {
         this.hidden = hidden;
         this.notifyChanged();
      }
   }

   public final boolean isStarred() {
      return this.starred;
   }

   public final void setStarred(boolean starred) {
      if (this.starred != starred) {
         this.starred = starred;
         this.notifyChanged();
      }
   }

   public final void addStateListener(ModuleStateListener listener) {
      if (listener != null) {
         this.listeners.add(listener);
      }
   }

   public final void initialize(EventBus eventBus) {
      this.eventBus = eventBus;
      if (this.enabled && eventBus != null) {
         eventBus.register(this);
      }
   }

   public void onTick(MinecraftClient client) {
   }

   protected void onEnable() {
   }

   protected void onDisable() {
   }

   protected final <S extends Setting<?>> S register(S setting) {
      if (setting == null) {
         throw new ModuleException("Cannot register null setting in " + this.name);
      } else {
         this.settings.add(setting);
         setting.addListener((changedSetting, oldValue, newValue) -> this.notifyChanged());
         return setting;
      }
   }

   private void notifyChanged() {
      for (ModuleStateListener listener : this.listeners) {
         listener.onChanged(this);
      }
   }
}
