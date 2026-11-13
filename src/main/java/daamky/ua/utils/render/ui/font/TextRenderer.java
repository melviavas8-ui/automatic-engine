package daamky.ua.utils.render.ui.font;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import daamky.ua.access.GuiRenderStateLayerAccessor;
import daamky.ua.mixin.accessor.GuiGraphicsExtractorAccessor;
import daamky.ua.utils.render.ScissorUtil;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;

public final class TextRenderer implements AutoCloseable {
   private static volatile TextRenderer instance;
   public static final RenderPipeline TEXT_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/text_atlas"))
      .withVertexShader(id("ui/shared/text_atlas"))
      .withFragmentShader(id("ui/text_atlas/text_atlas"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline TEXT_FADE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/text_atlas_fade"))
      .withVertexShader(id("ui/shared/text_atlas"))
      .withFragmentShader(id("ui/text_atlas_fade/text_atlas_fade"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   private final FontManager fontManager = new FontManager();
   private final Map<TextRenderer.FrameBatchKey, TextRenderState> frameBatches = new LinkedHashMap<>(64);
   private DrawContext activeGraphics;

   private TextRenderer() {
   }

   public static TextRenderer getInstance() {
      TextRenderer local = instance;
      if (local == null) {
         synchronized (TextRenderer.class) {
            local = instance;
            if (local == null) {
               local = new TextRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      TextRenderer local = instance;
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

   public void enqueue(BuiltText text) {
      this.submit(this.activeGraphics, text);
   }

   public void draw(DrawContext graphics, BuiltText text) {
      this.beginFrame(graphics);
      this.enqueue(text);
      this.flush();
   }

   public void flush() {
      this.activeGraphics = null;
      this.frameBatches.clear();
   }

   public float width(String fontName, String text, float size) {
      return text != null && !text.isEmpty() ? this.fontManager.strike(fontName, size).width(text) : 0.0F;
   }

   public TextRenderer.GlyphLayout glyphLayout(String fontName, String text, float size) {
      if (text != null && !text.isEmpty() && !(size <= 0.0F)) {
         try {
            FontStrike strike = this.fontManager.strike(fontName, size);
            TextLayout layout = strike.layout(text);
            if (layout.empty()) {
               return TextRenderer.GlyphLayout.EMPTY;
            } else {
               strike.uploadDirtyPages();
               List<TextRenderer.GlyphPage> pages = new ArrayList<>(layout.pages().size());

               for (TextLayout.Page page : layout.pages()) {
                  List<TextRenderer.Glyph> glyphs = new ArrayList<>(page.glyphs().size());

                  for (LayoutGlyph glyph : page.glyphs()) {
                     glyphs.add(new TextRenderer.Glyph(glyph.x0(), glyph.y0(), glyph.x1(), glyph.y1(), glyph.u0(), glyph.v0(), glyph.u1(), glyph.v1()));
                  }

                  pages.add(new TextRenderer.GlyphPage(page.page().textureSetup(), glyphs));
               }

               return new TextRenderer.GlyphLayout(pages, layout.width(), layout.height());
            }
         } catch (RuntimeException var12) {
            return TextRenderer.GlyphLayout.EMPTY;
         }
      } else {
         return TextRenderer.GlyphLayout.EMPTY;
      }
   }

   private void submit(DrawContext graphics, BuiltText text) {
      if (graphics != null && text != null) {
         BuiltText normalized = text;
         if (text.visible()) {
            try {
               FontStrike strike = this.fontManager.strike(normalized.fontName(), normalized.size());
               Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
               TextLayout layout = strike.layout(normalized.text());
               if (layout.empty()) {
                  return;
               }

               strike.uploadDirtyPages();
               GuiRenderState guiState = ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState();
               int layerSerial = ((GuiRenderStateLayerAccessor)guiState).daamky$getLayerSerial();
               TextRenderer.PoseKey poseKey = TextRenderer.PoseKey.of(pose);
               ScreenRect scissorArea = ScissorUtil.current();
               RenderPipeline pipeline = normalized.hasHorizontalFade() ? TEXT_FADE_PIPELINE : TEXT_PIPELINE;

               for (TextLayout.Page page : layout.pages()) {
                  if (!page.glyphs().isEmpty()) {
                     TextRenderer.FrameBatchKey key = new TextRenderer.FrameBatchKey(guiState, layerSerial, page.page(), poseKey, scissorArea, pipeline);
                     TextRenderState state = this.frameBatches.get(key);
                     if (state == null) {
                        state = new TextRenderState(pose, page.page(), scissorArea, pipeline);
                        this.frameBatches.put(key, state);
                        guiState.addPreparedTextElement(state);
                     }

                     state.add(
                        page.glyphs(),
                        normalized.x(),
                        normalized.y(),
                        normalized.colorTopLeft(),
                        normalized.colorTopRight(),
                        normalized.colorBottomRight(),
                        normalized.colorBottomLeft(),
                        normalized.rotationDegrees(),
                        normalized.rotationOriginX(),
                        normalized.rotationOriginY(),
                        normalized.fadeLeft(),
                        normalized.fadeRight(),
                        normalized.fadeLeftX(),
                        normalized.fadeRightX(),
                        normalized.fadeWidth(),
                        normalized.fadeLeftStrength(),
                        normalized.fadeRightStrength()
                     );
                  }
               }
            } catch (RuntimeException var16) {
            }
         }
      }
   }

   private static Identifier id(String path) {
      return Identifier.of("daamky", path);
   }

   @Override
   public void close() {
      this.activeGraphics = null;
      this.frameBatches.clear();
      this.fontManager.close();
   }

   private static record FrameBatchKey(
      GuiRenderState state, int layerSerial, GlyphAtlasPage page, TextRenderer.PoseKey pose, ScreenRect scissorArea, RenderPipeline pipeline
   ) {
   }

   public static record Glyph(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {
   }

   public static record GlyphLayout(List<TextRenderer.GlyphPage> pages, float width, float height) {
      public static final TextRenderer.GlyphLayout EMPTY = new TextRenderer.GlyphLayout(List.of(), 0.0F, 0.0F);

      public GlyphLayout(List<TextRenderer.GlyphPage> pages, float width, float height) {
         pages = List.copyOf(pages);
         this.pages = pages;
         this.width = width;
         this.height = height;
      }

      public boolean empty() {
         return this.pages.isEmpty();
      }
   }

   public static record GlyphPage(TextureSetup textureSetup, List<TextRenderer.Glyph> glyphs) {
      public GlyphPage(TextureSetup textureSetup, List<TextRenderer.Glyph> glyphs) {
         glyphs = List.copyOf(glyphs);
         this.textureSetup = textureSetup;
         this.glyphs = glyphs;
      }
   }

   private static record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
      static TextRenderer.PoseKey of(Matrix3x2f matrix) {
         return new TextRenderer.PoseKey(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21());
      }
   }
}
