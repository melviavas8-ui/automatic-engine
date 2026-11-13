package daamky.ua.screens.csgui;

import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class CsMenuAssets {
   public static final String CHECKMARK = "✓";
   public static final String TRASH = "×";

   private CsMenuAssets() {
   }

   public static void icon(String glyph, float x, float y, float size, int color) {
      if (glyph != null && !glyph.isEmpty() && !(size <= 0.0F)) {
         Render2D.text(FontType.SEMIBOLD, glyph, x, y, size, color);
      }
   }

   public static void iconCentered(String glyph, float cx, float cy, float size, int color) {
      float width = Render2D.textWidth(FontType.SEMIBOLD, glyph, size);
      Render2D.text(FontType.SEMIBOLD, glyph, cx - width * 0.5F, cy - size * 0.5F, size, color);
   }
}
