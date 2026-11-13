package daamky.ua.api.module.impl.player;

import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.drag.impl.Notifications;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.utils.inventory.InventoryTask;

public final class ClanUpgrade extends Module {
   private static final long PLACE_DELAY_MS = 60L;
   private final StopWatch stopWatch = new StopWatch();
   private final StopWatch notifyWatch = new StopWatch();
   private BlockPos lastPlacedPos;
   private boolean breakNext;

   public ClanUpgrade() {
      super("ClanUpgrade", "Clan Upgrade", ModuleCategory.PLAYER);
   }

   @SubscribeEvent
   private void onPostTick(TickEvent.Post event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         BlockPos nearbyRedstone = this.findNearbyRedstone();
         if (nearbyRedstone != null) {
            this.lastPlacedPos = nearbyRedstone;
            this.breakNext = true;
         }

         if (this.breakNext && this.lastPlacedPos != null) {
            this.lookDown();
            if (mc.world.getBlockState(this.lastPlacedPos).isAir()) {
               this.breakNext = false;
               this.lastPlacedPos = null;
            } else {
               this.breakBlock(this.lastPlacedPos, Direction.UP);
            }
         } else if (this.stopWatch.finished(60.0)) {
            int redstoneSlot = InventoryTask.getHotbarSlotId(slot -> mc.player.getInventory().getStack(slot).isOf(Items.REDSTONE));
            if (redstoneSlot == -1) {
               if (this.notifyWatch.finished(1000.0)) {
                  Notifications.push("ClanUpgrade", "Нужен редстоун в хотбаре", 1000L);
                  this.notifyWatch.reset();
               }
            } else {
               BlockPos placed = this.placeUnderPlayer(redstoneSlot);
               if (placed != null) {
                  this.lastPlacedPos = placed;
                  this.breakNext = true;
               }

               this.stopWatch.reset();
            }
         }
      }
   }

   @Override
   protected void onEnable() {
      this.lastPlacedPos = null;
      this.breakNext = false;
      this.stopWatch.reset();
      this.notifyWatch.reset();
   }

   @Override
   protected void onDisable() {
      this.lastPlacedPos = null;
      this.breakNext = false;
   }

   private BlockPos placeUnderPlayer(int slot) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         BlockPos placePos = mc.player.getBlockPos();
         if (!mc.world.getBlockState(placePos).isReplaceable()) {
            return null;
         } else {
            int previousSlot = mc.player.getInventory().getSelectedSlot();
            InventoryTask.switchTo(slot);
            this.lookDown();
            boolean placed = false;

            for (Direction direction : Direction.values()) {
               BlockPos neighborPos = placePos.offset(direction);
               if (!mc.world.getBlockState(neighborPos).isReplaceable()) {
                  Direction placeSide = direction.getOpposite();
                  Vec3d hitPos = Vec3d.ofCenter(neighborPos)
                     .add((double)placeSide.getOffsetX() * 0.5, (double)placeSide.getOffsetY() * 0.5, (double)placeSide.getOffsetZ() * 0.5);
                  BlockHitResult hit = new BlockHitResult(hitPos, placeSide, neighborPos, false);
                  ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                  if (result.isAccepted()) {
                     mc.player.swingHand(Hand.MAIN_HAND);
                     placed = true;
                     break;
                  }
               }
            }

            InventoryTask.switchTo(previousSlot);
            return placed ? placePos : null;
         }
      } else {
         return null;
      }
   }

   private void breakBlock(BlockPos pos, Direction side) {
      if (mc.player != null && mc.interactionManager != null) {
         this.lookDown();
         mc.interactionManager.attackBlock(pos, side);
         mc.interactionManager.updateBlockBreakingProgress(pos, side);
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private void lookDown() {
      if (mc.player != null) {
         mc.player.setPitch(90.0F);
      }
   }

   private BlockPos findNearbyRedstone() {
      if (mc.player != null && mc.world != null) {
         BlockPos basePos = mc.player.getBlockPos();
         BlockPos bestPos = null;
         double bestDistance = Double.MAX_VALUE;

         for (int y = -1; y <= 0; y++) {
            for (int x = -1; x <= 1; x++) {
               for (int z = -1; z <= 1; z++) {
                  BlockPos pos = basePos.add(x, y, z);
                  if (mc.world.getBlockState(pos).isOf(Blocks.REDSTONE_WIRE)) {
                     double distance = pos.getSquaredDistance(basePos);
                     if (distance < bestDistance) {
                        bestDistance = distance;
                        bestPos = pos;
                     }
                  }
               }
            }
         }

         return bestPos;
      } else {
         return null;
      }
   }
}
