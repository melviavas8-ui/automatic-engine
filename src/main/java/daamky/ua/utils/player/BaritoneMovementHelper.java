package daamky.ua.utils.player;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class BaritoneMovementHelper {
   private BaritoneMovementHelper() {
   }

   public static boolean isBaritoneActive() {
      return isBaritoneActive(Minecraft.getInstance().player);
   }

   public static boolean isBaritoneActive(LocalPlayer player) {
      return isMovementControlled(player);
   }

   public static boolean isMovementControlled(LocalPlayer player) {
      if (player != null && player.input != null) {
         String inputName = player.input.getClass().getName().toLowerCase(Locale.ROOT);
         return inputName.contains("baritone") || inputName.contains("playermovementinput");
      } else {
         return false;
      }
   }
}
