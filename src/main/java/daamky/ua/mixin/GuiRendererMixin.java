package daamky.ua.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.ui.blur.BlurFramebuffer;
import daamky.ua.utils.render.ui.glass.GlassRenderer;
import daamky.ua.utils.render.ui.image.ImageRenderer;
import daamky.ua.utils.render.ui.outline.outline360.Outline360Renderer;
import daamky.ua.utils.render.ui.outline.outlinedefault.DefaultOutlineRenderer;
import daamky.ua.utils.render.ui.outline.outlineglass.GlassOutlineRenderer;
import daamky.ua.utils.render.ui.rectangle.rectdefault.DefaultRectangleRenderer;
import daamky.ua.utils.render.ui.rectangle.recthalficon.HalfIconRectangleRenderer;
import daamky.ua.utils.render.ui.rectangle.recthalftone.HalftoneRectangleRenderer;
import daamky.ua.utils.render.ui.ripple.RippleRenderer;
import daamky.ua.utils.render.ui.zippy.ZippyRenderer;

@Mixin({GuiRenderer.class})
public abstract class GuiRendererMixin {
   private RenderPass daamky$currentRenderPass;
   private boolean daamky$blurDrawActive;
   private boolean daamky$glassDrawActive;
   private boolean daamky$glassOutlineDrawActive;
   private boolean daamky$rectangleDrawActive;
   private boolean daamky$halfIconRectangleDrawActive;
   private boolean daamky$halftoneRectangleDrawActive;
   private boolean daamky$zippyDrawActive;
   private boolean daamky$outlineDrawActive;
   private boolean daamky$outline360DrawActive;
   private boolean daamky$imageDrawActive;
   private boolean daamky$itemDrawActive;
   private boolean daamky$rippleDrawActive;

   public GuiRendererMixin() {
   }

   @Inject(
      method = {"render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"},
      at = {@At("HEAD")}
   )
   private void daamky$beginBlurFrame(GpuBufferSlice fogBuffer, CallbackInfo ci) {
      BlurFramebuffer.getInstance().beginGuiFrame();
      GlassRenderer.getInstance().beginGuiFrame();
      GlassOutlineRenderer.getInstance().beginGuiFrame();
      DefaultRectangleRenderer.getInstance().beginGuiFrame();
      HalfIconRectangleRenderer.getInstance().beginGuiFrame();
      HalftoneRectangleRenderer.getInstance().beginGuiFrame();
      ZippyRenderer.getInstance().beginGuiFrame();
      DefaultOutlineRenderer.getInstance().beginGuiFrame();
      Outline360Renderer.getInstance().beginGuiFrame();
      ImageRenderer.getInstance().beginGuiFrame();
      RippleRenderer.getInstance().beginGuiFrame();
      RenderItem.beginGuiFrame();
   }

   @Inject(
      method = {"prepare"},
      at = {@At("HEAD")}
   )
   private void daamky$preparePendingBlurResources(CallbackInfo ci) {
      BlurFramebuffer.getInstance().preparePending();
   }

   @Inject(
      method = {"prepare"},
      at = {@At("RETURN")}
   )
   private void daamky$prepareRenderUniforms(CallbackInfo ci) {
      BlurFramebuffer.getInstance().prepareBuffers();
      GlassRenderer.getInstance().prepareBuffers();
      GlassOutlineRenderer.getInstance().prepareBuffers();
      DefaultRectangleRenderer.getInstance().prepareBuffers();
      HalfIconRectangleRenderer.getInstance().prepareBuffers();
      HalftoneRectangleRenderer.getInstance().prepareBuffers();
      ZippyRenderer.getInstance().prepareBuffers();
      DefaultOutlineRenderer.getInstance().prepareBuffers();
      Outline360Renderer.getInstance().prepareBuffers();
      ImageRenderer.getInstance().prepareBuffers();
      RippleRenderer.getInstance().prepareBuffers();
      RenderItem.prepareBuffers();
   }

   @Inject(
      method = {"renderPreparedDraws"},
      at = {@At("HEAD")}
   )
   private void daamky$prepareBlurCapture(GpuBufferSlice fogBuffer, CallbackInfo ci) {
      BlurFramebuffer.getInstance().prepareGuiDraw();
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"
      )
   )
   private void daamky$trackPipeline(RenderPass renderPass, RenderPipeline pipeline) {
      this.daamky$currentRenderPass = renderPass;
      this.daamky$blurDrawActive = BlurFramebuffer.getInstance().isBlurPipeline(pipeline);
      this.daamky$glassDrawActive = GlassRenderer.getInstance().isGlassPipeline(pipeline);
      this.daamky$glassOutlineDrawActive = GlassOutlineRenderer.getInstance().isGlassOutlinePipeline(pipeline);
      this.daamky$rectangleDrawActive = DefaultRectangleRenderer.getInstance().isRectanglePipeline(pipeline);
      this.daamky$halfIconRectangleDrawActive = HalfIconRectangleRenderer.getInstance().isHalfIconRectanglePipeline(pipeline);
      this.daamky$halftoneRectangleDrawActive = HalftoneRectangleRenderer.getInstance().isHalftoneRectanglePipeline(pipeline);
      this.daamky$zippyDrawActive = ZippyRenderer.getInstance().isZippyPipeline(pipeline);
      this.daamky$outlineDrawActive = DefaultOutlineRenderer.getInstance().isOutlinePipeline(pipeline);
      this.daamky$outline360DrawActive = Outline360Renderer.getInstance().isOutline360Pipeline(pipeline);
      this.daamky$imageDrawActive = ImageRenderer.getInstance().isImagePipeline(pipeline);
      this.daamky$rippleDrawActive = RippleRenderer.getInstance().isRipplePipeline(pipeline);
      this.daamky$itemDrawActive = RenderItem.isItemPipeline(pipeline);
      renderPass.setPipeline(pipeline);
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V",
         shift = Shift.BEFORE
      )}
   )
   private void daamky$bindBlurParams(@Coerce Object draw, RenderPass renderPass, GpuBuffer gpuBuffer, IndexType indexType, CallbackInfo ci) {
      if (this.daamky$blurDrawActive && this.daamky$currentRenderPass != null) {
         BlurFramebuffer.getInstance().bindBlurParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$glassDrawActive && this.daamky$currentRenderPass != null) {
         GlassRenderer.getInstance().bindParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$glassOutlineDrawActive && this.daamky$currentRenderPass != null) {
         GlassOutlineRenderer.getInstance().bindParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$rectangleDrawActive && this.daamky$currentRenderPass != null) {
         DefaultRectangleRenderer.getInstance().bindParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$halfIconRectangleDrawActive && this.daamky$currentRenderPass != null) {
         HalfIconRectangleRenderer.getInstance().bindParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$halftoneRectangleDrawActive && this.daamky$currentRenderPass != null) {
         HalftoneRectangleRenderer.getInstance().bindParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$zippyDrawActive && this.daamky$currentRenderPass != null) {
         ZippyRenderer.getInstance().bindParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$outlineDrawActive && this.daamky$currentRenderPass != null) {
         DefaultOutlineRenderer.getInstance().bindParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$outline360DrawActive && this.daamky$currentRenderPass != null) {
         Outline360Renderer.getInstance().bindParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$imageDrawActive && this.daamky$currentRenderPass != null) {
         ImageRenderer.getInstance().bindParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$rippleDrawActive && this.daamky$currentRenderPass != null) {
         RippleRenderer.getInstance().bindParams(this.daamky$currentRenderPass);
      }

      if (this.daamky$itemDrawActive && this.daamky$currentRenderPass != null) {
         RenderItem.bindParams(this.daamky$currentRenderPass);
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V"},
      at = {@At("RETURN")}
   )
   private void daamky$clearTrackedPipeline(@Coerce Object draw, RenderPass renderPass, GpuBuffer gpuBuffer, IndexType indexType, CallbackInfo ci) {
      this.daamky$currentRenderPass = null;
      this.daamky$blurDrawActive = false;
      this.daamky$glassDrawActive = false;
      this.daamky$glassOutlineDrawActive = false;
      this.daamky$rectangleDrawActive = false;
      this.daamky$halfIconRectangleDrawActive = false;
      this.daamky$halftoneRectangleDrawActive = false;
      this.daamky$zippyDrawActive = false;
      this.daamky$outlineDrawActive = false;
      this.daamky$outline360DrawActive = false;
      this.daamky$imageDrawActive = false;
      this.daamky$itemDrawActive = false;
      this.daamky$rippleDrawActive = false;
   }
}
