package daamky.ua.api.module.impl.movement;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.utils.chat.ChatMessage;
import daamky.ua.utils.inventory.lookup.InventoryUtils;

public final class ElytraFly extends Module {
   private static final long FIREWORK_DELAY_MS = 500L;
   private final StopWatch fireworkTimer = new StopWatch();
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Elytra flight mode.", "Normal", "Normal", "Grim-2.3.69", "Grim-2.3.71"));
   private int packetCounter;
   private boolean forcedJump;
   private boolean fireworkArmed;

   public ElytraFly() {
      super("ElytraFly", "Automatically starts elytra flight and uses fireworks.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.packetCounter = 0;
      this.forcedJump = false;
      this.fireworkArmed = false;
      this.fireworkTimer.reset();
   }

   @Override
   protected void onDisable() {
      this.releaseJump();
      this.packetCounter = 0;
      this.fireworkArmed = false;
   }

   @SubscribeEvent
   private void onUpdate(TickEvent.Pre event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null && mc.options != null) {
         int fireworkHotbarSlot = InventoryUtils.findItemInHotbar(Items.FIREWORK_ROCKET);
         int fireworkInventorySlot = InventoryUtils.findItemInInventory(Items.FIREWORK_ROCKET);
         if (fireworkHotbarSlot != -1 || fireworkInventorySlot != -1) {
            int elytraSlot = InventoryUtils.findItemInHotbar(Items.ELYTRA);
            if (elytraSlot == -1) {
               ChatMessage.brandmessage("Возьмите элитру в хотбар.");
               this.toggle();
            } else {
               this.handleAutoJump();
               this.handleTakeoff(elytraSlot);
               this.useArmedFirework();
               if (this.mode.is("Grim-2.3.69")) {
                  this.handleGrim269();
               } else if (this.mode.is("Grim-2.3.71")) {
                  this.handleGrim271();
               }
            }
         }
      } else {
         this.releaseJump();
      }
   }

   private void handleAutoJump() {
      if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
         mc.options.jumpKey.setPressed(true);
         this.forcedJump = true;
      } else if (!mc.player.isOnGround()) {
         this.releaseJump();
      }
   }

   private void handleTakeoff(int elytraSlot) {
      if (!mc.player.isTouchingWater() && !mc.player.isOnGround() && !mc.player.isGliding()) {
         boolean elytraEquipped = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
         if (!elytraEquipped) {
            this.swapChestWithHotbar(elytraSlot);
         }

         mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
         this.fireworkArmed = true;
         if (!elytraEquipped) {
            this.swapChestWithHotbar(elytraSlot);
         }
      }
   }

   private void useArmedFirework() {
      if (this.fireworkArmed && this.fireworkTimer.finished(500.0)) {
         InventoryUtils.silentUseItem(Items.FIREWORK_ROCKET);
         this.fireworkTimer.reset();
         this.fireworkArmed = false;
      }
   }

   private void swapChestWithHotbar(int hotbarSlot) {
      mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 6, hotbarSlot, SlotActionType.SWAP, mc.player);
   }

   private void handleGrim269() {
      if (mc.player.isGliding()) {
         if (this.packetCounter % 3 == 0) {
            this.movePlayerRandomly(0.02);
         }

         if (this.packetCounter % 5 == 0) {
            this.sendRandomPositionPackets(2, 0.03, 0.02);
         }

         if (ThreadLocalRandom.current().nextFloat() < 0.2F) {
            this.randomizeVelocity(0.95, 0.1);
         }
      }

      this.packetCounter++;
   }

   private void handleGrim271() {
      if (mc.player.isGliding()) {
         if (this.packetCounter % 2 == 0) {
            this.movePlayerRandomly(0.03);
         }

         if (this.packetCounter % 4 == 0) {
            this.sendRandomPositionPackets(3, 0.04, 0.03);
         }

         if (ThreadLocalRandom.current().nextFloat() < 0.3F) {
            this.randomizeVelocity(0.9, 0.2);
         }
      }

      this.packetCounter++;
   }

   private void movePlayerRandomly(double horizontalRange) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      mc.player
         .setPosition(
            mc.player.getX() + (random.nextDouble() - 0.5) * horizontalRange,
            mc.player.getY(),
            mc.player.getZ() + (random.nextDouble() - 0.5) * horizontalRange
         );
   }

   private void sendRandomPositionPackets(int count, double horizontalRange, double verticalRange) {
      ThreadLocalRandom random = ThreadLocalRandom.current();

      for (int i = 0; i < count; i++) {
         mc.player
            .networkHandler
            .sendPacket(
               new PositionAndOnGround(
                  mc.player.getX() + (random.nextDouble() - 0.5) * horizontalRange,
                  mc.player.getY() + (random.nextDouble() - 0.5) * verticalRange,
                  mc.player.getZ() + (random.nextDouble() - 0.5) * horizontalRange,
                  false,
                  false
               )
            );
      }
   }

   private void randomizeVelocity(double baseMultiplier, double randomRange) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      Vec3d velocity = mc.player.getVelocity();
      mc.player
         .setVelocity(
            velocity.x * (baseMultiplier + random.nextDouble() * randomRange),
            velocity.y * (baseMultiplier + random.nextDouble() * randomRange),
            velocity.z * (baseMultiplier + random.nextDouble() * randomRange)
         );
   }

   private void releaseJump() {
      if (this.forcedJump && mc.options != null) {
         mc.options.jumpKey.setPressed(false);
      }

      this.forcedJump = false;
   }
}
