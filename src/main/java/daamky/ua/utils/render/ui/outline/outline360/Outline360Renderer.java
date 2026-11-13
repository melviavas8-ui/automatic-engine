package daamky.ua.utils.render.ui.outline.outline360;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import daamky.ua.mixin.accessor.GuiGraphicsExtractorAccessor;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;

public final class Outline360Renderer implements AutoCloseable {
   private static final int MAX_OUTLINES = 256;
   private static final int MAX_RANGES = 1024;
   private static final int PARAMS_PER_OUTLINE = 4;
   private static final int PARAMS_PER_RANGE = 2;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int OUTLINE_UNIFORM_BYTES = 16384;
   private static final int RANGE_UNIFORM_BYTES = 32768;
   private static volatile Outline360Renderer instance;
   public static final RenderPipeline OUTLINE_360_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/outline_360"))
      .withVertexShader(id("ui/shared/outline_quad"))
      .withFragmentShader(id("ui/outline_360/outline_360"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("Outline360ParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("Outline360RangesArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltOutline360> preparedOutlines = new ArrayList<>(64);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private GpuBuffer rangesBuffer;
   private boolean paramsDirty = true;

   private Outline360Renderer() {
   }

   public static Outline360Renderer getInstance() {
      Outline360Renderer local = instance;
      if (local == null) {
         synchronized (Outline360Renderer.class) {
            local = instance;
            if (local == null) {
               local = new Outline360Renderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      Outline360Renderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(DrawContext graphics) {
      this.activeGraphics = graphics;
   }

   public void draw(DrawContext graphics, BuiltOutline360 outline) {
      this.beginFrame(graphics);
      this.enqueue(outline);
      this.flush();
   }

   public void enqueue(BuiltOutline360 outline) {
      this.submit(this.activeGraphics, outline);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedOutlines.clear();
      this.paramsDirty = false;
   }

   public boolean isOutline360Pipeline(RenderPipeline pipeline) {
      return pipeline == OUTLINE_360_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedOutlines.isEmpty()) {
         this.ensureBuffers();
         if (this.paramsBuffer != null) {
            renderPass.setUniform("Outline360ParamsArray", this.paramsBuffer);
         }

         if (this.rangesBuffer != null) {
            renderPass.setUniform("Outline360RangesArray", this.rangesBuffer);
         }
      }
   }

   public void prepareBuffers() {
      if (!this.preparedOutlines.isEmpty() && this.paramsDirty) {
         if (this.ensureWritableBuffers()) {
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer paramsData = stack.calloc(this.outlineUniformBytes(this.preparedOutlines));
                  ByteBuffer rangesData = stack.calloc(this.rangeUniformBytes(this.preparedOutlines));
                  this.buildUniformData(paramsData, rangesData, this.preparedOutlines);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.paramsBuffer.slice(0L, (long)paramsData.remaining()), paramsData);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.rangesBuffer.slice(0L, (long)rangesData.remaining()), rangesData);
                  this.paramsDirty = false;
               } catch (Throwable var5) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                     }
                  }

                  throw var5;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (RuntimeException var6) {
               this.paramsDirty = true;
            }
         }
      }
   }

   int reserve(BuiltOutline360 outline) {
      int index = this.preparedOutlines.size();
      if (index == 256) {
         return -1;
      } else {
         this.preparedOutlines.add(outline);
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltOutline360 outline) {
      if (graphics != null && outline != null && outline.visible()) {
         try {
            BuiltOutline360 normalized = this.normalize(outline);
            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState().addSimpleElement(new Outline360RenderState(pose, normalized));
         } catch (RuntimeException var5) {
         }
      }
   }

   private void ensureBuffers() {
      if (this.paramsDirty || this.paramsBuffer == null || this.rangesBuffer == null) {
         this.prepareBuffers();
         if (this.paramsDirty || this.paramsBuffer == null || this.rangesBuffer == null) {
            this.closeBuffers();
            MemoryStack stack = MemoryStack.stackPush();

            try {
               ByteBuffer paramsData = stack.calloc(this.outlineUniformBytes(this.preparedOutlines));
               ByteBuffer rangesData = stack.calloc(this.rangeUniformBytes(this.preparedOutlines));
               this.buildUniformData(paramsData, rangesData, this.preparedOutlines);
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_outline_360_params", 128, paramsData);
               this.rangesBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_outline_360_ranges", 128, rangesData);
               this.paramsDirty = false;
            } catch (Throwable var5) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (stack != null) {
               stack.close();
            }
         }
      }
   }

   private boolean ensureWritableBuffers() {
      boolean paramsReady = this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 16384L;
      boolean rangesReady = this.rangesBuffer != null && !this.rangesBuffer.isClosed() && this.rangesBuffer.size() >= 32768L;
      if (paramsReady && rangesReady) {
         return true;
      } else {
         this.closeBuffers();

         try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_outline_360_params", 136, 16384L);
            this.rangesBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_outline_360_ranges", 136, 32768L);
            return true;
         } catch (RuntimeException var4) {
            this.closeBuffers();
            return false;
         }
      }
   }

   private int outlineUniformBytes(List<BuiltOutline360> batch) {
      return Math.max(1, batch.size()) * 4 * 4 * 4;
   }

   private int rangeUniformBytes(List<BuiltOutline360> batch) {
      int ranges = 0;

      for (BuiltOutline360 outline : batch) {
         ranges += Math.min(outline.ranges().size(), 1024 - ranges);
         if (ranges == 1024) {
            break;
         }
      }

      return Math.max(1, ranges) * 2 * 4 * 4;
   }

   private void buildUniformData(ByteBuffer paramsData, ByteBuffer rangesData, List<BuiltOutline360> batch) {
      int rangeOffset = 0;

      for (int i = 0; i < batch.size(); i++) {
         BuiltOutline360 outline = batch.get(i);
         int outlineOffset = i * 4 * 4 * 4;
         int rangeCount = Math.min(outline.ranges().size(), 1024 - rangeOffset);
         paramsData.putFloat(outlineOffset, outline.radiusTopLeft());
         paramsData.putFloat(outlineOffset + 4, outline.radiusTopRight());
         paramsData.putFloat(outlineOffset + 8, outline.radiusBottomRight());
         paramsData.putFloat(outlineOffset + 12, outline.radiusBottomLeft());
         paramsData.putFloat(outlineOffset + 16, outline.width());
         paramsData.putFloat(outlineOffset + 20, outline.height());
         paramsData.putFloat(outlineOffset + 24, outline.thickness());
         paramsData.putFloat(outlineOffset + 28, outline.smoothness());
         putColor(paramsData, outlineOffset + 32, outline.defaultColor());
         paramsData.putFloat(outlineOffset + 48, (float)rangeOffset);
         paramsData.putFloat(outlineOffset + 52, (float)rangeCount);
         paramsData.putFloat(outlineOffset + 56, outline.blendDegrees());
         paramsData.putFloat(outlineOffset + 60, outline.angleOffsetDegrees());

         for (int rangeIndex = 0; rangeIndex < rangeCount; rangeIndex++) {
            Outline360Range range = outline.ranges().get(rangeIndex);
            int offset = (rangeOffset + rangeIndex) * 2 * 4 * 4;
            rangesData.putFloat(offset, range.startDegrees());
            rangesData.putFloat(offset + 4, range.endDegrees());
            rangesData.putFloat(offset + 8, 0.0F);
            rangesData.putFloat(offset + 12, 0.0F);
            putColor(rangesData, offset + 16, range.color());
         }

         rangeOffset += rangeCount;
      }

      paramsData.position(0);
      rangesData.position(0);
   }

   private BuiltOutline360 normalize(BuiltOutline360 outline) {
      float maxRadius = Math.max(0.0F, Math.min(outline.width(), outline.height()) * 0.5F);
      float maxThickness = Math.max(0.0F, Math.min(outline.width(), outline.height()) * 0.5F);
      float radiusTopLeft = clamp(outline.radiusTopLeft(), 0.0F, maxRadius);
      float radiusTopRight = clamp(outline.radiusTopRight(), 0.0F, maxRadius);
      float radiusBottomRight = clamp(outline.radiusBottomRight(), 0.0F, maxRadius);
      float radiusBottomLeft = clamp(outline.radiusBottomLeft(), 0.0F, maxRadius);
      float thickness = clamp(outline.thickness(), 0.0F, maxThickness);
      float smoothness = Math.max(outline.smoothness(), 0.5F);
      float blendDegrees = Math.max(outline.blendDegrees(), 0.0F);
      return radiusTopLeft == outline.radiusTopLeft()
            && radiusTopRight == outline.radiusTopRight()
            && radiusBottomRight == outline.radiusBottomRight()
            && radiusBottomLeft == outline.radiusBottomLeft()
            && thickness == outline.thickness()
            && smoothness == outline.smoothness()
            && blendDegrees == outline.blendDegrees()
         ? outline
         : new BuiltOutline360(
            outline.x(),
            outline.y(),
            outline.width(),
            outline.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            outline.defaultColor(),
            smoothness,
            blendDegrees,
            outline.angleOffsetDegrees(),
            outline.ranges()
         );
   }

   private static void putColor(ByteBuffer data, int offset, int color) {
      data.putFloat(offset, (float)(color >>> 16 & 0xFF) / 255.0F);
      data.putFloat(offset + 4, (float)(color >>> 8 & 0xFF) / 255.0F);
      data.putFloat(offset + 8, (float)(color & 0xFF) / 255.0F);
      data.putFloat(offset + 12, (float)(color >>> 24 & 0xFF) / 255.0F);
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private void closeBuffers() {
      if (this.paramsBuffer != null) {
         this.paramsBuffer.close();
         this.paramsBuffer = null;
      }

      if (this.rangesBuffer != null) {
         this.rangesBuffer.close();
         this.rangesBuffer = null;
      }
   }

   private static Identifier id(String path) {
      return Identifier.of("daamky", path);
   }

   @Override
   public void close() {
      this.preparedOutlines.clear();
      this.activeGraphics = null;
      this.closeBuffers();
   }
}
