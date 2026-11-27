package daamky.ua.utils.render.ui.glass;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3x2f;
import daamky.ua.utils.render.ui.blur.BlurFramebuffer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;

final class GlassRenderState implements GuiElementRenderState {
   private final BuiltGlass glass;
   private final Matrix3x2f pose;
   private final ScreenRectangle scissorArea;
   private final ScreenRectangle bounds;

   GlassRenderState(Matrix3x2f pose, BuiltGlass glass, ScreenRectangle scissorArea) {
      this.glass = glass;
      this.pose = pose;
      this.scissorArea = scissorArea;
      ScreenRectangle transformedBounds = new ScreenRectangle(Math.round(glass.x()), Math.round(glass.y()), Math.round(glass.width()), Math.round(glass.height()))
         .transformMaxBounds(this.pose);
      this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
   }

   public void buildVertices(VertexConsumer vertices) {
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
      consumer.addVertexWith2DPose(this.pose, x, y).setColor(this.glass.color()).setLineWidth((float)(batchIndex + 1));
   }

   public RenderPipeline pipeline() {
      return GlassRenderer.GLASS_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return BlurFramebuffer.getInstance().textureSetup();
   }

   public ScreenRectangle scissorArea() {
      return this.scissorArea;
   }

   public ScreenRectangle bounds() {
      return this.bounds;
   }
}
