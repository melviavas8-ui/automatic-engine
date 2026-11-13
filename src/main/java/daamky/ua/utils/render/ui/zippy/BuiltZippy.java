package daamky.ua.utils.render.ui.zippy;

import net.minecraft.client.gui.DrawContext;

public record BuiltZippy(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   int color,
   float smoothness,
   float timeOffset
) {
   public static final float DEFAULT_SMOOTHNESS = 1.0F;
   public static final int DEFAULT_COLOR = -1;

   public BuiltZippy(float x, float y, float width, float height, float radius, int color) {
      this(x, y, width, height, radius, radius, radius, radius, color, 1.0F, 0.0F);
   }

   public BuiltZippy(
      float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int color
   ) {
      this(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color, 1.0F, 0.0F);
   }

   public BuiltZippy withColor(int color) {
      return new BuiltZippy(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         color,
         this.smoothness,
         this.timeOffset
      );
   }

   public BuiltZippy withSmoothness(float smoothness) {
      return new BuiltZippy(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         smoothness,
         this.timeOffset
      );
   }

   public BuiltZippy withTimeOffset(float timeOffset) {
      return new BuiltZippy(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.smoothness,
         timeOffset
      );
   }

   public void render(DrawContext graphics) {
      ZippyRenderer.getInstance().draw(graphics, this);
   }

   public boolean visible() {
      return this.width > 0.0F && this.height > 0.0F && this.color >>> 24 != 0;
   }
}
