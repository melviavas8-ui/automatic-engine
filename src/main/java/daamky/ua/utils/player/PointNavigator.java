package daamky.ua.utils.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class PointNavigator {
   private static final int MAX_VISITED_NODES = 12000;
   private static final int REPATH_AFTER_STUCK_TICKS = 20;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private BlockPos target;
   private List<BlockPos> path = List.of();
   private int pathIndex;
   private int repathCooldown;
   private int stuckTicks;
   private double lastX;
   private double lastZ;

   public PointNavigator() {
   }

   public void moveTo(BlockPos newTarget) {
      if (newTarget == null) {
         this.stop();
      } else if (!newTarget.equals(this.target)) {
         this.target = newTarget.toImmutable();
         this.path = List.of();
         this.pathIndex = 0;
         this.repathCooldown = 0;
         this.stuckTicks = 0;
         this.rememberPosition();
      }
   }

   public void tick() {
      if (this.target != null && this.mc.player != null && this.mc.world != null && this.mc.options != null) {
         if (this.mc.player.squaredDistanceTo(Vec3d.ofCenter(this.target)) <= 2.25) {
            this.stop();
         } else {
            if (this.repathCooldown > 0) {
               this.repathCooldown--;
            }

            this.updateStuckState();
            if (this.path.isEmpty() || this.pathIndex >= this.path.size() || this.stuckTicks >= 20 && this.repathCooldown == 0) {
               List<BlockPos> newPath = this.findPath(this.mc.player.getBlockPos(), this.target);
               this.path = newPath.isEmpty() ? List.of(this.target) : newPath;
               this.pathIndex = 0;
               this.stuckTicks = 0;
               this.repathCooldown = 10;
            }

            while (this.pathIndex < this.path.size() - 1 && this.reached(this.path.get(this.pathIndex))) {
               this.pathIndex++;
            }

            BlockPos waypoint = this.path.get(Math.min(this.pathIndex, this.path.size() - 1));
            this.driveTowards(waypoint);
         }
      }
   }

   public void stop() {
      this.target = null;
      this.path = List.of();
      this.pathIndex = 0;
      this.repathCooldown = 0;
      this.stuckTicks = 0;
      this.releaseKeys();
   }

   private void driveTowards(BlockPos waypoint) {
      double dx = (double)waypoint.getX() + 0.5 - this.mc.player.getX();
      double dz = (double)waypoint.getZ() + 0.5 - this.mc.player.getZ();
      double length = Math.hypot(dx, dz);
      if (length < 0.01) {
         this.releaseKeys();
      } else {
         dx /= length;
         dz /= length;
         double yaw = Math.toRadians((double)this.mc.player.getYaw());
         double forward = dx * -Math.sin(yaw) + dz * Math.cos(yaw);
         double right = dx * Math.cos(yaw) + dz * Math.sin(yaw);
         this.mc.options.forwardKey.setPressed(forward > 0.2);
         this.mc.options.backKey.setPressed(forward < -0.2);
         this.mc.options.rightKey.setPressed(right > 0.2);
         this.mc.options.leftKey.setPressed(right < -0.2);
         boolean shouldJump = (double)waypoint.getY() > this.mc.player.getY() + 0.25 || this.mc.player.horizontalCollision && this.mc.player.isOnGround();
         this.mc.options.jumpKey.setPressed(shouldJump);
         this.mc.options.sprintKey.setPressed(forward > 0.2);
         this.mc.player.setSprinting(forward > 0.2);
         this.mc.player.setSprinting(true);
         if (shouldJump && this.mc.player.isOnGround()) {
            this.mc.player.jump();
         }
      }
   }

   private List<BlockPos> findPath(BlockPos rawStart, BlockPos goal) {
      BlockPos start = this.nearestWalkable(rawStart);
      BlockPos end = this.nearestWalkable(goal);
      if (start != null && end != null) {
         int horizontalLimit = Math.min(96, Math.abs(end.getX() - start.getX()) + Math.abs(end.getZ() - start.getZ()) + 20);
         int minY = Math.min(start.getY(), end.getY()) - 8;
         int maxY = Math.max(start.getY(), end.getY()) + 8;
         PriorityQueue<PointNavigator.PathNode> open = new PriorityQueue<>(Comparator.comparingDouble(PointNavigator.PathNode::score));
         Map<BlockPos, Double> costs = new HashMap<>();
         Map<BlockPos, BlockPos> parents = new HashMap<>();
         costs.put(start, 0.0);
         open.add(new PointNavigator.PathNode(start, this.heuristic(start, end)));
         int visited = 0;

         while (!open.isEmpty() && visited++ < 12000) {
            PointNavigator.PathNode currentNode = open.poll();
            BlockPos current = currentNode.pos();
            double currentCost = costs.getOrDefault(current, Double.POSITIVE_INFINITY);
            if (!(currentNode.score() > currentCost + this.heuristic(current, end) + 0.001)) {
               if (current.equals(end)) {
                  return this.reconstruct(parents, start, end);
               }

               for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                  BlockPos next = this.findNeighbor(current, direction[0], direction[1]);
                  if (next != null
                     && next.getY() >= minY
                     && next.getY() <= maxY
                     && Math.abs(next.getX() - start.getX()) <= horizontalLimit
                     && Math.abs(next.getZ() - start.getZ()) <= horizontalLimit) {
                     double nextCost = currentCost + 1.0 + (double)Math.abs(next.getY() - current.getY()) * 0.35;
                     if (!(nextCost + 0.001 >= costs.getOrDefault(next, Double.POSITIVE_INFINITY))) {
                        costs.put(next, nextCost);
                        parents.put(next, current);
                        open.add(new PointNavigator.PathNode(next, nextCost + this.heuristic(next, end)));
                     }
                  }
               }
            }
         }

         return List.of();
      } else {
         return List.of();
      }
   }

   private BlockPos findNeighbor(BlockPos current, int dx, int dz) {
      for (int dy : new int[]{0, 1, -1}) {
         BlockPos candidate = current.add(dx, dy, dz);
         if (this.isWalkable(candidate)) {
            return candidate;
         }
      }

      return null;
   }

   private BlockPos nearestWalkable(BlockPos pos) {
      if (this.isWalkable(pos)) {
         return pos.toImmutable();
      } else {
         for (int radius = 1; radius <= 2; radius++) {
            for (int x = -radius; x <= radius; x++) {
               for (int z = -radius; z <= radius; z++) {
                  for (int y = 1; y >= -1; y--) {
                     BlockPos candidate = pos.add(x, y, z);
                     if (this.isWalkable(candidate)) {
                        return candidate.toImmutable();
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   private boolean isWalkable(BlockPos pos) {
      if (!this.mc.world.isChunkLoaded(pos)) {
         return false;
      } else {
         BlockState feet = this.mc.world.getBlockState(pos);
         BlockState head = this.mc.world.getBlockState(pos.up());
         BlockState floor = this.mc.world.getBlockState(pos.down());
         return feet.getCollisionShape(this.mc.world, pos).isEmpty()
            && head.getCollisionShape(this.mc.world, pos.up()).isEmpty()
            && !floor.getCollisionShape(this.mc.world, pos.down()).isEmpty()
            && feet.getFluidState().isEmpty()
            && head.getFluidState().isEmpty();
      }
   }

   private List<BlockPos> reconstruct(Map<BlockPos, BlockPos> parents, BlockPos start, BlockPos end) {
      List<BlockPos> result = new ArrayList<>();
      BlockPos cursor = end;

      while (!cursor.equals(start)) {
         result.add(cursor);
         cursor = parents.get(cursor);
         if (cursor == null) {
            return List.of();
         }
      }

      Collections.reverse(result);
      return result;
   }

   private double heuristic(BlockPos from, BlockPos to) {
      return (double)(Math.abs(from.getX() - to.getX()) + Math.abs(from.getZ() - to.getZ())) + (double)Math.abs(from.getY() - to.getY()) * 0.35;
   }

   private boolean reached(BlockPos pos) {
      double dx = (double)pos.getX() + 0.5 - this.mc.player.getX();
      double dz = (double)pos.getZ() + 0.5 - this.mc.player.getZ();
      return dx * dx + dz * dz < 0.2025 && Math.abs((double)pos.getY() - this.mc.player.getY()) < 1.25;
   }

   private void updateStuckState() {
      double dx = this.mc.player.getX() - this.lastX;
      double dz = this.mc.player.getZ() - this.lastZ;
      if (dx * dx + dz * dz < 4.0E-4) {
         this.stuckTicks++;
      } else {
         this.stuckTicks = 0;
      }

      this.rememberPosition();
   }

   private void rememberPosition() {
      if (this.mc.player != null) {
         this.lastX = this.mc.player.getX();
         this.lastZ = this.mc.player.getZ();
      }
   }

   private void releaseKeys() {
      if (this.mc.options != null) {
         this.mc.options.forwardKey.setPressed(false);
         this.mc.options.backKey.setPressed(false);
         this.mc.options.leftKey.setPressed(false);
         this.mc.options.rightKey.setPressed(false);
         this.mc.options.jumpKey.setPressed(false);
         this.mc.options.sprintKey.setPressed(false);
         if (this.mc.player != null) {
            this.mc.player.setSprinting(false);
         }
      }
   }

   private static record PathNode(BlockPos pos, double score) {
   }
}
