package daamky.ua.utils.render.world.targetesp;

import net.minecraft.util.math.MathHelper;

public final class TargetEspMath {
   private TargetEspMath() {
   }

   public static float approach(float current, float target, float delta) {
      return current < target ? Math.min(current + delta, target) : Math.max(current - delta, target);
   }

   public static float easeOutCubic(float value) {
      float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
      float inv = 1.0F - clamped;
      return 1.0F - inv * inv * inv;
   }
}
