package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;
import net.minecraft.world.phys.Vec3;

public final class CameraPositionEvent implements Event {
   private Vec3 pos;

   public CameraPositionEvent(Vec3 pos) {
      this.pos = pos;
   }

   public Vec3 getPos() {
      return this.pos;
   }

   public void setPos(Vec3 pos) {
      this.pos = pos;
   }
}
