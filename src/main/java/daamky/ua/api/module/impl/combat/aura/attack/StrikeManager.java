package daamky.ua.api.module.impl.combat.aura.attack;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket.Handler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
         if (packet instanceof ServerboundSwingPacket || packet instanceof ServerboundSetCarriedItemPacket) {
            this.clickScheduler.recalculate();
         } else if (!(packet instanceof ServerboundMovePlayerPacket) && !(packet instanceof ServerboundClientTickEndPacket)) {
            if (packet instanceof ServerboundInteractPacket interactPacket
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

   public boolean shouldCancelShieldUse(InteractionHand hand) {
      if (mc.player != null && hand != null) {
         this.syncShieldState();
         return this.waitingForPostReleaseTick
            ? true
            : this.tickCounter <= this.shieldSuppressedUntilTick && mc.player.getItemInHand(hand).getItem() == Items.SHIELD;
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

      boolean elytraFlight = this.isElytraTargetFlight();
      if (elytraFlight || !this.checkElytraMode(config) || this.checkElytraRaycast(config)) {
         boolean raytrace = elytraFlight ? this.isTargetInElytraReach(config) : RaycastAngle.rayTrace(config);
         boolean sprint = mc.player.isSprinting();
         if (mc.player.isSwimming()) {
            sprint = false;
         }

         PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(0);
         boolean wallSprintBypass = sprint && this.shouldBypassSprintGateInTightSpace(config, simulated);
         boolean effectiveSprint = sprint && !wallSprintBypass && !elytraFlight;
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
      if (mc.options == null || !mc.options.keyUse.isDown()) {
         this.shieldSuppressedUntilTick = -1;
      }
   }

   private boolean releaseShieldForAttack() {
      if (mc.player != null && mc.gameMode != null) {
         if (mc.player.isUsingItem() && !mc.player.getUseItem().isEmpty() && mc.player.getUseItem().getItem() == Items.SHIELD) {
            mc.gameMode.releaseUsingItem(mc.player);
            mc.player.stopUsingItem();
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
         mc.player.swing(InteractionHand.MAIN_HAND);
         this.attackTimer.reset();
         this.count++;
      } else {
         AutoSwap autoSwap = AutoSwap.getInstance();
         Runnable attack = () -> {
            mc.gameMode.attack(mc.player, config.getTarget());
            mc.player.swing(InteractionHand.MAIN_HAND);
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

   private boolean isRightClickInteraction(ServerboundInteractPacket packet) {
      final boolean[] interaction = new boolean[]{false};
      packet.dispatch(new Handler() {
         public void onInteraction(InteractionHand hand) {
            interaction[0] = true;
         }

         public void onInteraction(InteractionHand hand, Vec3 pos) {
            interaction[0] = true;
         }

         public void onAttack() {
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
         } else if (mc.player.isUsingItem() && mc.player.getUseItem().getItem() != Items.SHIELD && config.isEatAndAttack()) {
            return false;
         } else {
            AuraModule aura = AuraModule.getInstance();
            float minimumCooldown = aura != null && aura.isCriticalBalanceEnabled() ? randomBetween(0.86F, 0.89F) : 0.82F;
            if (!this.clickScheduler.isCooldownComplete(config.isTpsSync(), (float)ticks, minimumCooldown)) {
               return false;
            } else if (this.isElytraTargetFlight()) {
               return true;
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
                     } else if (criticals.mode.is("Matrix") && !mc.player.onGround()) {
                        criticalState = true;
                     }
                  }

                  boolean smartCritical = aura != null && aura.isEnabled() && aura.getSmartCriticals().getValue();
                  if (smartCritical && !mc.options.keyJump.isDown()) {
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
      if (mc.player.isUsingItem() && !mc.player.getUseItem().getItem().equals(Items.SHIELD) && config.isEatAndAttack()) {
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
      return simulated.hasStatusEffect(MobEffects.BLINDNESS)
         || simulated.hasStatusEffect(MobEffects.LEVITATION)
         || PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.inflate(-0.001), Blocks.COBWEB)
         || simulated.isSubmergedInWater()
         || simulated.isInLava()
         || simulated.isClimbing()
         || !PlayerInteractionHelper.canChangeIntoPose(Pose.STANDING, simulated.pos)
         || simulated.player.getAbilities().flying;
   }

   private boolean shouldBypassCriticalChecks(PlayerSimulation simulated) {
      return this.hasMovementRestrictions(simulated);
   }

   private boolean isInsideTightSpace(PlayerSimulation simulated) {
      return !PlayerInteractionHelper.canChangeIntoPose(Pose.STANDING, simulated.pos)
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
      if (mc.level != null && simulated != null) {
         AABB expandedBox = simulated.boundingBox.inflate(0.22, 0.0, 0.22).deflate(1.0E-7);
         return !mc.level.noCollision(simulated.player, expandedBox);
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
      if (mc.level != null && simulated != null) {
         double footY = simulated.boundingBox.minY + 0.1;
         double bodyY = Math.min(simulated.boundingBox.maxY - 0.1, simulated.boundingBox.minY + 0.95);
         return this.isSolidCollisionBlock(BlockPos.containing(simulated.pos.x + offsetX, footY, simulated.pos.z + offsetZ))
            || this.isSolidCollisionBlock(BlockPos.containing(simulated.pos.x + offsetX, bodyY, simulated.pos.z + offsetZ));
      } else {
         return false;
      }
   }

   private boolean isSolidCollisionBlock(BlockPos pos) {
      if (mc.level == null) {
         return false;
      } else {
         BlockState state = mc.level.getBlockState(pos);
         return !state.isAir() && !state.getCollisionShape(mc.level, pos).isEmpty();
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
         return mc.player.getAttackStrengthScale(ticks) > requiredStrength;
      }
   }

   private boolean isHoldingMace() {
      ItemStack mainHand = mc.player.getMainHandItem();
      return mainHand.getItem().getDescriptionId().toLowerCase().contains("mace");
   }

   private boolean hasCriticalRestrictions(PlayerSimulation simulated) {
      return simulated.hasStatusEffect(MobEffects.BLINDNESS)
         || simulated.hasStatusEffect(MobEffects.LEVITATION)
         || simulated.isSubmergedInWater()
         || simulated.isInLava()
         || simulated.isClimbing()
         || simulated.isSwimming
         || simulated.isFallFlying
         || simulated.player.getAbilities().flying;
   }

   private boolean hasDebuffThatBreaksCrit(PlayerSimulation simulated) {
      return simulated.hasStatusEffect(MobEffects.BLINDNESS) || simulated.hasStatusEffect(MobEffects.LEVITATION);
   }

   private boolean isSmartCritJumpRequired() {
      AuraModule aura = AuraModule.getInstance();
      return aura != null && aura.isEnabled() && aura.getOnlyCriticals().getValue() && aura.getSmartCriticals().getValue();
   }

   private boolean isSmartCritJumpIntent() {
      if (mc.player == null) {
         return false;
      } else if (mc.options.keyJump.isDown()) {
         return true;
      } else {
         LivingEntityAccessor accessor = (LivingEntityAccessor)mc.player;
         if (accessor.daamky$isJumping()) {
            return true;
         } else {
            return accessor.daamky$getJumpingCooldown() > 0 && !mc.player.onGround() ? true : !mc.player.onGround() && mc.player.getDeltaMovement().y > 0.08;
         }
      }
   }

   private boolean isAirAttackMode(PlayerSimulation simulated) {
      return simulated.isFallFlying || simulated.player.getAbilities().flying || mc.player.isFallFlying();
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

   private boolean isElytraTargetFlight() {
      return mc.player != null
         && mc.player.isFallFlying()
         && ElytraTarget.getInstance() != null
         && ElytraTarget.getInstance().isEnabled();
   }

   private boolean isTargetInElytraReach(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (mc.player == null || config == null || config.getTarget() == null) {
         return false;
      }

      Vec3 eye = mc.player.getEyePosition();
      AABB box = config.getTarget().getBoundingBox();
      Vec3 closest = new Vec3(
         Math.max(box.minX, Math.min(eye.x, box.maxX)),
         Math.max(box.minY, Math.min(eye.y, box.maxY)),
         Math.max(box.minZ, Math.min(eye.z, box.maxZ))
      );
      double distance = eye.distanceTo(closest);
      if (distance > (double)config.getMaximumRange()) {
         return false;
      } else if (distance < 1.0E-6) {
         return true;
      } else {
         return RaycastAngle.rayTrace(closest.subtract(eye).normalize(), distance + 0.05, box, config.isIgnoreWalls());
      }
   }

   private boolean checkElytraMode(StrikerConstructor.AttackPerpetratorConfigurable config) {
      return AuraModule.target != null
         && AuraModule.target.isFallFlying()
         && mc.player.isFallFlying()
         && ElytraTarget.getInstance() != null
         && ElytraTarget.getInstance().isEnabled();
   }

   private boolean checkElytraRaycast(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Vec3 targetVelocity = config.getTarget().getDeltaMovement();
      float leadTicks = 0.0F;
      ElytraTarget elytraTarget = ElytraTarget.getInstance();
      if (ElytraTarget.shouldElytraTarget && elytraTarget != null) {
         leadTicks = elytraTarget.getForward();
      }

      Vec3 predictedPos = config.getTarget().position().add(targetVelocity.scale((double)leadTicks));
      AABB predictedBox = new AABB(
         predictedPos.x - (double)(config.getTarget().getBbWidth() / 2.0F),
         predictedPos.y,
         predictedPos.z - (double)(config.getTarget().getBbWidth() / 2.0F),
         predictedPos.x + (double)(config.getTarget().getBbWidth() / 2.0F),
         predictedPos.y + (double)config.getTarget().getBbHeight(),
         predictedPos.z + (double)(config.getTarget().getBbWidth() / 2.0F)
      );
      Vec3 eyePos = mc.player.getEyePosition();
      Vec3 lookVec = AngleConnection.INSTANCE.getRotation().toVector();
      return predictedBox.clip(eyePos, eyePos.add(lookVec.scale((double)config.getMaximumRange()))).isPresent();
   }

   private boolean isPlayerInCriticalState(PlayerSimulation simulated, int ticks) {
      float requiredFall = mc.player.getMainHandItem().getItem() == Items.MACE ? 1.2F : 0.0F;
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
      boolean inCobweb = PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.inflate(-0.001), Blocks.COBWEB);
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
      return mc.player != null && mc.player.hurtTime > 0 || simulated.hasStatusEffect(MobEffects.SLOWNESS);
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
