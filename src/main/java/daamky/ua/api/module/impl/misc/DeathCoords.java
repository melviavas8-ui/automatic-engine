package daamky.ua.api.module.impl.misc;

import java.util.Locale;
import net.minecraft.network.chat.ClickEvent.CopyToClipboard;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;

public final class DeathCoords extends Module {
   public DeathCoords() {
      super("Death Coords", "Prints death coordinates to chat.", ModuleCategory.MISC);
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive() && mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ClientboundPlayerCombatKillPacket) {
            String coords = String.format(Locale.ROOT, "%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            Component text = Component.literal(coords).withStyle(style -> style.withClickEvent(new CopyToClipboard(coords)));
            mc.player.displayClientMessage(text, false);
         }
      }
   }
}
