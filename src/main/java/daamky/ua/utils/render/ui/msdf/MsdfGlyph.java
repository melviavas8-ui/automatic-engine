package daamky.ua.utils.render.ui.msdf;

record MsdfGlyph(float xOffset, float yOffset, float width, float height, float advance, float u0, float v0, float u1, float v1) {
   boolean drawable() {
      return this.width > 0.0F && this.height > 0.0F && this.u1 > this.u0 && this.v1 > this.v0;
   }
}
