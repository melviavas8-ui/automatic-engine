package daamky.ua.utils.render.menu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

public final class MenuShaderBackgroundRenderer {
   private static final int UNIFORM_SIZE = 32;
   private static final Identifier PIPELINE_ID = Identifier.fromNamespaceAndPath("daamky", "pipeline/menu/background");
   private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath("daamky", "menu/menu_background");
   private static final Identifier FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("daamky", "menu/menu_background");
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static boolean disabledAfterError;

   private MenuShaderBackgroundRenderer() {
   }

   public static boolean isDisabledAfterError() {
      return disabledAfterError;
   }

   public static void render(RenderTarget target, int color, float intensity) {
      if (!disabledAfterError && target != null && target.getColorTextureView() != null) {
         if (target.width > 0 && target.height > 0) {
            init();
            if (pipeline != null && uniformBuffer != null) {
               try {
                  CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                  writeUniforms(encoder, target.width, target.height, color, intensity);
                  RenderPass pass = encoder.createRenderPass(() -> "daamky:menu_background", target.getColorTextureView(), OptionalInt.empty());

                  try {
                     pass.setPipeline(pipeline);
                     pass.setUniform("MenuData", uniformBuffer);
                     pass.draw(0, 6);
                  } catch (Throwable var8) {
                     if (pass != null) {
                        try {
                           pass.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (pass != null) {
                     pass.close();
                  }
               } catch (Throwable var9) {
                  disabledAfterError = true;
                  var9.printStackTrace();
                  close();
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
                     .withVertexFormat(DefaultVertexFormat.EMPTY, Mode.TRIANGLES)
                     .withUniform("MenuData", UniformType.UNIFORM_BUFFER)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (uniformBuffer == null || uniformBuffer.isClosed() || uniformBuffer.size() < 32L) {
               close();
               uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:menu_background_uniforms", 136, 32L);
            }
         } catch (Throwable var1) {
            disabledAfterError = true;
            var1.printStackTrace();
            close();
         }
      }
   }

   private static void writeUniforms(CommandEncoder encoder, int width, int height, int color, float intensity) {
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      float time = (float)(System.currentTimeMillis() % 10000000L) / 1000.0F;
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.calloc(32);
         data.putFloat(0, (float)width);
         data.putFloat(4, (float)height);
         data.putFloat(8, time);
         data.putFloat(12, Math.max(0.0F, intensity));
         data.putFloat(16, red);
         data.putFloat(20, green);
         data.putFloat(24, blue);
         data.putFloat(28, 1.0F);
         encoder.writeToBuffer(uniformBuffer.slice(0L, 32L), data);
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

   public static void reload() {
      disabledAfterError = false;
      pipeline = null;
      close();
   }

   private static void close() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }
   }
}
