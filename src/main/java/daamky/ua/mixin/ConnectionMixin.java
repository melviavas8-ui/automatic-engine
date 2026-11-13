package daamky.ua.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import java.net.InetSocketAddress;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.handler.PacketSizeLogger;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.common.proxy.Proxy;
import daamky.ua.common.proxy.ProxyServer;
import daamky.ua.manager.Manager;

@Mixin({ClientConnection.class})
public abstract class ConnectionMixin {
   public ConnectionMixin() {
   }

   @Inject(
      method = {"sendImmediately"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onSendPacket(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.SEND, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"addHandlers"},
      at = {@At("RETURN")}
   )
   private static void daamky$addProxyHandler(
      ChannelPipeline pipeline, NetworkSide flow, boolean local, PacketSizeLogger bandwidthDebugMonitor, CallbackInfo ci
   ) {
      Proxy proxy = ProxyServer.proxy;
      if (!local && flow == NetworkSide.CLIENTBOUND && ProxyServer.proxyEnabled && proxy != null && proxy.hasAddress()) {
         if (pipeline.get("daamky_proxy") == null) {
            InetSocketAddress address = new InetSocketAddress(proxy.getIp(), proxy.getPort());
            if (proxy.type == Proxy.ProxyType.SOCKS4 && proxy.password.isEmpty()) {
               pipeline.addFirst("daamky_proxy", proxy.username.isEmpty() ? new Socks4ProxyHandler(address) : new Socks4ProxyHandler(address, proxy.username));
            } else if (proxy.username.isEmpty() && proxy.password.isEmpty()) {
               pipeline.addFirst("daamky_proxy", new Socks5ProxyHandler(address));
            } else {
               pipeline.addFirst("daamky_proxy", new Socks5ProxyHandler(address, proxy.username, proxy.password));
            }

            ProxyServer.lastUsedProxy = proxy;
            System.out
               .println(
                  "[daamky Proxy] Enabled "
                     + (proxy.type == Proxy.ProxyType.SOCKS4 && proxy.password.isEmpty() ? "SOCKS4" : "SOCKS5")
                     + " proxy: "
                     + proxy.ipPort
               );
         }
      }
   }
}
