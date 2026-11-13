package daamky.ua.utils.render.post.customfog;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
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
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import daamky.ua.utils.render.RenderSampler;

public final class CustomFogRenderer {
   private static final int UNIFORM_SIZE = 64;
   private static final Identifier PIPELINE_ID = Identifier.of("daamky", "pipeline/post/custom_fog");
   private static final Identifier VERTEX_SHADER = Identifier.of("daamky", "post/custom_fog/custom_fog");
   private static final Identifier FRAGMENT_SHADER = Identifier.of("daamky", "post/custom_fog/custom_fog");
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuTexture sceneCopyTexture;
   private static GpuTextureView sceneCopyTextureView;
   private static GpuTexture depthCopyTexture;
   private static GpuTextureView depthCopyTextureView;
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static boolean disabledAfterError;
   private static float time = 0.0F;

   private CustomFogRenderer() {
   }

   public static boolean isDisabledAfterError() {
      return disabledAfterError;
   }

   public static void clear() {
      closeTextures();
   }

   public static void apply(
      Framebuffer renderTarget, int fogColor, float density, float waveSpeed, float waveScale, float waveStrength, float glowIntensity, String weatherMode
   ) {
      if (!disabledAfterError && renderTarget != null && renderTarget.getColorAttachmentView() != null && renderTarget.getDepthAttachment() != null) {
         if (renderTarget.textureWidth > 0 && renderTarget.textureHeight > 0) {
            init();
            if (pipeline != null && uniformBuffer != null) {
               if (ensureTextures(renderTarget.textureWidth, renderTarget.textureHeight)) {
                  try {
                     CommandEncoder copyEncoder = RenderSystem.getDevice().createCommandEncoder();
                     copyEncoder.copyTextureToTexture(
                        renderTarget.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, renderTarget.textureWidth, renderTarget.textureHeight
                     );
                     copyEncoder.copyTextureToTexture(
                        renderTarget.getDepthAttachment(), depthCopyTexture, 0, 0, 0, 0, 0, renderTarget.textureWidth, renderTarget.textureHeight
                     );
                     time += 0.016F;
                     if (time > 10000.0F) {
                        time = 0.0F;
                     }

                     applyShader(
                        renderTarget.getColorAttachmentView(),
                        sceneCopyTextureView,
                        depthCopyTextureView,
                        fogColor,
                        density,
                        waveSpeed,
                        waveScale,
                        waveStrength,
                        glowIntensity,
                        weatherMode
                     );
                  } catch (Throwable var9) {
                     disabledAfterError = true;
                     var9.printStackTrace();
                     closeTextures();
                     closeUniformBuffer();
                  }
               }
            }
         }
      }
   }

   private static void init() {
      if (!disabledAfterError) {
         try {
            if (pipeline == null) {
               pipeline = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[0])
                     .withLocation(PIPELINE_ID)
                     .withVertexShader(VERTEX_SHADER)
                     .withFragmentShader(FRAGMENT_SHADER)
                     .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                     .withUniform("CustomFogData", UniformType.UNIFORM_BUFFER)
                     .withSampler("SceneSampler")
                     .withSampler("DepthSampler")
                     .withBlend(BlendFunction.TRANSLUCENT)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (uniformBuffer == null || uniformBuffer.isClosed() || uniformBuffer.size() < 64L) {
               closeUniformBuffer();
               uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:custom_fog_uniforms", 136, 64L);
            }
         } catch (Throwable var1) {
            disabledAfterError = true;
            var1.printStackTrace();
            pipeline = null;
            closeUniformBuffer();
         }
      }
   }

   private static boolean ensureTextures(int width, int height) {
      if (sceneCopyTexture != null
         && sceneCopyTextureView != null
         && depthCopyTexture != null
         && depthCopyTextureView != null
         && lastWidth == width
         && lastHeight == height) {
         return true;
      } else {
         closeTextures();
         sceneCopyTexture = RenderSystem.getDevice().createTexture(() -> "daamky:custom_fog_scene_copy", 5, TextureFormat.RGBA8, width, height, 1, 1);
         sceneCopyTextureView = RenderSystem.getDevice().createTextureView(sceneCopyTexture);
         depthCopyTexture = RenderSystem.getDevice().createTexture(() -> "daamky:custom_fog_depth_copy", 5, TextureFormat.DEPTH32, width, height, 1, 1);
         depthCopyTextureView = RenderSystem.getDevice().createTextureView(depthCopyTexture);
         lastWidth = width;
         lastHeight = height;
         return true;
      }
   }

   private static void applyShader(
      GpuTextureView target,
      GpuTextureView scene,
      GpuTextureView depth,
      int fogColor,
      float density,
      float waveSpeed,
      float waveScale,
      float waveStrength,
      float glowIntensity,
      String weatherMode
   ) {
      if (target != null && scene != null && depth != null) {
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         writeUniforms(encoder, fogColor, density, waveSpeed, waveScale, waveStrength, glowIntensity, weatherMode);
         RenderPass renderPass = encoder.createRenderPass(() -> "daamky:custom_fog", target, OptionalInt.empty());

         try {
            renderPass.setPipeline(pipeline);
            renderPass.setUniform("CustomFogData", uniformBuffer);
            renderPass.bindTexture("SceneSampler", scene, RenderSampler.linear());
            renderPass.bindTexture("DepthSampler", depth, RenderSampler.nearest());
            renderPass.draw(0, 6);
         } catch (Throwable var15) {
            if (renderPass != null) {
               try {
                  renderPass.close();
               } catch (Throwable var14) {
                  var15.addSuppressed(var14);
               }
            }

            throw var15;
         }

         if (renderPass != null) {
            renderPass.close();
         }
      }
   }

   private static void writeUniforms(
      CommandEncoder encoder, int fogColor, float density, float waveSpeed, float waveScale, float waveStrength, float glowIntensity, String weatherMode
   ) {
      float farPlane = viewDistance();
      float red = (float)(fogColor >> 16 & 0xFF) / 255.0F;
      float green = (float)(fogColor >> 8 & 0xFF) / 255.0F;
      float blue = (float)(fogColor & 0xFF) / 255.0F;
      float alpha = (float)(fogColor >> 24 & 0xFF) / 255.0F;
      if (alpha < 0.01F) {
         alpha = 1.0F;
      }

      float weatherType = 0.0F;
      float rainIntensity = 0.0F;
      float thunderFlash = 0.0F;
      if (weatherMode != null) {
         switch (weatherMode) {
            case "Rain":
               weatherType = 1.0F;
               rainIntensity = 0.8F;
               break;
            case "Thunder":
               weatherType = 2.0F;
               rainIntensity = 1.0F;
               if (Math.random() < 0.05) {
                  thunderFlash = (float)(0.5 + Math.random() * 0.5);
               }
               break;
            case "Snow":
               weatherType = 3.0F;
               break;
            default:
               weatherType = 0.0F;
         }
      }

      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.calloc(64);
         data.putFloat(0, red);
         data.putFloat(4, green);
         data.putFloat(8, blue);
         data.putFloat(12, alpha);
         data.putFloat(16, 0.05F);
         data.putFloat(20, farPlane);
         data.putFloat(24, density);
         data.putFloat(28, time);
         data.putFloat(32, waveSpeed);
         data.putFloat(36, waveScale);
         data.putFloat(40, waveStrength);
         data.putFloat(44, glowIntensity);
         data.putFloat(48, weatherType);
         data.putFloat(52, rainIntensity);
         data.putFloat(56, thunderFlash);
         data.putFloat(60, 0.0F);
         data.position(0);
         encoder.writeToBuffer(uniformBuffer.slice(0L, 64L), data);
      } catch (Throwable var20) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var19) {
               var20.addSuppressed(var19);
            }
         }

         throw var20;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static float viewDistance() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      return minecraft != null && minecraft.options != null
         ? Math.max(96.0F, (float)((Integer)minecraft.options.getViewDistance().getValue() + 1) * 16.0F)
         : 192.0F;
   }

   private static void closeTextures() {
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

      lastWidth = -1;
      lastHeight = -1;
   }

   private static void closeUniformBuffer() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }
   }
}
