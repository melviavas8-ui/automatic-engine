package daamky.ua.utils.render.ui.blur;

import net.minecraft.client.gui.DrawContext;
import daamky.ua.utils.render.color.ColorUtil;

public record BuiltBlur(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   float smoothness,
   float blurRadius,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft
) {
   private static final int DEFAULT_COLOR = ColorUtil.WHITE;

   public BuiltBlur(float x, float y, float width, float height, float radius, float smoothness, float blurRadius) {
      this(x, y, width, height, radius, radius, radius, radius, smoothness, blurRadius, DEFAULT_COLOR);
   }

   public BuiltBlur(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float smoothness,
      float blurRadius,
      int color
   ) {
      this(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, smoothness, blurRadius, color, color, color, color);
   }

   public void render(DrawContext graphics) {
      BlurFramebuffer.getInstance().draw(graphics, this);
   }

   public BuiltBlur withColor(int color) {
      return this.withColors(color, color, color, color);
   }

   public BuiltBlur withColors(int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft) {
      return new BuiltBlur(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.smoothness,
         this.blurRadius,
         colorTopLeft,
         colorTopRight,
         colorBottomRight,
         colorBottomLeft
      );
   }

   public int color() {
      return this.colorTopLeft;
   }

   public boolean visible() {
      int colors = this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft;
      return this.width > 0.0F && this.height > 0.0F && colors >>> 24 != 0 && this.blurRadius > 0.0F;
   }
}
