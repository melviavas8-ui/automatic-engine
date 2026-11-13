package daamky.ua.api.module.impl.combat.aura.utils;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import daamky.ua.api.module.impl.combat.aura.Angle;

public final class Animator {
   private static final ConcurrentHashMap<Object, Animator> REGISTRY = new ConcurrentHashMap<>();
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final long STALE_RESET_NANOS = 90000000L;
   private static final float REBASE_YAW_THRESHOLD = 25.0F;
   private static final float REBASE_PITCH_THRESHOLD = 12.0F;
   private final Animator.Axis yaw;
   private final Animator.Axis pitch;
   private boolean initialized = false;
   private boolean pitchInitialized = false;
   private long lastApplyNanos = 0L;
   private WeakReference<ClientWorld> lastLevel = new WeakReference<>(null);

   private Animator(Animator.Axis yaw, Animator.Axis pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   private Animator(float stiffness, float dampingRatio) {
      this(new Animator.SpringAxis(stiffness, dampingRatio), new Animator.SpringAxis(stiffness, dampingRatio));
   }

   private static float s(float speed) {
      return speed * 60.0F + 100.0F;
   }

   private static Animator fromRegistry(Class<?> type, float stiffness, float ratio) {
      Animator anim = REGISTRY.computeIfAbsent(type, k -> new Animator(stiffness, ratio));
      if (anim.yaw instanceof Animator.SpringAxis ya) {
         ya.retune(stiffness, ratio);
      }

      if (anim.pitch instanceof Animator.SpringAxis pa) {
         pa.retune(stiffness, ratio);
      }

      return anim;
   }

   public static Animator back(float speed, Class<?> type) {
      return fromRegistry(type, s(speed), 0.3F);
   }

   public static Animator nemesis(float speed, Class<?> type) {
      return fromRegistry(type, s(speed), 0.6F);
   }

   public static Animator test(float speed, Class<?> type) {
      return fromRegistry(type, s(speed), 0.8F);
   }

   public static Animator funtime(float speed, Class<?> type) {
      return fromRegistry(type, s(speed), 0.7F);
   }

   public static Animator spooky(float speed, Class<?> type) {
      return fromRegistry(type, s(speed), 0.3F);
   }

   public static Animator custom(Class<?> type, float stiffness, float damping) {
      Object key = new Animator.Key(type, "_customSpring", "_customSpring");
      Animator anim = REGISTRY.computeIfAbsent(key, k -> new Animator(stiffness, damping));
      if (anim.yaw instanceof Animator.SpringAxis ya) {
         ya.retune(stiffness, damping);
      }

      if (anim.pitch instanceof Animator.SpringAxis pa) {
         pa.retune(stiffness, damping);
      }

      return anim;
   }

   public static Animator customAxes(Class<?> type, float yStiff, float yDamp, float pStiff, float pDamp) {
      Object key = new Animator.Key(type, "_customAxes", "_customAxes");
      Animator anim = REGISTRY.computeIfAbsent(key, k -> new Animator(new Animator.SpringAxis(yStiff, yDamp), new Animator.SpringAxis(pStiff, pDamp)));
      if (anim.yaw instanceof Animator.SpringAxis ya) {
         ya.retune(yStiff, yDamp);
      }

      if (anim.pitch instanceof Animator.SpringAxis pa) {
         pa.retune(pStiff, pDamp);
      }

      return anim;
   }

   public static Animator pair(String yawType, String pitchType, float speed) {
      return new Animator(Animator.AxisFactory.create(yawType, speed), Animator.AxisFactory.create(pitchType, speed));
   }

   public static Animator pair(Class<?> type, String yawType, String pitchType, float speed) {
      Animator.Key key = new Animator.Key(type, yawType, pitchType);
      Animator anim = REGISTRY.get(key);
      if (anim == null) {
         anim = new Animator(Animator.AxisFactory.create(yawType, speed), Animator.AxisFactory.create(pitchType, speed));
         REGISTRY.put(key, anim);
      } else {
         anim.retuneSpring(speed);
      }

      return anim;
   }

   private static Animator cachedSingle(Class<?> type, String name, float speed) {
      Animator.Key key = new Animator.Key(type, name, name);
      Animator anim = REGISTRY.get(key);
      if (anim == null) {
         anim = new Animator(Animator.AxisFactory.create(name, speed), Animator.AxisFactory.create(name, speed));
         REGISTRY.put(key, anim);
      } else {
         anim.retuneSpring(speed);
      }

      return anim;
   }

   private void retuneSpring(float speed) {
      if (this.yaw instanceof Animator.SpringAxis ya) {
         ya.retune(s(speed), ya.getDampingRatio());
      }

      if (this.pitch instanceof Animator.SpringAxis pa) {
         pa.retune(s(speed), pa.getDampingRatio());
      }

      if (this.yaw instanceof Animator.TunableAxis ya) {
         ya.retune(speed);
      }

      if (this.pitch instanceof Animator.TunableAxis pa) {
         pa.retune(speed);
      }
   }

   private static float springRatioFromKnown(Animator.Axis a) {
      return a instanceof Animator.SpringAxis sa ? sa.getDampingRatio() : 1.2F;
   }

   public static Animator.Builder builder() {
      return new Animator.Builder();
   }

   public static void clear(Class<?> type) {
      REGISTRY.entrySet().removeIf(e -> {
         Object k = e.getKey();
         if (k == type) {
            return true;
         } else {
            if (k instanceof Animator.Key key && key.owner == type) {
               return true;
            }

            return false;
         }
      });
   }

   public static void clearAll() {
      REGISTRY.clear();
   }

   public Angle apply(float targetYaw, float targetPitch) {
      return this.apply(null, targetYaw, targetPitch);
   }

   public Angle apply(Angle target) {
      return target == null ? this.fallbackSeed(0.0F, 0.0F) : this.apply(null, target.getYaw(), target.getPitch());
   }

   public Angle apply(Angle current, Angle target) {
      return target == null
         ? this.apply(current, current != null ? current.getYaw() : 0.0F, current != null ? current.getPitch() : 0.0F)
         : this.apply(current, target.getYaw(), target.getPitch());
   }

   public Angle apply(Angle current, float targetYaw, float targetPitch) {
      ClientWorld currentLevel = mc.world;
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

   public float applyPitch(float currentPitch, float targetPitch) {
      ClientWorld currentLevel = mc.world;
      long now = System.nanoTime();
      if (this.lastLevel.get() != currentLevel) {
         this.initialized = false;
         this.pitchInitialized = false;
         this.lastLevel = new WeakReference<>(currentLevel);
      }

      if (!this.pitchInitialized || this.isStale(now) || Math.abs(currentPitch - this.pitch.getValue()) > 12.0F) {
         this.pitch.forceValue(MathHelper.clamp(currentPitch, -90.0F, 90.0F));
         this.pitchInitialized = true;
      }

      this.lastApplyNanos = now;
      this.pitch.setTarget(MathHelper.clamp(targetPitch, -90.0F, 90.0F));
      this.pitch.update();
      return MathHelper.clamp(this.pitch.getValue(), -90.0F, 90.0F);
   }

   public void forceValue(float valueYaw, float valuePitch) {
      this.yaw.forceValue(valueYaw);
      this.pitch.forceValue(MathHelper.clamp(valuePitch, -90.0F, 90.0F));
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
         float yawDiff = Math.abs(MathHelper.wrapDegrees(current.getYaw() - this.yaw.getValue()));
         float pitchDiff = Math.abs(current.getPitch() - this.pitch.getValue());
         return yawDiff > 25.0F || pitchDiff > 12.0F;
      }
   }

   private Angle fallbackSeed(float targetYaw, float targetPitch) {
      return mc.player == null ? new Angle(targetYaw, MathHelper.clamp(targetPitch, -90.0F, 90.0F)) : new Angle(mc.player.getYaw(), mc.player.getPitch());
   }

   private interface Axis {
      void setTarget(float var1);

      void forceValue(float var1);

      void update();

      float getValue();

      void reset();
   }

   private static final class AxisFactory {
      private AxisFactory() {
      }

      private static Animator.Axis create(String name, float speed) {
         float s = Math.max(0.01F, speed);

         return (Animator.Axis)(switch (name) {
            case "Back" -> spring(s, 0.3F);
            case "Spooky" -> spring(s, 0.4F);
            case "Smooth" -> spring(s * 0.85F, 1.15F);
            case "Test" -> spring(s, 1.0F);
            case "Snap" -> spring(s * 1.35F, 0.7F);
            case "Elastic" -> spring(s * 0.95F, 0.18F);
            case "Heavy" -> spring(s * 0.55F, 1.7F);
            case "Glide" -> new Animator.GlideAxis(s);
            case "Flick" -> new Animator.FlickAxis(s);
            default -> spring(s, 1.2F);
         });
      }

      private static Animator.SpringAxis spring(float speed, float ratio) {
         float stiffness = speed * 60.0F + 100.0F;
         return new Animator.SpringAxis(stiffness, ratio);
      }

      private static Animator.Axis createCustom(Function<Float, Animator.Axis> custom, float speed) {
         return custom.apply(speed);
      }
   }

   public static class Builder {
      private String yawName = "Smooth";
      private String pitchName = "Smooth";
      private float speed = 5.0F;
      private Class<?> cacheKey = null;

      public Builder() {
      }

      public Animator.Builder yaw(String name) {
         this.yawName = name;
         return this;
      }

      public Animator.Builder pitch(String name) {
         this.pitchName = name;
         return this;
      }

      public Animator.Builder speed(float speed) {
         this.speed = speed;
         return this;
      }

      public Animator.Builder cacheKey(Class<?> c) {
         this.cacheKey = c;
         return this;
      }

      public Animator build() {
         return this.cacheKey != null
            ? Animator.pair(this.cacheKey, this.yawName, this.pitchName, this.speed)
            : Animator.pair(this.yawName, this.pitchName, this.speed);
      }
   }

   private static final class FlickAxis implements Animator.Axis, Animator.TunableAxis {
      private static final float MAX_DELTA = 0.05F;
      private static final float MIN_DELTA = 0.001F;
      private static final float SNAP_DISTANCE = 0.02F;
      private static final float SNAP_VELOCITY = 0.03F;
      private float value;
      private float target;
      private float velocity;
      private float responsiveness;
      private float damping;
      private float flickBoost;
      private long lastUpdateTime;

      private FlickAxis(float speed) {
         this.retune(speed);
         this.reset();
      }

      @Override
      public void retune(float speed) {
         float safeSpeed = Math.max(0.01F, speed);
         this.responsiveness = 34.0F + safeSpeed * 11.0F;
         this.damping = 9.0F + safeSpeed * 2.2F;
         this.flickBoost = 0.16F + safeSpeed * 0.018F;
      }

      @Override
      public void setTarget(float target) {
         float delta = MathHelper.wrapDegrees(target - this.target);
         this.target = target;
         float impulse = delta * this.flickBoost;
         this.velocity = this.velocity + MathHelper.clamp(impulse, -90.0F, 90.0F);
      }

      @Override
      public void forceValue(float value) {
         this.value = value;
         this.target = value;
         this.velocity = 0.0F;
         this.lastUpdateTime = System.nanoTime();
      }

      @Override
      public void update() {
         float delta = this.deltaSeconds();
         float error = MathHelper.wrapDegrees(this.target - this.value);
         float accel = error * this.responsiveness - this.velocity * this.damping;
         this.velocity += accel * delta;
         this.value = this.value + this.velocity * delta;
         if (Math.abs(error) < 0.02F && Math.abs(this.velocity) < 0.03F) {
            this.value = this.target;
            this.velocity = 0.0F;
         }
      }

      @Override
      public float getValue() {
         return this.value;
      }

      @Override
      public void reset() {
         this.value = 0.0F;
         this.target = 0.0F;
         this.velocity = 0.0F;
         this.lastUpdateTime = System.nanoTime();
      }

      private float deltaSeconds() {
         long now = System.nanoTime();
         float delta = (float)(now - this.lastUpdateTime) / 1.0E9F;
         this.lastUpdateTime = now;
         return MathHelper.clamp(delta, 0.001F, 0.05F);
      }
   }

   private static final class GlideAxis implements Animator.Axis, Animator.TunableAxis {
      private static final float MAX_DELTA = 0.05F;
      private static final float MIN_DELTA = 0.001F;
      private static final float SNAP_DISTANCE = 0.01F;
      private static final float SNAP_VELOCITY = 0.02F;
      private float value;
      private float target;
      private float velocity;
      private float gain;
      private float damping;
      private long lastUpdateTime;

      private GlideAxis(float speed) {
         this.retune(speed);
         this.reset();
      }

      @Override
      public void retune(float speed) {
         float safeSpeed = Math.max(0.01F, speed);
         this.gain = 22.0F + safeSpeed * 7.0F;
         this.damping = 7.0F + safeSpeed * 1.6F;
      }

      @Override
      public void setTarget(float target) {
         this.target = target;
      }

      @Override
      public void forceValue(float value) {
         this.value = value;
         this.target = value;
         this.velocity = 0.0F;
         this.lastUpdateTime = System.nanoTime();
      }

      @Override
      public void update() {
         float delta = this.deltaSeconds();
         float error = MathHelper.wrapDegrees(this.target - this.value);
         float accel = error * this.gain - this.velocity * this.damping;
         this.velocity += accel * delta;
         this.value = this.value + this.velocity * delta;
         if (Math.abs(error) < 0.01F && Math.abs(this.velocity) < 0.02F) {
            this.value = this.target;
            this.velocity = 0.0F;
         }
      }

      @Override
      public float getValue() {
         return this.value;
      }

      @Override
      public void reset() {
         this.value = 0.0F;
         this.target = 0.0F;
         this.velocity = 0.0F;
         this.lastUpdateTime = System.nanoTime();
      }

      private float deltaSeconds() {
         long now = System.nanoTime();
         float delta = (float)(now - this.lastUpdateTime) / 1.0E9F;
         this.lastUpdateTime = now;
         return MathHelper.clamp(delta, 0.001F, 0.05F);
      }
   }

   private static record Key(Class<?> owner, String yawType, String pitchType) {
      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else {
            return !(o instanceof Animator.Key k)
               ? false
               : this.owner == k.owner && Objects.equals(this.yawType, k.yawType) && Objects.equals(this.pitchType, k.pitchType);
         }
      }
   }

   private static class Spring {
      private static final float MAX_DELTA = 0.05F;
      private static final float SETTLE_THRESHOLD = 0.001F;
      private static final float MASS = 1.0F;
      private float stiffness;
      private float damping;
      private float value;
      private float velocity;
      private float target;
      private long lastUpdateTime;

      private Spring(float stiffness, float damping) {
         this.stiffness = stiffness;
         this.damping = damping;
         this.value = 0.0F;
         this.target = 0.0F;
         this.velocity = 0.0F;
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
            float acceleration = (force + dampingForce) / 1.0F;
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

      @Generated
      public float getStiffness() {
         return this.stiffness;
      }

      @Generated
      public void setStiffness(float stiffness) {
         this.stiffness = stiffness;
      }

      @Generated
      public float getDamping() {
         return this.damping;
      }

      @Generated
      public void setDamping(float damping) {
         this.damping = damping;
      }

      @Generated
      public float getValue() {
         return this.value;
      }

      @Generated
      public float getVelocity() {
         return this.velocity;
      }
   }

   private static class SpringAxis implements Animator.Axis {
      private final Animator.Spring spring;
      private float dampingRatio;

      private SpringAxis(float stiffness, float dampingRatio) {
         float damping = dampingRatio * 2.0F * (float)Math.sqrt((double)stiffness);
         this.spring = new Animator.Spring(stiffness, damping);
         this.dampingRatio = dampingRatio;
      }

      private float getDampingRatio() {
         return this.dampingRatio;
      }

      private void retune(float stiffness, float dampingRatio) {
         float damping = dampingRatio * 2.0F * (float)Math.sqrt((double)stiffness);
         this.spring.setStiffness(stiffness);
         this.spring.setDamping(damping);
         this.dampingRatio = dampingRatio;
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
         return this.spring.getValue();
      }

      @Override
      public void reset() {
         this.spring.reset();
      }
   }

   private interface TunableAxis {
      void retune(float var1);
   }
}
