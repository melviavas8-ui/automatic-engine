package daamky.ua.utils.render.world.jumpcircle;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
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
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.RenderSampler;

public final class GroundGlassRenderer {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static final int MATRIX_DATA_SIZE = 128;
   private static final int EFFECT_DATA_SIZE = 48;
   private static final int VERTEX_COUNT = 6;
   private static final int VERTEX_SIZE = 24;
   private static final Identifier VERTEX_SHADER = Identifier.of("daamky", "world/jump_circle/ground_glass");
   private static final Identifier FRAGMENT_SHADER = Identifier.of("daamky", "world/jump_circle/ground_glass");
   private static final BlendFunction REPLACE_BLEND = new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ONE, DestFactor.ZERO);
   private static RenderPipeline pipeline;
   private static GpuBuffer matrixBuffer;
   private static GpuBuffer effectBuffer;
   private static GpuBuffer vertexBuffer;
   private static GpuTexture sceneTexture;
   private static GpuTextureView sceneTextureView;
   private static int sceneWidth = -1;
   private static int sceneHeight = -1;
   private static boolean disabledAfterError;

   private GroundGlassRenderer() {
   }

   public static void render(Vec3d center, float radius, int color, float alpha, float progress) {
      if (!disabledAfterError && center != null && !(radius <= 0.01F) && !(alpha <= 0.01F)) {
         Framebuffer target = MC.getFramebuffer();
         if (target != null
            && target.getColorAttachment() != null
            && target.getColorAttachmentView() != null
            && target.getDepthAttachmentView() != null
            && target.textureWidth > 0
            && target.textureHeight > 0) {
            try {
               init();
               if (!ensureSceneTexture(target.textureWidth, target.textureHeight)) {
                  return;
               }

               Vec3d camera = MC.gameRenderer.getCamera().getCameraPos();
               float x = (float)(center.x - camera.x);
               float y = (float)(center.y - camera.y + 0.012);
               float z = (float)(center.z - camera.z);
               float time = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer matrices = stack.calloc(128);
                  putMatrix(matrices, new Matrix4f(Render3D.lastProjMat).mul(Render3D.lastModMat));
                  putMatrix(matrices, new Matrix4f());
                  matrices.flip();
                  ByteBuffer effect = stack.calloc(48);
                  effect.putFloat(0, (float)target.textureWidth);
                  effect.putFloat(4, (float)target.textureHeight);
                  effect.putFloat(8, time);
                  effect.putFloat(12, Math.clamp(progress, 0.0F, 1.0F));
                  effect.putFloat(16, (float)(color >> 16 & 0xFF) / 255.0F);
                  effect.putFloat(20, (float)(color >> 8 & 0xFF) / 255.0F);
                  effect.putFloat(24, (float)(color & 0xFF) / 255.0F);
                  effect.putFloat(28, Math.clamp(alpha, 0.0F, 1.0F));
                  effect.putFloat(32, 32.0F);
                  effect.putFloat(36, 0.0F);
                  effect.putFloat(40, 0.0F);
                  effect.putFloat(44, 0.0F);
                  effect.position(0);
                  ByteBuffer vertices = stack.calloc(144);
                  vertex(vertices, x - radius, y, z - radius, 0.0F, 0.0F);
                  vertex(vertices, x + radius, y, z - radius, 1.0F, 0.0F);
                  vertex(vertices, x + radius, y, z + radius, 1.0F, 1.0F);
                  vertex(vertices, x - radius, y, z - radius, 0.0F, 0.0F);
                  vertex(vertices, x + radius, y, z + radius, 1.0F, 1.0F);
                  vertex(vertices, x - radius, y, z + radius, 0.0F, 1.0F);
                  vertices.flip();
                  CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                  encoder.copyTextureToTexture(target.getColorAttachment(), sceneTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
                  encoder.writeToBuffer(matrixBuffer.slice(0L, 128L), matrices);
                  encoder.writeToBuffer(effectBuffer.slice(0L, 48L), effect);
                  encoder.writeToBuffer(vertexBuffer.slice(0L, 144L), vertices);
                  RenderPass pass = encoder.createRenderPass(
                     () -> "daamky:jump_circle_ground_glass",
                     target.getColorAttachmentView(),
                     OptionalInt.empty(),
                     target.getDepthAttachmentView(),
                     OptionalDouble.empty()
                  );

                  try {
                     pass.setPipeline(pipeline);
                     pass.setUniform("Uniforms", matrixBuffer);
                     pass.setUniform("GlassData", effectBuffer);
                     pass.bindTexture("SceneSampler", sceneTextureView, RenderSampler.linear());
                     pass.setVertexBuffer(0, vertexBuffer);
                     pass.draw(0, 6);
                  } catch (Throwable var21) {
                     if (pass != null) {
                        try {
                           pass.close();
                        } catch (Throwable var20) {
                           var21.addSuppressed(var20);
                        }
                     }

                     throw var21;
                  }

                  if (pass != null) {
                     pass.close();
                  }
               } catch (Throwable var22) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var19) {
                        var22.addSuppressed(var19);
                     }
                  }

                  throw var22;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (Throwable var23) {
               disabledAfterError = true;
               var23.printStackTrace();
            }
         }
      }
   }

   private static void init() {
      if (pipeline == null) {
         VertexFormat format = VertexFormat.builder()
            .add("inPosition", VertexFormatElement.POSITION)
            .add("inColor", VertexFormatElement.COLOR)
            .add("inUV", VertexFormatElement.UV)
            .build();
         pipeline = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.of("daamky", "pipeline/world/jump_circle_ground_glass"))
               .withVertexShader(VERTEX_SHADER)
               .withFragmentShader(FRAGMENT_SHADER)
               .withVertexFormat(format, DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withUniform("GlassData", UniformType.UNIFORM_BUFFER)
               .withSampler("SceneSampler")
               .withBlend(REPLACE_BLEND)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withCull(false)
               .build()
         );
      }

      GpuDevice device = RenderSystem.getDevice();
      if (matrixBuffer == null || matrixBuffer.isClosed()) {
         matrixBuffer = device.createBuffer(() -> "daamky:jump_circle_ground_matrices", 136, 128L);
      }

      if (effectBuffer == null || effectBuffer.isClosed()) {
         effectBuffer = device.createBuffer(() -> "daamky:jump_circle_ground_effect", 136, 48L);
      }

      if (vertexBuffer == null || vertexBuffer.isClosed()) {
         vertexBuffer = device.createBuffer(() -> "daamky:jump_circle_ground_vertices", 40, 144L);
      }
   }

   private static boolean ensureSceneTexture(int width, int height) {
      if (sceneTexture != null && sceneWidth == width && sceneHeight == height) {
         return sceneTextureView != null;
      } else {
         closeSceneTexture();
         GpuDevice device = RenderSystem.tryGetDevice();
         if (device == null) {
            return false;
         } else {
            sceneTexture = device.createTexture(() -> "daamky:jump_circle_ground_scene", 5, TextureFormat.RGBA8, width, height, 1, 1);
            sceneTextureView = device.createTextureView(sceneTexture);
            sceneWidth = width;
            sceneHeight = height;
            return sceneTextureView != null;
         }
      }
   }

   private static void vertex(ByteBuffer buffer, float x, float y, float z, float u, float v) {
      buffer.putFloat(x).putFloat(y).putFloat(z);
      buffer.putInt(-1);
      buffer.putFloat(u).putFloat(v);
   }

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }

   private static void closeSceneTexture() {
      if (sceneTextureView != null) {
         sceneTextureView.close();
         sceneTextureView = null;
      }

      if (sceneTexture != null) {
         sceneTexture.close();
         sceneTexture = null;
      }

      sceneWidth = -1;
      sceneHeight = -1;
   }
}
