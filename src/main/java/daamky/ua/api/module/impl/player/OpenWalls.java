package daamky.ua.api.module.impl.player;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.settings.impl.NumberSetting;

public final class OpenWalls extends Module {
   private final NumberSetting range = this.register(new NumberSetting("Range", "Open range through walls.", 6.0, 3.0, 8.0, 0.1));
   private boolean usePressedLastTick;

   public OpenWalls() {
      super("Open Walls", "Opens containers through walls.", ModuleCategory.PLAYER);
   }

   @Override
   protected void onDisable() {
      this.usePressedLastTick = false;
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null && client.interactionManager != null) {
         if (client.currentScreen != null) {
            this.usePressedLastTick = false;
         } else {
            boolean usePressed = client.options.useKey.isPressed();
            if (!usePressed) {
               this.usePressedLastTick = false;
            } else if (!this.usePressedLastTick) {
               this.usePressedLastTick = true;
               BlockPos target = this.findOpenableBlockByLook(client, this.range.getFloat());
               if (target != null) {
                  Direction side = this.resolveClickSide(client, target);
                  Vec3d hitVec = Vec3d.ofCenter(target).add((double)side.getOffsetX() * 0.5, (double)side.getOffsetY() * 0.5, (double)side.getOffsetZ() * 0.5);
                  ActionResult result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, new BlockHitResult(hitVec, side, target, false));
                  if (result.isAccepted()) {
                     client.player.swingHand(Hand.MAIN_HAND);
                  }
               }
            }
         }
      }
   }

   private BlockPos findOpenableBlockByLook(MinecraftClient client, float distance) {
      Vec3d start = client.player.getEyePos();
      Vec3d direction = MathAngle.cameraAngle().toVector();
      Set<BlockPos> visited = new HashSet<>();

      for (double current = 0.0; current <= (double)distance; current += 0.2) {
         BlockPos pos = BlockPos.ofFloored(start.add(direction.multiply(current)));
         if (visited.add(pos) && this.isOpenable(client.world.getBlockState(pos).getBlock())) {
            return pos;
         }
      }

      return null;
   }

   private Direction resolveClickSide(MinecraftClient client, BlockPos target) {
      Vec3d toBlock = Vec3d.ofCenter(target).subtract(client.player.getEyePos());
      return nearestDirection(toBlock.x, toBlock.y, toBlock.z).getOpposite();
   }

   private boolean isOpenable(Block block) {
      return block == Blocks.CHEST
         || block == Blocks.TRAPPED_CHEST
         || block == Blocks.ENDER_CHEST
         || block == Blocks.BARREL
         || block instanceof ShulkerBoxBlock
         || block == Blocks.HOPPER
         || block == Blocks.DISPENSER
         || block == Blocks.DROPPER
         || block == Blocks.FURNACE
         || block == Blocks.BLAST_FURNACE
         || block == Blocks.SMOKER
         || block == Blocks.BREWING_STAND
         || block == Blocks.CRAFTING_TABLE
         || block == Blocks.CARTOGRAPHY_TABLE
         || block == Blocks.SMITHING_TABLE
         || block == Blocks.STONECUTTER
         || block == Blocks.LOOM
         || block == Blocks.GRINDSTONE
         || block == Blocks.ENCHANTING_TABLE
         || block == Blocks.ANVIL
         || block == Blocks.CHIPPED_ANVIL
         || block == Blocks.DAMAGED_ANVIL;
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
