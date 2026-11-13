package daamky.ua.utils.render.ui.outline.outlinedefault;

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
import daamky.ua.utils.render.ScissorUtil;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;

public final class DefaultOutlineRenderer implements AutoCloseable {
   private static final int MAX_OUTLINES = 512;
   private static final int PARAMS_PER_OUTLINE = 6;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 49152;
   private static volatile DefaultOutlineRenderer instance;
   public static final RenderPipeline OUTLINE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/outline_default"))
      .withVertexShader(id("ui/shared/outline_quad"))
      .withFragmentShader(id("ui/outline_default/outline_default"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("OutlineParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltOutline> preparedOutlines = new ArrayList<>(128);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty = true;

   private DefaultOutlineRenderer() {
   }

   public static DefaultOutlineRenderer getInstance() {
      DefaultOutlineRenderer local = instance;
      if (local == null) {
         synchronized (DefaultOutlineRenderer.class) {
            local = instance;
            if (local == null) {
               local = new DefaultOutlineRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      DefaultOutlineRenderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(DrawContext graphics) {
      this.activeGraphics = graphics;
   }

   public void draw(DrawContext graphics, BuiltOutline outline) {
      this.beginFrame(graphics);
      this.enqueue(outline);
      this.flush();
   }

   public void enqueue(BuiltOutline outline) {
      this.submit(this.activeGraphics, outline);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedOutlines.clear();
      this.paramsDirty = false;
   }

   public boolean isOutlinePipeline(RenderPipeline pipeline) {
      return pipeline == OUTLINE_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedOutlines.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("OutlineParamsArray", buffer);
         }
      }
   }

   public void prepareBuffers() {
      if (!this.preparedOutlines.isEmpty() && this.paramsDirty) {
         GpuBuffer buffer = this.ensureWritableParamsBuffer();
         if (buffer != null) {
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer uniformData = this.buildUniformData(stack, this.preparedOutlines);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(0L, (long)uniformData.remaining()), uniformData);
                  this.paramsDirty = false;
               } catch (Throwable var6) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                     }
                  }

                  throw var6;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (RuntimeException var7) {
               this.paramsDirty = true;
            }
         }
      }
   }

   int reserve(BuiltOutline outline) {
      int index = this.preparedOutlines.size();
      if (index == 512) {
         return -1;
      } else {
         this.preparedOutlines.add(outline);
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltOutline outline) {
      if (graphics != null && outline != null && outline.visible()) {
         try {
            BuiltOutline normalized = this.normalize(outline);
            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            ((GuiGraphicsExtractorAccessor)graphics)
               .daamky$getGuiRenderState()
               .addSimpleElement(new DefaultOutlineRenderState(pose, normalized, ScissorUtil.current()));
         } catch (RuntimeException var5) {
         }
      }
   }

   private GpuBuffer ensureParamsBuffer() {
      if (!this.paramsDirty && this.paramsBuffer != null) {
         return this.paramsBuffer;
      } else {
         this.prepareBuffers();
         if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
         } else {
            this.closeParamsBuffer();
            MemoryStack stack = MemoryStack.stackPush();

            GpuBuffer var3;
            try {
               ByteBuffer uniformData = this.buildUniformData(stack, this.preparedOutlines);
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_outline_default_params", 128, uniformData);
               this.paramsDirty = false;
               var3 = this.paramsBuffer;
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

            return var3;
         }
      }
   }

   private GpuBuffer ensureWritableParamsBuffer() {
      if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 49152L) {
         return this.paramsBuffer;
      } else {
         this.closeParamsBuffer();

         try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_outline_default_params", 136, 49152L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltOutline> batch) {
      int usedBytes = Math.max(1, batch.size()) * 6 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < batch.size(); i++) {
         BuiltOutline outline = batch.get(i);
         int offset = i * 6 * 4 * 4;
         data.putFloat(offset, outline.radiusTopLeft());
         data.putFloat(offset + 4, outline.radiusTopRight());
         data.putFloat(offset + 8, outline.radiusBottomRight());
         data.putFloat(offset + 12, outline.radiusBottomLeft());
         data.putFloat(offset + 16, outline.width());
         data.putFloat(offset + 20, outline.height());
         data.putFloat(offset + 24, outline.thickness());
         data.putFloat(offset + 28, outline.smoothness());
         putColor(data, offset + 32, outline.colorTopLeft());
         putColor(data, offset + 48, outline.colorTopRight());
         putColor(data, offset + 64, outline.colorBottomRight());
         putColor(data, offset + 80, outline.colorBottomLeft());
      }

      data.position(0);
      return data;
   }

   private BuiltOutline normalize(BuiltOutline outline) {
      float maxRadius = Math.max(0.0F, Math.min(outline.width(), outline.height()) * 0.5F);
      float maxThickness = Math.max(0.0F, Math.min(outline.width(), outline.height()) * 0.5F);
      float radiusTopLeft = clamp(outline.radiusTopLeft(), 0.0F, maxRadius);
      float radiusTopRight = clamp(outline.radiusTopRight(), 0.0F, maxRadius);
      float radiusBottomRight = clamp(outline.radiusBottomRight(), 0.0F, maxRadius);
      float radiusBottomLeft = clamp(outline.radiusBottomLeft(), 0.0F, maxRadius);
      float thickness = clamp(outline.thickness(), 0.0F, maxThickness);
      float smoothness = Math.max(outline.smoothness(), 0.5F);
      return radiusTopLeft == outline.radiusTopLeft()
            && radiusTopRight == outline.radiusTopRight()
            && radiusBottomRight == outline.radiusBottomRight()
            && radiusBottomLeft == outline.radiusBottomLeft()
            && thickness == outline.thickness()
            && smoothness == outline.smoothness()
         ? outline
         : new BuiltOutline(
            outline.x(),
            outline.y(),
            outline.width(),
            outline.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            outline.colorTopLeft(),
            outline.colorTopRight(),
            outline.colorBottomRight(),
            outline.colorBottomLeft(),
            smoothness
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

   private void closeParamsBuffer() {
      if (this.paramsBuffer != null) {
         this.paramsBuffer.close();
         this.paramsBuffer = null;
      }
   }

   private static Identifier id(String path) {
      return Identifier.of("daamky", path);
   }

   @Override
   public void close() {
      this.preparedOutlines.clear();
      this.activeGraphics = null;
      this.closeParamsBuffer();
   }
}
