package daamky.ua.api.module.impl.combat.aura.rotations.funtime;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import daamky.ua.api.module.impl.combat.aura.Angle;

public final class FunTimeAimAnimator {
   private static final ConcurrentHashMap<Class<?>, FunTimeAimAnimator> REGISTRY = new ConcurrentHashMap<>();
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static final long STALE_RESET_NANOS = 90000000L;
   private static final float REBASE_YAW_THRESHOLD = 25.0F;
   private static final float REBASE_PITCH_THRESHOLD = 12.0F;
   private final FunTimeAimAnimator.Axis yaw;
   private final FunTimeAimAnimator.Axis pitch;
   private boolean initialized;
   private boolean pitchInitialized;
   private long lastApplyNanos;
   private WeakReference<ClientWorld> lastLevel = new WeakReference<>(null);

   private FunTimeAimAnimator(FunTimeAimAnimator.Axis yaw, FunTimeAimAnimator.Axis pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   private FunTimeAimAnimator(float stiffness, float dampingRatio) {
      this(new FunTimeAimAnimator.SpringAxis(stiffness, dampingRatio), new FunTimeAimAnimator.SpringAxis(stiffness, dampingRatio));
   }

   private static float stiffness(float speed) {
      return speed * 60.0F + 100.0F;
   }

   private static FunTimeAimAnimator fromRegistry(Class<?> type, float stiffness, float dampingRatio) {
      FunTimeAimAnimator animator = REGISTRY.computeIfAbsent(type, ignored -> new FunTimeAimAnimator(stiffness, dampingRatio));
      if (animator.yaw instanceof FunTimeAimAnimator.SpringAxis yawAxis) {
         yawAxis.retune(stiffness, dampingRatio);
      }

      if (animator.pitch instanceof FunTimeAimAnimator.SpringAxis pitchAxis) {
         pitchAxis.retune(stiffness, dampingRatio);
      }

      return animator;
   }

   public static FunTimeAimAnimator back(float speed, Class<?> type) {
      return fromRegistry(type, stiffness(speed), 0.3F);
   }

   public static FunTimeAimAnimator nemesis(float speed, Class<?> type) {
      return fromRegistry(type, stiffness(speed), 0.6F);
   }

   public static void clear(Class<?> type) {
      REGISTRY.remove(type);
   }

   public static void clearAll() {
      REGISTRY.clear();
   }

   public Angle apply(Angle current, float targetYaw, float targetPitch) {
      ClientWorld currentLevel = MC.world;
      long now = System.nanoTime();
      if (this.lastLevel.get() != currentLevel) {
         this.initialized = false;
         this.pitchInitialized = false;
         this.lastLevel = new WeakReference<>(currentLevel);
      }

      if (!this.initialized || this.isStale(now) || this.isDiverged(current)) {
         Angle seed = current != null ? current : this.fallbackSeed(targetYaw, targetPitch);
         this.forceValue(seed.getYaw(), seed.getPitch());
      }

      this.lastApplyNanos = now;
      float normalizedTargetYaw = this.yaw.getValue() + MathHelper.wrapDegrees(targetYaw - this.yaw.getValue());
      this.yaw.setTarget(normalizedTargetYaw);
      this.pitch.setTarget(MathHelper.clamp(targetPitch, -90.0F, 90.0F));
      this.yaw.update();
      this.pitch.update();
      this.pitchInitialized = true;
      return new Angle(this.yaw.getValue(), MathHelper.clamp(this.pitch.getValue(), -90.0F, 90.0F));
   }

   public void forceValue(float yawValue, float pitchValue) {
      this.yaw.forceValue(yawValue);
      this.pitch.forceValue(MathHelper.clamp(pitchValue, -90.0F, 90.0F));
      this.initialized = true;
      this.pitchInitialized = true;
      this.lastApplyNanos = System.nanoTime();
   }

   public void reset() {
      this.initialized = false;
      this.pitchInitialized = false;
      this.lastApplyNanos = 0L;
      this.yaw.reset();
      this.pitch.reset();
   }

   private boolean isStale(long now) {
      return this.lastApplyNanos != 0L && now - this.lastApplyNanos > 90000000L;
   }

   private boolean isDiverged(Angle current) {
      if (current == null) {
         return false;
      } else {
         float yawDifference = Math.abs(MathHelper.wrapDegrees(current.getYaw() - this.yaw.getValue()));
         float pitchDifference = Math.abs(current.getPitch() - this.pitch.getValue());
         return yawDifference > 25.0F || pitchDifference > 12.0F;
      }
   }

   private Angle fallbackSeed(float targetYaw, float targetPitch) {
      return MC.player == null ? new Angle(targetYaw, MathHelper.clamp(targetPitch, -90.0F, 90.0F)) : new Angle(MC.player.getYaw(), MC.player.getPitch());
   }

   private interface Axis {
      void setTarget(float var1);

      void forceValue(float var1);

      void update();

      float getValue();

      void reset();
   }

   private static final class Spring {
      private static final float MAX_DELTA = 0.05F;
      private static final float SETTLE_THRESHOLD = 0.001F;
      private float stiffness;
      private float damping;
      private float value;
      private float velocity;
      private float target;
      private long lastUpdateTime;

      private Spring(float stiffness, float damping) {
         this.stiffness = stiffness;
         this.damping = damping;
         this.lastUpdateTime = System.nanoTime();
      }

      private void setTarget(float target) {
         this.target = target;
      }

      private void update() {
         long now = System.nanoTime();
         float delta = (float)(now - this.lastUpdateTime) / 1.0E9F;
         this.lastUpdateTime = now;
         delta = Math.min(delta, 0.05F);
         if (this.isSettled()) {
            this.value = this.target;
            this.velocity = 0.0F;
         } else {
            float force = -this.stiffness * (this.value - this.target);
            float dampingForce = -this.damping * this.velocity;
            float acceleration = force + dampingForce;
            this.velocity += acceleration * delta;
            this.value = this.value + this.velocity * delta;
         }
      }

      private boolean isSettled() {
         return Math.abs(this.value - this.target) < 0.001F && Math.abs(this.velocity) < 0.001F;
      }

      private void forceValue(float value) {
         this.value = value;
         this.target = value;
         this.velocity = 0.0F;
         this.lastUpdateTime = System.nanoTime();
      }

      private void reset() {
         this.value = 0.0F;
         this.target = 0.0F;
         this.velocity = 0.0F;
         this.lastUpdateTime = System.nanoTime();
      }
   }

   private static final class SpringAxis implements FunTimeAimAnimator.Axis {
      private final FunTimeAimAnimator.Spring spring;

      private SpringAxis(float stiffness, float dampingRatio) {
         this.spring = new FunTimeAimAnimator.Spring(stiffness, dampingRatio * 2.0F * (float)Math.sqrt((double)stiffness));
      }

      private void retune(float stiffness, float dampingRatio) {
         this.spring.stiffness = stiffness;
         this.spring.damping = dampingRatio * 2.0F * (float)Math.sqrt((double)stiffness);
      }

      @Override
      public void setTarget(float target) {
         this.spring.setTarget(target);
      }

      @Override
      public void forceValue(float value) {
         this.spring.forceValue(value);
      }

      @Override
      public void update() {
         this.spring.update();
      }

      @Override
      public float getValue() {
         return this.spring.value;
      }

      @Override
      public void reset() {
         this.spring.reset();
      }
   }
}
