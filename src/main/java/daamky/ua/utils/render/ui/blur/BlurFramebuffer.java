package daamky.ua.utils.render.ui.blur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import daamky.ua.mixin.accessor.GuiGraphicsExtractorAccessor;
import daamky.ua.utils.render.ScissorUtil;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.post.fogblur.FogBlurRenderer;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;

public final class BlurFramebuffer implements AutoCloseable {
   private static final int MAX_BLUR_RECTS = 1024;
   private static final int MAX_BLUR_ITERATIONS = 4;
   private static final int PARAMS_PER_RECT = 2;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 32768;
   private static final int REGION_UNIFORM_BYTES = 16;
   private static final int KAWASE_UNIFORM_BYTES = 48;
   private static final float MAX_BLUR_RENDER_SCALE = 0.5F;
   private static final float MEDIUM_BLUR_RENDER_SCALE = 0.44F;
   private static final float MIN_BLUR_RENDER_SCALE = 0.4F;
   private static volatile BlurFramebuffer instance;
   private static float skyFallbackRed = 0.55F;
   private static float skyFallbackGreen = 0.65F;
   private static float skyFallbackBlue = 0.78F;
   public static final RenderPipeline BATCHED_BLUR_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/batched_blur"))
      .withVertexShader(id("ui/batched_blur/batched_blur"))
      .withFragmentShader(id("ui/batched_blur/batched_blur"))
      .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH, Mode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("BlurParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("BlurRegion", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline KAWASE_DOWN_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/kawase_down"))
      .withVertexShader(id("ui/kawase/down"))
      .withFragmentShader(id("ui/kawase/down"))
      .withVertexFormat(DefaultVertexFormat.POSITION, Mode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline KAWASE_UP_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/kawase_up"))
      .withVertexShader(id("ui/kawase/down"))
      .withFragmentShader(id("ui/kawase/up"))
      .withVertexFormat(DefaultVertexFormat.POSITION, Mode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BlurFramebuffer.PendingBlur> pendingBlurs = new ArrayList<>(32);
   private final List<BuiltBlur> preparedBlurs = new ArrayList<>(32);
   private GuiGraphics activeGraphics;
   private TextureTarget blurTarget;
   private final TextureTarget[] downTargets = new TextureTarget[4];
   private final TextureTarget[] upTargets = new TextureTarget[4];
   private GpuBuffer fullscreenQuadBuffer;
   private GpuBuffer paramsBuffer;
   private GpuBuffer regionBuffer;
   private GpuBuffer kawaseParamsBuffer;
   private TextureSetup blurTextureSetup = TextureSetup.noTexture();
   private boolean captureRequested;
   private boolean paramsDirty = true;
   private boolean regionDirty = true;
   private int regionX;
   private int regionY;
   private int regionWidth;
   private int regionHeight;
   private int preparedIterations;
   private float maxBlurRadius;

   private BlurFramebuffer() {
   }

   public static BlurFramebuffer getInstance() {
      BlurFramebuffer local = instance;
      if (local == null) {
         synchronized (BlurFramebuffer.class) {
            local = instance;
            if (local == null) {
               local = new BlurFramebuffer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      BlurFramebuffer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public static void setSkyFallbackColor(float red, float green, float blue) {
      if (Float.isFinite(red) && Float.isFinite(green) && Float.isFinite(blue)) {
         skyFallbackRed = clamp(red, 0.0F, 1.0F);
         skyFallbackGreen = clamp(green, 0.0F, 1.0F);
         skyFallbackBlue = clamp(blue, 0.0F, 1.0F);
      }
   }

   public void beginFrame(GuiGraphics graphics) {
      this.activeGraphics = graphics;
   }

   public void draw(GuiGraphics graphics, BuiltBlur blur) {
      this.beginFrame(graphics);
      this.enqueue(blur);
      this.flush();
   }

   public void enqueue(BuiltBlur blur) {
      this.submit(this.activeGraphics, blur);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedBlurs.clear();
      this.captureRequested = false;
      this.paramsDirty = false;
      this.regionDirty = false;
      this.regionWidth = 0;
      this.regionHeight = 0;
      this.preparedIterations = 0;
      this.maxBlurRadius = 0.0F;
      this.blurTextureSetup = TextureSetup.noTexture();
   }

   public void preparePending() {
      if (!this.pendingBlurs.isEmpty()) {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft == null) {
            this.pendingBlurs.clear();
         } else {
            RenderTarget mainTarget = minecraft.getMainRenderTarget();
            if (mainTarget != null && mainTarget.getColorTexture() != null && this.computeBlurRegion(minecraft, mainTarget)) {
               this.preparedIterations = this.computeIterations(this.maxBlurRadius);
               this.ensureBlurTargets(
                  this.regionWidth,
                  this.regionHeight,
                  this.computeRenderScale(this.maxBlurRadius, this.regionWidth, this.regionHeight),
                  this.preparedIterations
               );
               if (this.blurTarget != null && this.blurTarget.getColorTextureView() != null) {
                  TextureSetup textureSetup = TextureSetup.singleTexture(this.blurTarget.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                  this.blurTextureSetup = textureSetup;
                  this.captureRequested = true;
                  this.regionDirty = true;
                  this.uploadRegionBuffer();
                  this.pendingBlurs.clear();
               } else {
                  this.pendingBlurs.clear();
               }
            } else {
               this.pendingBlurs.clear();
            }
         }
      }
   }

   public TextureSetup textureSetup() {
      return this.blurTextureSetup;
   }

   public void requestCapture(GuiGraphics graphics, BuiltBlur blur) {
      if (graphics != null && blur != null && blur.visible()) {
         try {
            BuiltBlur normalized = this.normalize(blur);
            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            this.pendingBlurs.add(new BlurFramebuffer.PendingBlur(normalized, pose, ScissorUtil.current()));
         } catch (RuntimeException var5) {
         }
      }
   }

   public void prepareGuiDraw() {
      if (this.captureRequested && this.regionWidth > 0 && this.regionHeight > 0) {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft != null) {
            RenderTarget mainTarget = minecraft.getMainRenderTarget();
            if (mainTarget != null && mainTarget.getColorTextureView() != null) {
               GpuTextureView guiSource = FogBlurRenderer.getGuiSourceTextureView(mainTarget.width, mainTarget.height);
               this.renderBlurChain(mainTarget, guiSource);
            }
         }
      }
   }

   public boolean isBlurPipeline(RenderPipeline pipeline) {
      return pipeline == BATCHED_BLUR_PIPELINE;
   }

   public void bindBlurParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedBlurs.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("BlurParamsArray", buffer);
         }

         GpuBuffer region = this.ensureRegionBuffer();
         if (region != null) {
            renderPass.setUniform("BlurRegion", region);
         }
      }
   }

   public void bindBlurRegion(RenderPass renderPass) {
      if (renderPass != null) {
         GpuBuffer region = this.ensureRegionBuffer();
         if (region != null) {
            renderPass.setUniform("BlurRegion", region);
         }
      }
   }

   int reserve(BuiltBlur blur) {
      int index = this.preparedBlurs.size();
      if (index == 1024) {
         return -1;
      } else {
         this.preparedBlurs.add(blur);
         this.paramsDirty = true;
         this.maxBlurRadius = Math.max(this.maxBlurRadius, blur.blurRadius());
         return index;
      }
   }

   private void submit(GuiGraphics graphics, BuiltBlur blur) {
      if (blur != null && blur.visible()) {
         if (graphics != null) {
            try {
               BuiltBlur normalized = this.normalize(blur);
               Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
               ScreenRectangle scissorArea = ScissorUtil.current();
               this.pendingBlurs.add(new BlurFramebuffer.PendingBlur(normalized, pose, scissorArea));
               ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState().submitGuiElement(new BlurRenderState(pose, normalized, scissorArea));
            } catch (RuntimeException var6) {
            }
         }
      }
   }

   private GpuBuffer ensureParamsBuffer() {
      if (this.paramsDirty) {
         this.prepareBuffers();
      }

      if (!this.paramsDirty && this.paramsBuffer != null) {
         return this.paramsBuffer;
      } else if (this.paramsDirty) {
         this.closeParamsBuffer();
         MemoryStack stack = MemoryStack.stackPush();

         try {
            ByteBuffer uniformData = this.buildUniformData(stack, this.preparedBlurs);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_blur_params", 136, uniformData);
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

         this.paramsDirty = false;
         return this.paramsBuffer;
      } else {
         if (this.paramsBuffer == null || this.paramsBuffer.isClosed() || this.paramsBuffer.size() < 32768L) {
            this.closeParamsBuffer();
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_blur_params", 136, 32768L);
         }

         return this.paramsBuffer;
      }
   }

   public void prepareBuffers() {
      if (!this.preparedBlurs.isEmpty() && this.paramsDirty) {
         GpuBuffer buffer = this.ensureWritableParamsBuffer();
         if (buffer != null) {
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer uniformData = this.buildUniformData(stack, this.preparedBlurs);
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

   private GpuBuffer ensureWritableParamsBuffer() {
      if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 32768L) {
         return this.paramsBuffer;
      } else {
         this.closeParamsBuffer();

         try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_blur_params", 136, 32768L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltBlur> batch) {
      int usedBytes = Math.max(1, batch.size()) * 2 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < batch.size(); i++) {
         BuiltBlur blur = batch.get(i);
         int offset = i * 2 * 4 * 4;
         data.putFloat(offset, blur.radiusTopLeft());
         data.putFloat(offset + 4, blur.radiusTopRight());
         data.putFloat(offset + 8, blur.radiusBottomRight());
         data.putFloat(offset + 12, blur.radiusBottomLeft());
         data.putFloat(offset + 16, blur.width());
         data.putFloat(offset + 20, blur.height());
         data.putFloat(offset + 24, blur.smoothness());
         data.putFloat(offset + 28, blur.blurRadius());
      }

      data.position(0);
      return data;
   }

   private void ensureBlurTargets(int width, int height, float renderScale, int iterations) {
      if (width > 0 && height > 0) {
         int resultWidth = Math.max(Math.round((float)width * renderScale), 1);
         int resultHeight = Math.max(Math.round((float)height * renderScale), 1);
         if (this.blurTarget == null) {
            this.blurTarget = new TextureTarget("daamky_blur_result", resultWidth, resultHeight, false);
         } else if (this.blurTarget.width != resultWidth || this.blurTarget.height != resultHeight) {
            this.blurTarget.resize(resultWidth, resultHeight);
         }

         int targetWidth = resultWidth;
         int targetHeight = resultHeight;

         for (int i = 0; i < iterations; i++) {
            this.downTargets[i] = this.ensureTarget(this.downTargets[i], "daamky_blur_down_" + i, targetWidth, targetHeight);
            if (i < iterations - 1) {
               this.upTargets[i] = this.ensureTarget(this.upTargets[i], "daamky_blur_up_" + i, targetWidth, targetHeight);
            }

            targetWidth = Math.max(targetWidth / 2, 1);
            targetHeight = Math.max(targetHeight / 2, 1);
         }
      }
   }

   private TextureTarget ensureTarget(TextureTarget target, String name, int width, int height) {
      if (target == null) {
         return new TextureTarget(name, width, height, false);
      } else {
         if (target.width != width || target.height != height) {
            target.resize(width, height);
         }

         return target;
      }
   }

   private boolean computeBlurRegion(Minecraft minecraft, RenderTarget mainTarget) {
      Window window = minecraft.getWindow();
      int guiWidth = Math.max(window.getGuiScaledWidth(), 1);
      int guiHeight = Math.max(window.getGuiScaledHeight(), 1);
      float scaleX = (float)mainTarget.width / (float)guiWidth;
      float scaleY = (float)mainTarget.height / (float)guiHeight;
      float minX = (float)guiWidth;
      float minY = (float)guiHeight;
      float maxX = 0.0F;
      float maxY = 0.0F;
      this.maxBlurRadius = 0.0F;
      boolean hasBounds = false;

      for (BlurFramebuffer.PendingBlur pendingBlur : this.pendingBlurs) {
         BuiltBlur blur = pendingBlur.blur();
         float padding = Render2DCoordinateSpace.toGui(Math.max(16.0F, blur.blurRadius() * 2.0F));
         ScreenRectangle bounds = new ScreenRectangle(Math.round(blur.x()), Math.round(blur.y()), Math.round(blur.width()), Math.round(blur.height()))
            .transformMaxBounds(pendingBlur.pose());
         if (pendingBlur.scissorArea() != null) {
            bounds = pendingBlur.scissorArea().intersection(bounds);
            if (bounds == null) {
               continue;
            }
         }

         minX = Math.min(minX, (float)bounds.left() - padding);
         minY = Math.min(minY, (float)bounds.top() - padding);
         maxX = Math.max(maxX, (float)bounds.right() + padding);
         maxY = Math.max(maxY, (float)bounds.bottom() + padding);
         this.maxBlurRadius = Math.max(this.maxBlurRadius, blur.blurRadius());
         hasBounds = true;
      }

      if (!hasBounds) {
         return false;
      } else {
         minX = clamp(minX, 0.0F, (float)guiWidth);
         minY = clamp(minY, 0.0F, (float)guiHeight);
         maxX = clamp(maxX, minX + 1.0F, (float)guiWidth);
         maxY = clamp(maxY, minY + 1.0F, (float)guiHeight);
         int x0 = clampInt((int)Math.floor((double)(minX * scaleX)), 0, mainTarget.width - 1);
         int x1 = clampInt((int)Math.ceil((double)(maxX * scaleX)), x0 + 1, mainTarget.width);
         int y0 = clampInt((int)Math.floor((double)(((float)guiHeight - maxY) * scaleY)), 0, mainTarget.height - 1);
         int y1 = clampInt((int)Math.ceil((double)(((float)guiHeight - minY) * scaleY)), y0 + 1, mainTarget.height);
         this.regionX = x0;
         this.regionY = y0;
         this.regionWidth = Math.max(x1 - x0, 1);
         this.regionHeight = Math.max(y1 - y0, 1);
         return this.regionWidth > 0 && this.regionHeight > 0;
      }
   }

   private void renderBlurChain(RenderTarget mainTarget, GpuTextureView sourceOverride) {
      if (this.blurTarget != null) {
         int iterations = this.preparedIterations > 0 ? this.preparedIterations : this.computeIterations(this.maxBlurRadius);
         float offsetScale = clamp(this.maxBlurRadius / 18.0F, 0.85F, 2.0F);
         GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
         GpuTextureView source = sourceOverride != null ? sourceOverride : mainTarget.getColorTextureView();
         int sourceWidth = mainTarget.width;
         int sourceHeight = mainTarget.height;
         float sourceX = (float)this.regionX / (float)Math.max(mainTarget.width, 1);
         float sourceY = (float)this.regionY / (float)Math.max(mainTarget.height, 1);
         float sourceW = (float)this.regionWidth / (float)Math.max(mainTarget.width, 1);
         float sourceH = (float)this.regionHeight / (float)Math.max(mainTarget.height, 1);

         for (int i = 0; i < iterations; i++) {
            TextureTarget target = this.downTargets[i];
            this.renderKawasePass(KAWASE_DOWN_PIPELINE, source, sourceWidth, sourceHeight, target, offsetScale, sourceX, sourceY, sourceW, sourceH, sampler);
            source = target.getColorTextureView();
            sourceWidth = target.width;
            sourceHeight = target.height;
            sourceX = 0.0F;
            sourceY = 0.0F;
            sourceW = 1.0F;
            sourceH = 1.0F;
         }

         for (int i = iterations - 2; i >= 0; i--) {
            TextureTarget target = this.upTargets[i];
            this.renderKawasePass(KAWASE_UP_PIPELINE, source, sourceWidth, sourceHeight, target, offsetScale, 0.0F, 0.0F, 1.0F, 1.0F, sampler);
            source = target.getColorTextureView();
            sourceWidth = target.width;
            sourceHeight = target.height;
         }

         this.renderKawasePass(KAWASE_UP_PIPELINE, source, sourceWidth, sourceHeight, this.blurTarget, offsetScale, 0.0F, 0.0F, 1.0F, 1.0F, sampler);
      }
   }

   private int computeIterations(float blurRadius) {
      if (blurRadius >= 52.0F) {
         return 4;
      } else {
         return blurRadius >= 36.0F ? 3 : 2;
      }
   }

   private float computeRenderScale(float blurRadius, int width, int height) {
      int area = Math.max(width, 1) * Math.max(height, 1);
      if (blurRadius >= 30.0F || area >= 220000) {
         return 0.4F;
      } else {
         return !(blurRadius >= 18.0F) && area < 120000 ? 0.5F : 0.44F;
      }
   }

   private void renderKawasePass(
      RenderPipeline pipeline,
      GpuTextureView source,
      int sourceWidth,
      int sourceHeight,
      TextureTarget target,
      float offsetScale,
      float sourceX,
      float sourceY,
      float sourceW,
      float sourceH,
      GpuSampler sampler
   ) {
      if (source != null && target != null) {
         GpuBuffer fullscreenQuad = this.ensureFullscreenQuadBuffer();
         if (fullscreenQuad != null) {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(Mode.QUADS);
            GpuBuffer indices = indexBuffer.getBuffer(6);
            MemoryStack stack = MemoryStack.stackPush();

            label71: {
               try {
                  ByteBuffer uniformData = stack.calloc(48);
                  uniformData.putFloat(0, sourceX);
                  uniformData.putFloat(4, sourceY);
                  uniformData.putFloat(8, sourceW);
                  uniformData.putFloat(12, sourceH);
                  uniformData.putFloat(16, offsetScale / (float)Math.max(sourceWidth, 1));
                  uniformData.putFloat(20, offsetScale / (float)Math.max(sourceHeight, 1));
                  uniformData.putFloat(32, skyFallbackRed);
                  uniformData.putFloat(36, skyFallbackGreen);
                  uniformData.putFloat(40, skyFallbackBlue);
                  uniformData.putFloat(44, 1.0F);
                  uniformData.position(0);
                  GpuBuffer uniformBuffer = this.ensureKawaseParamsBuffer();
                  if (uniformBuffer == null) {
                     break label71;
                  }

                  encoder.writeToBuffer(uniformBuffer.slice(0L, 48L), uniformData);
                  RenderPass renderPass = encoder.createRenderPass(
                     () -> "daamky_kawase_blur", target.getColorTextureView(), OptionalInt.of(ColorUtil.TRANSPARENT), null, OptionalDouble.empty()
                  );

                  try {
                     renderPass.setPipeline(pipeline);
                     renderPass.bindTexture("Sampler0", source, sampler);
                     renderPass.setUniform("KawaseParams", uniformBuffer);
                     renderPass.setVertexBuffer(0, fullscreenQuad);
                     renderPass.setIndexBuffer(indices, indexBuffer.type());
                     renderPass.drawIndexed(0, 0, 6, 1);
                  } catch (Throwable var24) {
                     if (renderPass != null) {
                        try {
                           renderPass.close();
                        } catch (Throwable var23) {
                           var24.addSuppressed(var23);
                        }
                     }

                     throw var24;
                  }

                  if (renderPass != null) {
                     renderPass.close();
                  }
               } catch (Throwable var25) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var22) {
                        var25.addSuppressed(var22);
                     }
                  }

                  throw var25;
               }

               if (stack != null) {
                  stack.close();
               }

               return;
            }

            if (stack != null) {
               stack.close();
            }
         }
      }
   }

   private GpuBuffer ensureRegionBuffer() {
      if (this.regionDirty) {
         this.closeRegionBuffer();
         MemoryStack stack = MemoryStack.stackPush();

         try {
            ByteBuffer data = this.buildRegionData(stack);
            this.regionBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_blur_region", 136, data);
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

         this.regionDirty = false;
         return this.regionBuffer;
      } else {
         if (this.regionBuffer == null || this.regionBuffer.isClosed() || this.regionBuffer.size() < 16L) {
            this.closeRegionBuffer();
            this.regionBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_blur_region", 136, 16L);
         }

         return this.regionBuffer;
      }
   }

   private void uploadRegionBuffer() {
      GpuBuffer buffer = this.ensureWritableRegionBuffer();
      if (buffer != null) {
         try {
            MemoryStack stack = MemoryStack.stackPush();

            try {
               ByteBuffer data = this.buildRegionData(stack);
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(0L, 16L), data);
               this.regionDirty = false;
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
         }
      }
   }

   private GpuBuffer ensureWritableRegionBuffer() {
      if (this.regionBuffer != null && !this.regionBuffer.isClosed() && this.regionBuffer.size() >= 16L) {
         return this.regionBuffer;
      } else {
         this.closeRegionBuffer();

         try {
            this.regionBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_blur_region", 136, 16L);
            return this.regionBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildRegionData(MemoryStack stack) {
      ByteBuffer data = stack.calloc(16);
      data.putFloat(0, (float)this.regionX);
      data.putFloat(4, (float)this.regionY);
      data.putFloat(8, (float)this.regionWidth);
      data.putFloat(12, (float)this.regionHeight);
      data.position(0);
      return data;
   }

   private GpuBuffer ensureKawaseParamsBuffer() {
      if (this.kawaseParamsBuffer == null || this.kawaseParamsBuffer.isClosed() || this.kawaseParamsBuffer.size() < 48L) {
         this.closeKawaseParamsBuffer();
         this.kawaseParamsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_kawase_params", 136, 48L);
      }

      return this.kawaseParamsBuffer;
   }

   private GpuBuffer ensureFullscreenQuadBuffer() {
      if (this.fullscreenQuadBuffer != null) {
         return this.fullscreenQuadBuffer;
      } else {
         ByteBufferBuilder allocator = new ByteBufferBuilder(4 * DefaultVertexFormat.POSITION.getVertexSize());

         GpuBuffer var3;
         try {
            MeshData meshData = this.buildFullscreenQuad(allocator);

            try {
               this.fullscreenQuadBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_blur_fullscreen_quad", 32, meshData.vertexBuffer());
               var3 = this.fullscreenQuadBuffer;
            } catch (Throwable var7) {
               if (meshData != null) {
                  try {
                     meshData.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (meshData != null) {
               meshData.close();
            }
         } catch (Throwable var8) {
            try {
               allocator.close();
            } catch (Throwable var5) {
               var8.addSuppressed(var5);
            }

            throw var8;
         }

         allocator.close();
         return var3;
      }
   }

   private MeshData buildFullscreenQuad(ByteBufferBuilder allocator) {
      BufferBuilder builder = new BufferBuilder(allocator, Mode.QUADS, DefaultVertexFormat.POSITION);
      builder.addVertex(-1.0F, -1.0F, 0.0F);
      builder.addVertex(-1.0F, 1.0F, 0.0F);
      builder.addVertex(1.0F, 1.0F, 0.0F);
      builder.addVertex(1.0F, -1.0F, 0.0F);
      return builder.buildOrThrow();
   }

   private BuiltBlur normalize(BuiltBlur blur) {
      float maxRadius = Math.max(0.0F, Math.min(blur.width(), blur.height()) * 0.5F);
      float radiusTopLeft = clamp(blur.radiusTopLeft(), 0.0F, maxRadius);
      float radiusTopRight = clamp(blur.radiusTopRight(), 0.0F, maxRadius);
      float radiusBottomRight = clamp(blur.radiusBottomRight(), 0.0F, maxRadius);
      float radiusBottomLeft = clamp(blur.radiusBottomLeft(), 0.0F, maxRadius);
      float smoothness = Math.max(0.0F, blur.smoothness());
      float blurRadius = Math.max(0.0F, blur.blurRadius());
      return radiusTopLeft == blur.radiusTopLeft()
            && radiusTopRight == blur.radiusTopRight()
            && radiusBottomRight == blur.radiusBottomRight()
            && radiusBottomLeft == blur.radiusBottomLeft()
            && smoothness == blur.smoothness()
            && blurRadius == blur.blurRadius()
         ? blur
         : new BuiltBlur(
            blur.x(),
            blur.y(),
            blur.width(),
            blur.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            smoothness,
            blurRadius,
            blur.color()
         );
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static int clampInt(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private void closeParamsBuffer() {
      if (this.paramsBuffer != null) {
         this.paramsBuffer.close();
         this.paramsBuffer = null;
      }
   }

   private void closeRegionBuffer() {
      if (this.regionBuffer != null) {
         this.regionBuffer.close();
         this.regionBuffer = null;
      }
   }

   private void closeKawaseParamsBuffer() {
      if (this.kawaseParamsBuffer != null) {
         this.kawaseParamsBuffer.close();
         this.kawaseParamsBuffer = null;
      }
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("daamky", path);
   }

   @Override
   public void close() {
      this.pendingBlurs.clear();
      this.preparedBlurs.clear();
      this.activeGraphics = null;
      this.captureRequested = false;
      this.blurTextureSetup = TextureSetup.noTexture();
      this.closeParamsBuffer();
      this.closeRegionBuffer();
      this.closeKawaseParamsBuffer();
      if (this.blurTarget != null) {
         this.blurTarget.destroyBuffers();
         this.blurTarget = null;
      }

      for (int i = 0; i < 4; i++) {
         if (this.downTargets[i] != null) {
            this.downTargets[i].destroyBuffers();
            this.downTargets[i] = null;
         }

         if (this.upTargets[i] != null) {
            this.upTargets[i].destroyBuffers();
            this.upTargets[i] = null;
         }
      }

      if (this.fullscreenQuadBuffer != null) {
         this.fullscreenQuadBuffer.close();
         this.fullscreenQuadBuffer = null;
      }
   }

   private static record PendingBlur(BuiltBlur blur, Matrix3x2f pose, ScreenRectangle scissorArea) {
   }
}
