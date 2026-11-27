package daamky.ua.api.module.impl.movement;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.PlayerTravelEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AirStuck extends Module {
   private static final double RELEASE_FALL_VELOCITY = -0.0784;
   private Vec3 stuckPosition;

   public AirStuck() {
      super("AirStuck", "Air Stuck", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.capturePosition();
      this.freezePlayer();
   }

   @Override
   protected void onDisable() {
      this.releasePlayer();
      this.stuckPosition = null;
   }

   @Override
   public void onTick(Minecraft client) {
      this.freezePlayer();
   }

   @SubscribeEvent
   private void onPreTick(TickEvent.Pre event) {
      this.freezePlayer();
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      event.inputNone();
   }

   @SubscribeEvent
   private void onTravel(PlayerTravelEvent event) {
      if (event.isPre()) {
         this.freezePlayer();
         event.setMotion(Vec3.ZERO);
         event.setCancelled(true);
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isSend() && event.getPacket() instanceof ServerboundMovePlayerPacket) {
         event.setCancelled(true);
      }
   }

   private void freezePlayer() {
      if (mc.player != null && mc.level != null) {
         if (this.stuckPosition == null) {
            this.capturePosition();
         }

         mc.player.setDeltaMovement(Vec3.ZERO);
         mc.player.setPos(this.stuckPosition.x, this.stuckPosition.y, this.stuckPosition.z);
         mc.player.setSprinting(false);
         mc.player.fallDistance = 0.0;
      } else {
         this.stuckPosition = null;
      }
   }

   private void capturePosition() {
      if (mc.player != null) {
         this.stuckPosition = mc.player.position();
      }
   }

   private void releasePlayer() {
      if (mc.player != null && mc.level != null) {
         double fallVelocity = Math.min(mc.player.getDeltaMovement().y, -0.0784);
         double releaseMoveY = this.getReleaseMoveY(fallVelocity);
         if (releaseMoveY != 0.0) {
            mc.player.setPos(mc.player.getX(), mc.player.getY() + releaseMoveY, mc.player.getZ());
         }

         mc.player.setDeltaMovement(0.0, fallVelocity, 0.0);
         mc.player.setSprinting(false);
      }
   }

   private double getReleaseMoveY(double moveY) {
      AABB movedBox = mc.player.getBoundingBox().move(0.0, moveY, 0.0);
      return mc.level.noCollision(mc.player, movedBox) ? moveY : 0.0;
   }
}
