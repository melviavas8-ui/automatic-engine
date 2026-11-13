package daamky.ua.utils.render.ui.effecticon;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
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
      .withVertexFormat(EFFECT_ICON_VERTEX_FORMAT, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   private final Map<Identifier, EffectIconRenderer.CachedTexture> textures = new HashMap<>();
   private final Map<EffectIconRenderer.FrameBatchKey, EffectIconRenderState> frameBatches = new LinkedHashMap<>(16);
   private DrawContext activeGraphics;

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

   public void beginFrame(DrawContext graphics) {
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

   private void submit(DrawContext graphics, BuiltEffectIcon icon) {
      if (graphics != null && icon != null && icon.visible()) {
         Sprite sprite = this.resolveSprite(icon);
         if (sprite != null) {
            EffectIconTexture texture = this.resolveTexture(sprite.getAtlasId());
            if (texture != null) {
               EffectIconQuad quad = new EffectIconQuad(
                  icon.x(), icon.y(), icon.size(), sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), normalizeColor(icon.color())
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
                     guiState.addSimpleElement(state);
                  } else {
                     state.add(quad);
                  }
               } catch (RuntimeException var11) {
               }
            }
         }
      }
   }

   private Sprite resolveSprite(BuiltEffectIcon icon) {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      if (minecraft != null && icon.effect() != null) {
         SpriteAtlasTexture atlas = minecraft.getAtlasManager().getAtlasTexture(Atlases.GUI);
         Identifier spriteId = InGameHud.getEffectTexture(icon.effect());
         return atlas.getSprite(spriteId);
      } else {
         return null;
      }
   }

   private EffectIconTexture resolveTexture(Identifier atlasLocation) {
      if (atlasLocation == null) {
         return null;
      } else {
         MinecraftClient minecraft = MinecraftClient.getInstance();
         if (minecraft != null && minecraft.getTextureManager() != null) {
            AbstractTexture texture = minecraft.getTextureManager().getTexture(atlasLocation);
            if (texture != null && texture.getGlTextureView() != null) {
               EffectIconRenderer.CachedTexture cached = this.textures.get(atlasLocation);
               if (cached != null && cached.texture() == texture) {
                  return cached.value();
               } else {
                  TextureSetup setup = TextureSetup.of(texture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
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
      return Identifier.of("daamky", path);
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
