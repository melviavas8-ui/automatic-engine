package daamky.ua.utils.render.ui.ripple;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;

public final class RippleRenderState implements GuiElementRenderState {
   private final Matrix3x2f pose;
   private final BuiltRipple ripple;
   private final ScreenRectangle scissorArea;

   public RippleRenderState(Matrix3x2f pose, BuiltRipple ripple, ScreenRectangle scissorArea) {
      this.pose = pose;
      this.ripple = ripple;
      this.scissorArea = scissorArea;
   }

   public void buildVertices(VertexConsumer vertices) {
      if (RippleRenderer.getInstance().reserve(this.ripple)) {
         vertices.addVertexWith2DPose(this.pose, this.ripple.x(), this.ripple.y()).setUv(0.0F, 0.0F);
         vertices.addVertexWith2DPose(this.pose, this.ripple.x(), this.ripple.y() + this.ripple.height()).setUv(0.0F, 1.0F);
         vertices.addVertexWith2DPose(this.pose, this.ripple.x() + this.ripple.width(), this.ripple.y() + this.ripple.height()).setUv(1.0F, 1.0F);
         vertices.addVertexWith2DPose(this.pose, this.ripple.x() + this.ripple.width(), this.ripple.y()).setUv(1.0F, 0.0F);
      }
   }

   public RenderPipeline pipeline() {
      return RippleRenderer.RIPPLE_PIPELINE;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }

   public ScreenRectangle scissorArea() {
      return this.scissorArea;
   }

   public ScreenRectangle bounds() {
      return new ScreenRectangle(Math.round(this.ripple.x()), Math.round(this.ripple.y()), Math.round(this.ripple.width()), Math.round(this.ripple.height()))
         .transformMaxBounds(this.pose);
   }
}
