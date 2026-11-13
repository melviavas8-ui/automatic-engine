package daamky.ua.api.module.impl.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.types.EventPhase;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.attack.StrikerConstructor;
import daamky.ua.api.module.impl.combat.aura.impl.LinearConstructor;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.target.MultiPoint;
import daamky.ua.api.module.impl.combat.aura.target.TargetFinder;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.player.BaritoneMovementHelper;

public class TriggerBot extends Module {
   private static TriggerBot instance;
   private final TargetFinder targetSelector = new TargetFinder();
   private final MultiPoint pointFinder = new MultiPoint();
   private final StrikerConstructor attackPerpetrator = new StrikerConstructor();
   public LivingEntity target;
   private int sprintSuppressUntilTick = -1;
   private final NumberSetting attackRange = this.register(new NumberSetting("Attack Range", "Attack distance to the target.", 3.0, 1.0, 6.0, 0.1));
   private final NumberSetting attackDelay = new NumberSetting("Attack Delay", "Minimum delay between attacks.", 0.0, 0.0, 1000.0, 10.0);
   private final MultiModeSetting targetType = this.register(
      new MultiModeSetting(
         "Targets",
         "Target type filter.",
         new String[]{"Players", "Friends", "Mobs", "Animals", "Armor Stands", "Naked Players"},
         "Players",
         "Mobs",
         "Animals",
         "Invisible"
      )
   );
   private final MultiModeSetting attackSetting = this.register(
      new MultiModeSetting(
         "Options",
         "Attack options.",
         new String[]{"Only Critical", "Break Shield", "Release Shield", "Pause While Using", "Ignore Walls", "Hit Chance"},
         "Only Critical",
         "Break Shield"
      )
   );
   private final NumberSetting hitChance = this.register(new NumberSetting("Hit Chance", "Chance to attack the target.", 100.0, 1.0, 100.0, 1.0));
   private final BooleanSetting smartCrits = this.register(
      new BooleanSetting("Smart Crits", "Allow smart ground hits while waiting for critical timing.", false)
   );
   private final BooleanSetting tpsSync = this.register(new BooleanSetting("Sync TPS", "Adjust attack timing to current server TPS.", true));
   private final ModeSetting sprintReset = new ModeSetting("Sprint Reset", "Sprint reset mode before attack.", "Legit", "Legit", "SpookyTime");

   public TriggerBot() {
      super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
      instance = this;
      this.hitChance.visibleWhen(() -> this.attackSetting.isSelected("Hit Chance"));
      this.smartCrits.visibleWhen(() -> this.attackSetting.isSelected("Only Critical"));
   }

   public static TriggerBot getInstance() {
      return instance;
   }

   @Override
   protected void onDisable() {
      this.target = null;
      this.sprintSuppressUntilTick = -1;
      this.attackPerpetrator.getAttackHandler().resetPendingState();
   }

   @SubscribeEvent
   private void onPreTick(TickEvent.Pre event) {
      MinecraftClient client = event.getClient();
      if (client != null && client.player != null && client.world != null) {
         this.attackPerpetrator.tick();
      } else {
         this.targetSelector.releaseTarget();
         this.target = null;
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      this.attackPerpetrator.onPacket(event);
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.isEnabled() && client.player != null && client.world != null && this.target != null) {
         if (!BaritoneMovementHelper.isBaritoneActive(client.player)) {
            StrikeManager attackHandler = this.attackPerpetrator.getAttackHandler();
            StrikerConstructor.AttackPerpetratorConfigurable config = this.getConfig(client);
            boolean shouldPrepareAttack = attackHandler.canAttack(config, 1)
               && client.player.distanceTo(this.target) <= this.attackRange.getFloat()
               && !client.player.isSwimming();
            if (shouldPrepareAttack) {
               event.setSprinting(false);
               client.player.setSprinting(false);
            }
         }
      }
   }

   @SubscribeEvent
   private void onRotationUpdate(RotationUpdateEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null || client.world == null) {
         this.targetSelector.releaseTarget();
         this.target = null;
      } else if (BaritoneMovementHelper.isBaritoneActive(client.player)) {
         this.targetSelector.releaseTarget();
         this.target = null;
      } else if (event.getPhase() == EventPhase.PRE) {
         this.target = this.updateTarget(client);
      } else {
         if (event.getPhase() == EventPhase.POST && this.target != null) {
            this.attackPerpetrator.performTriggerAttack(this.getConfig(client), this);
         }
      }
   }

   private LivingEntity updateTarget(MinecraftClient client) {
      TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(this.targetType.getValue());
      float range = this.attackRange.getFloat();
      this.targetSelector.searchTargets(client.world.getEntities(), range, 360.0F, this.isAttackSettingSelected("Ignore Walls"), filter::isValid);
      this.targetSelector.validateTarget(filter::isValid);
      return this.targetSelector.getCurrentTarget();
   }

   public StrikerConstructor.AttackPerpetratorConfigurable getConfig(MinecraftClient client) {
      float range = this.attackRange.getFloat();
      Pair<Vec3d, Box> point = this.pointFinder
         .computeVector(
            this.target, range, AngleConnection.INSTANCE.getRotation(), this.getSmoothMode().randomValue(), this.isAttackSettingSelected("Ignore Walls")
         );
      Vec3d computedPoint = (Vec3d)point.getLeft();
      Box hitbox = (Box)point.getRight();
      Angle angle = MathAngle.fromVec3d(computedPoint.subtract(client.player.getEyePos()));
      return new StrikerConstructor.AttackPerpetratorConfigurable(
         this.target,
         angle,
         range,
         this.attackSetting.getValue(),
         null,
         hitbox,
         computedPoint,
         this.isAttackSettingSelected("Only Critical"),
         this.tpsSync.getValue(),
         false
      );
   }

   public RotateConstructor getSmoothMode() {
      return LinearConstructor.INSTANCE;
   }

   private boolean isAttackSettingSelected(String... names) {
      for (String name : names) {
         if (this.attackSetting.isSelected(name)) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldPreventSprinting() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (!this.isEnabled() || this.target == null || client.player == null || !this.isOnlyCrits()) {
         return false;
      } else if (client.player.age <= this.sprintSuppressUntilTick) {
         return true;
      } else {
         StrikerConstructor.AttackPerpetratorConfigurable config = this.getConfig(client);
         StrikeManager attackHandler = this.attackPerpetrator.getAttackHandler();
         boolean shouldPrevent = attackHandler.canAttack(config, 4) && !attackHandler.canCrit(config, 4);
         if (shouldPrevent) {
            this.sprintSuppressUntilTick = client.player.age + 1;
         }

         return shouldPrevent;
      }
   }

   public boolean isResetSprintLegit() {
      return this.sprintReset.is("Legit");
   }

   public boolean isResetSprintPacket() {
      return false;
   }

   public boolean isRandomizeCrit() {
      return false;
   }

   public boolean isSmartCritsEnabled() {
      return this.isOnlyCrits() && this.smartCrits.getValue();
   }

   public boolean isOnlyCrits() {
      return this.isAttackSettingSelected("Only Critical");
   }

   public int getAttackDelayMs() {
      return 500;
   }

   public boolean shouldPassHitChance() {
      return true;
   }

   public ModeSetting getSprintReset() {
      return this.sprintReset;
   }

   public long getActivationTimeMs() {
      return 0L;
   }
}
