package daamky.ua.utils.render.ui.outline.outlinedefault;

import net.minecraft.client.gui.DrawContext;
import daamky.ua.utils.render.color.ColorUtil;

public record BuiltOutline(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   float thickness,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft,
   float smoothness
) {
   public static final float DEFAULT_SMOOTHNESS = 1.0F;
   public static final int DEFAULT_COLOR = ColorUtil.WHITE;

   public BuiltOutline(float x, float y, float width, float height, float radius, float thickness, int color) {
      this(x, y, width, height, radius, radius, radius, radius, thickness, color, color, color, color, 1.0F);
   }

   public BuiltOutline(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float thickness,
      int color
   ) {
      this(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, thickness, color, color, color, color, 1.0F);
   }

   public BuiltOutline withSmoothness(float smoothness) {
      return new BuiltOutline(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         smoothness
      );
   }

   public BuiltOutline withColor(int color) {
      return new BuiltOutline(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         color,
         color,
         color,
         color,
         this.smoothness
      );
   }

   public void render(DrawContext graphics) {
      DefaultOutlineRenderer.getInstance().draw(graphics, this);
   }

   public boolean visible() {
      int colors = this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft;
      return this.width > 0.0F && this.height > 0.0F && this.thickness > 0.0F && colors >>> 24 != 0;
   }
}
