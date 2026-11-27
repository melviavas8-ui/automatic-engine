package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;
import net.minecraft.world.phys.Vec3;

public final class FireworkEvent implements Event {
   private Vec3 vector;

   public FireworkEvent(Vec3 vector) {
      this.vector = vector;
   }

   public Vec3 getVector() {
      return this.vector;
   }

   public void setVector(Vec3 vector) {
      this.vector = vector;
   }
}
