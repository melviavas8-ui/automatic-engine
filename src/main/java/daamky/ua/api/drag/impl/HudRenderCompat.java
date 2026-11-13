package daamky.ua.api.drag.impl;

import java.lang.reflect.Field;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.blur.BuiltBlur;

public final class HudRenderCompat {
   private static final float NO_BLUR_RECT_ALPHA_SCALE = 0.78431374F;

   private HudRenderCompat() {
   }

   static void background(float x, float y, float width, float height, float radius, float blurRadius, float smoothness, int color) {
      if (hudBoolean("blur", true)) {
         Render2D.blur(x, y, width, height, radius, blurRadius, smoothness, color);
      } else {
         Render2D.rect(x, y, width, height, radius, noBlurRectColor(color));
      }
   }

   public static void background(BuiltBlur blur) {
      if (blur != null) {
         if (hudBoolean("blur", true)) {
            Render2D.blur(blur);
         } else {
            Render2D.rect(
               blur.x(),
               blur.y(),
               blur.width(),
               blur.height(),
               blur.radiusTopLeft(),
               blur.radiusTopRight(),
               blur.radiusBottomRight(),
               blur.radiusBottomLeft(),
               noBlurRectColor(blur.color())
            );
         }
      }
   }

   public static void glow(String texture, float x, float y, float width, float height, float radius, int color) {
      if (glowEnabled()) {
         Render2D.image(texture, x, y, width, height, radius, color);
      }
   }

   static boolean glowEnabled() {
      return hudBoolean("glow", true);
   }

   private static boolean hudBoolean(String fieldName, boolean fallback) {
      try {
         Class<?> hudClass = Class.forName("daamky.ua.api.module.impl.visual.Hud");
         Object hud = hudClass.getMethod("getInstance").invoke(null);
         if (hud == null) {
            return fallback;
         } else {
            Field field = hudClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object setting = field.get(hud);
            return setting.getClass().getMethod("getValue").invoke(setting) instanceof Boolean booleanValue ? booleanValue : fallback;
         }
      } catch (RuntimeException | ReflectiveOperationException var8) {
         return fallback;
      }
   }

   private static int noBlurRectColor(int color) {
      return ColorUtil.multAlpha(color, 0.78431374F);
   }
}
