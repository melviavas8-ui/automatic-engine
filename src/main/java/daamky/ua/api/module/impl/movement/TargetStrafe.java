package daamky.ua.api.module.impl.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.inventory.InventoryFlowManager;

public final class TargetStrafe extends Module {
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Target strafe mode.", "Matrix", "Matrix", "Grim"));
   private final ModeSetting type = this.register(new ModeSetting("Walk Point", "Target strafe walk point.", "Cube", "Cube", "Center", "Circle"));
   private final ModeSetting typeMatrix = this.register(new ModeSetting("Bypass Point", "Target strafe bypass point.", "Circle", "Cube", "Circle"));
   private final NumberSetting grimRadius = this.register(new NumberSetting("Walk Radius", "Target strafe walk radius.", 0.87, 0.1, 1.5, 0.01));
   private final NumberSetting radius = this.register(new NumberSetting("Radius", "Matrix strafe radius.", 2.5, 0.1, 7.0, 0.1));
   private final NumberSetting speed = this.register(new NumberSetting("Speed", "Matrix strafe speed.", 0.3, 0.1, 1.0, 0.01));
   private final MultiModeSetting options = this.register(
      new MultiModeSetting("Options", "Target strafe options.", new String[]{"Auto Jump", "Only While Pressing", "Front", "Direction Mode"}, "Auto Jump")
   );
   private final ModeSetting directionMode = this.register(
      new ModeSetting("Direction", "Strafe direction.", "Clockwise", "Clockwise", "Counter Clockwise", "Random")
   );
   private int grimPointIndex;

   public TargetStrafe() {
      super("Target Strafe", "Strafes around Aura target.", ModuleCategory.MOVEMENT);
      this.type.visibleWhen(() -> this.mode.is("Grim"));
      this.typeMatrix.visibleWhen(() -> this.mode.is("Matrix"));
      this.grimRadius.visibleWhen(() -> this.mode.is("Grim") && (this.type.is("Cube") || this.type.is("Circle")));
      this.radius.visibleWhen(() -> this.mode.is("Matrix"));
      this.speed.visibleWhen(() -> this.mode.is("Matrix"));
      this.directionMode.visibleWhen(() -> this.options.isSelected("Direction Mode"));
   }

   @Override
   protected void onEnable() {
      this.grimPointIndex = 0;
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null && InventoryFlowManager.isIdle()) {
         LivingEntity target = AuraModule.target;
         if (target != null && target.isAlive() && this.mode.is("Grim")) {
            if (!this.options.isSelected("Only While Pressing") || this.isAnyMoveKeyDown(client)) {
               Vec3d nextPoint = this.calculateGrimNextPoint(client, target);
               this.applyGrimMovement(client, event, nextPoint);
            }
         }
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent event) {
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null && InventoryFlowManager.isIdle()) {
         LivingEntity target = AuraModule.target;
         if (target != null && target.isAlive() && this.mode.is("Matrix")) {
            if (!this.options.isSelected("Only While Pressing") || this.isAnyMoveKeyDown(client)) {
               if (this.options.isSelected("Auto Jump") && client.player.isOnGround()) {
                  client.player.jump();
               }

               this.processMatrixStrafe(client, target);
            }
         }
      }
   }

   private Vec3d calculateGrimNextPoint(MinecraftClient client, LivingEntity target) {
      Vec3d playerPos = client.player.getEntityPos();
      Vec3d targetPos = target.getEntityPos();
      double r = this.grimRadius.getValue();
      int directionMultiplier = this.getDirectionMultiplier();
      return this.options.isSelected("Front")
         ? this.calculateFrontPoint(target, targetPos, r, directionMultiplier)
         : this.calculateNormalPoint(playerPos, targetPos, r, directionMultiplier, this.type);
   }

   private Vec3d calculateFrontPoint(LivingEntity target, Vec3d targetPos, double r, int directionMultiplier) {
      float targetYaw = target.getYaw();
      if (this.type.is("Center")) {
         return targetPos.add(
            -Math.sin(Math.toRadians((double)targetYaw)) * r * (double)directionMultiplier,
            0.0,
            Math.cos(Math.toRadians((double)targetYaw)) * r * (double)directionMultiplier
         );
      } else {
         double offset = Math.cos((double)System.currentTimeMillis() / 500.0) * r * (double)directionMultiplier;
         return targetPos.add(
            -Math.sin(Math.toRadians((double)targetYaw)) * r + Math.cos(Math.toRadians((double)targetYaw)) * offset,
            0.0,
            Math.cos(Math.toRadians((double)targetYaw)) * r + Math.sin(Math.toRadians((double)targetYaw)) * offset
         );
      }
   }

   private Vec3d calculateNormalPoint(Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier, ModeSetting selectedType) {
      if (selectedType.is("Cube")) {
         Vec3d[] points = new Vec3d[]{
            new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
            new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
            new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
            new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
         };
         if (playerPos.distanceTo(points[this.grimPointIndex]) < 0.5) {
            this.grimPointIndex = (this.grimPointIndex + directionMultiplier + points.length) % points.length;
         }

         return points[this.grimPointIndex];
      } else if (selectedType.is("Circle")) {
         double baseAngle = (double)(System.currentTimeMillis() % 3600L) / 3600.0 * 4.0 * Math.PI;
         double angle = directionMultiplier > 0 ? baseAngle : (Math.PI * 2) - baseAngle;
         return new Vec3d(targetPos.x + Math.cos(angle) * r, playerPos.y, targetPos.z + Math.sin(angle) * r);
      } else {
         return new Vec3d(targetPos.x, playerPos.y, targetPos.z);
      }
   }

   private void applyGrimMovement(MinecraftClient client, InputEvent event, Vec3d nextPoint) {
      Vec3d direction = nextPoint.subtract(client.player.getEntityPos()).normalize();
      float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
      float movementAngle = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
      float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);
      boolean forward = false;
      boolean back = false;
      boolean left = false;
      boolean right = false;
      if (angleDiff >= -22.5F && angleDiff < 22.5F) {
         forward = true;
      } else if (angleDiff >= 22.5F && angleDiff < 67.5F) {
         forward = true;
         right = true;
      } else if (angleDiff >= 67.5F && angleDiff < 112.5F) {
         right = true;
      } else if (angleDiff >= 112.5F && angleDiff < 157.5F) {
         back = true;
         right = true;
      } else if (angleDiff >= -67.5F && angleDiff < -22.5F) {
         forward = true;
         left = true;
      } else if (angleDiff >= -112.5F && angleDiff < -67.5F) {
         left = true;
      } else if (angleDiff >= -157.5F && angleDiff < -112.5F) {
         back = true;
         left = true;
      } else {
         back = true;
      }

      event.setDirectionalLow(forward, back, left, right);
      if (this.options.isSelected("Auto Jump") && client.player.isOnGround()) {
         event.setJumping(true);
      }
   }

   private void processMatrixStrafe(MinecraftClient client, LivingEntity target) {
      Vec3d playerPos = client.player.getEntityPos();
      Vec3d targetPos = target.getEntityPos();
      double r = this.radius.getValue();
      int directionMultiplier = this.getDirectionMultiplier();
      if (this.options.isSelected("Front")) {
         this.processMatrixFrontStrafe(client, target, playerPos, targetPos, r, directionMultiplier);
      } else {
         if (this.typeMatrix.is("Cube")) {
            this.processMatrixCubeStrafe(client, playerPos, targetPos, r, directionMultiplier);
         } else if (this.typeMatrix.is("Circle")) {
            this.processMatrixCircleStrafe(client, playerPos, targetPos, r, directionMultiplier);
         }
      }
   }

   private void processMatrixFrontStrafe(MinecraftClient client, LivingEntity target, Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
      float targetYaw = target.getYaw();
      double x = targetPos.x - Math.sin(Math.toRadians((double)targetYaw)) * r * (double)directionMultiplier;
      double z = targetPos.z + Math.cos(Math.toRadians((double)targetYaw)) * r * (double)directionMultiplier;
      this.setMotionTo(client, playerPos, x, z);
   }

   private void processMatrixCubeStrafe(MinecraftClient client, Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
      Vec3d nextPoint = this.calculateNormalPoint(playerPos, targetPos, r, directionMultiplier, this.typeMatrix);
      this.setMotionTo(client, playerPos, nextPoint.x, nextPoint.z);
   }

   private void processMatrixCircleStrafe(MinecraftClient client, Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
      double angle = Math.atan2(playerPos.z - targetPos.z, playerPos.x - targetPos.x);
      angle += (double)directionMultiplier * this.speed.getValue() / Math.max(playerPos.distanceTo(targetPos), r);
      double x = targetPos.x + r * Math.cos(angle);
      double z = targetPos.z + r * Math.sin(angle);
      this.setMotionTo(client, playerPos, x, z);
   }

   private void setMotionTo(MinecraftClient client, Vec3d playerPos, double x, double z) {
      float yaw = (float)Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90.0F;
      double motionSpeed = this.speed.getValue();
      client.player
         .setVelocity(-Math.sin(Math.toRadians((double)yaw)) * motionSpeed, client.player.getVelocity().y, Math.cos(Math.toRadians((double)yaw)) * motionSpeed);
   }

   private int getDirectionMultiplier() {
      if (this.options.isSelected("Direction Mode")) {
         if (this.directionMode.is("Counter Clockwise")) {
            return -1;
         }

         if (this.directionMode.is("Random")) {
            return System.currentTimeMillis() / 3000L % 2L == 0L ? 1 : -1;
         }
      }

      return 1;
   }

   private boolean isAnyMoveKeyDown(MinecraftClient client) {
      return client.options.forwardKey.isPressed()
         || client.options.backKey.isPressed()
         || client.options.leftKey.isPressed()
         || client.options.rightKey.isPressed();
   }
}
