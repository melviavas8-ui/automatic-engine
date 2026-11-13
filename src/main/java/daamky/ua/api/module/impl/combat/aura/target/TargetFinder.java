package daamky.ua.api.module.impl.combat.aura.target;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import daamky.ua.IMinecraft;
import daamky.ua.api.module.impl.combat.AntiBot;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.impl.LinearConstructor;
import daamky.ua.utils.repository.friend.FriendUtils;

public class TargetFinder implements IMinecraft {
   private final MultiPoint pointFinder = new MultiPoint();
   private LivingEntity currentTarget = null;
   private Stream<LivingEntity> potentialTargets;

   public TargetFinder() {
   }

   public LivingEntity getCurrentTarget() {
      return this.currentTarget;
   }

   public void lockTarget(LivingEntity target) {
      if (this.currentTarget == null) {
         this.currentTarget = target;
      }
   }

   public void releaseTarget() {
      this.currentTarget = null;
   }

   public void validateTarget(Predicate<LivingEntity> predicate) {
      this.findFirstMatch(predicate).ifPresent(this::lockTarget);
      if (this.currentTarget != null && !predicate.test(this.currentTarget)) {
         this.releaseTarget();
      }
   }

   public void searchTargets(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls) {
      this.searchTargets(entities, maxDistance, maxFov, ignoreWalls, entity -> true);
   }

   public void searchTargets(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls, Predicate<LivingEntity> predicate) {
      if (this.currentTarget != null
         && (
            !this.pointFinder.hasValidPoint(this.currentTarget, maxDistance, ignoreWalls)
               || this.getFov(this.currentTarget, maxDistance, ignoreWalls) > (double)maxFov
         )) {
         this.releaseTarget();
      }

      this.potentialTargets = this.createStreamFromEntities(entities, maxDistance, maxFov, ignoreWalls, predicate);
   }

   private double getFov(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Vec3d attackVector = (Vec3d)this.pointFinder
         .computeVector(entity, maxDistance, AngleConnection.INSTANCE.getRotation(), new LinearConstructor().randomValue(), ignoreWalls)
         .getLeft();
      return AngleConnection.computeRotationDifference(MathAngle.cameraAngle(), MathAngle.calculateAngle(attackVector));
   }

   private Stream<LivingEntity> createStreamFromEntities(
      Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls, Predicate<LivingEntity> predicate
   ) {
      return mc.player == null
         ? Stream.empty()
         : StreamSupport.stream(entities.spliterator(), false)
            .filter(LivingEntity.class::isInstance)
            .map(LivingEntity.class::cast)
            .filter(
               entity -> this.pointFinder.hasValidPoint(entity, maxDistance, ignoreWalls) && this.getFov(entity, maxDistance, ignoreWalls) < (double)maxFov
            )
            .filter(predicate)
            .sorted(Comparator.comparingDouble(entity -> (double)entity.distanceTo(mc.player)));
   }

   private Optional<LivingEntity> findFirstMatch(Predicate<LivingEntity> predicate) {
      return this.potentialTargets == null ? Optional.empty() : this.potentialTargets.filter(predicate).findFirst();
   }

   public static class EntityFilter {
      private final List<String> targetSettings;

      public EntityFilter(Collection<String> targetSettings) {
         this.targetSettings = new ArrayList<>(targetSettings);
      }

      public boolean isValid(LivingEntity entity) {
         if (this.isLocalPlayer(entity)) {
            return false;
         } else if (this.isInvalidHealth(entity)) {
            return false;
         } else {
            return this.isBotPlayer(entity) ? false : this.isValidEntityType(entity);
         }
      }

      private boolean isLocalPlayer(LivingEntity entity) {
         return entity == MinecraftClient.getInstance().player;
      }

      private boolean isInvalidHealth(LivingEntity entity) {
         return !entity.isAlive() || entity.getHealth() <= 0.0F;
      }

      private boolean isBotPlayer(LivingEntity entity) {
         if (entity instanceof PlayerEntity player && AntiBot.shouldIgnore(player)) {
            return true;
         }

         return false;
      }

      private boolean isNakedPlayer(LivingEntity entity) {
         if (entity instanceof PlayerEntity player) {
            for (int i = 36; i < 40; i++) {
               if (!player.getInventory().getStack(i).isEmpty()) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }

      private boolean isValidEntityType(LivingEntity entity) {
         Objects.requireNonNull(entity);

         return switch (entity) {
            case PlayerEntity player -> FriendUtils.isFriend(player)
            ? this.targetSettings.contains("Friends")
            : (this.isNakedPlayer(player) ? this.targetSettings.contains("Naked Players") : this.targetSettings.contains("Players"));
            case AnimalEntity animal -> this.targetSettings.contains("Animals");
            case MobEntity mob -> this.targetSettings.contains("Mobs");
            case ArmorStandEntity stand -> this.targetSettings.contains("Armor Stand");
            default -> false;
         };
      }
   }
}
