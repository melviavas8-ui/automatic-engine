package daamky.ua.api.events.bus;

import java.util.concurrent.atomic.AtomicBoolean;
import daamky.ua.api.events.Event;
import daamky.ua.api.events.listener.RegisteredListener;

public final class EventSubscription implements AutoCloseable {
   private final EventBus eventBus;
   private final Class<? extends Event> eventType;
   private final RegisteredListener<?> listener;
   private final AtomicBoolean active = new AtomicBoolean(true);

   EventSubscription(EventBus eventBus, Class<? extends Event> eventType, RegisteredListener<?> listener) {
      this.eventBus = eventBus;
      this.eventType = eventType;
      this.listener = listener;
   }

   public boolean isActive() {
      return this.active.get();
   }

   public void unsubscribe() {
      if (this.active.compareAndSet(true, false)) {
         this.eventBus.unsubscribe(this.eventType, this.listener);
      }
   }

   @Override
   public void close() {
      this.unsubscribe();
   }
}
