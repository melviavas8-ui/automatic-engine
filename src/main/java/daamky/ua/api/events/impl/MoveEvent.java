package daamky.ua.api.events.impl;

import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.Event;

public final class MoveEvent implements Event {
   private Vec3d movement;

   public MoveEvent(Vec3d movement) {
      this.movement = movement;
   }

   public Vec3d getMovement() {
      return this.movement;
   }

   public void setMovement(Vec3d movement) {
      this.movement = movement;
   }
}
