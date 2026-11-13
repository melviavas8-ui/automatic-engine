package daamky.ua.utils.render.ui.glass;

import net.minecraft.client.gui.DrawContext;

public record BuiltGlass(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
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
   public BuiltGlass(
      float x,
      float y,
      float width,
      float height,
      float[] radius,
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
         color,
         globalAlpha,
         fresnelPower,
         fresnelColor,
         baseAlpha,
         fresnelInvert,
         fresnelMix,
         distortStrength,
         squirt,
         z
      );
   }

   public void render(DrawContext graphics) {
      GlassRenderer.getInstance().draw(graphics, this);
   }

   public boolean visible() {
      return this.width > 0.0F && this.height > 0.0F && this.globalAlpha > 0.0F && (this.baseAlpha > 0.0F || this.fresnelColor >>> 24 != 0);
   }

   private static float radiusValue(float[] radius, int index) {
      return radius != null && index >= 0 && index < radius.length ? radius[index] : 0.0F;
   }
}
