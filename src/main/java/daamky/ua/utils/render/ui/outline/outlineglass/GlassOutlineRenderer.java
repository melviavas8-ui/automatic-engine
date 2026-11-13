package daamky.ua.utils.render.ui.outline.outlineglass;

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

public final class GlassOutlineRenderer implements AutoCloseable {
   private static final int MAX_OUTLINES = 512;
   private static final int PARAMS_PER_OUTLINE = 6;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 49152;
   private static volatile GlassOutlineRenderer instance;
   public static final RenderPipeline GLASS_OUTLINE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/outline_glass"))
      .withVertexShader(id("ui/outline_glass/outline_glass"))
      .withFragmentShader(id("ui/outline_glass/outline_glass"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("GlassOutlineParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("BlurRegion", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltGlassOutline> preparedOutlines = new ArrayList<>(128);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty = true;

   private GlassOutlineRenderer() {
   }

   public static GlassOutlineRenderer getInstance() {
      GlassOutlineRenderer local = instance;
      if (local == null) {
         synchronized (GlassOutlineRenderer.class) {
            local = instance;
            if (local == null) {
               local = new GlassOutlineRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      GlassOutlineRenderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(DrawContext graphics) {
      this.activeGraphics = graphics;
   }

   public void draw(DrawContext graphics, BuiltGlassOutline outline) {
      this.beginFrame(graphics);
      this.enqueue(outline);
      this.flush();
   }

   public void enqueue(BuiltGlassOutline outline) {
      this.submit(this.activeGraphics, outline);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedOutlines.clear();
      this.paramsDirty = false;
   }

   public boolean isGlassOutlinePipeline(RenderPipeline pipeline) {
      return pipeline == GLASS_OUTLINE_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedOutlines.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("GlassOutlineParamsArray", buffer);
         }

         BlurFramebuffer.getInstance().bindBlurRegion(renderPass);
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

   int reserve(BuiltGlassOutline outline) {
      int index = this.preparedOutlines.size();
      if (index == 512) {
         return -1;
      } else {
         this.preparedOutlines.add(outline);
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltGlassOutline outline) {
      if (graphics != null && outline != null && outline.visible()) {
         try {
            BuiltGlassOutline normalized = this.normalize(outline);
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
            ((GuiGraphicsExtractorAccessor)graphics)
               .daamky$getGuiRenderState()
               .addSimpleElement(new GlassOutlineRenderState(pose, normalized, ScissorUtil.current()));
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
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_outline_glass_params", 128, uniformData);
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
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_outline_glass_params", 136, 49152L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltGlassOutline> batch) {
      int usedBytes = Math.max(1, batch.size()) * 6 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < batch.size(); i++) {
         BuiltGlassOutline outline = batch.get(i);
         int offset = i * 6 * 4 * 4;
         data.putFloat(offset, outline.radiusTopLeft());
         data.putFloat(offset + 4, outline.radiusTopRight());
         data.putFloat(offset + 8, outline.radiusBottomRight());
         data.putFloat(offset + 12, outline.radiusBottomLeft());
         data.putFloat(offset + 16, outline.width());
         data.putFloat(offset + 20, outline.height());
         data.putFloat(offset + 24, outline.thickness());
         data.putFloat(offset + 28, outline.smoothness());
         data.putFloat(offset + 32, outline.globalAlpha());
         data.putFloat(offset + 36, outline.fresnelPower());
         data.putFloat(offset + 40, outline.baseAlpha());
         data.putFloat(offset + 44, outline.fresnelMix());
         putColor(data, offset + 48, outline.fresnelColor());
         data.putFloat(offset + 64, outline.fresnelInvert() ? 1.0F : 0.0F);
         data.putFloat(offset + 68, outline.distortStrength());
         data.putFloat(offset + 72, outline.z());
         data.putFloat(offset + 76, Math.max(outline.squirt(), 0.001F));
         putColor(data, offset + 80, outline.color());
      }

      data.position(0);
      return data;
   }

   private BuiltGlassOutline normalize(BuiltGlassOutline outline) {
      float maxRadius = Math.max(0.0F, Math.min(outline.width(), outline.height()) * 0.5F);
      float maxThickness = Math.max(0.0F, Math.min(outline.width(), outline.height()) * 0.5F);
      float radiusTopLeft = clamp(outline.radiusTopLeft(), 0.0F, maxRadius);
      float radiusTopRight = clamp(outline.radiusTopRight(), 0.0F, maxRadius);
      float radiusBottomRight = clamp(outline.radiusBottomRight(), 0.0F, maxRadius);
      float radiusBottomLeft = clamp(outline.radiusBottomLeft(), 0.0F, maxRadius);
      float thickness = clamp(outline.thickness(), 0.0F, maxThickness);
      float globalAlpha = clamp(outline.globalAlpha(), 0.0F, 1.0F);
      float fresnelPower = Math.max(outline.fresnelPower(), 0.001F);
      float baseAlpha = clamp(outline.baseAlpha(), 0.0F, 1.0F);
      float fresnelMix = clamp(outline.fresnelMix(), 0.0F, 1.0F);
      float squirt = Math.max(outline.squirt(), 0.001F);
      float smoothness = Math.max(outline.smoothness(), 0.5F);
      return radiusTopLeft == outline.radiusTopLeft()
            && radiusTopRight == outline.radiusTopRight()
            && radiusBottomRight == outline.radiusBottomRight()
            && radiusBottomLeft == outline.radiusBottomLeft()
            && thickness == outline.thickness()
            && globalAlpha == outline.globalAlpha()
            && fresnelPower == outline.fresnelPower()
            && baseAlpha == outline.baseAlpha()
            && fresnelMix == outline.fresnelMix()
            && squirt == outline.squirt()
            && smoothness == outline.smoothness()
         ? outline
         : new BuiltGlassOutline(
            outline.x(),
            outline.y(),
            outline.width(),
            outline.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            outline.color(),
            globalAlpha,
            fresnelPower,
            outline.fresnelColor(),
            baseAlpha,
            outline.fresnelInvert(),
            fresnelMix,
            outline.distortStrength(),
            squirt,
            smoothness,
            outline.z()
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
