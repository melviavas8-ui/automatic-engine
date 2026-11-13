package daamky.ua.utils.render.ui.msdf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import daamky.ua.access.GuiRenderStateLayerAccessor;
import daamky.ua.mixin.accessor.GuiGraphicsExtractorAccessor;
import daamky.ua.utils.render.ScissorUtil;
import daamky.ua.utils.render.ui.GuiResourceLoader;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;
import daamky.ua.utils.render.ui.font.FontType;

public final class MsdfIconRenderer implements AutoCloseable {
   private static volatile MsdfIconRenderer instance;
   private static final int MAX_WIDTH_CACHE = 512;
   private static final VertexFormat MSDF_ICON_VERTEX_FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline MSDF_ICON_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/msdf_icon"))
      .withVertexShader(id("ui/msdf_icon/msdf_icon"))
      .withFragmentShader(id("ui/msdf_icon/msdf_icon"))
      .withVertexFormat(MSDF_ICON_VERTEX_FORMAT, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   private final Map<String, MsdfIconRenderer.Atlas> atlases = new HashMap<>();
   private final Map<MsdfIconRenderer.FrameBatchKey, MsdfIconRenderState> frameBatches = new LinkedHashMap<>(32);
   private final Map<MsdfIconRenderer.WidthKey, Float> widthCache = new LinkedHashMap<MsdfIconRenderer.WidthKey, Float>(128, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<MsdfIconRenderer.WidthKey, Float> eldest) {
         return this.size() > 512;
      }
   };
   private DrawContext activeGraphics;

   private MsdfIconRenderer() {
   }

   public static MsdfIconRenderer getInstance() {
      MsdfIconRenderer local = instance;
      if (local == null) {
         synchronized (MsdfIconRenderer.class) {
            local = instance;
            if (local == null) {
               local = new MsdfIconRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      MsdfIconRenderer local = instance;
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

   public void draw(FontType font, String text, float x, float y, float size, int color) {
      this.draw(font, text, x, y, size, color, color, color, color, 0.0F, x, y, false, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   public void draw(
      FontType font,
      String text,
      float x,
      float y,
      float size,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY,
      boolean fadeLeft,
      boolean fadeRight,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      float fadeLeftStrength,
      float fadeRightStrength
   ) {
      if (this.activeGraphics != null
         && font != null
         && text != null
         && !text.isEmpty()
         && !(size <= 0.0F)
         && maxAlpha(colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft) > 0) {
         MsdfIconRenderer.Atlas atlas = this.atlas(font);
         TextureSetup setup = this.textureSetup(font, atlas);
         if (setup != null) {
            try {
               GuiRenderState guiState = ((GuiGraphicsExtractorAccessor)this.activeGraphics).daamky$getGuiRenderState();
               int layerSerial = ((GuiRenderStateLayerAccessor)guiState).daamky$getLayerSerial();
               Matrix3x2f pose = Render2DCoordinateSpace.pose(this.activeGraphics);
               ScreenRect scissorArea = ScissorUtil.current();
               MsdfIconRenderer.FrameBatchKey key = new MsdfIconRenderer.FrameBatchKey(
                  guiState, layerSerial, MsdfIconRenderer.PoseKey.of(pose), scissorArea, setup
               );
               MsdfIconRenderState state = this.frameBatches.get(key);
               if (state == null) {
                  state = new MsdfIconRenderState(pose, setup, scissorArea, atlas.distanceRange());
                  this.frameBatches.put(key, state);
                  guiState.addPreparedTextElement(state);
               }

               float cursorX = x;
               float scale = size / atlas.fontSize();
               int index = 0;

               while (index < text.length()) {
                  int codePoint = text.codePointAt(index);
                  index += Character.charCount(codePoint);
                  MsdfGlyph glyph = glyph(atlas, codePoint);
                  if (glyph != null) {
                     if (glyph.drawable()) {
                        float glyphX = cursorX + glyph.xOffset() * scale;
                        float glyphY = y + glyph.yOffset() * scale;
                        float glyphWidth = glyph.width() * scale;
                        float glyphHeight = glyph.height() * scale;
                        state.add(
                           glyphX,
                           glyphY,
                           glyphWidth,
                           glyphHeight,
                           glyph,
                           normalizeColor(colorTopLeft),
                           normalizeColor(colorTopRight),
                           normalizeColor(colorBottomRight),
                           normalizeColor(colorBottomLeft),
                           rotationDegrees,
                           rotationOriginX,
                           rotationOriginY,
                           fadeLeft,
                           fadeRight,
                           fadeLeftX,
                           fadeRightX,
                           fadeWidth,
                           fadeLeftStrength,
                           fadeRightStrength
                        );
                     }

                     cursorX += glyph.advance() * scale;
                  }
               }
            } catch (RuntimeException var37) {
            }
         }
      }
   }

   public boolean canRender(FontType font, String text) {
      if (font != null && text != null && !text.isEmpty()) {
         MsdfIconRenderer.Atlas atlas = this.atlas(font);
         if (!atlas.ready()) {
            return false;
         } else {
            int index = 0;

            while (index < text.length()) {
               int codePoint = text.codePointAt(index);
               index += Character.charCount(codePoint);
               if (Character.isISOControl(codePoint)) {
                  return false;
               }

               if (glyph(atlas, codePoint) == null) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public float width(FontType font, String text, float size) {
      if (font != null && text != null && !text.isEmpty() && !(size <= 0.0F)) {
         MsdfIconRenderer.Atlas atlas = this.atlas(font);
         if (!atlas.ready()) {
            return 0.0F;
         } else if (text.codePointCount(0, text.length()) == 1) {
            MsdfGlyph glyph = glyph(atlas, text.codePointAt(0));
            return glyph == null ? 0.0F : glyph.advance() * (size / atlas.fontSize());
         } else {
            MsdfIconRenderer.WidthKey key = new MsdfIconRenderer.WidthKey(font.fontName(), text, normalizeSize(size));
            Float cached = this.widthCache.get(key);
            if (cached != null) {
               return cached;
            } else {
               float scale = size / atlas.fontSize();
               float width = 0.0F;
               int index = 0;

               while (index < text.length()) {
                  int codePoint = text.codePointAt(index);
                  index += Character.charCount(codePoint);
                  MsdfGlyph glyph = glyph(atlas, codePoint);
                  if (glyph != null) {
                     width += glyph.advance() * scale;
                  }
               }

               this.widthCache.put(key, width);
               return width;
            }
         }
      } else {
         return 0.0F;
      }
   }

   public void flush() {
      this.activeGraphics = null;
      this.frameBatches.clear();
   }

   private MsdfIconRenderer.Atlas atlas(FontType font) {
      return this.atlases.computeIfAbsent(font.fontName(), ignored -> this.load(font));
   }

   private MsdfIconRenderer.Atlas load(FontType font) {
      Map<Integer, MsdfGlyph> glyphs = new HashMap<>();
      MsdfGlyph[] asciiGlyphs = new MsdfGlyph[128];
      Identifier jsonId = Identifier.of("daamky", "ui/msdf/" + font.path() + ".json");
      InputStream source = GuiResourceLoader.open(jsonId);
      if (source == null) {
         MinecraftClient minecraft = MinecraftClient.getInstance();
         if (minecraft == null || minecraft.getResourceManager() == null) {
            return new MsdfIconRenderer.Atlas(font.path(), glyphs, asciiGlyphs, null, null, 96.0F, 8.0F);
         }

         Optional<Resource> resource = minecraft.getResourceManager().getResource(jsonId);
         if (resource.isEmpty()) {
            return new MsdfIconRenderer.Atlas(font.path(), glyphs, asciiGlyphs, null, null, 96.0F, 8.0F);
         }

         try {
            source = resource.get().getInputStream();
         } catch (Exception var34) {
            return new MsdfIconRenderer.Atlas(font.path(), glyphs, asciiGlyphs, null, null, 96.0F, 8.0F);
         }
      }

      float fontSize = 96.0F;
      float distanceRange = 8.0F;

      try {
         InputStream stream = source;

         try (InputStreamReader reader = new InputStreamReader(source, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject atlas = root.getAsJsonObject("atlas");
            float atlasWidth = getFloat(atlas, "width", 4096.0F);
            float atlasHeight = getFloat(atlas, "height", 4096.0F);
            fontSize = getFloat(atlas, "size", 96.0F);
            distanceRange = Math.max(1.0F, getFloat(atlas, "distanceRange", 8.0F));
            boolean originBottom = atlas.has("yOrigin") && "bottom".equalsIgnoreCase(atlas.get("yOrigin").getAsString());

            for (JsonElement element : root.getAsJsonArray("glyphs")) {
               JsonObject glyph = element.getAsJsonObject();
               if (glyph.has("unicode")) {
                  int unicode = glyph.get("unicode").getAsInt();
                  MsdfGlyph msdfGlyph;
                  if (glyph.has("atlasBounds") && glyph.has("planeBounds")) {
                     JsonObject atlasBounds = glyph.getAsJsonObject("atlasBounds");
                     JsonObject planeBounds = glyph.getAsJsonObject("planeBounds");
                     float left = getFloat(atlasBounds, "left", 0.0F);
                     float bottom = getFloat(atlasBounds, "bottom", 0.0F);
                     float right = getFloat(atlasBounds, "right", 0.0F);
                     float top = getFloat(atlasBounds, "top", 0.0F);
                     float textureY = originBottom ? atlasHeight - top : bottom;
                     float planeLeft = getFloat(planeBounds, "left", 0.0F);
                     float planeBottom = getFloat(planeBounds, "bottom", 0.0F);
                     float planeRight = getFloat(planeBounds, "right", 0.0F);
                     float planeTop = getFloat(planeBounds, "top", 0.0F);
                     float ascender = 0.95F;
                     msdfGlyph = new MsdfGlyph(
                        planeLeft * fontSize,
                        (ascender - planeTop) * fontSize,
                        (planeRight - planeLeft) * fontSize,
                        (planeTop - planeBottom) * fontSize,
                        getFloat(glyph, "advance", 1.0F) * fontSize,
                        left / atlasWidth,
                        textureY / atlasHeight,
                        right / atlasWidth,
                        (textureY + top - bottom) / atlasHeight
                     );
                  } else {
                     msdfGlyph = new MsdfGlyph(0.0F, 0.0F, 0.0F, 0.0F, getFloat(glyph, "advance", 1.0F) * fontSize, 0.0F, 0.0F, 0.0F, 0.0F);
                  }

                  glyphs.put(unicode, msdfGlyph);
                  if (unicode >= 0 && unicode < asciiGlyphs.length) {
                     asciiGlyphs[unicode] = msdfGlyph;
                  }
               }
            }
         } catch (Throwable var36) {
            if (source != null) {
               try {
                  stream.close();
               } catch (Throwable var32) {
                  var36.addSuppressed(var32);
               }
            }

            throw var36;
         }

         if (source != null) {
            source.close();
         }
      } catch (Exception var37) {
         glyphs.clear();
         Arrays.fill(asciiGlyphs, null);
      }

      return new MsdfIconRenderer.Atlas(font.path(), glyphs, asciiGlyphs, null, null, fontSize, distanceRange);
   }

   private TextureSetup textureSetup(FontType font, MsdfIconRenderer.Atlas atlas) {
      if (atlas.textureSetup() != null) {
         return atlas.textureSetup();
      } else {
         MinecraftClient minecraft = MinecraftClient.getInstance();
         if (minecraft != null && minecraft.getTextureManager() != null) {
            Identifier textureId = Identifier.of("daamky", "ui/msdf/" + font.path() + ".png");
            NativeImageBackedTexture classpathTexture = GuiResourceLoader.loadTexture(textureId, "daamky_msdf_");
            if (classpathTexture != null && classpathTexture.getGlTextureView() != null) {
               TextureSetup setup = TextureSetup.of(classpathTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
               this.atlases.put(font.fontName(), atlas.withTextureSetup(setup, classpathTexture));
               return setup;
            } else {
               AbstractTexture texture = minecraft.getTextureManager().getTexture(textureId);
               if (texture != null && texture.getGlTextureView() != null) {
                  TextureSetup setup = TextureSetup.of(texture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                  this.atlases.put(font.fontName(), atlas.withTextureSetup(setup, null));
                  return setup;
               } else {
                  return null;
               }
            }
         } else {
            return null;
         }
      }
   }

   private static float getFloat(JsonObject object, String key, float fallback) {
      return object != null && object.has(key) ? object.get(key).getAsFloat() : fallback;
   }

   private static int normalizeColor(int color) {
      return color;
   }

   private static int effectiveAlpha(int color) {
      return color >>> 24 & 0xFF;
   }

   private static int maxAlpha(int... colors) {
      int alpha = 0;

      for (int color : colors) {
         alpha = Math.max(alpha, effectiveAlpha(color));
      }

      return alpha;
   }

   private static MsdfGlyph glyph(MsdfIconRenderer.Atlas atlas, int codePoint) {
      return codePoint >= 0 && codePoint < atlas.asciiGlyphs().length ? atlas.asciiGlyphs()[codePoint] : atlas.glyphs().get(codePoint);
   }

   private static float normalizeSize(float size) {
      return Math.max(1.0F, Math.min(512.0F, (float)Math.round(size * 4.0F) / 4.0F));
   }

   private static Identifier id(String path) {
      return Identifier.of("daamky", path);
   }

   @Override
   public void close() {
      for (MsdfIconRenderer.Atlas atlas : this.atlases.values()) {
         if (atlas.texture() != null) {
            atlas.texture().close();
         }
      }

      this.atlases.clear();
      this.frameBatches.clear();
      this.widthCache.clear();
      this.activeGraphics = null;
   }

   private static record Atlas(
      String path,
      Map<Integer, MsdfGlyph> glyphs,
      MsdfGlyph[] asciiGlyphs,
      TextureSetup textureSetup,
      NativeImageBackedTexture texture,
      float fontSize,
      float distanceRange
   ) {
      private boolean ready() {
         return !this.glyphs.isEmpty();
      }

      private MsdfIconRenderer.Atlas withTextureSetup(TextureSetup setup, NativeImageBackedTexture texture) {
         return new MsdfIconRenderer.Atlas(this.path, this.glyphs, this.asciiGlyphs, setup, texture, this.fontSize, this.distanceRange);
      }
   }

   private static record FrameBatchKey(GuiRenderState state, int layerSerial, MsdfIconRenderer.PoseKey pose, ScreenRect scissorArea, TextureSetup textureSetup) {
   }

   private static record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
      static MsdfIconRenderer.PoseKey of(Matrix3x2f matrix) {
         return new MsdfIconRenderer.PoseKey(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21());
      }
   }

   private static record WidthKey(String fontName, String text, float size) {
   }
}
