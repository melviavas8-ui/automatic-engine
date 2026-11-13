package daamky.ua.utils.color;

import java.awt.Color;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

public final class ColorAssist {
   public static final int green = new Color(64, 255, 64).getRGB();
   public static final int yellow = new Color(255, 255, 64).getRGB();
   public static final int orange = new Color(255, 128, 32).getRGB();
   public static final int red = new Color(255, 64, 64).getRGB();

   private ColorAssist() {
   }

   public static int colorForRectsCustom$() {
      return new Color(91, 63, 212, 255).getRGB();
   }

   public static int colorForRectsBlack$() {
      return new Color(26, 26, 26, 255).getRGB();
   }

   public static int colorForTextWhite$() {
      return new Color(255, 255, 255, 255).getRGB();
   }

   public static int colorForTextCustom$() {
      return new Color(130, 100, 210, 255).getRGB();
   }

   public static int red(int color) {
      return color >> 16 & 0xFF;
   }

   public static int green(int color) {
      return color >> 8 & 0xFF;
   }

   public static int blue(int color) {
      return color & 0xFF;
   }

   public static int alpha(int color) {
      return color >> 24 & 0xFF;
   }

   public static float redf(int color) {
      return (float)red(color) / 255.0F;
   }

   public static float greenf(int color) {
      return (float)green(color) / 255.0F;
   }

   public static float bluef(int color) {
      return (float)blue(color) / 255.0F;
   }

   public static float alphaf(int color) {
      return (float)alpha(color) / 255.0F;
   }

   public static int[] getRGB(int color) {
      return new int[]{red(color), green(color), blue(color)};
   }

   public static int getColor(int brightness, int alpha) {
      return getColor(brightness, brightness, brightness, alpha);
   }

   public static int getColor(int brightness, float alpha) {
      return getColor(brightness, Math.round(alpha * 255.0F));
   }

   public static int getColor(int brightness) {
      return getColor(brightness, brightness, brightness);
   }

   public static int getColor(int red, int green, int blue) {
      return getColor(red, green, blue, 255);
   }

   public static int getColor(int red, int green, int blue, int alpha) {
      return ColorHelper.getArgb(
         MathHelper.clamp(alpha, 0, 255), MathHelper.clamp(red, 0, 255), MathHelper.clamp(green, 0, 255), MathHelper.clamp(blue, 0, 255)
      );
   }

   public static int applyOpacity(int hex, float opacity) {
      return ColorHelper.getArgb(
         (int)((float)ColorHelper.getAlpha(hex) * (opacity / 255.0F)), ColorHelper.getRed(hex), ColorHelper.getGreen(hex), ColorHelper.getBlue(hex)
      );
   }

   public static int calculateHuyDegrees(int divisor, int offset) {
      long currentTime = System.currentTimeMillis();
      long calculatedValue = (currentTime / (long)Math.max(1, divisor) + (long)offset) % 360L;
      return (int)calculatedValue;
   }

   public static int reAlphaInt(int color, int alpha) {
      return MathHelper.clamp(alpha, 0, 255) << 24 | color & 16777215;
   }

   public static int astolfo(int speed, int index, float saturation, float brightness, float alpha) {
      float hueStep = 90.0F;
      float baseHue = (float)calculateHuyDegrees(speed, index);
      float hue = (baseHue + (float)index * hueStep) % 360.0F;
      int rgb = Color.HSBtoRGB(hue / 360.0F, MathHelper.clamp(saturation, 0.0F, 1.0F), MathHelper.clamp(brightness, 0.0F, 1.0F));
      return reAlphaInt(rgb, MathHelper.clamp((int)(alpha * 255.0F), 0, 255));
   }

   public static int toColor(String hexColor) {
      if (hexColor != null && hexColor.startsWith("#")) {
         int rgb = Integer.parseInt(hexColor.substring(1), 16);
         return setAlpha(rgb, 255);
      } else {
         return colorForTextWhite$();
      }
   }

   public static int setAlpha(int color, int alpha) {
      return color & 16777215 | MathHelper.clamp(alpha, 0, 255) << 24;
   }

   public static int interpolateColor(int color1, int color2, float amount) {
      float value = MathHelper.clamp(amount, 0.0F, 1.0F);
      int red = interpolateInt(getRed(color1), getRed(color2), (double)value);
      int green = interpolateInt(getGreen(color1), getGreen(color2), (double)value);
      int blue = interpolateInt(getBlue(color1), getBlue(color2), (double)value);
      int alpha = interpolateInt(getAlpha(color1), getAlpha(color2), (double)value);
      return getColor(red, green, blue, alpha);
   }

   public static Double interpolateD(double oldValue, double newValue, double interpolationValue) {
      return oldValue + (newValue - oldValue) * interpolationValue;
   }

   public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
      return interpolateD((double)oldValue, (double)newValue, interpolationValue).intValue();
   }

   public static int getRed(int hex) {
      return hex >> 16 & 0xFF;
   }

   public static int getGreen(int hex) {
      return hex >> 8 & 0xFF;
   }

   public static int getBlue(int hex) {
      return hex & 0xFF;
   }

   public static int getAlpha(int hex) {
      return hex >> 24 & 0xFF;
   }

   public static int overCol(int color1, int color2, float percent01) {
      float percent = MathHelper.clamp(percent01, 0.0F, 1.0F);
      return getColor(
         MathHelper.lerp(percent, red(color1), red(color2)),
         MathHelper.lerp(percent, green(color1), green(color2)),
         MathHelper.lerp(percent, blue(color1), blue(color2)),
         MathHelper.lerp(percent, alpha(color1), alpha(color2))
      );
   }

   public static int multRedAndAlpha(int color, float red, float alpha) {
      return getColor(
         red(color),
         Math.min(255, Math.round((float)green(color) / red)),
         Math.min(255, Math.round((float)blue(color) / red)),
         Math.round((float)alpha(color) * alpha)
      );
   }

   public static int rgba(int red, int green, int blue, int alpha) {
      return getColor(red, green, blue, alpha);
   }

   public static float[] rgba(int color) {
      return new float[]{
         (float)(color >> 16 & 0xFF) / 255.0F, (float)(color >> 8 & 0xFF) / 255.0F, (float)(color & 0xFF) / 255.0F, (float)(color >> 24 & 0xFF) / 255.0F
      };
   }

   public static int interpolate(int start, int end, float value) {
      return interpolateColor(start, end, value);
   }

   public static int[] genGradientForText(int color1, int color2, int length) {
      int safeLength = Math.max(0, length);
      int[] gradient = new int[safeLength];
      if (safeLength == 0) {
         return gradient;
      } else if (safeLength == 1) {
         gradient[0] = color1;
         return gradient;
      } else {
         for (int i = 0; i < safeLength; i++) {
            gradient[i] = overCol(color1, color2, (float)i / (float)(safeLength - 1));
         }

         return gradient;
      }
   }
}
