package daamky.ua.api.module.impl.movement;

import daamky.gui.GuiStyle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.CloseScreenEvent;
import daamky.ua.api.events.impl.HotBarScrollEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.screens.clickgui.ClickGui;
import daamky.ua.utils.inventory.InventoryFlowManager;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;

public final class InventoryMove extends Module {
   private static final long DEFAULT_PHASE_DELAY_MS = 0L;
   private static final long FUN_TIME_PHASE_DELAY_MS = 0L;
   private static final long FUN_TIME_CLOSE_DELAY_MS = 0L;
   private static final long FUN_TIME_CLOSE_MAX_DELAY_MS = 0L;
   private static final long FUN_TIME_RESTORE_DELAY_MS = 0L;
   private static final long FUN_TIME_SOFT_CLICK_DELAY_MS = 1L;
   private static final double FUN_TIME_CLOSE_MAX_SPEED_SQ = 1.0E-4;
   private static final int FUN_TIME_PACKETS_PER_TICK = 440;
   private static final int FUN_TIME_CLOSE_PACKETS_PER_TICK = 440;
   private static InventoryMove instance;
   private final ModeSetting mode = new ModeSetting("Mode", "Inventory move bypass mode.", "Normal", "Normal", "FunTime");
   private final List<Packet<?>> packets = new ArrayList<>();
   private InventoryMove.MovePhase movePhase = InventoryMove.MovePhase.READY;
   private long actionStartTime = 0L;
   private boolean playerFullyStopped = false;
   private boolean wasForwardPressed;
   private boolean wasBackPressed;
   private boolean wasLeftPressed;
   private boolean wasRightPressed;
   private boolean wasJumpPressed;
   private boolean keysOverridden = false;
   private boolean inventoryOpened = false;
   private boolean packetsHeld = false;
   private boolean closingScreen = false;
   private boolean delayedClosePending = false;
   private boolean bypassCloseEvent = false;
   private boolean softClickSlowdown = false;
   private int activeContainerId = -1;
   private int heldContainerId = -1;

   public InventoryMove() {
      super("Inventory Move", "Allows movement while screens are open.", ModuleCategory.MOVEMENT);
      this.register(this.mode);
      instance = this;
   }

   public static InventoryMove getInstance() {
      return instance;
   }

   public String getSelectedMode() {
      return this.mode.getValue();
   }

   public boolean shouldPreserveInventoryMovementInput() {
      return this.isEnabled()
         && !this.isClickGuiOpen()
         && this.isInventoryScreenOpen()
         && InventoryFlowManager.isMovementAllowed()
         && !this.shouldFreezeCloseInput();
   }

   public boolean shouldSuppressSprintInput() {
      return this.softClickSlowdown && !this.delayedClosePending
         ? false
         : !this.isClickGuiOpen() && (this.closingScreen || this.movePhase != InventoryMove.MovePhase.READY && !this.shouldForceInventorySprint());
   }

   @Override
   protected void onDisable() {
      InventoryFlowManager.reset();
      this.resetState();
   }

   @Override
   public void onTick(MinecraftClient client) {
   }

   @SubscribeEvent
   private void onPreTick(TickEvent.Pre event) {
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null) {
         this.processLegitMovement();
      } else {
         this.resetState();
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isSend()
         && event.getPacket() instanceof CloseHandledScreenC2SPacket packet
         && this.isFunTimeMode()
         && this.matchesCurrentContainer(packet.getSyncId())
         && this.shouldDelayCloseScreen(mc.currentScreen)
         && !this.bypassCloseEvent) {
         event.setCancelled(true);
         this.startDelayedClose();
      } else if (event.isSend() && event.getPacket() instanceof ClickSlotC2SPacket packet && this.shouldHoldClickPacket(packet)) {
         this.packets.add(packet);
         event.setCancelled(true);
         this.packetsHeld = true;
         this.heldContainerId = packet.syncId();
         this.startSlowdownForInventoryClick(packet);
      } else {
         if (event.isReceive() && event.getPacket() instanceof CloseScreenS2CPacket packet) {
            if (this.shouldIgnoreServerClosePacket(packet)) {
               event.setCancelled(true);
            } else if (this.isFunTimeMode() && this.matchesTrackedContainer(packet.getSyncId())) {
               this.resetState();
            }
         }
      }
   }

   @SubscribeEvent
   private void onCloseScreen(CloseScreenEvent event) {
      if (!this.bypassCloseEvent) {
         this.closingScreen = true;
         this.stopPlayerSprintBeforeClose();
         if (this.isFunTimeMode() && this.delayedClosePending) {
            event.setCancelled(true);
         } else if (this.shouldDelayCloseScreen(event.getScreen())) {
            event.setCancelled(true);
            this.startDelayedClose();
         } else {
            if (this.packetsHeld && this.movePhase == InventoryMove.MovePhase.ALLOW_MOVEMENT) {
               if (this.isFunTimeMode()) {
                  event.setCancelled(true);
                  this.delayedClosePending = true;
               }

               this.movePhase = InventoryMove.MovePhase.SLOWING_DOWN;
               this.actionStartTime = System.currentTimeMillis();
            }
         }
      }
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      if (event != null && mc.player != null) {
         if (this.shouldFreezeCloseInput()) {
            event.inputNone();
         } else if (this.shouldForceSoftClickSprint()) {
            event.setSprinting(true);
         } else {
            if (this.shouldForceInventorySprint()) {
               event.setSprinting(true);
            } else if (this.shouldSuppressSprintInput()) {
               event.setSprinting(false);
            }
         }
      }
   }

   @SubscribeEvent
   private void onScroll(HotBarScrollEvent event) {
      if (MinecraftClient.getInstance().currentScreen != null && !(MinecraftClient.getInstance().currentScreen instanceof ChatScreen)) {
         event.setCancelled(true);
      }
   }

   private void processLegitMovement() {
      if (!this.isClickGuiOpen()) {
         boolean hasOpenScreen = mc.currentScreen != null;
         if (hasOpenScreen && this.inventoryOpened && this.containerChanged()) {
            this.resetState();
         }

         if (hasOpenScreen && !this.inventoryOpened && this.movePhase == InventoryMove.MovePhase.READY) {
            this.closingScreen = false;
            this.startLegitMovement();
            this.inventoryOpened = true;
         }

         if (!hasOpenScreen && this.inventoryOpened) {
            this.stopInventorySprint();
            if (this.packetsHeld && this.movePhase == InventoryMove.MovePhase.ALLOW_MOVEMENT) {
               this.movePhase = InventoryMove.MovePhase.SLOWING_DOWN;
               this.actionStartTime = System.currentTimeMillis();
            } else if (!this.packetsHeld) {
               this.resetState();
            }

            this.inventoryOpened = false;
            this.closingScreen = this.packetsHeld || this.delayedClosePending;
         } else {
            if (this.movePhase != InventoryMove.MovePhase.READY) {
               this.handleMovementStates();
            }
         }
      }
   }

   private void startLegitMovement() {
      this.captureMovementKeyStates();
      this.activeContainerId = this.currentContainerId();
      if (this.heldContainerId != -1 && this.heldContainerId != this.activeContainerId) {
         this.packets.clear();
         this.packetsHeld = false;
         this.heldContainerId = -1;
      }

      this.keysOverridden = false;
      this.packetsHeld = !this.packets.isEmpty();
      this.movePhase = this.packetsHeld ? InventoryMove.MovePhase.SLOWING_DOWN : InventoryMove.MovePhase.ALLOW_MOVEMENT;
      this.actionStartTime = System.currentTimeMillis();
   }

   private void handleMovementStates() {
      for (int phaseBudget = 0; phaseBudget < 6; phaseBudget++) {
         long elapsed = System.currentTimeMillis() - this.actionStartTime;
         boolean continueSameTick = false;
         switch (this.movePhase) {
            case SLOWING_DOWN:
               if (this.softClickSlowdown) {
                  this.keepSoftClickMovement();
               } else if (!this.keysOverridden) {
                  mc.options.forwardKey.setPressed(false);
                  mc.options.backKey.setPressed(false);
                  mc.options.leftKey.setPressed(false);
                  mc.options.rightKey.setPressed(false);
                  mc.options.jumpKey.setPressed(false);
                  this.keysOverridden = true;
               }

               if (!this.softClickSlowdown) {
                  this.stopInventorySprint();
               }

               long slowdownDelay = this.getSlowdownDelayMs();
               if (!this.hasDelayElapsed(elapsed, slowdownDelay)) {
                  return;
               }

               this.movePhase = InventoryMove.MovePhase.SEND_PACKETS;
               this.actionStartTime = System.currentTimeMillis();
               continueSameTick = this.isInstantDelay(slowdownDelay);
               break;
            case ALLOW_MOVEMENT:
               if (this.isInventoryScreenOpen()) {
                  InventoryFlowManager.updateMoveKeys();
                  this.forceInventorySprint();
               }
               break;
            case CLOSE_SCREEN:
               this.stopPlayerSprintBeforeClose();
               if (this.shouldWaitBeforeClose(elapsed)) {
                  return;
               }

               this.closeDelayedScreen();
               this.movePhase = InventoryMove.MovePhase.SPEEDING_UP;
               this.actionStartTime = System.currentTimeMillis();
               continueSameTick = this.isInstantDelay(this.getRestoreDelayMs());
               break;
            case SPEEDING_UP:
               long restoreDelay = this.getRestoreDelayMs();
               if (!this.hasDelayElapsed(elapsed, restoreDelay)) {
                  if (this.softClickSlowdown) {
                     this.keepSoftClickMovement();
                  } else if (this.isFunTimeMode()) {
                     this.stopPlayerSprintBeforeClose();
                  }

                  return;
               }

               if (this.keysOverridden) {
                  this.restoreKeyStates();
               }

               this.movePhase = InventoryMove.MovePhase.FINISHED;
               continueSameTick = true;
               break;
            case SEND_PACKETS:
               if (this.softClickSlowdown) {
                  this.keepSoftClickMovement();
               } else {
                  this.stopInventorySprint();
               }

               if (!this.packets.isEmpty()) {
                  if (this.isFunTimeMode()) {
                     if (!this.hasDelayElapsed(elapsed, 0L)) {
                        return;
                     }

                     int packetLimit = this.delayedClosePending ? 440 : 440;
                     int packetsToSend = Math.min(packetLimit, this.packets.size());

                     for (int i = 0; i < packetsToSend; i++) {
                        PlayerInteractionHelper.sendPacketWithOutEvent(this.packets.remove(0));
                     }

                     this.actionStartTime = System.currentTimeMillis();
                     if (!this.packets.isEmpty()) {
                        return;
                     }
                  }

                  if (!this.packets.isEmpty()) {
                     this.packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
                     this.packets.clear();
                  }
               }

               this.packetsHeld = false;
               if (this.delayedClosePending) {
                  this.movePhase = InventoryMove.MovePhase.CLOSE_SCREEN;
                  this.actionStartTime = System.currentTimeMillis();
                  continueSameTick = this.isInstantDelay(0L);
               } else {
                  this.movePhase = InventoryMove.MovePhase.SPEEDING_UP;
                  this.actionStartTime = System.currentTimeMillis();
                  continueSameTick = this.isInstantDelay(this.getRestoreDelayMs());
               }
               break;
            case FINISHED:
               this.resetState();
               return;
            default:
               return;
         }

         if (!continueSameTick) {
            return;
         }
      }
   }

   private void restoreKeyStates() {
      boolean currentForward = this.isKeyPressed(mc.options.forwardKey);
      boolean currentBack = this.isKeyPressed(mc.options.backKey);
      boolean currentLeft = this.isKeyPressed(mc.options.leftKey);
      boolean currentRight = this.isKeyPressed(mc.options.rightKey);
      boolean currentJump = this.isKeyPressed(mc.options.jumpKey);
      mc.options.forwardKey.setPressed(this.wasForwardPressed && currentForward);
      mc.options.backKey.setPressed(this.wasBackPressed && currentBack);
      mc.options.leftKey.setPressed(this.wasLeftPressed && currentLeft);
      mc.options.rightKey.setPressed(this.wasRightPressed && currentRight);
      mc.options.jumpKey.setPressed(this.wasJumpPressed && currentJump);
      this.keysOverridden = false;
   }

   private float lerp(float start, float end, float delta) {
      return start + (end - start) * delta;
   }

   private void resetState() {
      if (this.keysOverridden) {
         this.restoreKeyStates();
      }

      this.movePhase = InventoryMove.MovePhase.READY;
      this.playerFullyStopped = false;
      this.inventoryOpened = false;
      this.packetsHeld = false;
      this.closingScreen = false;
      this.delayedClosePending = false;
      this.bypassCloseEvent = false;
      this.softClickSlowdown = false;
      this.activeContainerId = -1;
      this.heldContainerId = -1;
      this.packets.clear();
      this.stopInventorySprint();
      this.restoreSprintKeyState();
   }

   private void stopInventorySprint() {
      if (mc.options != null) {
         mc.options.sprintKey.setPressed(false);
      }
   }

   private void forceInventorySprint() {
      if (!this.hasDirectionalMovementInput()) {
         this.stopPlayerSprintBeforeClose();
      } else {
         if (mc.options != null) {
            mc.options.sprintKey.setPressed(true);
         }

         if (mc.player != null && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
         }
      }
   }

   private void stopPlayerSprintBeforeClose() {
      this.stopInventorySprint();
      if (mc.player != null) {
         mc.player.setSprinting(false);
      }
   }

   private void keepSoftClickMovement() {
      if (this.isInventoryScreenOpen()) {
         InventoryFlowManager.updateMoveKeys();
         this.forceInventorySprint();
      }
   }

   private void restoreSprintKeyState() {
      if (mc.options != null) {
         mc.options.sprintKey.setPressed(this.isKeyPressed(mc.options.sprintKey));
      }
   }

   private boolean isInventoryScreenOpen() {
      return mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen);
   }

   private boolean isClickGuiOpen() {
      return mc.currentScreen instanceof ClickGui || GuiStyle.isClientGui(mc.currentScreen);
   }

   private boolean shouldForceInventorySprint() {
      return !this.closingScreen
         && InventoryFlowManager.isMovementAllowed()
         && this.isInventoryScreenOpen()
         && this.hasDirectionalMovementInput()
         && (this.movePhase == InventoryMove.MovePhase.READY || this.movePhase == InventoryMove.MovePhase.ALLOW_MOVEMENT);
   }

   private boolean shouldForceSoftClickSprint() {
      return this.softClickSlowdown
         && !this.delayedClosePending
         && InventoryFlowManager.isMovementAllowed()
         && this.isInventoryScreenOpen()
         && this.hasDirectionalMovementInput();
   }

   private boolean shouldFreezeCloseInput() {
      return this.softClickSlowdown && !this.delayedClosePending
         ? false
         : this.isFunTimeMode()
            && (
               this.delayedClosePending
                  || this.movePhase == InventoryMove.MovePhase.SLOWING_DOWN
                  || this.movePhase == InventoryMove.MovePhase.SEND_PACKETS
                  || this.movePhase == InventoryMove.MovePhase.CLOSE_SCREEN
                  || this.movePhase == InventoryMove.MovePhase.SPEEDING_UP
            );
   }

   private long getSlowdownDelayMs() {
      if (this.softClickSlowdown) {
         return 1L;
      } else {
         return this.isFunTimeMode() ? 0L : 0L;
      }
   }

   private long getRestoreDelayMs() {
      return this.isFunTimeMode() ? 0L : 0L;
   }

   private boolean hasDelayElapsed(long elapsed, long delayMs) {
      return this.isInstantDelay(delayMs) || elapsed >= delayMs;
   }

   private boolean isInstantDelay(long delayMs) {
      return delayMs <= 1L;
   }

   private boolean shouldWaitBeforeClose(long elapsed) {
      return !this.hasDelayElapsed(elapsed, 0L);
   }

   private boolean isFunTimeMode() {
      return "FunTime".equals(this.mode.getValue());
   }

   private boolean shouldDelayCloseScreen(Screen screen) {
      return this.isFunTimeMode()
         && screen != null
         && !(screen instanceof ChatScreen)
         && !(screen instanceof ClickGui) && !GuiStyle.isClientGui(screen)
         && !(screen instanceof GenericContainerScreen)
         && !(screen instanceof ShulkerBoxScreen)
         && !(screen instanceof AnvilScreen);
   }

   private boolean shouldIgnoreServerClosePacket(CloseScreenS2CPacket packet) {
      return this.isFunTimeMode()
         && packet != null
         && this.matchesTrackedContainer(packet.getSyncId())
         && (this.delayedClosePending || this.movePhase == InventoryMove.MovePhase.CLOSE_SCREEN);
   }

   private void startSlowdownForInventoryClick(ClickSlotC2SPacket packet) {
      if (this.isFunTimeMode() && this.movePhase == InventoryMove.MovePhase.ALLOW_MOVEMENT && this.hasDirectionalMovementInput()) {
         SlotActionType clickType = packet.actionType();
         if (clickType == SlotActionType.PICKUP
            || clickType == SlotActionType.QUICK_CRAFT
            || clickType == SlotActionType.PICKUP_ALL
            || clickType == SlotActionType.QUICK_MOVE
            || clickType == SlotActionType.THROW) {
            this.softClickSlowdown = true;
            this.keepSoftClickMovement();
            this.movePhase = InventoryMove.MovePhase.SLOWING_DOWN;
            this.actionStartTime = System.currentTimeMillis();
         }
      }
   }

   private boolean shouldHoldClickPacket(ClickSlotC2SPacket packet) {
      if (this.isFunTimeMode() && packet != null && InventoryFlowManager.shouldSkipExecution()) {
         int packetContainerId = packet.syncId();
         int currentContainerId = this.currentContainerId();
         if (currentContainerId != -1 && packetContainerId != currentContainerId) {
            this.resetState();
            return false;
         } else {
            if (this.heldContainerId != -1 && this.heldContainerId != packetContainerId) {
               this.packets.clear();
               this.packetsHeld = false;
            }

            if (!this.packetsHeld && !this.hasDirectionalMovementInput()) {
               return false;
            } else {
               this.activeContainerId = currentContainerId;
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private boolean containerChanged() {
      int currentContainerId = this.currentContainerId();
      return this.activeContainerId != -1 && currentContainerId != -1 && this.activeContainerId != currentContainerId;
   }

   private boolean matchesCurrentContainer(int containerId) {
      int currentContainerId = this.currentContainerId();
      return currentContainerId == -1 || containerId == currentContainerId;
   }

   private boolean matchesTrackedContainer(int containerId) {
      int currentContainerId = this.currentContainerId();
      return containerId == this.activeContainerId || containerId == this.heldContainerId || containerId == currentContainerId;
   }

   private int currentContainerId() {
      return mc.player != null && mc.player.currentScreenHandler != null ? mc.player.currentScreenHandler.syncId : -1;
   }

   private boolean hasCloseMovement() {
      if (mc.player == null) {
         return false;
      } else {
         double motionX = mc.player.getVelocity().x;
         double motionZ = mc.player.getVelocity().z;
         return motionX * motionX + motionZ * motionZ > 1.0E-4;
      }
   }

   private void startDelayedClose() {
      this.activeContainerId = this.currentContainerId();
      this.delayedClosePending = true;
      this.closingScreen = true;
      this.softClickSlowdown = false;
      this.stopPlayerSprintBeforeClose();
      if (this.movePhase == InventoryMove.MovePhase.READY) {
         this.captureMovementKeyStates();
         this.keysOverridden = false;
         this.inventoryOpened = true;
         this.movePhase = InventoryMove.MovePhase.ALLOW_MOVEMENT;
      }

      if (this.movePhase == InventoryMove.MovePhase.ALLOW_MOVEMENT) {
         this.movePhase = InventoryMove.MovePhase.SLOWING_DOWN;
         this.actionStartTime = System.currentTimeMillis();
      }
   }

   private void captureMovementKeyStates() {
      this.wasForwardPressed = this.isKeyPressed(mc.options.forwardKey);
      this.wasBackPressed = this.isKeyPressed(mc.options.backKey);
      this.wasLeftPressed = this.isKeyPressed(mc.options.leftKey);
      this.wasRightPressed = this.isKeyPressed(mc.options.rightKey);
      this.wasJumpPressed = this.isKeyPressed(mc.options.jumpKey);
   }

   private boolean hasDirectionalMovementInput() {
      return mc.options != null
         && (
            this.isKeyPressed(mc.options.forwardKey)
               || this.isKeyPressed(mc.options.backKey)
               || this.isKeyPressed(mc.options.leftKey)
               || this.isKeyPressed(mc.options.rightKey)
         );
   }

   private void closeDelayedScreen() {
      if (this.delayedClosePending && mc.player != null) {
         this.bypassCloseEvent = true;

         try {
            InventoryTask.closeScreen(false);
         } finally {
            this.bypassCloseEvent = false;
            this.delayedClosePending = false;
         }
      }
   }

   private boolean isKeyPressed(KeyBinding key) {
      if (mc.getWindow() == null) {
         return false;
      } else {
         Key inputKey = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
         long handle = mc.getWindow().getHandle();

         return switch (inputKey.getCategory()) {
            case KEYSYM -> InputUtil.isKeyPressed(mc.getWindow(), inputKey.getCode());
            case MOUSE -> GLFW.glfwGetMouseButton(handle, inputKey.getCode()) == 1;
            default -> false;
         };
      }
   }

   private static enum MovePhase {
      READY,
      SLOWING_DOWN,
      ALLOW_MOVEMENT,
      CLOSE_SCREEN,
      SPEEDING_UP,
      SEND_PACKETS,
      FINISHED;

      private MovePhase() {
      }
   }
}
