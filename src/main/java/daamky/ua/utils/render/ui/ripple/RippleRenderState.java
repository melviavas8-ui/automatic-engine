package daamky.ua.utils.render.ui.ripple;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

public final class RippleRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f pose;
   private final BuiltRipple ripple;
   private final ScreenRect scissorArea;

   public RippleRenderState(Matrix3x2f pose, BuiltRipple ripple, ScreenRect scissorArea) {
      this.pose = pose;
      this.ripple = ripple;
      this.scissorArea = scissorArea;
   }

   public void setupVertices(VertexConsumer vertices) {
      if (RippleRenderer.getInstance().reserve(this.ripple)) {
         vertices.vertex(this.pose, this.ripple.x(), this.ripple.y()).texture(0.0F, 0.0F);
         vertices.vertex(this.pose, this.ripple.x(), this.ripple.y() + this.ripple.height()).texture(0.0F, 1.0F);
         vertices.vertex(this.pose, this.ripple.x() + this.ripple.width(), this.ripple.y() + this.ripple.height()).texture(1.0F, 1.0F);
         vertices.vertex(this.pose, this.ripple.x() + this.ripple.width(), this.ripple.y()).texture(1.0F, 0.0F);
      }
   }

   public RenderPipeline pipeline() {
      return RippleRenderer.RIPPLE_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   public ScreenRect scissorArea() {
      return this.scissorArea;
   }

   public ScreenRect bounds() {
      return new ScreenRect(Math.round(this.ripple.x()), Math.round(this.ripple.y()), Math.round(this.ripple.width()), Math.round(this.ripple.height()))
         .transformEachVertex(this.pose);
   }
}
