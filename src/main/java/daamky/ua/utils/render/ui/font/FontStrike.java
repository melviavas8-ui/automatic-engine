package daamky.ua.utils.render.ui.font;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.text.Bidi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class FontStrike implements AutoCloseable {
   private final FontFamily family;
   private final float size;
   private final int oversample;
   private final int glyphPadding;
   private final FontRenderContext renderContext;
   private final Map<FontStrike.GlyphKey, GlyphInfo> glyphs = new HashMap<>();
   private final Map<String, Font> physicalFonts = new HashMap<>();
   private final Map<String, TextLayout> layouts = new LinkedHashMap<String, TextLayout>(256, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, TextLayout> eldest) {
         return this.size() > 2048;
      }
   };
   private final Map<String, Float> widths = new LinkedHashMap<String, Float>(256, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, Float> eldest) {
         return this.size() > 2048;
      }
   };
   private final List<GlyphAtlasPage> pages = new ArrayList<>();
   private final float ascent;
   private final float lineHeight;
   private final float spaceAdvance;

   FontStrike(FontFamily family, float size) {
      this.family = family;
      this.size = size;
      this.oversample = FontQuality.oversampleFor(size);
      this.glyphPadding = FontQuality.glyphPadding(this.oversample);
      this.renderContext = new FontRenderContext(new AffineTransform(), RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      Font metricsFont = family.primary().deriveFont(0, size * (float)this.oversample);
      LineMetrics metrics = metricsFont.getLineMetrics("Mg", this.renderContext);
      this.ascent = metrics.getAscent() / (float)this.oversample;
      this.lineHeight = Math.max(size, metrics.getHeight() / (float)this.oversample);
      this.spaceAdvance = Math.max(
         metricsFont.createGlyphVector(this.renderContext, " ").getGlyphMetrics(0).getAdvance() / (float)this.oversample, size * 0.25F
      );
   }

   float ascent() {
      return this.ascent;
   }

   float lineHeight() {
      return this.lineHeight;
   }

   GlyphInfo glyph(int codePoint) {
      if (codePoint == 9) {
         GlyphInfo space = this.glyph(32);
         return space.withAdvance(space.advance() * 4.0F);
      } else {
         Font sourceFont = this.family.resolve(codePoint);
         Font font = this.physicalFont(sourceFont);
         GlyphVector vector = font.createGlyphVector(this.renderContext, new String(Character.toChars(codePoint)));
         int glyphCode = vector.getGlyphCode(0);
         float advance = vector.getGlyphMetrics(0).getAdvance() / (float)this.oversample;
         return this.glyph(sourceFont, glyphCode, advance);
      }
   }

   Font fontFor(int codePoint) {
      return this.family.resolve(codePoint);
   }

   TextLayout layout(String text) {
      return text != null && !text.isEmpty() ? this.layouts.computeIfAbsent(text, this::buildLayout) : TextLayout.EMPTY;
   }

   float width(String text) {
      if (text != null && !text.isEmpty()) {
         TextLayout layout = this.layouts.get(text);
         return layout != null ? layout.width() : this.widths.computeIfAbsent(text, this::measureWidth);
      } else {
         return 0.0F;
      }
   }

   FontStrike.RunLayout layoutRun(Font sourceFont, String text) {
      if (text != null && !text.isEmpty()) {
         Font font = this.physicalFont(sourceFont);
         char[] chars = text.toCharArray();
         int flags = new Bidi(text, -2).isRightToLeft() ? 1 : 0;
         GlyphVector vector = font.layoutGlyphVector(this.renderContext, chars, 0, chars.length, flags);
         int glyphCount = vector.getNumGlyphs();
         if (glyphCount == 0) {
            return FontStrike.RunLayout.EMPTY;
         } else {
            List<FontStrike.ShapedGlyph> shapedGlyphs = new ArrayList<>(glyphCount);
            float minX = 0.0F;
            float maxX = 0.0F;

            for (int glyphIndex = 0; glyphIndex < glyphCount; glyphIndex++) {
               Point2D position = vector.getGlyphPosition(glyphIndex);
               Point2D next = vector.getGlyphPosition(glyphIndex + 1);
               float glyphX = (float)position.getX() / (float)this.oversample;
               float glyphY = (float)position.getY() / (float)this.oversample;
               float advance = Math.abs((float)(next.getX() - position.getX()) / (float)this.oversample);
               GlyphInfo glyph = this.glyph(sourceFont, vector.getGlyphCode(glyphIndex), advance);
               if (glyph.drawable()) {
                  minX = Math.min(minX, glyphX + glyph.xOffset());
                  maxX = Math.max(maxX, glyphX + glyph.xOffset() + glyph.width());
               } else {
                  maxX = Math.max(maxX, glyphX + glyph.advance());
               }

               shapedGlyphs.add(new FontStrike.ShapedGlyph(glyph, glyphX, glyphY));
            }

            float runAdvance = Math.max(Math.abs((float)vector.getGlyphPosition(glyphCount).getX() / (float)this.oversample), maxX - minX);
            float shiftX = minX < 0.0F ? -minX : 0.0F;
            if (shiftX > 0.0F) {
               List<FontStrike.ShapedGlyph> shifted = new ArrayList<>(shapedGlyphs.size());

               for (FontStrike.ShapedGlyph shapedGlyph : shapedGlyphs) {
                  shifted.add(new FontStrike.ShapedGlyph(shapedGlyph.glyph(), shapedGlyph.x() + shiftX, shapedGlyph.y()));
               }

               shapedGlyphs = shifted;
               runAdvance += shiftX;
            }

            return new FontStrike.RunLayout(shapedGlyphs, runAdvance);
         }
      } else {
         return FontStrike.RunLayout.EMPTY;
      }
   }

   private float measureWidth(String text) {
      float cursorX = 0.0F;
      float maxWidth = 0.0F;
      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         if (codePoint == 10) {
            i += Character.charCount(codePoint);
            maxWidth = Math.max(maxWidth, cursorX);
            cursorX = 0.0F;
         } else if (codePoint == 9) {
            i += Character.charCount(codePoint);
            cursorX += this.spaceAdvance * 4.0F;
         } else if (Character.isISOControl(codePoint)) {
            i += Character.charCount(codePoint);
         } else {
            Font font = this.fontFor(codePoint);
            int runStart = i;
            i += Character.charCount(codePoint);

            while (true) {
               if (i < text.length()) {
                  int nextCodePoint = text.codePointAt(i);
                  if (nextCodePoint != 10 && nextCodePoint != 9 && !Character.isISOControl(nextCodePoint)) {
                     Font nextFont = this.fontFor(nextCodePoint);
                     if (sameFont(font, nextFont)) {
                        i += Character.charCount(nextCodePoint);
                        continue;
                     }
                  }
               }

               cursorX += this.measureRun(font, text.substring(runStart, i));
               break;
            }
         }
      }

      return Math.max(maxWidth, cursorX);
   }

   private float measureRun(Font sourceFont, String text) {
      if (text != null && !text.isEmpty()) {
         Font font = this.physicalFont(sourceFont);
         char[] chars = text.toCharArray();
         int flags = new Bidi(text, -2).isRightToLeft() ? 1 : 0;
         GlyphVector vector = font.layoutGlyphVector(this.renderContext, chars, 0, chars.length, flags);
         int glyphCount = vector.getNumGlyphs();
         if (glyphCount == 0) {
            return 0.0F;
         } else {
            float advance = Math.abs((float)vector.getGlyphPosition(glyphCount).getX() / (float)this.oversample);
            return advance > 0.0F ? advance : (float)font.getStringBounds(text, this.renderContext).getWidth() / (float)this.oversample;
         }
      } else {
         return 0.0F;
      }
   }

   void uploadDirtyPages() {
      for (GlyphAtlasPage page : this.pages) {
         page.uploadIfDirty();
      }
   }

   private GlyphInfo glyph(Font sourceFont, int glyphCode, float fallbackAdvance) {
      FontStrike.GlyphKey key = new FontStrike.GlyphKey(FontFamily.fontKey(sourceFont), glyphCode);
      return this.glyphs.computeIfAbsent(key, ignored -> this.bakeGlyph(sourceFont, glyphCode, fallbackAdvance));
   }

   private static boolean sameFont(Font left, Font right) {
      return left == right || left != null && left.equals(right);
   }

   private TextLayout buildLayout(String text) {
      Map<GlyphAtlasPage, List<LayoutGlyph>> batches = new LinkedHashMap<>();
      float cursorX = 0.0F;
      float cursorY = 0.0F;
      float maxWidth = 0.0F;
      float baselineY = this.ascent;
      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         if (codePoint == 10) {
            i += Character.charCount(codePoint);
            maxWidth = Math.max(maxWidth, cursorX);
            cursorX = 0.0F;
            cursorY += this.lineHeight;
         } else if (codePoint == 9) {
            i += Character.charCount(codePoint);
            cursorX += this.glyph(9).advance();
         } else if (Character.isISOControl(codePoint)) {
            i += Character.charCount(codePoint);
         } else {
            Font font = this.fontFor(codePoint);
            int runStart = i;
            i += Character.charCount(codePoint);

            while (i < text.length()) {
               int nextCodePoint = text.codePointAt(i);
               if (nextCodePoint == 10 || nextCodePoint == 9 || Character.isISOControl(nextCodePoint)) {
                  break;
               }

               Font nextFont = this.fontFor(nextCodePoint);
               if (!sameFont(font, nextFont)) {
                  break;
               }

               i += Character.charCount(nextCodePoint);
            }

            FontStrike.RunLayout run = this.layoutRun(font, text.substring(runStart, i));

            for (FontStrike.ShapedGlyph shapedGlyph : run.glyphs()) {
               GlyphInfo glyph = shapedGlyph.glyph();
               if (glyph.drawable()) {
                  float x0 = cursorX + shapedGlyph.x() + glyph.xOffset();
                  float y0 = baselineY + cursorY + shapedGlyph.y() + glyph.yOffset();
                  float x1 = x0 + glyph.width();
                  float y1 = y0 + glyph.height();
                  batches.computeIfAbsent(glyph.page(), ignored -> new ArrayList<>())
                     .add(new LayoutGlyph(glyph.page(), x0, y0, x1, y1, glyph.u0(), glyph.v0(), glyph.u1(), glyph.v1()));
               }
            }

            cursorX += run.advance();
         }
      }

      maxWidth = Math.max(maxWidth, cursorX);
      List<TextLayout.Page> pages = new ArrayList<>(batches.size());

      for (Entry<GlyphAtlasPage, List<LayoutGlyph>> entry : batches.entrySet()) {
         pages.add(new TextLayout.Page(entry.getKey(), entry.getValue()));
      }

      return new TextLayout(pages, maxWidth, cursorY + this.lineHeight);
   }

   private GlyphInfo bakeGlyph(Font sourceFont, int glyphCode, float fallbackAdvance) {
      Font font = this.physicalFont(sourceFont);
      GlyphVector vector = font.createGlyphVector(this.renderContext, new int[]{glyphCode});
      GlyphMetrics metrics = vector.getGlyphMetrics(0);
      float advance = Math.max(fallbackAdvance > 0.0F ? fallbackAdvance : metrics.getAdvance() / (float)this.oversample, 0.0F);
      Rectangle bounds = vector.getPixelBounds(this.renderContext, 0.0F, 0.0F);
      if (bounds.width > 0 && bounds.height > 0) {
         int imageWidth = bounds.width + this.glyphPadding * 2;
         int imageHeight = bounds.height + this.glyphPadding * 2;
         if (imageWidth > 0 && imageHeight > 0 && imageWidth <= 2048 && imageHeight <= 2048) {
            BufferedImage image = new BufferedImage(imageWidth, imageHeight, 2);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.setColor(Color.WHITE);
            graphics.drawGlyphVector(vector, (float)(this.glyphPadding - bounds.x), (float)(this.glyphPadding - bounds.y));
            graphics.dispose();
            this.strengthenSmallGlyph(image);
            GlyphAtlasPage.Allocation allocation = this.allocate(imageWidth, imageHeight);
            if (allocation == null) {
               return GlyphInfo.empty(advance);
            } else {
               allocation.page().copy(image, allocation.x(), allocation.y());
               float xOffset = (float)(bounds.x - this.glyphPadding) / (float)this.oversample;
               float yOffset = (float)(bounds.y - this.glyphPadding) / (float)this.oversample;
               float width = (float)imageWidth / (float)this.oversample;
               float height = (float)imageHeight / (float)this.oversample;
               return new GlyphInfo(
                  allocation.page(), allocation.u0(), allocation.v0(), allocation.u1(), allocation.v1(), xOffset, yOffset, width, height, advance
               );
            }
         } else {
            return GlyphInfo.empty(advance);
         }
      } else {
         return GlyphInfo.empty(advance);
      }
   }

   private GlyphAtlasPage.Allocation allocate(int width, int height) {
      for (GlyphAtlasPage page : this.pages) {
         GlyphAtlasPage.Allocation allocation = page.allocate(width, height);
         if (allocation != null) {
            return allocation;
         }
      }

      GlyphAtlasPage pagex = new GlyphAtlasPage(2048);
      this.pages.add(pagex);
      return pagex.allocate(width, height);
   }

   private Font physicalFont(Font sourceFont) {
      String key = FontFamily.fontKey(sourceFont);
      return this.physicalFonts.computeIfAbsent(key, ignored -> sourceFont.deriveFont(0, this.size * (float)this.oversample));
   }

   private void strengthenSmallGlyph(BufferedImage image) {
      float weight = FontQuality.coverageWeight(this.size);
      if (!(weight <= 0.0F)) {
         int width = image.getWidth();
         int height = image.getHeight();
         int[] source = image.getRGB(0, 0, width, height, null, 0, width);
         int[] target = (int[])source.clone();

         for (int y = 1; y < height - 1; y++) {
            int row = y * width;

            for (int x = 1; x < width - 1; x++) {
               int index = row + x;
               int alpha = source[index] >>> 24 & 0xFF;
               int neighborAlpha = Math.max(alpha, source[index - 1] >>> 24 & 0xFF);
               neighborAlpha = Math.max(neighborAlpha, source[index + 1] >>> 24 & 0xFF);
               neighborAlpha = Math.max(neighborAlpha, source[index - width] >>> 24 & 0xFF);
               neighborAlpha = Math.max(neighborAlpha, source[index + width] >>> 24 & 0xFF);
               neighborAlpha = Math.max(neighborAlpha, source[index - width - 1] >>> 24 & 0xFF);
               neighborAlpha = Math.max(neighborAlpha, source[index - width + 1] >>> 24 & 0xFF);
               neighborAlpha = Math.max(neighborAlpha, source[index + width - 1] >>> 24 & 0xFF);
               neighborAlpha = Math.max(neighborAlpha, source[index + width + 1] >>> 24 & 0xFF);
               int boostedAlpha = Math.min(255, Math.round((float)alpha + (float)(neighborAlpha - alpha) * weight));
               target[index] = boostedAlpha << 24 | 16777215;
            }
         }

         image.setRGB(0, 0, width, height, target, 0, width);
      }
   }

   @Override
   public void close() {
      this.glyphs.clear();
      this.physicalFonts.clear();
      this.layouts.clear();
      this.widths.clear();

      for (GlyphAtlasPage page : this.pages) {
         page.close();
      }

      this.pages.clear();
   }

   private static record GlyphKey(String fontKey, int glyphCode) {
   }

   static record RunLayout(List<FontStrike.ShapedGlyph> glyphs, float advance) {
      static final FontStrike.RunLayout EMPTY = new FontStrike.RunLayout(List.of(), 0.0F);
   }

   static record ShapedGlyph(GlyphInfo glyph, float x, float y) {
   }
}
