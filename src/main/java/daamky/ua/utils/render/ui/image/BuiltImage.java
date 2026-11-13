package daamky.ua.utils.render.ui.image;

import daamky.ua.utils.render.color.ColorUtil;

public record BuiltImage(
   String texture,
   float x,
   float y,
   float size,
   float explicitWidth,
   float explicitHeight,
   float radius,
   float smoothness,
   boolean nearestFilter,
   float rotationDegrees,
   float rotationOriginX,
   float rotationOriginY,
   float u0,
   float v0,
   float u1,
   float v1,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft
) {
   public static final int DEFAULT_COLOR = ColorUtil.WHITE;
   public static final float DEFAULT_SMOOTHNESS = 1.0F;

   public BuiltImage(String texture, float x, float y, float size, float radius) {
      this(
         texture,
         x,
         y,
         size,
         0.0F,
         0.0F,
         radius,
         1.0F,
         false,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         DEFAULT_COLOR,
         DEFAULT_COLOR,
         DEFAULT_COLOR,
         DEFAULT_COLOR
      );
   }

   public BuiltImage(String texture, float x, float y, float size, float radius, int color) {
      this(texture, x, y, size, 0.0F, 0.0F, radius, 1.0F, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, color, color, color, color);
   }

   public BuiltImage(String texture, float x, float y, float width, float height, float radius) {
      this(
         texture,
         x,
         y,
         0.0F,
         width,
         height,
         radius,
         1.0F,
         false,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         DEFAULT_COLOR,
         DEFAULT_COLOR,
         DEFAULT_COLOR,
         DEFAULT_COLOR
      );
   }

   public BuiltImage(String texture, float x, float y, float width, float height, float radius, int color) {
      this(texture, x, y, 0.0F, width, height, radius, 1.0F, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, color, color, color, color);
   }

   public boolean visible() {
      return this.texture != null
         && !this.texture.isBlank()
         && (this.size > 0.0F || this.explicitWidth > 0.0F && this.explicitHeight > 0.0F)
         && (
            effectiveAlpha(this.colorTopLeft) > 0
               || effectiveAlpha(this.colorTopRight) > 0
               || effectiveAlpha(this.colorBottomRight) > 0
               || effectiveAlpha(this.colorBottomLeft) > 0
         );
   }

   public boolean hasExplicitDimensions() {
      return this.explicitWidth > 0.0F && this.explicitHeight > 0.0F;
   }

   public BuiltImage withColors(int... colors) {
      if (colors != null && colors.length != 0) {
         if (colors.length == 1) {
            return new BuiltImage(
               this.texture,
               this.x,
               this.y,
               this.size,
               this.explicitWidth,
               this.explicitHeight,
               this.radius,
               this.smoothness,
               this.nearestFilter,
               this.rotationDegrees,
               this.rotationOriginX,
               this.rotationOriginY,
               this.u0,
               this.v0,
               this.u1,
               this.v1,
               colors[0],
               colors[0],
               colors[0],
               colors[0]
            );
         } else if (colors.length == 2) {
            return new BuiltImage(
               this.texture,
               this.x,
               this.y,
               this.size,
               this.explicitWidth,
               this.explicitHeight,
               this.radius,
               this.smoothness,
               this.nearestFilter,
               this.rotationDegrees,
               this.rotationOriginX,
               this.rotationOriginY,
               this.u0,
               this.v0,
               this.u1,
               this.v1,
               colors[0],
               colors[1],
               colors[1],
               colors[0]
            );
         } else {
            return colors.length == 3
               ? new BuiltImage(
                  this.texture,
                  this.x,
                  this.y,
                  this.size,
                  this.explicitWidth,
                  this.explicitHeight,
                  this.radius,
                  this.smoothness,
                  this.nearestFilter,
                  this.rotationDegrees,
                  this.rotationOriginX,
                  this.rotationOriginY,
                  this.u0,
                  this.v0,
                  this.u1,
                  this.v1,
                  colors[0],
                  colors[1],
                  colors[2],
                  colors[0]
               )
               : new BuiltImage(
                  this.texture,
                  this.x,
                  this.y,
                  this.size,
                  this.explicitWidth,
                  this.explicitHeight,
                  this.radius,
                  this.smoothness,
                  this.nearestFilter,
                  this.rotationDegrees,
                  this.rotationOriginX,
                  this.rotationOriginY,
                  this.u0,
                  this.v0,
                  this.u1,
                  this.v1,
                  colors[0],
                  colors[1],
                  colors[2],
                  colors[3]
               );
         }
      } else {
         return this;
      }
   }

   public BuiltImage withColor(int color) {
      return new BuiltImage(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.explicitWidth,
         this.explicitHeight,
         this.radius,
         this.smoothness,
         this.nearestFilter,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         this.u0,
         this.v0,
         this.u1,
         this.v1,
         color,
         color,
         color,
         color
      );
   }

   public BuiltImage withRotation(float rotationDegrees, float originX, float originY) {
      return new BuiltImage(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.explicitWidth,
         this.explicitHeight,
         this.radius,
         this.smoothness,
         this.nearestFilter,
         rotationDegrees,
         originX,
         originY,
         this.u0,
         this.v0,
         this.u1,
         this.v1,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft
      );
   }

   public BuiltImage withUv(float u0, float v0, float u1, float v1) {
      return new BuiltImage(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.explicitWidth,
         this.explicitHeight,
         this.radius,
         this.smoothness,
         this.nearestFilter,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         u0,
         v0,
         u1,
         v1,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft
      );
   }

   public BuiltImage withSmoothness(float smoothness) {
      return new BuiltImage(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.explicitWidth,
         this.explicitHeight,
         this.radius,
         smoothness,
         this.nearestFilter,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         this.u0,
         this.v0,
         this.u1,
         this.v1,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft
      );
   }

   public BuiltImage withNearestFilter() {
      return new BuiltImage(
         this.texture,
         this.x,
         this.y,
         this.size,
         this.explicitWidth,
         this.explicitHeight,
         this.radius,
         this.smoothness,
         true,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         this.u0,
         this.v0,
         this.u1,
         this.v1,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft
      );
   }

   private static int alpha(int color) {
      return color >>> 24 & 0xFF;
   }

   private static int effectiveAlpha(int color) {
      int alpha = alpha(color);
      return alpha == 0 && (color & 16777215) != 0 ? 255 : alpha;
   }
}
