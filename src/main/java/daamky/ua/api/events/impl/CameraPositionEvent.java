package daamky.ua.api.events.impl;

import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.Event;

public final class CameraPositionEvent implements Event {
   private Vec3d pos;

   public CameraPositionEvent(Vec3d pos) {
      this.pos = pos;
   }

   public Vec3d getPos() {
      return this.pos;
   }

   public void setPos(Vec3d pos) {
      this.pos = pos;
   }
}
