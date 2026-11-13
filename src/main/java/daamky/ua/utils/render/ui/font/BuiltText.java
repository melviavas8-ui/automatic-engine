package daamky.ua.utils.render.ui.font;

public record BuiltText(
   String fontName,
   String text,
   float x,
   float y,
   float size,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft,
   float rotationDegrees,
   float rotationOriginX,
   float rotationOriginY,
   boolean fadeLeft,
   boolean fadeRight,
   float fadeLeftX,
   float fadeRightX,
   float fadeWidth,
   float fadeLeftStrength,
   float fadeRightStrength
) {
   public BuiltText(String fontName, String text, float x, float y, float size, int color) {
      this(fontName, text, x, y, size, color, color, color, color);
   }

   public BuiltText(String fontName, String text, float x, float y, float size, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft) {
      this(fontName, text, x, y, size, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft, 0.0F, x, y, false, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   public boolean visible() {
      return this.text != null
         && !this.text.isEmpty()
         && this.size > 0.0F
         && ((this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft) >>> 24 & 0xFF) > 0;
   }

   public boolean hasHorizontalFade() {
      return (this.fadeLeftStrength > 0.001F || this.fadeRightStrength > 0.001F) && this.fadeWidth > 0.0F && this.fadeRightX > this.fadeLeftX;
   }

   public BuiltText withHorizontalFade(float leftX, float rightX, float width, boolean left, boolean right) {
      return this.withHorizontalFade(leftX, rightX, width, left ? 1.0F : 0.0F, right ? 1.0F : 0.0F);
   }

   public BuiltText withHorizontalFade(float leftX, float rightX, float width, float leftStrength, float rightStrength) {
      return new BuiltText(
         this.fontName,
         this.text,
         this.x,
         this.y,
         this.size,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.rotationDegrees,
         this.rotationOriginX,
         this.rotationOriginY,
         leftStrength > 0.001F,
         rightStrength > 0.001F,
         leftX,
         rightX,
         width,
         clamp01(leftStrength),
         clamp01(rightStrength)
      );
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }
}
