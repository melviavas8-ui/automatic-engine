package daamky.ua.utils.player;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public final class BaritoneMovementHelper {
   private BaritoneMovementHelper() {
   }

   public static boolean isBaritoneActive() {
      return isBaritoneActive(MinecraftClient.getInstance().player);
   }

   public static boolean isBaritoneActive(ClientPlayerEntity player) {
      return isMovementControlled(player);
   }

   public static boolean isMovementControlled(ClientPlayerEntity player) {
      if (player != null && player.input != null) {
         String inputName = player.input.getClass().getName().toLowerCase(Locale.ROOT);
         return inputName.contains("baritone") || inputName.contains("playermovementinput");
      } else {
         return false;
      }
   }
}
