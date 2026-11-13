package daamky.ua.utils.render.ui.rectangle.recthalftone;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class HalftoneRectangleRenderState implements SimpleGuiElementRenderState {
   private final BuiltHalftoneRectangle rectangle;
   private final Matrix3x2f pose;
   private final ScreenRect bounds;

   HalftoneRectangleRenderState(Matrix3x2f pose, BuiltHalftoneRectangle rectangle) {
      this.rectangle = rectangle;
      this.pose = pose;
      this.bounds = new ScreenRect(Math.round(rectangle.x()), Math.round(rectangle.y()), Math.round(rectangle.width()), Math.round(rectangle.height()))
         .transformEachVertex(this.pose);
   }

   public void setupVertices(VertexConsumer vertices) {
      int batchIndex = HalftoneRectangleRenderer.getInstance().reserve(this.rectangle);
      if (batchIndex >= 0) {
         float x0 = this.rectangle.x();
         float y0 = this.rectangle.y();
         float x1 = this.rectangle.x() + this.rectangle.width();
         float y1 = this.rectangle.y() + this.rectangle.height();
         this.vertex(vertices, x0, y0, 0, 0, batchIndex);
         this.vertex(vertices, x0, y1, 0, 255, batchIndex);
         this.vertex(vertices, x1, y1, 255, 255, batchIndex);
         this.vertex(vertices, x1, y0, 255, 0, batchIndex);
      }
   }

   private void vertex(VertexConsumer consumer, float x, float y, int coordX, int coordY, int batchIndex) {
      consumer.vertex(this.pose, x, y).color(coordX, coordY, 255, 255).lineWidth((float)(batchIndex + 1));
   }

   public RenderPipeline pipeline() {
      return HalftoneRectangleRenderer.HALFTONE_RECTANGLE_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   public ScreenRect scissorArea() {
      return null;
   }

   public ScreenRect bounds() {
      return this.bounds;
   }
}
