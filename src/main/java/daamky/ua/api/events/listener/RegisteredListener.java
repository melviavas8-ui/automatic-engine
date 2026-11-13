package daamky.ua.api.events.listener;

import daamky.ua.api.events.Event;
import daamky.ua.api.events.types.EventPriority;

public final class RegisteredListener<T extends Event> {
   private final Class<T> eventType;
   private final EventListener<? super T> listener;
   private final EventPriority priority;
   private final boolean receiveCancelled;
   private final Object owner;
   private final long order;

   public RegisteredListener(Class<T> eventType, EventListener<? super T> listener, EventPriority priority, boolean receiveCancelled, Object owner, long order) {
      this.eventType = eventType;
      this.listener = listener;
      this.priority = priority;
      this.receiveCancelled = receiveCancelled;
      this.owner = owner;
      this.order = order;
   }

   public Class<T> getEventType() {
      return this.eventType;
   }

   public EventListener<? super T> getListener() {
      return this.listener;
   }

   public EventPriority getPriority() {
      return this.priority;
   }

   public boolean isReceiveCancelled() {
      return this.receiveCancelled;
   }

   public Object getOwner() {
      return this.owner;
   }

   public long getOrder() {
      return this.order;
   }
}
