package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.utils.modules.warden.rotation.Rotation;
import daamky.ua.utils.modules.warden.rotation.RotationController;

public final class HWangle extends RotateConstructor {
   public static final HWangle INSTANCE = new HWangle();
   private static final float EPSILON = 1.0E-4F;
   private static final float PITCH_OFFSET = -1.75F;
   private static final float STIFFNESS_NEAR_MIN = 190.0F;
   private static final float STIFFNESS_NEAR_MAX = 250.0F;
   private static final float STIFFNESS_FAR_MIN = 550.0F;
   private static final float STIFFNESS_FAR_MAX = 1050.0F;
   private static final float STIFFNESS_DIST_NEAR = 2.0F;
   private static final float STIFFNESS_DIST_FAR = 15.0F;
   private static final float DAMPING_RATIO = 3.0F;
   private static final float MAX_ACCEL_MIN = 16000.0F;
   private static final float MAX_ACCEL_MAX = 22500.0F;
   private static final float MAX_VEL_BASE_MIN = 4800.0F;
   private static final float MAX_VEL_BASE_MAX = 6200.0F;
   private static final float MAX_VEL_PER_DEG_MIN = 115.0F;
   private static final float MAX_VEL_PER_DEG_MAX = 135.0F;
   private static final float PITCH_FOLLOW = 0.8F;
   private static final float SETTLE_DIST = 1.5F;
   private static final float SETTLE_SCALE = 0.55F;
   private static final float DRIFT_AMPLITUDE = 0.45F;
   private static final float DRIFT_FREQ_YAW = 0.7F;
   private static final float DRIFT_FREQ_PITCH = 0.9F;
   private static final float JITTER_AMPLITUDE = 0.9F;
   private static final float JITTER_INTERVAL_MIN = 0.18F;
   private static final float JITTER_INTERVAL_MAX = 0.55F;
   private static final float JITTER_DECAY = 14.0F;
   private static final float TARGET_SMOOTH_RATE_MIN = 38.0F;
   private static final float TARGET_SMOOTH_RATE_MAX = 48.0F;
   private AuraModule aura;
   private LivingEntity target;
   private long lastFrameNanos;
   private final HWangle.State state = new HWangle.State();

   private HWangle() {
      super("HolyWorld");
      this.state.randomize();
   }

   @Override
   public Angle limitAngleChange(Angle currentRot, Angle targetRot, Vec3d aimPoint, Entity entity) {
      this.aura = AuraModule.getInstance();
      this.target = entity instanceof LivingEntity livingEntity ? livingEntity : null;
      return currentRot;
   }

   @Override
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }

   @SubscribeEvent
   public void on3D(WorldRenderEvent e) {
      if (this.isActive()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null && !mc.player.isDead()) {
            long now = System.nanoTime();
            float dt = this.lastFrameNanos == 0L ? 0.016666668F : Math.min((float)(now - this.lastFrameNanos) / 1.0E9F, 1.0F);
            this.lastFrameNanos = now;
            if (dt < 1.0E-4F) {
               dt = 0.016666668F;
            }

            if (this.target == null) {
               this.state.decayMotion(dt);
            } else {
               Vec3d eyes = mc.player.getEyePos();
               float[] delta = this.computeTargetDelta(this.target, e.getTickDelta(), dt, eyes);
               float yawErr = delta[0];
               float pitchErr = delta[1] + -1.75F;
               if (Float.isFinite(yawErr) && Float.isFinite(pitchErr)) {
                  float dist = (float)Math.sqrt((double)(yawErr * yawErr + pitchErr * pitchErr));
                  float stiffness = this.computeStiffness(dist);
                  float damping = 3.0F * (float)Math.sqrt((double)stiffness);
                  float yawAccel = MathHelper.clamp(
                     stiffness * yawErr - damping * this.state.yawVel, -HWangle.Util.random(16000.0F, 22500.0F), HWangle.Util.random(16000.0F, 22500.0F)
                  );
                  float pitchAccel = MathHelper.clamp(
                     (stiffness * pitchErr - damping * this.state.pitchVel) * 0.8F,
                     -HWangle.Util.random(16000.0F, 22500.0F),
                     HWangle.Util.random(16000.0F, 22500.0F)
                  );
                  this.state.yawVel += yawAccel * dt;
                  this.state.pitchVel += pitchAccel * dt;
                  if (dist < 1.5F) {
                     float scale = 0.55F + 0.45F * (dist / 1.5F);
                     this.state.yawVel *= scale;
                     this.state.pitchVel *= scale;
                  }

                  float maxVel = HWangle.Util.random(4800.0F, 6200.0F) + dist * HWangle.Util.random(115.0F, 135.0F);
                  this.state.yawVel = MathHelper.clamp(this.state.yawVel, -maxVel, maxVel);
                  this.state.pitchVel = MathHelper.clamp(this.state.pitchVel, -maxVel, maxVel);
                  this.applyDrift(dt, dist);
                  this.applyJitter(dt, dist);
                  this.applyFrame(dt);
               } else {
                  this.state.decayMotion(dt);
               }
            }
         }
      } else {
         if (this.target != null || this.state.hasSmoothed) {
            this.resetState();
         }
      }
   }

   private float[] computeTargetDelta(LivingEntity target, float tickDelta, float dt, Vec3d eyes) {
      MinecraftClient mc = MinecraftClient.getInstance();
      double tx = MathHelper.lerp((double)tickDelta, target.lastRenderX, target.getX());
      double ty = MathHelper.lerp((double)tickDelta, target.lastRenderY, target.getY()) + (double)(target.getHeight() * 0.5F);
      double tz = MathHelper.lerp((double)tickDelta, target.lastRenderZ, target.getZ());
      if (!this.state.hasSmoothed) {
         this.state.smoothedX = tx;
         this.state.smoothedY = ty;
         this.state.smoothedZ = tz;
         this.state.hasSmoothed = true;
      } else {
         double blend = MathHelper.clamp(1.0 - Math.exp((double)(-HWangle.Util.random(38.0F, 48.0F) * dt)), 0.05, 0.95);
         this.state.smoothedX = this.state.smoothedX + (tx - this.state.smoothedX) * blend;
         this.state.smoothedY = this.state.smoothedY + (ty - this.state.smoothedY) * blend;
         this.state.smoothedZ = this.state.smoothedZ + (tz - this.state.smoothedZ) * blend;
      }

      double dx = this.state.smoothedX - eyes.x;
      double dy = this.state.smoothedY - eyes.y;
      double dz = this.state.smoothedZ - eyes.z;
      double horiz = Math.sqrt(dx * dx + dz * dz);
      if (horiz < 1.0E-4F && Math.abs(dy) < 1.0E-4F) {
         return new float[]{0.0F, 0.0F};
      } else {
         float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
         float pitch = MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(dy, horiz))), -90.0F, 90.0F);
         return new float[]{MathHelper.wrapDegrees(yaw - mc.player.getYaw()), pitch - mc.player.getPitch()};
      }
   }

   private float computeStiffness(float dist) {
      float near = HWangle.Util.random(190.0F, 250.0F);
      float far = HWangle.Util.random(550.0F, 1050.0F);
      if (dist <= 2.0F) {
         return near;
      } else if (dist >= 15.0F) {
         return far;
      } else {
         float t = (dist - 2.0F) / 13.0F;
         t = t * t * (3.0F - 2.0F * t);
         return near + (far - near) * t;
      }
   }

   private void applyDrift(float dt, float dist) {
      this.state.driftPhaseYaw += dt * 0.7F;
      this.state.driftPhasePitch += dt * 0.9F;
      float scale = MathHelper.clamp(dist / 8.0F, 0.2F, 1.0F);
      this.state.yawVel = this.state.yawVel
         + (float)Math.sin((double)this.state.driftPhaseYaw * 2.0 * Math.PI + (double)this.state.driftSeedYaw) * 0.45F * scale * 10.0F * dt;
      this.state.pitchVel = this.state.pitchVel
         + (float)Math.cos((double)this.state.driftPhasePitch * 2.0 * Math.PI + (double)this.state.driftSeedPitch) * 0.45F * scale * 6.0F * dt;
   }

   private void applyJitter(float dt, float dist) {
      this.state.jitterTimer += dt;
      if (this.state.jitterTimer >= this.state.jitterInterval) {
         this.state.jitterTimer = 0.0F;
         this.state.jitterInterval = HWangle.Util.random(0.18F, 0.55F);
         float scale = MathHelper.clamp(dist / 10.0F, 0.3F, 1.0F);
         this.state.yawJitterVel = this.state.yawJitterVel + HWangle.Util.gaussian() * 0.9F * scale * 25.0F;
         this.state.pitchJitterVel = this.state.pitchJitterVel + HWangle.Util.gaussian() * 0.9F * scale * 15.0F;
      }

      float jitterFade = (float)Math.exp((double)(-14.0F * dt));
      this.state.yawJitterVel *= jitterFade;
      this.state.pitchJitterVel *= jitterFade;
      this.state.yawVel = this.state.yawVel + this.state.yawJitterVel * dt * 60.0F;
      this.state.pitchVel = this.state.pitchVel + this.state.pitchJitterVel * dt * 60.0F;
   }

   private void applyFrame(float dt) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         float yawDelta = Float.isFinite(this.state.yawVel * dt) ? this.state.yawVel * dt : 0.0F;
         float pitchDelta = Float.isFinite(this.state.pitchVel * dt) ? this.state.pitchVel * dt : 0.0F;
         float yaw = mc.player.getYaw() + yawDelta;
         float pitch = MathHelper.clamp(mc.player.getPitch() + pitchDelta, -90.0F, 90.0F);
         RotationController.update(
            new Rotation(yaw, pitch), 9999.0F, 9999.0F, HWangle.Util.random(20.0F, 40.0F), HWangle.Util.random(15.0F, 40.0F), 2, 100, false
         );
      }
   }

   private boolean isActive() {
      return this.aura != null && this.aura.isEnabled() && this.aura.mode.is("HolyWorld");
   }

   private void resetState() {
      this.aura = null;
      this.target = null;
      this.lastFrameNanos = 0L;
      this.state.reset();
      this.state.randomize();
   }

   public void reset() {
      this.resetState();
   }

   private static final class State {
      float yawVel;
      float pitchVel;
      float yawJitterVel;
      float pitchJitterVel;
      float jitterTimer;
      float jitterInterval;
      float driftPhaseYaw;
      float driftPhasePitch;
      float driftSeedYaw;
      float driftSeedPitch;
      double smoothedX;
      double smoothedY;
      double smoothedZ;
      boolean hasSmoothed;

      private State() {
      }

      void reset() {
         this.yawVel = this.pitchVel = 0.0F;
         this.yawJitterVel = this.pitchJitterVel = 0.0F;
         this.jitterTimer = this.driftPhaseYaw = this.driftPhasePitch = 0.0F;
         this.smoothedX = this.smoothedY = this.smoothedZ = 0.0;
         this.hasSmoothed = false;
      }

      void randomize() {
         this.driftSeedYaw = HWangle.Util.random(0.0F, (float) (Math.PI * 2));
         this.driftSeedPitch = HWangle.Util.random(0.0F, (float) (Math.PI * 2));
         this.jitterInterval = HWangle.Util.random(0.18F, 0.55F);
      }

      void decayMotion(float dt) {
         float decay = (float)Math.exp((double)(-12.0F * dt));
         this.yawVel *= decay;
         this.pitchVel *= decay;
         this.yawJitterVel *= decay;
         this.pitchJitterVel *= decay;
      }
   }

   private static final class Util {
      private Util() {
      }

      static float random(float min, float max) {
         return min + (max - min) * (float)Math.random();
      }

      static float gaussian() {
         double u1 = Math.max(1.0E-10, Math.random());
         double u2 = Math.random();
         return (float)(Math.sqrt(-2.0 * Math.log(u1)) * Math.cos((Math.PI * 2) * u2));
      }

      static float frameSeconds(long lastNanos, long now) {
         if (lastNanos == 0L) {
            return 0.016666668F;
         } else {
            long delta = now - lastNanos;
            return delta <= 0L ? 0.016666668F : Math.min((float)delta / 1.0E9F, 0.1F);
         }
      }
   }
}
