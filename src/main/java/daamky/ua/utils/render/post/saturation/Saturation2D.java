package daamky.ua.utils.render.post.saturation;

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
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;
import daamky.ua.utils.render.RenderSampler;

public class Saturation2D {
   private static final Identifier PIPELINE_ID = Identifier.of("daamky", "pipeline/post/saturation");
   private static final Identifier VERTEX_SHADER = Identifier.of("daamky", "post/saturation/saturation");
   private static final Identifier FRAGMENT_SHADER = Identifier.of("daamky", "post/saturation/saturation");
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation(PIPELINE_ID)
         .withVertexShader(VERTEX_SHADER)
         .withFragmentShader(FRAGMENT_SHADER)
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("SaturationData", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final ByteBuffer DATA_BUFFER = MemoryUtil.memAlloc(16);
   private static final GpuBuffer DUMMY_VERTEX_BUFFER;
   private static final GpuBuffer UNIFORM_BUFFER = RenderSystem.getDevice().createBuffer(() -> "daamky:saturation_uniform", 136, 16L);
   private static GpuTexture tempTexture;
   private static GpuTextureView tempTextureView;
   private static int lastWidth;
   private static int lastHeight;
   private static boolean disabledAfterError;

   public Saturation2D() {
   }

   private static void ensureTextures(int width, int height) {
      if (tempTexture == null || width != lastWidth || height != lastHeight) {
         if (tempTextureView != null) {
            tempTextureView.close();
         }

         if (tempTexture != null) {
            tempTexture.close();
         }

         tempTexture = RenderSystem.getDevice().createTexture(() -> "daamky:saturation_temp", 13, TextureFormat.RGBA8, width, height, 1, 1);
         tempTextureView = RenderSystem.getDevice().createTextureView(tempTexture);
         lastWidth = width;
         lastHeight = height;
      }
   }

   public static void applyWithCopy(float saturation) {
      if (!disabledAfterError) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.getFramebuffer() != null) {
            if (Float.isFinite(saturation)) {
               float clampedSaturation = Math.clamp(saturation, 0.0F, 2.0F);
               if (!(Math.abs(clampedSaturation - 1.0F) <= 5.0E-4F)) {
                  int width = client.getFramebuffer().textureWidth;
                  int height = client.getFramebuffer().textureHeight;
                  if (width > 0 && height > 0) {
                     ensureTextures(width, height);

                     try {
                        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                        encoder.copyTextureToTexture(client.getFramebuffer().getColorAttachment(), tempTexture, 0, 0, 0, 0, 0, width, height);
                        apply(client.getFramebuffer().getColorAttachmentView(), tempTextureView, clampedSaturation);
                     } catch (Throwable var6) {
                        disabledAfterError = true;
                     }
                  }
               }
            }
         }
      }
   }

   private static void apply(GpuTextureView targetView, GpuTextureView sourceView, float saturation) {
      DATA_BUFFER.clear();
      DATA_BUFFER.putFloat(saturation);
      DATA_BUFFER.flip();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(UNIFORM_BUFFER.slice(), DATA_BUFFER);
      RenderPass renderPass = encoder.createRenderPass(() -> "daamky:saturation_pass", targetView, OptionalInt.empty());

      try {
         renderPass.setPipeline(PIPELINE);
         renderPass.setVertexBuffer(0, DUMMY_VERTEX_BUFFER);
         renderPass.bindTexture("Sampler0", sourceView, RenderSampler.linear());
         renderPass.setUniform("SaturationData", UNIFORM_BUFFER.slice());
         renderPass.draw(0, 6);
      } catch (Throwable var8) {
         if (renderPass != null) {
            try {
               renderPass.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (renderPass != null) {
         renderPass.close();
      }
   }

   static {
      ByteBuffer dummyData = MemoryUtil.memAlloc(4);
      dummyData.putInt(0);
      dummyData.flip();
      DUMMY_VERTEX_BUFFER = RenderSystem.getDevice().createBuffer(() -> "daamky:saturation_dummy_vertex", 40, dummyData);
      MemoryUtil.memFree(dummyData);
   }
}
