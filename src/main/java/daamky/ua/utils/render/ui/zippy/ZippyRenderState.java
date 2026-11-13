package daamky.ua.utils.render.ui.zippy;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class ZippyRenderState implements SimpleGuiElementRenderState {
   private final BuiltZippy zippy;
   private final Matrix3x2f pose;
   private final ScreenRect bounds;

   ZippyRenderState(Matrix3x2f pose, BuiltZippy zippy) {
      this.zippy = zippy;
      this.pose = pose;
      this.bounds = new ScreenRect(Math.round(zippy.x()), Math.round(zippy.y()), Math.round(zippy.width()), Math.round(zippy.height()))
         .transformEachVertex(this.pose);
   }

   public void setupVertices(VertexConsumer vertices) {
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
      consumer.vertex(this.pose, x, y).color(coordX, coordY, 255, 255).lineWidth((float)(batchIndex + 1));
   }

   public RenderPipeline pipeline() {
      return ZippyRenderer.ZIPPY_PIPELINE;
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
