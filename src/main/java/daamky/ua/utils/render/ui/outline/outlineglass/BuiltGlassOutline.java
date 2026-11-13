package daamky.ua.utils.render.ui.outline.outlineglass;

import net.minecraft.client.gui.DrawContext;

public record BuiltGlassOutline(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   float thickness,
   int color,
   float globalAlpha,
   float fresnelPower,
   int fresnelColor,
   float baseAlpha,
   boolean fresnelInvert,
   float fresnelMix,
   float distortStrength,
   float squirt,
   float smoothness,
   float z
) {
   public static final float DEFAULT_SMOOTHNESS = 1.0F;

   public BuiltGlassOutline(
      float x,
      float y,
      float width,
      float height,
      float[] radius,
      float thickness,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      this(
         x,
         y,
         width,
         height,
         radiusValue(radius, 0),
         radiusValue(radius, 1),
         radiusValue(radius, 2),
         radiusValue(radius, 3),
         thickness,
         color,
         globalAlpha,
         fresnelPower,
         fresnelColor,
         baseAlpha,
         fresnelInvert,
         fresnelMix,
         distortStrength,
         squirt,
         1.0F,
         z
      );
   }

   public void render(DrawContext graphics) {
      GlassOutlineRenderer.getInstance().draw(graphics, this);
   }

   public boolean visible() {
      return this.width > 0.0F
         && this.height > 0.0F
         && this.thickness > 0.0F
         && this.globalAlpha > 0.0F
         && (this.color >>> 24 != 0 || this.baseAlpha > 0.0F || this.fresnelColor >>> 24 != 0);
   }

   private static float radiusValue(float[] radius, int index) {
      return radius != null && index >= 0 && index < radius.length ? radius[index] : 0.0F;
   }
}
