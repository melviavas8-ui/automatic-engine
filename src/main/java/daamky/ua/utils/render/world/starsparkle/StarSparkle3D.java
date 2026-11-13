package daamky.ua.utils.render.world.starsparkle;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class StarSparkle3D {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int UNIFORM_SIZE = 128;
   private static final int SPARKLE_DATA_SIZE = 48;
   private static final int MAX_VERTICES = 16384;
   private static final int VERTEX_SIZE = 24;
   private static final int VERTEX_STRIDE = 5;
   private static final float[] vertices = new float[81920];
   private static int vertexCount;
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer sparkleDataBuffer;
   private static GpuBuffer vertexBuffer;
   private static final Matrix4f projectionMatrix = new Matrix4f();
   private static final Matrix4f viewMatrix = new Matrix4f();
   private static final Matrix4f combinedMatrix = new Matrix4f();
   private static final Matrix4f identityMatrix = new Matrix4f();
   private static boolean disabledAfterError;

   private StarSparkle3D() {
   }

   public static void init() {
      if (pipeline == null) {
         try {
            VertexFormat format = VertexFormat.builder()
               .add("inPosition", VertexFormatElement.POSITION)
               .add("inColor", VertexFormatElement.COLOR)
               .add("inUV", VertexFormatElement.UV)
               .build();
            pipeline = RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.of("daamky", "pipeline/world/starsparkle"))
               .withVertexShader(Identifier.of("daamky", "world/shared/particle_billboard"))
               .withFragmentShader(Identifier.of("daamky", "world/starsparkle/starsparkle"))
               .withVertexFormat(format, DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withUniform("SparkleData", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "StarSparkle3D Uniforms", 136, 128L);
            sparkleDataBuffer = RenderSystem.getDevice().createBuffer(() -> "StarSparkle3D Data", 136, 48L);
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "StarSparkle3D Vertices", 40, 393216L);
         } catch (Exception var1) {
            disabledAfterError = true;
         }
      }
   }

   public static void setMatrices(Matrix4f projection, Matrix4f view) {
      projectionMatrix.set(projection);
      viewMatrix.set(view);
   }

   public static void begin() {
      if (!disabledAfterError) {
         if (pipeline == null) {
            init();
         }

         vertexCount = 0;
      }
   }

   public static void box(Box box) {
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
      float rx1 = (float)((double)x1 - cam.x);
      float ry1 = (float)((double)y1 - cam.y);
      float rz1 = (float)((double)z1 - cam.z);
      float rx2 = (float)((double)x2 - cam.x);
      float ry2 = (float)((double)y2 - cam.y);
      float rz2 = (float)((double)z2 - cam.z);
      quad(rx1, ry1, rz1, rx2, ry1, rz1, rx2, ry1, rz2, rx1, ry1, rz2);
      quad(rx1, ry2, rz1, rx1, ry2, rz2, rx2, ry2, rz2, rx2, ry2, rz1);
      quad(rx1, ry1, rz1, rx1, ry2, rz1, rx2, ry2, rz1, rx2, ry1, rz1);
      quad(rx1, ry1, rz2, rx2, ry1, rz2, rx2, ry2, rz2, rx1, ry2, rz2);
      quad(rx1, ry1, rz1, rx1, ry1, rz2, rx1, ry2, rz2, rx1, ry2, rz1);
      quad(rx2, ry1, rz1, rx2, ry2, rz1, rx2, ry2, rz2, rx2, ry1, rz2);
   }

   private static void quad(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
      addVertex(x1, y1, z1, 0.0F, 0.0F);
      addVertex(x2, y2, z2, 1.0F, 0.0F);
      addVertex(x3, y3, z3, 1.0F, 1.0F);
      addVertex(x1, y1, z1, 0.0F, 0.0F);
      addVertex(x3, y3, z3, 1.0F, 1.0F);
      addVertex(x4, y4, z4, 0.0F, 1.0F);
   }

   private static void addVertex(float x, float y, float z, float u, float v) {
      if (vertexCount < 16384 && Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z) && Float.isFinite(u) && Float.isFinite(v)) {
         int offset = vertexCount++ * 5;
         vertices[offset] = x;
         vertices[offset + 1] = y;
         vertices[offset + 2] = z;
         vertices[offset + 3] = u;
         vertices[offset + 4] = v;
      }
   }

   public static void end(int backgroundColor, int starColor, float backgroundAlpha, float starAlpha, float time, float scale) {
      if (!disabledAfterError) {
         if (pipeline != null && vertexCount != 0 && uniformBuffer != null && sparkleDataBuffer != null && vertexBuffer != null) {
            if (Float.isFinite(backgroundAlpha) && Float.isFinite(starAlpha) && Float.isFinite(time) && Float.isFinite(scale)) {
               int count = Math.min(vertexCount, 16384);
               Matrix4f combined = combinedMatrix.set(projectionMatrix).mul(viewMatrix);
               if (!isFiniteMatrix(combined)) {
                  vertexCount = 0;
               } else {
                  backgroundAlpha = Math.clamp(backgroundAlpha, 0.0F, 1.0F);
                  starAlpha = Math.clamp(starAlpha, 0.0F, 2.0F);
                  scale = Math.clamp(scale, 0.05F, 6.0F);
                  ByteBuffer uniformData = MemoryUtil.memAlloc(128);
                  putMatrix(uniformData, combined);
                  putMatrix(uniformData, identityMatrix);
                  uniformData.flip();
                  ByteBuffer sparkleData = MemoryUtil.memAlloc(48);
                  putColor(sparkleData, backgroundColor, backgroundAlpha);
                  putColor(sparkleData, starColor, starAlpha);
                  sparkleData.putFloat(time).putFloat(scale).putFloat(0.0F).putFloat(0.0F);
                  sparkleData.flip();
                  ByteBuffer vertexData = MemoryUtil.memAlloc(count * 24);

                  for (int i = 0; i < count; i++) {
                     int offset = i * 5;
                     vertexData.putFloat(vertices[offset]).putFloat(vertices[offset + 1]).putFloat(vertices[offset + 2]);
                     vertexData.putInt(-1);
                     vertexData.putFloat(vertices[offset + 3]).putFloat(vertices[offset + 4]);
                  }

                  vertexData.flip();

                  label128: {
                     try {
                        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                        encoder.writeToBuffer(uniformBuffer.slice(), uniformData);
                        encoder.writeToBuffer(sparkleDataBuffer.slice(), sparkleData);
                        encoder.writeToBuffer(vertexBuffer.slice(), vertexData);
                        Framebuffer framebuffer = mc.getFramebuffer();
                        if (framebuffer != null && framebuffer.getColorAttachmentView() != null && framebuffer.getDepthAttachmentView() != null) {
                           RenderPass pass = encoder.createRenderPass(
                              () -> "StarSparkle3D",
                              framebuffer.getColorAttachmentView(),
                              OptionalInt.empty(),
                              framebuffer.getDepthAttachmentView(),
                              OptionalDouble.empty()
                           );

                           try {
                              pass.setPipeline(pipeline);
                              pass.setUniform("Uniforms", uniformBuffer);
                              pass.setUniform("SparkleData", sparkleDataBuffer);
                              pass.setVertexBuffer(0, vertexBuffer);
                              pass.draw(0, count);
                           } catch (Throwable var22) {
                              if (pass != null) {
                                 try {
                                    pass.close();
                                 } catch (Throwable var21) {
                                    var22.addSuppressed(var21);
                                 }
                              }

                              throw var22;
                           }

                           if (pass != null) {
                              pass.close();
                           }
                           break label128;
                        }

                        vertexCount = 0;
                     } catch (Throwable var23) {
                        disabledAfterError = true;
                        break label128;
                     } finally {
                        MemoryUtil.memFree(uniformData);
                        MemoryUtil.memFree(sparkleData);
                        MemoryUtil.memFree(vertexData);
                     }

                     return;
                  }

                  vertexCount = 0;
               }
            } else {
               vertexCount = 0;
            }
         }
      }
   }

   private static void putColor(ByteBuffer buffer, int color, float alpha) {
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      buffer.putFloat(r).putFloat(g).putFloat(b).putFloat(alpha);
   }

   private static boolean isFiniteMatrix(Matrix4f matrix) {
      return Float.isFinite(matrix.m00())
         && Float.isFinite(matrix.m01())
         && Float.isFinite(matrix.m02())
         && Float.isFinite(matrix.m03())
         && Float.isFinite(matrix.m10())
         && Float.isFinite(matrix.m11())
         && Float.isFinite(matrix.m12())
         && Float.isFinite(matrix.m13())
         && Float.isFinite(matrix.m20())
         && Float.isFinite(matrix.m21())
         && Float.isFinite(matrix.m22())
         && Float.isFinite(matrix.m23())
         && Float.isFinite(matrix.m30())
         && Float.isFinite(matrix.m31())
         && Float.isFinite(matrix.m32())
         && Float.isFinite(matrix.m33());
   }

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }
}
