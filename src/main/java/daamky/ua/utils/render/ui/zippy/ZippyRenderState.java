package daamky.ua.utils.render.ui.zippy;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;

final class ZippyRenderState implements GuiElementRenderState {
   private final BuiltZippy zippy;
   private final Matrix3x2f pose;
   private final ScreenRectangle bounds;

   ZippyRenderState(Matrix3x2f pose, BuiltZippy zippy) {
      this.zippy = zippy;
      this.pose = pose;
      this.bounds = new ScreenRectangle(Math.round(zippy.x()), Math.round(zippy.y()), Math.round(zippy.width()), Math.round(zippy.height()))
         .transformMaxBounds(this.pose);
   }

   public void buildVertices(VertexConsumer vertices) {
      int batchIndex = ZippyRenderer.getInstance().reserve(this.zippy);
      if (batchIndex >= 0) {
         float x0 = this.zippy.x();
         float y0 = this.zippy.y();
         float x1 = this.zippy.x() + this.zippy.width();
         float y1 = this.zippy.y() + this.zippy.height();
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
      return ZippyRenderer.ZIPPY_PIPELINE;
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
