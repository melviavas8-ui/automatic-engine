package daamky.ua.api.module.impl.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Box;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.target.TargetFinder;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.player.PlayerSimulation;

public final class AutoMaceSwap extends Module {
   private static AutoMaceSwap instance;
   private static final int SWAP_STOP_TICKS = 1;
   private final NumberSetting targetRange = this.register(new NumberSetting("Target Range", "Search range for targets.", 8.0, 3.0, 16.0, 0.5));
   private final NumberSetting attackRange = this.register(new NumberSetting("Attack Range", "Range for the mace hit.", 5.0, 2.5, 6.5, 0.1));
   private final MultiModeSetting targetType = this.register(
      new MultiModeSetting(
         "Targets", "Target type filter.", new String[]{"Players", "Friends", "Mobs", "Animals", "Invisible", "Armor Stands", "Naked Players"}, "Players"
      )
   );
   private final BooleanSetting throughWalls = this.register(new BooleanSetting("Through Walls", "Allows target search without line of sight.", false));
   private final TargetFinder targetFinder = new TargetFinder();
   private final StopWatch armedTimer = new StopWatch();
   private LivingEntity target;
   private boolean maceArmed;
   private int restoredSlot = -1;
   private int armedInventorySlot = -1;
   private boolean armedFromHotbar;
   private int nextAttackTick = -1;
   private int pendingSwapSlot = -1;
   private int pendingSwapButton = -1;
   private int pendingStopTicks;
   private boolean pendingRestoreSwap;
   private boolean pendingArmedFromHotbar;
   private int pendingArmedInventorySlot = -1;
   private boolean restoreUp;
   private boolean restoreDown;
   private boolean restoreLeft;
   private boolean restoreRight;
   private boolean restoreSprint;

   public AutoMaceSwap() {
      super("Mace Swap", "Takes mace when there is enough air time left to cool down and hit.", ModuleCategory.COMBAT);
      instance = this;
   }

   public static AutoMaceSwap getInstance() {
      return instance;
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player == null || client.world == null || client.interactionManager == null) {
         this.resetState();
      } else if (client.currentScreen != null || client.player.isGliding() || client.player.getAbilities().flying || this.isInLiquidOrClimbable(client)) {
         this.resetState();
      } else if (!client.player.isTouchingWater()) {
         if (client.player.isOnGround()) {
            if (this.maceArmed) {
               this.restoreArm();
               if (this.pendingSwapSlot != -1) {
                  return;
               }
            }

            this.resetState();
         } else if (!this.isAirborne(client)) {
            this.resetState();
         } else {
            double requiredHeightBlocks = this.estimateRequiredHeightForCooldownBlocks(client);
            double heightAboveGround = this.estimateHeightAboveGroundBlocks(client);
            if (!this.maceArmed && heightAboveGround < requiredHeightBlocks) {
               this.target = null;
               this.targetFinder.releaseTarget();
            } else {
               this.updateTarget(client);
               if (this.target != null && this.target.isAlive()) {
                  if (this.maceArmed || this.armMace(client)) {
                     if (this.armedTimer.finished(500.0)) {
                        if (client.player.age >= this.nextAttackTick) {
                           double rangeSq = this.attackRange.getValue() * this.attackRange.getValue();
                           if (!(client.player.squaredDistanceTo(this.target) > rangeSq)) {
                              if (this.tryAttackWithMace(client)) {
                                 this.nextAttackTick = client.player.age + 2;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      this.restoreArm();
      this.resetState();
   }

   private void updateTarget(MinecraftClient client) {
      TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(this.targetType.getValue());
      LivingEntity sharedTarget = this.resolveSharedTarget(filter);
      if (sharedTarget != null && sharedTarget.isAlive()) {
         this.target = sharedTarget;
         this.targetFinder.releaseTarget();
      } else {
         this.targetFinder.searchTargets(client.world.getEntities(), this.targetRange.getFloat(), 360.0F, this.throughWalls.getValue(), filter::isValid);
         this.targetFinder.validateTarget(filter::isValid);
         this.target = this.targetFinder.getCurrentTarget();
      }
   }

   private LivingEntity resolveSharedTarget(TargetFinder.EntityFilter filter) {
      AuraModule aura = AuraModule.getInstance();
      if (aura != null && aura.isEnabled() && AuraModule.target != null && AuraModule.target.isAlive() && filter.isValid(AuraModule.target)) {
         return AuraModule.target;
      } else {
         TriggerBot triggerBot = TriggerBot.getInstance();
         if (triggerBot != null && triggerBot.isEnabled() && triggerBot.target != null && triggerBot.target.isAlive() && filter.isValid(triggerBot.target)) {
            return triggerBot.target;
         } else {
            LivingEntity syncedAimTarget = AimAssist.getSyncedTarget();
            if (syncedAimTarget != null && syncedAimTarget.isAlive() && filter.isValid(syncedAimTarget)) {
               AimAssist aimAssist = AimAssist.getInstance();
               if (aimAssist != null && aimAssist.isEnabled()) {
                  return syncedAimTarget;
               }
            }

            return null;
         }
      }
   }

   private boolean armMace(MinecraftClient client) {
      if (this.pendingSwapSlot != -1) {
         return this.finishPendingArmSwap(client);
      } else {
         int hotbarMace = InventoryUtils.findHotbarItem(Items.MACE);
         int inventoryMace = hotbarMace == -1 ? InventoryUtils.findItemInInventory(Items.MACE) : -1;
         if (hotbarMace == -1 && inventoryMace == -1) {
            return false;
         } else {
            this.restoredSlot = client.player.getInventory().getSelectedSlot();
            if (hotbarMace != -1) {
               this.armedFromHotbar = true;
               this.armedInventorySlot = hotbarMace;
               if (hotbarMace != this.restoredSlot) {
                  this.scheduleArmSwap(client, InventoryUtils.wrapSlot(hotbarMace), this.restoredSlot, true, hotbarMace);
                  return false;
               } else {
                  this.maceArmed = true;
                  this.armedTimer.reset();
                  this.nextAttackTick = client.player.age + 1;
                  return true;
               }
            } else {
               this.armedFromHotbar = false;
               this.armedInventorySlot = inventoryMace;
               this.scheduleArmSwap(client, InventoryUtils.wrapSlot(inventoryMace), this.restoredSlot, false, inventoryMace);
               return false;
            }
         }
      }
   }

   private void scheduleArmSwap(MinecraftClient client, int slot, int button, boolean fromHotbar, int inventorySlot) {
      this.pendingSwapSlot = slot;
      this.pendingSwapButton = button;
      this.pendingStopTicks = 1;
      this.pendingArmedFromHotbar = fromHotbar;
      this.pendingArmedInventorySlot = inventorySlot;
      this.restoreUp = client.options.forwardKey.isPressed();
      this.restoreDown = client.options.backKey.isPressed();
      this.restoreLeft = client.options.leftKey.isPressed();
      this.restoreRight = client.options.rightKey.isPressed();
      this.restoreSprint = client.options.sprintKey.isPressed();
   }

   private boolean finishPendingArmSwap(MinecraftClient client) {
      this.fullStop(client);
      if (this.pendingStopTicks > 0) {
         this.pendingStopTicks--;
         return false;
      } else {
         InventoryUtils.click(this.pendingSwapSlot, this.pendingSwapButton, SlotActionType.SWAP);
         this.restoreMovement(client);
         if (this.pendingRestoreSwap) {
            this.maceArmed = false;
            this.restoredSlot = -1;
            this.armedInventorySlot = -1;
            this.armedFromHotbar = false;
            this.pendingSwapSlot = -1;
            this.pendingSwapButton = -1;
            this.pendingRestoreSwap = false;
            this.pendingArmedInventorySlot = -1;
            return true;
         } else {
            this.armedFromHotbar = this.pendingArmedFromHotbar;
            this.armedInventorySlot = this.pendingArmedInventorySlot;
            this.maceArmed = true;
            this.armedTimer.reset();
            this.nextAttackTick = client.player.age + 1;
            this.pendingSwapSlot = -1;
            this.pendingSwapButton = -1;
            this.pendingRestoreSwap = false;
            this.pendingArmedInventorySlot = -1;
            return true;
         }
      }
   }

   private void restoreMovement(MinecraftClient client) {
      if (client != null && client.options != null) {
         client.options.forwardKey.setPressed(this.restoreUp);
         client.options.backKey.setPressed(this.restoreDown);
         client.options.leftKey.setPressed(this.restoreLeft);
         client.options.rightKey.setPressed(this.restoreRight);
         client.options.sprintKey.setPressed(this.restoreSprint);
      }
   }

   private void fullStop(MinecraftClient client) {
      if (client != null && client.player != null && client.options != null) {
         client.options.forwardKey.setPressed(false);
         client.options.backKey.setPressed(false);
         client.options.leftKey.setPressed(false);
         client.options.rightKey.setPressed(false);
         client.options.sprintKey.setPressed(false);
         client.options.jumpKey.setPressed(false);
         client.player.setSprinting(false);
      }
   }

   private boolean tryAttackWithMace(MinecraftClient client) {
      return client.player != null && client.interactionManager != null && this.target != null && this.target.isAlive();
   }

   private void restoreArm() {
      if (this.maceArmed && this.restoredSlot != -1 && mc.player != null) {
         if (this.pendingSwapSlot != -1) {
            this.finishPendingArmSwap(mc);
         } else {
            if (this.armedFromHotbar) {
               if (this.armedInventorySlot != this.restoredSlot) {
                  this.scheduleArmSwap(mc, InventoryUtils.wrapSlot(this.armedInventorySlot), this.restoredSlot, this.armedFromHotbar, this.armedInventorySlot);
                  this.pendingRestoreSwap = true;
               }
            } else if (this.armedInventorySlot != -1) {
               this.scheduleArmSwap(mc, InventoryUtils.wrapSlot(this.armedInventorySlot), this.restoredSlot, this.armedFromHotbar, this.armedInventorySlot);
               this.pendingRestoreSwap = true;
            }
         }
      }
   }

   private boolean isFalling(MinecraftClient client) {
      return client.player != null
         && !client.player.isOnGround()
         && !client.player.isTouchingWater()
         && !client.player.isInLava()
         && !client.player.isClimbing();
   }

   private boolean isAirborne(MinecraftClient client) {
      return this.isFalling(client);
   }

   private double estimateHeightAboveGroundBlocks(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         PlayerSimulation simulation = PlayerSimulation.fromClientPlayer(PlayerSimulation.SimulatedPlayerInput.guessInput(client.player));
         int ticksToSimulate = Math.max(1, (int)Math.ceil(10.0));
         double lowestHeight = this.sampleHeightAboveGround(client, simulation.boundingBox);

         for (int tick = 0; tick < ticksToSimulate; tick++) {
            simulation.tick();
            lowestHeight = Math.min(lowestHeight, this.sampleHeightAboveGround(client, simulation.boundingBox));
            if (lowestHeight <= 0.0) {
               break;
            }
         }

         return lowestHeight;
      } else {
         return 0.0;
      }
   }

   private double estimateRequiredHeightForCooldownBlocks(MinecraftClient client) {
      if (client.player == null) {
         return 0.0;
      } else {
         PlayerSimulation simulation = PlayerSimulation.fromClientPlayer(
            PlayerSimulation.SimulatedPlayerInput.fromClientPlayer(client.player.input.playerInput)
         );
         double startY = simulation.pos.y;
         double cooldownMs = 500.0;
         int ticksToSimulate = Math.max(1, (int)Math.ceil(cooldownMs / 50.0));
         double lowestY = startY;

         for (int tick = 0; tick < ticksToSimulate; tick++) {
            simulation.tick();
            lowestY = Math.min(lowestY, simulation.pos.y);
            if (simulation.onGround) {
               return Math.max(0.5, startY - lowestY + 0.35);
            }
         }

         double drop = Math.max(0.0, startY - lowestY);
         return Math.max(0.5, drop + 0.35);
      }
   }

   private boolean isInLiquidOrClimbable(MinecraftClient client) {
      return client.player.isTouchingWater() || client.player.isInLava() || client.player.isClimbing();
   }

   private double sampleHeightAboveGround(MinecraftClient client, Box box) {
      double maxDistance = 24.0;
      double step = 0.05;

      for (double distance = 0.0; distance <= maxDistance; distance += step) {
         if (!client.world.isSpaceEmpty(client.player, box.offset(0.0, -distance, 0.0))) {
            return Math.max(0.0, distance - step);
         }
      }

      return maxDistance;
   }

   private void resetState() {
      this.target = null;
      this.targetFinder.releaseTarget();
      this.maceArmed = false;
      this.restoredSlot = -1;
      this.armedInventorySlot = -1;
      this.armedFromHotbar = false;
      this.armedTimer.reset();
      this.nextAttackTick = -1;
      this.pendingSwapSlot = -1;
      this.pendingSwapButton = -1;
      this.pendingStopTicks = 0;
      this.pendingRestoreSwap = false;
      this.pendingArmedFromHotbar = false;
      this.pendingArmedInventorySlot = -1;
      this.restoreUp = false;
      this.restoreDown = false;
      this.restoreLeft = false;
      this.restoreRight = false;
      this.restoreSprint = false;
   }
}
