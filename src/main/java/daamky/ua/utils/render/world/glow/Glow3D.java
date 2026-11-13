package daamky.ua.utils.render.world.glow;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.Camera;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class Glow3D {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static final int UNIFORM_SIZE = 128;
   private static final int MAX_GLOWS = 512;
   private static final int VERTEX_SIZE = 24;
   private static final int GLOW_STRIDE = 9;
   private static final float[] QUAD_UVS = new float[]{-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F};
   private static final float[] glows = new float[4608];
   private static int glowCount;
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer vertexBuffer;
   private static final Matrix4f projectionMatrix = new Matrix4f();
   private static final Matrix4f viewMatrix = new Matrix4f();
   private static final Matrix4f combinedMatrix = new Matrix4f();
   private static final Matrix4f viewInvMatrix = new Matrix4f();
   private static boolean ignoreDepth;

   private Glow3D() {
   }

   public static void init() {
      if (pipeline == null) {
         VertexFormat format = VertexFormat.builder()
            .add("inPosition", VertexFormatElement.POSITION)
            .add("inColor", VertexFormatElement.COLOR)
            .add("inUV", VertexFormatElement.UV)
            .build();
         pipeline = RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.of("daamky", "pipeline/world/glow3d"))
            .withVertexShader(Identifier.of("daamky", "world/glow3d/glow3d"))
            .withFragmentShader(Identifier.of("daamky", "world/glow3d/glow3d"))
            .withVertexFormat(format, DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Glow3D Uniforms", 136, 128L);
         vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Glow3D Vertices", 40, 73728L);
      }
   }

   public static void setMatrices(Matrix4f projection, Matrix4f view) {
      projectionMatrix.set(projection);
      viewMatrix.set(view);
   }

   public static Vec3d getCameraPos() {
      Camera camera = MC.gameRenderer.getCamera();
      return camera.getCameraPos();
   }

   public static void begin() {
      begin(false);
   }

   public static void begin(boolean ignore) {
      if (pipeline == null) {
         init();
      }

      glowCount = 0;
      ignoreDepth = ignore;
   }

   public static void glow(double x, double y, double z, float size, int color, float alpha) {
      glow(x, y, z, size, color, alpha, 1.0F);
   }

   public static void glow(double x, double y, double z, float size, int color, float alpha, float intensity) {
      if (glowCount < 512) {
         Vec3d cam = getCameraPos();
         float rx = (float)(x - cam.x);
         float ry = (float)(y - cam.y);
         float rz = (float)(z - cam.z);
         float r = (float)(color >> 16 & 0xFF) / 255.0F;
         float g = (float)(color >> 8 & 0xFF) / 255.0F;
         float b = (float)(color & 0xFF) / 255.0F;
         putGlow(rx, ry, rz, size, r, g, b, alpha * intensity, 0.0F);
      }
   }

   public static void core(double x, double y, double z, float size, int color, float alpha) {
      if (glowCount < 512) {
         Vec3d cam = getCameraPos();
         float rx = (float)(x - cam.x);
         float ry = (float)(y - cam.y);
         float rz = (float)(z - cam.z);
         float r = (float)(color >> 16 & 0xFF) / 255.0F;
         float g = (float)(color >> 8 & 0xFF) / 255.0F;
         float b = (float)(color & 0xFF) / 255.0F;
         putGlow(rx, ry, rz, size * 1.8F, r, g, b, alpha, 1.0F);
      }
   }

   public static void end() {
      if (pipeline != null && uniformBuffer != null && vertexBuffer != null && glowCount != 0) {
         Matrix4f combined = combinedMatrix.set(projectionMatrix).mul(viewMatrix);
         Matrix4f viewInv = viewInvMatrix.set(viewMatrix).invert();
         float rightX = viewInv.m00();
         float rightY = viewInv.m01();
         float rightZ = viewInv.m02();
         float upX = viewInv.m10();
         float upY = viewInv.m11();
         float upZ = viewInv.m12();
         ByteBuffer uniformData = MemoryUtil.memAlloc(128);
         ByteBuffer vertexData = null;

         try {
            putMatrix(uniformData, combined);
            uniformData.flip();
            int count = Math.min(glowCount, 512);
            vertexData = MemoryUtil.memAlloc(count * 6 * 24);

            for (int i = 0; i < count; i++) {
               int offset = i * 9;
               float cx = glows[offset];
               float cy = glows[offset + 1];
               float cz = glows[offset + 2];
               float size = glows[offset + 3];
               float r = glows[offset + 4];
               float g = glows[offset + 5];
               float b = glows[offset + 6];
               float a = glows[offset + 7];
               float isCore = glows[offset + 8];
               float alphaWithCore = isCore > 0.5F ? Math.min(1.0F, a * 1.5F) : a;

               for (int v = 0; v < 6; v++) {
                  int uvOffset = v * 2;
                  float u = QUAD_UVS[uvOffset];
                  float vv = QUAD_UVS[uvOffset + 1];
                  vertexData.putFloat(cx + (rightX * u + upX * vv) * size);
                  vertexData.putFloat(cy + (rightY * u + upY * vv) * size);
                  vertexData.putFloat(cz + (rightZ * u + upZ * vv) * size);
                  vertexData.put((byte)((int)(r * 255.0F)))
                     .put((byte)((int)(g * 255.0F)))
                     .put((byte)((int)(b * 255.0F)))
                     .put((byte)((int)(alphaWithCore * 255.0F)));
                  vertexData.putFloat(u).putFloat(vv);
               }
            }

            vertexData.flip();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(), uniformData);
            encoder.writeToBuffer(vertexBuffer.slice(), vertexData);
            Framebuffer framebuffer = MC.getFramebuffer();
            if (framebuffer == null || framebuffer.getColorAttachmentView() == null || framebuffer.getDepthAttachmentView() == null) {
               return;
            }

            RenderPass pass = encoder.createRenderPass(
               () -> "Glow3D",
               framebuffer.getColorAttachmentView(),
               OptionalInt.empty(),
               framebuffer.getDepthAttachmentView(),
               ignoreDepth ? OptionalDouble.of(1.0) : OptionalDouble.empty()
            );

            try {
               pass.setPipeline(pipeline);
               pass.setUniform("Uniforms", uniformBuffer);
               pass.setVertexBuffer(0, vertexBuffer);
               pass.draw(0, count * 6);
            } catch (Throwable var32) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable var31) {
                     var32.addSuppressed(var31);
                  }
               }

               throw var32;
            }

            if (pass != null) {
               pass.close();
            }
         } finally {
            MemoryUtil.memFree(uniformData);
            if (vertexData != null) {
               MemoryUtil.memFree(vertexData);
            }

            glowCount = 0;
         }
      }
   }

   private static void putGlow(float x, float y, float z, float size, float r, float g, float b, float alpha, float core) {
      int offset = glowCount++ * 9;
      glows[offset] = x;
      glows[offset + 1] = y;
      glows[offset + 2] = z;
      glows[offset + 3] = size;
      glows[offset + 4] = r;
      glows[offset + 5] = g;
      glows[offset + 6] = b;
      glows[offset + 7] = alpha;
      glows[offset + 8] = core;
   }

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }

   public static void shutdown() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      if (vertexBuffer != null) {
         vertexBuffer.close();
         vertexBuffer = null;
      }

      pipeline = null;
   }
}
