package daamky.ua.utils.render.ui.rectangle.rectdefault;

import net.minecraft.client.gui.DrawContext;
import daamky.ua.utils.render.color.ColorUtil;

public record BuiltRectangle(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft,
   float smoothness
) {
   public static final float DEFAULT_SMOOTHNESS = 1.0F;
   public static final int DEFAULT_COLOR = ColorUtil.WHITE;

   public BuiltRectangle(float x, float y, float width, float height, float radius, int color) {
      this(x, y, width, height, radius, radius, radius, radius, color, color, color, color, 1.0F);
   }

   public BuiltRectangle(
      float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int color
   ) {
      this(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color, color, color, color, 1.0F);
   }

   public BuiltRectangle withSmoothness(float smoothness) {
      return new BuiltRectangle(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         smoothness
      );
   }

   public BuiltRectangle withColor(int color) {
      return new BuiltRectangle(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         color,
         color,
         color,
         color,
         this.smoothness
      );
   }

   public void render(DrawContext graphics) {
      DefaultRectangleRenderer.getInstance().draw(graphics, this);
   }

   public boolean visible() {
      return this.width > 0.0F && this.height > 0.0F && (this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft) >>> 24 != 0;
   }
}
