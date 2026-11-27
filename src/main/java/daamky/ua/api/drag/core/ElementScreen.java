package daamky.ua.api.drag.core;

import daamky.ua.utils.render.ui.Render2DCoordinateSpace;
import net.minecraft.client.Minecraft;

public record ElementScreen(float width, float height, float coordinateScale) {
   public static ElementScreen current() {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.getWindow() != null) {
         float scale = Render2DCoordinateSpace.guiIndependentScale();
         float safeScale = Math.max(1.0E-4F, scale);
         return new ElementScreen(
            Math.max(1.0F, (float)client.getWindow().getGuiScaledWidth() / safeScale),
            Math.max(1.0F, (float)client.getWindow().getGuiScaledHeight() / safeScale),
            scale
         );
      } else {
         return new ElementScreen(1.0F, 1.0F, 1.0F);
      }
   }

   public float clampX(float x, float width, float margin) {
      float max = Math.max(margin, this.width - width - margin);
      return clamp(x, margin, max);
   }

   public float clampY(float y, float height, float margin) {
      float max = Math.max(margin, this.height - height - margin);
      return clamp(y, margin, max);
   }

   public boolean valid() {
      return this.width > 0.0F && this.height > 0.0F && Float.isFinite(this.width) && Float.isFinite(this.height);
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
