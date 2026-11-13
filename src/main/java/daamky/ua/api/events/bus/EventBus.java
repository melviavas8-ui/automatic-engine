package daamky.ua.api.events.bus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import daamky.ua.api.events.Cancellable;
import daamky.ua.api.events.Event;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.exception.EventDispatchException;
import daamky.ua.api.events.exception.EventRegistrationException;
import daamky.ua.api.events.listener.EventListener;
import daamky.ua.api.events.listener.RegisteredListener;
import daamky.ua.api.events.types.EventPriority;

public final class EventBus {
   private final Map<Class<? extends Event>, CopyOnWriteArrayList<RegisteredListener<?>>> listeners = new ConcurrentHashMap<>();
   private final Map<Class<? extends Event>, RegisteredListener<?>[]> listenerCache = new ConcurrentHashMap<>();
   private final Map<Object, List<EventSubscription>> ownerSubscriptions = Collections.synchronizedMap(new IdentityHashMap<>());
   private final AtomicLong orderCounter = new AtomicLong();

   public EventBus() {
   }

   public <T extends Event> EventSubscription subscribe(Class<T> eventType, EventListener<? super T> listener) {
      return this.subscribe(eventType, EventPriority.NORMAL, false, listener, listener);
   }

   public <T extends Event> EventSubscription subscribe(Class<T> eventType, EventPriority priority, EventListener<? super T> listener) {
      return this.subscribe(eventType, priority, false, listener, listener);
   }

   public <T extends Event> EventSubscription subscribe(Class<T> eventType, EventPriority priority, boolean receiveCancelled, EventListener<? super T> listener) {
      return this.subscribe(eventType, priority, receiveCancelled, listener, listener);
   }

   public void register(Object owner) {
      if (owner == null) {
         throw new EventRegistrationException("Event owner cannot be null");
      } else {
         List<EventSubscription> subscriptions = new ArrayList<>();

         for (Method method : owner.getClass().getDeclaredMethods()) {
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            if (annotation != null) {
               subscriptions.add(this.registerMethod(owner, method, annotation));
            }
         }

         if (!subscriptions.isEmpty()) {
            this.ownerSubscriptions.put(owner, subscriptions);
         }
      }
   }

   public void unregister(Object owner) {
      if (owner != null) {
         List<EventSubscription> subscriptions = this.ownerSubscriptions.remove(owner);
         if (subscriptions != null) {
            for (EventSubscription subscription : subscriptions) {
               subscription.unsubscribe();
            }
         }
      }
   }

   public <T extends Event> T post(T event) {
      if (event == null) {
         throw new EventDispatchException(null, new NullPointerException("event"));
      } else {
         RegisteredListener<?>[] eventListeners = this.getListeners((Class<? extends Event>)event.getClass());

         for (RegisteredListener<?> listener : eventListeners) {
            if (!(event instanceof Cancellable cancellable) || !cancellable.isCancelled() || listener.isReceiveCancelled()) {
               this.invoke(listener, event);
            }
         }

         return event;
      }
   }

   private <T extends Event> EventSubscription subscribe(
      Class<T> eventType, EventPriority priority, boolean receiveCancelled, EventListener<? super T> listener, Object owner
   ) {
      if (eventType == null) {
         throw new EventRegistrationException("Event type cannot be null");
      } else if (listener == null) {
         throw new EventRegistrationException("Event listener cannot be null");
      } else {
         RegisteredListener<T> registered = new RegisteredListener<>(
            eventType, listener, priority == null ? EventPriority.NORMAL : priority, receiveCancelled, owner, this.orderCounter.incrementAndGet()
         );
         this.listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(registered);
         this.listenerCache.clear();
         return new EventSubscription(this, eventType, registered);
      }
   }

   void unsubscribe(Class<? extends Event> eventType, RegisteredListener<?> listener) {
      CopyOnWriteArrayList<RegisteredListener<?>> registeredListeners = this.listeners.get(eventType);
      if (registeredListeners != null) {
         registeredListeners.remove(listener);
         if (registeredListeners.isEmpty()) {
            this.listeners.remove(eventType);
         }

         this.listenerCache.clear();
      }
   }

   private EventSubscription registerMethod(Object owner, Method method, SubscribeEvent annotation) {
      if (method.getParameterCount() != 1) {
         throw new EventRegistrationException("@SubscribeEvent method must have exactly one event parameter: " + method);
      } else {
         Class<?> parameterType = method.getParameterTypes()[0];
         if (!Event.class.isAssignableFrom(parameterType)) {
            throw new EventRegistrationException("@SubscribeEvent parameter must implement Event: " + method);
         } else {
            method.setAccessible(true);

            MethodHandle handle;
            try {
               handle = MethodHandles.privateLookupIn(owner.getClass(), MethodHandles.lookup()).unreflect(method).bindTo(owner);
            } catch (IllegalAccessException var8) {
               throw new EventRegistrationException("Cannot access @SubscribeEvent method: " + method, var8);
            }

            EventListener<Event> reflectedListener = event -> {
               try {
                  handle.invoke((Event)event);
               } catch (Throwable var4x) {
                  if (var4x instanceof Exception exception) {
                     throw exception;
                  } else if (var4x instanceof Error error) {
                     throw error;
                  } else {
                     throw new EventDispatchException(event, var4x);
                  }
               }
            };
            return this.subscribe((Class)parameterType, annotation.priority(), annotation.receiveCancelled(), (EventListener)reflectedListener, owner);
         }
      }
   }

   private RegisteredListener<?>[] getListeners(Class<? extends Event> eventType) {
      return this.listenerCache.computeIfAbsent(eventType, this::resolveListeners);
   }

   private RegisteredListener<?>[] resolveListeners(Class<? extends Event> eventType) {
      List<RegisteredListener<?>> resolved = new ArrayList<>();

      for (Entry<Class<? extends Event>, CopyOnWriteArrayList<RegisteredListener<?>>> entry : this.listeners.entrySet()) {
         if (entry.getKey().isAssignableFrom(eventType)) {
            resolved.addAll(entry.getValue());
         }
      }

      resolved.sort(
         Comparator.<RegisteredListener<?>>comparingInt(listener -> listener.getPriority().getOrder()).thenComparingLong(RegisteredListener::getOrder)
      );
      return resolved.toArray(RegisteredListener[]::new);
   }

   private void invoke(RegisteredListener<?> listener, Event event) {
      try {
         ((EventListener<Event>)listener.getListener()).invoke(event);
      } catch (Throwable var4) {
         throw new EventDispatchException(event, var4);
      }
   }
}
