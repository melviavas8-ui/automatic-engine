package daamky.ua.utils.render.ui.rectangle.rectdefault;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;

final class DefaultRectangleRenderState implements GuiElementRenderState {
   private final BuiltRectangle rectangle;
   private final Matrix3x2f pose;
   private final ScreenRectangle scissorArea;
   private final ScreenRectangle bounds;

   DefaultRectangleRenderState(Matrix3x2f pose, BuiltRectangle rectangle, ScreenRectangle scissorArea) {
      this.rectangle = rectangle;
      this.pose = pose;
      this.scissorArea = scissorArea;
      ScreenRectangle transformedBounds = new ScreenRectangle(
            Math.round(rectangle.x()), Math.round(rectangle.y()), Math.round(rectangle.width()), Math.round(rectangle.height())
         )
         .transformMaxBounds(this.pose);
      this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
   }

   public void buildVertices(VertexConsumer vertices) {
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
      consumer.addVertexWith2DPose(this.pose, x, y);
   }

   public RenderPipeline pipeline() {
      return DefaultRectangleRenderer.RECTANGLE_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }

   public ScreenRectangle scissorArea() {
      return this.scissorArea;
   }

   public ScreenRectangle bounds() {
      return this.bounds;
   }
}
