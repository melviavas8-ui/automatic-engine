package daamky.ua.utils.modules.warden.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

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
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player != null) {
         freeYaw = player.getYaw();
         freePitch = player.getPitch();
      }
   }

   public static void applyMouseDelta(double cursorDeltaX, double cursorDeltaY) {
      if (active) {
         freeYaw += (float)cursorDeltaX * 0.15F;
         freePitch = MathHelper.clamp(freePitch + (float)cursorDeltaY * 0.15F, -90.0F, 90.0F);
      }
   }

   public static void restorePlayerLook() {
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player != null) {
         player.setYaw(freeYaw);
         player.setPitch(freePitch);
         player.lastYaw = freeYaw;
         player.lastPitch = freePitch;
         player.headYaw = freeYaw;
         player.lastHeadYaw = freeYaw;
         player.bodyYaw = freeYaw;
         player.lastBodyYaw = freeYaw;
      }
   }
}
