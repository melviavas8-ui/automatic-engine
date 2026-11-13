package daamky.ua.utils.render.world.blockblur;

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
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import daamky.ua.utils.render.RenderSampler;
import daamky.ua.utils.render.color.ColorUtil;

public final class BlockBlur3D {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static final int MAX_VERTICES = 16384;
   private static final int VERTEX_STRIDE = 5;
   private static final int VERTEX_SIZE = 20;
   private static final int MATRIX_UNIFORM_SIZE = 128;
   private static final int BLUR_UNIFORM_SIZE = 16;
   private static final int COMPOSITE_UNIFORM_SIZE = 32;
   private static final int MAX_BLUR_PASSES = 5;
   private static final float[] vertices = new float[81920];
   private static final Matrix4f projectionMatrix = new Matrix4f();
   private static final Matrix4f viewMatrix = new Matrix4f();
   private static final Matrix4f combinedMatrix = new Matrix4f();
   private static final Matrix4f identityMatrix = new Matrix4f();
   private static final SimpleFramebuffer[] downTargets = new SimpleFramebuffer[5];
   private static final SimpleFramebuffer[] upTargets = new SimpleFramebuffer[5];
   private static int vertexCount;
   private static RenderPipeline kawasePipeline;
   private static RenderPipeline compositePipeline;
   private static GpuBuffer matrixBuffer;
   private static GpuBuffer blurUniformBuffer;
   private static GpuBuffer compositeUniformBuffer;
   private static GpuBuffer vertexBuffer;
   private static GpuTexture sceneTexture;
   private static GpuTextureView sceneTextureView;
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static int allocatedPasses;
   private static boolean disabledAfterError;

   private BlockBlur3D() {
   }

   public static void setMatrices(Matrix4f projection, Matrix4f view) {
      projectionMatrix.set(projection);
      viewMatrix.set(view);
   }

   public static void begin() {
      if (!disabledAfterError) {
         init();
         vertexCount = 0;
      }
   }

   public static void box(Box box) {
      if (box != null) {
         float x1 = (float)box.minX;
         float y1 = (float)box.minY;
         float z1 = (float)box.minZ;
         float x2 = (float)box.maxX;
         float y2 = (float)box.maxY;
         float z2 = (float)box.maxZ;
         Vec3d cam = MC.gameRenderer.getCamera().getCameraPos();
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
   }

   public static void end(float opacity, float blurRadius, int blurPasses) {
      if (!disabledAfterError && vertexCount != 0 && compositePipeline != null && kawasePipeline != null) {
         Framebuffer framebuffer = MC.getFramebuffer();
         if (!isUsable(framebuffer)) {
            vertexCount = 0;
         } else {
            int passes = Math.clamp((long)blurPasses, 1, 5);
            if (!ensureTargets(framebuffer.textureWidth, framebuffer.textureHeight, passes)) {
               vertexCount = 0;
            } else {
               try {
                  CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                  encoder.copyTextureToTexture(
                     framebuffer.getColorAttachment(), sceneTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight
                  );
                  GpuTextureView blurred = renderBlurChain(sceneTextureView, framebuffer.textureWidth, framebuffer.textureHeight, passes, blurRadius);
                  renderComposite(framebuffer, blurred, Math.clamp(opacity, 0.0F, 1.0F));
               } catch (Throwable var10) {
                  disabledAfterError = true;
                  var10.printStackTrace();
                  closeTargets();
               } finally {
                  vertexCount = 0;
               }
            }
         }
      } else {
         vertexCount = 0;
      }
   }

   private static void init() {
      if (!disabledAfterError && compositePipeline == null) {
         try {
            VertexFormat format = VertexFormat.builder().add("inPosition", VertexFormatElement.POSITION).add("inUV", VertexFormatElement.UV).build();
            kawasePipeline = RenderPipelines.register(
               RenderPipeline.builder(new Snippet[0])
                  .withLocation(Identifier.of("daamky", "pipeline/world/block_blur/kawase"))
                  .withVertexShader(Identifier.of("daamky", "effects/glass_hands/kawase"))
                  .withFragmentShader(Identifier.of("daamky", "effects/glass_hands/kawase"))
                  .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                  .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
                  .withSampler("Sampler0")
                  .withBlend(BlendFunction.TRANSLUCENT)
                  .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                  .withDepthWrite(false)
                  .withCull(false)
                  .build()
            );
            compositePipeline = RenderPipelines.register(
               RenderPipeline.builder(new Snippet[0])
                  .withLocation(Identifier.of("daamky", "pipeline/world/block_blur/composite"))
                  .withVertexShader(Identifier.of("daamky", "world/block_blur/block_blur"))
                  .withFragmentShader(Identifier.of("daamky", "world/block_blur/block_blur"))
                  .withVertexFormat(format, DrawMode.TRIANGLES)
                  .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                  .withUniform("BlockBlurData", UniformType.UNIFORM_BUFFER)
                  .withSampler("BlurSampler")
                  .withBlend(BlendFunction.TRANSLUCENT)
                  .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                  .withDepthWrite(false)
                  .withCull(false)
                  .build()
            );
            GpuDevice device = RenderSystem.getDevice();
            matrixBuffer = device.createBuffer(() -> "daamky:block_blur_matrices", 136, 128L);
            blurUniformBuffer = device.createBuffer(() -> "daamky:block_blur_kawase_uniform", 136, 16L);
            compositeUniformBuffer = device.createBuffer(() -> "daamky:block_blur_uniform", 136, 32L);
            vertexBuffer = device.createBuffer(() -> "daamky:block_blur_vertices", 40, 327680L);
         } catch (Throwable var2) {
            disabledAfterError = true;
            var2.printStackTrace();
         }
      }
   }

   private static boolean ensureTargets(int width, int height, int passes) {
      GpuDevice device = RenderSystem.tryGetDevice();
      if (device == null) {
         return false;
      } else if (sceneTexture != null && width == lastWidth && height == lastHeight && allocatedPasses >= passes) {
         return true;
      } else {
         closeTargets();
         sceneTexture = device.createTexture(() -> "daamky:block_blur_scene", 5, TextureFormat.RGBA8, width, height, 1, 1);
         sceneTextureView = device.createTextureView(sceneTexture);
         int currentWidth = width;
         int currentHeight = height;

         for (int i = 0; i < passes; i++) {
            currentWidth = Math.max(1, currentWidth / 2);
            currentHeight = Math.max(1, currentHeight / 2);
            downTargets[i] = ensureTarget(downTargets[i], "daamky_block_blur_down_" + i, currentWidth, currentHeight);
            upTargets[i] = ensureTarget(upTargets[i], "daamky_block_blur_up_" + i, currentWidth, currentHeight);
         }

         lastWidth = width;
         lastHeight = height;
         allocatedPasses = passes;
         return sceneTextureView != null;
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

   private static GpuTextureView renderBlurChain(GpuTextureView source, int sourceWidth, int sourceHeight, int passes, float offset) {
      GpuTextureView currentSource = source;
      int currentWidth = sourceWidth;
      int currentHeight = sourceHeight;
      GpuSampler sampler = RenderSampler.linear();

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

   private static void renderKawasePass(GpuTextureView source, int sourceWidth, int sourceHeight, SimpleFramebuffer target, float offset, GpuSampler sampler) {
      if (source != null && target != null && target.getColorAttachmentView() != null) {
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         MemoryStack stack = MemoryStack.stackPush();

         try {
            ByteBuffer data = stack.calloc(16);
            data.putFloat(0, (float)sourceWidth);
            data.putFloat(4, (float)sourceHeight);
            data.putFloat(8, Math.clamp(offset, 1.0F, 8.0F));
            data.position(0);
            encoder.writeToBuffer(blurUniformBuffer.slice(0L, 16L), data);
            RenderPass renderPass = encoder.createRenderPass(
               () -> "daamky:block_blur_kawase", target.getColorAttachmentView(), OptionalInt.of(ColorUtil.TRANSPARENT)
            );

            try {
               renderPass.setPipeline(kawasePipeline);
               renderPass.setUniform("KawaseData", blurUniformBuffer);
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

   private static void renderComposite(Framebuffer framebuffer, GpuTextureView blurred, float opacity) {
      if (blurred != null && framebuffer.getColorAttachmentView() != null && framebuffer.getDepthAttachmentView() != null) {
         int count = Math.min(vertexCount, 16384);
         Matrix4f combined = combinedMatrix.set(projectionMatrix).mul(viewMatrix);
         ByteBuffer matrixData = MemoryUtil.memAlloc(128);
         ByteBuffer vertexData = MemoryUtil.memAlloc(count * 20);

         try {
            putMatrix(matrixData, combined);
            putMatrix(matrixData, identityMatrix);
            matrixData.flip();

            for (int i = 0; i < count; i++) {
               int offset = i * 5;
               vertexData.putFloat(vertices[offset]).putFloat(vertices[offset + 1]).putFloat(vertices[offset + 2]);
               vertexData.putFloat(vertices[offset + 3]).putFloat(vertices[offset + 4]);
            }

            vertexData.flip();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(matrixBuffer.slice(0L, 128L), matrixData);
            encoder.writeToBuffer(vertexBuffer.slice(0L, (long)(count * 20)), vertexData);
            MemoryStack stack = MemoryStack.stackPush();

            try {
               ByteBuffer data = stack.calloc(32);
               data.putFloat(0, (float)framebuffer.textureWidth);
               data.putFloat(4, (float)framebuffer.textureHeight);
               data.putFloat(8, opacity);
               data.putFloat(12, 0.0F);
               data.position(0);
               encoder.writeToBuffer(compositeUniformBuffer.slice(0L, 32L), data);
            } catch (Throwable var20) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var18) {
                     var20.addSuppressed(var18);
                  }
               }

               throw var20;
            }

            if (stack != null) {
               stack.close();
            }

            RenderPass pass = encoder.createRenderPass(
               () -> "daamky:block_blur_composite",
               framebuffer.getColorAttachmentView(),
               OptionalInt.empty(),
               framebuffer.getDepthAttachmentView(),
               OptionalDouble.empty()
            );

            try {
               pass.setPipeline(compositePipeline);
               pass.setUniform("Uniforms", matrixBuffer);
               pass.setUniform("BlockBlurData", compositeUniformBuffer);
               pass.bindTexture("BlurSampler", blurred, RenderSampler.linear());
               pass.setVertexBuffer(0, vertexBuffer);
               pass.draw(0, count);
            } catch (Throwable var19) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable var17) {
                     var19.addSuppressed(var17);
                  }
               }

               throw var19;
            }

            if (pass != null) {
               pass.close();
            }
         } finally {
            MemoryUtil.memFree(matrixData);
            MemoryUtil.memFree(vertexData);
         }
      }
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
      if (vertexCount < 16384 && Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)) {
         int offset = vertexCount++ * 5;
         vertices[offset] = x;
         vertices[offset + 1] = y;
         vertices[offset + 2] = z;
         vertices[offset + 3] = u;
         vertices[offset + 4] = v;
      }
   }

   private static boolean isUsable(Framebuffer framebuffer) {
      return framebuffer != null
         && framebuffer.getColorAttachment() != null
         && framebuffer.getColorAttachmentView() != null
         && framebuffer.getDepthAttachmentView() != null
         && framebuffer.textureWidth > 0
         && framebuffer.textureHeight > 0;
   }

   private static void closeTargets() {
      if (sceneTextureView != null) {
         sceneTextureView.close();
      }

      if (sceneTexture != null) {
         sceneTexture.close();
      }

      sceneTextureView = null;
      sceneTexture = null;

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

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }
}
