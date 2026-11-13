package daamky.ua.api.events.impl;

import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.CancellableEvent;

public final class SwimmingEvent extends CancellableEvent {
   private Vec3d vector;

   public SwimmingEvent(Vec3d vector) {
      this.vector = vector;
   }

   public Vec3d getVector() {
      return this.vector;
   }

   public void setVector(Vec3d vector) {
      this.vector = vector;
   }
}
