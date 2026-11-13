package daamky.ua.common.proxy;

import net.minecraft.client.gui.widget.ButtonWidget;

public class ProxyServer {
   public static boolean proxyEnabled = false;
   public static Proxy proxy = new Proxy();
   public static Proxy lastUsedProxy = new Proxy();
   public static ButtonWidget proxyMenuButton;

   public ProxyServer() {
   }

   public static String getLastUsedProxyIp() {
      return lastUsedProxy.ipPort.isEmpty() ? "none" : lastUsedProxy.getIp();
   }

   public static String getButtonText() {
      if (proxyEnabled && proxy != null && proxy.hasAddress()) {
         return "Proxy: Active";
      } else {
         return proxy != null && proxy.hasAddress() ? "Proxy: Off" : "Proxy";
      }
   }
}
