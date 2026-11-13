package daamky.ua.utils.render.ui.font;

import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

final class FontFamily {
   private static final FontRenderContext CHECK_CONTEXT = new FontRenderContext(
      new AffineTransform(), RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_FRACTIONALMETRICS_ON
   );
   private final String name;
   private final Font primary;
   private final List<Font> fonts;
   private final IntFunction<List<Font>> lazyFallbacks;
   private final Map<Integer, Font> resolvedFonts = new HashMap<>();

   FontFamily(String name, Font primary, List<Font> fallbacks, IntFunction<List<Font>> lazyFallbacks) {
      this.name = name;
      this.primary = primary;
      this.fonts = new ArrayList<>(1 + fallbacks.size());
      this.fonts.add(primary);
      this.fonts.addAll(fallbacks);
      this.lazyFallbacks = lazyFallbacks;
   }

   String name() {
      return this.name;
   }

   Font primary() {
      return this.primary;
   }

   Font resolve(int codePoint) {
      return this.resolvedFonts.computeIfAbsent(codePoint, this::findFont);
   }

   private Font findFont(int codePoint) {
      if (!Character.isValidCodePoint(codePoint)) {
         return this.primary;
      } else {
         Font preferred = this.findPreferredFont(codePoint);
         if (preferred != null) {
            return preferred;
         } else {
            for (Font font : this.fonts) {
               if (canDisplay(font, codePoint)) {
                  return font;
               }
            }

            if (this.lazyFallbacks != null) {
               for (Font fontx : this.lazyFallbacks.apply(codePoint)) {
                  if (canDisplay(fontx, codePoint)) {
                     return fontx;
                  }
               }
            }

            return this.primary;
         }
      }
   }

   private Font findPreferredFont(int codePoint) {
      if (isPrivateUse(codePoint)) {
         return this.findFontByDisplay(codePoint, "icon");
      } else if (isHangul(codePoint)) {
         return this.findFontByDisplay(codePoint, "cjkkr", "cjk kr", "korean", "malgun");
      } else if (isJapanese(codePoint)) {
         return this.findFontByDisplay(codePoint, "cjkjp", "cjk jp", "japanese", "meiryo");
      } else if (isCjk(codePoint)) {
         return this.findFontByDisplay(codePoint, "cjksc", "cjk sc", "chinese", "yahei", "simsun");
      } else if (isArabic(codePoint)) {
         return this.findFontByDisplay(codePoint, "naskh", "arabic");
      } else {
         return isSymbol(codePoint) ? this.findFontByDisplay(codePoint, "symbols2", "symbols 2", "symbol") : null;
      }
   }

   private Font findFontByDisplay(int codePoint, String... tokens) {
      for (String token : tokens) {
         for (Font font : this.fonts) {
            String key = fontKey(font).toLowerCase(Locale.ROOT);
            if (key.contains(token) && canDisplay(font, codePoint)) {
               return font;
            }
         }
      }

      return null;
   }

   private static boolean canDisplay(Font font, int codePoint) {
      try {
         if (font != null && font.canDisplay(codePoint)) {
            GlyphVector vector = font.createGlyphVector(CHECK_CONTEXT, new String(Character.toChars(codePoint)));
            return vector.getNumGlyphs() > 0 && vector.getGlyphCode(0) != font.getMissingGlyphCode();
         } else {
            return false;
         }
      } catch (IllegalArgumentException var3) {
         return false;
      }
   }

   static String fontKey(Font font) {
      return font == null ? "missing" : font.getPSName() + "/" + font.getFontName(Locale.ROOT);
   }

   private static boolean isPrivateUse(int codePoint) {
      return codePoint >= 57344 && codePoint <= 63743 || codePoint >= 983040 && codePoint <= 1048573 || codePoint >= 1048576 && codePoint <= 1114109;
   }

   private static boolean isHangul(int codePoint) {
      return codePoint >= 4352 && codePoint <= 4607
         || codePoint >= 12592 && codePoint <= 12687
         || codePoint >= 43360 && codePoint <= 43391
         || codePoint >= 44032 && codePoint <= 55215;
   }

   private static boolean isJapanese(int codePoint) {
      return codePoint >= 12352 && codePoint <= 12543 || codePoint >= 12784 && codePoint <= 12799;
   }

   private static boolean isCjk(int codePoint) {
      return codePoint >= 13312 && codePoint <= 19903
         || codePoint >= 19968 && codePoint <= 40959
         || codePoint >= 63744 && codePoint <= 64255
         || codePoint >= 131072 && codePoint <= 195103;
   }

   private static boolean isArabic(int codePoint) {
      return codePoint >= 1536 && codePoint <= 1791
         || codePoint >= 1872 && codePoint <= 1919
         || codePoint >= 2208 && codePoint <= 2303
         || codePoint >= 64336 && codePoint <= 65023
         || codePoint >= 65136 && codePoint <= 65279;
   }

   private static boolean isSymbol(int codePoint) {
      return codePoint >= 8192 && codePoint <= 11263 || codePoint >= 126976 && codePoint <= 129791;
   }
}
