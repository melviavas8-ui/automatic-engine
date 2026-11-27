package daamky.ua.api.module.impl.movement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConfig;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.util.TaskPriority;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.chat.ChatMessage;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.modules.warden.rotation.FreeLookController;
import daamky.ua.utils.modules.warden.rotation.Rotation;
import daamky.ua.utils.modules.warden.rotation.RotationController;
import daamky.ua.utils.move.MoveUtil;

public final class Fly extends Module {
   private static final long ELYTRA_START_DELAY_MS = 110L;
   private static final long PACKET_FLUSH_DELAY_MS = 1025L;
   private static final long GLIDE_NUDGE_DELAY_MS = 12L;
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Flight mode.", "Vanilla", "Vanilla", "Dragon", "ElytraSpeed"));
   private final NumberSetting horizontal = this.register(new NumberSetting("Horizontal", "Horizontal speed.", 1.0, 0.1, 5.0, 0.1));
   private final NumberSetting vertical = this.register(new NumberSetting("Vertical", "Vertical speed.", 0.6, 0.1, 3.0, 0.1));
   private final List<Packet<?>> bufferedPackets = new CopyOnWriteArrayList<>();
   private long speedRampStartTime;
   private long lastElytraStart;
   private long lastPacketFlush;
   private long lastGlideNudge;
   private boolean isSpeedRamping;
   private boolean flushingPackets;
   private boolean missingElytraWarned;
   private boolean elytraSpeedRotationActive;

   public Fly() {
      super("Fly", "Flight movement module.", ModuleCategory.MOVEMENT);
   }

   public boolean isElytraSpeedMode() {
      return this.mode.is("ElytraSpeed");
   }

   @Override
   protected void onEnable() {
      long now = System.currentTimeMillis();
      this.lastElytraStart = now;
      this.lastPacketFlush = now;
      this.lastGlideNudge = now;
      this.isSpeedRamping = false;
      this.missingElytraWarned = false;
      this.elytraSpeedRotationActive = false;
      this.bufferedPackets.clear();
   }

   @Override
   protected void onDisable() {
      if (this.elytraSpeedRotationActive) {
         RotationController.hardReset();
         this.elytraSpeedRotationActive = false;
      }

      this.flushPackets();
      this.isSpeedRamping = false;
      this.missingElytraWarned = false;
      if (mc.player != null) {
         Vec3 movement = mc.player.getDeltaMovement();
         mc.player.setDeltaMovement(movement.x, 0.0, movement.z);
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (this.mode.is("ElytraUP") && !this.flushingPackets && event.isSend() && mc.player != null && mc.level != null) {
         this.bufferedPackets.add(event.getPacket());
         event.cancel();
      }
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.level != null && client.options != null) {
         if (!this.mode.is("ElytraSpeed") && this.elytraSpeedRotationActive) {
            RotationController.hardReset();
            this.elytraSpeedRotationActive = false;
         }

         if (this.mode.is("Glide")) {
            this.handleGlide(client);
         } else if (this.mode.is("Глайд")) {
            this.handleDirectionalGlide(client);
         } else if (this.mode.is("ElytraUP")) {
            this.handleElytraUp(client);
         } else if (this.mode.is("Hollyworld")) {
            this.handleHollyworld(client);
         } else if (this.mode.is("ElytraSpeed")) {
            this.handleElytraSpeed(client);
         } else {
            this.handleVanilla(client);
         }
      }
   }

   private void handleVanilla(Minecraft client) {
      if (!this.mode.is("Dragon") || client.player.getAbilities().mayfly && client.player.getAbilities().flying) {
         double[] direction = MoveUtil.calculateDirection(this.horizontal.getValue());
         double verticalMotion = client.options.keyJump.isDown()
            ? this.vertical.getValue()
            : (client.options.keyShift.isDown() ? -this.vertical.getValue() : 0.0);
         if (!MoveUtil.hasPlayerMovement()) {
            direction[0] = 0.0;
            direction[1] = 0.0;
         }

         client.player.setDeltaMovement(direction[0], verticalMotion, direction[1]);
      }
   }

   private void handleGlide(Minecraft client) {
      if (client.player.onGround()) {
         client.player.jumpFromGround();
      } else {
         double y = -0.01;
         if (client.options.keyShift.isDown()) {
            y -= this.vertical.getValue();
         } else if (client.options.keyJump.isDown()) {
            y += this.vertical.getValue();
         }

         client.player.setDeltaMovement(0.0, y, 0.0);
         MoveUtil.setVelocity(this.horizontal.getValue());
      }
   }

   private void handleDirectionalGlide(Minecraft client) {
      if (client.player.isFallFlying()) {
         double forward = 0.085;
         double yawRadians = Math.toRadians((double)client.player.getYRot());
         double dx = -Math.sin(yawRadians) * forward;
         double dz = Math.cos(yawRadians) * forward;
         Vec3 movement = client.player.getDeltaMovement();
         client.player.setDeltaMovement(dx * 1.25, movement.y + 0.015, dz * 1.25);
         long now = System.currentTimeMillis();
         if (now - this.lastGlideNudge >= 12L) {
            client.player.setPos(client.player.getX() + dx, client.player.getY(), client.player.getZ() + dz);
            this.lastGlideNudge = now;
         }
      }
   }

   private void handleElytraUp(Minecraft client) {
      if (!client.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
         if (!this.missingElytraWarned) {
            ChatMessage.brandmessage("Элитра не надета!");
            this.missingElytraWarned = true;
         }

         this.toggle();
      } else {
         this.missingElytraWarned = false;
         this.startElytraFlight(client);
         if (client.player.isFallFlying()) {
            Vec3 movement = client.player.getDeltaMovement();
            client.player.setDeltaMovement(0.0, movement.y + 0.019, 0.0);
            this.holdHorizontalRotation(client);
         }

         long now = System.currentTimeMillis();
         if (now - this.lastPacketFlush >= 1025L) {
            this.flushPackets();
            this.lastPacketFlush = now;
         }
      }
   }

   private void handleHollyworld(Minecraft client) {
      int elytraSlot = InventoryUtils.findItemInHotbar(Items.ELYTRA);
      if (elytraSlot < 0) {
         if (!this.missingElytraWarned) {
            ChatMessage.brandmessage("Положите элитру в хотбар.");
            this.missingElytraWarned = true;
         }
      } else {
         this.missingElytraWarned = false;
         if (client.player.onGround()) {
            client.player.jumpFromGround();
         } else if (!client.player.isInWater()) {
            boolean equipped = client.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
            if (!equipped) {
               this.swapChestWithHotbar(client, elytraSlot);
            }

            this.startElytraFlight(client);
            if (client.player.isFallFlying()) {
               Vec3 movement = client.player.getDeltaMovement();
               client.player.setDeltaMovement(0.0, movement.y + 0.049, 0.0);
               this.holdHorizontalRotation(client);
            }

            if (!equipped) {
               this.swapChestWithHotbar(client, elytraSlot);
            }
         }
      }
   }

   private void startElytraFlight(Minecraft client) {
      if (client.player.onGround()) {
         client.player.jumpFromGround();
      } else if (!client.player.isInWater() && !client.player.isFallFlying()) {
         long now = System.currentTimeMillis();
         if (now - this.lastElytraStart >= 110L) {
            client.player.connection.send(new ServerboundPlayerCommandPacket(client.player, Action.START_FALL_FLYING));
            this.lastElytraStart = now;
         }
      }
   }

   private void swapChestWithHotbar(Minecraft client, int hotbarSlot) {
      if (client.gameMode != null) {
         client.gameMode.handleInventoryMouseClick(client.player.inventoryMenu.containerId, 6, hotbarSlot, ClickType.SWAP, client.player);
      }
   }

   private void holdHorizontalRotation(Minecraft client) {
      AngleConnection.INSTANCE.rotateTo(new Angle(client.player.getYRot(), 0.0F), 1, AngleConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
   }

   private void flushPackets() {
      if (!this.bufferedPackets.isEmpty() && mc.player != null && mc.player.connection != null) {
         this.flushingPackets = true;

         try {
            for (Packet<?> packet : this.bufferedPackets) {
               mc.player.connection.send(packet);
            }

            this.bufferedPackets.clear();
         } finally {
            this.flushingPackets = false;
         }
      } else {
         this.bufferedPackets.clear();
      }
   }

   private void handleElytraSpeed(Minecraft client) {
      if (client.player != null && client.level != null && client.player.connection != null) {
         int elytra = InventoryUtils.findItemInHotbar(Items.ELYTRA);
         int previous = mc.player.getInventory().getSelectedSlot();
         InventoryUtils.syncSelectedHotbarSlot(elytra);
         PlayerInteractionHelper.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), 70.0F));
         PlayerInteractionHelper.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), 70.0F));
         mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
         if (!FreeLookController.isActive()) {
            FreeLookController.syncFromPlayer();
         }

         RotationController.update(new Rotation(FreeLookController.getFreeYaw(), 70.0F), 360.0F, 360.0F, 360.0F, 360.0F, 0, 1000, false);
         this.elytraSpeedRotationActive = true;
         InventoryUtils.syncSelectedHotbarSlot(previous);
      }
   }
}
