package daamky.ua.utils.render.post.fogblur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
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

public final class FogBlurRenderer {
   private static final int COMPOSITE_UNIFORM_SIZE = 48;
   private static final int KAWASE_UNIFORM_SIZE = 48;
   private static final int MAX_BLUR_ITERATIONS = 4;
   private static final Identifier KAWASE_DOWN_PIPELINE_ID = Identifier.of("daamky", "pipeline/post/fogblur/kawase_down");
   private static final Identifier KAWASE_UP_PIPELINE_ID = Identifier.of("daamky", "pipeline/post/fogblur/kawase_up");
   private static final Identifier COMPOSITE_PIPELINE_ID = Identifier.of("daamky", "pipeline/post/fogblur/composite");
   private static final Identifier KAWASE_VERTEX = Identifier.of("daamky", "post/fogblur/kawase");
   private static final Identifier KAWASE_DOWN_SHADER = Identifier.of("daamky", "ui/kawase/down");
   private static final Identifier KAWASE_UP_SHADER = Identifier.of("daamky", "ui/kawase/up");
   private static final Identifier COMPOSITE_SHADER = Identifier.of("daamky", "post/fogblur/composite");
   private static float fallbackRed = 0.55F;
   private static float fallbackGreen = 0.65F;
   private static float fallbackBlue = 0.78F;
   private static RenderPipeline kawaseDownPipeline;
   private static RenderPipeline kawaseUpPipeline;
   private static RenderPipeline compositePipeline;
   private static GpuBuffer compositeUniformBuffer;
   private static GpuBuffer kawaseUniformBuffer;
   private static GpuTexture sceneCopyTexture;
   private static GpuTextureView sceneCopyTextureView;
   private static GpuTexture depthCopyTexture;
   private static GpuTextureView depthCopyTextureView;
   private static final SimpleFramebuffer[] downTargets = new SimpleFramebuffer[4];
   private static final SimpleFramebuffer[] upTargets = new SimpleFramebuffer[4];
   private static int sceneWidth = -1;
   private static int sceneHeight = -1;
   private static int blurWidth = -1;
   private static int blurHeight = -1;
   private static int allocatedPasses;
   private static boolean disabledAfterError;

   private FogBlurRenderer() {
   }

   public static boolean isDisabledAfterError() {
      return disabledAfterError;
   }

   public static GpuTextureView getGuiSourceTextureView(int width, int height) {
      if (disabledAfterError || sceneCopyTextureView == null) {
         return null;
      } else {
         return sceneWidth == width && sceneHeight == height ? sceneCopyTextureView : null;
      }
   }

   public static void setFallbackColor(float red, float green, float blue) {
      if (Float.isFinite(red) && Float.isFinite(green) && Float.isFinite(blue)) {
         fallbackRed = clamp(red, 0.0F, 1.0F);
         fallbackGreen = clamp(green, 0.0F, 1.0F);
         fallbackBlue = clamp(blue, 0.0F, 1.0F);
      }
   }

   public static void clear() {
      closeTargets();
   }

   public static void apply(Framebuffer renderTarget, float strength, float distanceBlocks, float opacityPercent, float renderScale, int maxPasses) {
      if (!disabledAfterError
         && renderTarget != null
         && renderTarget.getColorAttachment() != null
         && renderTarget.getColorAttachmentView() != null
         && renderTarget.getDepthAttachment() != null) {
         if (renderTarget.textureWidth > 0 && renderTarget.textureHeight > 0) {
            init();
            if (kawaseDownPipeline != null
               && kawaseUpPipeline != null
               && compositePipeline != null
               && compositeUniformBuffer != null
               && kawaseUniformBuffer != null) {
               int passes = computePasses(strength, maxPasses);
               if (ensureTargets(renderTarget.textureWidth, renderTarget.textureHeight, renderScale, passes)) {
                  try {
                     CommandEncoder copyEncoder = RenderSystem.getDevice().createCommandEncoder();
                     copyEncoder.copyTextureToTexture(
                        renderTarget.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, renderTarget.textureWidth, renderTarget.textureHeight
                     );
                     copyEncoder.copyTextureToTexture(
                        renderTarget.getDepthAttachment(), depthCopyTexture, 0, 0, 0, 0, 0, renderTarget.textureWidth, renderTarget.textureHeight
                     );
                     float offsetScale = clamp(0.85F + strength * 0.06F, 0.85F, 2.0F);
                     GpuTextureView blurred = renderBlurChain(sceneCopyTextureView, renderTarget.textureWidth, renderTarget.textureHeight, passes, offsetScale);
                     float farPlane = viewDistance();
                     float maxStartDistance = Math.max(1.0F, farPlane * 0.96F);
                     float startDistance = clamp(distanceBlocks, 1.0F, maxStartDistance);
                     float fadeDistance = clamp(32.0F + strength * 1.8F, 36.0F, 72.0F);
                     float minThreshold = clamp(startDistance / farPlane, 0.0F, 0.995F);
                     float maxThreshold = clamp((startDistance + fadeDistance) / farPlane, minThreshold + 0.002F, 1.0F);
                     float opacity = clamp(opacityPercent / 100.0F, 0.0F, 1.0F);
                     composite(
                        renderTarget.getColorAttachmentView(),
                        sceneCopyTextureView,
                        blurred,
                        depthCopyTextureView,
                        opacity,
                        farPlane,
                        minThreshold,
                        maxThreshold
                     );
                  } catch (Throwable var17) {
                     disabledAfterError = true;
                     var17.printStackTrace();
                     closeTargets();
                     closeCompositeBuffer();
                     closeKawaseBuffer();
                  }
               }
            }
         }
      }
   }

   private static void init() {
      if (!disabledAfterError) {
         try {
            if (kawaseDownPipeline == null) {
               kawaseDownPipeline = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[0])
                     .withLocation(KAWASE_DOWN_PIPELINE_ID)
                     .withVertexShader(KAWASE_VERTEX)
                     .withFragmentShader(KAWASE_DOWN_SHADER)
                     .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                     .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
                     .withSampler("Sampler0")
                     .withBlend(BlendFunction.TRANSLUCENT)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (kawaseUpPipeline == null) {
               kawaseUpPipeline = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[0])
                     .withLocation(KAWASE_UP_PIPELINE_ID)
                     .withVertexShader(KAWASE_VERTEX)
                     .withFragmentShader(KAWASE_UP_SHADER)
                     .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                     .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
                     .withSampler("Sampler0")
                     .withBlend(BlendFunction.TRANSLUCENT)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (compositePipeline == null) {
               compositePipeline = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[0])
                     .withLocation(COMPOSITE_PIPELINE_ID)
                     .withVertexShader(Identifier.of("daamky", "post/fogblur/fogblur"))
                     .withFragmentShader(COMPOSITE_SHADER)
                     .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                     .withUniform("FogBlurData", UniformType.UNIFORM_BUFFER)
                     .withSampler("SceneSampler")
                     .withSampler("BlurSampler")
                     .withSampler("DepthSampler")
                     .withBlend(BlendFunction.TRANSLUCENT)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (compositeUniformBuffer == null || compositeUniformBuffer.isClosed() || compositeUniformBuffer.size() < 48L) {
               closeCompositeBuffer();
               compositeUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:fog_blur_uniforms", 136, 48L);
            }

            if (kawaseUniformBuffer == null || kawaseUniformBuffer.isClosed() || kawaseUniformBuffer.size() < 48L) {
               closeKawaseBuffer();
               kawaseUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:fog_blur_kawase_uniforms", 136, 48L);
            }
         } catch (Throwable var1) {
            disabledAfterError = true;
            var1.printStackTrace();
            kawaseDownPipeline = null;
            kawaseUpPipeline = null;
            compositePipeline = null;
            closeCompositeBuffer();
            closeKawaseBuffer();
         }
      }
   }

   private static boolean ensureTargets(int width, int height, float renderScale, int passes) {
      GpuDevice device = RenderSystem.tryGetDevice();
      if (device == null) {
         return false;
      } else {
         int targetWidth = Math.max(1, Math.round((float)width * clamp(renderScale, 0.25F, 0.75F)));
         int targetHeight = Math.max(1, Math.round((float)height * clamp(renderScale, 0.25F, 0.75F)));
         if (sceneCopyTexture != null
            && depthCopyTexture != null
            && sceneWidth == width
            && sceneHeight == height
            && blurWidth == targetWidth
            && blurHeight == targetHeight
            && allocatedPasses >= passes) {
            return true;
         } else {
            closeTargets();
            sceneCopyTexture = device.createTexture(() -> "daamky:fog_blur_scene_copy", 5, TextureFormat.RGBA8, width, height, 1, 1);
            sceneCopyTextureView = device.createTextureView(sceneCopyTexture);
            depthCopyTexture = device.createTexture(() -> "daamky:fog_blur_depth_copy", 5, TextureFormat.DEPTH32, width, height, 1, 1);
            depthCopyTextureView = device.createTextureView(depthCopyTexture);
            int currentWidth = targetWidth;
            int currentHeight = targetHeight;

            for (int i = 0; i < passes; i++) {
               downTargets[i] = ensureTarget(downTargets[i], "daamky_fog_blur_down_" + i, currentWidth, currentHeight);
               if (i < passes - 1) {
                  upTargets[i] = ensureTarget(upTargets[i], "daamky_fog_blur_up_" + i, currentWidth, currentHeight);
               }

               currentWidth = Math.max(currentWidth / 2, 1);
               currentHeight = Math.max(currentHeight / 2, 1);
            }

            sceneWidth = width;
            sceneHeight = height;
            blurWidth = targetWidth;
            blurHeight = targetHeight;
            allocatedPasses = passes;
            return true;
         }
      }
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

   private static GpuTextureView renderBlurChain(GpuTextureView source, int sourceWidth, int sourceHeight, int passes, float offsetScale) {
      if (source == null) {
         return null;
      } else {
         GpuTextureView currentSource = source;
         int currentWidth = sourceWidth;
         int currentHeight = sourceHeight;
         GpuSampler sampler = RenderSampler.linear();

         for (int i = 0; i < passes; i++) {
            SimpleFramebuffer target = downTargets[i];
            renderKawasePass(kawaseDownPipeline, currentSource, currentWidth, currentHeight, target, offsetScale, 0.0F, 0.0F, 1.0F, 1.0F, sampler);
            currentSource = target.getColorAttachmentView();
            currentWidth = target.textureWidth;
            currentHeight = target.textureHeight;
         }

         for (int i = passes - 2; i >= 0; i--) {
            SimpleFramebuffer target = upTargets[i];
            renderKawasePass(kawaseUpPipeline, currentSource, currentWidth, currentHeight, target, offsetScale, 0.0F, 0.0F, 1.0F, 1.0F, sampler);
            currentSource = target.getColorAttachmentView();
            currentWidth = target.textureWidth;
            currentHeight = target.textureHeight;
         }

         return currentSource;
      }
   }

   private static void renderKawasePass(
      RenderPipeline pipeline,
      GpuTextureView source,
      int sourceWidth,
      int sourceHeight,
      SimpleFramebuffer target,
      float offsetScale,
      float sourceX,
      float sourceY,
      float sourceW,
      float sourceH,
      GpuSampler sampler
   ) {
      if (source != null && target != null && target.getColorAttachmentView() != null) {
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         MemoryStack stack = MemoryStack.stackPush();

         try {
            ByteBuffer data = stack.calloc(48);
            data.putFloat(0, sourceX);
            data.putFloat(4, sourceY);
            data.putFloat(8, sourceW);
            data.putFloat(12, sourceH);
            data.putFloat(16, offsetScale / (float)Math.max(sourceWidth, 1));
            data.putFloat(20, offsetScale / (float)Math.max(sourceHeight, 1));
            data.putFloat(32, fallbackRed);
            data.putFloat(36, fallbackGreen);
            data.putFloat(40, fallbackBlue);
            data.putFloat(44, 1.0F);
            data.position(0);
            encoder.writeToBuffer(kawaseUniformBuffer.slice(0L, 48L), data);
            RenderPass renderPass = encoder.createRenderPass(
               () -> "daamky:fog_blur_kawase", target.getColorAttachmentView(), OptionalInt.of(ColorUtil.TRANSPARENT)
            );

            try {
               renderPass.setPipeline(pipeline);
               renderPass.bindTexture("Sampler0", source, sampler);
               renderPass.setUniform("KawaseParams", kawaseUniformBuffer);
               renderPass.draw(0, 6);
            } catch (Throwable var19) {
               if (renderPass != null) {
                  try {
                     renderPass.close();
                  } catch (Throwable var18) {
                     var19.addSuppressed(var18);
                  }
               }

               throw var19;
            }

            if (renderPass != null) {
               renderPass.close();
            }
         } catch (Throwable var20) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var17) {
                  var20.addSuppressed(var17);
               }
            }

            throw var20;
         }

         if (stack != null) {
            stack.close();
         }
      }
   }

   private static void composite(
      GpuTextureView target,
      GpuTextureView scene,
      GpuTextureView blur,
      GpuTextureView depth,
      float opacity,
      float farPlane,
      float minThreshold,
      float maxThreshold
   ) {
      if (target != null && scene != null && blur != null && depth != null && !(opacity <= 0.0F)) {
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         writeCompositeUniform(encoder, 0.0F, 0.0F, 0.0F, opacity, 0.05F, farPlane, minThreshold, maxThreshold);
         RenderPass renderPass = encoder.createRenderPass(() -> "daamky:fog_blur_composite", target, OptionalInt.empty());

         try {
            renderPass.setPipeline(compositePipeline);
            renderPass.setUniform("FogBlurData", compositeUniformBuffer);
            renderPass.bindTexture("SceneSampler", scene, RenderSampler.linear());
            renderPass.bindTexture("BlurSampler", blur, RenderSampler.linear());
            renderPass.bindTexture("DepthSampler", depth, RenderSampler.nearest());
            renderPass.draw(0, 6);
         } catch (Throwable var13) {
            if (renderPass != null) {
               try {
                  renderPass.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }
            }

            throw var13;
         }

         if (renderPass != null) {
            renderPass.close();
         }
      }
   }

   private static void writeCompositeUniform(
      CommandEncoder encoder, float texelX, float texelY, float offset, float opacity, float nearPlane, float farPlane, float minThreshold, float maxThreshold
   ) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.calloc(48);
         data.putFloat(0, texelX);
         data.putFloat(4, texelY);
         data.putFloat(8, offset);
         data.putFloat(12, opacity);
         data.putFloat(16, nearPlane);
         data.putFloat(20, farPlane);
         data.putFloat(24, minThreshold);
         data.putFloat(28, maxThreshold);
         data.putFloat(32, 0.0F);
         data.putFloat(36, 0.0F);
         data.putFloat(40, 0.0F);
         data.putFloat(44, 0.0F);
         data.position(0);
         encoder.writeToBuffer(compositeUniformBuffer.slice(0L, 48L), data);
      } catch (Throwable var13) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }
         }

         throw var13;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static int computePasses(float strength, int maxPasses) {
      int requested;
      if (strength >= 14.0F) {
         requested = 4;
      } else if (strength >= 8.0F) {
         requested = 3;
      } else if (strength >= 3.0F) {
         requested = 2;
      } else {
         requested = 1;
      }

      return Math.clamp((long)requested, 1, Math.max(1, maxPasses));
   }

   private static float viewDistance() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      return minecraft != null && minecraft.options != null
         ? Math.max(96.0F, (float)((Integer)minecraft.options.getViewDistance().getValue() + 1) * 16.0F)
         : 192.0F;
   }

   private static void closeTargets() {
      if (sceneCopyTextureView != null) {
         sceneCopyTextureView.close();
         sceneCopyTextureView = null;
      }

      if (sceneCopyTexture != null) {
         sceneCopyTexture.close();
         sceneCopyTexture = null;
      }

      if (depthCopyTextureView != null) {
         depthCopyTextureView.close();
         depthCopyTextureView = null;
      }

      if (depthCopyTexture != null) {
         depthCopyTexture.close();
         depthCopyTexture = null;
      }

      for (int i = 0; i < 4; i++) {
         if (downTargets[i] != null) {
            downTargets[i].delete();
            downTargets[i] = null;
         }

         if (upTargets[i] != null) {
            upTargets[i].delete();
            upTargets[i] = null;
         }
      }

      sceneWidth = -1;
      sceneHeight = -1;
      blurWidth = -1;
      blurHeight = -1;
      allocatedPasses = 0;
   }

   private static void closeCompositeBuffer() {
      if (compositeUniformBuffer != null) {
         compositeUniformBuffer.close();
         compositeUniformBuffer = null;
      }
   }

   private static void closeKawaseBuffer() {
      if (kawaseUniformBuffer != null) {
         kawaseUniformBuffer.close();
         kawaseUniformBuffer = null;
      }
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
