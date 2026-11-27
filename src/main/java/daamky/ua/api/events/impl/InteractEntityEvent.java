package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;
import net.minecraft.world.entity.Entity;

public final class InteractEntityEvent extends CancellableEvent {
   private final Entity entity;

   public InteractEntityEvent(Entity entity) {
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
