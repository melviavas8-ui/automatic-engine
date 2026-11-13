package daamky.ua.api.module.impl.movement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConfig;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.impl.LinearConstructor;
import daamky.ua.api.module.impl.combat.aura.target.RaycastAngle;
import daamky.ua.api.module.impl.combat.aura.util.TaskPriority;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.inventory.lookup.InventoryUtils;

public final class Scaffold extends Module {
   private static Scaffold instance;
   private static final double PLACE_RANGE = 4.5;
   private final NumberSetting extend = this.register(new NumberSetting("Extend", "How many blocks to place forward.", 1.0, 0.0, 5.0, 1.0));
   private final BooleanSetting tower = this.register(new BooleanSetting("Tower", "Jump tower.", false));
   private final BooleanSetting autoSwap = this.register(new BooleanSetting("Auto Swap", "Automatically select blocks.", true));
   private final BooleanSetting safeWalk = this.register(new BooleanSetting("Safe Walk", "Hold sneak at edges.", true));
   private final BooleanSetting downward = this.register(new BooleanSetting("Downward", "Place lower while sneaking.", false));
   private final Random random = new Random();
   private Scaffold.BlockPlacement currentPlacement;
   private int previousSlot = -1;
   private int placeCooldownTicks;
   private boolean rotationInitialized;
   private float accumulatedYaw;
   private float accumulatedPitch;
   private boolean wasSneak;
   private boolean safeWalkActive;
   private float lastPlaceYaw;
   private float lastPlacePitch;
   private float lastDeltaYaw;
   private int placeCounter;
   private long startTime;

   public Scaffold() {
      super("Scaffold", "Auto bridge.", ModuleCategory.MOVEMENT);
      instance = this;
   }

   public static Scaffold getInstance() {
      return instance;
   }

   @Override
   protected void onEnable() {
      this.currentPlacement = null;
      this.previousSlot = -1;
      this.placeCooldownTicks = 0;
      this.rotationInitialized = false;
      this.wasSneak = false;
      this.safeWalkActive = false;
      this.lastPlaceYaw = 0.0F;
      this.lastPlacePitch = 0.0F;
      this.lastDeltaYaw = 0.0F;
      this.placeCounter = 0;
      this.startTime = System.currentTimeMillis();
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         this.accumulatedYaw = client.player.getYaw();
         this.accumulatedPitch = client.player.getPitch();
      }
   }

   @Override
   protected void onDisable() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.previousSlot != -1 && client.player != null) {
         InventoryUtils.syncSelectedHotbarSlot(this.previousSlot);
         this.previousSlot = -1;
      }

      if (this.wasSneak && client.options != null) {
         client.options.sneakKey.setPressed(false);
         this.wasSneak = false;
      }

      this.safeWalkActive = false;
      this.currentPlacement = null;
      this.resetRotation();
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         if (this.placeCooldownTicks > 0) {
            this.placeCooldownTicks--;
         }

         this.updateSafeWalk(client);
         this.handleTower(client);
      }
   }

   @SubscribeEvent
   private void onRotationUpdate(RotationUpdateEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null || client.world == null) {
         this.currentPlacement = null;
      } else if (event.isPre()) {
         Scaffold.BlockPlacement placement = this.findPlacement(client);
         if (placement == null) {
            this.currentPlacement = null;
         } else {
            this.currentPlacement = placement;
            this.rotateToPlacement(client, placement);
         }
      } else {
         if (event.isPost() && this.currentPlacement != null && this.canPlaceBlock(client)) {
            this.tryPlaceBlock(client, this.currentPlacement);
         }
      }
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null) {
         if (this.safeWalk.getValue() && this.safeWalkActive) {
            client.options.sneakKey.setPressed(true);
            this.wasSneak = true;
            PlayerInput input = event.getInput();
            event.setInput(new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), input.jump(), true, input.sprint()));
         } else if (this.wasSneak && !this.safeWalkActive) {
            client.options.sneakKey.setPressed(false);
            this.wasSneak = false;
         }

         if (this.currentPlacement != null) {
            this.fixMovementForRotation(client, event);
         }
      }
   }

   private boolean canPlaceBlock(MinecraftClient client) {
      return this.placeCooldownTicks <= 0 && !client.player.isUsingItem();
   }

   private void updateSafeWalk(MinecraftClient client) {
      if (!this.safeWalk.getValue()) {
         this.safeWalkActive = false;
      } else if (!client.player.isOnGround()) {
         this.safeWalkActive = false;
      } else if (!this.hasFullSupportBelow(client)) {
         this.safeWalkActive = true;
      } else {
         Vec3d velocity = client.player.getVelocity();
         double futureX = client.player.getX() + velocity.x * 1.5;
         double futureZ = client.player.getZ() + velocity.z * 1.5;
         BlockPos futureBelow = BlockPos.ofFloored(futureX, client.player.getY() - 1.0, futureZ);
         BlockState belowState = client.world.getBlockState(futureBelow);
         if (this.isReplaceable(belowState)) {
            BlockState placed = this.currentPlacement != null ? client.world.getBlockState(this.currentPlacement.placePos) : null;
            if (placed == null || this.isReplaceable(placed)) {
               this.safeWalkActive = true;
               return;
            }
         }

         this.safeWalkActive = false;
      }
   }

   private boolean hasFullSupportBelow(MinecraftClient client) {
      Box box = client.player.getBoundingBox();
      double y = client.player.getY() - 0.01;
      double[][] points = new double[][]{
         {box.minX, box.minZ}, {box.minX, box.maxZ}, {box.maxX, box.minZ}, {box.maxX, box.maxZ}, {(box.minX + box.maxX) / 2.0, (box.minZ + box.maxZ) / 2.0}
      };

      for (double[] point : points) {
         BlockPos pos = BlockPos.ofFloored(point[0], y - 0.5, point[1]);
         if (!this.isReplaceable(client.world.getBlockState(pos))) {
            return true;
         }
      }

      return false;
   }

   private List<BlockPos> findPlacePositions(MinecraftClient client) {
      Vec3d playerPos = client.player.getEntityPos();
      BlockPos playerBlock = client.player.getBlockPos();
      BlockPos below = playerBlock.down();
      Set<BlockPos> positions = new LinkedHashSet<>();
      if (this.downward.getValue() && client.options.sneakKey.isPressed()) {
         this.addPlacePosition(client, positions, below.down());
      }

      this.addPlacePosition(client, positions, below);
      Box box = client.player.getBoundingBox();
      double y = (double)below.getY();
      double[][] corners = new double[][]{
         {box.minX + 0.03, box.minZ + 0.03},
         {box.minX + 0.03, box.maxZ - 0.03},
         {box.maxX - 0.03, box.minZ + 0.03},
         {box.maxX - 0.03, box.maxZ - 0.03},
         {box.minX, box.minZ},
         {box.minX, box.maxZ},
         {box.maxX, box.minZ},
         {box.maxX, box.maxZ}
      };

      for (double[] corner : corners) {
         this.addPlacePosition(client, positions, BlockPos.ofFloored(corner[0], y, corner[1]));
      }

      for (Direction direction : Type.HORIZONTAL) {
         this.addPlacePosition(client, positions, below.offset(direction));
      }

      this.addPlacePosition(client, positions, below.north().east());
      this.addPlacePosition(client, positions, below.north().west());
      this.addPlacePosition(client, positions, below.south().east());
      this.addPlacePosition(client, positions, below.south().west());
      int extendValue = this.extend.getValue().intValue();
      if (extendValue > 0) {
         Vec3d moveDir = this.getMoveDirection(client);
         if (moveDir.lengthSquared() > 0.001) {
            for (int i = 1; i <= extendValue; i++) {
               double distance = (double)i * 0.45;
               Vec3d side = new Vec3d(-moveDir.z, 0.0, moveDir.x);
               this.addPlacePosition(
                  client, positions, BlockPos.ofFloored(playerPos.x + moveDir.x * distance, (double)below.getY(), playerPos.z + moveDir.z * distance)
               );
               this.addPlacePosition(
                  client,
                  positions,
                  BlockPos.ofFloored(
                     playerPos.x + moveDir.x * distance + side.x * 0.28, (double)below.getY(), playerPos.z + moveDir.z * distance + side.z * 0.28
                  )
               );
               this.addPlacePosition(
                  client,
                  positions,
                  BlockPos.ofFloored(
                     playerPos.x + moveDir.x * distance - side.x * 0.28, (double)below.getY(), playerPos.z + moveDir.z * distance - side.z * 0.28
                  )
               );
            }
         }
      }

      Vec3d velocity = client.player.getVelocity();
      if (Math.abs(velocity.x) > 0.05 || Math.abs(velocity.z) > 0.05) {
         for (double mult = 0.3; mult <= 2.0; mult += 0.3) {
            this.addPlacePosition(client, positions, BlockPos.ofFloored(playerPos.x + velocity.x * mult, (double)below.getY(), playerPos.z + velocity.z * mult));
         }
      }

      return List.copyOf(positions);
   }

   private void addPlacePosition(MinecraftClient client, Set<BlockPos> positions, BlockPos pos) {
      if (pos != null && this.isPlaceTarget(client.world.getBlockState(pos))) {
         positions.add(pos.toImmutable());
      }
   }

   private boolean hasValidNeighbor(MinecraftClient client, BlockPos pos) {
      for (Direction direction : Direction.values()) {
         if (this.isValidSupport(client, pos.offset(direction))) {
            return true;
         }
      }

      return false;
   }

   private Vec3d getMoveDirection(MinecraftClient client) {
      float yaw = client.player.getYaw();
      double forward = 0.0;
      double strafe = 0.0;
      if (client.options.forwardKey.isPressed()) {
         forward++;
      }

      if (client.options.backKey.isPressed()) {
         forward--;
      }

      if (client.options.leftKey.isPressed()) {
         strafe++;
      }

      if (client.options.rightKey.isPressed()) {
         strafe--;
      }

      if (forward == 0.0 && strafe == 0.0) {
         Vec3d velocity = client.player.getVelocity();
         return !(Math.abs(velocity.x) > 0.05) && !(Math.abs(velocity.z) > 0.05) ? Vec3d.ZERO : new Vec3d(velocity.x, 0.0, velocity.z).normalize();
      } else {
         double radians = Math.toRadians((double)yaw);
         double moveAngle = Math.atan2(forward, strafe) - (Math.PI / 2);
         double finalAngle = radians + moveAngle;
         return new Vec3d(-Math.sin(finalAngle), 0.0, Math.cos(finalAngle)).normalize();
      }
   }

   private Scaffold.BlockPlacement findPlacement(MinecraftClient client) {
      Scaffold.BlockPlacement best = null;
      double bestScore = Double.MAX_VALUE;
      int order = 0;

      for (BlockPos targetPos : this.findPlacePositions(client)) {
         Scaffold.BlockPlacement placement = this.findPlacement(client, targetPos, order++);
         if (placement != null && placement.score < bestScore) {
            bestScore = placement.score;
            best = placement;
         }
      }

      return best;
   }

   private Scaffold.BlockPlacement findPlacement(MinecraftClient client, BlockPos targetPos, int targetOrder) {
      Direction[] directions = new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
      List<Scaffold.BlockPlacement> candidates = new ArrayList<>();
      Vec3d eyePos = client.player.getEyePos();

      for (Direction direction : directions) {
         BlockPos neighborPos = targetPos.offset(direction);
         if (this.isValidSupport(client, neighborPos)) {
            Direction placeFace = direction.getOpposite();
            if (this.isFaceVisible(neighborPos, placeFace, eyePos)) {
               Vec3d hitVec = this.calculateHitVec(neighborPos, placeFace);
               if (!(eyePos.distanceTo(hitVec) > 4.5)) {
                  candidates.add(new Scaffold.BlockPlacement(targetPos, neighborPos, placeFace, hitVec));
               }
            }
         }
      }

      if (candidates.isEmpty()) {
         return null;
      } else {
         Angle currentAngle = AngleConnection.INSTANCE.getRotation();
         Scaffold.BlockPlacement best = null;
         double bestScore = Double.MAX_VALUE;

         for (Scaffold.BlockPlacement candidate : candidates) {
            double distance = eyePos.distanceTo(candidate.hitVec);
            Angle requiredAngle = MathAngle.calculateAngle(candidate.hitVec);
            double angleDiff = AngleConnection.computeRotationDifference(currentAngle, requiredAngle);
            double score = (double)targetOrder * 4.0 + distance + angleDiff * 0.25;
            if (candidate.direction == Direction.UP) {
               score -= 5.0;
            }

            if (score < bestScore) {
               bestScore = score;
               best = candidate;
               candidate.score = score;
            }
         }

         return best;
      }
   }

   private boolean isFaceVisible(BlockPos blockPos, Direction face, Vec3d eyePos) {
      Box box = new Box(
         (double)blockPos.getX(),
         (double)blockPos.getY(),
         (double)blockPos.getZ(),
         (double)blockPos.getX() + 1.0,
         (double)blockPos.getY() + 1.0,
         (double)blockPos.getZ() + 1.0
      );

      return switch (face) {
         case UP -> eyePos.y > box.maxY;
         case DOWN -> eyePos.y < box.minY;
         case NORTH -> eyePos.z < box.minZ;
         case SOUTH -> eyePos.z > box.maxZ;
         case WEST -> eyePos.x < box.minX;
         case EAST -> eyePos.x > box.maxX;
         default -> throw new MatchException(null, null);
      };
   }

   private Vec3d calculateHitVec(BlockPos blockPos, Direction face) {
      double x = (double)blockPos.getX();
      double y = (double)blockPos.getY();
      double z = (double)blockPos.getZ();
      double rx = 0.25 + this.random.nextDouble() * 0.5;
      double ry = 0.25 + this.random.nextDouble() * 0.5;
      double rz = 0.25 + this.random.nextDouble() * 0.5;

      return switch (face) {
         case UP -> new Vec3d(x + rx, y + 1.0, z + rz);
         case DOWN -> new Vec3d(x + rx, y, z + rz);
         case NORTH -> new Vec3d(x + rx, y + ry, z);
         case SOUTH -> new Vec3d(x + rx, y + ry, z + 1.0);
         case WEST -> new Vec3d(x, y + ry, z + rz);
         case EAST -> new Vec3d(x + 1.0, y + ry, z + rz);
         default -> throw new MatchException(null, null);
      };
   }

   private boolean isBlacklistedNeighbor(Block block) {
      return block == Blocks.CHEST
         || block == Blocks.ENDER_CHEST
         || block == Blocks.TRAPPED_CHEST
         || block == Blocks.CRAFTING_TABLE
         || block == Blocks.FURNACE
         || block == Blocks.ENCHANTING_TABLE
         || block == Blocks.ANVIL
         || block == Blocks.CHIPPED_ANVIL
         || block == Blocks.DAMAGED_ANVIL;
   }

   private void rotateToPlacement(MinecraftClient client, Scaffold.BlockPlacement placement) {
      Angle targetAngle = MathAngle.fromVec3d(placement.hitVec.subtract(client.player.getEyePos()));
      targetAngle = new Angle(targetAngle.getYaw(), MathHelper.clamp(targetAngle.getPitch(), 30.0F, 89.0F));
      Angle safeAngle = this.createSafeAngle(targetAngle);
      AngleConnection.INSTANCE
         .rotateTo(
            new Angle.VecRotation(safeAngle, safeAngle.toVector()),
            client.player,
            1,
            new AngleConfig(LinearConstructor.INSTANCE, true, false),
            TaskPriority.HIGH_IMPORTANCE_1,
            this
         );
   }

   private Angle createSafeAngle(Angle rawAngle) {
      if (!this.rotationInitialized) {
         this.accumulatedYaw = AngleConnection.INSTANCE.getRotation().getYaw();
         this.accumulatedPitch = AngleConnection.INSTANCE.getRotation().getPitch();
         this.rotationInitialized = true;
      }

      float yawNoise = (this.random.nextFloat() - 0.5F) * 0.8F;
      float pitchNoise = (this.random.nextFloat() - 0.5F) * 0.6F;
      long elapsed = System.currentTimeMillis() - this.startTime;
      float timeSeconds = (float)elapsed / 1000.0F;
      float idleYawNoise = (float)(
         Math.sin((double)(timeSeconds * 1.7F)) * 2.5 + Math.sin((double)(timeSeconds * 3.3F)) * 1.2F + Math.sin((double)(timeSeconds * 0.9F)) * 1.8F
      );
      float idlePitchNoise = (float)(Math.sin((double)(timeSeconds * 1.4F)) * 1.0 + Math.cos((double)(timeSeconds * 2.1F)) * 0.7F);
      idleYawNoise += (this.random.nextFloat() - 0.5F) * 0.5F;
      idlePitchNoise += (this.random.nextFloat() - 0.5F) * 0.3F;
      float targetYaw = rawAngle.getYaw() + yawNoise + idleYawNoise;
      float targetPitch = MathHelper.clamp(rawAngle.getPitch() + pitchNoise + idlePitchNoise, 30.0F, 89.0F);
      float currentServerYaw = AngleConnection.INSTANCE.getRotation().getYaw();
      float yawDelta = MathHelper.wrapDegrees(targetYaw - currentServerYaw);
      float pitchDelta = targetPitch - this.accumulatedPitch;
      float absYaw = Math.abs(yawDelta);
      float yawSpeed;
      if (absYaw < 5.0F) {
         yawSpeed = 1.0F;
      } else if (absYaw < 30.0F) {
         yawSpeed = 0.92F + this.random.nextFloat() * 0.08F;
      } else if (absYaw < 90.0F) {
         yawSpeed = 0.82F + this.random.nextFloat() * 0.1F;
      } else {
         yawSpeed = 0.72F + this.random.nextFloat() * 0.12F;
      }

      float pitchSpeed = 0.75F + this.random.nextFloat() * 0.15F;
      float yawStep = yawDelta * yawSpeed;
      float pitchStep = pitchDelta * pitchSpeed;
      float newYaw = currentServerYaw + yawStep;
      this.accumulatedPitch = MathHelper.clamp(this.accumulatedPitch + pitchStep, -90.0F, 90.0F);
      this.accumulatedYaw = MathHelper.wrapDegrees(newYaw);
      this.accumulatedYaw = this.applyGcd(this.accumulatedYaw, currentServerYaw);
      this.accumulatedPitch = this.applyGcd(this.accumulatedPitch, AngleConnection.INSTANCE.getRotation().getPitch());
      return new Angle(this.accumulatedYaw, MathHelper.clamp(this.accumulatedPitch, -90.0F, 90.0F));
   }

   private float applyGcd(float target, float current) {
      double sensitivity = (Double)MinecraftClient.getInstance().options.getMouseSensitivity().getValue();
      double factor = sensitivity * 0.6 + 0.2;
      float gcd = (float)(factor * factor * factor * 1.2);
      if (gcd <= 0.0F) {
         return target;
      } else {
         float delta = target - current;
         delta = (float)Math.round(delta / gcd) * gcd;
         return current + delta;
      }
   }

   private boolean tryPlaceBlock(MinecraftClient client, Scaffold.BlockPlacement placement) {
      int blockSlot = this.findBlockSlot(client);
      if (blockSlot == -1 || client.interactionManager == null) {
         return false;
      } else if (!this.isPlaceTarget(client.world.getBlockState(placement.placePos))) {
         return false;
      } else if (!this.isValidSupport(client, placement.neighborPos)) {
         return false;
      } else {
         BlockHitResult hitResult = this.raycastPlacementHit(client, placement);
         if (hitResult == null) {
            return false;
         } else {
            if (this.autoSwap.getValue()) {
               if (this.previousSlot == -1) {
                  this.previousSlot = client.player.getInventory().getSelectedSlot();
               }

               if (client.player.getInventory().getSelectedSlot() != blockSlot) {
                  InventoryUtils.syncSelectedHotbarSlot(blockSlot);
               }
            } else if (!(client.player.getMainHandStack().getItem() instanceof BlockItem)) {
               return false;
            }

            ActionResult result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            if (result.isAccepted()) {
               client.player.swingHand(Hand.MAIN_HAND);
               this.placeCooldownTicks = 1;
               this.placeCounter++;
               float newYaw = AngleConnection.INSTANCE.getRotation().getYaw();
               this.lastDeltaYaw = Math.abs(MathHelper.wrapDegrees(newYaw - this.lastPlaceYaw));
               this.lastPlaceYaw = newYaw;
               this.lastPlacePitch = AngleConnection.INSTANCE.getRotation().getPitch();
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private BlockHitResult raycastPlacementHit(MinecraftClient client, Scaffold.BlockPlacement placement) {
      Angle currentAngle = AngleConnection.INSTANCE.getRotation();
      if (currentAngle == null) {
         return null;
      } else {
         BlockHitResult hit = RaycastAngle.raycast(client.player.getEyePos(), 4.75, currentAngle, false);
         if (hit == null || hit.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) {
            return null;
         } else {
            return hit.getBlockPos().equals(placement.neighborPos) && hit.getSide() == placement.direction ? hit : null;
         }
      }
   }

   private int findBlockSlot(MinecraftClient client) {
      int bestSlot = -1;
      int bestCount = 0;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = client.player.getInventory().getStack(i);
         if (!stack.isEmpty()) {
            Item var7 = stack.getItem();
            if (var7 instanceof BlockItem) {
               BlockItem blockItem = (BlockItem)var7;
               if (!this.isUnusableBlock(blockItem.getBlock()) && stack.getCount() > bestCount) {
                  bestCount = stack.getCount();
                  bestSlot = i;
               }
            }
         }
      }

      return bestSlot;
   }

   private boolean isUnusableBlock(Block block) {
      return block == Blocks.TNT
         || block == Blocks.SAND
         || block == Blocks.RED_SAND
         || block == Blocks.GRAVEL
         || block == Blocks.ANVIL
         || block == Blocks.CHIPPED_ANVIL
         || block == Blocks.DAMAGED_ANVIL
         || block == Blocks.CHEST
         || block == Blocks.ENDER_CHEST
         || block == Blocks.TRAPPED_CHEST
         || block == Blocks.ENCHANTING_TABLE
         || block == Blocks.CRAFTING_TABLE
         || block == Blocks.FURNACE
         || block == Blocks.TORCH
         || block == Blocks.WALL_TORCH
         || block == Blocks.REDSTONE_TORCH
         || block == Blocks.REDSTONE_WALL_TORCH
         || block == Blocks.LADDER
         || block == Blocks.VINE
         || block == Blocks.LILY_PAD
         || block == Blocks.CACTUS;
   }

   private void handleTower(MinecraftClient client) {
      if (this.tower.getValue() && client.options.jumpKey.isPressed()) {
         if (client.player.isOnGround() && this.currentPlacement != null) {
            client.player.jump();
         }
      }
   }

   private void fixMovementForRotation(MinecraftClient client, InputEvent event) {
      boolean w = client.options.forwardKey.isPressed();
      boolean s = client.options.backKey.isPressed();
      boolean a = client.options.leftKey.isPressed();
      boolean d = client.options.rightKey.isPressed();
      if (w || s || a || d) {
         float playerYaw = client.player.getYaw();
         float serverYaw = MathHelper.wrapDegrees(AngleConnection.INSTANCE.getRotation().getYaw());
         float moveAngle;
         if (w && !s && !a && !d) {
            moveAngle = 0.0F;
         } else if (s && !w && !a && !d) {
            moveAngle = 180.0F;
         } else if (a && !d && !w && !s) {
            moveAngle = -90.0F;
         } else if (d && !a && !w && !s) {
            moveAngle = 90.0F;
         } else if (w && a) {
            moveAngle = -45.0F;
         } else if (w && d) {
            moveAngle = 45.0F;
         } else if (s && a) {
            moveAngle = -135.0F;
         } else if (s && d) {
            moveAngle = 135.0F;
         } else {
            moveAngle = 180.0F;
         }

         float desiredDirection = MathHelper.wrapDegrees(playerYaw + moveAngle);
         float angleDiff = MathHelper.wrapDegrees(desiredDirection - serverYaw);
         boolean forward = false;
         boolean back = false;
         boolean left = false;
         boolean right = false;
         if (angleDiff >= -22.5F && angleDiff < 22.5F) {
            forward = true;
         } else if (angleDiff >= 22.5F && angleDiff < 67.5F) {
            forward = true;
            right = true;
         } else if (angleDiff >= 67.5F && angleDiff < 112.5F) {
            right = true;
         } else if (angleDiff >= 112.5F && angleDiff < 157.5F) {
            back = true;
            right = true;
         } else if (angleDiff >= -67.5F && angleDiff < -22.5F) {
            forward = true;
            left = true;
         } else if (angleDiff >= -112.5F && angleDiff < -67.5F) {
            left = true;
         } else if (angleDiff >= -157.5F && angleDiff < -112.5F) {
            back = true;
            left = true;
         } else {
            back = true;
         }

         event.setDirectionalLow(forward, back, left, right);
      }
   }

   private void resetRotation() {
      if (MinecraftClient.getInstance().player != null) {
         AngleConnection.INSTANCE.startReturning();
         AngleConnection.INSTANCE.reset();
         this.rotationInitialized = false;
      }
   }

   private boolean isReplaceable(BlockState state) {
      return state.isAir() || state.isReplaceable();
   }

   private boolean isPlaceTarget(BlockState state) {
      return this.isReplaceable(state) && state.getFluidState().isEmpty();
   }

   private boolean isValidSupport(MinecraftClient client, BlockPos pos) {
      BlockState state = client.world.getBlockState(pos);
      return !this.isReplaceable(state)
         && state.getFluidState().isEmpty()
         && !state.getCollisionShape(client.world, pos).isEmpty()
         && !this.isBlacklistedNeighbor(state.getBlock());
   }

   private static final class BlockPlacement {
      private final BlockPos placePos;
      private final BlockPos neighborPos;
      private final Direction direction;
      private final Vec3d hitVec;
      private double score;

      private BlockPlacement(BlockPos placePos, BlockPos neighborPos, Direction direction, Vec3d hitVec) {
         this.placePos = placePos;
         this.neighborPos = neighborPos;
         this.direction = direction;
         this.hitVec = hitVec;
      }
   }
}
