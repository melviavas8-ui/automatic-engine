package daamky.ua.api.module.impl.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.FireworkEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;

public final class SuperFireWork extends Module {
   private final ModeSetting modeSetting = this.register(
      new ModeSetting("Mode", "Firework boost mode.", "BravoHvH", "BravoHvH", "ReallyWorld", "PulseHVH", "Custom")
   );
   private final NumberSetting customSpeedSetting = this.register(new NumberSetting("Speed", "Custom firework speed.", 1.963, 1.5, 3.0, 0.001));
   private final BooleanSetting nearBoostSetting = this.register(new BooleanSetting("Near Boost", "Boosts when a player is close.", false));

   public SuperFireWork() {
      super("Super Fire Work", "Amplifies elytra firework boost.", ModuleCategory.MOVEMENT);
      this.customSpeedSetting.visibleWhen(() -> this.modeSetting.is("Custom"));
   }

   @SubscribeEvent
   private void onFirework(FireworkEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.player.isGliding()) {
         float yaw = AngleConnection.INSTANCE.getRotation().getYaw() % 360.0F;
         if (yaw < 0.0F) {
            yaw += 360.0F;
         }

         if (this.modeSetting.is("ReallyWorld")) {
            this.handleReallyWorldMode(event, yaw);
         } else if (this.modeSetting.is("BravoHvH")) {
            this.handleBravoHvHMode(client, event, yaw);
         } else if (this.modeSetting.is("PulseHVH")) {
            this.handlePulseHVHMode(client, event, yaw);
         } else if (this.modeSetting.is("Custom")) {
            this.handleCustomMode(client, event, yaw);
         }
      }
   }

   private void handleReallyWorldMode(FireworkEvent event, float yaw) {
      float closestDiff = 180.0F;

      for (float diagonal : new float[]{45.0F, 135.0F, 225.0F, 315.0F}) {
         float diff = Math.abs(yaw - diagonal);
         diff = Math.min(diff, 360.0F - diff);
         if (diff < closestDiff) {
            closestDiff = diff;
         }
      }

      double speedXZ = 1.5;
      double speedY = 1.5;
      if (closestDiff <= 4.0F) {
         speedXZ = 2.2;
      } else if (closestDiff <= 8.0F) {
         speedXZ = 2.06;
      } else if (closestDiff <= 12.0F) {
         speedXZ = 1.98;
      } else if (closestDiff <= 16.0F) {
         speedXZ = 1.87;
      } else if (closestDiff <= 20.0F) {
         speedXZ = 1.8;
      } else if (closestDiff <= 24.0F) {
         speedXZ = 1.74;
      } else if (closestDiff <= 28.0F) {
         speedXZ = 1.7;
      } else if (closestDiff <= 32.0F) {
         speedXZ = 1.65;
      } else if (closestDiff <= 36.0F) {
         speedXZ = 1.63;
      } else {
         speedXZ = 1.61;
         speedY = 1.61;
      }

      this.applyFireworkVelocity(event, speedXZ, speedY);
   }

   private void handleBravoHvHMode(MinecraftClient client, FireworkEvent event, float yaw) {
      boolean diagonal = this.checkDiagonal(yaw, 16.0F);
      boolean nearPlayer = this.checkNearPlayer(client, 4.0F);
      double speedY = 1.66;
      double speedXZ;
      if (diagonal) {
         speedXZ = 1.963;
      } else if (this.nearBoostSetting.getValue() && nearPlayer) {
         speedXZ = 1.82;
         speedY = 1.67;
      } else {
         speedXZ = 1.675;
      }

      this.applyFireworkVelocity(event, speedXZ, speedY);
   }

   private void handlePulseHVHMode(MinecraftClient client, FireworkEvent event, float yaw) {
      boolean diagonal = this.checkDiagonal(yaw, 16.0F);
      boolean nearPlayer = this.checkNearPlayer(client, 5.0F);
      double speedY = 1.66;
      double speedXZ;
      if (diagonal) {
         speedXZ = 1.963;
      } else if (this.nearBoostSetting.getValue() && nearPlayer) {
         speedXZ = 1.82;
         speedY = 1.67;
      } else {
         speedXZ = 1.675;
      }

      this.applyFireworkVelocity(event, speedXZ, speedY);
   }

   private void handleCustomMode(MinecraftClient client, FireworkEvent event, float yaw) {
      boolean diagonal = this.checkDiagonal(yaw, 16.0F);
      boolean nearPlayer = this.checkNearPlayer(client, 5.0F);
      double speedY = 1.66;
      double speedXZ;
      if (diagonal) {
         speedXZ = this.customSpeedSetting.getValue();
      } else if (this.nearBoostSetting.getValue() && nearPlayer) {
         speedXZ = this.customSpeedSetting.getValue() - 0.1;
         speedY = 1.67;
      } else {
         speedXZ = 1.675;
      }

      this.applyFireworkVelocity(event, speedXZ, speedY);
   }

   private boolean checkDiagonal(float yaw, float threshold) {
      for (float diagonal : new float[]{45.0F, 135.0F, 225.0F, 315.0F}) {
         float diff = Math.abs(yaw - diagonal);
         diff = Math.min(diff, 360.0F - diff);
         if (diff <= threshold) {
            return true;
         }
      }

      return false;
   }

   private boolean checkNearPlayer(MinecraftClient client, float distance) {
      if (this.nearBoostSetting.getValue() && client.world != null && client.player != null) {
         for (PlayerEntity player : client.world.getPlayers()) {
            if (player != client.player && player.distanceTo(client.player) <= distance) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void applyFireworkVelocity(FireworkEvent event, double speedXZ, double speedY) {
      Vec3d rotationVector = AngleConnection.INSTANCE.getMoveRotation().toVector();
      Vec3d currentVelocity = event.getVector();
      event.setVector(
         currentVelocity.add(
            rotationVector.x * 0.1 + (rotationVector.x * speedXZ - currentVelocity.x) * 0.5,
            rotationVector.y * 0.1 + (rotationVector.y * speedY - currentVelocity.y) * 0.5,
            rotationVector.z * 0.1 + (rotationVector.z * speedXZ - currentVelocity.z) * 0.5
         )
      );
   }
}
