package daamky.ua.utils.render.ui.arc;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class ArcOutlineRenderState implements SimpleGuiElementRenderState {
   private final BuiltArcOutline outline;
   private final Matrix3x2f pose;
   private final ScreenRect scissorArea;
   private final ScreenRect bounds;

   ArcOutlineRenderState(Matrix3x2f pose, BuiltArcOutline outline, ScreenRect scissorArea) {
      this.outline = outline;
      this.pose = pose;
      this.scissorArea = scissorArea;
      ScreenRect transformedBounds = new ScreenRect(Math.round(outline.x()), Math.round(outline.y()), Math.round(outline.size()), Math.round(outline.size()))
         .transformEachVertex(this.pose);
      this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
   }

   public void setupVertices(VertexConsumer vertices) {
      int batchIndex = ArcOutlineRenderer.getInstance().reserve(this.outline);
      if (batchIndex >= 0) {
         float x0 = this.outline.x();
         float y0 = this.outline.y();
         float x1 = this.outline.x() + this.outline.size();
         float y1 = this.outline.y() + this.outline.size();
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
      return ArcOutlineRenderer.ARC_OUTLINE_PIPELINE;
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
