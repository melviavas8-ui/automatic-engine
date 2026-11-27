package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;
import net.minecraft.world.phys.Vec3;

public final class PlayerTravelEvent extends CancellableEvent {
   private Vec3 motion;
   private final boolean pre;

   public PlayerTravelEvent(Vec3 motion, boolean pre) {
      this.motion = motion;
      this.pre = pre;
   }

   public Vec3 getMotion() {
      return this.motion;
   }

   public void setMotion(Vec3 motion) {
      this.motion = motion;
   }

   public boolean isPre() {
      return this.pre;
   }

   public boolean isPost() {
      return !this.pre;
   }
}
