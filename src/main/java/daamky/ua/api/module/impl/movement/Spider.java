package daamky.ua.api.module.impl.movement;

import net.minecraft.block.ButtonBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
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
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.options != null) {
         client.options.jumpKey.setPressed(false);
      }

      this.buttonClimbLayer = 0;
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         if (this.mode.is("FunTime")) {
            this.handleLeverClimb(client);
         } else if (this.mode.is("FunTimeButtons")) {
            this.funtimeButtons(client);
         } else if (this.mode.is("FunTimeMed")) {
            this.handleFunTimeMed(client);
         } else if (this.mode.is("Collision")) {
            this.handleCollisionSpider(client);
         } else if (this.mode.is("Blocks")) {
            boolean offHand = client.player.getOffHandStack().getItem() instanceof BlockItem;
            int slot = this.findHotbarBlockSlot(client);
            BlockPos blockPos = this.findPos(client);
            if ((offHand || slot != -1) && !blockPos.equals(BlockPos.ORIGIN)) {
               ItemStack stack = offHand ? client.player.getOffHandStack() : client.player.getInventory().getStack(slot);
               if (stack.getItem() instanceof BlockItem) {
                  Vec3d vec = blockPos.toCenterPos();
                  Direction direction = nearestDirection(vec.x - client.player.getX(), vec.y - client.player.getY(), vec.z - client.player.getZ());
                  Angle angle = MathAngle.calculateAngle(vec.subtract(Vec3d.of(direction.getVector()).multiply(0.1F)));
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
                     Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
                     int previous = client.player.getInventory().getSelectedSlot();
                     if (!offHand) {
                        client.player.getInventory().setSelectedSlot(slot);
                        client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                     }

                     Direction hitFace = direction.getOpposite();
                     BlockHitResult hit = new BlockHitResult(vec, hitFace, blockPos, false);
                     this.sendPacketPlaceAndBreak(client, hand, hit, blockPos.offset(hitFace), hitFace);
                     Vec3d velocity = client.player.getVelocity();
                     client.player.setVelocity(velocity.x, 0.2, velocity.z);
                     if (!offHand) {
                        InventoryUtils.selectSlot(previous);
                        client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previous));
                        client.player.setVelocity(velocity.x, 0.2, velocity.z);
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
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.mode.is("Collision")
         && event.isReceive()
         && client.player != null
         && event.getPacket() instanceof PlayerPositionLookS2CPacket
         && this.collisionTimer.finished(600.0)) {
         this.boostCollisionSpider(client);
         this.collisionTimer.reset();
      }
   }

   private void handleCollisionSpider(MinecraftClient client) {
      if (client.player.horizontalCollision && this.collisionTimer.finished(600.0)) {
         this.boostCollisionSpider(client);
         this.collisionTimer.reset();
      }
   }

   private void boostCollisionSpider(MinecraftClient client) {
      client.player.setOnGround(true);
      Vec3d velocity = client.player.getVelocity();
      client.player.setVelocity(velocity.x, 0.6, velocity.z);
      client.player.fallDistance = 0.0;
   }

   private void sendPacketPlaceAndBreak(MinecraftClient client, Hand hand, BlockHitResult hit, BlockPos placedPos, Direction breakFace) {
      if (client.player != null && client.world != null && client.getNetworkHandler() != null) {
         PendingUpdateManager prediction = null;
         int sequence = 0;

         try {
            prediction = client.world.getPendingUpdateManager().incrementSequence();
            sequence = prediction.getSequence();
         } catch (Exception var56) {
         } finally {
            try {
               client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, hit, sequence));
               client.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
               client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, placedPos, breakFace));
               client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, placedPos, breakFace));
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

   private int findHotbarBlockSlot(MinecraftClient client) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = client.player.getInventory().getStack(i);
         if (stack.getItem() instanceof BlockItem) {
            return i;
         }
      }

      return -1;
   }

   private void handleLeverClimb(MinecraftClient client) {
      if (client.player.horizontalCollision) {
         int leverSlot = InventoryUtils.findItemAnywhere(Items.TORCH);
         if (leverSlot == -1) {
            this.setEnabled(false);
         } else if (this.ftbutton.finished(150.0)) {
            client.player.setOnGround(true);
            client.player.jump();
            client.player.fallDistance = 0.0;
            this.placeLever(client, leverSlot);
            this.ftbutton.reset();
         }
      }
   }

   private void placeLever(MinecraftClient client, int slot) {
      if (client.interactionManager != null) {
         int previous = this.selectSlot(client, slot);
         float yaw = this.getWallYaw(client, this.getWallDirection(client));
         float pitch = 70.0F;
         this.applyLook(client, yaw, pitch);
         BlockHitResult hit = this.getTargetedBlock(client, yaw, pitch, 4.0);
         if (hit.getType() == Type.BLOCK) {
            client.player.swingHand(Hand.MAIN_HAND);
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
         }

         this.restoreSlot(client, slot, previous);
         client.player.fallDistance = 0.0;
      }
   }

   private void handleFunTimeMed(MinecraftClient client) {
      if (client.player.horizontalCollision) {
         if (this.ftbutton.finished(1.0)) {
            int rodSlot = InventoryUtils.findItemAnywhere(Items.LIGHTNING_ROD);
            if (rodSlot == -1) {
               this.setEnabled(false);
            } else {
               client.player.setOnGround(true);
               client.player.jump();
               this.placeFenceStack(client, rodSlot);
               client.player.fallDistance = 0.0;
               this.ftbutton.reset();
            }
         }
      }
   }

   private void placeFenceStack(MinecraftClient client, int slot) {
      if (client.interactionManager != null) {
         int previous = this.selectSlot(client, slot);
         BlockPos playerPos = client.player.getBlockPos();

         for (int i = 1; i <= 2; i++) {
            BlockPos fencePos = playerPos.up(i);
            if (client.world.getBlockState(fencePos).isAir()) {
               this.placeFenceBlock(client, fencePos);
            }
         }

         this.restoreSlot(client, slot, previous);
      }
   }

   private void placeFenceBlock(MinecraftClient client, BlockPos pos) {
      if (client.interactionManager != null && client.world.getBlockState(pos).isAir()) {
         Vec3d center = pos.toCenterPos();
         BlockHitResult hit = new BlockHitResult(center, Direction.UP, pos.down(), false);
         client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
         client.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private void funtimeButtons(MinecraftClient client) {
      if (client.options != null) {
         client.options.jumpKey.setPressed(client.options.forwardKey.isPressed());
      }

      if (client.player.horizontalCollision) {
         int buttonSlot = this.findButtonSlot(client);
         boolean hasPlacedButton = this.hasButtonClimbStep(client);
         if (buttonSlot == -1 && !hasPlacedButton) {
            this.setEnabled(false);
         } else if (this.ftbutton.finished(180.0)) {
            client.player.setOnGround(true);
            client.player.jump();
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

   private boolean placeButtonClimbStep(MinecraftClient client, int slot) {
      if (client.interactionManager == null) {
         return false;
      } else {
         int previous = this.selectSlot(client, slot);
         Direction wallDirection = this.getWallDirection(client);
         Direction hitFace = wallDirection.getOpposite();
         BlockPos playerPos = client.player.getBlockPos();
         boolean placed = false;

         for (int offset = 0; offset <= 2; offset++) {
            int y = (this.buttonClimbLayer + offset) % 3;
            BlockPos wallPos = playerPos.offset(wallDirection).up(y);
            BlockPos buttonPos = wallPos.offset(hitFace);
            if (!client.world.getBlockState(wallPos).isAir()) {
               if (client.world.getBlockState(buttonPos).getBlock() instanceof ButtonBlock) {
                  this.lookAt(client, this.hitVector(wallPos, hitFace));
                  this.buttonClimbLayer = (y + 1) % 3;
                  placed = true;
                  break;
               }

               if (client.world.getBlockState(buttonPos).isReplaceable()) {
                  Vec3d hitVec = this.hitVector(wallPos, hitFace);
                  this.lookAt(client, hitVec);
                  BlockHitResult hit = new BlockHitResult(hitVec, hitFace, wallPos, false);
                  client.player.swingHand(Hand.MAIN_HAND);
                  ActionResult result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
                  if (result.isAccepted()) {
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

   private boolean lookAtButtonClimbStep(MinecraftClient client) {
      Direction wallDirection = this.getWallDirection(client);
      Direction hitFace = wallDirection.getOpposite();
      BlockPos playerPos = client.player.getBlockPos();

      for (int offset = 0; offset <= 2; offset++) {
         int y = (this.buttonClimbLayer + offset) % 3;
         BlockPos wallPos = playerPos.offset(wallDirection).up(y);
         BlockPos buttonPos = wallPos.offset(hitFace);
         if (client.world.getBlockState(buttonPos).getBlock() instanceof ButtonBlock) {
            this.lookAt(client, this.hitVector(wallPos, hitFace));
            this.buttonClimbLayer = (y + 1) % 3;
            return true;
         }
      }

      return false;
   }

   private boolean hasButtonClimbStep(MinecraftClient client) {
      Direction wallDirection = this.getWallDirection(client);
      Direction hitFace = wallDirection.getOpposite();
      BlockPos playerPos = client.player.getBlockPos();

      for (int y = 1; y >= 0; y--) {
         BlockPos wallPos = playerPos.offset(wallDirection).up(y);
         BlockPos buttonPos = wallPos.offset(hitFace);
         if (!client.world.getBlockState(wallPos).isAir() && client.world.getBlockState(buttonPos).getBlock() instanceof ButtonBlock) {
            return true;
         }
      }

      return false;
   }

   private int findButtonSlot(MinecraftClient client) {
      for (int i = 0; i < 36; i++) {
         if (this.isButtonStack(client.player.getInventory().getStack(i))) {
            return i;
         }
      }

      return -1;
   }

   private boolean isButtonStack(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ButtonBlock;
   }

   private int selectSlot(MinecraftClient client, int slot) {
      int previous = client.player.getInventory().getSelectedSlot();
      if (slot > 8) {
         InventoryUtils.click(InventoryUtils.wrapSlot(slot), previous, SlotActionType.SWAP);
      } else if (slot != previous) {
         InventoryUtils.syncSelectedHotbarSlot(slot);
      }

      return previous;
   }

   private void restoreSlot(MinecraftClient client, int slot, int previous) {
      if (slot > 8) {
         InventoryUtils.click(InventoryUtils.wrapSlot(slot), previous, SlotActionType.SWAP);
         InventoryUtils.closeScreen();
      } else if (slot != previous) {
         InventoryUtils.syncSelectedHotbarSlot(previous);
      }
   }

   private Vec3d hitVector(BlockPos wallPos, Direction hitFace) {
      return wallPos.toCenterPos().add(Vec3d.of(hitFace.getVector()).multiply(0.5));
   }

   private void lookAt(MinecraftClient client, Vec3d target) {
      Vec3d eye = client.player.getEyePos();
      double dx = target.x - eye.x;
      double dy = target.y - eye.y;
      double dz = target.z - eye.z;
      double distXZ = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, distXZ)));
      this.applyLook(client, yaw, pitch);
   }

   private void applyLook(MinecraftClient client, float yaw, float pitch) {
      client.player.setYaw(yaw);
      client.player.setPitch(pitch);
      client.player.lastYaw = yaw;
      client.player.lastPitch = pitch;
      client.player.setHeadYaw(yaw);
      client.player.setBodyYaw(yaw);
   }

   private Direction getWallDirection(MinecraftClient client) {
      Direction forward = client.player.getHorizontalFacing();
      BlockPos playerPos = client.player.getBlockPos();
      if (!client.world.getBlockState(playerPos.offset(forward)).isAir()) {
         return forward;
      } else {
         for (Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            if (!client.world.getBlockState(playerPos.offset(direction)).isAir()) {
               return direction;
            }
         }

         return forward;
      }
   }

   private BlockHitResult getTargetedBlock(MinecraftClient client, float yaw, float pitch, double distance) {
      Vec3d eye = client.player.getEyePos();
      Vec3d dir = Vec3d.fromPolar(pitch, yaw).normalize();
      Vec3d end = eye.add(dir.multiply(distance));
      RaycastContext context = new RaycastContext(eye, end, ShapeType.OUTLINE, FluidHandling.NONE, client.player);
      return client.world.raycast(context);
   }

   private float getWallYaw(MinecraftClient client, Direction direction) {
      return switch (direction) {
         case NORTH -> 180.0F;
         case SOUTH -> 0.0F;
         case WEST -> 90.0F;
         case EAST -> -90.0F;
         default -> client.player.getYaw();
      };
   }

   private BlockPos findPos(MinecraftClient client) {
      BlockPos blockPos = this.getBlockPos(client);
      if (client.world.getBlockState(blockPos).isFullCube(client.world, blockPos)) {
         return BlockPos.ORIGIN;
      } else {
         for (BlockPos pos : new BlockPos[]{blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north()}) {
            if (client.world.getBlockState(pos).isFullCube(client.world, pos)) {
               return pos;
            }
         }

         return BlockPos.ORIGIN;
      }
   }

   private BlockPos getBlockPos(MinecraftClient client) {
      Vec3d predicted = client.player.getEntityPos().add(client.player.getVelocity()).add(0.0, -0.001, 0.0);
      return BlockPos.ofFloored(predicted);
   }

   private boolean canPlace(MinecraftClient client, BlockItem blockItem) {
      BlockPos blockPos = this.getBlockPos(client);
      if (blockPos.getY() >= BlockPos.ofFloored(client.player.getX(), client.player.getY(), client.player.getZ()).getY()) {
         return false;
      } else {
         VoxelShape shape = blockItem.getBlock().getDefaultState().getCollisionShape(client.world, blockPos);
         if (shape.isEmpty()) {
            return false;
         } else {
            Box box = shape.getBoundingBox().offset(blockPos);
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
