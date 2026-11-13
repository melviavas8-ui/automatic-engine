package daamky.ua.utils.render.ui.glass;

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
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;
import daamky.ua.utils.render.ui.blur.BlurFramebuffer;
import daamky.ua.utils.render.ui.blur.BuiltBlur;

public final class GlassRenderer implements AutoCloseable {
   private static final int MAX_GLASSES = 512;
   private static final int PARAMS_PER_GLASS = 5;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 40960;
   private static volatile GlassRenderer instance;
   public static final RenderPipeline GLASS_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/glass"))
      .withVertexShader(id("ui/glass/glass"))
      .withFragmentShader(id("ui/glass/glass"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("GlassParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("BlurRegion", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltGlass> preparedGlasses = new ArrayList<>(64);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty = true;

   private GlassRenderer() {
   }

   public static GlassRenderer getInstance() {
      GlassRenderer local = instance;
      if (local == null) {
         synchronized (GlassRenderer.class) {
            local = instance;
            if (local == null) {
               local = new GlassRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      GlassRenderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(DrawContext graphics) {
      this.activeGraphics = graphics;
   }

   public void draw(DrawContext graphics, BuiltGlass glass) {
      this.beginFrame(graphics);
      this.enqueue(glass);
      this.flush();
   }

   public void enqueue(BuiltGlass glass) {
      this.submit(this.activeGraphics, glass);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedGlasses.clear();
      this.paramsDirty = false;
   }

   public boolean isGlassPipeline(RenderPipeline pipeline) {
      return pipeline == GLASS_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedGlasses.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("GlassParamsArray", buffer);
         }

         BlurFramebuffer.getInstance().bindBlurRegion(renderPass);
      }
   }

   public void prepareBuffers() {
      if (!this.preparedGlasses.isEmpty() && this.paramsDirty) {
         GpuBuffer buffer = this.ensureWritableParamsBuffer();
         if (buffer != null) {
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer uniformData = this.buildUniformData(stack, this.preparedGlasses);
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

   int reserve(BuiltGlass glass) {
      int index = this.preparedGlasses.size();
      if (index == 512) {
         return -1;
      } else {
         this.preparedGlasses.add(glass);
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltGlass glass) {
      if (graphics != null && glass != null && glass.visible()) {
         try {
            BuiltGlass normalized = this.normalize(glass);
            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            BlurFramebuffer.getInstance()
               .requestCapture(
                  graphics,
                  new BuiltBlur(
                     normalized.x(),
                     normalized.y(),
                     normalized.width(),
                     normalized.height(),
                     normalized.radiusTopLeft(),
                     normalized.radiusTopRight(),
                     normalized.radiusBottomRight(),
                     normalized.radiusBottomLeft(),
                     1.0F,
                     30.0F,
                     ColorUtil.WHITE
                  )
               );
            ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState().addSimpleElement(new GlassRenderState(pose, normalized, ScissorUtil.current()));
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
               ByteBuffer uniformData = this.buildUniformData(stack, this.preparedGlasses);
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_glass_params", 128, uniformData);
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
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_glass_params", 136, 40960L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltGlass> batch) {
      int usedBytes = Math.max(1, batch.size()) * 5 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < batch.size(); i++) {
         BuiltGlass glass = batch.get(i);
         int offset = i * 5 * 4 * 4;
         data.putFloat(offset, glass.radiusTopLeft());
         data.putFloat(offset + 4, glass.radiusTopRight());
         data.putFloat(offset + 8, glass.radiusBottomRight());
         data.putFloat(offset + 12, glass.radiusBottomLeft());
         data.putFloat(offset + 16, glass.width());
         data.putFloat(offset + 20, glass.height());
         data.putFloat(offset + 24, 0.5F);
         data.putFloat(offset + 28, Math.max(glass.squirt(), 0.001F));
         data.putFloat(offset + 32, glass.globalAlpha());
         data.putFloat(offset + 36, glass.fresnelPower());
         data.putFloat(offset + 40, glass.baseAlpha());
         data.putFloat(offset + 44, glass.fresnelMix());
         putColor(data, offset + 48, glass.fresnelColor());
         data.putFloat(offset + 64, glass.fresnelInvert() ? 1.0F : 0.0F);
         data.putFloat(offset + 68, glass.distortStrength());
         data.putFloat(offset + 72, glass.z());
         data.putFloat(offset + 76, 0.0F);
      }

      data.position(0);
      return data;
   }

   private BuiltGlass normalize(BuiltGlass glass) {
      float maxRadius = Math.max(0.0F, Math.min(glass.width(), glass.height()) * 0.5F);
      float radiusTopLeft = clamp(glass.radiusTopLeft(), 0.0F, maxRadius);
      float radiusTopRight = clamp(glass.radiusTopRight(), 0.0F, maxRadius);
      float radiusBottomRight = clamp(glass.radiusBottomRight(), 0.0F, maxRadius);
      float radiusBottomLeft = clamp(glass.radiusBottomLeft(), 0.0F, maxRadius);
      float globalAlpha = clamp(glass.globalAlpha(), 0.0F, 1.0F);
      float fresnelPower = Math.max(glass.fresnelPower(), 0.001F);
      float baseAlpha = clamp(glass.baseAlpha(), 0.0F, 1.0F);
      float fresnelMix = clamp(glass.fresnelMix(), 0.0F, 1.0F);
      float squirt = Math.max(glass.squirt(), 0.001F);
      return radiusTopLeft == glass.radiusTopLeft()
            && radiusTopRight == glass.radiusTopRight()
            && radiusBottomRight == glass.radiusBottomRight()
            && radiusBottomLeft == glass.radiusBottomLeft()
            && globalAlpha == glass.globalAlpha()
            && fresnelPower == glass.fresnelPower()
            && baseAlpha == glass.baseAlpha()
            && fresnelMix == glass.fresnelMix()
            && squirt == glass.squirt()
         ? glass
         : new BuiltGlass(
            glass.x(),
            glass.y(),
            glass.width(),
            glass.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            glass.color(),
            globalAlpha,
            fresnelPower,
            glass.fresnelColor(),
            baseAlpha,
            glass.fresnelInvert(),
            fresnelMix,
            glass.distortStrength(),
            squirt,
            glass.z()
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
      this.preparedGlasses.clear();
      this.activeGraphics = null;
      this.closeParamsBuffer();
   }
}
