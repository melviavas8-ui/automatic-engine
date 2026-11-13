package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;

public final class HotBarScrollEvent extends CancellableEvent {
   private final double horizontal;
   private final double vertical;

   public HotBarScrollEvent(double horizontal, double vertical) {
      this.horizontal = horizontal;
      this.vertical = vertical;
   }

   public double getHorizontal() {
      return this.horizontal;
   }

   public double getVertical() {
      return this.vertical;
   }
}
