package daamky.ua.utils.render.ui.glass;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import daamky.ua.utils.render.ui.blur.BlurFramebuffer;

final class GlassRenderState implements SimpleGuiElementRenderState {
   private final BuiltGlass glass;
   private final Matrix3x2f pose;
   private final ScreenRect scissorArea;
   private final ScreenRect bounds;

   GlassRenderState(Matrix3x2f pose, BuiltGlass glass, ScreenRect scissorArea) {
      this.glass = glass;
      this.pose = pose;
      this.scissorArea = scissorArea;
      ScreenRect transformedBounds = new ScreenRect(Math.round(glass.x()), Math.round(glass.y()), Math.round(glass.width()), Math.round(glass.height()))
         .transformEachVertex(this.pose);
      this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
   }

   public void setupVertices(VertexConsumer vertices) {
      int batchIndex = GlassRenderer.getInstance().reserve(this.glass);
      if (batchIndex >= 0) {
         float x0 = this.glass.x();
         float y0 = this.glass.y();
         float x1 = this.glass.x() + this.glass.width();
         float y1 = this.glass.y() + this.glass.height();
         this.vertex(vertices, x0, y0, batchIndex);
         this.vertex(vertices, x0, y1, batchIndex);
         this.vertex(vertices, x1, y1, batchIndex);
         this.vertex(vertices, x1, y0, batchIndex);
      }
   }

   private void vertex(VertexConsumer consumer, float x, float y, int batchIndex) {
      consumer.vertex(this.pose, x, y).color(this.glass.color()).lineWidth((float)(batchIndex + 1));
   }

   public RenderPipeline pipeline() {
      return GlassRenderer.GLASS_PIPELINE;
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
