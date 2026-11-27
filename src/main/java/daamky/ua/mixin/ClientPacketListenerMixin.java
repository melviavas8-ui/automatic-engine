package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.ChatEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.manager.Manager;
import daamky.ua.utils.network.Network;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;

@Mixin({ClientPacketListener.class})
public abstract class ClientPacketListenerMixin {
   public ClientPacketListenerMixin() {
   }

   @Inject(
      method = {"sendChat"},
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
      method = {"handleMovePlayer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onMovePlayer(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleMoveEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onMoveEntity(ClientboundMoveEntityPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleEntityPositionSync"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onEntityPositionSync(ClientboundEntityPositionSyncPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleTeleportEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onTeleportEntity(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleRespawn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleLogin"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"setTitleText"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onSetTitleText(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"setSubtitleText"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onSetSubtitleText(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleSetTime"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
      Network.handleTimePacket();
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleSetEntityMotion"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$onSetEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleSystemChat"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handlePlayerCombatKill"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onPlayerCombatKill(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleTakeItemEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onTakeItemEntity(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleContainerSetSlot"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleChunkBlocksUpdate"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleOpenScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleGameEvent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onGameEvent(ClientboundGameEventPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleEntityEvent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
      PacketEvent event = Manager.postEvent(new PacketEvent(PacketEvent.Type.RECEIVE, packet));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
