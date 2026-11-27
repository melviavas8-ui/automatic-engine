package daamky.ua.utils.modules.warden.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class FreeLookController {
   private static boolean active;
   private static float freeYaw;
   private static float freePitch;

   private FreeLookController() {
   }

   public static boolean isActive() {
      return active;
   }

   public static float getFreeYaw() {
      return freeYaw;
   }

   public static float getFreePitch() {
      return freePitch;
   }

   public static void activate() {
      if (!active) {
         syncFromPlayer();
         active = true;
      }
   }

   public static void deactivate() {
      active = false;
   }

   public static void syncFromPlayer() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         freeYaw = player.getYRot();
         freePitch = player.getXRot();
      }
   }

   public static void applyMouseDelta(double cursorDeltaX, double cursorDeltaY) {
      if (active) {
         freeYaw += (float)cursorDeltaX * 0.15F;
         freePitch = Mth.clamp(freePitch + (float)cursorDeltaY * 0.15F, -90.0F, 90.0F);
      }
   }

   public static void restorePlayerLook() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         player.setYRot(freeYaw);
         player.setXRot(freePitch);
         player.yRotO = freeYaw;
         player.xRotO = freePitch;
         player.yHeadRot = freeYaw;
         player.yHeadRotO = freeYaw;
         player.yBodyRot = freeYaw;
         player.yBodyRotO = freeYaw;
      }
   }
}
