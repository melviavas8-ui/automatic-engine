package daamky.ua.api.module.impl.misc;

import java.util.UUID;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;

public final class ServerRPSpoofer extends Module {
   private final StopWatch counter = new StopWatch();
   private ServerRPSpoofer.ResourcePackAction currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
   private UUID resourcePackId;
   private ClientCommonNetworkHandler resourcePackListener;

   public ServerRPSpoofer() {
      super("Server RP Spoof", "Spoofs server resource pack acceptance.", ModuleCategory.MISC);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   public boolean handleResourcePackPush(ClientCommonNetworkHandler listener, ResourcePackSendS2CPacket packet) {
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
      ClientCommonNetworkHandler connection = this.resourcePackListener;
      if (connection == null) {
         ClientPlayNetworkHandler playConnection = mc.getNetworkHandler();
         if (playConnection != null) {
            connection = playConnection;
         }
      }

      if (connection != null && this.resourcePackId != null) {
         if (this.currentAction == ServerRPSpoofer.ResourcePackAction.ACCEPT) {
            connection.sendPacket(new ResourcePackStatusC2SPacket(this.resourcePackId, Status.ACCEPTED));
            this.currentAction = ServerRPSpoofer.ResourcePackAction.SEND;
            this.counter.reset();
         } else {
            if (this.currentAction == ServerRPSpoofer.ResourcePackAction.SEND && this.counter.finished(300.0)) {
               connection.sendPacket(new ResourcePackStatusC2SPacket(this.resourcePackId, Status.SUCCESSFULLY_LOADED));
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
