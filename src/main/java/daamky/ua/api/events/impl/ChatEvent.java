package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;

public final class ChatEvent extends CancellableEvent {
   private final String message;

   public ChatEvent(String message) {
      this.message = message == null ? "" : message;
   }

   public String getMessage() {
      return this.message;
   }
}
