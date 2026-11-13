package daamky.ua.utils.render.ui.rectangle.recthalftone;

import net.minecraft.client.gui.DrawContext;
import daamky.ua.utils.render.color.ColorUtil;

public record BuiltHalftoneRectangle(
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
   float smoothness,
   int dotColor,
   float dotSize,
   float dotSpacing
) {
   public static final float DEFAULT_SMOOTHNESS = 1.0F;
   public static final int DEFAULT_COLOR = ColorUtil.WHITE;
   public static final int DEFAULT_DOT_COLOR = ColorUtil.rgba(0, 0, 0, 204);
   public static final float DEFAULT_DOT_SIZE = 1.25F;
   public static final float DEFAULT_DOT_SPACING = 4.0F;

   public BuiltHalftoneRectangle(float x, float y, float width, float height, float radius, int color, int dotColor, float dotSize, float dotSpacing) {
      this(x, y, width, height, radius, radius, radius, radius, color, color, color, color, 1.0F, dotColor, dotSize, dotSpacing);
   }

   public BuiltHalftoneRectangle(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int color,
      int dotColor,
      float dotSize,
      float dotSpacing
   ) {
      this(
         x,
         y,
         width,
         height,
         radiusTopLeft,
         radiusTopRight,
         radiusBottomRight,
         radiusBottomLeft,
         color,
         color,
         color,
         color,
         1.0F,
         dotColor,
         dotSize,
         dotSpacing
      );
   }

   public BuiltHalftoneRectangle withSmoothness(float smoothness) {
      return new BuiltHalftoneRectangle(
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
         smoothness,
         this.dotColor,
         this.dotSize,
         this.dotSpacing
      );
   }

   public BuiltHalftoneRectangle withColor(int color) {
      return new BuiltHalftoneRectangle(
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
         this.smoothness,
         this.dotColor,
         this.dotSize,
         this.dotSpacing
      );
   }

   public BuiltHalftoneRectangle withDots(int dotColor, float dotSize, float dotSpacing) {
      return new BuiltHalftoneRectangle(
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
         this.smoothness,
         dotColor,
         dotSize,
         dotSpacing
      );
   }

   public void render(DrawContext graphics) {
      HalftoneRectangleRenderer.getInstance().draw(graphics, this);
   }

   public boolean visible() {
      int colors = this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft | this.dotColor;
      return this.width > 0.0F && this.height > 0.0F && colors >>> 24 != 0;
   }
}
