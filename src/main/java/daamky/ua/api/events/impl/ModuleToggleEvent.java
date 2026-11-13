package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;
import daamky.ua.api.module.Module;

public final class ModuleToggleEvent implements Event {
   private final Module module;
   private final boolean enabled;

   public ModuleToggleEvent(Module module, boolean enabled) {
      this.module = module;
      this.enabled = enabled;
   }

   public Module getModule() {
      return this.module;
   }

   public boolean isEnabled() {
      return this.enabled;
   }
}
