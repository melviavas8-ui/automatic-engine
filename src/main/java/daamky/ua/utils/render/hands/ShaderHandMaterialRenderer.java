package daamky.ua.utils.render.hands;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;
import daamky.ua.mixin.accessor.MixinRenderType;

public final class ShaderHandMaterialRenderer {
   private static final Identifier ITEM_PIPELINE_ID = id("pipeline/effects/shader_hand_material_item");
   private static final Identifier ARM_PIPELINE_ID = id("pipeline/effects/shader_hand_material_arm");
   private static final Identifier ITEM_VERTEX_SHADER = id("effects/shader_hand/material_item");
   private static final Identifier ITEM_FRAGMENT_SHADER = id("effects/shader_hand/material_item");
   private static final Identifier ARM_VERTEX_SHADER = id("effects/shader_hand/material_arm");
   private static final Identifier ARM_FRAGMENT_SHADER = id("effects/shader_hand/material_arm");
   private static final int UNIFORM_BYTES = 16;
   private static final Map<Identifier, RenderType> ITEM_LAYERS = new HashMap<>();
   private static final Map<Identifier, RenderType> ARM_LAYERS = new HashMap<>();
   private static boolean enabled;
   private static boolean disabledAfterError;
   private static int mode;
   private static float intensity = 1.0F;
   private static float speed = 1.0F;
   private static int submissionDepth;
   private static Identifier activeSkin;
   private static RenderPipeline itemPipeline;
   private static RenderPipeline armPipeline;
   private static GpuBuffer uniformBuffer;
   private static ByteBuffer dataBuffer;

   private ShaderHandMaterialRenderer() {
   }

   public static void setEnabled(boolean value) {
      enabled = value;
      submissionDepth = 0;
      activeSkin = null;
   }

   public static void configure(int shaderMode, float shaderIntensity, float shaderSpeed) {
      mode = Math.max(0, Math.min(8, shaderMode));
      intensity = clamp(shaderIntensity, 0.25F, 1.5F);
      speed = clamp(shaderSpeed, 0.1F, 2.5F);
   }

   public static void beginFrame() {
      if (enabled && !disabledAfterError) {
         try {
            ensureReady();
            dataBuffer.clear();
            dataBuffer.putFloat((float)mode);
            dataBuffer.putFloat((float)(System.nanoTime() % 600000000000L) / 1.0E9F);
            dataBuffer.putFloat(intensity);
            dataBuffer.putFloat(speed);
            dataBuffer.flip();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniformBuffer.slice(0L, (long)dataBuffer.remaining()), dataBuffer);
         } catch (Throwable var1) {
            disableAfterError(var1);
         }
      }
   }

   public static void beginSubmission(AbstractClientPlayer player) {
      if (enabled && !disabledAfterError && player != null) {
         if (itemPipeline == null || armPipeline == null || uniformBuffer == null) {
            beginFrame();
         }

         if (!disabledAfterError) {
            submissionDepth++;
            activeSkin = player.getSkin().body().texturePath();
         }
      }
   }

   public static void endSubmission() {
      if (submissionDepth > 0) {
         submissionDepth--;
      }

      if (submissionDepth == 0) {
         activeSkin = null;
      }
   }

   public static boolean isSubmittingMaterial() {
      return enabled && !disabledAfterError && submissionDepth > 0;
   }

   public static RenderType itemLayer(RenderType original, List<BakedQuad> quads) {
      if (isSubmittingMaterial() && itemPipeline != null) {
         Identifier atlas = TextureAtlas.LOCATION_ITEMS;
         if (quads != null) {
            for (BakedQuad quad : quads) {
               if (quad != null && quad.sprite() != null && quad.sprite().atlasLocation() != null) {
                  atlas = quad.sprite().atlasLocation();
                  break;
               }
            }
         }

         return ITEM_LAYERS.computeIfAbsent(atlas, ShaderHandMaterialRenderer::createItemLayer);
      } else {
         return original;
      }
   }

   public static RenderType armLayer(RenderType original) {
      return isSubmittingMaterial() && armPipeline != null && activeSkin != null
         ? ARM_LAYERS.computeIfAbsent(activeSkin, ShaderHandMaterialRenderer::createArmLayer)
         : original;
   }

   public static boolean isMaterialPipeline(RenderPipeline pipeline) {
      return pipeline != null && (pipeline == itemPipeline || pipeline == armPipeline);
   }

   public static void bindUniform(RenderPass renderPass) {
      if (renderPass != null && uniformBuffer != null) {
         renderPass.setUniform("ShaderHandData", uniformBuffer.slice());
      }
   }

   private static void ensureReady() {
      if (itemPipeline == null || armPipeline == null || uniformBuffer == null || dataBuffer == null) {
         itemPipeline = RenderPipeline.builder(new Snippet[]{RenderPipelines.BLOCK_SNIPPET})
            .withLocation(ITEM_PIPELINE_ID)
            .withVertexShader(ITEM_VERTEX_SHADER)
            .withFragmentShader(ITEM_FRAGMENT_SHADER)
            .withVertexFormat(DefaultVertexFormat.BLOCK, Mode.QUADS)
            .withUniform("ShaderHandData", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(true)
            .withCull(false)
            .build();
         armPipeline = RenderPipeline.builder(new Snippet[]{RenderPipelines.ENTITY_SNIPPET})
            .withLocation(ARM_PIPELINE_ID)
            .withVertexShader(ARM_VERTEX_SHADER)
            .withFragmentShader(ARM_FRAGMENT_SHADER)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, Mode.QUADS)
            .withUniform("ShaderHandData", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(true)
            .withCull(false)
            .build();
         dataBuffer = MemoryUtil.memAlloc(16);
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky:shader_hand_material_uniform", 136, 16L);
      }
   }

   private static RenderType createItemLayer(Identifier atlas) {
      RenderSetup setup = RenderSetup.builder(itemPipeline).bufferSize(1536).withTexture("Sampler0", atlas).useLightmap().createRenderSetup();
      return MixinRenderType.daamky$create("daamky_shader_hand_item", setup);
   }

   private static RenderType createArmLayer(Identifier skin) {
      RenderSetup setup = RenderSetup.builder(armPipeline).bufferSize(1536).withTexture("Sampler0", skin).useLightmap().useOverlay().createRenderSetup();
      return MixinRenderType.daamky$create("daamky_shader_hand_arm", setup);
   }

   private static void disableAfterError(Throwable throwable) {
      disabledAfterError = true;
      enabled = false;
      submissionDepth = 0;
      activeSkin = null;
      System.err.println("Shader Hand material disabled after render error: " + throwable.getMessage());
      throwable.printStackTrace();
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("daamky", path);
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
