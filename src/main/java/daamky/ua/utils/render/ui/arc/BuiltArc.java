package daamky.ua.utils.render.ui.arc;

import java.util.Arrays;
import net.minecraft.client.gui.DrawContext;

public record BuiltArc(float x, float y, float size, float thickness, float degree, float rotation, int[] colors) {
   public BuiltArc(float x, float y, float size, float thickness, float degree, float rotation, int[] colors) {
      colors = normalizeColors(colors);
      this.x = x;
      this.y = y;
      this.size = size;
      this.thickness = thickness;
      this.degree = degree;
      this.rotation = rotation;
      this.colors = colors;
   }

   public BuiltArc(float x, float y, float size, float thickness, float degree, float rotation, int color) {
      this(x, y, size, thickness, degree, rotation, new int[]{color});
   }

   public void render(DrawContext graphics) {
      ArcRenderer.getInstance().draw(graphics, this);
   }

   public boolean visible() {
      return this.size > 0.0F && this.thickness > 0.0F && this.degree > 0.0F && this.hasVisibleColor();
   }

   private boolean hasVisibleColor() {
      for (int color : this.colors) {
         if (color >>> 24 != 0) {
            return true;
         }
      }

      return false;
   }

   private static int[] normalizeColors(int[] colors) {
      if (colors != null && colors.length != 0) {
         if (colors.length == 1) {
            int color = colors[0];
            int[] result = new int[9];
            Arrays.fill(result, color);
            return result;
         } else {
            int[] result = new int[9];

            for (int i = 0; i < result.length; i++) {
               result[i] = i < colors.length ? colors[i] : colors[colors.length - 1];
            }

            return result;
         }
      } else {
         return new int[9];
      }
   }
}
