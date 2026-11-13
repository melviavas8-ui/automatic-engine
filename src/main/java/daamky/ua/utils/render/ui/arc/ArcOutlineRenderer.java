package daamky.ua.utils.render.ui.arc;

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

public final class ArcOutlineRenderer implements AutoCloseable {
   private static final int MAX_ARC_OUTLINES = 256;
   private static final int PARAMS_PER_ARC_OUTLINE = 4;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 16384;
   private static volatile ArcOutlineRenderer instance;
   public static final RenderPipeline ARC_OUTLINE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/arc_outline"))
      .withVertexShader(id("ui/shared/outline_quad"))
      .withFragmentShader(id("ui/arc/arc_outline"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ArcOutlineParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltArcOutline> preparedOutlines = new ArrayList<>(64);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty = true;

   private ArcOutlineRenderer() {
   }

   public static ArcOutlineRenderer getInstance() {
      ArcOutlineRenderer local = instance;
      if (local == null) {
         synchronized (ArcOutlineRenderer.class) {
            local = instance;
            if (local == null) {
               local = new ArcOutlineRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      ArcOutlineRenderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(DrawContext graphics) {
      this.activeGraphics = graphics;
   }

   public void draw(DrawContext graphics, BuiltArcOutline outline) {
      this.beginFrame(graphics);
      this.enqueue(outline);
      this.flush();
   }

   public void enqueue(BuiltArcOutline outline) {
      this.submit(this.activeGraphics, outline);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedOutlines.clear();
      this.paramsDirty = false;
   }

   public boolean isArcOutlinePipeline(RenderPipeline pipeline) {
      return pipeline == ARC_OUTLINE_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedOutlines.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("ArcOutlineParamsArray", buffer);
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

   int reserve(BuiltArcOutline outline) {
      int index = this.preparedOutlines.size();
      if (index == 256) {
         return -1;
      } else {
         this.preparedOutlines.add(outline);
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltArcOutline outline) {
      if (graphics != null && outline != null && outline.visible()) {
         try {
            BuiltArcOutline normalized = this.normalize(outline);
            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            ((GuiGraphicsExtractorAccessor)graphics)
               .daamky$getGuiRenderState()
               .addSimpleElement(new ArcOutlineRenderState(pose, normalized, ScissorUtil.current()));
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
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_arc_outline_params", 128, uniformData);
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
      if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 16384L) {
         return this.paramsBuffer;
      } else {
         this.closeParamsBuffer();

         try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_arc_outline_params", 136, 16384L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltArcOutline> batch) {
      int usedBytes = Math.max(1, batch.size()) * 4 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < batch.size(); i++) {
         BuiltArcOutline outline = batch.get(i);
         int offset = i * 4 * 4 * 4;
         data.putFloat(offset, outline.size());
         data.putFloat(offset + 4, outline.arcThickness());
         data.putFloat(offset + 8, outline.degree());
         data.putFloat(offset + 12, outline.rotation());
         data.putFloat(offset + 16, outline.outlineThickness());
         data.putFloat(offset + 20, 0.0F);
         data.putFloat(offset + 24, 0.0F);
         data.putFloat(offset + 28, 0.0F);
         this.putColor(data, offset + 32, outline.fillColor());
         this.putColor(data, offset + 48, outline.outlineColor());
      }

      data.position(0);
      return data;
   }

   private BuiltArcOutline normalize(BuiltArcOutline outline) {
      float size = Math.max(0.0F, outline.size());
      float arcThickness = Math.max(0.0F, Math.min(outline.arcThickness(), size));
      float degree = Math.max(0.0F, Math.min(outline.degree(), 360.0F));
      float outlineThickness = Math.max(0.0F, Math.min(outline.outlineThickness(), arcThickness * 0.5F));
      return new BuiltArcOutline(
         outline.x(), outline.y(), size, arcThickness, degree, outline.rotation(), outlineThickness, outline.fillColor(), outline.outlineColor()
      );
   }

   private void putColor(ByteBuffer data, int offset, int color) {
      data.putFloat(offset, (float)(color >>> 16 & 0xFF) / 255.0F);
      data.putFloat(offset + 4, (float)(color >>> 8 & 0xFF) / 255.0F);
      data.putFloat(offset + 8, (float)(color & 0xFF) / 255.0F);
      data.putFloat(offset + 12, (float)(color >>> 24 & 0xFF) / 255.0F);
   }

   private static Identifier id(String path) {
      return Identifier.of("daamky", path);
   }

   @Override
   public void close() {
      this.closeParamsBuffer();
   }

   private void closeParamsBuffer() {
      if (this.paramsBuffer != null) {
         this.paramsBuffer.close();
         this.paramsBuffer = null;
      }
   }
}
