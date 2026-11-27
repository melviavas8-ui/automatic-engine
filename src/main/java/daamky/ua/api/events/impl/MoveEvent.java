package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;
import net.minecraft.world.phys.Vec3;

public final class MoveEvent implements Event {
   private Vec3 movement;

   public MoveEvent(Vec3 movement) {
      this.movement = movement;
   }

   public Vec3 getMovement() {
      return this.movement;
   }

   public void setMovement(Vec3 movement) {
      this.movement = movement;
   }
}
