package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;

public final class MouseAppliedRotationEvent implements Event {
   private final float cursorDeltaX;
   private final float cursorDeltaY;
   private final float beforeYaw;
   private final float beforePitch;
   private final float afterYaw;
   private final float afterPitch;

   public MouseAppliedRotationEvent(float cursorDeltaX, float cursorDeltaY, float beforeYaw, float beforePitch, float afterYaw, float afterPitch) {
      this.cursorDeltaX = cursorDeltaX;
      this.cursorDeltaY = cursorDeltaY;
      this.beforeYaw = beforeYaw;
      this.beforePitch = beforePitch;
      this.afterYaw = afterYaw;
      this.afterPitch = afterPitch;
   }

   public float getCursorDeltaX() {
      return this.cursorDeltaX;
   }

   public float getCursorDeltaY() {
      return this.cursorDeltaY;
   }

   public float getBeforeYaw() {
      return this.beforeYaw;
   }

   public float getBeforePitch() {
      return this.beforePitch;
   }

   public float getAfterYaw() {
      return this.afterYaw;
   }

   public float getAfterPitch() {
      return this.afterPitch;
   }
}
