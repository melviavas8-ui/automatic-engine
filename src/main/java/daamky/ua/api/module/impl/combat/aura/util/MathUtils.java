package daamky.ua.api.module.impl.combat.aura.util;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.Angle;

public final class MathUtils {
   private MathUtils() {
   }

   public static float getRandom(float min, float max) {
      if (min == max) {
         return min;
      } else {
         if (max < min) {
            float tmp = min;
            min = max;
            max = tmp;
         }

         return (float)ThreadLocalRandom.current().nextDouble((double)min, (double)max);
      }
   }

   public static float smoothRandom(float min, float max, float smoothness) {
      if (max < min) {
         float value = min;
         min = max;
         max = value;
      }

      double time = (double)System.currentTimeMillis() / ((double)Math.max(1.0F, smoothness) * 50.0);
      float progress = (float)((Math.sin(time) + 1.0) * 0.5);
      return MathHelper.lerp(progress, min, max);
   }

   public static double getrandomDouble(double min, double max) {
      if (min == max) {
         return min;
      } else {
         if (max < min) {
            double tmp = min;
            min = max;
            max = tmp;
         }

         return ThreadLocalRandom.current().nextDouble(min, max);
      }
   }

   public static Angle fromVec3d(Vec3d vector) {
      return new Angle(
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90.0),
         (float)MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
      );
   }

   public static double computeGcd() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.options != null) {
         double sensitivity = (Double)client.options.getMouseSensitivity().getValue();
         double factor = sensitivity * 0.6 + 0.2;
         return factor * factor * factor * 1.2;
      } else {
         return 0.0;
      }
   }
}
