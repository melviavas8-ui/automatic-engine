package daamky.ua.api.module.impl.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.DrawEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.repository.friend.FriendUtils;

public class AimAssist extends Module {
   private static AimAssist instance;
   private final MultiModeSetting targetType = this.register(
      new MultiModeSetting(
         "Targets", "Target type filter.", new String[]{"Players", "Naked Players", "Mobs", "Animals", "Friends", "Armor Stands"}, "Players", "Mobs", "Animals"
      )
   );
   private final NumberSetting distanceSetting = this.register(new NumberSetting("Distance", "Maximum target distance.", 6.0, 1.0, 6.0, 0.1));
   private final NumberSetting fovSetting = this.register(new NumberSetting("FOV", "Target capture field of view.", 360.0, 20.0, 360.0, 1.0));
   private final BooleanSetting drawFovRadiusSetting = this.register(new BooleanSetting("Draw FOV", "Draws aim assist FOV circle.", false));
   private final NumberSetting speedSetting = this.register(new NumberSetting("Speed", "Base aiming speed.", 4.0, 0.5, 10.0, 0.1));
   private final NumberSetting smoothnessSetting = this.register(new NumberSetting("Smoothness", "Aiming smoothing amount.", 6.0, 1.0, 10.0, 0.1));
   private final NumberSetting aimHeightSetting = this.register(new NumberSetting("Aim Height", "Aim point height on target.", 0.85, 0.0, 1.0, 0.01));
   private final ModeSetting prioritySetting = this.register(new ModeSetting("Priority", "Target priority.", "Distance", "Distance", "Health"));
   private final BooleanSetting onlyWithWeaponSetting = this.register(new BooleanSetting("Only Weapon", "Aim only while holding a weapon.", false));
   private final BooleanSetting onlyPlayersSetting = this.register(new BooleanSetting("Only Players", "Aim only at players.", false));
   private final BooleanSetting noAimInInventorySetting = this.register(
      new BooleanSetting("No Inventory Aim", "Do not aim while a container screen is open.", true)
   );
   private final BooleanSetting hitInvisibleSetting = this.register(new BooleanSetting("Invisible", "Aim at invisible entities.", false));
   private final BooleanSetting onlyArmoredSetting = this.register(new BooleanSetting("Only Armored", "Aim only at armored players.", false));
   private final BooleanSetting ignoreNakedSetting = this.register(new BooleanSetting("Ignore Naked", "Ignore players without armor.", false));
   private final BooleanSetting onlyYawSetting = this.register(new BooleanSetting("Only Yaw", "Only change horizontal rotation.", true));
   private final BooleanSetting multipointSetting = this.register(new BooleanSetting("Multipoint", "Use randomized aim points inside the hitbox.", true));
   private final BooleanSetting wallCheckSetting = this.register(new BooleanSetting("Wall Check", "Skip targets behind full blocks.", false));
   private final BooleanSetting disableOnWorldChangeSetting = this.register(new BooleanSetting("Disable On World", "Disable module after world change.", false));
   private final BooleanSetting microMovementsSetting = this.register(
      new BooleanSetting("Micro Movements", "Adds small aiming noise and profile variation.", true)
   );
   private static LivingEntity syncedTarget;
   private LivingEntity target;
   private long targetAcquiredAtMs;
   private long lastFrameNs;
   private long lastRenderAimNs;
   private double previousYawStep;
   private double previousPitchStep;
   private double aimYawVelocity;
   private double aimPitchVelocity;
   private double yawWavePhase;
   private double pitchWavePhase;
   private double yawWaveSpeed;
   private double pitchWaveSpeed;
   private double speedMultiplier;
   private double heightNoise;
   private long nextHeightNoiseUpdateMs;
   private double currentPointX;
   private double currentPointY;
   private double currentPointZ;
   private double nextPointX;
   private double nextPointY;
   private double nextPointZ;
   private long nextPointChangeAtMs;
   private double pointLerpSpeed;
   private RegistryKey<World> worldKey;

   public AimAssist() {
      super("Aim Assist", "Smoothly assists aiming at nearby targets.", ModuleCategory.COMBAT);
      this.ignoreNakedSetting.visibleWhen(() -> !this.onlyArmoredSetting.getValue());
      this.drawFovRadiusSetting.visibleWhen(() -> false);
      this.aimHeightSetting.visibleWhen(() -> false);
      this.onlyYawSetting.visibleWhen(() -> false);
      this.multipointSetting.visibleWhen(() -> false);
      this.disableOnWorldChangeSetting.visibleWhen(() -> false);
      this.microMovementsSetting.visibleWhen(() -> false);
      this.enforceLockedSettings();
      instance = this;
   }

   public static AimAssist getInstance() {
      return instance;
   }

   public static LivingEntity getSyncedTarget() {
      return syncedTarget;
   }

   @Override
   protected void onEnable() {
      this.enforceLockedSettings();
      this.resetState();
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   @Override
   public void onTick(MinecraftClient client) {
      this.enforceLockedSettings();
      long now = System.nanoTime();
      if (this.lastRenderAimNs == 0L || now - this.lastRenderAimNs > 75000000L) {
         this.updateAim(client);
      }
   }

   @SubscribeEvent
   private void onDraw(DrawEvent event) {
      if (event.getLayer() == DrawEvent.Layer.GAME) {
         this.lastRenderAimNs = System.nanoTime();
         this.updateAim(MinecraftClient.getInstance());
      }

      this.drawFovRadius();
   }

   private void updateAim(MinecraftClient client) {
      if (client != null && client.player != null && client.world != null) {
         RegistryKey<World> currentWorldKey = client.world.getRegistryKey();
         if (this.worldKey == null) {
            this.worldKey = currentWorldKey;
         } else if (!this.worldKey.equals(currentWorldKey)) {
            if (this.disableOnWorldChangeSetting.getValue()) {
               this.setEnabled(false);
               return;
            }

            this.resetMotionState();
            this.worldKey = currentWorldKey;
            return;
         }

         if ((!this.onlyWithWeaponSetting.getValue() || this.isHoldingWeapon(client))
            && (!this.noAimInInventorySetting.getValue() || !(client.currentScreen instanceof HandledScreen))) {
            long frameNs = System.nanoTime();
            double frameScale = this.lastFrameNs > 0L ? (double)(frameNs - this.lastFrameNs) / 1.66666667E7 : 1.0;
            frameScale = MathHelper.clamp(frameScale, 0.05, 3.0);
            this.lastFrameNs = frameNs;
            LivingEntity selectedTarget = this.findTarget(client);
            if (selectedTarget == null) {
               this.resetMotionState();
            } else {
               if (selectedTarget != this.target) {
                  this.target = selectedTarget;
                  syncedTarget = selectedTarget;
                  this.targetAcquiredAtMs = System.currentTimeMillis();
                  this.randomizeProfile(this.targetAcquiredAtMs);
               } else {
                  syncedTarget = selectedTarget;
               }

               long nowMs = System.currentTimeMillis();
               this.updateHumanization(nowMs, frameScale);
               this.rotateToTarget(client, selectedTarget, frameScale, nowMs);
            }
         } else {
            this.resetMotionState();
         }
      } else {
         this.resetState();
      }
   }

   private void rotateToTarget(MinecraftClient client, LivingEntity selectedTarget, double frameScale, long nowMs) {
      Vec3d eyePos = client.player.getEyePos();
      Vec3d targetPoint = this.getAimPoint(selectedTarget);
      Vec3d delta = targetPoint.subtract(eyePos);
      double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      if (!(horizontal < 1.0E-4)) {
         float currentYaw = client.player.getYaw();
         float currentPitch = client.player.getPitch();
         double targetYaw = Math.toDegrees(Math.atan2(-delta.x, delta.z));
         double targetPitch = MathHelper.clamp(-Math.toDegrees(Math.atan2(delta.y, horizontal)), -90.0, 90.0);
         double yawDiff = MathHelper.wrapDegrees(targetYaw - (double)currentYaw);
         double pitchDiff = this.onlyYawSetting.getValue() ? 0.0 : targetPitch - (double)currentPitch;
         double response = MathHelper.clamp(1.0 / this.smoothnessSetting.getValue(), 0.08, 1.0);
         double maxYawStep = Math.max(0.35, this.speedSetting.getValue() * frameScale * this.speedMultiplier);
         double maxPitchStep = this.onlyYawSetting.getValue() ? 0.0 : Math.max(0.25, this.speedSetting.getValue() * frameScale * 0.8 * this.speedMultiplier);
         double followStrength = MathHelper.clamp(0.18 * frameScale + response * 0.45, 0.08, 1.0);
         this.aimYawVelocity = this.smoothStep(this.aimYawVelocity, yawDiff * response, followStrength);
         this.aimPitchVelocity = this.smoothStep(this.aimPitchVelocity, pitchDiff * response, followStrength);
         double yawStep = MathHelper.clamp(this.aimYawVelocity, -maxYawStep, maxYawStep);
         double pitchStep = MathHelper.clamp(this.aimPitchVelocity, -maxPitchStep, maxPitchStep);
         if (this.microMovementsSetting.getValue()) {
            double secondsOnTarget = (double)(nowMs - this.targetAcquiredAtMs) / 1000.0;
            yawStep += Math.sin(this.yawWavePhase + secondsOnTarget * this.yawWaveSpeed * 6.0) * 0.08 * frameScale;
            pitchStep += Math.cos(this.pitchWavePhase + secondsOnTarget * this.pitchWaveSpeed * 6.0) * 0.05 * frameScale;
         }

         yawStep = this.clampToRemaining(this.clampStep(yawStep, this.previousYawStep, maxYawStep * 0.9), yawDiff);
         pitchStep = this.clampToRemaining(this.clampStep(pitchStep, this.previousPitchStep, Math.max(maxPitchStep * 0.9, 0.1)), pitchDiff);
         if (Math.abs(yawDiff) < 0.02) {
            yawStep = yawDiff;
         }

         if (Math.abs(pitchDiff) < 0.02) {
            pitchStep = pitchDiff;
         }

         float newYaw = currentYaw + (float)yawStep;
         float newPitch = MathHelper.clamp(currentPitch + (float)pitchStep, -90.0F, 90.0F);
         if (!Float.isNaN(newYaw) && !Float.isNaN(newPitch)) {
            client.player.setYaw(newYaw);
            client.player.setPitch(newPitch);
            this.previousYawStep = yawStep;
            this.previousPitchStep = pitchStep;
         }
      }
   }

   private LivingEntity findTarget(MinecraftClient client) {
      double maxDistance = this.distanceSetting.getValue();
      double halfFov = this.fovSetting.getValue() / 2.0;
      boolean healthPriority = this.prioritySetting.isSelected("Health");
      LivingEntity bestTarget = null;
      double bestScore = Double.MAX_VALUE;
      Vec3d eyePos = client.player.getEyePos();
      Vec3d lookVec = client.player.getRotationVector();

      for (Entity entity : client.world.getEntities()) {
         if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            if (this.isValidTarget(entity, livingEntity)) {
               double distance = (double)client.player.distanceTo(entity);
               if (!(distance > maxDistance)
                  && !(distance < 0.5)
                  && (!this.wallCheckSetting.getValue() || !this.isBlockedByWall(client, eyePos, this.getAimPoint(entity)))
                  && !(this.getAngleTo(eyePos, lookVec, entity) > halfFov)) {
                  double score = healthPriority
                     ? (double)livingEntity.getHealth() + distance * 0.05
                     : distance + this.getAngleTo(eyePos, lookVec, entity) * 0.01;
                  if (score < bestScore) {
                     bestScore = score;
                     bestTarget = livingEntity;
                  }
               }
            }
         }
      }

      return bestTarget;
   }

   private boolean isValidTarget(Entity entity, LivingEntity livingEntity) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null || entity == client.player || !entity.isAlive() || livingEntity.isInvulnerable() || livingEntity.getHealth() <= 0.0F) {
         return false;
      } else if (this.onlyPlayersSetting.getValue() && !(entity instanceof PlayerEntity)) {
         return false;
      } else if (!this.hitInvisibleSetting.getValue() && livingEntity.isInvisible()) {
         return false;
      } else if (entity instanceof PlayerEntity player) {
         if (AntiBot.shouldIgnore(player)) {
            return false;
         } else if (this.targetType.isSelected("Players") && !player.isCreative()) {
            boolean hasArmor = this.hasArmor(livingEntity);
            if (!this.targetType.isSelected("Naked Players") && !hasArmor) {
               return false;
            } else if (this.onlyArmoredSetting.getValue() && !hasArmor) {
               return false;
            } else if (this.ignoreNakedSetting.getValue() && !hasArmor) {
               return false;
            } else {
               return FriendUtils.isFriend(player) ? this.targetType.isSelected("Friends") : true;
            }
         } else {
            return false;
         }
      } else if (entity instanceof AnimalEntity) {
         return this.targetType.isSelected("Animals");
      } else if (entity instanceof MobEntity) {
         return this.targetType.isSelected("Mobs");
      } else {
         return entity instanceof ArmorStandEntity ? this.targetType.isSelected("Armor Stands") : false;
      }
   }

   private Vec3d getAimPoint(Entity entity) {
      double heightFraction = this.aimHeightSetting.getValue() + (this.microMovementsSetting.getValue() ? this.heightNoise : 0.0);
      double offsetX = 0.0;
      double offsetZ = 0.0;
      if (this.multipointSetting.getValue()) {
         double horizontalRadius = (double)entity.getWidth() * 0.5;
         offsetX = this.currentPointX * horizontalRadius;
         offsetZ = this.currentPointZ * horizontalRadius;
         heightFraction += this.currentPointY;
      }

      return entity.getEntityPos().add(offsetX, (double)entity.getHeight() * MathHelper.clamp(heightFraction, 0.05, 0.95), offsetZ);
   }

   private double getAngleTo(Vec3d eyePos, Vec3d lookVec, Entity entity) {
      Vec3d targetVec = this.getAimPoint(entity).subtract(eyePos);
      double length = targetVec.length();
      if (length < 0.001) {
         return 0.0;
      } else {
         targetVec = targetVec.multiply(1.0 / length);
         double dot = lookVec.x * targetVec.x + lookVec.y * targetVec.y + lookVec.z * targetVec.z;
         return Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));
      }
   }

   private void updateHumanization(long nowMs, double frameScale) {
      if (!this.microMovementsSetting.getValue()) {
         this.heightNoise = 0.0;
      } else if (nowMs >= this.nextHeightNoiseUpdateMs) {
         this.heightNoise = ThreadLocalRandom.current().nextDouble(-0.02, 0.02);
         this.nextHeightNoiseUpdateMs = nowMs + ThreadLocalRandom.current().nextLong(180L, 450L);
      }

      if (!this.multipointSetting.getValue()) {
         this.currentPointX = this.currentPointY = this.currentPointZ = 0.0;
         this.nextPointX = this.nextPointY = this.nextPointZ = 0.0;
      } else {
         if (nowMs >= this.nextPointChangeAtMs) {
            this.randomizePoint(nowMs);
         }

         double lerpFactor = MathHelper.clamp(this.pointLerpSpeed * frameScale, 0.01, 1.0);
         this.currentPointX = lerp(this.currentPointX, this.nextPointX, lerpFactor);
         this.currentPointY = lerp(this.currentPointY, this.nextPointY, lerpFactor);
         this.currentPointZ = lerp(this.currentPointZ, this.nextPointZ, lerpFactor);
      }
   }

   private void drawFovRadius() {
      if (this.drawFovRadiusSetting.getValue() && mc.player != null && mc.world != null && mc.currentScreen == null && mc.getWindow() != null) {
         int width = mc.getWindow().getScaledWidth();
         int height = mc.getWindow().getScaledHeight();
         float radius = this.calculateFovRadius(width, height);
         if (!(radius < 1.0F)) {
            float diameter = radius * 2.0F;
            int color = this.target != null ? ColorUtil.rgba(127, 242, 255, 165) : ColorUtil.rgba(255, 255, 255, 95);
            Render2D.outline360(
               (float)width * 0.5F - radius,
               (float)height * 0.5F - radius,
               diameter,
               diameter,
               radius,
               1.2F,
               color,
               Render2D.outline360Range(0.0F, 360.0F, color)
            );
         }
      }
   }

   private float calculateFovRadius(int width, int height) {
      float maxRadius = Math.max(0.0F, (float)Math.min(width, height) * 0.5F - 2.0F);
      if (maxRadius <= 0.0F) {
         return 0.0F;
      } else {
         double aimFov = this.fovSetting.getValue();
         double gameFov = MathHelper.clamp((double)((Integer)mc.options.getFov().getValue()).intValue(), 30.0, 170.0);
         if (aimFov >= gameFov) {
            return maxRadius;
         } else {
            double aimHalf = MathHelper.clamp(aimFov * 0.5, 0.1, 89.9);
            double gameHalf = MathHelper.clamp(gameFov * 0.5, 1.0, 89.9);
            double radius = Math.tan(Math.toRadians(aimHalf)) / Math.tan(Math.toRadians(gameHalf)) * (double)Math.min(width, height) * 0.5;
            return (float)MathHelper.clamp(radius, 0.0, (double)maxRadius);
         }
      }
   }

   private void resetState() {
      this.worldKey = null;
      this.resetMotionState();
   }

   private void resetMotionState() {
      this.target = null;
      syncedTarget = null;
      this.targetAcquiredAtMs = 0L;
      this.lastFrameNs = 0L;
      this.lastRenderAimNs = 0L;
      this.previousYawStep = 0.0;
      this.previousPitchStep = 0.0;
      this.aimYawVelocity = 0.0;
      this.aimPitchVelocity = 0.0;
      this.currentPointX = 0.0;
      this.currentPointY = 0.0;
      this.currentPointZ = 0.0;
      this.nextPointX = 0.0;
      this.nextPointY = 0.0;
      this.nextPointZ = 0.0;
      this.nextPointChangeAtMs = 0L;
      this.randomizeProfile(System.currentTimeMillis());
   }

   private void randomizeProfile(long nowMs) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      this.yawWavePhase = random.nextDouble(Math.PI * 2);
      this.pitchWavePhase = random.nextDouble(Math.PI * 2);
      this.yawWaveSpeed = random.nextDouble(0.08, 0.25);
      this.pitchWaveSpeed = random.nextDouble(0.06, 0.2);
      this.speedMultiplier = random.nextDouble(0.75, 1.25);
      this.heightNoise = random.nextDouble(-0.02, 0.02);
      this.nextHeightNoiseUpdateMs = nowMs + random.nextLong(180L, 450L);
      this.pointLerpSpeed = random.nextDouble(0.008, 0.025);
      this.randomizePoint(nowMs);
   }

   private void randomizePoint(long nowMs) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      this.nextPointX = random.nextDouble(-0.35, 0.35);
      this.nextPointY = random.nextDouble(-0.12, 0.12);
      this.nextPointZ = random.nextDouble(-0.35, 0.35);
      this.nextPointChangeAtMs = nowMs + random.nextLong(600L, 1900L);
      this.pointLerpSpeed = random.nextDouble(0.008, 0.025);
   }

   private boolean isHoldingWeapon(MinecraftClient client) {
      if (client.player == null) {
         return false;
      } else {
         Item item = client.player.getMainHandStack().getItem();
         return item == Items.WOODEN_SWORD
            || item == Items.STONE_SWORD
            || item == Items.IRON_SWORD
            || item == Items.GOLDEN_SWORD
            || item == Items.DIAMOND_SWORD
            || item == Items.NETHERITE_SWORD
            || item instanceof AxeItem
            || item instanceof TridentItem
            || item instanceof MaceItem;
      }
   }

   private boolean hasArmor(LivingEntity entity) {
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack stack = entity.getEquippedStack(slot);
         if (!stack.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private boolean isBlockedByWall(MinecraftClient client, Vec3d from, Vec3d to) {
      if (client.world == null) {
         return false;
      } else {
         BlockHitResult hitResult = client.world.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, client.player));
         if (hitResult.getType() == Type.MISS) {
            return false;
         } else {
            BlockState state = client.world.getBlockState(hitResult.getBlockPos());
            return state.isFullCube(client.world, hitResult.getBlockPos()) && state.isOpaqueFullCube();
         }
      }
   }

   private double clampStep(double value, double previousValue, double maxDelta) {
      double delta = value - previousValue;
      return Math.abs(delta) > maxDelta ? previousValue + Math.signum(delta) * maxDelta : value;
   }

   private double smoothStep(double previousValue, double targetValue, double response) {
      double clampedResponse = MathHelper.clamp(response, 0.05, 1.0);
      return previousValue + (targetValue - previousValue) * clampedResponse;
   }

   private double clampToRemaining(double step, double remaining) {
      return Math.abs(step) > Math.abs(remaining) ? remaining : step;
   }

   private void enforceLockedSettings() {
      this.onlyYawSetting.setValue(Boolean.valueOf(true));
      this.multipointSetting.setValue(Boolean.valueOf(true));
      this.disableOnWorldChangeSetting.setValue(Boolean.valueOf(false));
      this.microMovementsSetting.setValue(Boolean.valueOf(true));
   }

   private static double lerp(double from, double to, double progress) {
      return from + (to - from) * MathHelper.clamp(progress, 0.0, 1.0);
   }
}
