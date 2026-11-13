package daamky.ua.api.module.impl.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import daamky.ua.api.drag.impl.Notifications;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.inventory.InventoryFlowManager;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;
import daamky.ua.utils.player.BaritoneMovementHelper;

public final class AncientFarm extends Module {
   private static final int USE_STOP_TICKS = 3;
   private static final int USE_START_TIMEOUT_TICKS = 6;
   private static final int FIRE_RESISTANCE_REFRESH_TICKS = 400;
   private static final int BARITONE_STUCK_TICKS = 60;
   private static final int BARITONE_MAX_RETRIES = 4;
   private final NumberSetting searchStep = this.register(new NumberSetting("Tunnel Length", "Short tunnel length before placing TNT.", 8.0, 4.0, 16.0, 1.0));
   private final NumberSetting scanRadius = this.register(
      new NumberSetting("Scan Radius", "Ancient debris scan radius after an explosion.", 20.0, 8.0, 32.0, 1.0)
   );
   private final NumberSetting retreatDistance = this.register(new NumberSetting("Retreat", "Distance to fly away after igniting TNT.", 14.0, 8.0, 24.0, 1.0));
   private final NumberSetting explosionWait = this.register(
      new NumberSetting("Explosion Wait", "Seconds to wait before scanning the area.", 5.5, 4.5, 9.0, 0.5)
   );
   private final BooleanSetting autoEat = this.register(new BooleanSetting("Auto Eat", "Stops and eats when the player is hungry.", true));
   private AncientFarm.State state = AncientFarm.State.WAIT_FIRE_RESISTANCE;
   private AncientFarm.State resumeState = AncientFarm.State.WAIT_FIRE_RESISTANCE;
   private Direction routeDirection = Direction.NORTH;
   private BlockPos tntPos;
   private BlockPos tntStandPos;
   private BlockPos retreatTarget;
   private BlockPos debrisTarget;
   private BlockPos tunnelStart;
   private BlockPos scanCenter;
   private BlockPos debrisApproach;
   private Direction debrisHitFace = Direction.UP;
   private final List<BlockPos> debrisQueue = new ArrayList<>();
   private boolean breakingDebris;
   private boolean debrisDescending;
   private Slot debrisToolSlot;
   private boolean debrisToolSwapped;
   private int debrisToolUseSlot = -1;
   private int debrisToolPreviousSlot = -1;
   private long stateStartedAt;
   private long notificationAt;
   private long tntIgnitedAt;
   private long flyCommandAt;
   private boolean flyCommandSent;
   private int stableStopTicks;
   private boolean baritoneNavigating;
   private BlockPos baritoneTarget;
   private double lastBaritoneX;
   private double lastBaritoneY;
   private double lastBaritoneZ;
   private int baritoneStuckTicks;
   private int baritoneRetries;
   private Slot tntPlacementSlot;
   private boolean tntPlacementSwapped;
   private int tntPlacementUseSlot = -1;
   private int tntPlacementPreviousSlot = -1;
   private boolean tntPlacementSent;
   private int tntPlacementTicks;
   private int tntConfirmTicks;
   private Slot ignitionSlot;
   private boolean ignitionSwapped;
   private int ignitionUseSlot = -1;
   private int ignitionPreviousSlot = -1;
   private boolean ignitionSent;
   private int ignitionTicks;
   private int ignitionConfirmTicks;
   private boolean consuming;
   private boolean consumeStarted;
   private int consumeStopTicks;
   private int consumeStartTicks;
   private Slot pendingConsumeSlot;
   private Slot restoreConsumeSlot;

   public AncientFarm() {
      super("AncientFarm", "Digs short TNT tunnels and mines exposed ancient debris.", ModuleCategory.PLAYER);
   }

   @Override
   protected void onEnable() {
      AutoTool.setMainHandReserved(false);
      this.resetRuntime();
      this.transition(AncientFarm.State.WAIT_FIRE_RESISTANCE);
   }

   @Override
   protected void onDisable() {
      this.finishConsuming(true);
      this.resetDebrisMining();
      this.stopBaritone();
      this.resetTntPlacement(true);
      this.resetIgnition(true);
      AutoTool.setMainHandReserved(false);
      this.releaseKeys();
      this.resetRuntime();
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      if (this.consuming || this.requiresFullStop()) {
         event.inputNone();
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null && client.interactionManager != null && client.options != null) {
         if (client.world.getRegistryKey() != World.NETHER) {
            this.enforceFullStop();
            this.notifyOnce("AncientFarm работает только в Незере.", 3000L);
         } else if (this.consuming) {
            this.tickConsuming();
         } else {
            if (this.shouldDrinkFireResistance() && this.canPauseForPotion()) {
               Slot potion = this.findDrinkableFireResistance();
               if (potion != null) {
                  this.beginConsuming(potion);
                  return;
               }
            }

            if (this.autoEat.getValue() && client.player.getHungerManager().isNotFull() && this.canPauseForFood()) {
               Slot food = InventoryTask.getFoodMaxSaturationSlot();
               if (food != null) {
                  this.beginConsuming(food);
                  return;
               }

               this.notifyOnce("Нужна еда в инвентаре.", 3000L);
            }

            if (this.flyCommandSent && client.player.getAbilities().allowFlying && !this.requiresFullStop() && !this.baritoneNavigating) {
               this.ensureFlying();
            }

            if (this.baritoneNavigating && client.player.horizontalCollision) {
               client.options.jumpKey.setPressed(true);
               if (client.player.isOnGround()) {
                  client.player.jump();
               } else if (client.player.getAbilities().flying) {
                  Vec3d movement = client.player.getVelocity();
                  client.player.setVelocity(movement.x, Math.max(movement.y, 0.22), movement.z);
               }
            }

            switch (this.state) {
               case WAIT_FIRE_RESISTANCE:
                  this.tickStart();
                  break;
               case START_ROUTE:
                  this.startNextRoute();
                  break;
               case DIG_TUNNEL:
                  this.tickDigTunnel();
                  break;
               case POSITION_TNT:
                  this.tickPositionForTnt();
                  break;
               case FIND_TNT_POSITION:
                  this.tickFindTntPosition();
                  break;
               case PREPARE_TNT:
                  this.tickPrepareTnt();
                  break;
               case PLACE_TNT:
                  this.tickPlaceTnt();
                  break;
               case IGNITE_TNT:
                  this.tickIgniteTnt();
                  break;
               case RETREAT:
                  this.tickRetreat();
                  break;
               case WAIT_EXPLOSION:
                  this.tickWaitExplosion();
                  break;
               case SCAN_DEBRIS:
                  this.scanDebris();
                  break;
               case NAVIGATE_TO_DEBRIS:
                  this.tickNavigateToDebris();
                  break;
               case MINE_DEBRIS:
                  this.tickMineDebris();
            }
         }
      }
   }

   private void tickStart() {
      if (!this.flyCommandSent) {
         mc.player.networkHandler.sendChatCommand("fly");
         this.flyCommandSent = true;
         this.flyCommandAt = System.currentTimeMillis();
         this.routeDirection = mc.player.getHorizontalFacing();
         if (this.routeDirection.getAxis().isVertical()) {
            this.routeDirection = Direction.NORTH;
         }

         this.notifyOnce("Включаю /fly...", 1000L);
      } else if (!mc.player.getAbilities().allowFlying) {
         this.enforceFullStop();
         if (System.currentTimeMillis() - this.flyCommandAt >= 3000L) {
            mc.player.networkHandler.sendChatCommand("fly");
            this.flyCommandAt = System.currentTimeMillis();
            this.notifyOnce("Жду подтверждение /fly от сервера.", 1500L);
         }
      } else {
         this.ensureFlying();
         this.configureBaritone();
         this.notifyOnce("Полёт включён. Начинаю тоннель под TNT.", 1000L);
         this.transition(AncientFarm.State.START_ROUTE);
      }
   }

   private void startNextRoute() {
      this.releaseKeys();
      this.tunnelStart = mc.player.getBlockPos();
      this.startBaritoneTunnel(true);
      this.transition(AncientFarm.State.DIG_TUNNEL);
   }

   private void tickDigTunnel() {
      if (this.tunnelStart == null) {
         this.transition(AncientFarm.State.START_ROUTE);
      } else {
         int length = this.tunnelLength();
         if (this.tunnelProgress() >= length) {
            this.stopBaritone();
            this.releaseKeys();
            this.faceDirection(this.routeDirection);
            this.tntPos = mc.player.getBlockPos().toImmutable();
            this.tntStandPos = this.tntPos.offset(this.routeDirection.getOpposite(), 2);
            this.startBaritoneGoto(this.tntStandPos, true);
            this.transition(AncientFarm.State.POSITION_TNT);
         } else {
            boolean processStopped = this.elapsed() >= 900L && !BaritoneMovementHelper.isBaritoneActive(mc.player);
            if (this.baritoneNavigating && !processStopped) {
               if (this.baritoneHasStalled()) {
                  this.baritoneRetries++;
                  if (this.baritoneRetries > 4) {
                     this.stopBaritone();
                     this.transition(AncientFarm.State.START_ROUTE);
                  } else {
                     this.startBaritoneTunnel(false);
                  }
               }
            } else {
               this.startBaritoneTunnel(false);
            }
         }
      }
   }

   private void tickPositionForTnt() {
      if (this.tntStandPos != null && this.tntPos != null) {
         if (this.horizontalDistanceSqr(mc.player.getBlockPos(), this.tntStandPos) <= 1.5625) {
            this.stopBaritone();
            this.transition(AncientFarm.State.FIND_TNT_POSITION);
         } else {
            boolean processStopped = this.elapsed() >= 900L && !BaritoneMovementHelper.isBaritoneActive(mc.player);
            if (this.baritoneNavigating && !processStopped) {
               if (this.baritoneHasStalled()) {
                  this.baritoneRetries++;
                  if (this.baritoneRetries > 4) {
                     this.stopBaritone();
                     this.transition(AncientFarm.State.START_ROUTE);
                  } else {
                     this.startBaritoneGoto(this.tntStandPos, false);
                  }
               }
            } else {
               this.startBaritoneGoto(this.tntStandPos, false);
            }
         }
      } else {
         this.stopBaritone();
         this.transition(AncientFarm.State.START_ROUTE);
      }
   }

   private void tickFindTntPosition() {
      this.enforceFullStop();
      if (this.tntPos != null
         && mc.world.getBlockState(this.tntPos).isAir()
         && mc.world.getBlockState(this.tntPos.down()).isSideSolidFullSquare(mc.world, this.tntPos.down(), Direction.UP)) {
         this.stableStopTicks = 0;
         this.transition(AncientFarm.State.PREPARE_TNT);
      } else {
         this.transition(AncientFarm.State.START_ROUTE);
      }
   }

   private void tickPrepareTnt() {
      this.enforceFullStop();
      this.stableStopTicks++;
      if (this.stableStopTicks >= 4) {
         this.resetTntPlacement(false);
         this.transition(AncientFarm.State.PLACE_TNT);
      }
   }

   private void tickPlaceTnt() {
      this.enforceFullStop();
      if (this.tntPos == null) {
         this.resetTntPlacement(true);
         this.transition(AncientFarm.State.FIND_TNT_POSITION);
      } else {
         BlockPos support = this.tntPos.down();
         Vec3d hitVec = Vec3d.ofCenter(support).add(0.0, 0.5, 0.0);
         this.lookAt(hitVec);
         if (this.tntPlacementSlot == null) {
            Slot tntSlot = InventoryTask.getSlot(Items.TNT);
            if (tntSlot == null) {
               this.notifyOnce("Закончился динамит.", 3000L);
            } else {
               this.tntPlacementSlot = tntSlot;
               this.tntPlacementSwapped = !mc.player.getOffHandStack().isOf(Items.TNT);
               if (this.tntPlacementSwapped) {
                  InventoryTask.swapHand(tntSlot, Hand.OFF_HAND, false, true);
               }

               this.tntPlacementTicks = 0;
               this.tntConfirmTicks = 0;
            }
         } else {
            this.tntPlacementTicks++;
            if (!mc.player.getOffHandStack().isOf(Items.TNT)) {
               if (this.tntPlacementTicks >= 20) {
                  this.resetTntPlacement(true);
                  this.notifyOnce("Сервер не подтвердил свап TNT. Повторяю.", 1500L);
               }
            } else if (!this.tntPlacementSent) {
               if (!mc.world.getBlockState(this.tntPos).isAir() || !mc.world.getBlockState(support).isSideSolidFullSquare(mc.world, support, Direction.UP)) {
                  this.resetTntPlacement(true);
                  this.transition(AncientFarm.State.FIND_TNT_POSITION);
               } else if (this.tntPlacementTicks >= 4) {
                  BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, support, false);
                  ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, hit);
                  if (!result.isAccepted()) {
                     this.resetTntPlacement(true);
                     this.transition(AncientFarm.State.FIND_TNT_POSITION);
                  } else {
                     mc.player.swingHand(Hand.OFF_HAND);
                     this.tntPlacementSent = true;
                     this.tntPlacementTicks = 0;
                  }
               }
            } else {
               if (mc.world.getBlockState(this.tntPos).isOf(Blocks.TNT)) {
                  this.tntConfirmTicks++;
               } else {
                  this.tntConfirmTicks = 0;
               }

               if (this.tntConfirmTicks >= 8) {
                  this.resetTntPlacement(true);
                  this.stableStopTicks = 0;
                  this.resetIgnition(false);
                  this.transition(AncientFarm.State.IGNITE_TNT);
               } else {
                  if (this.tntPlacementTicks >= 20) {
                     this.resetTntPlacement(true);
                     this.transition(AncientFarm.State.FIND_TNT_POSITION);
                  }
               }
            }
         }
      }
   }

   private void tickIgniteTnt() {
      this.enforceFullStop();
      if (this.tntPos != null && mc.world.getBlockState(this.tntPos).isOf(Blocks.TNT)) {
         this.ignitionConfirmTicks = 0;
         this.lookAt(Vec3d.ofCenter(this.tntPos));
         if (this.ignitionSlot == null) {
            Slot foundIgnition = this.findIgnitionItem();
            if (foundIgnition == null) {
               this.notifyOnce("Нужны огниво или огненный заряд.", 3000L);
            } else {
               this.ignitionSlot = foundIgnition;
               ItemStack offhand = mc.player.getOffHandStack();
               this.ignitionSwapped = !offhand.isOf(Items.FLINT_AND_STEEL) && !offhand.isOf(Items.FIRE_CHARGE);
               if (this.ignitionSwapped) {
                  InventoryTask.swapHand(foundIgnition, Hand.OFF_HAND, false, true);
               }

               this.ignitionTicks = 0;
            }
         } else {
            this.ignitionTicks++;
            ItemStack offhand = mc.player.getOffHandStack();
            if (!offhand.isOf(Items.FLINT_AND_STEEL) && !offhand.isOf(Items.FIRE_CHARGE)) {
               if (this.ignitionTicks >= 20) {
                  this.resetIgnition(true);
               }
            } else if (!this.ignitionSent && this.ignitionTicks >= 4) {
               BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(this.tntPos), Direction.UP, this.tntPos, false);
               ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, hit);
               if (result.isAccepted()) {
                  mc.player.swingHand(Hand.OFF_HAND);
                  this.ignitionSent = true;
                  this.ignitionTicks = 0;
               }
            }
         }
      } else {
         if (this.ignitionSent) {
            this.ignitionConfirmTicks++;
            if (this.ignitionConfirmTicks >= 5) {
               this.resetIgnition(true);
               this.scanCenter = this.tntPos.toImmutable();
               this.tntIgnitedAt = System.currentTimeMillis();
               this.buildRetreatTarget();
               this.stopBaritone();
               this.ensureFlying();
               this.releaseKeys();
               this.transition(AncientFarm.State.RETREAT);
            }
         } else {
            this.resetIgnition(true);
            this.transition(AncientFarm.State.FIND_TNT_POSITION);
         }
      }
   }

   private void tickRetreat() {
      if (this.retreatTarget != null && !(this.horizontalDistanceSqr(mc.player.getBlockPos(), this.retreatTarget) <= 9.0) && this.elapsed() < 4200L) {
         this.flyEscapeTo(this.retreatTarget);
      } else {
         this.hoverFlight();
         this.releaseKeys();
         this.transition(AncientFarm.State.WAIT_EXPLOSION);
      }
   }

   private void tickWaitExplosion() {
      this.enforceFullStop();
      long totalWait = (long)(this.explosionWait.getValue() * 1000.0);
      if (System.currentTimeMillis() - this.tntIgnitedAt >= totalWait) {
         this.transition(AncientFarm.State.SCAN_DEBRIS);
      }
   }

   private void scanDebris() {
      this.debrisQueue.clear();
      BlockPos center = this.scanCenter != null ? this.scanCenter : mc.player.getBlockPos();
      int radius = Math.round(this.scanRadius.getFloat());
      int verticalRadius = Math.min(radius, 16);
      int radiusSqr = radius * radius;

      for (int x = -radius; x <= radius; x++) {
         for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int z = -radius; z <= radius; z++) {
               if (x * x + y * y + z * z <= radiusSqr) {
                  BlockPos pos = center.add(x, y, z);
                  if (mc.world.isChunkLoaded(pos) && mc.world.getBlockState(pos).isOf(Blocks.ANCIENT_DEBRIS) && this.canStandOnDebris(pos)) {
                     this.debrisQueue.add(pos.toImmutable());
                  }
               }
            }
         }
      }

      this.debrisQueue.sort(Comparator.comparingDouble(posx -> mc.player.squaredDistanceTo(Vec3d.ofCenter(posx))));
      if (this.debrisQueue.isEmpty()) {
         this.stopBaritone();
         this.ensureFlying();
         this.debrisTarget = null;
         this.scanCenter = null;
         this.transition(AncientFarm.State.START_ROUTE);
      } else {
         this.selectNextDebris();
      }
   }

   private void selectNextDebris() {
      this.resetDebrisMining();
      this.debrisQueue.removeIf(pos -> !mc.world.getBlockState(pos).isOf(Blocks.ANCIENT_DEBRIS) || !this.canStandOnDebris(pos));
      if (this.debrisQueue.isEmpty()) {
         this.transition(AncientFarm.State.SCAN_DEBRIS);
      } else {
         this.debrisTarget = this.debrisQueue.removeFirst();
         this.debrisApproach = this.findDebrisApproach(this.debrisTarget);
         if (this.debrisApproach == null) {
            this.debrisTarget = null;
            this.selectNextDebris();
         } else {
            this.stopBaritone();
            this.ensureFlying();
            this.debrisDescending = false;
            this.transition(AncientFarm.State.NAVIGATE_TO_DEBRIS);
         }
      }
   }

   private void tickNavigateToDebris() {
      if (this.debrisTarget != null && mc.world.getBlockState(this.debrisTarget).isOf(Blocks.ANCIENT_DEBRIS) && this.canStandOnDebris(this.debrisTarget)) {
         if (this.debrisApproach == null || !mc.world.getBlockState(this.debrisApproach).isAir()) {
            this.debrisApproach = this.findDebrisApproach(this.debrisTarget);
            if (this.debrisApproach == null) {
               this.selectNextDebris();
               return;
            }
         }

         if (!this.debrisDescending) {
            Vec3d destination = new Vec3d(
               (double)this.debrisTarget.getX() + 0.5, (double)this.debrisTarget.getY() + 3.2, (double)this.debrisTarget.getZ() + 0.5
            );
            double dx = mc.player.getX() - destination.x;
            double dz = mc.player.getZ() - destination.z;
            if (!(dx * dx + dz * dz <= 0.2025) || !(Math.abs(mc.player.getY() - destination.y) <= 0.8)) {
               this.flyTo(destination);
               return;
            }

            this.debrisDescending = true;
         }

         Vec3d destination = new Vec3d((double)this.debrisTarget.getX() + 0.5, (double)this.debrisTarget.getY() + 1.05, (double)this.debrisTarget.getZ() + 0.5);
         if (mc.player.getEntityPos().squaredDistanceTo(destination) <= 1.3224999999999998) {
            this.hoverFlight();
            this.releaseKeys();
            this.disableFlyingForBaritone();
            this.transition(AncientFarm.State.MINE_DEBRIS);
         } else {
            this.flyTo(destination);
         }
      } else {
         this.releaseKeys();
         this.selectNextDebris();
      }
   }

   private void tickMineDebris() {
      if (this.debrisTarget == null || !mc.world.getBlockState(this.debrisTarget).isOf(Blocks.ANCIENT_DEBRIS)) {
         this.ensureFlying();
         mc.player.setVelocity(Vec3d.ZERO);
         this.resetDebrisMining();
         this.selectNextDebris();
      } else if (!this.canStandOnDebris(this.debrisTarget)) {
         this.ensureFlying();
         this.resetDebrisMining();
         this.selectNextDebris();
      } else if (mc.player.squaredDistanceTo(Vec3d.ofCenter(this.debrisTarget)) > 20.25) {
         this.resetDebrisMining();
         this.transition(AncientFarm.State.NAVIGATE_TO_DEBRIS);
      } else {
         this.releaseKeys();
         if (mc.player.getAbilities().flying) {
            this.disableFlyingForBaritone();
         }

         if (!mc.player.isOnGround()) {
            Vec3d movement = mc.player.getVelocity();
            mc.player.setVelocity(0.0, movement.y, 0.0);
         } else if (this.prepareDebrisTool()) {
            this.lookAt(Vec3d.ofCenter(this.debrisTarget));
            if (!this.breakingDebris) {
               mc.interactionManager.attackBlock(this.debrisTarget, this.debrisHitFace);
               this.breakingDebris = true;
            } else {
               mc.interactionManager.updateBlockBreakingProgress(this.debrisTarget, this.debrisHitFace);
            }

            mc.options.attackKey.setPressed(true);
            mc.player.swingHand(Hand.MAIN_HAND);
         }
      }
   }

   private BlockPos findDebrisApproach(BlockPos target) {
      if (!this.canStandOnDebris(target)) {
         return null;
      } else {
         this.debrisHitFace = Direction.UP;
         return target.up().toImmutable();
      }
   }

   private boolean canStandOnDebris(BlockPos pos) {
      return mc.world.getBlockState(pos).isOf(Blocks.ANCIENT_DEBRIS)
         && mc.world.getBlockState(pos.up()).isAir()
         && mc.world.getBlockState(pos.up(2)).isAir()
         && mc.world.getBlockState(pos.up(3)).isAir();
   }

   private boolean prepareDebrisTool() {
      AutoTool.setMainHandReserved(true);
      if (mc.player.getMainHandStack().isIn(ItemTags.PICKAXES)) {
         return true;
      } else if (this.debrisToolSlot == null) {
         Slot pickaxe = InventoryTask.slots()
            .filter(slot -> slot.getStack().isIn(ItemTags.PICKAXES))
            .max(Comparator.comparingDouble(slot -> (double)slot.getStack().getMiningSpeedMultiplier(mc.world.getBlockState(this.debrisTarget))))
            .orElse(null);
         if (pickaxe == null) {
            this.notifyOnce("Нужна кирка в инвентаре.", 3000L);
            return false;
         } else {
            this.debrisToolSlot = pickaxe;
            this.debrisToolPreviousSlot = mc.player.getInventory().getSelectedSlot();
            int menuSlot = InventoryTask.getMenuSlotId(pickaxe);
            if (menuSlot >= 36 && menuSlot <= 44) {
               this.debrisToolUseSlot = menuSlot - 36;
               InventoryTask.switchTo(this.debrisToolUseSlot);
            } else {
               this.debrisToolUseSlot = this.debrisToolPreviousSlot;
               this.debrisToolSwapped = true;
               InventoryTask.swapHand(pickaxe, Hand.MAIN_HAND, false, true);
            }

            return false;
         }
      } else {
         if (this.debrisToolUseSlot >= 0 && mc.player.getInventory().getSelectedSlot() != this.debrisToolUseSlot) {
            InventoryTask.switchTo(this.debrisToolUseSlot);
         }

         return mc.player.getMainHandStack().isIn(ItemTags.PICKAXES);
      }
   }

   private void resetDebrisMining() {
      if (this.breakingDebris && mc.interactionManager != null) {
         mc.interactionManager.cancelBlockBreaking();
      }

      this.breakingDebris = false;
      this.debrisDescending = false;
      if (mc.player != null && this.debrisToolUseSlot >= 0) {
         InventoryTask.switchTo(this.debrisToolUseSlot);
      }

      if (mc.player != null && this.debrisToolSwapped && this.debrisToolSlot != null) {
         InventoryTask.swapHand(this.debrisToolSlot, Hand.MAIN_HAND, false, true);
      }

      if (mc.player != null && this.debrisToolPreviousSlot >= 0) {
         InventoryTask.switchTo(this.debrisToolPreviousSlot);
      }

      this.debrisToolSlot = null;
      this.debrisToolSwapped = false;
      this.debrisToolUseSlot = -1;
      this.debrisToolPreviousSlot = -1;
      AutoTool.setMainHandReserved(false);
      if (mc.options != null) {
         mc.options.attackKey.setPressed(false);
      }
   }

   private boolean isExposedDebris(BlockPos pos) {
      for (Direction direction : Direction.values()) {
         BlockPos neighbor = pos.offset(direction);
         if (mc.world.getBlockState(neighbor).isAir()) {
            return true;
         }
      }

      return false;
   }

   private void ensureFlying() {
      if (mc.player != null && mc.player.getAbilities().allowFlying) {
         if (!mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = true;
            mc.player.networkHandler.sendPacket(new UpdatePlayerAbilitiesC2SPacket(mc.player.getAbilities()));
         }
      }
   }

   private void disableFlyingForBaritone() {
      if (mc.player != null && mc.player.getAbilities().flying) {
         mc.player.getAbilities().flying = false;
         mc.player.networkHandler.sendPacket(new UpdatePlayerAbilitiesC2SPacket(mc.player.getAbilities()));
      }
   }

   private void hoverFlight() {
      if (mc.player != null) {
         this.ensureFlying();
         mc.player.setSprinting(false);
         mc.player.setVelocity(Vec3d.ZERO);
      }
   }

   private void flyEscapeTo(BlockPos target) {
      if (mc.player != null && target != null) {
         this.releaseKeys();
         this.ensureFlying();
         Vec3d delta = Vec3d.ofCenter(target).subtract(mc.player.getEntityPos());
         double horizontal = Math.hypot(delta.x, delta.z);
         if (horizontal < 0.05) {
            this.hoverFlight();
         } else {
            double speed = 0.55;
            double motionY = mc.player.horizontalCollision ? 0.3 : 0.04;
            mc.player.setYaw((float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F);
            mc.player.setPitch(0.0F);
            mc.player.setSprinting(false);
            mc.options.forwardKey.setPressed(true);
            mc.options.jumpKey.setPressed(mc.player.horizontalCollision);
            mc.player.setVelocity(delta.x / horizontal * speed, motionY, delta.z / horizontal * speed);
         }
      }
   }

   private void flyTo(Vec3d target) {
      if (mc.player != null && target != null) {
         this.releaseKeys();
         this.ensureFlying();
         Vec3d delta = target.subtract(mc.player.getEntityPos());
         double distance = delta.length();
         if (distance < 0.05) {
            this.hoverFlight();
         } else {
            double speed = Math.min(0.48, distance * 0.35);
            double motionY = delta.y / distance * speed;
            if (mc.player.horizontalCollision) {
               motionY = Math.max(motionY, 0.32);
            }

            double horizontal = Math.hypot(delta.x, delta.z);
            if (horizontal > 0.001) {
               mc.player.setYaw((float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F);
            }

            mc.player.setPitch(0.0F);
            mc.player.setSprinting(false);
            mc.player.setVelocity(delta.x / distance * speed, motionY, delta.z / distance * speed);
         }
      }
   }

   private void configureBaritone() {
      if (mc.player != null) {
         mc.player.networkHandler.sendChatMessage("#set allowBreak true");
         mc.player.networkHandler.sendChatMessage("#set allowPlace false");
         mc.player.networkHandler.sendChatMessage("#set allowSprint true");
         mc.player.networkHandler.sendChatMessage("#set allowParkour true");
         mc.player.networkHandler.sendChatMessage("#set allowParkourAscend true");
         mc.player.networkHandler.sendChatMessage("#set allowDiagonalAscend true");
         mc.player.networkHandler.sendChatMessage("#set allowDiagonalDescend true");
         mc.player.networkHandler.sendChatMessage("#set assumeStep false");
         mc.player.networkHandler.sendChatMessage("#set autoTool true");
         mc.player.networkHandler.sendChatMessage("#set walkWhileBreaking true");
         mc.player.networkHandler.sendChatMessage("#set blockBreakAdditionalPenalty 0");
      }
   }

   private void startBaritoneTunnel(boolean resetRetries) {
      if (mc.player != null) {
         this.stopBaritone();
         this.releaseKeys();
         this.disableFlyingForBaritone();
         this.faceDirection(this.routeDirection);
         if (resetRetries) {
            this.baritoneRetries = 0;
         }

         this.baritoneNavigating = true;
         this.baritoneTarget = null;
         this.baritoneStuckTicks = 0;
         this.rememberBaritonePosition();
         mc.player.networkHandler.sendChatMessage("#tunnel");
      }
   }

   private void startBaritoneGoto(BlockPos target, boolean resetRetries) {
      if (target != null && mc.player != null) {
         this.stopBaritone();
         this.releaseKeys();
         this.ensureFlying();
         if (resetRetries) {
            this.baritoneRetries = 0;
         }

         this.baritoneNavigating = true;
         this.baritoneTarget = target.toImmutable();
         this.baritoneStuckTicks = 0;
         this.rememberBaritonePosition();
         mc.player.networkHandler.sendChatMessage("#goto " + target.getX() + " " + target.getY() + " " + target.getZ());
      }
   }

   private void updateBaritoneWatchdog() {
      if (this.baritoneHasStalled()) {
         this.baritoneRetries++;
         if (this.baritoneRetries > 4) {
            this.stopBaritone();
            this.notifyOnce("Baritone не смог дойти до обломка, пропускаю цель.", 2000L);
            this.debrisTarget = null;
            if (this.debrisQueue.isEmpty()) {
               this.ensureFlying();
               this.scanCenter = null;
               this.transition(AncientFarm.State.START_ROUTE);
            } else {
               this.selectNextDebris();
            }
         } else {
            this.notifyOnce("Baritone застрял, перестраиваю путь (" + this.baritoneRetries + "/4).", 1000L);
            this.selectNextDebris();
         }
      }
   }

   private boolean baritoneHasStalled() {
      double dx = mc.player.getX() - this.lastBaritoneX;
      double dy = mc.player.getY() - this.lastBaritoneY;
      double dz = mc.player.getZ() - this.lastBaritoneZ;
      if (dx * dx + dy * dy + dz * dz < 9.0E-4) {
         this.baritoneStuckTicks++;
      } else {
         this.baritoneStuckTicks = 0;
      }

      this.rememberBaritonePosition();
      if (this.baritoneStuckTicks < 60) {
         return false;
      } else {
         this.baritoneStuckTicks = 0;
         return true;
      }
   }

   private void rememberBaritonePosition() {
      this.lastBaritoneX = mc.player.getX();
      this.lastBaritoneY = mc.player.getY();
      this.lastBaritoneZ = mc.player.getZ();
   }

   private void stopBaritone() {
      if (this.baritoneNavigating && mc.player != null) {
         mc.player.networkHandler.sendChatMessage("#stop");
         mc.player.networkHandler.sendChatMessage("#cancel");
      }

      this.baritoneNavigating = false;
      this.baritoneTarget = null;
      this.baritoneStuckTicks = 0;
   }

   private int tunnelLength() {
      return Math.max(4, Math.round(this.searchStep.getFloat()));
   }

   private int tunnelProgress() {
      if (mc.player != null && this.tunnelStart != null) {
         BlockPos current = mc.player.getBlockPos();
         int dx = current.getX() - this.tunnelStart.getX();
         int dz = current.getZ() - this.tunnelStart.getZ();
         return (int)Math.floor(Math.sqrt((double)dx * (double)dx + (double)dz * (double)dz));
      } else {
         return 0;
      }
   }

   private void faceDirection(Direction direction) {
      if (mc.player != null && direction != null) {
         mc.player.setYaw(direction.getPositiveHorizontalDegrees());
         mc.player.setPitch(0.0F);
      }
   }

   private Direction directionToward(BlockPos target) {
      double dx = (double)target.getX() + 0.5 - mc.player.getX();
      double dz = (double)target.getZ() + 0.5 - mc.player.getZ();
      if (Math.abs(dx) >= Math.abs(dz)) {
         return dx >= 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         return dz >= 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private boolean shouldDrinkFireResistance() {
      if (mc.player == null) {
         return false;
      } else {
         StatusEffectInstance effect = mc.player.getStatusEffect(StatusEffects.FIRE_RESISTANCE);
         return effect == null || effect.getDuration() <= 400;
      }
   }

   private Slot findDrinkableFireResistance() {
      return InventoryTask.slots()
         .filter(slot -> slot.getStack().isOf(Items.POTION))
         .filter(slot -> this.hasPotionEffect(slot.getStack()))
         .findFirst()
         .orElse(null);
   }

   private boolean hasPotionEffect(ItemStack stack) {
      PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
      return contents != null
         && StreamSupport.<StatusEffectInstance>stream(contents.getEffects().spliterator(), false)
            .anyMatch(effect -> effect.getEffectType().equals(StatusEffects.FIRE_RESISTANCE));
   }

   private void beginConsuming(Slot slot) {
      if (slot != null && InventoryFlowManager.isIdle()) {
         this.resumeState = this.state;
         this.pendingConsumeSlot = slot;
         this.consuming = true;
         this.consumeStarted = false;
         this.consumeStopTicks = 0;
         this.consumeStartTicks = 0;
         this.stopBaritone();
         this.enforceFullStop();
      }
   }

   private void tickConsuming() {
      this.enforceFullStop();
      if (this.pendingConsumeSlot != null) {
         this.consumeStopTicks++;
         if (this.consumeStopTicks >= 3) {
            ItemStack stack = this.pendingConsumeSlot.getStack();
            if (!stack.isEmpty() && !mc.player.getItemCooldownManager().isCoolingDown(stack)) {
               if (!ItemStack.areItemsAndComponentsEqual(mc.player.getOffHandStack(), stack)) {
                  InventoryTask.swapHand(this.pendingConsumeSlot, Hand.OFF_HAND, false, true);
                  this.restoreConsumeSlot = this.pendingConsumeSlot;
               }

               this.pendingConsumeSlot = null;
               mc.options.useKey.setPressed(true);
               PlayerInteractionHelper.interactItem(Hand.OFF_HAND);
            } else {
               this.finishConsuming(false);
            }
         }
      } else {
         this.consumeStartTicks++;
         if (mc.player.isUsingItem()) {
            this.consumeStarted = true;
            mc.options.useKey.setPressed(true);
         } else {
            if (!this.consumeStarted && this.consumeStartTicks < 6) {
               mc.options.useKey.setPressed(true);
            } else {
               this.finishConsuming(true);
            }
         }
      }
   }

   private void finishConsuming(boolean restoreItem) {
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

      if (restoreItem && this.restoreConsumeSlot != null && mc.player != null) {
         InventoryTask.swapHand(this.restoreConsumeSlot, Hand.OFF_HAND, false, true);
      }

      this.consuming = false;
      this.consumeStarted = false;
      this.consumeStopTicks = 0;
      this.consumeStartTicks = 0;
      this.pendingConsumeSlot = null;
      this.restoreConsumeSlot = null;
      this.state = this.resumeState;
      this.stateStartedAt = System.currentTimeMillis();
      this.resumeRouteAfterUse();
      InventoryFlowManager.updateMoveKeys();
   }

   private void resumeRouteAfterUse() {
      this.releaseKeys();
      if ((this.state == AncientFarm.State.NAVIGATE_TO_DEBRIS || this.state == AncientFarm.State.MINE_DEBRIS) && this.debrisTarget != null) {
         this.ensureFlying();
         if (this.state == AncientFarm.State.MINE_DEBRIS) {
            this.resetDebrisMining();
            this.transition(AncientFarm.State.NAVIGATE_TO_DEBRIS);
         }
      }
   }

   private Slot findIgnitionItem() {
      Slot flint = InventoryTask.getSlot(Items.FLINT_AND_STEEL);
      return flint != null ? flint : InventoryTask.getSlot(Items.FIRE_CHARGE);
   }

   private void buildRetreatTarget() {
      if (this.tntPos == null) {
         this.retreatTarget = null;
      } else {
         Direction away = this.routeDirection.getOpposite();
         int distance = Math.round(this.retreatDistance.getFloat());
         this.retreatTarget = this.tntPos.offset(away, distance);
      }
   }

   private void resetTntPlacement(boolean restoreItem) {
      if (restoreItem && this.tntPlacementSwapped && this.tntPlacementSlot != null && mc.player != null) {
         InventoryTask.swapHand(this.tntPlacementSlot, Hand.OFF_HAND, false, true);
      }

      this.tntPlacementSlot = null;
      this.tntPlacementSwapped = false;
      this.tntPlacementUseSlot = -1;
      this.tntPlacementPreviousSlot = -1;
      this.tntPlacementSent = false;
      this.tntPlacementTicks = 0;
      this.tntConfirmTicks = 0;
   }

   private void resetIgnition(boolean restoreItem) {
      if (restoreItem && this.ignitionSwapped && this.ignitionSlot != null && mc.player != null) {
         InventoryTask.swapHand(this.ignitionSlot, Hand.OFF_HAND, false, true);
      }

      this.ignitionSlot = null;
      this.ignitionSwapped = false;
      this.ignitionUseSlot = -1;
      this.ignitionPreviousSlot = -1;
      this.ignitionSent = false;
      this.ignitionTicks = 0;
      this.ignitionConfirmTicks = 0;
   }

   private void lookAt(Vec3d target) {
      Vec3d eyes = mc.player.getEyePos();
      double dx = target.x - eyes.x;
      double dy = target.y - eyes.y;
      double dz = target.z - eyes.z;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      mc.player.setYaw((float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
      mc.player.setPitch((float)(-Math.toDegrees(Math.atan2(dy, horizontal))));
   }

   private void transition(AncientFarm.State next) {
      if (next == AncientFarm.State.PREPARE_TNT) {
         AutoTool.setMainHandReserved(true);
      } else if (next == AncientFarm.State.RETREAT || next == AncientFarm.State.START_ROUTE) {
         AutoTool.setMainHandReserved(false);
      }

      this.state = next;
      this.stateStartedAt = System.currentTimeMillis();
   }

   private long elapsed() {
      return System.currentTimeMillis() - this.stateStartedAt;
   }

   private boolean canPauseForFood() {
      return this.state != AncientFarm.State.FIND_TNT_POSITION
         && this.state != AncientFarm.State.PREPARE_TNT
         && this.state != AncientFarm.State.PLACE_TNT
         && this.state != AncientFarm.State.IGNITE_TNT
         && this.state != AncientFarm.State.RETREAT
         && this.state != AncientFarm.State.WAIT_EXPLOSION;
   }

   private boolean canPauseForPotion() {
      return this.state != AncientFarm.State.FIND_TNT_POSITION
         && this.state != AncientFarm.State.PREPARE_TNT
         && this.state != AncientFarm.State.PLACE_TNT
         && this.state != AncientFarm.State.IGNITE_TNT
         && this.state != AncientFarm.State.RETREAT
         && this.state != AncientFarm.State.WAIT_EXPLOSION;
   }

   private boolean requiresFullStop() {
      return this.state == AncientFarm.State.FIND_TNT_POSITION
         || this.state == AncientFarm.State.PREPARE_TNT
         || this.state == AncientFarm.State.PLACE_TNT
         || this.state == AncientFarm.State.IGNITE_TNT
         || this.state == AncientFarm.State.WAIT_EXPLOSION;
   }

   private void enforceFullStop() {
      this.releaseKeys();
      if (mc.player != null) {
         mc.player.setSprinting(false);
         double vertical = mc.player.getAbilities().flying ? 0.0 : mc.player.getVelocity().y;
         mc.player.setVelocity(0.0, vertical, 0.0);
      }
   }

   private void releaseKeys() {
      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.sneakKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         mc.options.attackKey.setPressed(false);
         mc.options.useKey.setPressed(false);
      }
   }

   private void notifyOnce(String text, long delayMs) {
      long now = System.currentTimeMillis();
      if (now - this.notificationAt >= delayMs) {
         this.notificationAt = now;
         Notifications.push("AncientFarm", text, Math.min(3000L, delayMs));
      }
   }

   private double horizontalDistanceSqr(BlockPos first, BlockPos second) {
      double dx = (double)(first.getX() - second.getX());
      double dz = (double)(first.getZ() - second.getZ());
      return dx * dx + dz * dz;
   }

   private void resetRuntime() {
      this.state = AncientFarm.State.WAIT_FIRE_RESISTANCE;
      this.resumeState = AncientFarm.State.WAIT_FIRE_RESISTANCE;
      this.tntPos = null;
      this.tntStandPos = null;
      this.retreatTarget = null;
      this.debrisTarget = null;
      this.debrisApproach = null;
      this.debrisHitFace = Direction.UP;
      this.tunnelStart = null;
      this.scanCenter = null;
      this.debrisQueue.clear();
      this.breakingDebris = false;
      this.debrisDescending = false;
      this.debrisToolSlot = null;
      this.debrisToolSwapped = false;
      this.debrisToolUseSlot = -1;
      this.debrisToolPreviousSlot = -1;
      this.stateStartedAt = System.currentTimeMillis();
      this.notificationAt = 0L;
      this.tntIgnitedAt = 0L;
      this.flyCommandSent = false;
      this.flyCommandAt = 0L;
      this.baritoneNavigating = false;
      this.baritoneTarget = null;
      this.lastBaritoneX = 0.0;
      this.lastBaritoneY = 0.0;
      this.lastBaritoneZ = 0.0;
      this.baritoneStuckTicks = 0;
      this.baritoneRetries = 0;
      this.stableStopTicks = 0;
      this.tntPlacementSlot = null;
      this.tntPlacementSwapped = false;
      this.tntPlacementUseSlot = -1;
      this.tntPlacementPreviousSlot = -1;
      this.tntPlacementSent = false;
      this.tntPlacementTicks = 0;
      this.tntConfirmTicks = 0;
      this.ignitionSlot = null;
      this.ignitionSwapped = false;
      this.ignitionUseSlot = -1;
      this.ignitionPreviousSlot = -1;
      this.ignitionSent = false;
      this.ignitionTicks = 0;
      this.ignitionConfirmTicks = 0;
      this.consuming = false;
      this.consumeStarted = false;
      this.consumeStopTicks = 0;
      this.consumeStartTicks = 0;
      this.pendingConsumeSlot = null;
      this.restoreConsumeSlot = null;
   }

   private static enum State {
      WAIT_FIRE_RESISTANCE,
      START_ROUTE,
      DIG_TUNNEL,
      POSITION_TNT,
      FIND_TNT_POSITION,
      PREPARE_TNT,
      PLACE_TNT,
      IGNITE_TNT,
      RETREAT,
      WAIT_EXPLOSION,
      SCAN_DEBRIS,
      NAVIGATE_TO_DEBRIS,
      MINE_DEBRIS;

      private State() {
      }
   }
}
