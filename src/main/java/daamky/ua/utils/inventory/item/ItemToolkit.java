package daamky.ua.utils.inventory.item;

import java.util.Objects;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import daamky.ua.IMinecraft;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;

public class ItemToolkit implements IMinecraft {
   public static final ItemToolkit INSTANCE = new ItemToolkit();
   private boolean useItem;
   private boolean releaseItem = true;

   private ItemToolkit() {
   }

   public boolean isUseItem() {
      return this.useItem;
   }

   public void setUseItem(boolean useItem) {
      this.useItem = useItem;
   }

   public boolean isReleaseItem() {
      return this.releaseItem;
   }

   public void setReleaseItem(boolean releaseItem) {
      this.releaseItem = releaseItem;
   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      Object packet = event.getPacket();
      Objects.requireNonNull(packet);
      switch (packet) {
         case ServerboundPlayerActionPacket actionPacket when actionPacket.getAction() == Action.RELEASE_USE_ITEM:
            this.releaseItem = true;
            break;
         case ServerboundClientCommandPacket statusPacket when statusPacket.getAction() == net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN:
            this.releaseItem = true;
            break;
         case ClientboundRespawnPacket ignored:
            this.releaseItem = true;
            break;
         case ClientboundLoginPacket ignoredx:
            this.releaseItem = true;
            break;
         default:
            break;
      }
   }

   public void useHand(InteractionHand hand) {
      if (mc.player != null && mc.gameMode != null && hand != null) {
         if (this.releaseItem) {
            PlayerInteractionHelper.interactItem(hand);
            this.releaseItem = false;
         }

         this.useItem = true;
      }
   }
}
