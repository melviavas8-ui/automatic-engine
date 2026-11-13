package daamky.ua.api.events.impl;

import net.minecraft.network.packet.Packet;
import daamky.ua.api.events.CancellableEvent;

public final class PacketEvent extends CancellableEvent {
   private final PacketEvent.Type type;
   private final Packet<?> packet;

   public PacketEvent(PacketEvent.Type type, Packet<?> packet) {
      this.type = type;
      this.packet = packet;
   }

   public PacketEvent.Type getType() {
      return this.type;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public boolean isSend() {
      return this.type == PacketEvent.Type.SEND;
   }

   public boolean isReceive() {
      return this.type == PacketEvent.Type.RECEIVE;
   }

   public static enum Type {
      SEND,
      RECEIVE;

      private Type() {
      }
   }
}
