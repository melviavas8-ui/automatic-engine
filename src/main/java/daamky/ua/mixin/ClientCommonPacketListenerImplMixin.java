package daamky.ua.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.manager.Manager;

@Mixin({ClientCommonNetworkHandler.class})
public abstract class ClientCommonPacketListenerImplMixin {
   public ClientCommonPacketListenerImplMixin() {
   }

   @Inject(
      method = {"onResourcePackSend"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onResourcePackPush(ResourcePackSendS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      ModuleManager modules = Manager.getModules();
      boolean spoofed = modules != null
         && modules.getServerRPSpoofer().isPresent() && modules.getServerRPSpoofer().get().handleResourcePackPush((ClientCommonNetworkHandler)(Object)this, packet);
      if (event.isCancelled() || spoofed) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onDisconnect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onDisconnect(DisconnectS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onKeepAlive"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onKeepAlive(KeepAliveS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onPing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onPing(CommonPingS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}

