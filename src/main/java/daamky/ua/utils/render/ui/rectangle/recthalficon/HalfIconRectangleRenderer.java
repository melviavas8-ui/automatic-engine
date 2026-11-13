package daamky.ua.utils.render.ui.rectangle.recthalficon;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import daamky.ua.access.GuiRenderStateLayerAccessor;
import daamky.ua.mixin.accessor.GuiGraphicsExtractorAccessor;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;
import daamky.ua.utils.render.ui.font.TextRenderer;

public final class HalfIconRectangleRenderer implements AutoCloseable {
   private static final int MAX_RECTANGLES = 256;
   private static final int PARAMS_PER_RECTANGLE = 3;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 12288;
   private static final int MAX_ICON_COLUMNS = 96;
   private static final int MAX_ICON_ROWS = 96;
   private static final VertexFormat POSITION_TEX_COLOR_LINE_WIDTH = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   private static volatile HalfIconRectangleRenderer instance;
   public static final RenderPipeline HALF_ICON_RECTANGLE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/rect_half_icon"))
      .withVertexShader(id("ui/rect_half_icon/rect_half_icon"))
      .withFragmentShader(id("ui/rect_half_icon/rect_half_icon"))
      .withVertexFormat(POSITION_TEX_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("RectHalfIconParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltHalfIconRectangle> preparedRectangles = new ArrayList<>(32);
   private final Map<HalfIconRectangleRenderer.FrameBatchKey, HalfIconRectangleRenderState> frameBatches = new LinkedHashMap<>(32);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty = true;

   private HalfIconRectangleRenderer() {
   }

   public static HalfIconRectangleRenderer getInstance() {
      HalfIconRectangleRenderer local = instance;
      if (local == null) {
         synchronized (HalfIconRectangleRenderer.class) {
            local = instance;
            if (local == null) {
               local = new HalfIconRectangleRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      HalfIconRectangleRenderer local = instance;
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

   public void draw(DrawContext graphics, BuiltHalfIconRectangle rectangle) {
      this.beginFrame(graphics);
      this.enqueue(rectangle);
      this.flush();
   }

   public void enqueue(BuiltHalfIconRectangle rectangle) {
      this.submit(this.activeGraphics, rectangle);
   }

   public void flush() {
      this.activeGraphics = null;
      this.frameBatches.clear();
   }

   public void beginGuiFrame() {
      this.preparedRectangles.clear();
      this.paramsDirty = false;
   }

   public boolean isHalfIconRectanglePipeline(RenderPipeline pipeline) {
      return pipeline == HALF_ICON_RECTANGLE_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedRectangles.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("RectHalfIconParamsArray", buffer);
         }
      }
   }

   public void prepareBuffers() {
      if (!this.preparedRectangles.isEmpty() && this.paramsDirty) {
         GpuBuffer buffer = this.ensureWritableParamsBuffer();
         if (buffer != null) {
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer uniformData = this.buildUniformData(stack, this.preparedRectangles);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(0L, (long)uniformData.remaining()), uniformData);
                  this.paramsDirty = false;
               } catch (Throwable var6) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                     }
                  }

                  throw var6;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (RuntimeException var7) {
               this.paramsDirty = true;
            }
         }
      }
   }

   int reserve(BuiltHalfIconRectangle rectangle) {
      int index = this.preparedRectangles.size();
      if (index == 256) {
         return -1;
      } else {
         this.preparedRectangles.add(rectangle);
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltHalfIconRectangle rectangle) {
      if (graphics != null && rectangle != null && rectangle.iconsVisible()) {
         try {
            BuiltHalfIconRectangle normalized = this.normalize(rectangle);
            TextRenderer.GlyphLayout layout = TextRenderer.getInstance().glyphLayout(normalized.fontName(), normalized.icon(), normalized.iconSize());
            if (layout.empty()) {
               return;
            }

            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            GuiRenderState guiState = ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState();
            int layerSerial = ((GuiRenderStateLayerAccessor)guiState).daamky$getLayerSerial();
            HalfIconRectangleRenderer.PoseKey poseKey = HalfIconRectangleRenderer.PoseKey.of(pose);

            for (TextRenderer.GlyphPage page : layout.pages()) {
               if (!page.glyphs().isEmpty()) {
                  List<HalfIconRectangleRenderState.IconQuad> quads = this.buildIconQuads(normalized, layout, page);
                  if (!quads.isEmpty()) {
                     HalfIconRectangleRenderer.FrameBatchKey key = new HalfIconRectangleRenderer.FrameBatchKey(
                        guiState, layerSerial, page.textureSetup(), poseKey
                     );
                     HalfIconRectangleRenderState state = this.frameBatches.get(key);
                     if (state == null) {
                        state = new HalfIconRectangleRenderState(pose, page.textureSetup());
                        state.add(normalized, quads);
                        this.frameBatches.put(key, state);
                        guiState.addSimpleElement(state);
                     } else {
                        state.add(normalized, quads);
                     }
                  }
               }
            }
         } catch (RuntimeException var14) {
         }
      }
   }

   private List<HalfIconRectangleRenderState.IconQuad> buildIconQuads(
      BuiltHalfIconRectangle rectangle, TextRenderer.GlyphLayout layout, TextRenderer.GlyphPage page
   ) {
      List<HalfIconRectangleRenderState.IconQuad> quads = new ArrayList<>(64);
      float spacingX = Math.max(rectangle.spacingX(), Math.max(layout.width(), 1.0F));
      float spacingY = Math.max(rectangle.spacingY(), Math.max(layout.height(), 1.0F));
      float startX = rectangle.x() + rectangle.paddingX() + layout.width() * 0.5F;
      float startY = rectangle.y() + rectangle.paddingY() + layout.height() * 0.5F;
      float endX = rectangle.x() + rectangle.width() - rectangle.paddingX() - layout.width() * 0.5F;
      float endY = rectangle.y() + rectangle.height() - rectangle.paddingY() - layout.height() * 0.5F;
      if (!(endX < startX) && !(endY < startY)) {
         int rows = Math.min(96, Math.max(1, (int)Math.floor((double)((endY - startY) / spacingY)) + 1));
         int columns = Math.min(96, Math.max(1, (int)Math.floor((double)((endX - startX) / spacingX)) + 1));

         for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
               float centerX = startX + (float)column * spacingX + randomSigned(rectangle.seed(), column, row, 1) * rectangle.jitterX();
               float centerY = startY + (float)row * spacingY + randomSigned(rectangle.seed(), column, row, 2) * rectangle.jitterY();
               float angle = (float)Math.toRadians((double)(randomSigned(rectangle.seed(), column, row, 3) * rectangle.rotationMaxDegrees()));
               float sin = (float)Math.sin((double)angle);
               float cos = (float)Math.cos((double)angle);
               float originX = centerX - layout.width() * 0.5F;
               float originY = centerY - layout.height() * 0.5F;
               int iconColor = withAlphaMultiplier(
                  rectangle.iconColor(), 1.0F - clamp((centerY - rectangle.y()) / Math.max(rectangle.height(), 1.0F), 0.0F, 1.0F) * 0.92F
               );

               for (TextRenderer.Glyph glyph : page.glyphs()) {
                  float x0 = originX + glyph.x0();
                  float y0 = originY + glyph.y0();
                  float x1 = originX + glyph.x1();
                  float y1 = originY + glyph.y1();
                  HalfIconRectangleRenderer.RotatedPoint topLeft = rotate(x0, y0, centerX, centerY, sin, cos);
                  HalfIconRectangleRenderer.RotatedPoint bottomLeft = rotate(x0, y1, centerX, centerY, sin, cos);
                  HalfIconRectangleRenderer.RotatedPoint bottomRight = rotate(x1, y1, centerX, centerY, sin, cos);
                  HalfIconRectangleRenderer.RotatedPoint topRight = rotate(x1, y0, centerX, centerY, sin, cos);
                  quads.add(
                     new HalfIconRectangleRenderState.IconQuad(
                        topLeft.x(),
                        topLeft.y(),
                        glyph.u0(),
                        glyph.v0(),
                        bottomLeft.x(),
                        bottomLeft.y(),
                        glyph.u0(),
                        glyph.v1(),
                        bottomRight.x(),
                        bottomRight.y(),
                        glyph.u1(),
                        glyph.v1(),
                        topRight.x(),
                        topRight.y(),
                        glyph.u1(),
                        glyph.v0(),
                        iconColor
                     )
                  );
               }
            }
         }

         return quads;
      } else {
         return List.of();
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltHalfIconRectangle> batch) {
      int usedBytes = Math.max(1, batch.size()) * 3 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < batch.size(); i++) {
         BuiltHalfIconRectangle rectangle = batch.get(i);
         int offset = i * 3 * 4 * 4;
         data.putFloat(offset, rectangle.x());
         data.putFloat(offset + 4, rectangle.y());
         data.putFloat(offset + 8, rectangle.width());
         data.putFloat(offset + 12, rectangle.height());
         data.putFloat(offset + 16, rectangle.radiusTopLeft());
         data.putFloat(offset + 20, rectangle.radiusTopRight());
         data.putFloat(offset + 24, rectangle.radiusBottomRight());
         data.putFloat(offset + 28, rectangle.radiusBottomLeft());
         data.putFloat(offset + 32, rectangle.smoothness());
         data.putFloat(offset + 36, 0.0F);
         data.putFloat(offset + 40, 0.0F);
         data.putFloat(offset + 44, 0.0F);
      }

      data.position(0);
      return data;
   }

   private GpuBuffer ensureParamsBuffer() {
      if (!this.paramsDirty && this.paramsBuffer != null) {
         return this.paramsBuffer;
      } else {
         this.prepareBuffers();
         if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
         } else {
            this.closeParamsBuffer();
            MemoryStack stack = MemoryStack.stackPush();

            GpuBuffer var3;
            try {
               ByteBuffer uniformData = this.buildUniformData(stack, this.preparedRectangles);
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_rect_half_icon_params", 128, uniformData);
               this.paramsDirty = false;
               var3 = this.paramsBuffer;
            } catch (Throwable var5) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (stack != null) {
               stack.close();
            }

            return var3;
         }
      }
   }

   private GpuBuffer ensureWritableParamsBuffer() {
      if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 12288L) {
         return this.paramsBuffer;
      } else {
         this.closeParamsBuffer();

         try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_rect_half_icon_params", 136, 12288L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private BuiltHalfIconRectangle normalize(BuiltHalfIconRectangle rectangle) {
      float maxRadius = Math.max(0.0F, Math.min(rectangle.width(), rectangle.height()) * 0.5F);
      float iconSize = Math.max(rectangle.iconSize(), 1.0F);
      float radiusTopLeft = clamp(rectangle.radiusTopLeft(), 0.0F, maxRadius);
      float radiusTopRight = clamp(rectangle.radiusTopRight(), 0.0F, maxRadius);
      float radiusBottomRight = clamp(rectangle.radiusBottomRight(), 0.0F, maxRadius);
      float radiusBottomLeft = clamp(rectangle.radiusBottomLeft(), 0.0F, maxRadius);
      float smoothness = Math.max(rectangle.smoothness(), 0.5F);
      float spacingX = Math.max(rectangle.spacingX(), iconSize);
      float spacingY = Math.max(rectangle.spacingY(), iconSize);
      float paddingX = Math.max(rectangle.paddingX(), 0.0F);
      float paddingY = Math.max(rectangle.paddingY(), 0.0F);
      float jitterX = Math.max(rectangle.jitterX(), 0.0F);
      float jitterY = Math.max(rectangle.jitterY(), 0.0F);
      float rotationMaxDegrees = clamp(rectangle.rotationMaxDegrees(), 0.0F, 90.0F);
      return radiusTopLeft == rectangle.radiusTopLeft()
            && radiusTopRight == rectangle.radiusTopRight()
            && radiusBottomRight == rectangle.radiusBottomRight()
            && radiusBottomLeft == rectangle.radiusBottomLeft()
            && smoothness == rectangle.smoothness()
            && iconSize == rectangle.iconSize()
            && spacingX == rectangle.spacingX()
            && spacingY == rectangle.spacingY()
            && paddingX == rectangle.paddingX()
            && paddingY == rectangle.paddingY()
            && jitterX == rectangle.jitterX()
            && jitterY == rectangle.jitterY()
            && rotationMaxDegrees == rectangle.rotationMaxDegrees()
         ? rectangle
         : new BuiltHalfIconRectangle(
            rectangle.x(),
            rectangle.y(),
            rectangle.width(),
            rectangle.height(),
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            rectangle.colorTopLeft(),
            rectangle.colorTopRight(),
            rectangle.colorBottomRight(),
            rectangle.colorBottomLeft(),
            smoothness,
            rectangle.fontName(),
            rectangle.icon(),
            iconSize,
            rectangle.iconColor(),
            spacingX,
            spacingY,
            paddingX,
            paddingY,
            jitterX,
            jitterY,
            rotationMaxDegrees,
            rectangle.seed()
         );
   }

   private static HalfIconRectangleRenderer.RotatedPoint rotate(float x, float y, float centerX, float centerY, float sin, float cos) {
      float localX = x - centerX;
      float localY = y - centerY;
      return new HalfIconRectangleRenderer.RotatedPoint(centerX + localX * cos - localY * sin, centerY + localX * sin + localY * cos);
   }

   private static float randomSigned(long seed, int column, int row, int salt) {
      long value = seed ^ (long)column * -7046029254386353131L ^ (long)row * -4658895280553007687L ^ (long)salt * -7723592293110705685L;
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      value ^= value >>> 31;
      return (float)(value & 16777215L) / 1.6777215E7F * 2.0F - 1.0F;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static int withAlphaMultiplier(int color, float multiplier) {
      int alpha = Math.round((float)(color >>> 24 & 0xFF) * clamp(multiplier, 0.0F, 1.0F));
      return alpha << 24 | color & 16777215;
   }

   private void closeParamsBuffer() {
      if (this.paramsBuffer != null) {
         this.paramsBuffer.close();
         this.paramsBuffer = null;
      }
   }

   private static Identifier id(String path) {
      return Identifier.of("daamky", path);
   }

   @Override
   public void close() {
      this.preparedRectangles.clear();
      this.frameBatches.clear();
      this.activeGraphics = null;
      this.closeParamsBuffer();
   }

   private static record FrameBatchKey(GuiRenderState state, int layerSerial, TextureSetup textureSetup, HalfIconRectangleRenderer.PoseKey pose) {
   }

   private static record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
      static HalfIconRectangleRenderer.PoseKey of(Matrix3x2f matrix) {
         return new HalfIconRectangleRenderer.PoseKey(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21());
      }
   }

   private static record RotatedPoint(float x, float y) {
   }
}
