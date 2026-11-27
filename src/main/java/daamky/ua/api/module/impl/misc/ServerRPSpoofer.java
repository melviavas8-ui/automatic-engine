package daamky.ua.api.module.impl.misc;

import java.util.UUID;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;

public final class ServerRPSpoofer extends Module {
   private final StopWatch counter = new StopWatch();
   private ServerRPSpoofer.ResourcePackAction currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
   private UUID resourcePackId;
   private ClientCommonPacketListenerImpl resourcePackListener;

   public ServerRPSpoofer() {
      super("Server RP Spoof", "Spoofs server resource pack acceptance.", ModuleCategory.MISC);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   public boolean handleResourcePackPush(ClientCommonPacketListenerImpl listener, ClientboundResourcePackPushPacket packet) {
      if (this.isEnabled() && listener != null && packet != null) {
         this.resourcePackListener = listener;
         this.resourcePackId = packet.id();
         this.currentAction = ServerRPSpoofer.ResourcePackAction.ACCEPT;
         this.processResourcePackAction();
         return true;
      } else {
         return false;
      }
   }

   @SubscribeEvent(
      receiveCancelled = true
   )
   private void onTick(TickEvent event) {
      this.processResourcePackAction();
   }

   private void processResourcePackAction() {
      ClientCommonPacketListenerImpl connection = this.resourcePackListener;
      if (connection == null) {
         ClientPacketListener playConnection = mc.getConnection();
         if (playConnection != null) {
            connection = playConnection;
         }
      }

      if (connection != null && this.resourcePackId != null) {
         if (this.currentAction == ServerRPSpoofer.ResourcePackAction.ACCEPT) {
            connection.send(new ServerboundResourcePackPacket(this.resourcePackId, Action.ACCEPTED));
            this.currentAction = ServerRPSpoofer.ResourcePackAction.SEND;
            this.counter.reset();
         } else {
            if (this.currentAction == ServerRPSpoofer.ResourcePackAction.SEND && this.counter.finished(300.0)) {
               connection.send(new ServerboundResourcePackPacket(this.resourcePackId, Action.SUCCESSFULLY_LOADED));
               this.reset();
            }
         }
      }
   }

   private void reset() {
      this.currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
      this.resourcePackId = null;
      this.resourcePackListener = null;
   }

   private static enum ResourcePackAction {
      ACCEPT,
      SEND,
      WAIT;

      private ResourcePackAction() {
      }
   }
}
