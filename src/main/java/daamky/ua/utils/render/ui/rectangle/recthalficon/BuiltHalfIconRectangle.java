package daamky.ua.utils.render.ui.rectangle.recthalficon;

import net.minecraft.client.gui.DrawContext;

public record BuiltHalfIconRectangle(
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
   String fontName,
   String icon,
   float iconSize,
   int iconColor,
   float spacingX,
   float spacingY,
   float paddingX,
   float paddingY,
   float jitterX,
   float jitterY,
   float rotationMaxDegrees,
   long seed
) {
   public static final float DEFAULT_SMOOTHNESS = 1.0F;
   public static final float DEFAULT_ICON_SIZE = 8.0F;
   public static final float DEFAULT_SPACING = 14.0F;
   public static final float DEFAULT_PADDING = 2.0F;

   public BuiltHalfIconRectangle(
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      String fontName,
      String icon,
      float iconSize,
      int iconColor,
      float spacingX,
      float spacingY,
      float rotationMaxDegrees
   ) {
      this(
         x,
         y,
         width,
         height,
         radius,
         radius,
         radius,
         radius,
         color,
         color,
         color,
         color,
         1.0F,
         fontName,
         icon,
         iconSize,
         iconColor,
         spacingX,
         spacingY,
         2.0F,
         2.0F,
         0.0F,
         0.0F,
         rotationMaxDegrees,
         0L
      );
   }

   public BuiltHalfIconRectangle withBackground(int color) {
      return new BuiltHalfIconRectangle(
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
         this.fontName,
         this.icon,
         this.iconSize,
         this.iconColor,
         this.spacingX,
         this.spacingY,
         this.paddingX,
         this.paddingY,
         this.jitterX,
         this.jitterY,
         this.rotationMaxDegrees,
         this.seed
      );
   }

   public BuiltHalfIconRectangle withIcon(int iconColor, float iconSize, float spacingX, float spacingY) {
      return new BuiltHalfIconRectangle(
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
         this.fontName,
         this.icon,
         iconSize,
         iconColor,
         spacingX,
         spacingY,
         this.paddingX,
         this.paddingY,
         this.jitterX,
         this.jitterY,
         this.rotationMaxDegrees,
         this.seed
      );
   }

   public BuiltHalfIconRectangle withPadding(float paddingX, float paddingY) {
      return new BuiltHalfIconRectangle(
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
         this.fontName,
         this.icon,
         this.iconSize,
         this.iconColor,
         this.spacingX,
         this.spacingY,
         paddingX,
         paddingY,
         this.jitterX,
         this.jitterY,
         this.rotationMaxDegrees,
         this.seed
      );
   }

   public BuiltHalfIconRectangle withRandomization(float rotationMaxDegrees, float jitterX, float jitterY, long seed) {
      return new BuiltHalfIconRectangle(
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
         this.fontName,
         this.icon,
         this.iconSize,
         this.iconColor,
         this.spacingX,
         this.spacingY,
         this.paddingX,
         this.paddingY,
         jitterX,
         jitterY,
         rotationMaxDegrees,
         seed
      );
   }

   public void render(DrawContext graphics) {
      HalfIconRectangleRenderer.getInstance().draw(graphics, this);
   }

   public boolean visible() {
      return this.backgroundVisible() || this.iconsVisible();
   }

   public boolean backgroundVisible() {
      return this.width > 0.0F
         && this.height > 0.0F
         && ((this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft) >>> 24 & 0xFF) > 0;
   }

   public boolean iconsVisible() {
      return this.width > 0.0F
         && this.height > 0.0F
         && this.fontName != null
         && this.icon != null
         && !this.icon.isEmpty()
         && this.iconSize > 0.0F
         && this.iconColor >>> 24 != 0;
   }
}
