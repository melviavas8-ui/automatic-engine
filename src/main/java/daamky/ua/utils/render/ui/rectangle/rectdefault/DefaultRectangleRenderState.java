package daamky.ua.utils.render.ui.rectangle.rectdefault;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class DefaultRectangleRenderState implements SimpleGuiElementRenderState {
   private final BuiltRectangle rectangle;
   private final Matrix3x2f pose;
   private final ScreenRect scissorArea;
   private final ScreenRect bounds;

   DefaultRectangleRenderState(Matrix3x2f pose, BuiltRectangle rectangle, ScreenRect scissorArea) {
      this.rectangle = rectangle;
      this.pose = pose;
      this.scissorArea = scissorArea;
      ScreenRect transformedBounds = new ScreenRect(
            Math.round(rectangle.x()), Math.round(rectangle.y()), Math.round(rectangle.width()), Math.round(rectangle.height())
         )
         .transformEachVertex(this.pose);
      this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
   }

   public void setupVertices(VertexConsumer vertices) {
      if (DefaultRectangleRenderer.getInstance().reserve(this.rectangle)) {
         float x0 = this.rectangle.x();
         float y0 = this.rectangle.y();
         float x1 = this.rectangle.x() + this.rectangle.width();
         float y1 = this.rectangle.y() + this.rectangle.height();
         this.vertex(vertices, x0, y0);
         this.vertex(vertices, x0, y1);
         this.vertex(vertices, x1, y1);
         this.vertex(vertices, x1, y0);
      }
   }

   private void vertex(VertexConsumer consumer, float x, float y) {
      consumer.vertex(this.pose, x, y);
   }

   public RenderPipeline pipeline() {
      return DefaultRectangleRenderer.RECTANGLE_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   public ScreenRect scissorArea() {
      return this.scissorArea;
   }

   public ScreenRect bounds() {
      return this.bounds;
   }
}
