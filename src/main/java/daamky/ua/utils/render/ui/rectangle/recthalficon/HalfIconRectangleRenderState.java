package daamky.ua.utils.render.ui.rectangle.recthalficon;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class HalfIconRectangleRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f pose;
   private final TextureSetup textureSetup;
   private final List<HalfIconRectangleRenderState.PatternBatch> batches = new ArrayList<>(8);
   private float minX = Float.MAX_VALUE;
   private float minY = Float.MAX_VALUE;
   private float maxX = -Float.MAX_VALUE;
   private float maxY = -Float.MAX_VALUE;

   HalfIconRectangleRenderState(Matrix3x2f pose, TextureSetup textureSetup) {
      this.pose = pose;
      this.textureSetup = textureSetup;
   }

   void add(BuiltHalfIconRectangle rectangle, List<HalfIconRectangleRenderState.IconQuad> quads) {
      if (!quads.isEmpty()) {
         this.batches.add(new HalfIconRectangleRenderState.PatternBatch(rectangle, quads));
         this.minX = Math.min(this.minX, rectangle.x());
         this.minY = Math.min(this.minY, rectangle.y());
         this.maxX = Math.max(this.maxX, rectangle.x() + rectangle.width());
         this.maxY = Math.max(this.maxY, rectangle.y() + rectangle.height());
      }
   }

   boolean empty() {
      return this.batches.isEmpty();
   }

   public void setupVertices(VertexConsumer vertices) {
      for (HalfIconRectangleRenderState.PatternBatch batch : this.batches) {
         int batchIndex = HalfIconRectangleRenderer.getInstance().reserve(batch.rectangle());
         if (batchIndex >= 0) {
            for (HalfIconRectangleRenderState.IconQuad quad : batch.quads()) {
               this.vertex(vertices, quad.x0(), quad.y0(), quad.u0(), quad.v0(), quad.color(), batchIndex);
               this.vertex(vertices, quad.x1(), quad.y1(), quad.u1(), quad.v1(), quad.color(), batchIndex);
               this.vertex(vertices, quad.x2(), quad.y2(), quad.u2(), quad.v2(), quad.color(), batchIndex);
               this.vertex(vertices, quad.x3(), quad.y3(), quad.u3(), quad.v3(), quad.color(), batchIndex);
            }
         }
      }
   }

   private void vertex(VertexConsumer consumer, float x, float y, float u, float v, int color, int batchIndex) {
      consumer.vertex(this.pose, x, y).texture(u, v).color(color).lineWidth((float)(batchIndex + 1));
   }

   public RenderPipeline pipeline() {
      return HalfIconRectangleRenderer.HALF_ICON_RECTANGLE_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return this.textureSetup;
   }

   public ScreenRect scissorArea() {
      return null;
   }

   public ScreenRect bounds() {
      return this.batches.isEmpty()
         ? new ScreenRect(0, 0, 1, 1)
         : new ScreenRect(
               (int)Math.floor((double)this.minX),
               (int)Math.floor((double)this.minY),
               Math.max(1, (int)Math.ceil((double)(this.maxX - this.minX))),
               Math.max(1, (int)Math.ceil((double)(this.maxY - this.minY)))
            )
            .transformEachVertex(this.pose);
   }

   static record IconQuad(
      float x0,
      float y0,
      float u0,
      float v0,
      float x1,
      float y1,
      float u1,
      float v1,
      float x2,
      float y2,
      float u2,
      float v2,
      float x3,
      float y3,
      float u3,
      float v3,
      int color
   ) {
   }

   private static record PatternBatch(BuiltHalfIconRectangle rectangle, List<HalfIconRectangleRenderState.IconQuad> quads) {
   }
}
