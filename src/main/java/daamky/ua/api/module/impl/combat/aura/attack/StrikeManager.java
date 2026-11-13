package daamky.ua.api.module.impl.combat.aura.attack;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import daamky.ua.IMinecraft;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.UsingItemEvent;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.AutoSwap;
import daamky.ua.api.module.impl.combat.Criticals;
import daamky.ua.api.module.impl.combat.TriggerBot;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.context.AutoRegressionContext;
import daamky.ua.api.module.impl.combat.aura.rotations.NeuroTrainingCollector;
import daamky.ua.api.module.impl.combat.aura.target.RaycastAngle;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.module.impl.movement.ElytraTarget;
import daamky.ua.api.module.impl.player.FreeCam;
import daamky.ua.mixin.accessor.LivingEntityAccessor;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;
import daamky.ua.utils.inventory.swap.SwapExecutor;
import daamky.ua.utils.player.PlayerSimulation;

public class StrikeManager implements IMinecraft {
   private static final long CRIT_STARTUP_HOLD_MS = 450L;
   private static final int SWING_ONLY_EVERY_ATTACKS = 15;
   private static final float LEGIT_CRIT_STAGE_ONE_FALL = 0.2F;
   private static final float LEGIT_CRIT_STAGE_TWO_FALL = 0.0F;
   private static final float LEGIT_CRIT_STAGE_THREE_FALL = 0.1F;
   private static final float LEGIT_CRIT_STAGE_FOUR_FALL = 0.25F;
   private static final double MIN_CRIT_DESCENT_SPEED = -0.001;
   private final StopWatch attackTimer = new StopWatch();
   private final Pressing clickScheduler = new Pressing();
   private final SwapExecutor shieldSwapExecutor = new SwapExecutor();
   private int count = 0;
   private int tickCounter = 0;
   private int shieldSuppressedUntilTick = -1;
   private int lastShieldReleaseTick = -1;
   private boolean waitingForPostReleaseTick = false;

   public StrikeManager() {
   }

   void tick() {
      if (this.shieldSwapExecutor.isRunning()) {
         this.shieldSwapExecutor.tick();
      }

      this.tickCounter++;
      this.syncShieldState();
   }

   void onPacket(PacketEvent e) {
      if (e.isSend()) {
         Object packet = e.getPacket();
         if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
            this.clickScheduler.recalculate();
         } else if (!(packet instanceof PlayerMoveC2SPacket) && !(packet instanceof ClientTickEndC2SPacket)) {
            if (packet instanceof PlayerInteractEntityC2SPacket interactPacket
               && this.waitingForPostReleaseTick
               && this.isRightClickInteraction(interactPacket)) {
               e.cancel();
            }
         } else {
            this.waitingForPostReleaseTick = false;
         }
      }
   }

   void onUsingItem(UsingItemEvent e) {
   }

   public boolean unpressShield() {
      return false;
   }

   public void resetPendingState() {
      if (this.shieldSwapExecutor.isRunning()) {
         this.shieldSwapExecutor.cancel();
      }

      this.shieldSuppressedUntilTick = -1;
      this.lastShieldReleaseTick = -1;
      this.waitingForPostReleaseTick = false;
   }

   public boolean shouldCancelShieldUse(Hand hand) {
      if (mc.player != null && hand != null) {
         this.syncShieldState();
         return this.waitingForPostReleaseTick
            ? true
            : this.tickCounter <= this.shieldSuppressedUntilTick && mc.player.getStackInHand(hand).getItem() == Items.SHIELD;
      } else {
         return false;
      }
   }

   public boolean shouldCancelUseItemOn() {
      return this.waitingForPostReleaseTick;
   }

   public boolean shouldCancelEntityInteraction() {
      return this.waitingForPostReleaseTick;
   }

   public void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (this.canAttack(config, 0)) {
         this.preAttackEntity(config);
      }

      if (!this.checkElytraMode(config) || this.checkElytraRaycast(config)) {
         boolean raytrace = RaycastAngle.rayTrace(config);
         boolean sprint = mc.player.isSprinting();
         if (mc.player.isSwimming()) {
            sprint = false;
         }

         PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(0);
         boolean wallSprintBypass = sprint && this.shouldBypassSprintGateInTightSpace(config, simulated);
         // ИСПРАВЛЕНИЕ 1: Разрешаем спринт при полете на элитре
         boolean effectiveSprint = sprint && !wallSprintBypass && !mc.player.isGliding();
         boolean canAttackNow = this.canAttack(config, 0);
         if (raytrace && canAttackNow && !effectiveSprint) {
            if (config.isShouldUnPressShield() && this.waitingForPostReleaseTick) {
               return;
            }

            if (config.isShouldUnPressShield() && this.lastShieldReleaseTick == this.tickCounter) {
               return;
            }

            if (config.isShouldUnPressShield() && this.releaseShieldForAttack()) {
               return;
            }

            this.attackEntity(config);
         }
      }
   }

   void handleTriggerAttack(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot) {
      if (!this.canAttackTrigger(config, triggerBot, 0) || this.preAttackEntity(config)) {
         boolean raytrace = RaycastAngle.rayTrace(config);
         if (raytrace && this.canAttackTrigger(config, triggerBot, 0)) {
            if (this.shouldDelayAttackAfterShieldRelease(config)) {
               return;
            }

            this.attackEntity(config);
         }
      }
   }

   boolean preAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
      return true;
   }

   private boolean shouldDelayAttackAfterShieldRelease(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (config == null || !config.isShouldUnPressShield()) {
         return false;
      } else if (this.waitingForPostReleaseTick) {
         return true;
      } else {
         return this.lastShieldReleaseTick == this.tickCounter ? true : this.releaseShieldForAttack();
      }
   }

   public void syncShieldState() {
      if (mc.options == null || !mc.options.useKey.isPressed()) {
         this.shieldSuppressedUntilTick = -1;
      }
   }

   private boolean releaseShieldForAttack() {
      if (mc.player != null && mc.interactionManager != null) {
         if (mc.player.isUsingItem() && !mc.player.getActiveItem().isEmpty() && mc.player.getActiveItem().getItem() == Items.SHIELD) {
            mc.interactionManager.stopUsingItem(mc.player);
            mc.player.clearActiveItem();
            this.lastShieldReleaseTick = this.tickCounter;
            this.shieldSuppressedUntilTick = this.tickCounter;
            this.waitingForPostReleaseTick = true;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   void attackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (this.isAiMode(config) && (this.count + 1) % 15 == 0) {
         mc.player.swingHand(Hand.MAIN_HAND);
         this.attackTimer.reset();
         this.count++;
      } else {
         AutoSwap autoSwap = AutoSwap.getInstance();
         Runnable attack = () -> {
            mc.interactionManager.attackEntity(mc.player, config.getTarget());
            mc.player.swingHand(Hand.MAIN_HAND);
            this.finishAttack(config);
         };
         if (autoSwap == null || !autoSwap.runMaxDamageAuraAttack(attack)) {
            attack.run();
         }
      }
   }

   private void finishAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
      this.attackTimer.reset();
      this.count++;
      NeuroTrainingCollector.getInstance().recordAttack(AuraModule.getInstance(), config.getTarget(), true);
      if (config != null && config.getAimMode() != null && "FunTime".equals(config.getAimMode().getValue())) {
         AutoRegressionContext context = AutoRegressionContext.getInstance();
         context.updateLastAttackTime();
         context.hitContentQueue();
      }
   }

   private boolean isFunTimeMode(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (config != null && config.getAimMode() != null) {
         String mode = config.getAimMode().getValue();
         return "FunTime".equals(mode) || "FT".equals(mode) || "FTAngle".equals(mode);
      } else {
         return false;
      }
   }

   private boolean isAiMode(StrikerConstructor.AttackPerpetratorConfigurable config) {
      return config != null && config.getAimMode() != null && "Ai".equals(config.getAimMode().getValue());
   }

   private boolean isRightClickInteraction(PlayerInteractEntityC2SPacket packet) {
      final boolean[] interaction = new boolean[]{false};
      packet.handle(new Handler() {
         public void interact(Hand hand) {
            interaction[0] = true;
         }

         public void interactAt(Hand hand, Vec3d pos) {
            interaction[0] = true;
         }

         public void attack() {
            interaction[0] = false;
         }
      });
      return interaction[0];
   }

   public boolean canAttack(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
      for (int i = 0; i <= ticks; i++) {
         if (this.canCrit(config, i)) {
            return true;
         }
      }

      return false;
   }

   public boolean canCrit(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
      if (mc.player != null && mc.options != null && config != null && config.getTarget() != null) {
         AutoSwap autoSwap = AutoSwap.getInstance();
         if (autoSwap != null && autoSwap.shouldBlockAuraAttack()) {
            return false;
         } else if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() != Items.SHIELD && config.isEatAndAttack()) {
            return false;
         } else {
            AuraModule aura = AuraModule.getInstance();
            float minimumCooldown = aura != null && aura.isCriticalBalanceEnabled() ? randomBetween(0.86F, 0.89F) : 0.82F;
            if (!this.clickScheduler.isCooldownComplete(config.isTpsSync(), (float)ticks, minimumCooldown)) {
               return false;
            } else {
               PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);
               if (this.shouldBypassCriticalChecks(simulated)) {
                  return true;
               } else {
                  boolean criticalState = this.isPlayerInCriticalState(simulated, ticks);
                  Criticals criticals = Criticals.getInstance();
                  if (criticals != null && criticals.isEnabled()) {
                     if (criticals.mode.is("Vanilla")) {
                        criticalState = true;
                     } else if (criticals.mode.is("Matrix") && !mc.player.isOnGround()) {
                        criticalState = true;
                     }
                  }

                  boolean smartCritical = aura != null && aura.isEnabled() && aura.getSmartCriticals().getValue();
                  if (smartCritical && !mc.options.jumpKey.isPressed()) {
                     return criticalState || simulated.onGround;
                  } else {
                     return config.isOnlyCritical() ? criticalState : true;
                  }
               }
            }
         }
      } else {
         return false;
      }
   }

   private boolean canAttackTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot, int ticks) {
      for (int i = 0; i <= ticks; i++) {
         if (this.canCritTrigger(config, triggerBot, i)) {
            return true;
         }
      }

      return false;
   }

   private boolean canCritTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot, int ticks) {
      if (mc.player.isUsingItem() && !mc.player.getActiveItem().getItem().equals(Items.SHIELD) && config.isEatAndAttack()) {
         return false;
      } else if (!this.clickScheduler.isCooldownComplete(false, (float)ticks)) {
         return false;
      } else if (triggerBot.isOnlyCrits() && !this.hasCriticalAttackStrength((float)ticks)) {
         return false;
      } else {
         PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);
         if (this.isFreeCamMode()) {
            return triggerBot.isOnlyCrits() ? this.isFreeCamCritWindow(simulated) : true;
         } else if (this.isAirAttackMode(simulated)) {
            return true;
         } else if (triggerBot.isOnlyCrits()) {
            if (this.hasMovementRestrictions(simulated)) {
               return true;
            } else {
               boolean tightSpaceCritical = this.isInsideTightSpace(simulated);
               boolean debuffFallback = this.hasDebuffThatBreaksCrit(simulated);
               boolean smartCritJumpRequired = triggerBot.isSmartCritsEnabled();
               boolean smartCritJumpIntent = smartCritJumpRequired && this.isSmartCritJumpIntent();
               if (this.shouldHoldCriticalOnStartupTrigger(simulated, triggerBot, smartCritJumpRequired, smartCritJumpIntent)) {
                  return false;
               } else if (smartCritJumpRequired && !smartCritJumpIntent && !debuffFallback) {
                  return true;
               } else if (debuffFallback) {
                  return true;
               } else {
                  return tightSpaceCritical ? this.isTightSpaceCriticalWindow(simulated) : this.isPlayerInCriticalState(simulated);
               }
            }
         } else {
            return true;
         }
      }
   }

   private boolean hasMovementRestrictions(PlayerSimulation simulated) {
      return simulated.hasStatusEffect(StatusEffects.BLINDNESS)
         || simulated.hasStatusEffect(StatusEffects.LEVITATION)
         || PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.expand(-0.001), Blocks.COBWEB)
         || simulated.isSubmergedInWater()
         || simulated.isInLava()
         || simulated.isClimbing()
         || !PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, simulated.pos)
         || simulated.player.getAbilities().flying;
   }

   private boolean shouldBypassCriticalChecks(PlayerSimulation simulated) {
      // ИСПРАВЛЕНИЕ 2: Разрешаем игнорировать проверки критов при полете
      return this.hasMovementRestrictions(simulated) || simulated.isFallFlying || mc.player.isGliding();
   }

   private boolean isInsideTightSpace(PlayerSimulation simulated) {
      return !PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, simulated.pos)
         || simulated.horizontalCollision
         || this.hasHorizontalBoxPressure(simulated)
         || this.countBlockingSides(simulated) >= 2;
   }

   private boolean isTightSpaceCriticalWindow(PlayerSimulation simulated) {
      if (this.isPvpTradeCriticalWindow(simulated)) {
         return true;
      } else {
         float requiredFallDistance = this.resolveCriticalFallDistance(simulated, true);
         double requiredVelocity = this.resolveCriticalVelocity(simulated, true);
         if (simulated.horizontalCollision || this.hasHorizontalBoxPressure(simulated)) {
            requiredFallDistance = Math.min(requiredFallDistance, 0.004F);
            requiredVelocity = Math.max(requiredVelocity, -0.01);
         }

         return !simulated.onGround && simulated.fallDistance > requiredFallDistance && simulated.velocity.y < requiredVelocity;
      }
   }

   private boolean hasHorizontalBoxPressure(PlayerSimulation simulated) {
      if (mc.world != null && simulated != null) {
         Box expandedBox = simulated.boundingBox.expand(0.22, 0.0, 0.22).contract(1.0E-7);
         return !mc.world.isSpaceEmpty(simulated.player, expandedBox);
      } else {
         return false;
      }
   }

   private int countBlockingSides(PlayerSimulation simulated) {
      int blockingSides = 0;
      if (this.isLateralSideBlocked(simulated, 0.72, 0.0)) {
         blockingSides++;
      }

      if (this.isLateralSideBlocked(simulated, -0.72, 0.0)) {
         blockingSides++;
      }

      if (this.isLateralSideBlocked(simulated, 0.0, 0.72)) {
         blockingSides++;
      }

      if (this.isLateralSideBlocked(simulated, 0.0, -0.72)) {
         blockingSides++;
      }

      return blockingSides;
   }

   private boolean isLateralSideBlocked(PlayerSimulation simulated, double offsetX, double offsetZ) {
      if (mc.world != null && simulated != null) {
         double footY = simulated.boundingBox.minY + 0.1;
         double bodyY = Math.min(simulated.boundingBox.maxY - 0.1, simulated.boundingBox.minY + 0.95);
         return this.isSolidCollisionBlock(BlockPos.ofFloored(simulated.pos.x + offsetX, footY, simulated.pos.z + offsetZ))
            || this.isSolidCollisionBlock(BlockPos.ofFloored(simulated.pos.x + offsetX, bodyY, simulated.pos.z + offsetZ));
      } else {
         return false;
      }
   }

   private boolean isSolidCollisionBlock(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         return !state.isAir() && !state.getCollisionShape(mc.world, pos).isEmpty();
      }
   }

   private boolean shouldBypassSprintGateInTightSpace(StrikerConstructor.AttackPerpetratorConfigurable config, PlayerSimulation simulated) {
      if (mc.player == null || config == null || simulated == null || config.getTarget() == null) {
         return false;
      } else if (!this.isInsideTightSpace(simulated)) {
         return false;
      } else {
         boolean wallPressure = simulated.horizontalCollision || this.hasHorizontalBoxPressure(simulated) || this.countBlockingSides(simulated) >= 1;
         if (!wallPressure) {
            return false;
         } else {
            double distance = (double)mc.player.distanceTo(config.getTarget());
            return distance <= (double)Math.min(config.getMaximumRange() + 0.35F, 2.35F);
         }
      }
   }

   private boolean hasCriticalAttackStrength(float ticks) {
      if (mc.player == null) {
         return false;
      } else {
         float requiredStrength = this.isHoldingMace() ? 0.45F : 0.88F;
         return mc.player.getAttackCooldownProgress(ticks) > requiredStrength;
      }
   }

   private boolean isHoldingMace() {
      ItemStack mainHand = mc.player.getMainHandStack();
      return mainHand.getItem().getTranslationKey().toLowerCase().contains("mace");
   }

   private boolean hasCriticalRestrictions(PlayerSimulation simulated) {
      return simulated.hasStatusEffect(StatusEffects.BLINDNESS)
         || simulated.hasStatusEffect(StatusEffects.LEVITATION)
         || simulated.isSubmergedInWater()
         || simulated.isInLava()
         || simulated.isClimbing()
         || simulated.isSwimming
         || simulated.isFallFlying
         || simulated.player.getAbilities().flying;
   }

   private boolean hasDebuffThatBreaksCrit(PlayerSimulation simulated) {
      return simulated.hasStatusEffect(StatusEffects.BLINDNESS) || simulated.hasStatusEffect(StatusEffects.LEVITATION);
   }

   private boolean isSmartCritJumpRequired() {
      AuraModule aura = AuraModule.getInstance();
      return aura != null && aura.isEnabled() && aura.getOnlyCriticals().getValue() && aura.getSmartCriticals().getValue();
   }

   private boolean isSmartCritJumpIntent() {
      if (mc.player == null) {
         return false;
      } else if (mc.options.jumpKey.isPressed()) {
         return true;
      } else {
         LivingEntityAccessor accessor = (LivingEntityAccessor)mc.player;
         if (accessor.daamky$isJumping()) {
            return true;
         } else {
            return accessor.daamky$getJumpingCooldown() > 0 && !mc.player.isOnGround() ? true : !mc.player.isOnGround() && mc.player.getVelocity().y > 0.08;
         }
      }
   }

   private boolean isAirAttackMode(PlayerSimulation simulated) {
      return simulated.isFallFlying || simulated.player.getAbilities().flying || mc.player.isGliding();
   }

   private boolean shouldHoldCriticalOnStartup(PlayerSimulation simulated, boolean smartCritJumpRequired, boolean smartCritJumpIntent) {
      AuraModule aura = AuraModule.getInstance();
      if (aura == null || !aura.isEnabled() || !aura.getOnlyCriticals().getValue()) {
         return false;
      } else if (smartCritJumpRequired && !smartCritJumpIntent) {
         return false;
      } else {
         long activatedAt = aura.getActivationTimeMs();
         if (activatedAt <= 0L || System.currentTimeMillis() - activatedAt > 450L) {
            return false;
         } else if (simulated.onGround) {
            return false;
         } else {
            return simulated.fallDistance > 0.0F ? false : simulated.velocity.y > -0.03;
         }
      }
   }

   private boolean shouldHoldCriticalOnStartupTrigger(
      PlayerSimulation simulated, TriggerBot triggerBot, boolean smartCritJumpRequired, boolean smartCritJumpIntent
   ) {
      if (triggerBot == null || !triggerBot.isEnabled() || !triggerBot.isOnlyCrits()) {
         return false;
      } else if (smartCritJumpRequired && !smartCritJumpIntent) {
         return false;
      } else {
         long activatedAt = triggerBot.getActivationTimeMs();
         if (activatedAt <= 0L || System.currentTimeMillis() - activatedAt > 450L) {
            return false;
         } else if (simulated.onGround) {
            return false;
         } else {
            return simulated.fallDistance > 0.0F ? false : simulated.velocity.y > -0.03;
         }
      }
   }

   private boolean isFreeCamMode() {
      FreeCam freeCam = FreeCam.getInstance();
      return freeCam != null && freeCam.isEnabled();
   }

   private boolean isFreeCamCritWindow(PlayerSimulation simulated) {
      return !simulated.onGround && simulated.velocity.y <= -0.01 && !this.hasCriticalRestrictions(simulated);
   }

   private boolean checkElytraMode(StrikerConstructor.AttackPerpetratorConfigurable config) {
      // ИСПРАВЛЕНИЕ 3: Убрано требование, чтобы цель тоже летела (AuraModule.target.isGliding())
      return AuraModule.target != null
         && mc.player.isGliding()
         && ElytraTarget.getInstance() != null
         && ElytraTarget.getInstance().isEnabled();
   }

   private boolean checkElytraRaycast(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Vec3d targetVelocity = config.getTarget().getVelocity();
      float leadTicks = 0.0F;
      ElytraTarget elytraTarget = ElytraTarget.getInstance();
      if (ElytraTarget.shouldElytraTarget && elytraTarget != null) {
         leadTicks = elytraTarget.getForward();
      }

      Vec3d predictedPos = config.getTarget().getEntityPos().add(targetVelocity.multiply((double)leadTicks));
      Box predictedBox = new Box(
         predictedPos.x - (double)(config.getTarget().getWidth() / 2.0F),
         predictedPos.y,
         predictedPos.z - (double)(config.getTarget().getWidth() / 2.0F),
         predictedPos.x + (double)(config.getTarget().getWidth() / 2.0F),
         predictedPos.y + (double)config.getTarget().getHeight(),
         predictedPos.z + (double)(config.getTarget().getWidth() / 2.0F)
      );
      Vec3d eyePos = mc.player.getEyePos();
      
      // ИСПРАВЛЕНИЕ 4: Используем angle из config (целевой угол), а не текущий поворот камеры (чтобы не блокировалось плавной ротацией)
      Vec3d lookVec = config.getAngle().toVector();
      
      // ИСПРАВЛЕНИЕ 5: Увеличиваем дальность проверки рейкаста при полете на элитре
      float maxRange = (mc.player.isGliding() && elytraTarget != null && elytraTarget.isEnabled()) 
                       ? elytraTarget.getRange() 
                       : config.getMaximumRange();

      return predictedBox.raycast(eyePos, eyePos.add(lookVec.multiply((double)maxRange))).isPresent();
   }

   private boolean isPlayerInCriticalState(PlayerSimulation simulated, int ticks) {
      float requiredFall = mc.player.getMainHandStack().getItem() == Items.MACE ? 1.2F : 0.0F;
      AuraModule aura = AuraModule.getInstance();
      if (requiredFall < 1.2F && aura != null) {
         if (aura.isHitMomentLegit()) {
            requiredFall = this.currentLegitCritStageFallDistance();
         } else if (aura.isHitMomentRandom()) {
            requiredFall = randomBetween(aura.randomFallMin.getFloat(), aura.randomFallMax.getFloat());
         } else if (aura.isHitMomentStatic()) {
            requiredFall = aura.staticFall.getFloat();
         }
      }

      boolean falling = simulated.fallDistance > requiredFall && (simulated.fallDistance < 0.08F || !PlayerSimulation.simulateLocalPlayer(ticks).onGround);
      return !simulated.onGround && falling && simulated.velocity.y <= -0.001;
   }

   private float currentLegitCritStageFallDistance() {
      return switch (this.count & 3) {
         case 0 -> 0.2F;
         case 1 -> 0.0F;
         case 2 -> 0.25F;
         default -> 0.1F;
      };
   }

   private static float randomBetween(float first, float second) {
      float min = Math.min(first, second);
      float max = Math.max(first, second);
      return max - min <= 1.0E-6F ? min : (float)ThreadLocalRandom.current().nextDouble((double)min, (double)max);
   }

   private boolean isPlayerInCriticalState(PlayerSimulation simulated) {
      boolean inCobweb = PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.expand(-0.001), Blocks.COBWEB);
      Criticals criticals = Criticals.getInstance();
      if (criticals != null && criticals.isEnabled() && inCobweb) {
         return !simulated.onGround || inCobweb || simulated.fallDistance > 0.001F;
      } else if (simulated.onGround || this.hasCriticalRestrictions(simulated)) {
         return false;
      } else if (this.isPvpTradeCriticalWindow(simulated)) {
         return true;
      } else {
         boolean fallingByDistance = simulated.fallDistance > this.resolveCriticalFallDistance(simulated, false);
         boolean fallingByVelocity = simulated.velocity.y < this.resolveCriticalVelocity(simulated, false);
         return !simulated.onGround && fallingByDistance && fallingByVelocity;
      }
   }

   private boolean isPvpTradeCriticalWindow(PlayerSimulation simulated) {
      return this.isPvpPressureState(simulated) && !simulated.onGround && simulated.fallDistance > 0.0F && simulated.velocity.y < -0.01;
   }

   private boolean isPvpPressureState(PlayerSimulation simulated) {
      return mc.player != null && mc.player.hurtTime > 0 || simulated.hasStatusEffect(StatusEffects.SLOWNESS);
   }

   private float resolveCriticalFallDistance(PlayerSimulation simulated, boolean tightSpace) {
      float required = tightSpace ? 0.01F : 0.03F;
      if (this.isPvpPressureState(simulated)) {
         required = Math.min(required, tightSpace ? 0.008F : 0.012F);
      }

      if (simulated.horizontalCollision) {
         required = Math.min(required, 0.012F);
      }

      return required;
   }

   private double resolveCriticalVelocity(PlayerSimulation simulated, boolean tightSpace) {
      double required = tightSpace ? -0.02 : -0.03;
      if (this.isPvpPressureState(simulated)) {
         required = Math.max(required, tightSpace ? -0.012 : -0.018);
      }

      if (simulated.horizontalCollision) {
         required = Math.max(required, -0.015);
      }

      return required;
   }

   public StopWatch getAttackTimer() {
      return this.attackTimer;
   }

   public int getCount() {
      return this.count;
   }
}