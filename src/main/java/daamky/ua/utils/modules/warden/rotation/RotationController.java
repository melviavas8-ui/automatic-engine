package daamky.ua.utils.modules.warden.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public final class RotationController {
   private static RotationController.RotationControllerState state = RotationController.RotationControllerState.IDLE;
   private static float yawSpeed;
   private static float pitchSpeed;
   private static float returnYawSpeed;
   private static float returnPitchSpeed;
   private static int priority;
   private static int timeout;
   private static int idleTicks;
   private static Rotation targetRotation;

   private RotationController() {
   }

   public static boolean isRotating() {
      return state != RotationController.RotationControllerState.IDLE;
   }

   public static void tick() {
      if (state == RotationController.RotationControllerState.AIM && idleTicks > timeout) {
         state = RotationController.RotationControllerState.RESET;
      }

      if (state == RotationController.RotationControllerState.RESET) {
         Rotation cameraRotation = new Rotation(FreeLookController.getFreeYaw(), FreeLookController.getFreePitch());
         if (updateRotation(cameraRotation, returnYawSpeed, returnPitchSpeed)) {
            stop();
         }
      }

      idleTicks++;
   }

   public static void update(
      Rotation target,
      float turnYawSpeed,
      float turnPitchSpeed,
      float yawReturnSpeed,
      float pitchReturnSpeed,
      int resetTimeout,
      int taskPriority,
      boolean clientRotation
   ) {
      if (target != null && priority <= taskPriority) {
         if (state == RotationController.RotationControllerState.IDLE && !clientRotation) {
            FreeLookController.activate();
         }

         yawSpeed = turnYawSpeed;
         pitchSpeed = turnPitchSpeed;
         returnYawSpeed = yawReturnSpeed;
         returnPitchSpeed = pitchReturnSpeed;
         timeout = resetTimeout;
         priority = taskPriority;
         state = RotationController.RotationControllerState.AIM;
         targetRotation = target;
         updateRotation(target, turnYawSpeed, turnPitchSpeed);
      }
   }

   private static boolean updateRotation(Rotation target, float turnYawSpeed, float turnPitchSpeed) {
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player != null && target != null) {
         float yawDelta = MathHelper.wrapDegrees(target.floatValue - player.getYaw());
         float pitchDelta = target.floatValue2 - player.getPitch();
         float totalDelta = Math.abs(yawDelta) + Math.abs(pitchDelta);
         float maximumYaw = totalDelta == 0.0F ? 0.0F : Math.abs(yawDelta / totalDelta) * turnYawSpeed;
         float maximumPitch = totalDelta == 0.0F ? 0.0F : Math.abs(pitchDelta / totalDelta) * turnPitchSpeed;
         float nextYaw = player.getYaw() + MathHelper.clamp(yawDelta, -maximumYaw, maximumYaw);
         float nextPitch = MathHelper.clamp(player.getPitch() + MathHelper.clamp(pitchDelta, -maximumPitch, maximumPitch), -90.0F, 90.0F);
         Rotation patched = applySensitivityPatch(new Rotation(nextYaw, nextPitch), new Rotation(player.getYaw(), player.getPitch()));
         applyToPlayer(player, patched);
         idleTicks = 0;
         float completionSpeed = state == RotationController.RotationControllerState.RESET
            ? (float)Math.hypot((double)returnYawSpeed, (double)returnPitchSpeed)
            : (float)Math.hypot((double)yawSpeed, (double)pitchSpeed);
         return new Rotation(player.getYaw(), player.getPitch()).measure(target) < completionSpeed;
      } else {
         return false;
      }
   }

   private static Rotation applySensitivityPatch(Rotation rotation, Rotation previous) {
      MinecraftClient client = MinecraftClient.getInstance();
      double sensitivity = (Double)client.options.getMouseSensitivity().getValue();
      double gcd = Math.pow(sensitivity * 0.6F + 0.2F, 3.0) * 8.0;
      double step = gcd * 0.15F;
      float yaw = (float)((double)previous.floatValue + Math.ceil((double)MathHelper.wrapDegrees(rotation.floatValue - previous.floatValue) / step) * step);
      float pitch = (float)((double)previous.floatValue2 + Math.ceil((double)(rotation.floatValue2 - previous.floatValue2) / step) * step);
      return new Rotation(yaw, MathHelper.clamp(pitch, -90.0F, 90.0F));
   }

   private static void applyToPlayer(ClientPlayerEntity player, Rotation rotation) {
      player.setYaw(rotation.floatValue);
      player.setPitch(rotation.floatValue2);
      player.lastYaw = rotation.floatValue;
      player.lastPitch = rotation.floatValue2;
      player.headYaw = rotation.floatValue;
      player.lastHeadYaw = rotation.floatValue;
      player.bodyYaw = rotation.floatValue;
      player.lastBodyYaw = rotation.floatValue;
   }

   private static void stop() {
      state = RotationController.RotationControllerState.IDLE;
      priority = 0;
      targetRotation = null;
      FreeLookController.deactivate();
   }

   public static void hardReset() {
      if (FreeLookController.isActive()) {
         FreeLookController.restorePlayerLook();
      }

      state = RotationController.RotationControllerState.IDLE;
      priority = 0;
      timeout = 0;
      idleTicks = 0;
      targetRotation = null;
      FreeLookController.deactivate();
   }

   private static enum RotationControllerState {
      AIM,
      RESET,
      IDLE;

      private RotationControllerState() {
      }
   }
}
