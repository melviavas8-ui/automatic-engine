package daamky.ua.utils.inventory.item;

import java.util.Objects;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.Hand;
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
         case PlayerActionC2SPacket actionPacket when actionPacket.getAction() == Action.RELEASE_USE_ITEM:
            this.releaseItem = true;
            break;
         case ClientStatusC2SPacket statusPacket when statusPacket.getMode() == Mode.PERFORM_RESPAWN:
            this.releaseItem = true;
            break;
         case PlayerRespawnS2CPacket ignored:
            this.releaseItem = true;
            break;
         case GameJoinS2CPacket ignoredx:
            this.releaseItem = true;
            break;
         default:
            break;
      }
   }

   public void useHand(Hand hand) {
      if (mc.player != null && mc.interactionManager != null && hand != null) {
         if (this.releaseItem) {
            PlayerInteractionHelper.interactItem(hand);
            this.releaseItem = false;
         }

         this.useItem = true;
      }
   }
}
