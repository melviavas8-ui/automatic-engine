package daamky.ua.api.module.impl.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.events.types.EventPhase;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConfig;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.impl.LinearConstructor;
import daamky.ua.api.module.impl.combat.aura.target.TargetFinder;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.module.impl.combat.aura.util.TaskPriority;
import daamky.ua.api.module.impl.combat.macetarget.armor.ArmorSwapHandler;
import daamky.ua.api.module.impl.combat.macetarget.armor.FireworkHandler;
import daamky.ua.api.module.impl.combat.macetarget.attack.AttackHandler;
import daamky.ua.api.module.impl.combat.macetarget.flight.FlightController;
import daamky.ua.api.module.impl.combat.macetarget.prediction.TargetPredictor;
import daamky.ua.api.module.impl.combat.macetarget.stage.StageHandler;
import daamky.ua.api.module.impl.combat.macetarget.state.MaceState;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.inventory.swap.SwapSettings;

public class MaceTarget extends Module {
   private static MaceTarget instance;
   private final ModeSetting serverMode = this.register(new ModeSetting("Server", "Server mode.", "Standard", "Standard", "ReallyWorld"));
   private final ModeSetting swapMode = this.register(new ModeSetting("Swap", "Mace swap mode.", "Silent", "Silent", "Legit"));
   private final NumberSetting height = this.register(new NumberSetting("Height", "Flight height above target.", 30.0, 20.0, 60.0, 1.0));
   private final MultiModeSetting targetType = this.register(
      new MultiModeSetting("Targets", "Target type filter.", new String[]{"Players", "Mobs", "Animals"}, "Players")
   );
   private final BooleanSetting autoEquipChest = this.register(new BooleanSetting("Auto Chestplate", "Equip chestplate on disable when possible.", true));
   private final BooleanSetting predictMovement = this.register(new BooleanSetting("Predict", "Predict target movement.", true));
   private final TargetPredictor predictor = new TargetPredictor();
   private final FlightController flightController = new FlightController(this.predictor);
   private final ArmorSwapHandler armorSwapHandler = new ArmorSwapHandler(this::buildSettings);
   private final FireworkHandler fireworkHandler = new FireworkHandler(this::buildSettings);
   private final AttackHandler attackHandler = new AttackHandler();
   private final StopWatch fireworkTimer = new StopWatch();
   private final StageHandler stageHandler = new StageHandler(this.armorSwapHandler, this.fireworkHandler, this.attackHandler, this.fireworkTimer);
   private final TargetFinder targetFinder = new TargetFinder();
   private LivingEntity target;

   public MaceTarget() {
      super("Mace Target", "Mace combat target helper.", ModuleCategory.COMBAT);
      instance = this;
   }

   public static MaceTarget getInstance() {
      return instance;
   }

   @Override
   protected void onEnable() {
      this.stageHandler.reset();
      this.target = null;
      this.attackHandler.reset();
      this.armorSwapHandler.reset();
      this.fireworkHandler.reset();
      this.predictor.reset();
      this.fireworkTimer.reset();
   }

   @Override
   protected void onDisable() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.autoEquipChest.getValue() && client.player != null) {
         this.equipChestplateOnDisable();
      }

      this.armorSwapHandler.forceRestore();
      this.fireworkHandler.forceRestore();
      this.target = null;
      this.targetFinder.releaseTarget();
      this.armorSwapHandler.reset();
      this.fireworkHandler.reset();
      this.attackHandler.reset();
      this.predictor.reset();
      this.stageHandler.reset();
      AngleConnection.INSTANCE.startReturning();
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         if (!this.isSilentMode()) {
            this.armorSwapHandler.processLoop();
            this.fireworkHandler.processLoop();
         }

         this.attackHandler.processLoop();
         if (!this.armorSwapHandler.isActive() && !this.fireworkHandler.isActive()) {
            if (this.target != null && this.target.isAlive()) {
               this.processStage();
            }
         }
      } else {
         this.resetAllStates();
      }
   }

   @SubscribeEvent
   private void onRotationUpdate(RotationUpdateEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null) {
         if (event.getPhase() != EventPhase.PRE) {
            if (event.getPhase() == EventPhase.POST) {
               this.handlePostRotation();
            }
         } else {
            this.updateHandlers();
            if (this.target == null || !this.target.isAlive()) {
               this.findTarget();
            }

            if (this.target != null) {
               this.predictor.update(this.target);
               MaceState.Stage currentStage = this.stageHandler.getStage();
               if (currentStage == MaceState.Stage.FLYING_UP) {
                  if (InventoryUtils.hasElytra() && client.player.isGliding()) {
                     this.rotateTo(this.flightController.calculateAngle(this.target, currentStage));
                  }
               } else if (currentStage == MaceState.Stage.TARGETTING || currentStage == MaceState.Stage.ATTACKING) {
                  this.rotateTo(this.flightController.calculateAngle(this.target, currentStage));
               }
            }
         }
      }
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         if (this.armorSwapHandler.getMovement().isBlocked() || this.fireworkHandler.getMovement().isBlocked()) {
            event.setDirectionalLow(false, false, false, false);
            event.setJumping(false);
            event.setSprinting(false);
            client.player.setSprinting(false);
         }

         if (this.target != null && InventoryUtils.hasElytra() && this.stageHandler.getStage() == MaceState.Stage.FLYING_UP) {
            if (client.player.isOnGround()) {
               event.setJumping(true);
            } else if (!client.player.isGliding() && !client.player.getAbilities().flying) {
               event.setJumping(client.player.age % 2 == 0);
            }
         }
      }
   }

   private boolean isSilentMode() {
      return this.swapMode.is("Silent");
   }

   private SwapSettings buildSettings() {
      return this.isSilentMode() ? SwapSettings.instant() : SwapSettings.legit();
   }

   private void updateHandlers() {
      this.stageHandler.setSilentMode(this.isSilentMode());
      this.stageHandler.setReallyWorldMode(this.serverMode.is("ReallyWorld"));
      this.stageHandler.setHeight(this.height.getFloat());
      this.flightController.setPredictionEnabled(this.predictMovement.getValue());
      this.flightController.setHeight(this.height.getFloat());
   }

   private void findTarget() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world == null) {
         this.targetFinder.releaseTarget();
         this.target = null;
      } else {
         TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(this.targetType.getValue());
         this.targetFinder.searchTargets(client.world.getEntities(), 128.0F, 360.0F, true);
         this.targetFinder.validateTarget(filter::isValid);
         this.target = this.targetFinder.getCurrentTarget();
      }
   }

   private void processStage() {
      boolean hasElytra = InventoryUtils.hasElytra();
      switch (this.stageHandler.getStage()) {
         case PREPARE:
            this.stageHandler.handlePrepare(hasElytra);
            break;
         case FLYING_UP:
            this.stageHandler.handleFlyingUp(this.target, hasElytra);
            break;
         case TARGETTING:
            this.stageHandler.handleTargetting(this.target);
            break;
         case ATTACKING:
            this.stageHandler.handleAttacking(this.target, hasElytra);
      }
   }

   private void handlePostRotation() {
      if (this.attackHandler.isPendingAttack()) {
         this.attackHandler.performAttack(this.target);
         this.attackHandler.setPendingAttack(false);
         if (this.attackHandler.isShouldDisableAfterAttack()) {
            this.attackHandler.setShouldDisableAfterAttack(false);
            this.setEnabled(false);
         }
      }
   }

   private void rotateTo(Angle angle) {
      AngleConnection.INSTANCE
         .rotateTo(
            new Angle.VecRotation(angle, angle.toVector()),
            this.target,
            1,
            new AngleConfig(new LinearConstructor(), true, false),
            TaskPriority.HIGH_IMPORTANCE_1,
            this
         );
   }

   private void equipChestplateOnDisable() {
      if (InventoryUtils.hasElytra()) {
         int slot = InventoryUtils.findChestArmorSlot();
         if (slot != -1) {
            InventoryUtils.swap(InventoryUtils.wrapSlot(slot), 6);
            InventoryUtils.closeScreen();
         }
      }
   }

   private void resetAllStates() {
      this.armorSwapHandler.reset();
      this.fireworkHandler.reset();
      this.attackHandler.reset();
      this.predictor.reset();
      this.target = null;
      this.targetFinder.releaseTarget();
      this.stageHandler.reset();
   }
}
