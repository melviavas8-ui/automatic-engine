package daamky.ua.utils.math;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.utils.render.animation.modernfx.Decelerate;
import daamky.ua.utils.render.animation.modernfx.Direction;

public final class MathUtils {
   public static final double PI2 = Math.PI * 2;

   private MathUtils() {
   }

   public static float smoothJitter(float speed, float strength, String math) {
      float x = (float)(System.currentTimeMillis() % 628L) * speed;
      double mathValue = Math.cos((double)x);
      if ("sin".equals(math)) {
         mathValue = Math.sin((double)x);
      } else if ("atan".equals(math)) {
         mathValue = Math.atan((double)x);
      }

      return (float)((double)strength * mathValue);
   }

   public static int getRandom(int min, int max) {
      if (min == max) {
         return min;
      } else {
         if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
         }

         return ThreadLocalRandom.current().nextInt(min, max + 1);
      }
   }

   public static float getRandom(float min, float max) {
      return (float)getRandom((double)min, (double)max);
   }

   public static double getRandom(double min, double max) {
      if (min == max) {
         return min;
      } else {
         if (min > max) {
            double tmp = min;
            min = max;
            max = tmp;
         }

         return ThreadLocalRandom.current().nextDouble(min, max);
      }
   }

   public static double computeGcd() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.options != null) {
         double factor = (Double)client.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
         return factor * factor * factor * 1.2;
      } else {
         return 0.0;
      }
   }

   public static float clamp(float value, float min, float max) {
      return value < min ? min : Math.min(value, max);
   }

   public static boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
   }

   public static Decelerate createAnimation(int ms, Direction direction) {
      Decelerate animation = new Decelerate();
      animation.setMs(ms);
      animation.setValue(1.0);
      animation.setDirection(direction);
      animation.counter.setTime(System.currentTimeMillis() - (long)ms - 1L);
      return animation;
   }

   public static String shortBind(KeyBind bind) {
      return bind != null && bind.isBound()
         ? bind.getDisplayName()
            .replace("MOUSE ", "M")
            .replace("L SHIFT", "LSH")
            .replace("R SHIFT", "RSH")
            .replace("L CTRL", "LCT")
            .replace("R CTRL", "RCT")
            .replace("SPACE", "SPC")
         : "...";
   }

   public static float interpolate(float prev, float to, float value) {
      return prev + (to - prev) * value;
   }

   public static double interpolate(double prev, double to, double value) {
      return prev + (to - prev) * value;
   }

   public static float interpolate(float prev, float to) {
      return MathHelper.lerp(tickDelta(), prev, to);
   }

   public static double interpolate(double prev, double to) {
      return MathHelper.lerp((double)tickDelta(), prev, to);
   }

   public static Vec3d interpolate(Vec3d previous, Vec3d current) {
      return previous != null && current != null
         ? new Vec3d(interpolate(previous.x, current.x), interpolate(previous.y, current.y), interpolate(previous.z, current.z))
         : Vec3d.ZERO;
   }

   public static Vec3d interpolate(Entity entity) {
      return entity == null
         ? Vec3d.ZERO
         : new Vec3d(interpolate(entity.lastX, entity.getX()), interpolate(entity.lastY, entity.getY()), interpolate(entity.lastZ, entity.getZ()));
   }

   private static float tickDelta() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client != null && client.getRenderTickCounter() != null ? client.getRenderTickCounter().getTickProgress(false) : 1.0F;
   }
}
