package daamky.ua.api.module.impl.combat.aura.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;

public final class FromVector {
   private static final Random RANDOM = new Random();
   private static final StopWatch POINT_TIMER = new StopWatch();
   private static final StopWatch UPDATE_TIMER = new StopWatch();
   private static final List<Vec3d> CACHED_OFFSETS = new ArrayList<>();
   private static int currentPointIndex;

   private FromVector() {
   }

   public static Vec3d hitbox(Entity entity, float x, float y, float z, float width) {
      MinecraftClient client = MinecraftClient.getInstance();
      double wHalf = (double)(entity.getWidth() / width);
      double yExpand = MathHelper.clamp(entity.getEyeY() - entity.getY(), 0.0, (double)entity.getHeight());
      double xExpand = MathHelper.clamp(client.player.getX() - entity.getX(), -wHalf, wHalf);
      double zExpand = MathHelper.clamp(client.player.getZ() - entity.getZ(), -wHalf, wHalf);
      return new Vec3d(entity.getX() + xExpand / (double)x, entity.getY() + yExpand / (double)y, entity.getZ() + zExpand / (double)z);
   }

   public static Vec3d closest(Entity entity) {
      if (entity == null) {
         return Vec3d.ZERO;
      } else {
         Vec3d playerEyes = MinecraftClient.getInstance().player.getEyePos();
         double closestX = MathHelper.clamp(playerEyes.x, entity.getBoundingBox().minX, entity.getBoundingBox().maxX);
         double closestY = MathHelper.clamp(playerEyes.y, entity.getBoundingBox().minY, entity.getBoundingBox().maxY);
         double closestZ = MathHelper.clamp(playerEyes.z, entity.getBoundingBox().minZ, entity.getBoundingBox().maxZ);
         return new Vec3d(closestX, closestY, closestZ);
      }
   }

   public static Vec3d getBestPoint(Vec3d pos, Entity entity) {
      return entity == null
         ? Vec3d.ZERO
         : new Vec3d(
            MathHelper.clamp(pos.x, entity.getBoundingBox().minX, entity.getBoundingBox().maxX),
            MathHelper.clamp(pos.y, entity.getBoundingBox().minY, entity.getBoundingBox().maxY),
            MathHelper.clamp(pos.z, entity.getBoundingBox().minZ, entity.getBoundingBox().maxZ)
         );
   }

   public static Vec3d brain(Entity entity, float min, float max) {
      double distance = MinecraftClient.getInstance().player.getEntityPos().distanceTo(entity.getEntityPos());
      double normalizedDistance = MathHelper.clamp((distance - (double)min) / (double)(max - min), 0.0, 1.0);
      double targetHeight = 0.2 + 0.6000000000000001 * normalizedDistance;
      return new Vec3d(entity.getX(), entity.getY() + (double)entity.getHeight() * targetHeight, entity.getZ());
   }

   public static Vec3d custom(Entity entity, int pointCount, float switchDelay) {
      if (entity == null) {
         return Vec3d.ZERO;
      } else {
         if (UPDATE_TIMER.every(1000.0) || CACHED_OFFSETS.isEmpty()) {
            generateRandomPoints(entity, pointCount);
            currentPointIndex = 0;
            POINT_TIMER.reset();
         }

         if (POINT_TIMER.finished((double)switchDelay)) {
            currentPointIndex = (currentPointIndex + 1) % CACHED_OFFSETS.size();
            POINT_TIMER.reset();
         }

         return CACHED_OFFSETS.isEmpty() ? entity.getEntityPos() : entity.getEntityPos().add(CACHED_OFFSETS.get(currentPointIndex));
      }
   }

   private static void generateRandomPoints(Entity entity, int pointCount) {
      CACHED_OFFSETS.clear();
      double width = (double)entity.getWidth();
      double height = (double)entity.getHeight();

      for (int i = 0; i < pointCount; i++) {
         CACHED_OFFSETS.add(new Vec3d((RANDOM.nextDouble() - 0.5) * width, RANDOM.nextDouble() * height, (RANDOM.nextDouble() - 0.5) * width));
      }
   }
}
