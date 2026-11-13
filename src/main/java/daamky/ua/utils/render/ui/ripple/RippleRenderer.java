package daamky.ua.utils.render.ui.ripple;

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

public final class RippleRenderer implements AutoCloseable {
   private static final int MAX_RIPPLES = 512;
   private static final int PARAMS_PER_RIPPLE = 5;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 40960;
   private static volatile RippleRenderer instance;
   public static final RenderPipeline RIPPLE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/ripple"))
      .withVertexShader(id("ui/ripple/ripple"))
      .withFragmentShader(id("ui/ripple/ripple"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("RippleParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltRipple> preparedRipples = new ArrayList<>(128);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty = true;

   private RippleRenderer() {
   }

   public static RippleRenderer getInstance() {
      RippleRenderer local = instance;
      if (local == null) {
         synchronized (RippleRenderer.class) {
            local = instance;
            if (local == null) {
               local = new RippleRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      RippleRenderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(DrawContext graphics) {
      this.activeGraphics = graphics;
   }

   public void enqueue(BuiltRipple ripple) {
      this.submit(this.activeGraphics, ripple);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedRipples.clear();
      this.paramsDirty = false;
   }

   public boolean isRipplePipeline(RenderPipeline pipeline) {
      return pipeline == RIPPLE_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedRipples.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("RippleParamsArray", buffer);
         }
      }
   }

   public void prepareBuffers() {
      if (!this.preparedRipples.isEmpty() && this.paramsDirty) {
         GpuBuffer buffer = this.ensureWritableParamsBuffer();
         if (buffer != null) {
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer uniformData = this.buildUniformData(stack, this.preparedRipples);
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

   boolean reserve(BuiltRipple ripple) {
      if (this.preparedRipples.size() == 512) {
         return false;
      } else {
         this.preparedRipples.add(ripple);
         this.paramsDirty = true;
         return true;
      }
   }

   private void submit(DrawContext graphics, BuiltRipple ripple) {
      if (graphics != null && ripple != null && ripple.visible()) {
         try {
            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState().addSimpleElement(new RippleRenderState(pose, ripple, ScissorUtil.current()));
         } catch (RuntimeException var4) {
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
               ByteBuffer uniformData = this.buildUniformData(stack, this.preparedRipples);
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_ripple_params", 128, uniformData);
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
      if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 40960L) {
         return this.paramsBuffer;
      } else {
         this.closeParamsBuffer();

         try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_ripple_params", 136, 40960L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltRipple> batch) {
      int usedBytes = Math.max(1, batch.size()) * 5 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < batch.size(); i++) {
         BuiltRipple ripple = batch.get(i);
         int offset = i * 5 * 4 * 4;
         data.putFloat(offset, ripple.radiusTopLeft());
         data.putFloat(offset + 4, ripple.radiusTopRight());
         data.putFloat(offset + 8, ripple.radiusBottomRight());
         data.putFloat(offset + 12, ripple.radiusBottomLeft());
         data.putFloat(offset + 16, ripple.width());
         data.putFloat(offset + 20, ripple.height());
         data.putFloat(offset + 24, ripple.smoothness());
         data.putFloat(offset + 28, ripple.isTextured() ? 1.0F : 0.0F);
         data.putFloat(offset + 32, ripple.centerX());
         data.putFloat(offset + 36, ripple.centerY());
         data.putFloat(offset + 40, ripple.rippleRadius());
         data.putFloat(offset + 44, ripple.rippleSmoothness());
         putColor(data, offset + 48, ripple.sourceColor());
         putColor(data, offset + 64, ripple.targetColor());
      }

      data.position(0);
      return data;
   }

   private static void putColor(ByteBuffer data, int offset, int color) {
      data.putFloat(offset, (float)(color >>> 16 & 0xFF) / 255.0F);
      data.putFloat(offset + 4, (float)(color >>> 8 & 0xFF) / 255.0F);
      data.putFloat(offset + 8, (float)(color & 0xFF) / 255.0F);
      data.putFloat(offset + 12, (float)(color >>> 24 & 0xFF) / 255.0F);
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
      this.preparedRipples.clear();
      this.activeGraphics = null;
      this.closeParamsBuffer();
   }
}
