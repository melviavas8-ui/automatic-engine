package daamky.ua.utils.render.post.jumpcircle;

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
import java.util.List;
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

public final class JumpCircleDistortionRenderer {
   public static final int MAX_CIRCLES = 8;
   private static final int UNIFORM_SIZE = 496;
   private static final Identifier PIPELINE_ID = Identifier.of("daamky", "pipeline/post/jumpcircle/distortion");
   private static final Identifier SHADER = Identifier.of("daamky", "post/jumpcircle/distortion");
   private static final Identifier VERTEX_SHADER = Identifier.of("daamky", "post/fogblur/fogblur");
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuTexture sceneCopyTexture;
   private static GpuTextureView sceneCopyTextureView;
   private static int sceneWidth = -1;
   private static int sceneHeight = -1;
   private static boolean disabledAfterError;

   private JumpCircleDistortionRenderer() {
   }

   public static void apply(
      Framebuffer target,
      Matrix4fc projectionMatrix,
      Matrix4fc viewMatrix,
      Vec3d cameraPosition,
      List<JumpCircleDistortionRenderer.Circle> circles,
      float distortionStrength,
      float groundLight
   ) {
      if (!disabledAfterError
         && target != null
         && projectionMatrix != null
         && viewMatrix != null
         && cameraPosition != null
         && circles != null
         && !circles.isEmpty()
         && !(distortionStrength <= 0.001F)
         && target.textureWidth > 0
         && target.textureHeight > 0
         && target.getColorAttachment() != null
         && target.getColorAttachmentView() != null
         && target.getDepthAttachmentView() != null) {
         init();
         if (pipeline != null && uniformBuffer != null && ensureSceneCopy(target.textureWidth, target.textureHeight)) {
            try {
               Matrix4f inverseViewProjection = new Matrix4f(projectionMatrix).mul(viewMatrix).invert();
               if (!isFinite(inverseViewProjection)) {
                  return;
               }

               CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
               encoder.copyTextureToTexture(target.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
               writeUniforms(
                  encoder, inverseViewProjection, cameraPosition, target.textureWidth, target.textureHeight, circles, distortionStrength, groundLight
               );
               RenderPass pass = encoder.createRenderPass(() -> "daamky:jump_circle_distortion", target.getColorAttachmentView(), OptionalInt.empty());

               try {
                  pass.setPipeline(pipeline);
                  pass.setUniform("CircleDistortionData", uniformBuffer);
                  pass.bindTexture("SceneSampler", sceneCopyTextureView, RenderSampler.linear());
                  pass.bindTexture("DepthSampler", target.getDepthAttachmentView(), RenderSampler.nearest());
                  pass.draw(0, 6);
               } catch (Throwable var13) {
                  if (pass != null) {
                     try {
                        pass.close();
                     } catch (Throwable var12) {
                        var13.addSuppressed(var12);
                     }
                  }

                  throw var13;
               }

               if (pass != null) {
                  pass.close();
               }
            } catch (Throwable var14) {
               disabledAfterError = true;
               var14.printStackTrace();
               closeResources();
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
                     .withFragmentShader(SHADER)
                     .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                     .withUniform("CircleDistortionData", UniformType.UNIFORM_BUFFER)
                     .withSampler("SceneSampler")
                     .withSampler("DepthSampler")
                     .withBlend(BlendFunction.TRANSLUCENT)
                     .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                     .withDepthWrite(false)
                     .withCull(false)
                     .build()
               );
            }

            if (uniformBuffer == null || uniformBuffer.isClosed() || uniformBuffer.size() < 496L) {
               if (uniformBuffer != null) {
                  uniformBuffer.close();
               }

               uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:jump_circle_distortion_uniforms", 136, 496L);
            }
         } catch (Throwable var1) {
            disabledAfterError = true;
            var1.printStackTrace();
            pipeline = null;
            closeResources();
         }
      }
   }

   private static boolean ensureSceneCopy(int width, int height) {
      if (sceneCopyTexture != null && sceneCopyTextureView != null && sceneWidth == width && sceneHeight == height) {
         return true;
      } else {
         closeSceneCopy();
         GpuDevice device = RenderSystem.tryGetDevice();
         if (device == null) {
            return false;
         } else {
            sceneCopyTexture = device.createTexture(() -> "daamky:jump_circle_scene_copy", 5, TextureFormat.RGBA8, width, height, 1, 1);
            sceneCopyTextureView = device.createTextureView(sceneCopyTexture);
            sceneWidth = width;
            sceneHeight = height;
            return true;
         }
      }
   }

   private static void writeUniforms(
      CommandEncoder encoder,
      Matrix4fc inverseViewProjection,
      Vec3d cameraPosition,
      int width,
      int height,
      List<JumpCircleDistortionRenderer.Circle> circles,
      float distortionStrength,
      float groundLight
   ) {
      int count = Math.min(circles.size(), 8);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.calloc(496);
         inverseViewProjection.get(0, data);
         data.putFloat(64, (float)cameraPosition.x);
         data.putFloat(68, (float)cameraPosition.y);
         data.putFloat(72, (float)cameraPosition.z);
         data.putFloat(76, (float)((double)System.nanoTime() * 1.0E-9));
         data.putFloat(80, (float)width);
         data.putFloat(84, (float)height);
         data.putFloat(88, (float)count);
         data.putFloat(92, (float)width / (float)Math.max(1, height));
         data.putFloat(96, Math.clamp(distortionStrength, 0.0F, 2.5F));
         data.putFloat(100, Math.clamp(groundLight, 0.0F, 2.0F));
         data.putFloat(104, 0.0F);
         data.putFloat(108, 0.0F);

         for (int index = 0; index < 8; index++) {
            JumpCircleDistortionRenderer.Circle circle = index < count ? circles.get(index) : JumpCircleDistortionRenderer.Circle.EMPTY;
            int centerOffset = 112 + index * 16;
            data.putFloat(centerOffset, (float)circle.center.x);
            data.putFloat(centerOffset + 4, (float)circle.center.y);
            data.putFloat(centerOffset + 8, (float)circle.center.z);
            data.putFloat(centerOffset + 12, circle.radius);
            int motionOffset = 240 + index * 16;
            data.putFloat(motionOffset, circle.progress);
            data.putFloat(motionOffset + 4, circle.alpha);
            data.putFloat(motionOffset + 8, circle.thickness);
            data.putFloat(motionOffset + 12, circle.seed);
            int colorOffset = 368 + index * 16;
            data.putFloat(colorOffset, circle.red);
            data.putFloat(colorOffset + 4, circle.green);
            data.putFloat(colorOffset + 8, circle.blue);
            data.putFloat(colorOffset + 12, circle.style);
         }

         data.position(0);
         encoder.writeToBuffer(uniformBuffer.slice(0L, 496L), data);
      } catch (Throwable var17) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var16) {
               var17.addSuppressed(var16);
            }
         }

         throw var17;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static boolean isFinite(Matrix4fc matrix) {
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

   private static void closeResources() {
      closeSceneCopy();
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }
   }

   private static void closeSceneCopy() {
      if (sceneCopyTextureView != null) {
         sceneCopyTextureView.close();
         sceneCopyTextureView = null;
      }

      if (sceneCopyTexture != null) {
         sceneCopyTexture.close();
         sceneCopyTexture = null;
      }

      sceneWidth = -1;
      sceneHeight = -1;
   }

   public static record Circle(
      Vec3d center, float radius, float progress, float alpha, float thickness, float seed, float red, float green, float blue, float style
   ) {
      private static final JumpCircleDistortionRenderer.Circle EMPTY = new JumpCircleDistortionRenderer.Circle(
         Vec3d.ZERO, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F
      );
   }
}
