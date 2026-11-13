package daamky.ua.utils.render.ui.rectangle.rectdefault;

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

public final class DefaultRectangleRenderer implements AutoCloseable {
   private static final int MAX_RECTANGLES = 512;
   private static final int PARAMS_PER_RECTANGLE = 6;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 49152;
   private static volatile DefaultRectangleRenderer instance;
   public static final RenderPipeline RECTANGLE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/rect_default"))
      .withVertexShader(id("ui/shared/rect_quad"))
      .withFragmentShader(id("ui/rect_default/rect_default"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("RectangleParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltRectangle> preparedRectangles = new ArrayList<>(128);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty = true;

   private DefaultRectangleRenderer() {
   }

   public static DefaultRectangleRenderer getInstance() {
      DefaultRectangleRenderer local = instance;
      if (local == null) {
         synchronized (DefaultRectangleRenderer.class) {
            local = instance;
            if (local == null) {
               local = new DefaultRectangleRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      DefaultRectangleRenderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(DrawContext graphics) {
      this.activeGraphics = graphics;
   }

   public void draw(DrawContext graphics, BuiltRectangle rectangle) {
      this.beginFrame(graphics);
      this.enqueue(rectangle);
      this.flush();
   }

   public void enqueue(BuiltRectangle rectangle) {
      this.submit(this.activeGraphics, rectangle);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedRectangles.clear();
      this.paramsDirty = false;
   }

   public boolean isRectanglePipeline(RenderPipeline pipeline) {
      return pipeline == RECTANGLE_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedRectangles.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("RectangleParamsArray", buffer);
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

   boolean reserve(BuiltRectangle rectangle) {
      if (this.preparedRectangles.size() == 512) {
         return false;
      } else {
         this.preparedRectangles.add(rectangle);
         this.paramsDirty = true;
         return true;
      }
   }

   private void submit(DrawContext graphics, BuiltRectangle rectangle) {
      if (graphics != null && rectangle != null && rectangle.visible()) {
         try {
            BuiltRectangle normalized = this.normalize(rectangle);
            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            ((GuiGraphicsExtractorAccessor)graphics)
               .daamky$getGuiRenderState()
               .addSimpleElement(new DefaultRectangleRenderState(pose, normalized, ScissorUtil.current()));
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
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_rect_default_params", 128, uniformData);
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
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_rect_default_params", 136, 49152L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltRectangle> batch) {
      int usedBytes = Math.max(1, batch.size()) * 6 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < batch.size(); i++) {
         BuiltRectangle rectangle = batch.get(i);
         int offset = i * 6 * 4 * 4;
         data.putFloat(offset, rectangle.radiusTopLeft());
         data.putFloat(offset + 4, rectangle.radiusTopRight());
         data.putFloat(offset + 8, rectangle.radiusBottomRight());
         data.putFloat(offset + 12, rectangle.radiusBottomLeft());
         data.putFloat(offset + 16, rectangle.width());
         data.putFloat(offset + 20, rectangle.height());
         data.putFloat(offset + 24, rectangle.smoothness());
         data.putFloat(offset + 28, 0.0F);
         putColor(data, offset + 32, rectangle.colorTopLeft());
         putColor(data, offset + 48, rectangle.colorTopRight());
         putColor(data, offset + 64, rectangle.colorBottomRight());
         putColor(data, offset + 80, rectangle.colorBottomLeft());
      }

      data.position(0);
      return data;
   }

   private BuiltRectangle normalize(BuiltRectangle rectangle) {
      float maxRadius = Math.max(0.0F, Math.min(rectangle.width(), rectangle.height()) * 0.5F);
      float radiusTopLeft = clamp(rectangle.radiusTopLeft(), 0.0F, maxRadius);
      float radiusTopRight = clamp(rectangle.radiusTopRight(), 0.0F, maxRadius);
      float radiusBottomRight = clamp(rectangle.radiusBottomRight(), 0.0F, maxRadius);
      float radiusBottomLeft = clamp(rectangle.radiusBottomLeft(), 0.0F, maxRadius);
      float smoothness = Math.max(rectangle.smoothness(), 0.5F);
      return radiusTopLeft == rectangle.radiusTopLeft()
            && radiusTopRight == rectangle.radiusTopRight()
            && radiusBottomRight == rectangle.radiusBottomRight()
            && radiusBottomLeft == rectangle.radiusBottomLeft()
            && smoothness == rectangle.smoothness()
         ? rectangle
         : new BuiltRectangle(
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
      this.preparedRectangles.clear();
      this.activeGraphics = null;
      this.closeParamsBuffer();
   }
}
