package daamky.ua.utils.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class ChatMessage {
   private ChatMessage() {
   }

   public static MutableText brandmessage() {
      return Text.literal("[daamky] ");
   }

   public static void brandmessage(String message) {
      brandmessage(Text.literal(message));
   }

   public static void brandmessage(Text message) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && message != null) {
         mc.player.sendMessage(brandmessage().append(message), false);
      }
   }
}
