package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;

public final class SwingDurationEvent extends CancellableEvent {
   private float animation = 1.0F;

   public SwingDurationEvent() {
   }

   public float getAnimation() {
      return this.animation;
   }

   public void setAnimation(float animation) {
      this.animation = animation;
   }
}
