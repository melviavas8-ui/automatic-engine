package daamky.ua.utils.render.sky;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class CustomSkyRenderer {
   private static final int UNIFORM_SIZE = 128;
   private static final int SKY_DATA_SIZE = 48;
   private static final int VERTEX_COUNT = 36;
   private static final int VERTEX_SIZE = 12;
   private static final float SKY_SIZE = 100.0F;
   private static final Identifier PIPELINE_ID = Identifier.of("daamky", "pipeline/world/ambience_sky");
   private static final Identifier VERTEX_SHADER = Identifier.of("mre", "core/ambience_sky");
   private static final Identifier FRAGMENT_SHADER = Identifier.of("mre", "core/ambience_sky");
   private static final Matrix4f projectionMatrix = new Matrix4f();
   private static final Matrix4f viewMatrix = new Matrix4f();
   private static final Matrix4f skyViewMatrix = new Matrix4f();
   private static final Matrix4f combinedMatrix = new Matrix4f();
   private static final Matrix4f identityMatrix = new Matrix4f();
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer skyDataBuffer;
   private static GpuBuffer vertexBuffer;
   private static boolean disabledAfterError;

   private CustomSkyRenderer() {
   }

   public static boolean isDisabledAfterError() {
      return disabledAfterError;
   }

   public static void setMatrices(Matrix4f projection, Matrix4f view) {
      if (projection != null) {
         projectionMatrix.set(projection);
      }

      if (view != null) {
         viewMatrix.set(view);
      }
   }

   public static void renderCustomSky(Framebuffer renderTarget, int skyColor, float speed, float intensity, int mode) {
      if (!disabledAfterError && renderTarget != null && renderTarget.getColorAttachmentView() != null) {
         if (renderTarget.textureWidth > 0 && renderTarget.textureHeight > 0) {
            init();
            if (pipeline != null && uniformBuffer != null && skyDataBuffer != null && vertexBuffer != null) {
               try {
                  renderSky(renderTarget, skyColor, speed, intensity, mode);
               } catch (Throwable var6) {
                  disabledAfterError = true;
                  var6.printStackTrace();
                  closeBuffers();
               }
            }
         }
      }
   }

   private static void init() {
      if (!disabledAfterError) {
         try {
            if (pipeline == null) {
               VertexFormat format = VertexFormat.builder().add("inPosition", VertexFormatElement.POSITION).build();
               pipeline = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[0])
                     .withLocation(PIPELINE_ID)
                     .withVertexShader(VERTEX_SHADER)
                     .withFragmentShader(FRAGMENT_SHADER)
                     .withVertexFormat(format, DrawMode.TRIANGLES)
                     .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                     .withUniform("SkyData", UniformType.UNIFORM_BUFFER)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (uniformBuffer == null || uniformBuffer.isClosed() || uniformBuffer.size() < 128L) {
               closeUniformBuffer();
               uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:ambience_sky_uniforms", 136, 128L);
            }

            if (skyDataBuffer == null || skyDataBuffer.isClosed() || skyDataBuffer.size() < 48L) {
               closeSkyDataBuffer();
               skyDataBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:ambience_sky_data", 136, 48L);
            }

            if (vertexBuffer == null || vertexBuffer.isClosed() || vertexBuffer.size() < 432L) {
               closeVertexBuffer();
               vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:ambience_sky_vertices", 40, 432L);
               uploadSkyCube();
            }
         } catch (Throwable var1) {
            disabledAfterError = true;
            var1.printStackTrace();
            pipeline = null;
            closeBuffers();
         }
      }
   }

   private static void renderSky(Framebuffer target, int skyColor, float speed, float intensity, int mode) {
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      writeUniforms(encoder);
      writeSkyData(encoder, skyColor, speed, intensity, mode);
      RenderPass renderPass = encoder.createRenderPass(() -> "daamky:block_overlay", target.getColorAttachmentView(), OptionalInt.empty());

      try {
         renderPass.setPipeline(pipeline);
         renderPass.setUniform("Uniforms", uniformBuffer);
         renderPass.setUniform("SkyData", skyDataBuffer);
         renderPass.setVertexBuffer(0, vertexBuffer);
         renderPass.draw(0, 36);
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
   }

   private static void writeUniforms(CommandEncoder encoder) {
      skyViewMatrix.set(viewMatrix);
      skyViewMatrix.m30(0.0F);
      skyViewMatrix.m31(0.0F);
      skyViewMatrix.m32(0.0F);
      Matrix4f combined = combinedMatrix.set(projectionMatrix).mul(skyViewMatrix);
      ByteBuffer uniformData = MemoryUtil.memAlloc(128);

      try {
         putMatrix(uniformData, combined);
         putMatrix(uniformData, identityMatrix);
         uniformData.flip();
         encoder.writeToBuffer(uniformBuffer.slice(0L, 128L), uniformData);
      } finally {
         MemoryUtil.memFree(uniformData);
      }
   }

   private static void writeSkyData(CommandEncoder encoder, int skyColor, float speed, float intensity, int mode) {
      float red = (float)(skyColor >> 16 & 0xFF) / 255.0F;
      float green = (float)(skyColor >> 8 & 0xFF) / 255.0F;
      float blue = (float)(skyColor & 0xFF) / 255.0F;
      float alpha = (float)(skyColor >> 24 & 0xFF) / 255.0F;
      if (alpha < 0.01F) {
         alpha = 1.0F;
      }

      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.calloc(48);
         data.putFloat(0, red);
         data.putFloat(4, green);
         data.putFloat(8, blue);
         data.putFloat(12, alpha);
         data.putFloat(16, (float)(System.currentTimeMillis() & 4194303L) / 1000.0F);
         data.putFloat(20, (float)Math.clamp((long)mode, 0, 5));
         data.putFloat(24, Math.clamp(intensity, 0.0F, 2.5F));
         data.putFloat(28, Math.max(0.01F, speed));
         data.putFloat(32, 0.0F);
         data.putFloat(36, 0.0F);
         data.putFloat(40, 0.0F);
         data.putFloat(44, 0.0F);
         data.position(0);
         encoder.writeToBuffer(skyDataBuffer.slice(0L, 48L), data);
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

   private static void uploadSkyCube() {
      ByteBuffer vertexData = MemoryUtil.memAlloc(432);

      try {
         float n = -100.0F;
         float p = 100.0F;
         quad(vertexData, n, n, n, n, p, n, p, p, n, p, n, n);
         quad(vertexData, p, n, p, p, p, p, n, p, p, n, n, p);
         quad(vertexData, n, n, p, n, p, p, n, p, n, n, n, n);
         quad(vertexData, p, n, n, p, p, n, p, p, p, p, n, p);
         quad(vertexData, n, p, n, n, p, p, p, p, p, p, p, n);
         quad(vertexData, n, n, p, n, n, n, p, n, n, p, n, p);
         vertexData.flip();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(vertexBuffer.slice(0L, 432L), vertexData);
      } finally {
         MemoryUtil.memFree(vertexData);
      }
   }

   private static void quad(
      ByteBuffer buffer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4
   ) {
      vertex(buffer, x1, y1, z1);
      vertex(buffer, x2, y2, z2);
      vertex(buffer, x3, y3, z3);
      vertex(buffer, x1, y1, z1);
      vertex(buffer, x3, y3, z3);
      vertex(buffer, x4, y4, z4);
   }

   private static void vertex(ByteBuffer buffer, float x, float y, float z) {
      buffer.putFloat(x).putFloat(y).putFloat(z);
   }

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }

   private static void closeBuffers() {
      closeUniformBuffer();
      closeSkyDataBuffer();
      closeVertexBuffer();
   }

   private static void closeUniformBuffer() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }
   }

   private static void closeSkyDataBuffer() {
      if (skyDataBuffer != null) {
         skyDataBuffer.close();
         skyDataBuffer = null;
      }
   }

   private static void closeVertexBuffer() {
      if (vertexBuffer != null) {
         vertexBuffer.close();
         vertexBuffer = null;
      }
   }
}
