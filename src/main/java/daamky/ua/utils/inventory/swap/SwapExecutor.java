package daamky.ua.utils.inventory.swap;

import net.minecraft.client.MinecraftClient;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.inventory.movement.MovementController;

public class SwapExecutor {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private SwapExecutor.Phase phase = SwapExecutor.Phase.IDLE;
   private final MovementController movement = new MovementController();
   private SwapSettings settings = SwapSettings.defaults();
   private Runnable swapAction;
   private Runnable onComplete;
   private long phaseStartTime;
   private int currentDelay;

   public SwapExecutor() {
   }

   public void execute(Runnable swapAction, SwapSettings settings) {
      this.execute(swapAction, settings, null);
   }

   public void execute(Runnable swapAction, SwapSettings settings, Runnable onComplete) {
      if (this.phase == SwapExecutor.Phase.IDLE) {
         this.swapAction = swapAction;
         this.settings = settings != null ? settings : SwapSettings.defaults();
         this.onComplete = onComplete;
         if (this.settings.shouldStopMovement()) {
            this.movement.saveState();
            this.movement.block();
            if (this.settings.shouldStopSprint()) {
               this.movement.stopSprint();
            }

            this.startPhase(SwapExecutor.Phase.PRE_STOP, this.settings.randomPreStopDelay());
         } else {
            this.startPhase(SwapExecutor.Phase.SWAPPING, 0);
         }
      }
   }

   public void tick() {
      if (this.phase != SwapExecutor.Phase.IDLE && this.phase != SwapExecutor.Phase.FINISHED) {
         if (mc.player == null) {
            this.reset();
         } else {
            if (this.settings.shouldStopMovement() && this.phase != SwapExecutor.Phase.RESUMING && this.phase != SwapExecutor.Phase.FINISHED) {
               this.movement.block();
               if (this.settings.shouldStopSprint()) {
                  this.movement.stopSprint();
               }
            }

            boolean continueProcessing = true;
            int maxIterations = 10;

            for (int iterations = 0; continueProcessing && iterations < maxIterations; continueProcessing = this.processPhase()) {
               iterations++;
            }
         }
      }
   }

   private boolean processPhase() {
      long elapsed = System.currentTimeMillis() - this.phaseStartTime;
      switch (this.phase) {
         case PRE_STOP:
            this.movement.block();
            if (this.settings.shouldStopSprint()) {
               this.movement.stopSprint();
            }

            if (elapsed >= (long)this.currentDelay) {
               this.startPhase(SwapExecutor.Phase.STOPPING, 0);
               return true;
            }
            break;
         case STOPPING:
            this.movement.block();
            if (this.settings.shouldStopSprint()) {
               this.movement.stopSprint();
            }

            this.startPhase(SwapExecutor.Phase.WAIT_STOP, this.settings.randomWaitStopDelay());
            return this.currentDelay == 0;
         case WAIT_STOP:
            this.movement.block();
            if (this.settings.shouldStopSprint()) {
               this.movement.stopSprint();
            }

            boolean stopped = this.movement.isPlayerStopped(this.settings.getVelocityThreshold());
            if (stopped && elapsed >= (long)this.currentDelay) {
               this.startPhase(SwapExecutor.Phase.PRE_SWAP, this.settings.randomPreSwapDelay());
               return this.currentDelay == 0;
            }
            break;
         case PRE_SWAP:
            this.movement.block();
            if (this.settings.shouldStopSprint()) {
               this.movement.stopSprint();
            }

            if (elapsed >= (long)this.currentDelay) {
               this.startPhase(SwapExecutor.Phase.SWAPPING, 0);
               return true;
            }
            break;
         case SWAPPING:
            this.movement.block();
            if (this.settings.shouldStopSprint()) {
               this.movement.stopSprint();
            }

            if (this.swapAction != null) {
               this.swapAction.run();
            }

            this.startPhase(SwapExecutor.Phase.POST_SWAP, this.settings.randomPostSwapDelay());
            return this.currentDelay == 0;
         case POST_SWAP:
            this.movement.block();
            if (elapsed >= (long)this.currentDelay) {
               if (this.settings.shouldCloseInventory()) {
                  InventoryUtils.closeScreen();
               }

               this.startPhase(SwapExecutor.Phase.RESUMING, this.settings.randomResumeDelay());
               return this.currentDelay == 0;
            }
            break;
         case RESUMING:
            if (elapsed >= (long)this.currentDelay) {
               if (this.settings.shouldStopMovement()) {
                  this.movement.restoreFromCurrent();
               }

               this.phase = SwapExecutor.Phase.FINISHED;
               if (this.onComplete != null) {
                  this.onComplete.run();
               }

               this.reset();
               return false;
            }
      }

      return false;
   }

   private void startPhase(SwapExecutor.Phase newPhase, int delay) {
      this.phase = newPhase;
      this.phaseStartTime = System.currentTimeMillis();
      this.currentDelay = delay;
   }

   public void cancel() {
      if (this.movement.isBlocked()) {
         this.movement.restoreFromCurrent();
      }

      this.reset();
   }

   public void reset() {
      this.phase = SwapExecutor.Phase.IDLE;
      this.swapAction = null;
      this.onComplete = null;
      this.movement.reset();
   }

   public boolean isRunning() {
      return this.phase != SwapExecutor.Phase.IDLE && this.phase != SwapExecutor.Phase.FINISHED;
   }

   public boolean isBlocking() {
      return this.movement.isBlocked() || this.isRunning() && this.settings.shouldStopMovement();
   }

   public SwapExecutor.Phase getPhase() {
      return this.phase;
   }

   public static enum Phase {
      IDLE,
      PRE_STOP,
      STOPPING,
      WAIT_STOP,
      PRE_SWAP,
      SWAPPING,
      POST_SWAP,
      RESUMING,
      FINISHED;

      private Phase() {
      }
   }
}
