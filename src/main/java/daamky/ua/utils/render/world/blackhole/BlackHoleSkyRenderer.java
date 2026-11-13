package daamky.ua.utils.render.world.blackhole;

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
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import daamky.ua.utils.render.RenderSampler;

public final class BlackHoleSkyRenderer {
   private static final int UNIFORM_SIZE = 192;
   private static final Identifier PIPELINE_ID = Identifier.of("daamky", "pipeline/world/blackhole");
   private static final Identifier VERTEX_SHADER = Identifier.of("daamky", "world/blackhole/blackhole");
   private static final Identifier FRAGMENT_SHADER = Identifier.of("daamky", "world/blackhole/blackhole");
   private static final float SKY_DEPTH_THRESHOLD = 0.9995F;
   private static final long START_NANOS = System.nanoTime();
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuTexture depthCopyTexture;
   private static GpuTextureView depthCopyTextureView;
   private static int depthWidth = -1;
   private static int depthHeight = -1;
   private static boolean disabledAfterError;

   private BlackHoleSkyRenderer() {
   }

   public static boolean isDisabledAfterError() {
      return disabledAfterError;
   }

   public static void clear() {
      closeDepthCopy();
   }

   public static void render(Framebuffer renderTarget, Matrix4fc projectionMatrix, Matrix4fc viewMatrix, BlackHoleSettings settings) {
      if (!disabledAfterError && renderTarget != null && settings != null) {
         if (projectionMatrix != null && viewMatrix != null) {
            if (renderTarget.getColorAttachmentView() != null && renderTarget.getDepthAttachment() != null) {
               if (renderTarget.textureWidth > 0 && renderTarget.textureHeight > 0) {
                  init();
                  if (pipeline != null && uniformBuffer != null) {
                     if (ensureDepthCopy(renderTarget.textureWidth, renderTarget.textureHeight)) {
                        try {
                           CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                           encoder.copyTextureToTexture(
                              renderTarget.getDepthAttachment(), depthCopyTexture, 0, 0, 0, 0, 0, renderTarget.textureWidth, renderTarget.textureHeight
                           );
                           Matrix4f inverseViewProjection = new Matrix4f(projectionMatrix).mul(viewMatrix).invert();
                           writeUniform(encoder, inverseViewProjection, settings);
                           RenderPass renderPass = encoder.createRenderPass(
                              () -> "daamky:black_hole_sky", renderTarget.getColorAttachmentView(), OptionalInt.empty()
                           );

                           try {
                              renderPass.setPipeline(pipeline);
                              renderPass.setUniform("BlackHoleData", uniformBuffer);
                              renderPass.bindTexture("DepthSampler", depthCopyTextureView, RenderSampler.nearest());
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
                           disabledAfterError = true;
                           var11.printStackTrace();
                           closeDepthCopy();
                           closeUniformBuffer();
                        }
                     }
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
                     .withUniform("BlackHoleData", UniformType.UNIFORM_BUFFER)
                     .withSampler("DepthSampler")
                     .withBlend(BlendFunction.TRANSLUCENT)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (uniformBuffer == null || uniformBuffer.isClosed() || uniformBuffer.size() < 192L) {
               closeUniformBuffer();
               uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:black_hole_uniforms", 136, 192L);
            }
         } catch (Throwable var1) {
            disabledAfterError = true;
            var1.printStackTrace();
            pipeline = null;
            closeUniformBuffer();
         }
      }
   }

   private static boolean ensureDepthCopy(int width, int height) {
      GpuDevice device = RenderSystem.tryGetDevice();
      if (device == null) {
         return false;
      } else if (depthCopyTexture != null && depthWidth == width && depthHeight == height) {
         return true;
      } else {
         closeDepthCopy();
         depthCopyTexture = device.createTexture(() -> "daamky:black_hole_depth_copy", 5, TextureFormat.DEPTH32, width, height, 1, 1);
         depthCopyTextureView = device.createTextureView(depthCopyTexture);
         depthWidth = width;
         depthHeight = height;
         return true;
      }
   }

   private static void writeUniform(CommandEncoder encoder, Matrix4fc inverseViewProjection, BlackHoleSettings settings) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.calloc(192);
         inverseViewProjection.get(0, data);
         data.putFloat(64, settings.directionX());
         data.putFloat(68, settings.directionY());
         data.putFloat(72, settings.directionZ());
         data.putFloat(76, settings.cameraDistance());
         data.putFloat(80, settings.hotRed());
         data.putFloat(84, settings.hotGreen());
         data.putFloat(88, settings.hotBlue());
         data.putFloat(92, settings.innerRadius());
         data.putFloat(96, settings.coldRed());
         data.putFloat(100, settings.coldGreen());
         data.putFloat(104, settings.coldBlue());
         data.putFloat(108, settings.outerRadius());
         data.putFloat(112, settings.haloRed());
         data.putFloat(116, settings.haloGreen());
         data.putFloat(120, settings.haloBlue());
         data.putFloat(124, settings.haloStrength());
         data.putFloat(128, settings.spaceRed());
         data.putFloat(132, settings.spaceGreen());
         data.putFloat(136, settings.spaceBlue());
         data.putFloat(140, settings.stars());
         data.putFloat(144, currentTime());
         data.putFloat(148, settings.spinSpeed());
         data.putFloat(152, settings.turbulence());
         data.putFloat(156, settings.inclination());
         data.putFloat(160, settings.opacity());
         data.putFloat(164, settings.steps());
         data.putFloat(168, settings.brightness());
         data.putFloat(172, settings.doppler());
         data.putFloat(176, 0.9995F);
         data.putFloat(180, settings.diskSoftness());
         data.putFloat(184, settings.lensing());
         data.putFloat(188, settings.exposure());
         data.position(0);
         encoder.writeToBuffer(uniformBuffer.slice(0L, 192L), data);
      } catch (Throwable var7) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static float currentTime() {
      return (float)((double)(System.nanoTime() - START_NANOS) / 1.0E9);
   }

   private static void closeDepthCopy() {
      if (depthCopyTextureView != null) {
         depthCopyTextureView.close();
         depthCopyTextureView = null;
      }

      if (depthCopyTexture != null) {
         depthCopyTexture.close();
         depthCopyTexture = null;
      }

      depthWidth = -1;
      depthHeight = -1;
   }

   private static void closeUniformBuffer() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }
   }
}
