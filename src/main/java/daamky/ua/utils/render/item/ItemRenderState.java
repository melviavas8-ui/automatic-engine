package daamky.ua.utils.render.item;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class ItemRenderState implements SimpleGuiElementRenderState {
   private static final int FLOATS_PER_QUAD = 16;
   private final Matrix3x2f pose;
   private final ItemTexture texture;
   private float[] quads = new float[256];
   private int[] colors = new int[16];
   private float[] glints = new float[16];
   private int quadCount;
   private float minX = Float.MAX_VALUE;
   private float minY = Float.MAX_VALUE;
   private float maxX = -Float.MAX_VALUE;
   private float maxY = -Float.MAX_VALUE;

   ItemRenderState(Matrix3x2f pose, ItemTexture texture) {
      this.pose = new Matrix3x2f(pose);
      this.texture = texture;
   }

   void add(CachedItemQuad quad, float x, float y, float size, int color, float glintStrength) {
      this.ensureCapacity(this.quadCount + 1);
      int offset = this.quadCount * 16;
      float x0 = x + quad.x0() * size;
      float y0 = y + quad.y0() * size;
      float x1 = x + quad.x1() * size;
      float y1 = y + quad.y1() * size;
      float x2 = x + quad.x2() * size;
      float y2 = y + quad.y2() * size;
      float x3 = x + quad.x3() * size;
      float y3 = y + quad.y3() * size;
      this.quads[offset] = x0;
      this.quads[offset + 1] = y0;
      this.quads[offset + 2] = quad.u0();
      this.quads[offset + 3] = quad.v0();
      this.quads[offset + 4] = x1;
      this.quads[offset + 5] = y1;
      this.quads[offset + 6] = quad.u1();
      this.quads[offset + 7] = quad.v1();
      this.quads[offset + 8] = x2;
      this.quads[offset + 9] = y2;
      this.quads[offset + 10] = quad.u2();
      this.quads[offset + 11] = quad.v2();
      this.quads[offset + 12] = x3;
      this.quads[offset + 13] = y3;
      this.quads[offset + 14] = quad.u3();
      this.quads[offset + 15] = quad.v3();
      this.colors[this.quadCount] = color;
      this.glints[this.quadCount] = glintStrength;
      this.quadCount++;
      this.include(x0, y0);
      this.include(x1, y1);
      this.include(x2, y2);
      this.include(x3, y3);
   }

   public void setupVertices(VertexConsumer vertices) {
      for (int i = 0; i < this.quadCount; i++) {
         int batchIndex = CustomItemRenderer.getInstance().reserve(this.glints[i]);
         if (batchIndex >= 0) {
            int offset = i * 16;
            int color = this.colors[i];
            this.vertex(vertices, this.quads[offset], this.quads[offset + 1], this.quads[offset + 2], this.quads[offset + 3], color, batchIndex);
            this.vertex(vertices, this.quads[offset + 4], this.quads[offset + 5], this.quads[offset + 6], this.quads[offset + 7], color, batchIndex);
            this.vertex(vertices, this.quads[offset + 8], this.quads[offset + 9], this.quads[offset + 10], this.quads[offset + 11], color, batchIndex);
            this.vertex(vertices, this.quads[offset + 12], this.quads[offset + 13], this.quads[offset + 14], this.quads[offset + 15], color, batchIndex);
         }
      }
   }

   private void vertex(VertexConsumer consumer, float x, float y, float u, float v, int color, int batchIndex) {
      consumer.vertex(this.pose, x, y).texture(u, v).color(color).lineWidth((float)(batchIndex + 1));
   }

   public RenderPipeline pipeline() {
      return CustomItemRenderer.ITEM_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return this.texture.setup();
   }

   public ScreenRect scissorArea() {
      return null;
   }

   public ScreenRect bounds() {
      if (this.quadCount == 0) {
         return new ScreenRect(0, 0, 1, 1);
      } else {
         int x = (int)Math.floor((double)this.minX);
         int y = (int)Math.floor((double)this.minY);
         int width = Math.max(1, (int)Math.ceil((double)(this.maxX - this.minX)));
         int height = Math.max(1, (int)Math.ceil((double)(this.maxY - this.minY)));
         return new ScreenRect(x, y, width, height).transformEachVertex(this.pose);
      }
   }

   private void include(float x, float y) {
      this.minX = Math.min(this.minX, x);
      this.minY = Math.min(this.minY, y);
      this.maxX = Math.max(this.maxX, x);
      this.maxY = Math.max(this.maxY, y);
   }

   private void ensureCapacity(int targetQuads) {
      if (targetQuads > this.colors.length) {
         int newCapacity = Math.max(targetQuads, this.colors.length * 2);
         float[] nextQuads = new float[newCapacity * 16];
         int[] nextColors = new int[newCapacity];
         float[] nextGlints = new float[newCapacity];
         System.arraycopy(this.quads, 0, nextQuads, 0, this.quadCount * 16);
         System.arraycopy(this.colors, 0, nextColors, 0, this.quadCount);
         System.arraycopy(this.glints, 0, nextGlints, 0, this.quadCount);
         this.quads = nextQuads;
         this.colors = nextColors;
         this.glints = nextGlints;
      }
   }
}
