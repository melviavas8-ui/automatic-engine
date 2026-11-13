package daamky.ua.api.theme;

import java.awt.Color;

public record ClientTheme(String name, Color accent, Color surface) {
   public Color accentBright() {
      return mix(this.accent, Color.WHITE, 0.18F);
   }

   public Color accentDark() {
      return mix(this.accent, Color.BLACK, 0.28F);
   }

   public Color controlTop() {
      return mix(this.surface, this.accent, 0.22F);
   }

   public Color controlBottom() {
      return mix(this.surface, Color.BLACK, 0.18F);
   }

   public Color outline() {
      return mix(this.accent, Color.WHITE, 0.1F);
   }

   public Color surfaceBright() {
      return mix(this.surface, Color.WHITE, 0.08F);
   }

   public Color surfaceDark() {
      return mix(this.surface, Color.BLACK, 0.28F);
   }

   public Color surfaceDeep() {
      return mix(this.surface, Color.BLACK, 0.48F);
   }

   public Color badgeTop() {
      return mix(this.accent, Color.WHITE, 0.08F);
   }

   public Color badgeBottom() {
      return mix(this.accent, Color.BLACK, 0.34F);
   }

   private static Color mix(Color from, Color to, float amount) {
      float value = Math.max(0.0F, Math.min(1.0F, amount));
      return new Color(
         Math.round((float)from.getRed() + (float)(to.getRed() - from.getRed()) * value),
         Math.round((float)from.getGreen() + (float)(to.getGreen() - from.getGreen()) * value),
         Math.round((float)from.getBlue() + (float)(to.getBlue() - from.getBlue()) * value)
      );
   }
}
