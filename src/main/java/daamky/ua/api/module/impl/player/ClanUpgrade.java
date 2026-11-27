package daamky.ua.api.module.impl.player;

import daamky.ua.api.drag.impl.Notifications;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.utils.inventory.InventoryTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         BlockPos nearbyRedstone = this.findNearbyRedstone();
         if (nearbyRedstone != null) {
            this.lastPlacedPos = nearbyRedstone;
            this.breakNext = true;
         }

         if (this.breakNext && this.lastPlacedPos != null) {
            this.lookDown();
            if (mc.level.getBlockState(this.lastPlacedPos).isAir()) {
               this.breakNext = false;
               this.lastPlacedPos = null;
            } else {
               this.breakBlock(this.lastPlacedPos, Direction.UP);
            }
         } else if (this.stopWatch.finished(60.0)) {
            int redstoneSlot = InventoryTask.getHotbarSlotId(slot -> mc.player.getInventory().getItem(slot).is(Items.REDSTONE));
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
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         BlockPos placePos = mc.player.blockPosition();
         if (!mc.level.getBlockState(placePos).canBeReplaced()) {
            return null;
         } else {
            int previousSlot = mc.player.getInventory().getSelectedSlot();
            InventoryTask.switchTo(slot);
            this.lookDown();
            boolean placed = false;

            for (Direction direction : Direction.values()) {
               BlockPos neighborPos = placePos.relative(direction);
               if (!mc.level.getBlockState(neighborPos).canBeReplaced()) {
                  Direction placeSide = direction.getOpposite();
                  Vec3 hitPos = Vec3.atCenterOf(neighborPos)
                     .add((double)placeSide.getStepX() * 0.5, (double)placeSide.getStepY() * 0.5, (double)placeSide.getStepZ() * 0.5);
                  BlockHitResult hit = new BlockHitResult(hitPos, placeSide, neighborPos, false);
                  InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                  if (result.consumesAction()) {
                     mc.player.swing(InteractionHand.MAIN_HAND);
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
      if (mc.player != null && mc.gameMode != null) {
         this.lookDown();
         mc.gameMode.startDestroyBlock(pos, side);
         mc.gameMode.continueDestroyBlock(pos, side);
         mc.player.swing(InteractionHand.MAIN_HAND);
      }
   }

   private void lookDown() {
      if (mc.player != null) {
         mc.player.setXRot(90.0F);
      }
   }

   private BlockPos findNearbyRedstone() {
      if (mc.player != null && mc.level != null) {
         BlockPos basePos = mc.player.blockPosition();
         BlockPos bestPos = null;
         double bestDistance = Double.MAX_VALUE;

         for (int y = -1; y <= 0; y++) {
            for (int x = -1; x <= 1; x++) {
               for (int z = -1; z <= 1; z++) {
                  BlockPos pos = basePos.offset(x, y, z);
                  if (mc.level.getBlockState(pos).is(Blocks.REDSTONE_WIRE)) {
                     double distance = pos.distSqr(basePos);
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
