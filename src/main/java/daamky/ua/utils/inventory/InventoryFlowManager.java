package daamky.ua.utils.inventory;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.Window;
import daamky.ua.IMinecraft;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.module.impl.movement.InventoryMove;
import daamky.ua.utils.inventory.script.Script;
import daamky.ua.utils.inventory.script.ScriptAction;

public final class InventoryFlowManager implements IMinecraft {
   public static final Script script = new Script();
   public static final Script postScript = new Script();
   private static final Queue<Runnable> TASK_QUEUE = new ConcurrentLinkedQueue<>();
   private static final int CLOSE_SPRINT_DELAY_TICKS = 0;
   private static final int CLOSE_PACKET_DELAY_TICKS = 0;
   private static final int CLOSE_RESTORE_TICKS = 0;
   private static boolean canMove = true;

   private InventoryFlowManager() {
   }

   public static void tick() {
      script.update();
      postScript.update();
      InventoryTask.updateSwapAndUseScript();
      if (script.isFinished() && postScript.isFinished() && !TASK_QUEUE.isEmpty()) {
         Runnable nextTask = TASK_QUEUE.poll();
         if (nextTask != null) {
            addTask(nextTask);
         }
      }
   }

   public static void update() {
      tick();
   }

   public static void postMotion() {
      postScript.update();
   }

   public static void input(InputEvent event) {
      if (event != null) {
         InventoryMove inventoryMove = InventoryMove.getInstance();
         if (inventoryMove != null && inventoryMove.isEnabled() && inventoryMove.shouldSuppressSprintInput()) {
            event.setSprinting(false);
         }

         if (!canMove) {
            event.setDirectionalLow(false, false, false, false);
            event.setJumping(false);
            event.setSprinting(false);
         }
      }
   }

   public static void addTask(Runnable task) {
      if (task != null) {
         if (script.isFinished() && postScript.isFinished()) {
            String var1 = getSelectedMode();
            switch (var1) {
               case "FunTime":
                  script.cleanup().addStep(0, new InventoryFlowManager.ScriptActionAdapter() {
                     @Override
                     public void perform() {
                        InventoryFlowManager.disableMoveKeys();
                     }
                  }).addStep(1, new InventoryFlowManager.ScriptRunnableAction(task)).addStep(0, new InventoryFlowManager.ScriptActionAdapter() {
                     @Override
                     public void perform() {
                        InventoryFlowManager.enableMoveKeys();
                     }
                  });
                  break;
               case "SpookyTime":
                  script.cleanup().addStep(0, new InventoryFlowManager.ScriptActionAdapter() {
                     @Override
                     public void perform() {
                        InventoryFlowManager.disableMoveKeys();
                     }
                  }).addStep(0, new InventoryFlowManager.ScriptRunnableAction(task)).addStep(0, new InventoryFlowManager.ScriptActionAdapter() {
                     @Override
                     public void perform() {
                        InventoryFlowManager.enableMoveKeys();
                     }
                  });
                  break;
               case "HolyWorld":
                  script.cleanup().addStep(0, new InventoryFlowManager.ScriptActionAdapter() {
                     @Override
                     public void perform() {
                        InventoryFlowManager.disableMoveKeys();
                     }
                  }).addStep(0, new InventoryFlowManager.ScriptRunnableAction(task)).addStep(0, new InventoryFlowManager.ScriptActionAdapter() {
                     @Override
                     public void perform() {
                        InventoryFlowManager.enableMoveKeys();
                     }
                  });
                  break;
               case "CopyTime":
                  script.cleanup().addStep(0, new InventoryFlowManager.ScriptActionAdapter() {
                     @Override
                     public void perform() {
                        InventoryFlowManager.disableMoveKeys();
                     }
                  }).addStep(0, new InventoryFlowManager.ScriptRunnableAction(task)).addStep(0, new InventoryFlowManager.ScriptActionAdapter() {
                     @Override
                     public void perform() {
                        InventoryFlowManager.enableMoveKeys();
                     }
                  });
                  break;
               case "ReallyWorld":
               case "Normal":
                  task.run();
                  break;
               default:
                  task.run();
            }
         } else {
            TASK_QUEUE.offer(task);
         }
      }
   }

   public static void addTaskSequence(Runnable... tasks) {
      if (tasks != null && tasks.length != 0) {
         if (script.isFinished() && postScript.isFinished()) {
            script.cleanup();

            for (Runnable task : tasks) {
               if (task != null) {
                  script.addStep(1, task::run);
               }
            }
         } else {
            TASK_QUEUE.offer(() -> addTaskSequence(tasks));
         }
      }
   }

   public static void disableMoveKeys() {
      canMove = false;
      postScript.cleanup();
      unPressMoveKeys();
   }

   public static void enableMoveKeys() {
      Script restoreScript = postScript.cleanup().addTickStep(0, new InventoryFlowManager.ScriptActionAdapter() {
         @Override
         public void perform() {
            InventoryFlowManager.unPressMoveKeys();
         }
      });
      if (TASK_QUEUE.isEmpty()) {
         restoreScript.addTickStep(0, new InventoryFlowManager.ScriptActionAdapter() {
            @Override
            public void perform() {
               InventoryFlowManager.stopPlayerSprintBeforeClose();
            }
         }).addTickStep(0, new InventoryFlowManager.ScriptActionAdapter() {
            @Override
            public void perform() {
               InventoryTask.closeScreen(true);
            }
         }).addTickStep(0, new InventoryFlowManager.ScriptActionAdapter() {
            @Override
            public void perform() {
               InventoryFlowManager.finishEnableMoveKeys();
            }
         });
      } else {
         restoreScript.addTickStep(0, new InventoryFlowManager.ScriptActionAdapter() {
            @Override
            public void perform() {
               InventoryFlowManager.finishEnableMoveKeys();
            }
         });
      }
   }

   public static void unPressMoveKeys() {
      if (mc.options != null) {
         for (KeyMapping key : getMoveKeys()) {
            key.setDown(false);
         }

         stopSprint();
      }
   }

   public static void updateMoveKeys() {
      if (mc.options != null && mc.getWindow() != null) {
         for (KeyMapping key : getMoveKeys()) {
            key.setDown(isCurrentlyPressed(key));
         }
      }
   }

   public static boolean shouldSkipExecution() {
      if (mc.screen == null) {
         return false;
      } else {
         return !(mc.screen instanceof ChatScreen)
               && !(mc.screen instanceof SignEditScreen)
               && !(mc.screen instanceof AnvilScreen)
               && !(mc.screen instanceof AbstractCommandBlockEditScreen)
               && !(mc.screen instanceof StructureBlockEditScreen)
            ? !"HolyWorld".equals(getSelectedMode()) || !(mc.screen instanceof ContainerScreen)
            : false;
      }
   }

   public static void reset() {
      script.cleanup();
      postScript.cleanup();
      TASK_QUEUE.clear();
      canMove = true;
      updateMoveKeys();
      updateSprintKey();
   }

   public static void restoreMoveKeysOnly() {
      postScript.cleanup();
      canMove = true;
      updateMoveKeys();
      updateSprintKey();
   }

   public static boolean isMovementAllowed() {
      return canMove;
   }

   public static boolean isIdle() {
      return script.isFinished() && postScript.isFinished() && TASK_QUEUE.isEmpty();
   }

   private static void finishEnableMoveKeys() {
      canMove = true;
      updateMoveKeys();
      updateSprintKey();
   }

   private static void updateSprintKey() {
      if (mc.options != null && mc.getWindow() != null) {
         mc.options.keySprint.setDown(isCurrentlyPressed(mc.options.keySprint));
      }
   }

   private static void stopSprint() {
      if (mc.options != null) {
         mc.options.keySprint.setDown(false);
      }
   }

   private static void stopPlayerSprintBeforeClose() {
      stopSprint();
      if (mc.player != null) {
         mc.player.setSprinting(false);
      }
   }

   private static String getSelectedMode() {
      InventoryMove inventoryMove = InventoryMove.getInstance();
      return inventoryMove != null && inventoryMove.isEnabled() ? normalizeModeName(inventoryMove.getSelectedMode()) : "Normal";
   }

   private static String normalizeModeName(String mode) {
      if (mode == null) {
         return "Normal";
      } else {
         return switch (mode) {
            case "ФанТайм", "Фантайм" -> "FunTime";
            case "СпукиТайм" -> "SpookyTime";
            case "ХолиВорлд" -> "HolyWorld";
            case "РиллиВорлд" -> "ReallyWorld";
            case "КопиТайм" -> "CopyTime";
            default -> mode;
         };
      }
   }

   private static List<KeyMapping> getMoveKeys() {
      return List.of(mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump, mc.options.keySprint);
   }

   private static boolean isCurrentlyPressed(KeyMapping key) {
      if (mc.getWindow() == null) {
         return false;
      } else {
         Key inputKey = InputConstants.getKey(key.saveString());
         Window window = mc.getWindow();
         long handle = window.handle();

         return switch (inputKey.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, inputKey.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(handle, inputKey.getValue()) == 1;
            default -> false;
         };
      }
   }

   public static void addCombatSwapAttackSequence(Runnable preSwap, Runnable attack, Runnable postSwap) {
      if (preSwap != null && attack != null && postSwap != null) {
         if (script.isFinished() && postScript.isFinished()) {
            script.cleanup()
               .addStep(0, InventoryFlowManager::disableMoveKeys)
               .addStep(0, preSwap::run)
               .addStep(0, InventoryFlowManager::enableMoveKeys)
               .addStep(1, attack::run)
               .addStep(1, InventoryFlowManager::disableMoveKeys)
               .addStep(0, postSwap::run)
               .addStep(0, InventoryFlowManager::enableMoveKeys);
         } else {
            TASK_QUEUE.offer(() -> addCombatSwapAttackSequence(preSwap, attack, postSwap));
         }
      }
   }

   private abstract static class ScriptActionAdapter implements ScriptAction {
      private ScriptActionAdapter() {
      }
   }

   private static final class ScriptRunnableAction extends InventoryFlowManager.ScriptActionAdapter {
      private final Runnable runnable;

      private ScriptRunnableAction(Runnable runnable) {
         this.runnable = runnable;
      }

      @Override
      public void perform() {
         if (this.runnable != null) {
            this.runnable.run();
         }
      }
   }
}
