package daamky.ua.api.events.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import daamky.ua.api.events.types.EventPriority;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SubscribeEvent {
   EventPriority priority() default EventPriority.NORMAL;

   boolean receiveCancelled() default false;
}
