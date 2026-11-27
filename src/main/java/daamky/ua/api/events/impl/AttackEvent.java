package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;
import net.minecraft.world.entity.Entity;

public final class AttackEvent extends CancellableEvent {
   private final Entity target;

   public AttackEvent(Entity target) {
      this.target = target;
   }

   public Entity getTarget() {
      return this.target;
   }
}
