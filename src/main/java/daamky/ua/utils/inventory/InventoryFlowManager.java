package daamky.ua.utils.inventory;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.InputUtil.Key;
import org.lwjgl.glfw.GLFW;
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
         for (KeyBinding key : getMoveKeys()) {
            key.setPressed(false);
         }

         stopSprint();
      }
   }

   public static void updateMoveKeys() {
      if (mc.options != null && mc.getWindow() != null) {
         for (KeyBinding key : getMoveKeys()) {
            key.setPressed(isCurrentlyPressed(key));
         }
      }
   }

   public static boolean shouldSkipExecution() {
      if (mc.currentScreen == null) {
         return false;
      } else {
         return !(mc.currentScreen instanceof ChatScreen)
               && !(mc.currentScreen instanceof SignEditScreen)
               && !(mc.currentScreen instanceof AnvilScreen)
               && !(mc.currentScreen instanceof AbstractCommandBlockScreen)
               && !(mc.currentScreen instanceof StructureBlockScreen)
            ? !"HolyWorld".equals(getSelectedMode()) || !(mc.currentScreen instanceof GenericContainerScreen)
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
         mc.options.sprintKey.setPressed(isCurrentlyPressed(mc.options.sprintKey));
      }
   }

   private static void stopSprint() {
      if (mc.options != null) {
         mc.options.sprintKey.setPressed(false);
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

   private static List<KeyBinding> getMoveKeys() {
      return List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey);
   }

   private static boolean isCurrentlyPressed(KeyBinding key) {
      if (mc.getWindow() == null) {
         return false;
      } else {
         Key inputKey = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
         Window window = mc.getWindow();
         long handle = window.getHandle();

         return switch (inputKey.getCategory()) {
            case KEYSYM -> InputUtil.isKeyPressed(window, inputKey.getCode());
            case MOUSE -> GLFW.glfwGetMouseButton(handle, inputKey.getCode()) == 1;
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
