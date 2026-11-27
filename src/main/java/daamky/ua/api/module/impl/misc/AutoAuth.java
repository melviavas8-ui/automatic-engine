package daamky.ua.api.module.impl.misc;

import java.util.Locale;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.StringSetting;

public final class AutoAuth extends Module {
   private final StringSetting password = this.register(new StringSetting("Password", "Server auth password.", "daamkyPass123", 64));

   public AutoAuth() {
      super("Auto Auth", "Automatically sends /login and /register commands.", ModuleCategory.MISC);
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive() && mc.player != null && mc.level != null && mc.player.connection != null) {
         if (event.getPacket() instanceof ClientboundSystemChatPacket packet) {
            String pass = this.password.getValue() == null ? "" : this.password.getValue().trim();
            if (pass.length() >= 4) {
               String message = packet.content().getString().toLowerCase(Locale.ROOT);
               if (!message.contains("войдите") && !message.contains("/login")) {
                  if (message.contains("зарегистрируйтесь") || message.contains("регистрация") || message.contains("/reg")) {
                     mc.player.connection.sendCommand("reg " + pass + " " + pass);
                  }
               } else {
                  mc.player.connection.sendCommand("login " + pass);
               }
            }
         }
      }
   }
}
