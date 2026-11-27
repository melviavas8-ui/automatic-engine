package daamky.ua.api.module.impl.combat.aura.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;

public final class FromVector {
   private static final Random RANDOM = new Random();
   private static final StopWatch POINT_TIMER = new StopWatch();
   private static final StopWatch UPDATE_TIMER = new StopWatch();
   private static final List<Vec3> CACHED_OFFSETS = new ArrayList<>();
   private static int currentPointIndex;

   private FromVector() {
   }

   public static Vec3 hitbox(Entity entity, float x, float y, float z, float width) {
      Minecraft client = Minecraft.getInstance();
      double wHalf = (double)(entity.getBbWidth() / width);
      double yExpand = Mth.clamp(entity.getEyeY() - entity.getY(), 0.0, (double)entity.getBbHeight());
      double xExpand = Mth.clamp(client.player.getX() - entity.getX(), -wHalf, wHalf);
      double zExpand = Mth.clamp(client.player.getZ() - entity.getZ(), -wHalf, wHalf);
      return new Vec3(entity.getX() + xExpand / (double)x, entity.getY() + yExpand / (double)y, entity.getZ() + zExpand / (double)z);
   }

   public static Vec3 closest(Entity entity) {
      if (entity == null) {
         return Vec3.ZERO;
      } else {
         Vec3 playerEyes = Minecraft.getInstance().player.getEyePosition();
         double closestX = Mth.clamp(playerEyes.x, entity.getBoundingBox().minX, entity.getBoundingBox().maxX);
         double closestY = Mth.clamp(playerEyes.y, entity.getBoundingBox().minY, entity.getBoundingBox().maxY);
         double closestZ = Mth.clamp(playerEyes.z, entity.getBoundingBox().minZ, entity.getBoundingBox().maxZ);
         return new Vec3(closestX, closestY, closestZ);
      }
   }

   public static Vec3 getBestPoint(Vec3 pos, Entity entity) {
      return entity == null
         ? Vec3.ZERO
         : new Vec3(
            Mth.clamp(pos.x, entity.getBoundingBox().minX, entity.getBoundingBox().maxX),
            Mth.clamp(pos.y, entity.getBoundingBox().minY, entity.getBoundingBox().maxY),
            Mth.clamp(pos.z, entity.getBoundingBox().minZ, entity.getBoundingBox().maxZ)
         );
   }

   public static Vec3 brain(Entity entity, float min, float max) {
      double distance = Minecraft.getInstance().player.position().distanceTo(entity.position());
      double normalizedDistance = Mth.clamp((distance - (double)min) / (double)(max - min), 0.0, 1.0);
      double targetHeight = 0.2 + 0.6000000000000001 * normalizedDistance;
      return new Vec3(entity.getX(), entity.getY() + (double)entity.getBbHeight() * targetHeight, entity.getZ());
   }

   public static Vec3 custom(Entity entity, int pointCount, float switchDelay) {
      if (entity == null) {
         return Vec3.ZERO;
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

         return CACHED_OFFSETS.isEmpty() ? entity.position() : entity.position().add(CACHED_OFFSETS.get(currentPointIndex));
      }
   }

   private static void generateRandomPoints(Entity entity, int pointCount) {
      CACHED_OFFSETS.clear();
      double width = (double)entity.getBbWidth();
      double height = (double)entity.getBbHeight();

      for (int i = 0; i < pointCount; i++) {
         CACHED_OFFSETS.add(new Vec3((RANDOM.nextDouble() - 0.5) * width, RANDOM.nextDouble() * height, (RANDOM.nextDouble() - 0.5) * width));
      }
   }
}
