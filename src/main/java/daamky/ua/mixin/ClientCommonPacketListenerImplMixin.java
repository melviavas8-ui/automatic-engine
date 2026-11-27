package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.manager.Manager;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;

@Mixin({ClientCommonPacketListenerImpl.class})
public abstract class ClientCommonPacketListenerImplMixin {
   public ClientCommonPacketListenerImplMixin() {
   }

   @Inject(
      method = {"handleResourcePackPush"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onResourcePackPush(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      ModuleManager modules = Manager.getModules();
      boolean spoofed = modules != null
         && modules.getServerRPSpoofer().isPresent() && modules.getServerRPSpoofer().get().handleResourcePackPush((ClientCommonPacketListenerImpl)(Object)this, packet);
      if (event.isCancelled() || spoofed) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleDisconnect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onDisconnect(ClientboundDisconnectPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleKeepAlive"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onKeepAlive(ClientboundKeepAlivePacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handlePing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onPing(ClientboundPingPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}

