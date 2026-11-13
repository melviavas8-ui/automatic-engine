package daamky.ua.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.ChatEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.manager.Manager;
import daamky.ua.utils.network.Network;

@Mixin({ClientPlayNetworkHandler.class})
public abstract class ClientPacketListenerMixin {
   public ClientPacketListenerMixin() {
   }

   @Inject(
      method = {"sendChatMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onSendChat(String message, CallbackInfo ci) {
      ChatEvent event = Manager.postEvent(new ChatEvent(message));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onPlayerPositionLook"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onMovePlayer(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onMoveEntity(EntityS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onEntityPositionSync"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onEntityPositionSync(EntityPositionSyncS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onEntityPosition"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onTeleportEntity(EntityPositionS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onPlayerRespawn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onGameJoin"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onLogin(GameJoinS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onTitle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onSetTitleText(TitleS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onSubtitle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onSetSubtitleText(SubtitleS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onWorldTimeUpdate"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onSetTime(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
      Network.handleTimePacket();
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onEntityVelocityUpdate"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$onSetEntityMotion(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onGameMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onSystemChat(GameMessageS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onDeathMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onPlayerCombatKill(DeathMessageS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onItemPickupAnimation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onTakeItemEntity(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onScreenHandlerSlotUpdate"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onContainerSetSlot(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onChunkDeltaUpdate"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onChunkBlocksUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onOpenScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onGameStateChange"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onGameEvent(GameStateChangeS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onEntityStatus"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onEntityEvent(EntityStatusS2CPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
