package daamky.ua.utils.move;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;

public final class MoveUtil {
   private static final MinecraftClient MC = MinecraftClient.getInstance();

   private MoveUtil() {
   }

   public static boolean hasPlayerMovement() {
      if (MC.player != null && MC.player.input != null) {
         Input input = MC.player.input;
         if (input.hasForwardMovement()) {
            return true;
         } else {
            Vec2f movement = input.getMovementInput();
            return movement.x != 0.0F || movement.y != 0.0F;
         }
      } else {
         return false;
      }
   }

   public static double getDistanceToGround() {
      if (MC.player != null && MC.world != null) {
         for (double y = MC.player.getY(); y > (double)MC.world.getBottomY(); y -= 0.1) {
            if (!MC.world.getBlockState(MC.player.getBlockPos().down((int)(MC.player.getY() - y + 1.0))).isAir()) {
               return MC.player.getY() - y;
            }
         }

         return 256.0;
      } else {
         return 256.0;
      }
   }

   public static double getDegreesRelativeToView(Vec3d positionRelativeToPlayer, float yaw) {
      float optimalYaw = (float)Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
      double currentYaw = Math.toRadians((double)MathHelper.wrapDegrees(yaw));
      return Math.toDegrees((double)MathHelper.wrapDegrees((float)((double)optimalYaw - currentYaw)));
   }

   public static void setVelocity(double velocity) {
      if (MC.player != null) {
         double[] direction = calculateDirection(velocity);
         MC.player.setVelocity(direction[0], MC.player.getVelocity().getY(), direction[1]);
      }
   }

   public static double[] forward(double distance) {
      if (MC.player != null && MC.player.input != null) {
         Vec2f movement = MC.player.input.getMovementInput();
         float forward = movement.y;
         float sideways = movement.x;
         float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
         if (forward != 0.0F) {
            if (sideways > 0.0F) {
               yaw += forward > 0.0F ? -45.0F : 45.0F;
            } else if (sideways < 0.0F) {
               yaw += forward > 0.0F ? 45.0F : -45.0F;
            }

            sideways = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
         }

         double sin = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
         double cos = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
         return new double[]{
            (double)forward * distance * cos + (double)sideways * distance * sin, (double)forward * distance * sin - (double)sideways * distance * cos
         };
      } else {
         return new double[]{0.0, 0.0};
      }
   }

   public static double[] calculateDirection(double distance) {
      if (MC.player != null && MC.player.input != null) {
         Vec2f movement = MC.player.input.getMovementInput();
         return calculateDirection(movement.y, movement.x, distance);
      } else {
         return new double[]{0.0, 0.0};
      }
   }

   public static double[] calculateDirection(float forward, float sideways, double distance) {
      float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
      if (forward != 0.0F) {
         if (sideways > 0.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
         } else if (sideways < 0.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
         }

         sideways = 0.0F;
         forward = forward > 0.0F ? 1.0F : -1.0F;
      }

      double sin = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
      double cos = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
      double x = (double)forward * distance * cos + (double)sideways * distance * sin;
      double z = (double)forward * distance * sin - (double)sideways * distance * cos;
      return new double[]{x, z};
   }

   public static boolean moveKeyPressed(int keyNumber) {
      if (MC.options == null) {
         return false;
      } else {
         boolean w = MC.options.forwardKey.isPressed();
         boolean a = MC.options.leftKey.isPressed();
         boolean s = MC.options.backKey.isPressed();
         boolean d = MC.options.rightKey.isPressed();
         return keyNumber == 0 ? w : (keyNumber == 1 ? a : (keyNumber == 2 ? s : keyNumber == 3 && d));
      }
   }

   public static boolean w() {
      return moveKeyPressed(0);
   }

   public static boolean a() {
      return moveKeyPressed(1);
   }

   public static boolean s() {
      return moveKeyPressed(2);
   }

   public static boolean d() {
      return moveKeyPressed(3);
   }

   public static float moveYaw(float entityYaw) {
      return entityYaw
         + (float)(
            !a() || !d() || w() && s() || !w() && !s()
               ? (
                  !w() || !s() || a() && d() || !a() && !d()
                     ? (
                        (!a() || !d() || w() && s()) && (!w() || !s() || a() && d())
                           ? (
                              !a() && !d() && !s()
                                 ? 0
                                 : (w() && !s() ? 45 : (!s() || w() ? (!w() && !s() || w() && s() ? 90 : 0) : (!a() && !d() ? 180 : 135))) * (a() ? -1 : 1)
                           )
                           : 0
                     )
                     : (a() ? -90 : (d() ? 90 : 0))
               )
               : (w() ? 0 : (s() ? 180 : 0))
         );
   }

   public static float calculateBodyYaw(float yaw, float prevBodyYaw, double prevX, double prevZ, double currentX, double currentZ, float handSwingProgress) {
      double motionX = currentX - prevX;
      double motionZ = currentZ - prevZ;
      float motionSquared = (float)(motionX * motionX + motionZ * motionZ);
      float bodyYaw = prevBodyYaw;
      if (motionSquared > 0.0025000002F) {
         float movementYaw = (float)MathHelper.atan2(motionZ, motionX) * (180.0F / (float)Math.PI) - 90.0F;
         float yawDiff = MathHelper.abs(MathHelper.wrapDegrees(yaw) - movementYaw);
         if (95.0F < yawDiff && yawDiff < 265.0F) {
            bodyYaw = movementYaw - 180.0F;
         } else {
            bodyYaw = movementYaw;
         }
      }

      if (MC.player != null && MC.player.handSwingProgress - 0.2F > 0.0F) {
         bodyYaw = yaw;
      }

      float deltaYaw = MathHelper.wrapDegrees(bodyYaw - prevBodyYaw);
      bodyYaw = prevBodyYaw + deltaYaw * 0.3F;
      float yawOffsetDiff = MathHelper.wrapDegrees(yaw - bodyYaw);
      float maxHeadRotation = 52.0F;
      if (Math.abs(yawOffsetDiff) > maxHeadRotation) {
         bodyYaw += yawOffsetDiff - (float)MathHelper.sign((double)yawOffsetDiff) * maxHeadRotation;
      }

      return bodyYaw;
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double degrees, float deadAngle) {
      Objects.requireNonNull(input, "input");
      boolean forward = input.forward();
      boolean backward = input.backward();
      boolean left = input.left();
      boolean right = input.right();
      if (degrees >= (double)(-90.0F + deadAngle) && degrees <= (double)(90.0F - deadAngle)) {
         forward = true;
      } else if (degrees < (double)(-90.0F - deadAngle) || degrees > (double)(90.0F + deadAngle)) {
         backward = true;
      }

      if (degrees >= (double)(0.0F + deadAngle) && degrees <= (double)(180.0F - deadAngle)) {
         right = true;
      } else if (degrees >= (double)(-180.0F + deadAngle) && degrees <= (double)(0.0F - deadAngle)) {
         left = true;
      }

      return new PlayerInput(forward, backward, left, right, input.jump(), input.sneak(), input.sprint());
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double degrees) {
      return getDirectionalInputForDegrees(input, degrees, 20.0F);
   }
}
