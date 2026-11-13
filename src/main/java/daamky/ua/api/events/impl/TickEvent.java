package daamky.ua.api.events.impl;

import net.minecraft.client.MinecraftClient;
import daamky.ua.api.events.Event;
import daamky.ua.api.events.types.EventPhase;

public abstract class TickEvent implements Event {
   private final MinecraftClient client;
   private final EventPhase phase;

   private TickEvent(MinecraftClient client, EventPhase phase) {
      this.client = client;
      this.phase = phase;
   }

   public MinecraftClient getClient() {
      return this.client;
   }

   public EventPhase getPhase() {
      return this.phase;
   }

   public boolean isPre() {
      return this.phase == EventPhase.PRE;
   }

   public boolean isPost() {
      return this.phase == EventPhase.POST;
   }

   public static final class Post extends TickEvent {
      public Post(MinecraftClient client) {
         super(client, EventPhase.POST);
      }
   }

   public static final class Pre extends TickEvent {
      public Pre(MinecraftClient client) {
         super(client, EventPhase.PRE);
      }
   }
}
