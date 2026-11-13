package daamky.ua.utils.render.ui.blur;

import daamky.ua.utils.render.color.ColorUtil;

public final class BlurBuilder {
   private float x;
   private float y;
   private float width;
   private float height;
   private float radiusTopLeft;
   private float radiusTopRight;
   private float radiusBottomRight;
   private float radiusBottomLeft;
   private float smoothness = 1.0F;
   private float blurRadius = 16.0F;
   private int color = ColorUtil.WHITE;

   public BlurBuilder() {
   }

   public BlurBuilder rectangle(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      return this;
   }

   public BlurBuilder rectangle(float x, float y, float width, float height, float cornerRadius) {
      return this.rectangle(x, y, width, height).radius(cornerRadius);
   }

   public BlurBuilder radius(float radius) {
      this.radiusTopLeft = radius;
      this.radiusTopRight = radius;
      this.radiusBottomRight = radius;
      this.radiusBottomLeft = radius;
      return this;
   }

   public BlurBuilder radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
      this.radiusTopLeft = topLeft;
      this.radiusTopRight = topRight;
      this.radiusBottomRight = bottomRight;
      this.radiusBottomLeft = bottomLeft;
      return this;
   }

   public BlurBuilder smoothness(float smoothness) {
      this.smoothness = smoothness;
      return this;
   }

   public BlurBuilder blurRadius(float blurRadius) {
      this.blurRadius = blurRadius;
      return this;
   }

   public BlurBuilder color(int color) {
      this.color = color;
      return this;
   }

   public BuiltBlur build() {
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
         this.color
      );
   }
}
