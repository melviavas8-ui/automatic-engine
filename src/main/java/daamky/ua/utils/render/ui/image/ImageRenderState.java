package daamky.ua.utils.render.ui.image;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;

final class ImageRenderState implements GuiElementRenderState {
   private final Matrix3x2f pose;
   private final ImageTexture texture;
   private final ScreenRectangle scissorArea;
   private final List<ImageQuad> images = new ArrayList<>(16);
   private float minX = Float.MAX_VALUE;
   private float minY = Float.MAX_VALUE;
   private float maxX = -Float.MAX_VALUE;
   private float maxY = -Float.MAX_VALUE;

   ImageRenderState(Matrix3x2f pose, ImageTexture texture, ScreenRectangle scissorArea) {
      this.pose = pose;
      this.texture = texture;
      this.scissorArea = scissorArea;
   }

   void add(ImageQuad image) {
      this.images.add(image);
      this.includeBounds(image, image.x(), image.y());
      this.includeBounds(image, image.x(), image.y() + image.height());
      this.includeBounds(image, image.x() + image.width(), image.y() + image.height());
      this.includeBounds(image, image.x() + image.width(), image.y());
   }

   public void buildVertices(VertexConsumer vertices) {
      for (ImageQuad image : this.images) {
         int batchIndex = ImageRenderer.getInstance().reserve(image);
         if (batchIndex >= 0) {
            float x0 = image.x();
            float y0 = image.y();
            float x1 = image.x() + image.width();
            float y1 = image.y() + image.height();
            this.vertex(vertices, image, x0, y0, 0.0F, 0.0F, image.colorTopLeft(), batchIndex);
            this.vertex(vertices, image, x0, y1, 0.0F, 1.0F, image.colorBottomLeft(), batchIndex);
            this.vertex(vertices, image, x1, y1, 1.0F, 1.0F, image.colorBottomRight(), batchIndex);
            this.vertex(vertices, image, x1, y0, 1.0F, 0.0F, image.colorTopRight(), batchIndex);
         }
      }
   }

   private void vertex(VertexConsumer consumer, ImageQuad image, float x, float y, float u, float v, int color, int batchIndex) {
      consumer.addVertexWith2DPose(this.pose, rotateX(image, x, y), rotateY(image, x, y)).setUv(u, v).setColor(color).setLineWidth((float)(batchIndex + 1));
   }

   private void includeBounds(ImageQuad image, float x, float y) {
      float rotatedX = rotateX(image, x, y);
      float rotatedY = rotateY(image, x, y);
      this.minX = Math.min(this.minX, rotatedX);
      this.minY = Math.min(this.minY, rotatedY);
      this.maxX = Math.max(this.maxX, rotatedX);
      this.maxY = Math.max(this.maxY, rotatedY);
   }

   private static float rotateX(ImageQuad image, float x, float y) {
      float rotation = image.rotationDegrees();
      if (rotation == 0.0F) {
         return x;
      } else {
         float dx = x - image.rotationOriginX();
         float dy = y - image.rotationOriginY();
         return image.rotationOriginX() + dx * image.rotationCos() - dy * image.rotationSin();
      }
   }

   private static float rotateY(ImageQuad image, float x, float y) {
      float rotation = image.rotationDegrees();
      if (rotation == 0.0F) {
         return y;
      } else {
         float dx = x - image.rotationOriginX();
         float dy = y - image.rotationOriginY();
         return image.rotationOriginY() + dx * image.rotationSin() + dy * image.rotationCos();
      }
   }

   public RenderPipeline pipeline() {
      return ImageRenderer.IMAGE_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return this.texture.setup();
   }

   public ScreenRectangle scissorArea() {
      return this.scissorArea;
   }

   public ScreenRectangle bounds() {
      if (this.images.isEmpty()) {
         return new ScreenRectangle(0, 0, 1, 1);
      } else {
         int x = (int)Math.floor((double)this.minX);
         int y = (int)Math.floor((double)this.minY);
         int width = Math.max(1, (int)Math.ceil((double)(this.maxX - this.minX)));
         int height = Math.max(1, (int)Math.ceil((double)(this.maxY - this.minY)));
         ScreenRectangle transformedBounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(this.pose);
         return this.scissorArea == null ? transformedBounds : this.scissorArea.intersection(transformedBounds);
      }
   }
}
