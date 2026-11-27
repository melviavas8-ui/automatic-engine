package daamky.ua.api.module.impl.combat;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.phys.Vec3;

public class Velocity extends Module {
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Knockback reduce mode.", "New Grim", "New Grim", "Old Grim", "Matrix", "Normal"));
   private boolean flag;
   private int grimTicks;
   private int ccCooldown;
   private Vec3 pendingVelocity;

   public Velocity() {
      super("Velocity", "Reduces or cancels knockback.", ModuleCategory.COMBAT);
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (event.isReceive() && client.player != null && !client.player.isInWater() && !client.player.isUnderWater() && !client.player.isInLava()) {
         if (this.ccCooldown > 0) {
            this.ccCooldown--;
         } else {
            if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.getId() == client.player.getId()) {
               this.handleVelocityPacket(event, packet);
            }

            if (this.mode.is("Old Grim") && event.getPacket() instanceof ClientboundPingPacket && this.grimTicks > 0) {
               event.cancel();
               this.grimTicks--;
            }

            if (event.getPacket() instanceof ClientboundPlayerPositionPacket && this.mode.is("New Grim")) {
               this.ccCooldown = 5;
            }
         }
      }
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && !client.player.isInWater() && !client.player.isUnderWater()) {
         if (this.mode.is("Matrix")) {
            this.handleMatrixTick(client);
         }

         if (this.mode.is("New Grim") && this.flag) {
            this.handleNewGrimTick(client);
         }

         if (this.grimTicks > 0) {
            this.grimTicks--;
         }
      }
   }

   @Override
   protected void onEnable() {
      this.grimTicks = 0;
      this.flag = false;
      this.ccCooldown = 0;
      this.pendingVelocity = null;
   }

   @Override
   protected void onDisable() {
      this.pendingVelocity = null;
   }

   private void handleVelocityPacket(PacketEvent event, ClientboundSetEntityMotionPacket packet) {
      Vec3 velocity = packet.getMovement();
      if (this.mode.is("Matrix")) {
         if (!this.flag) {
            event.cancel();
            this.flag = true;
         } else {
            this.flag = false;
            event.cancel();
            this.pendingVelocity = new Vec3(velocity.x * -0.1, velocity.y, velocity.z * -0.1);
         }
      } else if (this.mode.is("Normal")) {
         event.cancel();
      } else if (this.mode.is("Old Grim")) {
         event.cancel();
         this.grimTicks = 6;
      } else if (this.mode.is("New Grim")) {
         event.cancel();
         this.flag = true;
      }
   }

   private void handleMatrixTick(Minecraft client) {
      if (this.pendingVelocity != null) {
         client.player.setDeltaMovement(this.pendingVelocity);
         this.pendingVelocity = null;
      }

      if (client.player.hurtTime > 0 && !client.player.onGround()) {
         double yaw = (double)(client.player.getYRot() * (float) (Math.PI / 180.0));
         double speed = Math.hypot(client.player.getDeltaMovement().x, client.player.getDeltaMovement().z);
         client.player.setDeltaMovement(-Math.sin(yaw) * speed, client.player.getDeltaMovement().y, Math.cos(yaw) * speed);
         client.player.setSprinting(client.player.tickCount % 2 != 0);
      }
   }

   private void handleNewGrimTick(Minecraft client) {
      if (this.ccCooldown <= 0) {
         client.player
            .connection
            .send(
               new PosRot(
                  client.player.getX(),
                  client.player.getY(),
                  client.player.getZ(),
                  client.player.getYRot(),
                  client.player.getXRot(),
                  client.player.onGround(),
                  false
               )
            );
         client.player
            .connection
            .send(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, BlockPos.containing(client.player.position()), Direction.DOWN));
      }

      this.flag = false;
   }
}
