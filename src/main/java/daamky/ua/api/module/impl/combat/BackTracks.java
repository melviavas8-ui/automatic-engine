package daamky.ua.api.module.impl.combat;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityPosition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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
   private Vec3d realPosition;
   private Vec3d interpolatedPosition;
   private boolean releasing;

   public BackTracks() {
      super("Back Tracks", "Delays target updates to keep its previous position attackable.", ModuleCategory.COMBAT);
      this.distance.visibleWhen(() -> !this.syncAura.getValue());
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive() && !this.releasing && mc.player != null && mc.world != null) {
         Packet<?> packet = event.getPacket();
         if (!this.isIgnoredPacket(packet)) {
            if (packet instanceof PlayerPositionLookS2CPacket || packet instanceof PlayerRespawnS2CPacket || packet instanceof GameJoinS2CPacket) {
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
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         AuraModule aura = AuraModule.getInstance();
         LivingEntity auraTarget = aura != null && aura.isEnabled() ? AuraModule.target : null;
         if (auraTarget != null && auraTarget != this.trackedEntity) {
            this.trackedEntity = auraTarget;
            this.realPosition = auraTarget.getEntityPos();
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
         double halfWidth = (double)this.trackedEntity.getWidth() / 2.0;
         Box box = new Box(
            this.interpolatedPosition.x - halfWidth,
            this.interpolatedPosition.y,
            this.interpolatedPosition.z - halfWidth,
            this.interpolatedPosition.x + halfWidth,
            this.interpolatedPosition.y + (double)this.trackedEntity.getHeight(),
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
         if (packet instanceof EntityS2CPacket move && move.getEntity(mc.world) == this.trackedEntity && move.isPositionChanged()) {
            this.realPosition = this.realPosition.add((double)move.getDeltaX() / 4096.0, (double)move.getDeltaY() / 4096.0, (double)move.getDeltaZ() / 4096.0);
            return;
         }

         if (packet instanceof EntityPositionSyncS2CPacket sync && sync.id() == this.trackedEntity.getId()) {
            this.realPosition = sync.values().position();
            return;
         }

         if (packet instanceof EntityPositionS2CPacket teleport && teleport.entityId() == this.trackedEntity.getId()) {
            EntityPosition current = new EntityPosition(
               this.realPosition, this.trackedEntity.getVelocity(), this.trackedEntity.getYaw(), this.trackedEntity.getPitch()
            );
            this.realPosition = EntityPosition.apply(current, teleport.change(), teleport.relatives()).position();
         }
      }
   }

   private void releasePackets(boolean all) {
      if (mc.getNetworkHandler() == null) {
         this.packetQueue.clear();
      } else {
         long now = System.currentTimeMillis();

         while (!this.packetQueue.isEmpty()) {
            BackTracks.PacketData data = this.packetQueue.peekFirst();
            if (!all) {
               double factor = Math.min(1.0, mc.player.getEntityPos().distanceTo(this.realPosition) / this.getRange());
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
         ((Packet)packet).apply(mc.getNetworkHandler());
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
         && mc.player.getEntityPos().distanceTo(this.realPosition) <= this.getRange();
   }

   private boolean isIgnoredPacket(Packet<?> packet) {
      return packet instanceof PlaySoundS2CPacket
         || packet instanceof PlaySoundFromEntityS2CPacket
         || packet instanceof EntityStatusS2CPacket
         || packet instanceof KeepAliveS2CPacket
         || packet instanceof CommonPingS2CPacket;
   }

   private double getRange() {
      AuraModule aura = AuraModule.getInstance();
      return this.syncAura.getValue() && aura != null && aura.isEnabled() ? aura.getRange().getValue() : this.distance.getValue();
   }

   private Vec3d interpolate(Vec3d current, Vec3d target, double speed) {
      return new Vec3d(current.x + (target.x - current.x) / speed, current.y + (target.y - current.y) / speed, current.z + (target.z - current.z) / speed);
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
