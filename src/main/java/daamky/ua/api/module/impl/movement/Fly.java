package daamky.ua.api.module.impl.movement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
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
         Vec3d movement = mc.player.getVelocity();
         mc.player.setVelocity(movement.x, 0.0, movement.z);
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (this.mode.is("ElytraUP") && !this.flushingPackets && event.isSend() && mc.player != null && mc.world != null) {
         this.bufferedPackets.add(event.getPacket());
         event.cancel();
      }
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null && client.options != null) {
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

   private void handleVanilla(MinecraftClient client) {
      if (!this.mode.is("Dragon") || client.player.getAbilities().allowFlying && client.player.getAbilities().flying) {
         double[] direction = MoveUtil.calculateDirection(this.horizontal.getValue());
         double verticalMotion = client.options.jumpKey.isPressed()
            ? this.vertical.getValue()
            : (client.options.sneakKey.isPressed() ? -this.vertical.getValue() : 0.0);
         if (!MoveUtil.hasPlayerMovement()) {
            direction[0] = 0.0;
            direction[1] = 0.0;
         }

         client.player.setVelocity(direction[0], verticalMotion, direction[1]);
      }
   }

   private void handleGlide(MinecraftClient client) {
      if (client.player.isOnGround()) {
         client.player.jump();
      } else {
         double y = -0.01;
         if (client.options.sneakKey.isPressed()) {
            y -= this.vertical.getValue();
         } else if (client.options.jumpKey.isPressed()) {
            y += this.vertical.getValue();
         }

         client.player.setVelocity(0.0, y, 0.0);
         MoveUtil.setVelocity(this.horizontal.getValue());
      }
   }

   private void handleDirectionalGlide(MinecraftClient client) {
      if (client.player.isGliding()) {
         double forward = 0.085;
         double yawRadians = Math.toRadians((double)client.player.getYaw());
         double dx = -Math.sin(yawRadians) * forward;
         double dz = Math.cos(yawRadians) * forward;
         Vec3d movement = client.player.getVelocity();
         client.player.setVelocity(dx * 1.25, movement.y + 0.015, dz * 1.25);
         long now = System.currentTimeMillis();
         if (now - this.lastGlideNudge >= 12L) {
            client.player.setPosition(client.player.getX() + dx, client.player.getY(), client.player.getZ() + dz);
            this.lastGlideNudge = now;
         }
      }
   }

   private void handleElytraUp(MinecraftClient client) {
      if (!client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
         if (!this.missingElytraWarned) {
            ChatMessage.brandmessage("Элитра не надета!");
            this.missingElytraWarned = true;
         }

         this.toggle();
      } else {
         this.missingElytraWarned = false;
         this.startElytraFlight(client);
         if (client.player.isGliding()) {
            Vec3d movement = client.player.getVelocity();
            client.player.setVelocity(0.0, movement.y + 0.019, 0.0);
            this.holdHorizontalRotation(client);
         }

         long now = System.currentTimeMillis();
         if (now - this.lastPacketFlush >= 1025L) {
            this.flushPackets();
            this.lastPacketFlush = now;
         }
      }
   }

   private void handleHollyworld(MinecraftClient client) {
      int elytraSlot = InventoryUtils.findItemInHotbar(Items.ELYTRA);
      if (elytraSlot < 0) {
         if (!this.missingElytraWarned) {
            ChatMessage.brandmessage("Положите элитру в хотбар.");
            this.missingElytraWarned = true;
         }
      } else {
         this.missingElytraWarned = false;
         if (client.player.isOnGround()) {
            client.player.jump();
         } else if (!client.player.isTouchingWater()) {
            boolean equipped = client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
            if (!equipped) {
               this.swapChestWithHotbar(client, elytraSlot);
            }

            this.startElytraFlight(client);
            if (client.player.isGliding()) {
               Vec3d movement = client.player.getVelocity();
               client.player.setVelocity(0.0, movement.y + 0.049, 0.0);
               this.holdHorizontalRotation(client);
            }

            if (!equipped) {
               this.swapChestWithHotbar(client, elytraSlot);
            }
         }
      }
   }

   private void startElytraFlight(MinecraftClient client) {
      if (client.player.isOnGround()) {
         client.player.jump();
      } else if (!client.player.isTouchingWater() && !client.player.isGliding()) {
         long now = System.currentTimeMillis();
         if (now - this.lastElytraStart >= 110L) {
            client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(client.player, Mode.START_FALL_FLYING));
            this.lastElytraStart = now;
         }
      }
   }

   private void swapChestWithHotbar(MinecraftClient client, int hotbarSlot) {
      if (client.interactionManager != null) {
         client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, 6, hotbarSlot, SlotActionType.SWAP, client.player);
      }
   }

   private void holdHorizontalRotation(MinecraftClient client) {
      AngleConnection.INSTANCE.rotateTo(new Angle(client.player.getYaw(), 0.0F), 1, AngleConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
   }

   private void flushPackets() {
      if (!this.bufferedPackets.isEmpty() && mc.player != null && mc.player.networkHandler != null) {
         this.flushingPackets = true;

         try {
            for (Packet<?> packet : this.bufferedPackets) {
               mc.player.networkHandler.sendPacket(packet);
            }

            this.bufferedPackets.clear();
         } finally {
            this.flushingPackets = false;
         }
      } else {
         this.bufferedPackets.clear();
      }
   }

   private void handleElytraSpeed(MinecraftClient client) {
      if (client.player != null && client.world != null && client.player.networkHandler != null) {
         int elytra = InventoryUtils.findItemInHotbar(Items.ELYTRA);
         int previous = mc.player.getInventory().getSelectedSlot();
         InventoryUtils.syncSelectedHotbarSlot(elytra);
         PlayerInteractionHelper.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), 70.0F));
         PlayerInteractionHelper.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), 70.0F));
         mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
         if (!FreeLookController.isActive()) {
            FreeLookController.syncFromPlayer();
         }

         RotationController.update(new Rotation(FreeLookController.getFreeYaw(), 70.0F), 360.0F, 360.0F, 360.0F, 360.0F, 0, 1000, false);
         this.elytraSpeedRotationActive = true;
         InventoryUtils.syncSelectedHotbarSlot(previous);
      }
   }
}
