package daamky.ua.utils.render.hands;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import daamky.ua.utils.render.RenderSampler;
import daamky.ua.utils.render.color.ColorUtil;

public final class GlassHandsRenderer {
   private static final int MAX_BLUR_ITERATIONS = 5;
   private static final int KAWASE_UNIFORM_SIZE = 16;
   private static final int GLASS_UNIFORM_SIZE = 48;
   private static final int MASK_UNIFORM_SIZE = 16;
   private static final Identifier FULLSCREEN_SHADER = Identifier.of("daamky", "effects/glass_hands/fullscreen");
   private static final Identifier KAWASE_SHADER = Identifier.of("daamky", "effects/glass_hands/kawase");
   private static final Identifier MASK_SHADER = Identifier.of("daamky", "effects/glass_hands/mask_diff");
   private static final Identifier COMPOSITE_SHADER = Identifier.of("daamky", "effects/glass_hands/composite");
   private static final BlendFunction REPLACE_BLEND = new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ONE, DestFactor.ZERO);
   private static RenderPipeline kawasePipeline;
   private static RenderPipeline maskPipeline;
   private static RenderPipeline compositePipeline;
   private static GpuBuffer kawaseUniformBuffer;
   private static GpuBuffer maskUniformBuffer;
   private static GpuBuffer compositeUniformBuffer;
   private static GpuTexture sceneBeforeTexture;
   private static GpuTexture sceneAfterTexture;
   private static GpuTexture depthBeforeTexture;
   private static GpuTexture depthAfterTexture;
   private static GpuTexture maskTexture;
   private static GpuTextureView sceneBeforeTextureView;
   private static GpuTextureView sceneAfterTextureView;
   private static GpuTextureView depthBeforeTextureView;
   private static GpuTextureView depthAfterTextureView;
   private static GpuTextureView maskTextureView;
   private static final SimpleFramebuffer[] downTargets = new SimpleFramebuffer[5];
   private static final SimpleFramebuffer[] upTargets = new SimpleFramebuffer[5];
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static int allocatedPasses;
   private static boolean enabled;
   private static boolean capturedBeforeHands;
   private static boolean disabledAfterError;
   private static float blurRadius = 2.5F;
   private static int blurIterations = 3;
   private static float saturation;
   private static boolean reflect = true;
   private static int tintColor;
   private static float tintIntensity;
   private static float edgeGlowIntensity = 0.2F;

   private GlassHandsRenderer() {
   }

   public static void setEnabled(boolean enabled) {
      GlassHandsRenderer.enabled = enabled;
      if (!enabled) {
         capturedBeforeHands = false;
      }
   }

   public static void configure(
      float blurRadius, int blurIterations, float saturation, boolean reflect, int tintColor, float tintIntensity, float edgeGlowIntensity
   ) {
      GlassHandsRenderer.blurRadius = clamp(blurRadius, 1.0F, 5.0F);
      GlassHandsRenderer.blurIterations = Math.clamp((long)blurIterations, 1, 5);
      GlassHandsRenderer.saturation = clamp(saturation, 0.0F, 2.0F);
      GlassHandsRenderer.reflect = reflect;
      GlassHandsRenderer.tintColor = tintColor;
      GlassHandsRenderer.tintIntensity = clamp(tintIntensity, 0.0F, 0.5F);
      GlassHandsRenderer.edgeGlowIntensity = clamp(edgeGlowIntensity, 0.0F, 1.0F);
   }

   public static void captureBeforeHandRender() {
      capturedBeforeHands = false;
      if (shouldRender()) {
         Framebuffer target = MinecraftClient.getInstance().getFramebuffer();
         if (isUsable(target)) {
            init();
            if (ensureTargets(target.textureWidth, target.textureHeight, blurIterations)) {
               try {
                  CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                  encoder.copyTextureToTexture(target.getColorAttachment(), sceneBeforeTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
                  encoder.copyTextureToTexture(target.getDepthAttachment(), depthBeforeTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
                  capturedBeforeHands = true;
               } catch (Throwable var2) {
                  disableAfterError(var2);
               }
            }
         }
      }
   }

   public static boolean hasCapturedHands() {
      return capturedBeforeHands;
   }

   public static void renderCapturedGlass() {
      if (capturedBeforeHands) {
         capturedBeforeHands = false;
         if (shouldRender()) {
            Framebuffer target = MinecraftClient.getInstance().getFramebuffer();
            if (isUsable(target) && ensureTargets(target.textureWidth, target.textureHeight, blurIterations)) {
               try {
                  CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                  encoder.copyTextureToTexture(target.getColorAttachment(), sceneAfterTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
                  encoder.copyTextureToTexture(target.getDepthAttachment(), depthAfterTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
                  createMask(target.textureWidth, target.textureHeight);
                  GpuTextureView blurred = renderBlurChain(sceneBeforeTextureView, target.textureWidth, target.textureHeight, blurIterations, blurRadius);
                  composite(target.getColorAttachmentView(), sceneBeforeTextureView, blurred, maskTextureView, target.textureWidth, target.textureHeight);
               } catch (Throwable var3) {
                  disableAfterError(var3);
               }
            }
         }
      }
   }

   public static void shutdown() {
      closeTargets();
      closeBuffer(kawaseUniformBuffer);
      closeBuffer(maskUniformBuffer);
      closeBuffer(compositeUniformBuffer);
      kawaseUniformBuffer = null;
      maskUniformBuffer = null;
      compositeUniformBuffer = null;
      kawasePipeline = null;
      maskPipeline = null;
      compositePipeline = null;
      capturedBeforeHands = false;
   }

   private static boolean shouldRender() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      return enabled && !disabledAfterError && minecraft != null && minecraft.player != null && minecraft.world != null;
   }

   private static void init() {
      if (!disabledAfterError) {
         try {
            if (kawasePipeline == null) {
               kawasePipeline = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[0])
                     .withLocation(Identifier.of("daamky", "pipeline/effects/glass_hands/kawase"))
                     .withVertexShader(KAWASE_SHADER)
                     .withFragmentShader(KAWASE_SHADER)
                     .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                     .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
                     .withSampler("Sampler0")
                     .withBlend(BlendFunction.TRANSLUCENT)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (maskPipeline == null) {
               maskPipeline = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[0])
                     .withLocation(Identifier.of("daamky", "pipeline/effects/glass_hands/mask_diff"))
                     .withVertexShader(FULLSCREEN_SHADER)
                     .withFragmentShader(MASK_SHADER)
                     .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                     .withUniform("MaskData", UniformType.UNIFORM_BUFFER)
                     .withSampler("BeforeSampler")
                     .withSampler("AfterSampler")
                     .withSampler("DepthBeforeSampler")
                     .withSampler("DepthAfterSampler")
                     .withBlend(REPLACE_BLEND)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (compositePipeline == null) {
               compositePipeline = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[0])
                     .withLocation(Identifier.of("daamky", "pipeline/effects/glass_hands/composite"))
                     .withVertexShader(COMPOSITE_SHADER)
                     .withFragmentShader(COMPOSITE_SHADER)
                     .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                     .withUniform("GlassData", UniformType.UNIFORM_BUFFER)
                     .withSampler("SceneSampler")
                     .withSampler("BlurSampler")
                     .withSampler("MaskSampler")
                     .withBlend(REPLACE_BLEND)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            kawaseUniformBuffer = ensureBuffer(kawaseUniformBuffer, "daamky:glass_hands_kawase_uniform", 16);
            maskUniformBuffer = ensureBuffer(maskUniformBuffer, "daamky:glass_hands_mask_uniform", 16);
            compositeUniformBuffer = ensureBuffer(compositeUniformBuffer, "daamky:glass_hands_composite_uniform", 48);
         } catch (Throwable var1) {
            disableAfterError(var1);
         }
      }
   }

   private static boolean ensureTargets(int width, int height, int passes) {
      GpuDevice device = RenderSystem.tryGetDevice();
      if (device == null) {
         return false;
      } else if (sceneBeforeTexture != null && width == lastWidth && height == lastHeight && allocatedPasses >= passes) {
         return true;
      } else {
         closeTargets();
         sceneBeforeTexture = createTexture(device, "daamky:glass_hands_scene_before", width, height, TextureFormat.RGBA8);
         sceneAfterTexture = createTexture(device, "daamky:glass_hands_scene_after", width, height, TextureFormat.RGBA8);
         depthBeforeTexture = createTexture(device, "daamky:glass_hands_depth_before", width, height, TextureFormat.DEPTH32);
         depthAfterTexture = createTexture(device, "daamky:glass_hands_depth_after", width, height, TextureFormat.DEPTH32);
         maskTexture = createTexture(device, "daamky:glass_hands_mask", width, height, TextureFormat.RGBA8);
         sceneBeforeTextureView = device.createTextureView(sceneBeforeTexture);
         sceneAfterTextureView = device.createTextureView(sceneAfterTexture);
         depthBeforeTextureView = device.createTextureView(depthBeforeTexture);
         depthAfterTextureView = device.createTextureView(depthAfterTexture);
         maskTextureView = device.createTextureView(maskTexture);
         int currentWidth = width;
         int currentHeight = height;

         for (int i = 0; i < passes; i++) {
            currentWidth = Math.max(1, currentWidth / 2);
            currentHeight = Math.max(1, currentHeight / 2);
            downTargets[i] = ensureTarget(downTargets[i], "daamky_glass_hands_down_" + i, currentWidth, currentHeight);
            upTargets[i] = ensureTarget(upTargets[i], "daamky_glass_hands_up_" + i, currentWidth, currentHeight);
         }

         lastWidth = width;
         lastHeight = height;
         allocatedPasses = passes;
         return sceneBeforeTextureView != null
            && sceneAfterTextureView != null
            && depthBeforeTextureView != null
            && depthAfterTextureView != null
            && maskTextureView != null;
      }
   }

   private static GpuTexture createTexture(GpuDevice device, String name, int width, int height, TextureFormat format) {
      return device.createTexture(() -> name, 13, format, width, height, 1, 1);
   }

   private static SimpleFramebuffer ensureTarget(SimpleFramebuffer target, String name, int width, int height) {
      if (target == null) {
         return new SimpleFramebuffer(name, width, height, false);
      } else {
         if (target.textureWidth != width || target.textureHeight != height) {
            target.resize(width, height);
         }

         return target;
      }
   }

   private static GpuTextureView renderBlurChain(GpuTextureView source, int sourceWidth, int sourceHeight, int passes, float offset) {
      if (source == null) {
         return null;
      } else {
         GpuSampler sampler = RenderSampler.linear();
         GpuTextureView currentSource = source;
         int currentWidth = sourceWidth;
         int currentHeight = sourceHeight;

         for (int i = 0; i < passes; i++) {
            SimpleFramebuffer target = downTargets[i];
            renderKawasePass(currentSource, currentWidth, currentHeight, target, offset, sampler);
            currentSource = target.getColorAttachmentView();
            currentWidth = target.textureWidth;
            currentHeight = target.textureHeight;
         }

         for (int i = passes - 2; i >= 0; i--) {
            SimpleFramebuffer target = upTargets[i];
            renderKawasePass(currentSource, currentWidth, currentHeight, target, offset, sampler);
            currentSource = target.getColorAttachmentView();
            currentWidth = target.textureWidth;
            currentHeight = target.textureHeight;
         }

         return currentSource;
      }
   }

   private static void renderKawasePass(GpuTextureView source, int sourceWidth, int sourceHeight, SimpleFramebuffer target, float offset, GpuSampler sampler) {
      if (source != null && target != null && target.getColorAttachmentView() != null) {
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         MemoryStack stack = MemoryStack.stackPush();

         try {
            ByteBuffer data = stack.calloc(16);
            data.putFloat(0, (float)sourceWidth);
            data.putFloat(4, (float)sourceHeight);
            data.putFloat(8, offset);
            data.putFloat(12, 0.0F);
            data.position(0);
            encoder.writeToBuffer(kawaseUniformBuffer.slice(0L, 16L), data);
            RenderPass renderPass = encoder.createRenderPass(
               () -> "daamky:glass_hands_kawase", target.getColorAttachmentView(), OptionalInt.of(ColorUtil.TRANSPARENT)
            );

            try {
               renderPass.setPipeline(kawasePipeline);
               renderPass.setUniform("KawaseData", kawaseUniformBuffer);
               renderPass.bindTexture("Sampler0", source, sampler);
               renderPass.draw(0, 6);
            } catch (Throwable var14) {
               if (renderPass != null) {
                  try {
                     renderPass.close();
                  } catch (Throwable var13) {
                     var14.addSuppressed(var13);
                  }
               }

               throw var14;
            }

            if (renderPass != null) {
               renderPass.close();
            }
         } catch (Throwable var15) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var12) {
                  var15.addSuppressed(var12);
               }
            }

            throw var15;
         }

         if (stack != null) {
            stack.close();
         }
      }
   }

   private static void createMask(int width, int height) {
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.calloc(16);
         data.putFloat(0, (float)width);
         data.putFloat(4, (float)height);
         data.position(0);
         encoder.writeToBuffer(maskUniformBuffer.slice(0L, 16L), data);
         RenderPass renderPass = encoder.createRenderPass(() -> "daamky:glass_hands_mask", maskTextureView, OptionalInt.of(ColorUtil.TRANSPARENT));

         try {
            renderPass.setPipeline(maskPipeline);
            renderPass.setUniform("MaskData", maskUniformBuffer);
            renderPass.bindTexture("BeforeSampler", sceneBeforeTextureView, RenderSampler.linear());
            renderPass.bindTexture("AfterSampler", sceneAfterTextureView, RenderSampler.linear());
            renderPass.bindTexture("DepthBeforeSampler", depthBeforeTextureView, RenderSampler.nearest());
            renderPass.bindTexture("DepthAfterSampler", depthAfterTextureView, RenderSampler.nearest());
            renderPass.draw(0, 6);
         } catch (Throwable var10) {
            if (renderPass != null) {
               try {
                  renderPass.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (renderPass != null) {
            renderPass.close();
         }
      } catch (Throwable var11) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }
         }

         throw var11;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static void composite(GpuTextureView target, GpuTextureView scene, GpuTextureView blur, GpuTextureView mask, int width, int height) {
      if (target != null && scene != null && blur != null && mask != null) {
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         MemoryStack stack = MemoryStack.stackPush();

         try {
            ByteBuffer data = stack.calloc(48);
            data.putFloat(0, (float)width);
            data.putFloat(4, (float)height);
            data.putFloat(8, saturation);
            data.putFloat(12, reflect ? 1.0F : 0.0F);
            data.putFloat(16, (float)(tintColor >> 16 & 0xFF) / 255.0F);
            data.putFloat(20, (float)(tintColor >> 8 & 0xFF) / 255.0F);
            data.putFloat(24, (float)(tintColor & 0xFF) / 255.0F);
            data.putFloat(28, (float)(tintColor >> 24 & 0xFF) / 255.0F);
            data.putFloat(32, tintIntensity);
            data.putFloat(36, edgeGlowIntensity);
            data.position(0);
            encoder.writeToBuffer(compositeUniformBuffer.slice(0L, 48L), data);
            RenderPass renderPass = encoder.createRenderPass(() -> "daamky:glass_hands_composite", target, OptionalInt.empty());

            try {
               renderPass.setPipeline(compositePipeline);
               renderPass.setUniform("GlassData", compositeUniformBuffer);
               renderPass.bindTexture("SceneSampler", scene, RenderSampler.linear());
               renderPass.bindTexture("BlurSampler", blur, RenderSampler.linear());
               renderPass.bindTexture("MaskSampler", mask, RenderSampler.nearest());
               renderPass.draw(0, 6);
            } catch (Throwable var14) {
               if (renderPass != null) {
                  try {
                     renderPass.close();
                  } catch (Throwable var13) {
                     var14.addSuppressed(var13);
                  }
               }

               throw var14;
            }

            if (renderPass != null) {
               renderPass.close();
            }
         } catch (Throwable var15) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var12) {
                  var15.addSuppressed(var12);
               }
            }

            throw var15;
         }

         if (stack != null) {
            stack.close();
         }
      }
   }

   private static boolean isUsable(Framebuffer target) {
      return target != null
         && target.getColorAttachment() != null
         && target.getColorAttachmentView() != null
         && target.getDepthAttachment() != null
         && target.textureWidth > 0
         && target.textureHeight > 0;
   }

   private static GpuBuffer ensureBuffer(GpuBuffer buffer, String name, int size) {
      if (buffer != null && !buffer.isClosed() && buffer.size() >= (long)size) {
         return buffer;
      } else {
         closeBuffer(buffer);
         return RenderSystem.getDevice().createBuffer(() -> name, 136, (long)size);
      }
   }

   private static void closeBuffer(GpuBuffer buffer) {
      if (buffer != null) {
         buffer.close();
      }
   }

   private static void closeTargets() {
      closeTextureView(sceneBeforeTextureView);
      closeTextureView(sceneAfterTextureView);
      closeTextureView(depthBeforeTextureView);
      closeTextureView(depthAfterTextureView);
      closeTextureView(maskTextureView);
      closeTexture(sceneBeforeTexture);
      closeTexture(sceneAfterTexture);
      closeTexture(depthBeforeTexture);
      closeTexture(depthAfterTexture);
      closeTexture(maskTexture);
      sceneBeforeTextureView = null;
      sceneAfterTextureView = null;
      depthBeforeTextureView = null;
      depthAfterTextureView = null;
      maskTextureView = null;
      sceneBeforeTexture = null;
      sceneAfterTexture = null;
      depthBeforeTexture = null;
      depthAfterTexture = null;
      maskTexture = null;

      for (int i = 0; i < 5; i++) {
         if (downTargets[i] != null) {
            downTargets[i].delete();
            downTargets[i] = null;
         }

         if (upTargets[i] != null) {
            upTargets[i].delete();
            upTargets[i] = null;
         }
      }

      lastWidth = -1;
      lastHeight = -1;
      allocatedPasses = 0;
   }

   private static void closeTextureView(GpuTextureView view) {
      if (view != null) {
         view.close();
      }
   }

   private static void closeTexture(GpuTexture texture) {
      if (texture != null) {
         texture.close();
      }
   }

   private static void disableAfterError(Throwable throwable) {
      disabledAfterError = true;
      throwable.printStackTrace();
      shutdown();
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
