package daamky.ua.api.events.listener;

import daamky.ua.api.events.Event;

@FunctionalInterface
public interface EventListener<T extends Event> {
   void invoke(T var1) throws Exception;
}
