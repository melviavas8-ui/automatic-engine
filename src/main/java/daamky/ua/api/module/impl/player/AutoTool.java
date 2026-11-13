package daamky.ua.api.module.impl.player;

import java.util.Comparator;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.BlockBreakingEvent;
import daamky.ua.api.events.impl.HotBarScrollEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.script.Script;
import daamky.ua.utils.inventory.script.ScriptAction;

public final class AutoTool extends Module {
   private static AutoTool instance;
   private static boolean mainHandReserved;
   private final StopWatch swap = new StopWatch();
   private final StopWatch breaking = new StopWatch();
   private final Script swapBackScript = new Script();
   private BlockPos lastBreakPos;
   private int previousSelectedSlot = -1;
   private Slot pendingInventorySwapSlot;

   public AutoTool() {
      super("Auto Tool", "Automatically selects the best tool for blocks.", ModuleCategory.PLAYER);
      instance = this;
   }

   public static void setMainHandReserved(boolean reserved) {
      mainHandReserved = reserved;
      if (reserved && instance != null) {
         instance.restoreMainHandNow();
      }
   }

   @Override
   protected void onEnable() {
      this.resetState();
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   private void resetState() {
      this.lastBreakPos = null;
      this.previousSelectedSlot = -1;
      this.pendingInventorySwapSlot = null;
      mainHandReserved = false;
      this.swap.reset();
      this.breaking.reset();
      this.swapBackScript.cleanup();
   }

   @SubscribeEvent
   private void onHotBarScroll(HotBarScrollEvent event) {
      if (!this.swapBackScript.isFinished()) {
         event.setCancelled(true);
      }
   }

   @SubscribeEvent
   private void onBlockBreaking(BlockBreakingEvent event) {
      final MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null) {
         if (!mainHandReserved) {
            this.breaking.reset();
            this.lastBreakPos = event.blockPos();
            if (!client.player.isCreative() && this.swapBackScript.isFinished() && this.swap.finished(350.0)) {
               Slot bestSlot = this.findBestTool(this.lastBreakPos);
               Slot mainHandSlot = InventoryTask.mainHandSlot();
               if (bestSlot != null && !this.sameSlot(bestSlot, mainHandSlot)) {
                  int hotbarSlot = this.getHotbarSlotFromSlot(bestSlot);
                  if (hotbarSlot != -1) {
                     if (this.previousSelectedSlot == -1) {
                        this.previousSelectedSlot = client.player.getInventory().getSelectedSlot();
                     }

                     InventoryTask.switchTo(hotbarSlot);
                     this.swapBackScript.cleanup().addTickStep(0, new AutoTool.ScriptActionAdapter() {
                        @Override
                        public void perform() {
                           if (AutoTool.this.previousSelectedSlot != -1 && client.player != null) {
                              InventoryTask.switchTo(AutoTool.this.previousSelectedSlot);
                              AutoTool.this.previousSelectedSlot = -1;
                           }
                        }
                     });
                  } else {
                     this.pendingInventorySwapSlot = bestSlot;
                     InventoryTask.swapHand(bestSlot, Hand.MAIN_HAND, false, true);
                     this.swapBackScript.cleanup().addTickStep(0, new AutoTool.ScriptActionAdapter() {
                        @Override
                        public void perform() {
                           if (AutoTool.this.pendingInventorySwapSlot != null) {
                              InventoryTask.swapHand(AutoTool.this.pendingInventorySwapSlot, Hand.MAIN_HAND, false, true);
                              AutoTool.this.pendingInventorySwapSlot = null;
                           }
                        }
                     });
                  }

                  this.swap.reset();
               }
            }
         }
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null) {
         if (mainHandReserved) {
            this.restoreMainHandNow();
         } else {
            if (!this.swapBackScript.isFinished() && this.swap.finished(350.0)) {
               Slot bestSlot = this.findBestTool(this.lastBreakPos);
               Slot mainHandSlot = InventoryTask.mainHandSlot();
               if (!this.sameSlot(bestSlot, mainHandSlot) || this.breaking.finished(100.0)) {
                  if (this.previousSelectedSlot != -1) {
                     InventoryTask.switchTo(this.previousSelectedSlot);
                     this.previousSelectedSlot = -1;
                  }

                  this.swapBackScript.update();
                  this.swap.reset();
               }
            }
         }
      }
   }

   private void restoreMainHandNow() {
      if (mc.player != null) {
         if (this.previousSelectedSlot != -1) {
            InventoryTask.switchTo(this.previousSelectedSlot);
            this.previousSelectedSlot = -1;
         }

         if (this.pendingInventorySwapSlot != null) {
            InventoryTask.swapHand(this.pendingInventorySwapSlot, Hand.MAIN_HAND, false, true);
            this.pendingInventorySwapSlot = null;
         }

         this.swapBackScript.cleanup();
         this.swap.reset();
      }
   }

   private Slot findBestTool(BlockPos blockPos) {
      MinecraftClient client = MinecraftClient.getInstance();
      Slot mainHandSlot = InventoryTask.mainHandSlot();
      if (client.player != null && client.world != null && blockPos != null) {
         BlockState state = client.world.getBlockState(blockPos);
         if (state.isAir()) {
            return mainHandSlot;
         } else {
            Slot bestHotbarTool = null;
            double bestHotbarSpeed = 0.0;

            for (int i = 0; i < 9; i++) {
               Slot hotbarSlot = client.player.playerScreenHandler.getSlot(36 + i);
               if (hotbarSlot != null && !hotbarSlot.getStack().isEmpty()) {
                  double speed = (double)hotbarSlot.getStack().getMiningSpeedMultiplier(state);
                  if (speed != 1.0 && speed > bestHotbarSpeed) {
                     bestHotbarSpeed = speed;
                     bestHotbarTool = hotbarSlot;
                  }
               }
            }

            return bestHotbarTool != null && bestHotbarSpeed > 1.0
               ? bestHotbarTool
               : client.player
                  .playerScreenHandler
                  .slots
                  .stream()
                  .filter(slot -> !slot.getStack().isEmpty())
                  .filter(slot -> slot.getStack().getMiningSpeedMultiplier(state) != 1.0F)
                  .max(Comparator.comparingDouble(slot -> (double)slot.getStack().getMiningSpeedMultiplier(state)))
                  .orElse(mainHandSlot);
         }
      } else {
         return mainHandSlot;
      }
   }

   private int getHotbarSlotFromSlot(Slot slot) {
      if (slot == null) {
         return -1;
      } else {
         int slotId = InventoryTask.getMenuSlotId(slot);
         return slotId >= 36 && slotId <= 44 ? slotId - 36 : -1;
      }
   }

   private boolean sameSlot(Slot first, Slot second) {
      return first != null && second != null ? InventoryTask.getMenuSlotId(first) == InventoryTask.getMenuSlotId(second) : first == second;
   }

   private abstract static class ScriptActionAdapter implements ScriptAction {
      private ScriptActionAdapter() {
      }
   }
}
