package daamky.ua.api.module.impl.combat.aura.rotations.funtime;

import java.util.concurrent.ThreadLocalRandom;

public final class FTMathUtil {
   private static final float SINE_HALF_AMPLITUDE = 0.5F;
   private static final float SINE_OFFSET = 0.5F;
   private static final double SMOOTH_RANDOM_TIME_SCALE_MS = 1000.0;

   private FTMathUtil() {
   }

   public static float random(float min, float max) {
      return ThreadLocalRandom.current().nextFloat() * (max - min) + min;
   }

   public static float smoothRandom(float min, float max, float speed) {
      double t = (double)System.currentTimeMillis() * (double)speed / 1000.0;
      float normalized = (float)(Math.sin(t) * 0.5 + 0.5);
      return lerp(min, max, normalized);
   }

   private static float lerp(float min, float max, float delta) {
      return min + (max - min) * delta;
   }
}
