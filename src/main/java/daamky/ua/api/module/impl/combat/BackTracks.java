package daamky.ua.api.module.impl.combat;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.Render3D;

public final class BackTracks extends Module {
   private final NumberSetting distance = this.register(new NumberSetting("Distance", "Maximum distance at which packets are delayed.", 4.5, 1.5, 6.0, 0.1));
   private final NumberSetting packetDelay = this.register(
      new NumberSetting("Packet Delay", "Maximum incoming packet delay in milliseconds.", 500.0, 0.0, 1000.0, 50.0)
   );
   private final BooleanSetting syncAura = this.register(new BooleanSetting("Sync Aura", "Uses Aura's attack range.", false));
   private final BooleanSetting renderPosition = this.register(new BooleanSetting("Render Position", "Draws the target's delayed server position.", true));
   private final Deque<BackTracks.PacketData> packetQueue = new ArrayDeque<>();
   private LivingEntity trackedEntity;
   private Vec3 realPosition;
   private Vec3 interpolatedPosition;
   private boolean releasing;

   public BackTracks() {
      super("Back Tracks", "Delays target updates to keep its previous position attackable.", ModuleCategory.COMBAT);
      this.distance.visibleWhen(() -> !this.syncAura.getValue());
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive() && !this.releasing && mc.player != null && mc.level != null) {
         Packet<?> packet = event.getPacket();
         if (!this.isIgnoredPacket(packet)) {
            if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundRespawnPacket || packet instanceof ClientboundLoginPacket) {
               this.reset();
            } else if (this.shouldLag()) {
               this.updateRealPosition(packet);
               event.cancel();
               this.packetQueue.addLast(new BackTracks.PacketData(packet, System.currentTimeMillis()));
            }
         }
      }
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.level != null) {
         AuraModule aura = AuraModule.getInstance();
         LivingEntity auraTarget = aura != null && aura.isEnabled() ? AuraModule.target : null;
         if (auraTarget != null && auraTarget != this.trackedEntity) {
            this.trackedEntity = auraTarget;
            this.realPosition = auraTarget.position();
            this.interpolatedPosition = this.realPosition;
         }

         if (auraTarget == null && this.trackedEntity != null) {
            this.reset();
         } else if (!this.packetQueue.isEmpty() || this.trackedEntity != null) {
            if (this.shouldLag()) {
               this.releasePackets(false);
            } else {
               this.reset();
            }
         }
      } else {
         this.clearState();
      }
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      if (this.renderPosition.getValue() && this.realPosition != null && this.trackedEntity != null && this.trackedEntity.isAlive()) {
         if (this.interpolatedPosition == null || this.realPosition.distanceTo(this.interpolatedPosition) >= 2.0) {
            this.interpolatedPosition = this.realPosition;
         }

         this.interpolatedPosition = this.interpolate(this.interpolatedPosition, this.realPosition, 15.0);
         double halfWidth = (double)this.trackedEntity.getBbWidth() / 2.0;
         AABB box = new AABB(
            this.interpolatedPosition.x - halfWidth,
            this.interpolatedPosition.y,
            this.interpolatedPosition.z - halfWidth,
            this.interpolatedPosition.x + halfWidth,
            this.interpolatedPosition.y + (double)this.trackedEntity.getBbHeight(),
            this.interpolatedPosition.z + halfWidth
         );
         Render3D.drawBox(box, -1, 1.0F);
      }
   }

   @Override
   protected void onEnable() {
      this.clearState();
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void updateRealPosition(Packet<?> packet) {
      if (this.trackedEntity != null && this.realPosition != null) {
         if (packet instanceof ClientboundMoveEntityPacket move && move.getEntity(mc.level) == this.trackedEntity && move.hasPosition()) {
            this.realPosition = this.realPosition.add((double)move.getXa() / 4096.0, (double)move.getYa() / 4096.0, (double)move.getZa() / 4096.0);
            return;
         }

         if (packet instanceof ClientboundEntityPositionSyncPacket sync && sync.id() == this.trackedEntity.getId()) {
            this.realPosition = sync.values().position();
            return;
         }

         if (packet instanceof ClientboundTeleportEntityPacket teleport && teleport.id() == this.trackedEntity.getId()) {
            PositionMoveRotation current = new PositionMoveRotation(
               this.realPosition, this.trackedEntity.getDeltaMovement(), this.trackedEntity.getYRot(), this.trackedEntity.getXRot()
            );
            this.realPosition = PositionMoveRotation.calculateAbsolute(current, teleport.change(), teleport.relatives()).position();
         }
      }
   }

   private void releasePackets(boolean all) {
      if (mc.getConnection() == null) {
         this.packetQueue.clear();
      } else {
         long now = System.currentTimeMillis();

         while (!this.packetQueue.isEmpty()) {
            BackTracks.PacketData data = this.packetQueue.peekFirst();
            if (!all) {
               double factor = Math.min(1.0, mc.player.position().distanceTo(this.realPosition) / this.getRange());
               long delay = Math.round(this.packetDelay.getValue() * factor);
               if (data.timestamp() + delay > now) {
                  break;
               }
            }

            this.packetQueue.removeFirst();
            this.handlePacket(data.packet());
         }
      }
   }

   private void handlePacket(Packet<?> packet) {
      this.releasing = true;

      try {
         ((Packet)packet).handle(mc.getConnection());
      } finally {
         this.releasing = false;
      }
   }

   private boolean shouldLag() {
      return this.trackedEntity != null
         && this.trackedEntity.isAlive()
         && !this.trackedEntity.isInvulnerable()
         && this.realPosition != null
         && mc.player != null
         && mc.player.position().distanceTo(this.realPosition) <= this.getRange();
   }

   private boolean isIgnoredPacket(Packet<?> packet) {
      return packet instanceof ClientboundSoundPacket
         || packet instanceof ClientboundSoundEntityPacket
         || packet instanceof ClientboundEntityEventPacket
         || packet instanceof ClientboundKeepAlivePacket
         || packet instanceof ClientboundPingPacket;
   }

   private double getRange() {
      AuraModule aura = AuraModule.getInstance();
      return this.syncAura.getValue() && aura != null && aura.isEnabled() ? aura.getRange().getValue() : this.distance.getValue();
   }

   private Vec3 interpolate(Vec3 current, Vec3 target, double speed) {
      return new Vec3(current.x + (target.x - current.x) / speed, current.y + (target.y - current.y) / speed, current.z + (target.z - current.z) / speed);
   }

   private void reset() {
      this.releasePackets(true);
      this.clearState();
   }

   private void clearState() {
      this.packetQueue.clear();
      this.trackedEntity = null;
      this.realPosition = null;
      this.interpolatedPosition = null;
      this.releasing = false;
   }

   private static record PacketData(Packet<?> packet, long timestamp) {
   }
}
