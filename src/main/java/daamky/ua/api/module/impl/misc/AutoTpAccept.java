package daamky.ua.api.module.impl.misc;

import java.util.Locale;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.utils.repository.friend.FriendUtils;

public final class AutoTpAccept extends Module {
   private static final String[] TELEPORT_MESSAGES = new String[]{
      "has requested teleport", "просит телепортироваться", "хочет телепортироваться к вам", "просит к вам телепортироваться"
   };
   private final BooleanSetting friendsOnly = this.register(new BooleanSetting("Only Friends", "Accept teleport requests only from friends.", false));
   private boolean canAccept;

   public AutoTpAccept() {
      super("Auto Accept", "Automatically accepts teleport requests.", ModuleCategory.MISC);
   }

   @Override
   protected void onEnable() {
      this.canAccept = false;
   }

   @Override
   protected void onDisable() {
      this.canAccept = false;
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive() && event.getPacket() instanceof GameMessageS2CPacket packet) {
         if (!this.isOverlay(packet)) {
            String message = packet.content().getString();
            String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
            if (this.isTeleportMessage(lower)) {
               this.canAccept = !this.friendsOnly.getValue() || this.containsFriendName(lower);
            }
         }
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent event) {
      if (mc.player != null && mc.player.networkHandler != null && this.canAccept) {
         mc.player.networkHandler.sendChatCommand("tpaccept");
         this.canAccept = false;
      }
   }

   private boolean isTeleportMessage(String message) {
      for (String pattern : TELEPORT_MESSAGES) {
         if (message.contains(pattern.toLowerCase(Locale.ROOT))) {
            return true;
         }
      }

      return false;
   }

   private boolean containsFriendName(String message) {
      for (String friend : FriendUtils.friends()) {
         if (friend != null && message.contains(friend.toLowerCase(Locale.ROOT))) {
            return true;
         }
      }

      return false;
   }

   private boolean isOverlay(GameMessageS2CPacket packet) {
      try {
         return packet.overlay();
      } catch (Throwable var3) {
         return false;
      }
   }
}
