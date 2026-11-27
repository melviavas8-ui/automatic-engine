package daamky.ua.api.module.impl.movement;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConfig;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.rotations.SnapAngle;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.module.impl.combat.aura.util.TaskPriority;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Spider extends Module {
   private final ModeSetting mode = this.register(
      new ModeSetting("Mode", "Spider mode.", "Blocks", "Blocks", "FunTime", "FunTimeButtons", "FunTimeMed", "Collision")
   );
   private final StopWatch placeTimer = new StopWatch();
   private final StopWatch ftbutton = new StopWatch();
   private final StopWatch collisionTimer = new StopWatch();
   private int buttonClimbLayer;

   public Spider() {
      super("Spider", "Climbs walls.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.ftbutton.reset();
      this.collisionTimer.reset();
      this.buttonClimbLayer = 0;
   }

   @Override
   protected void onDisable() {
      Minecraft client = Minecraft.getInstance();
      if (client.options != null) {
         client.options.keyJump.setDown(false);
      }

      this.buttonClimbLayer = 0;
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.level != null) {
         if (this.mode.is("FunTime")) {
            this.handleLeverClimb(client);
         } else if (this.mode.is("FunTimeButtons")) {
            this.funtimeButtons(client);
         } else if (this.mode.is("FunTimeMed")) {
            this.handleFunTimeMed(client);
         } else if (this.mode.is("Collision")) {
            this.handleCollisionSpider(client);
         } else if (this.mode.is("Blocks")) {
            boolean offHand = client.player.getOffhandItem().getItem() instanceof BlockItem;
            int slot = this.findHotbarBlockSlot(client);
            BlockPos blockPos = this.findPos(client);
            if ((offHand || slot != -1) && !blockPos.equals(BlockPos.ZERO)) {
               ItemStack stack = offHand ? client.player.getOffhandItem() : client.player.getInventory().getItem(slot);
               if (stack.getItem() instanceof BlockItem) {
                  Vec3 vec = blockPos.getCenter();
                  Direction direction = nearestDirection(vec.x - client.player.getX(), vec.y - client.player.getY(), vec.z - client.player.getZ());
                  Angle angle = MathAngle.calculateAngle(vec.subtract(Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(0.1F)));
                  AngleConnection.INSTANCE
                     .rotateTo(
                        new Angle.VecRotation(angle, angle.toVector()),
                        client.player,
                        1,
                        new AngleConfig(new SnapAngle(), true, true),
                        TaskPriority.HIGH_IMPORTANCE_1,
                        this
                     );
                  if (this.placeTimer.finished(75.0) && this.canPlace(client, (BlockItem)stack.getItem())) {
                     InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                     int previous = client.player.getInventory().getSelectedSlot();
                     if (!offHand) {
                        client.player.getInventory().setSelectedSlot(slot);
                        client.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
                     }

                     Direction hitFace = direction.getOpposite();
                     BlockHitResult hit = new BlockHitResult(vec, hitFace, blockPos, false);
                     this.sendPacketPlaceAndBreak(client, hand, hit, blockPos.relative(hitFace), hitFace);
                     Vec3 velocity = client.player.getDeltaMovement();
                     client.player.setDeltaMovement(velocity.x, 0.2, velocity.z);
                     if (!offHand) {
                        InventoryUtils.selectSlot(previous);
                        client.player.connection.send(new ServerboundSetCarriedItemPacket(previous));
                        client.player.setDeltaMovement(velocity.x, 0.2, velocity.z);
                     }

                     this.placeTimer.reset();
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (this.mode.is("Collision")
         && event.isReceive()
         && client.player != null
         && event.getPacket() instanceof ClientboundPlayerPositionPacket
         && this.collisionTimer.finished(600.0)) {
         this.boostCollisionSpider(client);
         this.collisionTimer.reset();
      }
   }

   private void handleCollisionSpider(Minecraft client) {
      if (client.player.horizontalCollision && this.collisionTimer.finished(600.0)) {
         this.boostCollisionSpider(client);
         this.collisionTimer.reset();
      }
   }

   private void boostCollisionSpider(Minecraft client) {
      client.player.setOnGround(true);
      Vec3 velocity = client.player.getDeltaMovement();
      client.player.setDeltaMovement(velocity.x, 0.6, velocity.z);
      client.player.fallDistance = 0.0;
   }

   private void sendPacketPlaceAndBreak(Minecraft client, InteractionHand hand, BlockHitResult hit, BlockPos placedPos, Direction breakFace) {
      if (client.player != null && client.level != null && client.getConnection() != null) {
         BlockStatePredictionHandler prediction = null;
         int sequence = 0;

         try {
            prediction = client.level.getBlockStatePredictionHandler().startPredicting();
            sequence = prediction.currentSequence();
         } catch (Exception var56) {
         } finally {
            try {
               client.getConnection().send(new ServerboundUseItemOnPacket(hand, hit, sequence));
               client.player.connection.send(new ServerboundSwingPacket(hand));
               client.player.connection.send(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, placedPos, breakFace));
               client.player.connection.send(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, placedPos, breakFace));
            } finally {
               if (prediction != null) {
                  try {
                     prediction.close();
                  } catch (Exception var55) {
                  }
               }
            }
         }
      }
   }

   private int findHotbarBlockSlot(Minecraft client) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = client.player.getInventory().getItem(i);
         if (stack.getItem() instanceof BlockItem) {
            return i;
         }
      }

      return -1;
   }

   private void handleLeverClimb(Minecraft client) {
      if (client.player.horizontalCollision) {
         int leverSlot = InventoryUtils.findItemAnywhere(Items.TORCH);
         if (leverSlot == -1) {
            this.setEnabled(false);
         } else if (this.ftbutton.finished(150.0)) {
            client.player.setOnGround(true);
            client.player.jumpFromGround();
            client.player.fallDistance = 0.0;
            this.placeLever(client, leverSlot);
            this.ftbutton.reset();
         }
      }
   }

   private void placeLever(Minecraft client, int slot) {
      if (client.gameMode != null) {
         int previous = this.selectSlot(client, slot);
         float yaw = this.getWallYaw(client, this.getWallDirection(client));
         float pitch = 70.0F;
         this.applyLook(client, yaw, pitch);
         BlockHitResult hit = this.getTargetedBlock(client, yaw, pitch, 4.0);
         if (hit.getType() == Type.BLOCK) {
            client.player.swing(InteractionHand.MAIN_HAND);
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
         }

         this.restoreSlot(client, slot, previous);
         client.player.fallDistance = 0.0;
      }
   }

   private void handleFunTimeMed(Minecraft client) {
      if (client.player.horizontalCollision) {
         if (this.ftbutton.finished(1.0)) {
            int rodSlot = InventoryUtils.findItemAnywhere(Items.LIGHTNING_ROD);
            if (rodSlot == -1) {
               this.setEnabled(false);
            } else {
               client.player.setOnGround(true);
               client.player.jumpFromGround();
               this.placeFenceStack(client, rodSlot);
               client.player.fallDistance = 0.0;
               this.ftbutton.reset();
            }
         }
      }
   }

   private void placeFenceStack(Minecraft client, int slot) {
      if (client.gameMode != null) {
         int previous = this.selectSlot(client, slot);
         BlockPos playerPos = client.player.blockPosition();

         for (int i = 1; i <= 2; i++) {
            BlockPos fencePos = playerPos.above(i);
            if (client.level.getBlockState(fencePos).isAir()) {
               this.placeFenceBlock(client, fencePos);
            }
         }

         this.restoreSlot(client, slot, previous);
      }
   }

   private void placeFenceBlock(Minecraft client, BlockPos pos) {
      if (client.gameMode != null && client.level.getBlockState(pos).isAir()) {
         Vec3 center = pos.getCenter();
         BlockHitResult hit = new BlockHitResult(center, Direction.UP, pos.below(), false);
         client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
         client.player.swing(InteractionHand.MAIN_HAND);
      }
   }

   private void funtimeButtons(Minecraft client) {
      if (client.options != null) {
         client.options.keyJump.setDown(client.options.keyUp.isDown());
      }

      if (client.player.horizontalCollision) {
         int buttonSlot = this.findButtonSlot(client);
         boolean hasPlacedButton = this.hasButtonClimbStep(client);
         if (buttonSlot == -1 && !hasPlacedButton) {
            this.setEnabled(false);
         } else if (this.ftbutton.finished(180.0)) {
            client.player.setOnGround(true);
            client.player.jumpFromGround();
            client.player.fallDistance = 0.0;
            if (hasPlacedButton) {
               this.lookAtButtonClimbStep(client);
            } else {
               this.placeButtonClimbStep(client, buttonSlot);
            }

            this.ftbutton.reset();
         }
      }
   }

   private boolean placeButtonClimbStep(Minecraft client, int slot) {
      if (client.gameMode == null) {
         return false;
      } else {
         int previous = this.selectSlot(client, slot);
         Direction wallDirection = this.getWallDirection(client);
         Direction hitFace = wallDirection.getOpposite();
         BlockPos playerPos = client.player.blockPosition();
         boolean placed = false;

         for (int offset = 0; offset <= 2; offset++) {
            int y = (this.buttonClimbLayer + offset) % 3;
            BlockPos wallPos = playerPos.relative(wallDirection).above(y);
            BlockPos buttonPos = wallPos.relative(hitFace);
            if (!client.level.getBlockState(wallPos).isAir()) {
               if (client.level.getBlockState(buttonPos).getBlock() instanceof ButtonBlock) {
                  this.lookAt(client, this.hitVector(wallPos, hitFace));
                  this.buttonClimbLayer = (y + 1) % 3;
                  placed = true;
                  break;
               }

               if (client.level.getBlockState(buttonPos).canBeReplaced()) {
                  Vec3 hitVec = this.hitVector(wallPos, hitFace);
                  this.lookAt(client, hitVec);
                  BlockHitResult hit = new BlockHitResult(hitVec, hitFace, wallPos, false);
                  client.player.swing(InteractionHand.MAIN_HAND);
                  InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
                  if (result.consumesAction()) {
                     this.buttonClimbLayer = (y + 1) % 3;
                     placed = true;
                     break;
                  }
               }
            }
         }

         this.restoreSlot(client, slot, previous);
         client.player.fallDistance = 0.0;
         return placed;
      }
   }

   private boolean lookAtButtonClimbStep(Minecraft client) {
      Direction wallDirection = this.getWallDirection(client);
      Direction hitFace = wallDirection.getOpposite();
      BlockPos playerPos = client.player.blockPosition();

      for (int offset = 0; offset <= 2; offset++) {
         int y = (this.buttonClimbLayer + offset) % 3;
         BlockPos wallPos = playerPos.relative(wallDirection).above(y);
         BlockPos buttonPos = wallPos.relative(hitFace);
         if (client.level.getBlockState(buttonPos).getBlock() instanceof ButtonBlock) {
            this.lookAt(client, this.hitVector(wallPos, hitFace));
            this.buttonClimbLayer = (y + 1) % 3;
            return true;
         }
      }

      return false;
   }

   private boolean hasButtonClimbStep(Minecraft client) {
      Direction wallDirection = this.getWallDirection(client);
      Direction hitFace = wallDirection.getOpposite();
      BlockPos playerPos = client.player.blockPosition();

      for (int y = 1; y >= 0; y--) {
         BlockPos wallPos = playerPos.relative(wallDirection).above(y);
         BlockPos buttonPos = wallPos.relative(hitFace);
         if (!client.level.getBlockState(wallPos).isAir() && client.level.getBlockState(buttonPos).getBlock() instanceof ButtonBlock) {
            return true;
         }
      }

      return false;
   }

   private int findButtonSlot(Minecraft client) {
      for (int i = 0; i < 36; i++) {
         if (this.isButtonStack(client.player.getInventory().getItem(i))) {
            return i;
         }
      }

      return -1;
   }

   private boolean isButtonStack(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ButtonBlock;
   }

   private int selectSlot(Minecraft client, int slot) {
      int previous = client.player.getInventory().getSelectedSlot();
      if (slot > 8) {
         InventoryUtils.click(InventoryUtils.wrapSlot(slot), previous, ClickType.SWAP);
      } else if (slot != previous) {
         InventoryUtils.syncSelectedHotbarSlot(slot);
      }

      return previous;
   }

   private void restoreSlot(Minecraft client, int slot, int previous) {
      if (slot > 8) {
         InventoryUtils.click(InventoryUtils.wrapSlot(slot), previous, ClickType.SWAP);
         InventoryUtils.closeScreen();
      } else if (slot != previous) {
         InventoryUtils.syncSelectedHotbarSlot(previous);
      }
   }

   private Vec3 hitVector(BlockPos wallPos, Direction hitFace) {
      return wallPos.getCenter().add(Vec3.atLowerCornerOf(hitFace.getUnitVec3i()).scale(0.5));
   }

   private void lookAt(Minecraft client, Vec3 target) {
      Vec3 eye = client.player.getEyePosition();
      double dx = target.x - eye.x;
      double dy = target.y - eye.y;
      double dz = target.z - eye.z;
      double distXZ = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, distXZ)));
      this.applyLook(client, yaw, pitch);
   }

   private void applyLook(Minecraft client, float yaw, float pitch) {
      client.player.setYRot(yaw);
      client.player.setXRot(pitch);
      client.player.yRotO = yaw;
      client.player.xRotO = pitch;
      client.player.setYHeadRot(yaw);
      client.player.setYBodyRot(yaw);
   }

   private Direction getWallDirection(Minecraft client) {
      Direction forward = client.player.getDirection();
      BlockPos playerPos = client.player.blockPosition();
      if (!client.level.getBlockState(playerPos.relative(forward)).isAir()) {
         return forward;
      } else {
         for (Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (!client.level.getBlockState(playerPos.relative(direction)).isAir()) {
               return direction;
            }
         }

         return forward;
      }
   }

   private BlockHitResult getTargetedBlock(Minecraft client, float yaw, float pitch, double distance) {
      Vec3 eye = client.player.getEyePosition();
      Vec3 dir = Vec3.directionFromRotation(pitch, yaw).normalize();
      Vec3 end = eye.add(dir.scale(distance));
      ClipContext context = new ClipContext(eye, end, Block.OUTLINE, Fluid.NONE, client.player);
      return client.level.clip(context);
   }

   private float getWallYaw(Minecraft client, Direction direction) {
      return switch (direction) {
         case NORTH -> 180.0F;
         case SOUTH -> 0.0F;
         case WEST -> 90.0F;
         case EAST -> -90.0F;
         default -> client.player.getYRot();
      };
   }

   private BlockPos findPos(Minecraft client) {
      BlockPos blockPos = this.getBlockPos(client);
      if (client.level.getBlockState(blockPos).isCollisionShapeFullBlock(client.level, blockPos)) {
         return BlockPos.ZERO;
      } else {
         for (BlockPos pos : new BlockPos[]{blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north()}) {
            if (client.level.getBlockState(pos).isCollisionShapeFullBlock(client.level, pos)) {
               return pos;
            }
         }

         return BlockPos.ZERO;
      }
   }

   private BlockPos getBlockPos(Minecraft client) {
      Vec3 predicted = client.player.position().add(client.player.getDeltaMovement()).add(0.0, -0.001, 0.0);
      return BlockPos.containing(predicted);
   }

   private boolean canPlace(Minecraft client, BlockItem blockItem) {
      BlockPos blockPos = this.getBlockPos(client);
      if (blockPos.getY() >= BlockPos.containing(client.player.getX(), client.player.getY(), client.player.getZ()).getY()) {
         return false;
      } else {
         VoxelShape shape = blockItem.getBlock().defaultBlockState().getCollisionShape(client.level, blockPos);
         if (shape.isEmpty()) {
            return false;
         } else {
            AABB box = shape.bounds().move(blockPos);
            return !box.intersects(client.player.getBoundingBox());
         }
      }
   }

   private static Direction nearestDirection(double x, double y, double z) {
      double absX = Math.abs(x);
      double absY = Math.abs(y);
      double absZ = Math.abs(z);
      if (absX >= absY && absX >= absZ) {
         return x >= 0.0 ? Direction.EAST : Direction.WEST;
      } else if (absY >= absX && absY >= absZ) {
         return y >= 0.0 ? Direction.UP : Direction.DOWN;
      } else {
         return z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }
}
