package daamky.ua.api.module.impl.visual.particles;

import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

final class ParticleMotion {
   private final ParticleSettings settings;
   private final Random random;
   private final float[] rotationOut = new float[2];
   private final float[] speedOut = new float[2];
   private final double[] motionOut = new double[3];

   ParticleMotion(ParticleSettings settings, Random random) {
      this.settings = settings;
      this.random = random;
   }

   float[] startMotionRotations(MinecraftClient client, Vec3d sourcePos, Vec3d objPos, Entity entityFrom) {
      String rule = entityFrom != null && entityFrom != client.player
         ? this.settings.spawnPulseMotionSideRuleOnAttack.getValue()
         : this.settings.spawnPulseMotionSideRuleInWorld.getValue();
      float pitch = 0.0F;

      return switch (rule) {
         case "No XZ Push" -> this.setRotation(-1337.1488F, pitch);
         case "Random" -> this.setRotation(ParticleMath.randomFloat(this.random, 0.0F, 360.0F), ParticleMath.randomFloat(this.random, -90.0F, 90.0F));
         case "To Source" -> ParticleMath.vecNeeded(sourcePos, objPos, this.rotationOut);
         case "From Source" -> ParticleMath.vecNeeded(objPos, sourcePos, this.rotationOut);
         case "Camera Left" -> this.setRotation(this.cameraYaw(client) - 90.0F, pitch);
         case "Camera Right" -> this.setRotation(this.cameraYaw(client) + 90.0F, pitch);
         case "Camera Forward" -> this.setRotation(this.cameraYaw(client), pitch);
         case "Camera Back" -> this.setRotation(this.cameraYaw(client) + 180.0F, pitch);
         default -> this.setRotation(0.0F, pitch);
      };
   }

   float[] changeRotationUpdateIfRule(
      MinecraftClient client,
      String motionSideChangeRuleMode,
      double objPosX,
      double objPosY,
      double objPosZ,
      Vec3d tentativeTargetVec,
      float yawIn,
      float pitchIn,
      float[] speedYawPitch,
      byte metaDataRange1Only,
      ParticleRules rules,
      float randomFloat01StaticValue
   ) {
      float preYaw = yawIn;
      float prePitch = pitchIn;
      switch (motionSideChangeRuleMode) {
         case "Camera Left":
            yawIn = this.cameraYaw(client) - 90.0F;
            pitchIn += speedYawPitch[1] / 5.0F;
            break;
         case "Camera Right":
            yawIn = this.cameraYaw(client) + 90.0F;
            pitchIn += speedYawPitch[1] / 5.0F;
            break;
         case "Camera Forward":
            yawIn = this.cameraYaw(client);
            pitchIn += speedYawPitch[1] / 5.0F;
            break;
         case "Camera Back":
            yawIn = this.cameraYaw(client) + 180.0F;
            pitchIn += speedYawPitch[1] / 5.0F;
            break;
         case "Twist Left":
            yawIn -= speedYawPitch[0];
            pitchIn += speedYawPitch[1] / 5.0F;
            break;
         case "Twist Right":
            yawIn += speedYawPitch[0];
            pitchIn += speedYawPitch[1] / 5.0F;
            break;
         case "Twist Random":
            yawIn += speedYawPitch[0] * (float)metaDataRange1Only;
            pitchIn += speedYawPitch[1] / 5.0F;
            break;
         case "To Source":
         case "From Source":
            Vec3d target = tentativeTargetVec == null
               ? ParticleWorld.entityVecForParticle(client, rules.entityFor, randomFloat01StaticValue)
               : tentativeTargetVec;
            float[] targetRotation = "From Source".equals(motionSideChangeRuleMode)
               ? ParticleMath.vecNeeded(objPosX, objPosY, objPosZ, target.x, target.y, target.z, this.rotationOut)
               : ParticleMath.vecNeeded(target.x, target.y, target.z, objPosX, objPosY, objPosZ, this.rotationOut);
            yawIn = targetRotation[0];
            pitchIn = targetRotation[1];
      }

      return this.setRotation(
         ParticleMath.stepRotationSpeed(preYaw, yawIn, speedYawPitch[0]), ParticleMath.stepRotationSpeed(prePitch, pitchIn, speedYawPitch[1])
      );
   }

   double[] syncMotionsXYZToAngles(double xyzSqrtDelta, float yaw, float pitch, double speedIfZero) {
      double speed = !(xyzSqrtDelta > 0.0) && speedIfZero != 0.0 ? speedIfZero : xyzSqrtDelta;
      double[] out = ParticleMath.vec3FromAngles(yaw, pitch, speed, this.motionOut);
      if (yaw == -1337.1488F) {
         out[0] = 0.0;
         out[2] = 0.0;
      }

      return out;
   }

   float[] rotationSpeeds(float speedMultiplier) {
      this.speedOut[0] = speedMultiplier * 180.0F;
      this.speedOut[1] = speedMultiplier * 90.0F;
      return this.speedOut;
   }

   private float[] setRotation(float yaw, float pitch) {
      this.rotationOut[0] = yaw;
      this.rotationOut[1] = pitch;
      return this.rotationOut;
   }

   private float cameraYaw(MinecraftClient client) {
      if (client.player == null) {
         return 0.0F;
      } else {
         float yaw = client.player.getYaw();
         if (client.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            yaw += 180.0F;
         }

         return yaw;
      }
   }
}
