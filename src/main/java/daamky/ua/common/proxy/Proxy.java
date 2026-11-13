package daamky.ua.common.proxy;

import com.google.gson.annotations.SerializedName;

public class Proxy {
   @SerializedName("IP:PORT")
   public String ipPort = "";
   public Proxy.ProxyType type = Proxy.ProxyType.SOCKS5;
   public String username = "";
   public String password = "";

   public Proxy() {
   }

   public Proxy(boolean socks4, String ipPort, String username, String password) {
      this.type = socks4 ? Proxy.ProxyType.SOCKS4 : Proxy.ProxyType.SOCKS5;
      this.ipPort = ipPort == null ? "" : ipPort.trim();
      this.username = username == null ? "" : username;
      this.password = password == null ? "" : password;
   }

   public int getPort() {
      int separator = this.ipPort == null ? -1 : this.ipPort.lastIndexOf(58);
      if (separator >= 0 && separator != this.ipPort.length() - 1) {
         try {
            return Integer.parseInt(this.ipPort.substring(separator + 1));
         } catch (NumberFormatException var3) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public String getIp() {
      int separator = this.ipPort == null ? -1 : this.ipPort.lastIndexOf(58);
      return separator <= 0 ? "" : this.ipPort.substring(0, separator);
   }

   public boolean hasAddress() {
      return !this.getIp().isEmpty() && this.getPort() > 0 && this.getPort() <= 65535;
   }

   public static enum ProxyType {
      SOCKS4,
      SOCKS5;

      private ProxyType() {
      }
   }
}
