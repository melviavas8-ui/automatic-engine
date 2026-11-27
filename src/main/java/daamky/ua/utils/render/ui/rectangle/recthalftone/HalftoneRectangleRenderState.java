package daamky.ua.utils.render.ui.rectangle.recthalftone;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;

final class HalftoneRectangleRenderState implements GuiElementRenderState {
   private final BuiltHalftoneRectangle rectangle;
   private final Matrix3x2f pose;
   private final ScreenRectangle bounds;

   HalftoneRectangleRenderState(Matrix3x2f pose, BuiltHalftoneRectangle rectangle) {
      this.rectangle = rectangle;
      this.pose = pose;
      this.bounds = new ScreenRectangle(Math.round(rectangle.x()), Math.round(rectangle.y()), Math.round(rectangle.width()), Math.round(rectangle.height()))
         .transformMaxBounds(this.pose);
   }

   public void buildVertices(VertexConsumer vertices) {
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
      consumer.addVertexWith2DPose(this.pose, x, y).setColor(coordX, coordY, 255, 255).setLineWidth((float)(batchIndex + 1));
   }

   public RenderPipeline pipeline() {
      return HalftoneRectangleRenderer.HALFTONE_RECTANGLE_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }

   public ScreenRectangle scissorArea() {
      return null;
   }

   public ScreenRectangle bounds() {
      return this.bounds;
   }
}
