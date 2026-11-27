package daamky.ua.api.events.impl;

import daamky.ua.api.events.Event;
import daamky.ua.api.events.types.EventPhase;
import net.minecraft.client.Minecraft;

public abstract class TickEvent implements Event {
   private final Minecraft client;
   private final EventPhase phase;

   private TickEvent(Minecraft client, EventPhase phase) {
      this.client = client;
      this.phase = phase;
   }

   public Minecraft getClient() {
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
      public Post(Minecraft client) {
         super(client, EventPhase.POST);
      }
   }

   public static final class Pre extends TickEvent {
      public Pre(Minecraft client) {
         super(client, EventPhase.PRE);
      }
   }
}
