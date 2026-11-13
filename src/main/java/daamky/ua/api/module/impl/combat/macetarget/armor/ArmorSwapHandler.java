package daamky.ua.api.module.impl.combat.macetarget.armor;

import net.minecraft.client.MinecraftClient;
import daamky.ua.api.module.impl.combat.macetarget.state.MaceState;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.inventory.movement.MovementController;
import daamky.ua.utils.inventory.swap.SwapSettings;

public class ArmorSwapHandler {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private final MovementController movement = new MovementController();
   private final ArmorSwapHandler.SwapSettingsProvider settingsProvider;
   private MaceState.SwapPhase phase = MaceState.SwapPhase.IDLE;
   private int slot = -1;
   private long phaseStartTime;
   private int currentDelay;

   public ArmorSwapHandler(ArmorSwapHandler.SwapSettingsProvider settingsProvider) {
      this.settingsProvider = settingsProvider;
   }

   public MovementController getMovement() {
      return this.movement;
   }

   public boolean isActive() {
      return this.phase != MaceState.SwapPhase.IDLE;
   }

   public void startSwap(int slot, boolean silent) {
      if (silent) {
         int wrappedSlot = InventoryUtils.wrapSlot(slot);
         InventoryUtils.swap(wrappedSlot, 6);
         InventoryUtils.closeScreen();
      } else {
         this.slot = slot;
         SwapSettings settings = this.settingsProvider.get();
         this.startPhase(
            settings.shouldStopMovement() ? MaceState.SwapPhase.PRE_STOP : MaceState.SwapPhase.DO_SWAP,
            settings.shouldStopMovement() ? settings.randomPreStopDelay() : 0
         );
      }
   }

   public void processLoop() {
      int iterations = 0;

      while (this.phase != MaceState.SwapPhase.IDLE && iterations++ < 10 && this.processTick()) {
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

                  this.startPhase(MaceState.SwapPhase.STOPPING, 0);
                  return true;
               }
               break;
            case STOPPING:
               this.movement.block();
               if (settings.shouldStopSprint()) {
                  this.movement.stopSprint();
               }

               this.startPhase(MaceState.SwapPhase.WAIT_STOP, settings.randomWaitStopDelay());
               return this.currentDelay == 0;
            case WAIT_STOP:
               this.movement.block();
               if (!this.movement.isPlayerStopped(settings.getVelocityThreshold()) && elapsed < (long)this.currentDelay) {
                  break;
               }

               this.startPhase(MaceState.SwapPhase.PRE_SWAP, settings.randomPreSwapDelay());
               return this.currentDelay == 0;
            case PRE_SWAP:
               this.movement.block();
               if (elapsed >= (long)this.currentDelay) {
                  this.startPhase(MaceState.SwapPhase.DO_SWAP, 0);
                  return true;
               }
               break;
            case DO_SWAP:
               InventoryUtils.swap(InventoryUtils.wrapSlot(this.slot), 6);
               this.startPhase(MaceState.SwapPhase.POST_SWAP, settings.randomPostSwapDelay());
               return this.currentDelay == 0;
            case POST_SWAP:
               if (elapsed >= (long)this.currentDelay) {
                  if (settings.shouldCloseInventory()) {
                     InventoryUtils.closeScreen();
                  }

                  this.startPhase(MaceState.SwapPhase.RESUMING, settings.randomResumeDelay());
                  return this.currentDelay == 0;
               }
               break;
            case RESUMING:
               if (elapsed >= (long)this.currentDelay) {
                  this.movement.restoreFromCurrent();
                  this.reset();
               }
         }

         return false;
      }
   }

   private void startPhase(MaceState.SwapPhase phase, int delay) {
      this.phase = phase;
      this.phaseStartTime = System.currentTimeMillis();
      this.currentDelay = delay;
   }

   public void reset() {
      this.movement.reset();
      this.phase = MaceState.SwapPhase.IDLE;
      this.slot = -1;
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
