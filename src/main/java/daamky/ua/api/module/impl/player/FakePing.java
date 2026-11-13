package daamky.ua.api.module.impl.player;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.NumberSetting;

public final class FakePing extends Module {
   private final NumberSetting pingDelay = this.register(
      new NumberSetting("Ping Delay", "Delays keep alive packets sent back to the server.", 1000.0, 0.0, 10000.0, 50.0)
   );
   private final Deque<FakePing.PendingKeepAlive> pendingKeepAlives = new ArrayDeque<>();
   private boolean releasing;

   public FakePing() {
      super("Fake Ping", "Artificially delays keep alive packets.", ModuleCategory.PLAYER);
   }

   @Override
   protected void onEnable() {
      this.pendingKeepAlives.clear();
      this.releasing = false;
   }

   @Override
   protected void onDisable() {
      this.flushPendingKeepAlives();
      this.pendingKeepAlives.clear();
      this.releasing = false;
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isSend() && !this.releasing && mc.player != null) {
         if (event.getPacket() instanceof KeepAliveC2SPacket || event.getPacket() instanceof CommonPongC2SPacket) {
            this.queuePacket(event.getPacket());
            event.cancel();
         }
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent.Pre event) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         long now = System.currentTimeMillis();

         while (!this.pendingKeepAlives.isEmpty() && this.pendingKeepAlives.peekFirst().sendAtMs() <= now) {
            this.sendQueuedPacket(this.pendingKeepAlives.removeFirst().packet());
         }
      } else {
         this.pendingKeepAlives.clear();
      }
   }

   private void queuePacket(Packet<?> packet) {
      long sendAtMs = System.currentTimeMillis() + Math.max(0L, Math.round(this.pingDelay.getValue()));
      this.pendingKeepAlives.addLast(new FakePing.PendingKeepAlive(packet, sendAtMs));
   }

   private void flushPendingKeepAlives() {
      if (mc.getNetworkHandler() != null) {
         while (!this.pendingKeepAlives.isEmpty()) {
            this.sendQueuedPacket(this.pendingKeepAlives.removeFirst().packet());
         }
      }
   }

   private void sendQueuedPacket(Packet<?> packet) {
      this.releasing = true;

      try {
         mc.getNetworkHandler().sendPacket(packet);
      } finally {
         this.releasing = false;
      }
   }

   private static record PendingKeepAlive(Packet<?> packet, long sendAtMs) {
   }
}
