package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;
import net.minecraft.world.phys.Vec3;

public final class SwimmingEvent extends CancellableEvent {
   private Vec3 vector;

   public SwimmingEvent(Vec3 vector) {
      this.vector = vector;
   }

   public Vec3 getVector() {
      return this.vector;
   }

   public void setVector(Vec3 vector) {
      this.vector = vector;
   }
}
