package daamky.ua.utils.render.ui.rectangle.recthalftone;

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

public final class HalftoneRectangleRenderer implements AutoCloseable {
   private static final int MAX_RECTANGLES = 384;
   private static final int PARAMS_PER_RECTANGLE = 8;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 49152;
   private static volatile HalftoneRectangleRenderer instance;
   public static final RenderPipeline HALFTONE_RECTANGLE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/rect_halftone"))
      .withVertexShader(id("ui/shared/halftone_zippy"))
      .withFragmentShader(id("ui/rect_halftone/rect_halftone"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("HalftoneRectangleParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltHalftoneRectangle> preparedRectangles = new ArrayList<>(64);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty = true;

   private HalftoneRectangleRenderer() {
   }

   public static HalftoneRectangleRenderer getInstance() {
      HalftoneRectangleRenderer local = instance;
      if (local == null) {
         synchronized (HalftoneRectangleRenderer.class) {
            local = instance;
            if (local == null) {
               local = new HalftoneRectangleRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      HalftoneRectangleRenderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(DrawContext graphics) {
      this.activeGraphics = graphics;
   }

   public void draw(DrawContext graphics, BuiltHalftoneRectangle rectangle) {
      this.beginFrame(graphics);
      this.enqueue(rectangle);
      this.flush();
   }

   public void enqueue(BuiltHalftoneRectangle rectangle) {
      this.submit(this.activeGraphics, rectangle);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedRectangles.clear();
      this.paramsDirty = false;
   }

   public boolean isHalftoneRectanglePipeline(RenderPipeline pipeline) {
      return pipeline == HALFTONE_RECTANGLE_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedRectangles.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("HalftoneRectangleParamsArray", buffer);
         }
      }
   }

   public void prepareBuffers() {
      if (!this.preparedRectangles.isEmpty() && this.paramsDirty) {
         GpuBuffer buffer = this.ensureWritableParamsBuffer();
         if (buffer != null) {
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer uniformData = this.buildUniformData(stack, this.preparedRectangles);
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

   int reserve(BuiltHalftoneRectangle rectangle) {
      int index = this.preparedRectangles.size();
      if (index == 384) {
         return -1;
      } else {
         this.preparedRectangles.add(rectangle);
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltHalftoneRectangle rectangle) {
      if (graphics != null && rectangle != null && rectangle.visible()) {
         try {
            BuiltHalftoneRectangle normalized = this.normalize(rectangle);
            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState().addSimpleElement(new HalftoneRectangleRenderState(pose, normalized));
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
               ByteBuffer uniformData = this.buildUniformData(stack, this.preparedRectangles);
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_rect_halftone_params", 128, uniformData);
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
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_rect_halftone_params", 136, 49152L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltHalftoneRectangle> batch) {
      int usedBytes = Math.max(1, batch.size()) * 8 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < batch.size(); i++) {
         BuiltHalftoneRectangle rectangle = batch.get(i);
         int offset = i * 8 * 4 * 4;
         data.putFloat(offset, rectangle.radiusTopLeft());
         data.putFloat(offset + 4, rectangle.radiusTopRight());
         data.putFloat(offset + 8, rectangle.radiusBottomRight());
         data.putFloat(offset + 12, rectangle.radiusBottomLeft());
         data.putFloat(offset + 16, rectangle.width());
         data.putFloat(offset + 20, rectangle.height());
         data.putFloat(offset + 24, rectangle.smoothness());
         data.putFloat(offset + 28, rectangle.dotSize());
         putColor(data, offset + 32, rectangle.colorTopLeft());
         putColor(data, offset + 48, rectangle.colorTopRight());
         putColor(data, offset + 64, rectangle.colorBottomRight());
         putColor(data, offset + 80, rectangle.colorBottomLeft());
         putColor(data, offset + 96, rectangle.dotColor());
         data.putFloat(offset + 112, rectangle.dotSpacing());
         data.putFloat(offset + 116, 0.0F);
         data.putFloat(offset + 120, 0.0F);
         data.putFloat(offset + 124, 0.0F);
      }

      data.position(0);
      return data;
   }

   private BuiltHalftoneRectangle normalize(BuiltHalftoneRectangle rectangle) {
      float maxRadius = Math.max(0.0F, Math.min(rectangle.width(), rectangle.height()) * 0.5F);
      float maxDotSize = Math.max(Math.min(rectangle.width(), rectangle.height()), 0.0F);
      float radiusTopLeft = clamp(rectangle.radiusTopLeft(), 0.0F, maxRadius);
      float radiusTopRight = clamp(rectangle.radiusTopRight(), 0.0F, maxRadius);
      float radiusBottomRight = clamp(rectangle.radiusBottomRight(), 0.0F, maxRadius);
      float radiusBottomLeft = clamp(rectangle.radiusBottomLeft(), 0.0F, maxRadius);
      float smoothness = Math.max(rectangle.smoothness(), 0.5F);
      float dotSize = clamp(rectangle.dotSize(), 0.0F, maxDotSize);
      float dotSpacing = Math.max(rectangle.dotSpacing(), 0.0F);
      return radiusTopLeft == rectangle.radiusTopLeft()
            && radiusTopRight == rectangle.radiusTopRight()
            && radiusBottomRight == rectangle.radiusBottomRight()
            && radiusBottomLeft == rectangle.radiusBottomLeft()
            && smoothness == rectangle.smoothness()
            && dotSize == rectangle.dotSize()
            && dotSpacing == rectangle.dotSpacing()
         ? rectangle
         : new BuiltHalftoneRectangle(
            rectangle.x(),
            rectangle.y(),
            rectangle.width(),
            rectangle.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            rectangle.colorTopLeft(),
            rectangle.colorTopRight(),
            rectangle.colorBottomRight(),
            rectangle.colorBottomLeft(),
            smoothness,
            rectangle.dotColor(),
            dotSize,
            dotSpacing
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
      this.preparedRectangles.clear();
      this.activeGraphics = null;
      this.closeParamsBuffer();
   }
}
