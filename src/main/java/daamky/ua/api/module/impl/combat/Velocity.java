package daamky.ua.api.module.impl.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;

public class Velocity extends Module {
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Knockback reduce mode.", "New Grim", "New Grim", "Old Grim", "Matrix", "Normal"));
   private boolean flag;
   private int grimTicks;
   private int ccCooldown;
   private Vec3d pendingVelocity;

   public Velocity() {
      super("Velocity", "Reduces or cancels knockback.", ModuleCategory.COMBAT);
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (event.isReceive() && client.player != null && !client.player.isTouchingWater() && !client.player.isSubmergedInWater() && !client.player.isInLava()) {
         if (this.ccCooldown > 0) {
            this.ccCooldown--;
         } else {
            if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == client.player.getId()) {
               this.handleVelocityPacket(event, packet);
            }

            if (this.mode.is("Old Grim") && event.getPacket() instanceof CommonPingS2CPacket && this.grimTicks > 0) {
               event.cancel();
               this.grimTicks--;
            }

            if (event.getPacket() instanceof PlayerPositionLookS2CPacket && this.mode.is("New Grim")) {
               this.ccCooldown = 5;
            }
         }
      }
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null && !client.player.isTouchingWater() && !client.player.isSubmergedInWater()) {
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

   private void handleVelocityPacket(PacketEvent event, EntityVelocityUpdateS2CPacket packet) {
      Vec3d velocity = packet.getVelocity();
      if (this.mode.is("Matrix")) {
         if (!this.flag) {
            event.cancel();
            this.flag = true;
         } else {
            this.flag = false;
            event.cancel();
            this.pendingVelocity = new Vec3d(velocity.x * -0.1, velocity.y, velocity.z * -0.1);
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

   private void handleMatrixTick(MinecraftClient client) {
      if (this.pendingVelocity != null) {
         client.player.setVelocity(this.pendingVelocity);
         this.pendingVelocity = null;
      }

      if (client.player.hurtTime > 0 && !client.player.isOnGround()) {
         double yaw = (double)(client.player.getYaw() * (float) (Math.PI / 180.0));
         double speed = Math.hypot(client.player.getVelocity().x, client.player.getVelocity().z);
         client.player.setVelocity(-Math.sin(yaw) * speed, client.player.getVelocity().y, Math.cos(yaw) * speed);
         client.player.setSprinting(client.player.age % 2 != 0);
      }
   }

   private void handleNewGrimTick(MinecraftClient client) {
      if (this.ccCooldown <= 0) {
         client.player
            .networkHandler
            .sendPacket(
               new Full(
                  client.player.getX(),
                  client.player.getY(),
                  client.player.getZ(),
                  client.player.getYaw(),
                  client.player.getPitch(),
                  client.player.isOnGround(),
                  false
               )
            );
         client.player
            .networkHandler
            .sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, BlockPos.ofFloored(client.player.getEntityPos()), Direction.DOWN));
      }

      this.flag = false;
   }
}
