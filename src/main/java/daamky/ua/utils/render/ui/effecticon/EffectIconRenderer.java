package daamky.ua.utils.render.ui.effecticon;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import daamky.ua.access.GuiRenderStateLayerAccessor;
import daamky.ua.mixin.accessor.GuiGraphicsExtractorAccessor;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;

public final class EffectIconRenderer implements AutoCloseable {
   private static volatile EffectIconRenderer instance;
   private static final VertexFormat EFFECT_ICON_VERTEX_FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   public static final RenderPipeline EFFECT_ICON_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/effect_icon"))
      .withVertexShader(id("ui/effect_icon/effect_icon"))
      .withFragmentShader(id("ui/effect_icon/effect_icon"))
      .withVertexFormat(EFFECT_ICON_VERTEX_FORMAT, Mode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   private final Map<Identifier, EffectIconRenderer.CachedTexture> textures = new HashMap<>();
   private final Map<EffectIconRenderer.FrameBatchKey, EffectIconRenderState> frameBatches = new LinkedHashMap<>(16);
   private GuiGraphics activeGraphics;

   private EffectIconRenderer() {
   }

   public static EffectIconRenderer getInstance() {
      EffectIconRenderer local = instance;
      if (local == null) {
         synchronized (EffectIconRenderer.class) {
            local = instance;
            if (local == null) {
               local = new EffectIconRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      EffectIconRenderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(GuiGraphics graphics) {
      if (this.activeGraphics != graphics) {
         this.frameBatches.clear();
      }

      this.activeGraphics = graphics;
   }

   public void enqueue(BuiltEffectIcon icon) {
      this.submit(this.activeGraphics, icon);
   }

   public void flush() {
      this.activeGraphics = null;
      this.frameBatches.clear();
   }

   public void barrier() {
      this.frameBatches.clear();
   }

   private void submit(GuiGraphics graphics, BuiltEffectIcon icon) {
      if (graphics != null && icon != null && icon.visible()) {
         TextureAtlasSprite sprite = this.resolveSprite(icon);
         if (sprite != null) {
            EffectIconTexture texture = this.resolveTexture(sprite.atlasLocation());
            if (texture != null) {
               EffectIconQuad quad = new EffectIconQuad(
                  icon.x(), icon.y(), icon.size(), sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), normalizeColor(icon.color())
               );

               try {
                  GuiRenderState guiState = ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState();
                  int layerSerial = ((GuiRenderStateLayerAccessor)guiState).daamky$getLayerSerial();
                  Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
                  EffectIconRenderer.FrameBatchKey key = new EffectIconRenderer.FrameBatchKey(
                     guiState, layerSerial, texture.id(), EffectIconRenderer.PoseKey.of(pose)
                  );
                  EffectIconRenderState state = this.frameBatches.get(key);
                  if (state == null) {
                     state = new EffectIconRenderState(pose, texture);
                     state.add(quad);
                     this.frameBatches.put(key, state);
                     guiState.submitGuiElement(state);
                  } else {
                     state.add(quad);
                  }
               } catch (RuntimeException var11) {
               }
            }
         }
      }
   }

   private TextureAtlasSprite resolveSprite(BuiltEffectIcon icon) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft != null && icon.effect() != null) {
         TextureAtlas atlas = minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);
         Identifier spriteId = Gui.getMobEffectSprite(icon.effect());
         return atlas.getSprite(spriteId);
      } else {
         return null;
      }
   }

   private EffectIconTexture resolveTexture(Identifier atlasLocation) {
      if (atlasLocation == null) {
         return null;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft != null && minecraft.getTextureManager() != null) {
            AbstractTexture texture = minecraft.getTextureManager().getTexture(atlasLocation);
            if (texture != null && texture.getTextureView() != null) {
               EffectIconRenderer.CachedTexture cached = this.textures.get(atlasLocation);
               if (cached != null && cached.texture() == texture) {
                  return cached.value();
               } else {
                  TextureSetup setup = TextureSetup.singleTexture(texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                  EffectIconTexture value = new EffectIconTexture(atlasLocation, setup);
                  this.textures.put(atlasLocation, new EffectIconRenderer.CachedTexture(texture, value));
                  return value;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   private static int normalizeColor(int color) {
      return (color & 0xFF000000) == 0 && (color & 16777215) != 0 ? color | 0xFF000000 : color;
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("daamky", path);
   }

   @Override
   public void close() {
      this.frameBatches.clear();
      this.textures.clear();
      this.activeGraphics = null;
   }

   private static record CachedTexture(AbstractTexture texture, EffectIconTexture value) {
   }

   private static record FrameBatchKey(GuiRenderState state, int layerSerial, Identifier texture, EffectIconRenderer.PoseKey pose) {
   }

   private static record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
      static EffectIconRenderer.PoseKey of(Matrix3x2f matrix) {
         return new EffectIconRenderer.PoseKey(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21());
      }
   }
}
