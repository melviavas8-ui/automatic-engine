package daamky.ua.api.module.impl.combat.aura.target;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.attack.StrikerConstructor;

public final class RaycastAngle {
   private RaycastAngle() {
   }

   public static BlockHitResult raycast(double range, Angle angle, boolean includeFluids) {
      MinecraftClient client = MinecraftClient.getInstance();
      return raycast(Objects.requireNonNull(client.player).getCameraPosVec(1.0F), range, angle, includeFluids);
   }

   public static BlockHitResult raycast(Vec3d vec, double range, Angle angle, boolean includeFluids) {
      MinecraftClient client = MinecraftClient.getInstance();
      Entity entity = client.getCameraEntity();
      if (entity != null && client.world != null) {
         Vec3d rotationVec = angle.toVector();
         Vec3d end = vec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
         FluidHandling fluidHandling = includeFluids ? FluidHandling.ANY : FluidHandling.NONE;
         return client.world.raycast(new RaycastContext(vec, end, ShapeType.OUTLINE, fluidHandling, entity));
      } else {
         return null;
      }
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType) {
      MinecraftClient client = MinecraftClient.getInstance();
      return raycast(start, end, shapeType, FluidHandling.NONE, client.player);
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType, FluidHandling fluidHandling, Entity entity) {
      MinecraftClient client = MinecraftClient.getInstance();
      World level = client.world;
      return level == null ? null : level.raycast(new RaycastContext(start, end, shapeType, fluidHandling, entity));
   }

   public static EntityHitResult raytraceEntity(double range, Angle angle, Predicate<Entity> filter) {
      MinecraftClient client = MinecraftClient.getInstance();
      Entity entity = client.player;
      if (entity == null) {
         return null;
      } else {
         Vec3d cameraVec = entity.getCameraPosVec(1.0F);
         Vec3d rotationVec = angle.toVector();
         Vec3d end = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
         Box box = entity.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0);
         return ProjectileUtil.raycast(entity, cameraVec, end, box, e -> !e.isSpectator() && filter.test(e), range * range);
      }
   }

   public static boolean rayTrace(StrikerConstructor.AttackPerpetratorConfigurable config) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && config.getTarget() != null && client.player.isGliding() && config.getTarget().isGliding()) {
         return true;
      } else {
         boolean ignoreWalls = config.isIgnoreWalls();
         return rayTrace(AngleConnection.INSTANCE.getRotation().toVector(), (double)(config.getMaximumRange() - 0.25F), config.getBox(), ignoreWalls);
      }
   }

   public static boolean rayTrace(double range, Box box) {
      return rayTrace(AngleConnection.INSTANCE.getRotation().toVector(), range - 0.25, box, false);
   }

   public static boolean rayTrace(Vec3d clientVec, double range, Box box) {
      return rayTrace(clientVec, range, box, false);
   }

   public static boolean rayTrace(Vec3d clientVec, double range, Box box, boolean ignoreWalls) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null) {
         Vec3d cameraVec = client.player.getEyePos();
         Vec3d endVec = cameraVec.add(clientVec.multiply(range));
         if (box == null) {
            return false;
         } else {
            Optional<Vec3d> targetHit = box.contains(cameraVec) ? Optional.of(cameraVec) : box.raycast(cameraVec, endVec);
            if (targetHit.isEmpty()) {
               return false;
            } else if (ignoreWalls) {
               return true;
            } else {
               RaycastContext context = new RaycastContext(cameraVec, endVec, ShapeType.COLLIDER, FluidHandling.NONE, client.player);
               BlockHitResult blockHit = client.world.raycast(context);
               if (blockHit.getType() == Type.BLOCK) {
                  double blockDistance = cameraVec.distanceTo(blockHit.getPos());
                  double targetDistance = cameraVec.distanceTo(targetHit.get());
                  return blockDistance + 1.0E-4 >= targetDistance;
               } else {
                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }
}
