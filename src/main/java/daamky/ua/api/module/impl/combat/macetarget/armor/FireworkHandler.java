package daamky.ua.api.module.impl.combat.macetarget.armor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.macetarget.state.MaceState;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.inventory.movement.MovementController;
import daamky.ua.utils.inventory.swap.SwapSettings;

public class FireworkHandler {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private final MovementController movement = new MovementController();
   private final FireworkHandler.SwapSettingsProvider settingsProvider;
   private MaceState.FireworkPhase phase = MaceState.FireworkPhase.IDLE;
   private int slot = -1;
   private int savedSlot = -1;
   private boolean fromInventory;
   private long phaseStartTime;
   private int currentDelay;

   public FireworkHandler(FireworkHandler.SwapSettingsProvider settingsProvider) {
      this.settingsProvider = settingsProvider;
   }

   public MovementController getMovement() {
      return this.movement;
   }

   public boolean isActive() {
      return this.phase != MaceState.FireworkPhase.IDLE;
   }

   public void useFirework(boolean silent) {
      if (silent) {
         this.useSilent();
      } else {
         this.startLegit();
      }
   }

   private void useSilent() {
      if (MC.player != null && MC.getNetworkHandler() != null) {
         int hotbarSlot = InventoryUtils.findHotbarItem(Items.FIREWORK_ROCKET);
         if (hotbarSlot != -1) {
            int currentSlot = MC.player.getInventory().getSelectedSlot();
            if (hotbarSlot != currentSlot) {
               MC.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
            }

            this.sendUse();
            if (hotbarSlot != currentSlot) {
               MC.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
            }
         } else {
            int invSlot = InventoryUtils.findItemInInventory(Items.FIREWORK_ROCKET);
            if (invSlot != -1) {
               int currentHotbarSlot = MC.player.getInventory().getSelectedSlot();
               int wrappedSlot = InventoryUtils.wrapSlot(invSlot);
               InventoryUtils.click(wrappedSlot, currentHotbarSlot, SlotActionType.SWAP);
               this.sendUse();
               InventoryUtils.click(wrappedSlot, currentHotbarSlot, SlotActionType.SWAP);
               InventoryUtils.closeScreen();
            }
         }
      }
   }

   private void startLegit() {
      if (MC.player != null) {
         this.savedSlot = MC.player.getInventory().getSelectedSlot();
         int hotbarSlot = InventoryUtils.findHotbarItem(Items.FIREWORK_ROCKET);
         if (hotbarSlot != -1) {
            this.slot = hotbarSlot;
            this.fromInventory = false;
            InventoryUtils.selectSlot(this.slot);
            this.startPhase(MaceState.FireworkPhase.AWAIT_ITEM, 0);
         } else {
            int invSlot = InventoryUtils.findItemInInventory(Items.FIREWORK_ROCKET);
            if (invSlot != -1) {
               this.slot = invSlot;
               this.fromInventory = true;
               SwapSettings settings = this.settingsProvider.get();
               this.startPhase(
                  settings.shouldStopMovement() ? MaceState.FireworkPhase.PRE_STOP : MaceState.FireworkPhase.SWAP_TO_HAND,
                  settings.shouldStopMovement() ? settings.randomPreStopDelay() : 0
               );
            }
         }
      }
   }

   public void processLoop() {
      int iterations = 0;

      while (this.phase != MaceState.FireworkPhase.IDLE && iterations++ < 10 && this.processTick()) {
      }
   }

   private boolean processTick() {
      if (MC.player == null) {
         this.reset();
         return false;
      } else {
         long elapsed = System.currentTimeMillis() - this.phaseStartTime;
         SwapSettings settings = this.settingsProvider.get();
         switch (this.phase) {
            case PRE_STOP:
               if (elapsed >= (long)this.currentDelay) {
                  this.movement.saveState();
                  this.movement.block();
                  if (settings.shouldStopSprint()) {
                     this.movement.stopSprint();
                  }

                  this.startPhase(MaceState.FireworkPhase.STOPPING, 0);
                  return true;
               }
               break;
            case STOPPING:
               this.movement.block();
               if (settings.shouldStopSprint()) {
                  this.movement.stopSprint();
               }

               this.startPhase(MaceState.FireworkPhase.WAIT_STOP, settings.randomWaitStopDelay());
               return this.currentDelay == 0;
            case WAIT_STOP:
               this.movement.block();
               if (!this.movement.isPlayerStopped(settings.getVelocityThreshold()) && elapsed < (long)this.currentDelay) {
                  break;
               }

               this.startPhase(MaceState.FireworkPhase.PRE_SWAP, settings.randomPreSwapDelay());
               return this.currentDelay == 0;
            case PRE_SWAP:
               this.movement.block();
               if (elapsed >= (long)this.currentDelay) {
                  this.startPhase(MaceState.FireworkPhase.SWAP_TO_HAND, 0);
                  return true;
               }
               break;
            case SWAP_TO_HAND:
               InventoryUtils.click(InventoryUtils.wrapSlot(this.slot), MC.player.getInventory().getSelectedSlot(), SlotActionType.SWAP);
               this.startPhase(MaceState.FireworkPhase.AWAIT_ITEM, 0);
               return true;
            case AWAIT_ITEM:
               if (MC.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
                  this.startPhase(MaceState.FireworkPhase.USE, 0);
                  return true;
               }
               break;
            case USE:
               this.sendUse();
               MC.player.swingHand(Hand.MAIN_HAND);
               this.startPhase(MaceState.FireworkPhase.POST_USE, settings.randomPostSwapDelay());
               return this.currentDelay == 0;
            case POST_USE:
               if (elapsed >= (long)this.currentDelay) {
                  if (this.fromInventory) {
                     this.startPhase(MaceState.FireworkPhase.SWAP_BACK, 0);
                  } else {
                     InventoryUtils.selectSlot(this.savedSlot);
                     this.startPhase(MaceState.FireworkPhase.RESUMING, settings.randomResumeDelay());
                  }

                  return true;
               }
               break;
            case SWAP_BACK:
               InventoryUtils.click(InventoryUtils.wrapSlot(this.slot), MC.player.getInventory().getSelectedSlot(), SlotActionType.SWAP);
               InventoryUtils.selectSlot(this.savedSlot);
               if (settings.shouldCloseInventory()) {
                  InventoryUtils.closeScreen();
               }

               this.startPhase(MaceState.FireworkPhase.RESUMING, settings.randomResumeDelay());
               return this.currentDelay == 0;
            case RESUMING:
               if (elapsed >= (long)this.currentDelay) {
                  this.movement.restoreFromCurrent();
                  this.reset();
               }
         }

         return false;
      }
   }

   private void sendUse() {
      Angle rotation = AngleConnection.INSTANCE.getRotation();
      if (rotation == null) {
         rotation = MathAngle.cameraAngle();
      }

      InventoryUtils.sendUsePacket(Hand.MAIN_HAND, rotation.getYaw(), rotation.getPitch());
   }

   private void startPhase(MaceState.FireworkPhase phase, int delay) {
      this.phase = phase;
      this.phaseStartTime = System.currentTimeMillis();
      this.currentDelay = delay;
   }

   public void reset() {
      this.movement.reset();
      this.phase = MaceState.FireworkPhase.IDLE;
      this.slot = -1;
      this.savedSlot = -1;
      this.fromInventory = false;
      this.phaseStartTime = 0L;
      this.currentDelay = 0;
   }

   public void forceRestore() {
      if (this.movement.isBlocked()) {
         this.movement.restoreFromCurrent();
      }
   }

   @FunctionalInterface
   public interface SwapSettingsProvider {
      SwapSettings get();
   }
}
