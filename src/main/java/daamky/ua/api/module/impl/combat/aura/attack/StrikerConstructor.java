package daamky.ua.api.module.impl.combat.aura.attack;

import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import daamky.ua.IMinecraft;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.UsingItemEvent;
import daamky.ua.api.module.impl.combat.TriggerBot;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.settings.impl.ModeSetting;

public class StrikerConstructor implements IMinecraft {
   private StrikeManager attackHandler = new StrikeManager();

   public StrikerConstructor() {
   }

   public void tick() {
      this.getAttackHandler().tick();
   }

   public void onPacket(PacketEvent event) {
      this.getAttackHandler().onPacket(event);
   }

   public void performAttack(StrikerConstructor.AttackPerpetratorConfigurable configurable) {
      this.getAttackHandler().handleAttack(configurable);
   }

   public void performTriggerAttack(StrikerConstructor.AttackPerpetratorConfigurable configurable, TriggerBot triggerBot) {
      this.getAttackHandler().handleTriggerAttack(configurable, triggerBot);
   }

   public void onUsingItem(UsingItemEvent event) {
      this.getAttackHandler().onUsingItem(event);
   }

   public StrikeManager getAttackHandler() {
      if (this.attackHandler == null) {
         this.attackHandler = new StrikeManager();
      }

      return this.attackHandler;
   }

   public static class AttackPerpetratorConfigurable {
      private final LivingEntity target;
      private final Angle angle;
      private final float maximumRange;
      private final boolean onlyCritical;
      private final boolean shouldBreakShield;
      private final boolean shouldUnPressShield;
      private final boolean eatAndAttack;
      private final boolean multiPoints;
      private final boolean ignoreWalls;
      private final boolean tpsSync;
      private final boolean legacyPvp;
      private final AABB box;
      private final Vec3 aimPoint;
      private final ModeSetting aimMode;

      public AttackPerpetratorConfigurable(LivingEntity target, Angle angle, float maximumRange, Set<String> options, ModeSetting aimMode, AABB box) {
         this(target, angle, maximumRange, options, aimMode, box, null, options.contains("Only Crits"), false, false);
      }

      public AttackPerpetratorConfigurable(
         LivingEntity target, Angle angle, float maximumRange, Set<String> options, ModeSetting aimMode, AABB box, boolean onlyCritical
      ) {
         this(target, angle, maximumRange, options, aimMode, box, null, onlyCritical, false, false);
      }

      public AttackPerpetratorConfigurable(
         LivingEntity target, Angle angle, float maximumRange, Set<String> options, ModeSetting aimMode, AABB box, Vec3 aimPoint, boolean onlyCritical
      ) {
         this(target, angle, maximumRange, options, aimMode, box, aimPoint, onlyCritical, false, false);
      }

      public AttackPerpetratorConfigurable(
         LivingEntity target,
         Angle angle,
         float maximumRange,
         Set<String> options,
         ModeSetting aimMode,
         AABB box,
         boolean onlyCritical,
         boolean tpsSync,
         boolean legacyPvp
      ) {
         this(target, angle, maximumRange, options, aimMode, box, null, onlyCritical, tpsSync, legacyPvp);
      }

      public AttackPerpetratorConfigurable(
         LivingEntity target,
         Angle angle,
         float maximumRange,
         Set<String> options,
         ModeSetting aimMode,
         AABB box,
         Vec3 aimPoint,
         boolean onlyCritical,
         boolean tpsSync,
         boolean legacyPvp
      ) {
         Set<String> safeOptions = options == null ? Set.of() : options;
         this.target = target;
         this.angle = angle;
         this.maximumRange = maximumRange;
         this.onlyCritical = onlyCritical;
         this.shouldBreakShield = safeOptions.contains("Break Shield");
         this.shouldUnPressShield = safeOptions.contains("Release Shield");
         this.eatAndAttack = safeOptions.contains("Pause While Using");
         this.multiPoints = safeOptions.contains("Multi Points");
         this.ignoreWalls = safeOptions.contains("Ignore Walls");
         this.tpsSync = tpsSync;
         this.legacyPvp = legacyPvp;
         this.box = box;
         this.aimPoint = aimPoint;
         this.aimMode = aimMode;
      }

      public LivingEntity getTarget() {
         return this.target;
      }

      public Angle getAngle() {
         return this.angle;
      }

      public float getMaximumRange() {
         return this.maximumRange;
      }

      public boolean isOnlyCritical() {
         return this.onlyCritical;
      }

      public boolean isShouldBreakShield() {
         return this.shouldBreakShield;
      }

      public boolean isShouldUnPressShield() {
         return this.shouldUnPressShield;
      }

      public boolean isEatAndAttack() {
         return this.eatAndAttack;
      }

      public boolean isMultiPoints() {
         return this.multiPoints;
      }

      public boolean isIgnoreWalls() {
         return this.ignoreWalls;
      }

      public boolean isTpsSync() {
         return this.tpsSync;
      }

      public boolean isLegacyPvp() {
         return this.legacyPvp;
      }

      public AABB getBox() {
         return this.box;
      }

      public Vec3 getAimPoint() {
         return this.aimPoint;
      }

      public ModeSetting getAimMode() {
         return this.aimMode;
      }
   }
}
