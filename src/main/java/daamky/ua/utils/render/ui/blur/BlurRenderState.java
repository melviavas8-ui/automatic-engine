package daamky.ua.utils.render.ui.blur;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class BlurRenderState implements SimpleGuiElementRenderState {
   private final BuiltBlur blur;
   private final Matrix3x2f pose;
   private final ScreenRect scissorArea;
   private final ScreenRect bounds;

   BlurRenderState(Matrix3x2f pose, BuiltBlur blur, ScreenRect scissorArea) {
      this.blur = blur;
      this.pose = pose;
      this.scissorArea = scissorArea;
      ScreenRect transformedBounds = new ScreenRect(Math.round(blur.x()), Math.round(blur.y()), Math.round(blur.width()), Math.round(blur.height()))
         .transformEachVertex(this.pose);
      this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
   }

   public void setupVertices(VertexConsumer vertices) {
      int batchIndex = BlurFramebuffer.getInstance().reserve(this.blur);
      if (batchIndex >= 0) {
         float x0 = this.blur.x();
         float y0 = this.blur.y();
         float x1 = this.blur.x() + this.blur.width();
         float y1 = this.blur.y() + this.blur.height();
         int color = this.blur.color();
         this.vertex(vertices, x0, y0, color, batchIndex);
         this.vertex(vertices, x0, y1, color, batchIndex);
         this.vertex(vertices, x1, y1, color, batchIndex);
         this.vertex(vertices, x1, y0, color, batchIndex);
      }
   }

   private void vertex(VertexConsumer consumer, float x, float y, int color, int batchIndex) {
      consumer.vertex(this.pose, x, y).color(color).lineWidth((float)(batchIndex + 1));
   }

   public RenderPipeline pipeline() {
      return BlurFramebuffer.BATCHED_BLUR_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return BlurFramebuffer.getInstance().textureSetup();
   }

   public ScreenRect scissorArea() {
      return this.scissorArea;
   }

   public ScreenRect bounds() {
      return this.bounds;
   }
}
