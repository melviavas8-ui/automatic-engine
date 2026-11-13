package daamky.ua.api.module.impl.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.PlayerTravelEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;

public class AirStuck extends Module {
   private static final double RELEASE_FALL_VELOCITY = -0.0784;
   private Vec3d stuckPosition;

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
   public void onTick(MinecraftClient client) {
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
         event.setMotion(Vec3d.ZERO);
         event.setCancelled(true);
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isSend() && event.getPacket() instanceof PlayerMoveC2SPacket) {
         event.setCancelled(true);
      }
   }

   private void freezePlayer() {
      if (mc.player != null && mc.world != null) {
         if (this.stuckPosition == null) {
            this.capturePosition();
         }

         mc.player.setVelocity(Vec3d.ZERO);
         mc.player.setPosition(this.stuckPosition.x, this.stuckPosition.y, this.stuckPosition.z);
         mc.player.setSprinting(false);
         mc.player.fallDistance = 0.0;
      } else {
         this.stuckPosition = null;
      }
   }

   private void capturePosition() {
      if (mc.player != null) {
         this.stuckPosition = mc.player.getEntityPos();
      }
   }

   private void releasePlayer() {
      if (mc.player != null && mc.world != null) {
         double fallVelocity = Math.min(mc.player.getVelocity().y, -0.0784);
         double releaseMoveY = this.getReleaseMoveY(fallVelocity);
         if (releaseMoveY != 0.0) {
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + releaseMoveY, mc.player.getZ());
         }

         mc.player.setVelocity(0.0, fallVelocity, 0.0);
         mc.player.setSprinting(false);
      }
   }

   private double getReleaseMoveY(double moveY) {
      Box movedBox = mc.player.getBoundingBox().offset(0.0, moveY, 0.0);
      return mc.world.isSpaceEmpty(mc.player, movedBox) ? moveY : 0.0;
   }
}
