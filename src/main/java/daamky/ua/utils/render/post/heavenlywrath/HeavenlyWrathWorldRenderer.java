package daamky.ua.utils.render.post.heavenlywrath;

import com.mojang.blaze3d.buffers.GpuBuffer;
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
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import daamky.ua.utils.render.RenderSampler;

public final class HeavenlyWrathWorldRenderer {
   private static final int UNIFORM_SIZE = 112;
   private static final float SKY_DEPTH_THRESHOLD = 0.9995F;
   private static final long START_NANOS = System.nanoTime();
   private static final Identifier PIPELINE_ID = Identifier.of("daamky", "pipeline/post/heavenly_wrath_world");
   private static final Identifier VERTEX_SHADER = Identifier.of("daamky", "post/heavenly_wrath_world/heavenly_wrath_world");
   private static final Identifier FRAGMENT_SHADER = Identifier.of("daamky", "post/heavenly_wrath_world/heavenly_wrath_world");
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuTexture sceneCopyTexture;
   private static GpuTextureView sceneCopyTextureView;
   private static GpuTexture depthCopyTexture;
   private static GpuTextureView depthCopyTextureView;
   private static int textureWidth = -1;
   private static int textureHeight = -1;
   private static boolean disabledAfterError;

   private HeavenlyWrathWorldRenderer() {
   }

   public static boolean isDisabledAfterError() {
      return disabledAfterError;
   }

   public static void clear() {
      closeTextures();
   }

   public static void apply(
      Framebuffer target,
      Matrix4fc projectionMatrix,
      Matrix4fc viewMatrix,
      Vec3d cameraPosition,
      int skyColor,
      float speed,
      float intensity,
      float shadowStrength
   ) {
      if (!disabledAfterError && target != null && projectionMatrix != null && viewMatrix != null) {
         if (target.getColorAttachment() != null
            && target.getDepthAttachment() != null
            && target.getColorAttachmentView() != null
            && target.textureWidth > 0
            && target.textureHeight > 0) {
            init();
            if (pipeline != null && uniformBuffer != null && ensureTextures(target.textureWidth, target.textureHeight)) {
               try {
                  CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                  encoder.copyTextureToTexture(target.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
                  encoder.copyTextureToTexture(target.getDepthAttachment(), depthCopyTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
                  Matrix4f inverseViewProjection = new Matrix4f(projectionMatrix).mul(viewMatrix).invert();
                  writeUniforms(
                     encoder, inverseViewProjection, cameraPosition != null ? cameraPosition : Vec3d.ZERO, skyColor, speed, intensity, shadowStrength
                  );
                  RenderPass renderPass = encoder.createRenderPass(() -> "daamky:heavenly_wrath_world", target.getColorAttachmentView(), OptionalInt.empty());

                  try {
                     renderPass.setPipeline(pipeline);
                     renderPass.setUniform("HeavenlyWorldData", uniformBuffer);
                     renderPass.bindTexture("SceneSampler", sceneCopyTextureView, RenderSampler.linear());
                     renderPass.bindTexture("DepthSampler", depthCopyTextureView, RenderSampler.nearest());
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
                  disabledAfterError = true;
                  var15.printStackTrace();
                  closeTextures();
                  closeUniformBuffer();
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
                     .withUniform("HeavenlyWorldData", UniformType.UNIFORM_BUFFER)
                     .withSampler("SceneSampler")
                     .withSampler("DepthSampler")
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (uniformBuffer == null || uniformBuffer.isClosed() || uniformBuffer.size() < 112L) {
               closeUniformBuffer();
               uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:heavenly_wrath_world_uniforms", 136, 112L);
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
      GpuDevice device = RenderSystem.tryGetDevice();
      if (device == null) {
         return false;
      } else if (sceneCopyTexture != null && depthCopyTexture != null && textureWidth == width && textureHeight == height) {
         return true;
      } else {
         closeTextures();
         sceneCopyTexture = device.createTexture(() -> "daamky:heavenly_wrath_world_scene", 5, TextureFormat.RGBA8, width, height, 1, 1);
         sceneCopyTextureView = device.createTextureView(sceneCopyTexture);
         depthCopyTexture = device.createTexture(() -> "daamky:heavenly_wrath_world_depth", 5, TextureFormat.DEPTH32, width, height, 1, 1);
         depthCopyTextureView = device.createTextureView(depthCopyTexture);
         textureWidth = width;
         textureHeight = height;
         return true;
      }
   }

   private static void writeUniforms(
      CommandEncoder encoder, Matrix4fc inverseViewProjection, Vec3d cameraPosition, int skyColor, float speed, float intensity, float shadowStrength
   ) {
      float red = (float)(skyColor >> 16 & 0xFF) / 255.0F;
      float green = (float)(skyColor >> 8 & 0xFF) / 255.0F;
      float blue = (float)(skyColor & 0xFF) / 255.0F;
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.calloc(112);
         inverseViewProjection.get(0, data);
         data.putFloat(64, (float)cameraPosition.x);
         data.putFloat(68, (float)cameraPosition.y);
         data.putFloat(72, (float)cameraPosition.z);
         data.putFloat(76, currentTime());
         data.putFloat(80, Math.max(0.01F, speed));
         data.putFloat(84, Math.clamp(intensity, 0.0F, 2.5F));
         data.putFloat(88, Math.clamp(shadowStrength, 0.0F, 1.0F));
         data.putFloat(92, 0.9995F);
         data.putFloat(96, red);
         data.putFloat(100, green);
         data.putFloat(104, blue);
         data.putFloat(108, 1.0F);
         data.position(0);
         encoder.writeToBuffer(uniformBuffer.slice(0L, 112L), data);
      } catch (Throwable var14) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var13) {
               var14.addSuppressed(var13);
            }
         }

         throw var14;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static float currentTime() {
      return (float)((double)(System.nanoTime() - START_NANOS) / 1.0E9);
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

      textureWidth = -1;
      textureHeight = -1;
   }

   private static void closeUniformBuffer() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }
   }
}
