package daamky.ua.api.events.impl;

import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.Event;

public final class FireworkEvent implements Event {
   private Vec3d vector;

   public FireworkEvent(Vec3d vector) {
      this.vector = vector;
   }

   public Vec3d getVector() {
      return this.vector;
   }

   public void setVector(Vec3d vector) {
      this.vector = vector;
   }
}
