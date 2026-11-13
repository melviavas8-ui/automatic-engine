package daamky.ua.utils.render.hands;

import com.mojang.blaze3d.buffers.GpuBuffer;
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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;
import daamky.ua.utils.render.RenderSampler;
import daamky.ua.utils.render.color.ColorUtil;

public final class HandsFlameRenderer {
   private static final Identifier TRAIL_PIPELINE_ID = Identifier.of("daamky", "pipeline/effects/hands_flame_trail");
   private static final Identifier COMPOSITE_PIPELINE_ID = Identifier.of("daamky", "pipeline/effects/hands_flame_composite");
   private static final Identifier FULLSCREEN_VERTEX_SHADER = Identifier.of("daamky", "effects/hands_flame/fullscreen");
   private static final Identifier TRAIL_FRAGMENT_SHADER = Identifier.of("daamky", "effects/hands_flame/trail");
   private static final Identifier COMPOSITE_FRAGMENT_SHADER = Identifier.of("daamky", "effects/hands_flame/composite");
   private static final int UNIFORM_BYTES = 64;
   private static boolean flameEnabled;
   private static float flameStrength = 0.85F;
   private static float flameRiseSpeed;
   private static float flameWobble = 0.65F;
   private static float flameLength = 0.95F;
   private static float flameBrightness = 0.9F;
   private static int flameColorMode;
   private static int flameColor = ColorUtil.rgba(255, 255, 255, 230);
   private static boolean flameItemsOnly;
   private static RenderPipeline trailPipeline;
   private static RenderPipeline compositePipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer dummyVertexBuffer;
   private static ByteBuffer dataBuffer;
   private static GpuTexture beforeTexture;
   private static GpuTexture sceneTexture;
   private static GpuTexture trailTextureA;
   private static GpuTexture trailTextureB;
   private static GpuTextureView beforeTextureView;
   private static GpuTextureView sceneTextureView;
   private static GpuTextureView trailTextureViewA;
   private static GpuTextureView trailTextureViewB;
   private static boolean useTrailAAsHistory = true;
   private static boolean capturedBeforeHands;
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static boolean disabledAfterError;

   private HandsFlameRenderer() {
   }

   public static void captureBeforeHandRender() {
      capturedBeforeHands = false;
      if (shouldRenderFlame()) {
         Framebuffer target = MinecraftClient.getInstance().getFramebuffer();
         if (isUsable(target)) {
            if (ensureReady(target.textureWidth, target.textureHeight)) {
               try {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .copyTextureToTexture(target.getColorAttachment(), beforeTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
                  capturedBeforeHands = true;
               } catch (Throwable var2) {
                  disableAfterError(var2);
               }
            }
         }
      }
   }

   public static void renderCapturedHandsFlame() {
      if (capturedBeforeHands) {
         capturedBeforeHands = false;
         if (shouldRenderFlame()) {
            Framebuffer target = MinecraftClient.getInstance().getFramebuffer();
            if (isUsable(target) && ensureReady(target.textureWidth, target.textureHeight)) {
               try {
                  writeUniforms(target.textureWidth, target.textureHeight);
                  CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                  encoder.writeToBuffer(uniformBuffer.slice(0L, (long)dataBuffer.remaining()), dataBuffer);
                  renderTrail(encoder, target);
                  swapTrailHistory();
                  encoder.copyTextureToTexture(target.getColorAttachment(), sceneTexture, 0, 0, 0, 0, 0, target.textureWidth, target.textureHeight);
                  renderComposite(encoder, target);
               } catch (Throwable var2) {
                  disableAfterError(var2);
               }
            }
         }
      }
   }

   public static boolean hasCapturedHands() {
      return capturedBeforeHands;
   }

   public static void resetTrail() {
      capturedBeforeHands = false;
      if (trailTextureA != null && trailTextureB != null) {
         try {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.clearColorTexture(trailTextureA, 0);
            encoder.clearColorTexture(trailTextureB, 0);
         } catch (Throwable var1) {
            disableAfterError(var1);
         }
      }
   }

   public static void shutdown() {
      closeTextures();
      if (uniformBuffer != null) {
         uniformBuffer.close();
      }

      if (dummyVertexBuffer != null) {
         dummyVertexBuffer.close();
      }

      if (dataBuffer != null) {
         MemoryUtil.memFree(dataBuffer);
      }

      uniformBuffer = null;
      dummyVertexBuffer = null;
      dataBuffer = null;
      trailPipeline = null;
      compositePipeline = null;
      capturedBeforeHands = false;
   }

   public static void setFlameEnabled(boolean enabled) {
      flameEnabled = enabled;
      if (!enabled) {
         resetTrail();
      }
   }

   public static void configure(float strength, float riseSpeed, float wobble, float length, float brightness, int colorMode, int color, boolean itemsOnly) {
      flameStrength = clamp(strength, 0.0F, 2.0F);
      flameRiseSpeed = clamp(riseSpeed, 0.0F, 2.0F);
      flameWobble = clamp(wobble, 0.0F, 2.0F);
      flameLength = clamp(length, 0.1F, 2.5F);
      flameBrightness = clamp(brightness, 0.0F, 2.0F);
      flameColorMode = Math.max(0, Math.min(2, colorMode));
      flameColor = color;
      flameItemsOnly = itemsOnly;
   }

   public static boolean shouldRenderFlame() {
      if (flameEnabled && !disabledAfterError) {
         MinecraftClient minecraft = MinecraftClient.getInstance();
         return minecraft != null && minecraft.player != null && minecraft.world != null
            ? !flameItemsOnly || !minecraft.player.getMainHandStack().isEmpty() || !minecraft.player.getOffHandStack().isEmpty()
            : false;
      } else {
         return false;
      }
   }

   private static boolean ensureReady(int width, int height) {
      if (trailPipeline == null || compositePipeline == null || uniformBuffer == null || dummyVertexBuffer == null || dataBuffer == null) {
         initPipelines();
      }

      ensureTextures(width, height);
      return trailPipeline != null
         && compositePipeline != null
         && uniformBuffer != null
         && dummyVertexBuffer != null
         && dataBuffer != null
         && beforeTexture != null
         && sceneTexture != null
         && trailTextureA != null
         && trailTextureB != null
         && beforeTextureView != null
         && sceneTextureView != null
         && trailTextureViewA != null
         && trailTextureViewB != null;
   }

   private static void initPipelines() {
      try {
         trailPipeline = RenderPipeline.builder(new Snippet[0])
            .withLocation(TRAIL_PIPELINE_ID)
            .withVertexShader(FULLSCREEN_VERTEX_SHADER)
            .withFragmentShader(TRAIL_FRAGMENT_SHADER)
            .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
            .withUniform("HandsFlameData", UniformType.UNIFORM_BUFFER)
            .withSampler("BeforeSampler")
            .withSampler("AfterSampler")
            .withSampler("PrevTrailSampler")
            .withSampler("DepthSampler")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         compositePipeline = RenderPipeline.builder(new Snippet[0])
            .withLocation(COMPOSITE_PIPELINE_ID)
            .withVertexShader(FULLSCREEN_VERTEX_SHADER)
            .withFragmentShader(COMPOSITE_FRAGMENT_SHADER)
            .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
            .withUniform("HandsFlameData", UniformType.UNIFORM_BUFFER)
            .withSampler("SceneSampler")
            .withSampler("BeforeSampler")
            .withSampler("TrailSampler")
            .withSampler("DepthSampler")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         dataBuffer = MemoryUtil.memAlloc(64);
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:hands_flame_uniform", 136, 64L);
         ByteBuffer dummyData = MemoryUtil.memAlloc(4);
         dummyData.putInt(0);
         dummyData.flip();
         dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:hands_flame_dummy_vertex", 40, dummyData);
         MemoryUtil.memFree(dummyData);
      } catch (Throwable var1) {
         disableAfterError(var1);
      }
   }

   private static void ensureTextures(int width, int height) {
      if (beforeTexture == null || width != lastWidth || height != lastHeight) {
         closeTextures();
         beforeTexture = createTexture("daamky:hands_flame_before", width, height, 5);
         sceneTexture = createTexture("daamky:hands_flame_scene", width, height, 5);
         trailTextureA = createTexture("daamky:hands_flame_trail_a", width, height, 13);
         trailTextureB = createTexture("daamky:hands_flame_trail_b", width, height, 13);
         beforeTextureView = RenderSystem.getDevice().createTextureView(beforeTexture);
         sceneTextureView = RenderSystem.getDevice().createTextureView(sceneTexture);
         trailTextureViewA = RenderSystem.getDevice().createTextureView(trailTextureA);
         trailTextureViewB = RenderSystem.getDevice().createTextureView(trailTextureB);
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.clearColorTexture(trailTextureA, 0);
         encoder.clearColorTexture(trailTextureB, 0);
         useTrailAAsHistory = true;
         lastWidth = width;
         lastHeight = height;
      }
   }

   private static GpuTexture createTexture(String label, int width, int height, int usage) {
      return RenderSystem.getDevice().createTexture(() -> label, usage, TextureFormat.RGBA8, width, height, 1, 1);
   }

   private static void writeUniforms(int width, int height) {
      int color = flameColor;
      float alpha = (float)ColorUtil.getAlpha(color) / 255.0F;
      dataBuffer.clear();
      dataBuffer.putFloat((float)ColorUtil.getRed(color) / 255.0F);
      dataBuffer.putFloat((float)ColorUtil.getGreen(color) / 255.0F);
      dataBuffer.putFloat((float)ColorUtil.getBlue(color) / 255.0F);
      dataBuffer.putFloat(alpha);
      dataBuffer.putFloat(flameStrength);
      dataBuffer.putFloat(flameRiseSpeed);
      dataBuffer.putFloat(flameWobble);
      dataBuffer.putFloat(flameLength);
      dataBuffer.putFloat(flameBrightness);
      dataBuffer.putFloat(flameTime());
      dataBuffer.putFloat((float)flameColorMode + (flameItemsOnly ? 10.0F : 0.0F));
      dataBuffer.putFloat(alpha);
      dataBuffer.putFloat((float)width);
      dataBuffer.putFloat((float)height);
      dataBuffer.putFloat(1.0F / (float)Math.max(width, 1));
      dataBuffer.putFloat(1.0F / (float)Math.max(height, 1));
      dataBuffer.flip();
   }

   private static void renderTrail(CommandEncoder encoder, Framebuffer target) {
      RenderPass renderPass = encoder.createRenderPass(() -> "daamky:hands_flame_trail", nextTrailView(), OptionalInt.empty(), null, OptionalDouble.empty());

      try {
         renderPass.setPipeline(trailPipeline);
         renderPass.setVertexBuffer(0, dummyVertexBuffer);
         renderPass.bindTexture("BeforeSampler", beforeTextureView, RenderSampler.linear());
         renderPass.bindTexture("AfterSampler", target.getColorAttachmentView(), RenderSampler.linear());
         renderPass.bindTexture("PrevTrailSampler", historyTrailView(), RenderSampler.linear());
         renderPass.bindTexture("DepthSampler", target.getDepthAttachmentView(), RenderSampler.nearest());
         renderPass.setUniform("HandsFlameData", uniformBuffer.slice());
         renderPass.draw(0, 6);
      } catch (Throwable var6) {
         if (renderPass != null) {
            try {
               renderPass.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (renderPass != null) {
         renderPass.close();
      }
   }

   private static void renderComposite(CommandEncoder encoder, Framebuffer target) {
      RenderPass renderPass = encoder.createRenderPass(
         () -> "daamky:hands_flame_composite", target.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
      );

      try {
         renderPass.setPipeline(compositePipeline);
         renderPass.setVertexBuffer(0, dummyVertexBuffer);
         renderPass.bindTexture("SceneSampler", sceneTextureView, RenderSampler.linear());
         renderPass.bindTexture("BeforeSampler", beforeTextureView, RenderSampler.linear());
         renderPass.bindTexture("TrailSampler", historyTrailView(), RenderSampler.linear());
         renderPass.bindTexture("DepthSampler", target.getDepthAttachmentView(), RenderSampler.nearest());
         renderPass.setUniform("HandsFlameData", uniformBuffer.slice());
         renderPass.draw(0, 6);
      } catch (Throwable var6) {
         if (renderPass != null) {
            try {
               renderPass.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (renderPass != null) {
         renderPass.close();
      }
   }

   private static GpuTextureView historyTrailView() {
      return useTrailAAsHistory ? trailTextureViewA : trailTextureViewB;
   }

   private static GpuTextureView nextTrailView() {
      return useTrailAAsHistory ? trailTextureViewB : trailTextureViewA;
   }

   private static void swapTrailHistory() {
      useTrailAAsHistory = !useTrailAAsHistory;
   }

   private static boolean isUsable(Framebuffer target) {
      return target != null
         && target.getColorAttachment() != null
         && target.getColorAttachmentView() != null
         && target.getDepthAttachmentView() != null
         && target.textureWidth > 0
         && target.textureHeight > 0;
   }

   private static float flameTime() {
      return (float)(System.nanoTime() % 180000000000L) / 1.0E9F;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static void disableAfterError(Throwable throwable) {
      disabledAfterError = true;
      throwable.printStackTrace();
      shutdown();
   }

   private static void closeTextures() {
      if (beforeTextureView != null) {
         beforeTextureView.close();
      }

      if (sceneTextureView != null) {
         sceneTextureView.close();
      }

      if (trailTextureViewA != null) {
         trailTextureViewA.close();
      }

      if (trailTextureViewB != null) {
         trailTextureViewB.close();
      }

      if (beforeTexture != null) {
         beforeTexture.close();
      }

      if (sceneTexture != null) {
         sceneTexture.close();
      }

      if (trailTextureA != null) {
         trailTextureA.close();
      }

      if (trailTextureB != null) {
         trailTextureB.close();
      }

      beforeTextureView = null;
      sceneTextureView = null;
      trailTextureViewA = null;
      trailTextureViewB = null;
      beforeTexture = null;
      sceneTexture = null;
      trailTextureA = null;
      trailTextureB = null;
      lastWidth = -1;
      lastHeight = -1;
   }
}
