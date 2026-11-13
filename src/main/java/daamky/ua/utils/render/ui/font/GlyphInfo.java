package daamky.ua.utils.render.ui.font;

record GlyphInfo(GlyphAtlasPage page, float u0, float v0, float u1, float v1, float xOffset, float yOffset, float width, float height, float advance) {
   static GlyphInfo empty(float advance) {
      return new GlyphInfo(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, advance);
   }

   boolean drawable() {
      return this.page != null && this.width > 0.0F && this.height > 0.0F;
   }

   GlyphInfo withAdvance(float advance) {
      return new GlyphInfo(this.page, this.u0, this.v0, this.u1, this.v1, this.xOffset, this.yOffset, this.width, this.height, advance);
   }
}
