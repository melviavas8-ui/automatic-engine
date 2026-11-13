package daamky.ua.utils.render.ui.effecticon;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class EffectIconRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f pose;
   private final EffectIconTexture texture;
   private final List<EffectIconQuad> icons = new ArrayList<>(16);
   private float minX = Float.MAX_VALUE;
   private float minY = Float.MAX_VALUE;
   private float maxX = -Float.MAX_VALUE;
   private float maxY = -Float.MAX_VALUE;

   EffectIconRenderState(Matrix3x2f pose, EffectIconTexture texture) {
      this.pose = pose;
      this.texture = texture;
   }

   void add(EffectIconQuad icon) {
      this.icons.add(icon);
      this.minX = Math.min(this.minX, icon.x());
      this.minY = Math.min(this.minY, icon.y());
      this.maxX = Math.max(this.maxX, icon.x() + icon.size());
      this.maxY = Math.max(this.maxY, icon.y() + icon.size());
   }

   public void setupVertices(VertexConsumer vertices) {
      for (EffectIconQuad icon : this.icons) {
         float x0 = icon.x();
         float y0 = icon.y();
         float x1 = icon.x() + icon.size();
         float y1 = icon.y() + icon.size();
         int color = icon.color();
         this.vertex(vertices, x0, y0, icon.u0(), icon.v0(), color);
         this.vertex(vertices, x0, y1, icon.u0(), icon.v1(), color);
         this.vertex(vertices, x1, y1, icon.u1(), icon.v1(), color);
         this.vertex(vertices, x1, y0, icon.u1(), icon.v0(), color);
      }
   }

   private void vertex(VertexConsumer consumer, float x, float y, float u, float v, int color) {
      consumer.vertex(this.pose, x, y).texture(u, v).color(color);
   }

   public RenderPipeline pipeline() {
      return EffectIconRenderer.EFFECT_ICON_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return this.texture.setup();
   }

   public ScreenRect scissorArea() {
      return null;
   }

   public ScreenRect bounds() {
      if (this.icons.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      } else {
         int x = (int)Math.floor((double)this.minX);
         int y = (int)Math.floor((double)this.minY);
         int width = Math.max(1, (int)Math.ceil((double)(this.maxX - this.minX)));
         int height = Math.max(1, (int)Math.ceil((double)(this.maxY - this.minY)));
         return new ScreenRect(x, y, width, height).transformEachVertex(this.pose);
      }
   }
}
