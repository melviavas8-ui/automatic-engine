package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;
import net.minecraft.world.phys.Vec3;

public final class PlayerVelocityStrafeEvent implements Event {
   private final Vec3 movementInput;
   private final float speed;
   private final float yaw;
   private Vec3 velocity;

   public PlayerVelocityStrafeEvent(Vec3 movementInput, float speed, float yaw, Vec3 velocity) {
      this.movementInput = movementInput;
      this.speed = speed;
      this.yaw = yaw;
      this.velocity = velocity;
   }

   public Vec3 getMovementInput() {
      return this.movementInput;
   }

   public float getSpeed() {
      return this.speed;
   }

   public float getYaw() {
      return this.yaw;
   }

   public Vec3 getVelocity() {
      return this.velocity;
   }

   public void setVelocity(Vec3 velocity) {
      this.velocity = velocity;
   }
}
