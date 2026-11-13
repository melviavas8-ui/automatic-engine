package daamky.ua.utils.render.ui.font;

import daamky.ua.utils.render.ui.Render2DCoordinateSpace;

final class FontQuality {
   static final int ATLAS_SIZE = 2048;
   static final int ATLAS_GAP = 2;
   static final int MAX_LAYOUT_CACHE = 2048;

   private FontQuality() {
   }

   static int oversampleFor(float size) {
      if (size <= 10.0F) {
         return 6;
      } else if (size <= 14.0F) {
         return 5;
      } else if (size <= 28.0F) {
         return 4;
      } else {
         return size <= 64.0F ? 3 : 2;
      }
   }

   static int glyphPadding(int oversample) {
      return Math.max(4, oversample + 2);
   }

   static float snapOrigin(float value) {
      float scale = Render2DCoordinateSpace.designGuiScale();
      return (float)Math.round(value * scale) / scale;
   }

   static float coverageWeight(float size) {
      if (size <= 8.0F) {
         return 0.32F;
      } else if (size <= 10.0F) {
         return 0.22F;
      } else if (size <= 12.0F) {
         return 0.14F;
      } else {
         return size <= 14.0F ? 0.08F : 0.0F;
      }
   }
}
