package daamky.ua.utils.render.ui.zippy;

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

public final class ZippyRenderer implements AutoCloseable {
   private static final int MAX_ZIPPY = 256;
   private static final int PARAMS_PER_ZIPPY = 3;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 12288;
   private static volatile ZippyRenderer instance;
   public static final RenderPipeline ZIPPY_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/zippy"))
      .withVertexShader(id("ui/shared/halftone_zippy"))
      .withFragmentShader(id("ui/zippy/zippy"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ZippyParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltZippy> preparedZippy = new ArrayList<>(32);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty = true;

   private ZippyRenderer() {
   }

   public static ZippyRenderer getInstance() {
      ZippyRenderer local = instance;
      if (local == null) {
         synchronized (ZippyRenderer.class) {
            local = instance;
            if (local == null) {
               local = new ZippyRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      ZippyRenderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(DrawContext graphics) {
      this.activeGraphics = graphics;
   }

   public void draw(DrawContext graphics, BuiltZippy zippy) {
      this.beginFrame(graphics);
      this.enqueue(zippy);
      this.flush();
   }

   public void enqueue(BuiltZippy zippy) {
      this.submit(this.activeGraphics, zippy);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedZippy.clear();
      this.paramsDirty = false;
   }

   public boolean isZippyPipeline(RenderPipeline pipeline) {
      return pipeline == ZIPPY_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedZippy.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("ZippyParamsArray", buffer);
         }
      }
   }

   public void prepareBuffers() {
      if (!this.preparedZippy.isEmpty() && this.paramsDirty) {
         GpuBuffer buffer = this.ensureWritableParamsBuffer();
         if (buffer != null) {
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer uniformData = this.buildUniformData(stack, this.preparedZippy);
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

   int reserve(BuiltZippy zippy) {
      int index = this.preparedZippy.size();
      if (index == 256) {
         return -1;
      } else {
         this.preparedZippy.add(zippy);
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltZippy zippy) {
      if (graphics != null && zippy != null && zippy.visible()) {
         try {
            BuiltZippy normalized = this.normalize(zippy);
            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState().addSimpleElement(new ZippyRenderState(pose, normalized));
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
               ByteBuffer uniformData = this.buildUniformData(stack, this.preparedZippy);
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_zippy_params", 128, uniformData);
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
      if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 12288L) {
         return this.paramsBuffer;
      } else {
         this.closeParamsBuffer();

         try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_zippy_params", 136, 12288L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltZippy> batch) {
      int usedBytes = Math.max(1, batch.size()) * 3 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);
      float time = (float)(System.currentTimeMillis() % 120000L) / 1000.0F;

      for (int i = 0; i < batch.size(); i++) {
         BuiltZippy zippy = batch.get(i);
         int offset = i * 3 * 4 * 4;
         data.putFloat(offset, zippy.radiusTopLeft());
         data.putFloat(offset + 4, zippy.radiusTopRight());
         data.putFloat(offset + 8, zippy.radiusBottomRight());
         data.putFloat(offset + 12, zippy.radiusBottomLeft());
         data.putFloat(offset + 16, zippy.width());
         data.putFloat(offset + 20, zippy.height());
         data.putFloat(offset + 24, zippy.smoothness());
         data.putFloat(offset + 28, time + zippy.timeOffset());
         putColor(data, offset + 32, zippy.color());
      }

      data.position(0);
      return data;
   }

   private BuiltZippy normalize(BuiltZippy zippy) {
      float maxRadius = Math.max(0.0F, Math.min(zippy.width(), zippy.height()) * 0.5F);
      float radiusTopLeft = clamp(zippy.radiusTopLeft(), 0.0F, maxRadius);
      float radiusTopRight = clamp(zippy.radiusTopRight(), 0.0F, maxRadius);
      float radiusBottomRight = clamp(zippy.radiusBottomRight(), 0.0F, maxRadius);
      float radiusBottomLeft = clamp(zippy.radiusBottomLeft(), 0.0F, maxRadius);
      float smoothness = Math.max(zippy.smoothness(), 0.5F);
      return radiusTopLeft == zippy.radiusTopLeft()
            && radiusTopRight == zippy.radiusTopRight()
            && radiusBottomRight == zippy.radiusBottomRight()
            && radiusBottomLeft == zippy.radiusBottomLeft()
            && smoothness == zippy.smoothness()
         ? zippy
         : new BuiltZippy(
            zippy.x(),
            zippy.y(),
            zippy.width(),
            zippy.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            zippy.color(),
            smoothness,
            zippy.timeOffset()
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
      this.preparedZippy.clear();
      this.activeGraphics = null;
      this.closeParamsBuffer();
   }
}
