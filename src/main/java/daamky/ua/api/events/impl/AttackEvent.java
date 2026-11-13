package daamky.ua.api.events.impl;

import net.minecraft.entity.Entity;
import daamky.ua.api.events.CancellableEvent;

public final class AttackEvent extends CancellableEvent {
   private final Entity target;

   public AttackEvent(Entity target) {
      this.target = target;
   }

   public Entity getTarget() {
      return this.target;
   }
}
