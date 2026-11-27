package daamky.ua.api.module.impl.combat.aura.target;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.attack.StrikerConstructor;

public final class RaycastAngle {
   private RaycastAngle() {
   }

   public static BlockHitResult raycast(double range, Angle angle, boolean includeFluids) {
      Minecraft client = Minecraft.getInstance();
      return raycast(Objects.requireNonNull(client.player).getEyePosition(1.0F), range, angle, includeFluids);
   }

   public static BlockHitResult raycast(Vec3 vec, double range, Angle angle, boolean includeFluids) {
      Minecraft client = Minecraft.getInstance();
      Entity entity = client.getCameraEntity();
      if (entity != null && client.level != null) {
         Vec3 rotationVec = angle.toVector();
         Vec3 end = vec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
         Fluid fluidHandling = includeFluids ? Fluid.ANY : Fluid.NONE;
         return client.level.clip(new ClipContext(vec, end, Block.OUTLINE, fluidHandling, entity));
      } else {
         return null;
      }
   }

   public static BlockHitResult raycast(Vec3 start, Vec3 end, Block shapeType) {
      Minecraft client = Minecraft.getInstance();
      return raycast(start, end, shapeType, Fluid.NONE, client.player);
   }

   public static BlockHitResult raycast(Vec3 start, Vec3 end, Block shapeType, Fluid fluidHandling, Entity entity) {
      Minecraft client = Minecraft.getInstance();
      Level level = client.level;
      return level == null ? null : level.clip(new ClipContext(start, end, shapeType, fluidHandling, entity));
   }

   public static EntityHitResult raytraceEntity(double range, Angle angle, Predicate<Entity> filter) {
      Minecraft client = Minecraft.getInstance();
      Entity entity = client.player;
      if (entity == null) {
         return null;
      } else {
         Vec3 cameraVec = entity.getEyePosition(1.0F);
         Vec3 rotationVec = angle.toVector();
         Vec3 end = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
         AABB box = entity.getBoundingBox().expandTowards(rotationVec.scale(range)).inflate(1.0, 1.0, 1.0);
         return ProjectileUtil.getEntityHitResult(entity, cameraVec, end, box, e -> !e.isSpectator() && filter.test(e), range * range);
      }
   }

   public static boolean rayTrace(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && config.getTarget() != null && client.player.isFallFlying() && config.getTarget().isFallFlying()) {
         return true;
      } else {
         boolean ignoreWalls = config.isIgnoreWalls();
         return rayTrace(AngleConnection.INSTANCE.getRotation().toVector(), (double)(config.getMaximumRange() - 0.25F), config.getBox(), ignoreWalls);
      }
   }

   public static boolean rayTrace(double range, AABB box) {
      return rayTrace(AngleConnection.INSTANCE.getRotation().toVector(), range - 0.25, box, false);
   }

   public static boolean rayTrace(Vec3 clientVec, double range, AABB box) {
      return rayTrace(clientVec, range, box, false);
   }

   public static boolean rayTrace(Vec3 clientVec, double range, AABB box, boolean ignoreWalls) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.level != null) {
         Vec3 cameraVec = client.player.getEyePosition();
         Vec3 endVec = cameraVec.add(clientVec.scale(range));
         if (box == null) {
            return false;
         } else {
            Optional<Vec3> targetHit = box.contains(cameraVec) ? Optional.of(cameraVec) : box.clip(cameraVec, endVec);
            if (targetHit.isEmpty()) {
               return false;
            } else if (ignoreWalls) {
               return true;
            } else {
               ClipContext context = new ClipContext(cameraVec, endVec, Block.COLLIDER, Fluid.NONE, client.player);
               BlockHitResult blockHit = client.level.clip(context);
               if (blockHit.getType() == Type.BLOCK) {
                  double blockDistance = cameraVec.distanceTo(blockHit.getLocation());
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
