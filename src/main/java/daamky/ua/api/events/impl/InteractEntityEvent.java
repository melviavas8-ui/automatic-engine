package daamky.ua.api.events.impl;

import net.minecraft.entity.Entity;
import daamky.ua.api.events.CancellableEvent;

public final class InteractEntityEvent extends CancellableEvent {
   private final Entity entity;

   public InteractEntityEvent(Entity entity) {
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
