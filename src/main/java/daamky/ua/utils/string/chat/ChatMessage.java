package daamky.ua.utils.string.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import daamky.ua.utils.string.chat.helper.TextHelper;

public final class ChatMessage {
   private ChatMessage() {
   }

   public static MutableText brandmessage() {
      return (MutableText)TextHelper.applyPredefinedGradient("daamky", "black_light_purple", true);
   }

   public static MutableText blockesp() {
      return (MutableText)TextHelper.applyPredefinedGradient("Block Esp", "black_light_purple", true);
   }

   public static MutableText autobuy() {
      return (MutableText)TextHelper.applyPredefinedGradient("Auto Buy", "black_light_purple", true);
   }

   public static MutableText autobuiparcer() {
      return (MutableText)TextHelper.applyPredefinedGradient("Parce price", "black_light_purple", true);
   }

   public static void brandmessage(String message) {
      brandmessage(Text.literal(message == null ? "" : message));
   }

   public static void brandmessage(Text message) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         Text prefix = TextHelper.applyPredefinedGradient("daamky -> ", "black_light_purple", true);
         mc.player.sendMessage(prefix.copy().append((Text)(message == null ? Text.empty() : message)), false);
      }
   }

   public static void trackerMessage(Text message) {
      brandmessage(message);
   }

   public static void autobuymessage(String message) {
      prefixed("AutoBuy -> ", Text.literal(message == null ? "" : message));
   }

   public static void autobuymessageSuccess(String message) {
      prefixed("AutoBuy -> ", Text.literal(message == null ? "" : message).setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
   }

   public static void autobuymessageError(String message) {
      prefixed("AutoBuy -> ", Text.literal(message == null ? "" : message).setStyle(Style.EMPTY.withColor(Formatting.RED)));
   }

   public static void autobuymessageWarning(String message) {
      prefixed("AutoBuy -> ", Text.literal(message == null ? "" : message).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
   }

   public static void ancientmessage(String message) {
      prefixed("Ancient Xray -> ", Text.literal(message == null ? "" : message));
   }

   public static void helpmessage(String message) {
      prefixed("Help -> ", Text.literal(message == null ? "" : message));
   }

   public static void swapmessage(String message) {
      prefixed("AutoSwap -> ", Text.literal(message == null ? "" : message));
   }

   private static void prefixed(String prefixText, Text message) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         Text prefix = TextHelper.applyPredefinedGradient(prefixText, "black_light_purple", true);
         mc.player.sendMessage(prefix.copy().append(message), false);
      }
   }
}
