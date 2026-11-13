package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;

public final class PushEvent extends CancellableEvent {
   private final PushEvent.Type type;

   public PushEvent(PushEvent.Type type) {
      this.type = type;
   }

   public PushEvent.Type getType() {
      return this.type;
   }

   public static enum Type {
      COLLISION,
      BLOCK,
      WATER;

      private Type() {
      }
   }
}
