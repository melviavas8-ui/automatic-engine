package daamky.ua.utils.render.ui.font;

import java.util.List;

final class TextLayout {
   static final TextLayout EMPTY = new TextLayout(List.of(), 0.0F, 0.0F);
   private final List<TextLayout.Page> pages;
   private final float width;
   private final float height;

   TextLayout(List<TextLayout.Page> pages, float width, float height) {
      this.pages = List.copyOf(pages);
      this.width = width;
      this.height = height;
   }

   List<TextLayout.Page> pages() {
      return this.pages;
   }

   float width() {
      return this.width;
   }

   float height() {
      return this.height;
   }

   boolean empty() {
      return this.pages.isEmpty();
   }

   static record Page(GlyphAtlasPage page, List<LayoutGlyph> glyphs) {
      Page(GlyphAtlasPage page, List<LayoutGlyph> glyphs) {
         glyphs = List.copyOf(glyphs);
         this.page = page;
         this.glyphs = glyphs;
      }
   }
}
