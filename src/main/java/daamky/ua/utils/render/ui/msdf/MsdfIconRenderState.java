package daamky.ua.utils.render.ui.msdf;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class MsdfIconRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f pose;
   private final TextureSetup textureSetup;
   private final ScreenRect scissorArea;
   private final float distanceRange;
   private float[] positions = new float[128];
   private float[] uvs = new float[64];
   private int[] colors = new int[64];
   private int quadCount;
   private float minX = Float.MAX_VALUE;
   private float minY = Float.MAX_VALUE;
   private float maxX = -Float.MAX_VALUE;
   private float maxY = -Float.MAX_VALUE;

   MsdfIconRenderState(Matrix3x2f pose, TextureSetup textureSetup, ScreenRect scissorArea, float distanceRange) {
      this.pose = pose;
      this.textureSetup = textureSetup;
      this.scissorArea = scissorArea;
      this.distanceRange = distanceRange;
   }

   void add(float x, float y, float width, float height, MsdfGlyph glyph, int color) {
      this.add(x, y, width, height, glyph, color, color, color, color, 0.0F, x, y, false, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   void add(
      float x,
      float y,
      float width,
      float height,
      MsdfGlyph glyph,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY,
      boolean fadeLeft,
      boolean fadeRight,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      float fadeLeftStrength,
      float fadeRightStrength
   ) {
      this.ensureCapacity(this.quadCount + 1);
      int positionIndex = this.quadCount * 8;
      int uvIndex = this.quadCount * 4;
      int colorIndex = this.quadCount * 4;
      float x1 = x + width;
      float y1 = y + height;
      float tlX = x;
      float tlY = y;
      float blX = x;
      float blY = y1;
      float brX = x1;
      float brY = y1;
      float trX = x1;
      float trY = y;
      if (Math.abs(rotationDegrees) > 0.001F) {
         float radians = (float)Math.toRadians((double)rotationDegrees);
         float sin = (float)Math.sin((double)radians);
         float cos = (float)Math.cos((double)radians);
         tlX = this.rotateX(x, y, rotationOriginX, rotationOriginY, sin, cos);
         tlY = this.rotateY(x, y, rotationOriginX, rotationOriginY, sin, cos);
         blX = this.rotateX(x, y1, rotationOriginX, rotationOriginY, sin, cos);
         blY = this.rotateY(x, y1, rotationOriginX, rotationOriginY, sin, cos);
         brX = this.rotateX(x1, y1, rotationOriginX, rotationOriginY, sin, cos);
         brY = this.rotateY(x1, y1, rotationOriginX, rotationOriginY, sin, cos);
         trX = this.rotateX(x1, y, rotationOriginX, rotationOriginY, sin, cos);
         trY = this.rotateY(x1, y, rotationOriginX, rotationOriginY, sin, cos);
      }

      boolean faded = (fadeLeft || fadeRight) && fadeWidth > 0.0F && fadeRightX > fadeLeftX && (fadeLeftStrength > 0.001F || fadeRightStrength > 0.001F);
      this.positions[positionIndex] = tlX;
      this.positions[positionIndex + 1] = tlY;
      this.positions[positionIndex + 2] = blX;
      this.positions[positionIndex + 3] = blY;
      this.positions[positionIndex + 4] = brX;
      this.positions[positionIndex + 5] = brY;
      this.positions[positionIndex + 6] = trX;
      this.positions[positionIndex + 7] = trY;
      this.uvs[uvIndex] = glyph.u0();
      this.uvs[uvIndex + 1] = glyph.v0();
      this.uvs[uvIndex + 2] = glyph.u1();
      this.uvs[uvIndex + 3] = glyph.v1();
      this.colors[colorIndex] = faded
         ? this.fadeColor(colorTopLeft, tlX, fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength)
         : colorTopLeft;
      this.colors[colorIndex + 1] = faded
         ? this.fadeColor(colorBottomLeft, blX, fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength)
         : colorBottomLeft;
      this.colors[colorIndex + 2] = faded
         ? this.fadeColor(colorBottomRight, brX, fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength)
         : colorBottomRight;
      this.colors[colorIndex + 3] = faded
         ? this.fadeColor(colorTopRight, trX, fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength)
         : colorTopRight;
      this.quadCount++;
      this.include(tlX, tlY);
      this.include(blX, blY);
      this.include(brX, brY);
      this.include(trX, trY);
   }

   public void setupVertices(VertexConsumer vertices) {
      for (int i = 0; i < this.quadCount; i++) {
         int positionIndex = i * 8;
         int uvIndex = i * 4;
         int colorIndex = i * 4;
         float u0 = this.uvs[uvIndex];
         float v0 = this.uvs[uvIndex + 1];
         float u1 = this.uvs[uvIndex + 2];
         float v1 = this.uvs[uvIndex + 3];
         this.vertex(vertices, this.positions[positionIndex], this.positions[positionIndex + 1], u0, v0, this.colors[colorIndex]);
         this.vertex(vertices, this.positions[positionIndex + 2], this.positions[positionIndex + 3], u0, v1, this.colors[colorIndex + 1]);
         this.vertex(vertices, this.positions[positionIndex + 4], this.positions[positionIndex + 5], u1, v1, this.colors[colorIndex + 2]);
         this.vertex(vertices, this.positions[positionIndex + 6], this.positions[positionIndex + 7], u1, v0, this.colors[colorIndex + 3]);
      }
   }

   private void ensureCapacity(int requestedQuads) {
      int currentQuads = this.colors.length / 4;
      if (requestedQuads > currentQuads) {
         int newQuads = currentQuads;

         while (newQuads < requestedQuads) {
            newQuads *= 2;
         }

         float[] newPositions = new float[newQuads * 8];
         float[] newUvs = new float[newQuads * 4];
         int[] newColors = new int[newQuads * 4];
         System.arraycopy(this.positions, 0, newPositions, 0, this.quadCount * 8);
         System.arraycopy(this.uvs, 0, newUvs, 0, this.quadCount * 4);
         System.arraycopy(this.colors, 0, newColors, 0, this.quadCount * 4);
         this.positions = newPositions;
         this.uvs = newUvs;
         this.colors = newColors;
      }
   }

   private float rotateX(float x, float y, float originX, float originY, float sin, float cos) {
      float translatedX = x - originX;
      float translatedY = y - originY;
      return originX + translatedX * cos - translatedY * sin;
   }

   private float rotateY(float x, float y, float originX, float originY, float sin, float cos) {
      float translatedX = x - originX;
      float translatedY = y - originY;
      return originY + translatedX * sin + translatedY * cos;
   }

   private void include(float x, float y) {
      this.minX = Math.min(this.minX, x);
      this.minY = Math.min(this.minY, y);
      this.maxX = Math.max(this.maxX, x);
      this.maxY = Math.max(this.maxY, y);
   }

   private int fadeColor(
      int color,
      float x,
      boolean fadeLeft,
      boolean fadeRight,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      float fadeLeftStrength,
      float fadeRightStrength
   ) {
      float alpha = 1.0F;
      if (fadeLeft) {
         float edgeAlpha = this.smoothstep(this.clamp((x - fadeLeftX) / fadeWidth, 0.0F, 1.0F));
         alpha = Math.min(alpha, 1.0F - this.clamp(fadeLeftStrength, 0.0F, 1.0F) * (1.0F - edgeAlpha));
      }

      if (fadeRight) {
         float edgeAlpha = this.smoothstep(this.clamp((fadeRightX - x) / fadeWidth, 0.0F, 1.0F));
         alpha = Math.min(alpha, 1.0F - this.clamp(fadeRightStrength, 0.0F, 1.0F) * (1.0F - edgeAlpha));
      }

      int originalAlpha = color >>> 24 & 0xFF;
      return Math.round((float)originalAlpha * alpha) << 24 | color & 16777215;
   }

   private float smoothstep(float value) {
      return value * value * (3.0F - 2.0F * value);
   }

   private float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private void vertex(VertexConsumer consumer, float x, float y, float u, float v, int color) {
      consumer.vertex(this.pose, x, y).texture(u, v).color(color).lineWidth(this.distanceRange);
   }

   public RenderPipeline pipeline() {
      return MsdfIconRenderer.MSDF_ICON_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return this.textureSetup;
   }

   public ScreenRect scissorArea() {
      return this.scissorArea;
   }

   public ScreenRect bounds() {
      if (this.quadCount == 0) {
         return new ScreenRect(0, 0, 1, 1);
      } else {
         int x = (int)Math.floor((double)this.minX);
         int y = (int)Math.floor((double)this.minY);
         int width = Math.max(1, (int)Math.ceil((double)(this.maxX - this.minX)));
         int height = Math.max(1, (int)Math.ceil((double)(this.maxY - this.minY)));
         ScreenRect transformedBounds = new ScreenRect(x, y, width, height).transformEachVertex(this.pose);
         return this.scissorArea == null ? transformedBounds : this.scissorArea.intersection(transformedBounds);
      }
   }
}
