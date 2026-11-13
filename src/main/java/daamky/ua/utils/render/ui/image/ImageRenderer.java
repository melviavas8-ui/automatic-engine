package daamky.ua.utils.render.ui.image;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import daamky.ua.access.GuiRenderStateLayerAccessor;
import daamky.ua.mixin.accessor.GuiGraphicsExtractorAccessor;
import daamky.ua.utils.render.ScissorUtil;
import daamky.ua.utils.render.ui.GuiResourceLoader;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;

public final class ImageRenderer implements AutoCloseable {
   private static final String daamky_NAMESPACE = "daamky";
   private static final String[] TEXTURE_SEARCH_ROOTS = new String[]{
      "textures/features/particles/", "textures/features/predictions/particles/", "textures/particles/", "textures/world/"
   };
   private static final int MAX_IMAGES = 1024;
   private static final int PARAMS_PER_IMAGE = 3;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 49152;
   private static volatile ImageRenderer instance;
   private static final VertexFormat IMAGE_VERTEX_FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline IMAGE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/image"))
      .withVertexShader(id("ui/image/image"))
      .withFragmentShader(id("ui/image/image"))
      .withVertexFormat(IMAGE_VERTEX_FORMAT, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ImageParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final Map<ImageRenderer.TextureCacheKey, ImageRenderer.CachedTexture> textures = new HashMap<>();
   private final Map<ImageRenderer.FrameBatchKey, ImageRenderState> frameBatches = new LinkedHashMap<>(32);
   private final List<ImageQuad> preparedImages = new ArrayList<>(128);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty;

   private ImageRenderer() {
   }

   public static ImageRenderer getInstance() {
      ImageRenderer local = instance;
      if (local == null) {
         synchronized (ImageRenderer.class) {
            local = instance;
            if (local == null) {
               local = new ImageRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      ImageRenderer local = instance;
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

   public void enqueue(BuiltImage image) {
      this.submit(this.activeGraphics, image);
   }

   public void flush() {
      this.activeGraphics = null;
      this.frameBatches.clear();
   }

   public void barrier() {
      this.frameBatches.clear();
   }

   public void beginGuiFrame() {
      this.preparedImages.clear();
      this.paramsDirty = false;
   }

   public boolean isImagePipeline(RenderPipeline pipeline) {
      return pipeline == IMAGE_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedImages.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("ImageParamsArray", buffer);
         }
      }
   }

   public void prepareBuffers() {
      if (!this.preparedImages.isEmpty() && this.paramsDirty) {
         GpuBuffer buffer = this.ensureWritableParamsBuffer();
         if (buffer != null) {
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer uniformData = this.buildUniformData(stack);
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

   int reserve(ImageQuad image) {
      int index = this.preparedImages.size();
      if (index == 1024) {
         return -1;
      } else {
         this.preparedImages.add(image);
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltImage image) {
      if (graphics != null && image != null && image.visible()) {
         ImageTexture texture = this.resolveTexture(image.texture(), image.nearestFilter());
         if (texture != null) {
            float width = image.hasExplicitDimensions() ? image.explicitWidth() : texture.drawWidth(image.size());
            float height = image.hasExplicitDimensions() ? image.explicitHeight() : texture.drawHeight(image.size());
            if (!(width <= 0.0F) && !(height <= 0.0F)) {
               float maxRadius = Math.max(0.0F, Math.min(width, height) * 0.5F);
               float rotation = image.rotationDegrees();
               float radians = (float)Math.toRadians((double)rotation);
               ImageQuad quad = new ImageQuad(
                  image.x(),
                  image.y(),
                  width,
                  height,
                  clamp(image.radius(), 0.0F, maxRadius),
                  Math.max(0.001F, image.smoothness()),
                  rotation,
                  image.rotationOriginX(),
                  image.rotationOriginY(),
                  (float)Math.cos((double)radians),
                  (float)Math.sin((double)radians),
                  clamp(image.u0(), 0.0F, 1.0F),
                  clamp(image.v0(), 0.0F, 1.0F),
                  clamp(image.u1(), 0.0F, 1.0F),
                  clamp(image.v1(), 0.0F, 1.0F),
                  normalizeColor(image.colorTopLeft()),
                  normalizeColor(image.colorTopRight()),
                  normalizeColor(image.colorBottomRight()),
                  normalizeColor(image.colorBottomLeft())
               );

               try {
                  GuiRenderState guiState = ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState();
                  int layerSerial = ((GuiRenderStateLayerAccessor)guiState).daamky$getLayerSerial();
                  Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
                  ScreenRect scissorArea = ScissorUtil.current();
                  ImageRenderer.FrameBatchKey key = new ImageRenderer.FrameBatchKey(
                     guiState, layerSerial, texture.id(), texture.nearestFilter(), ImageRenderer.PoseKey.of(pose), scissorArea
                  );
                  ImageRenderState state = this.frameBatches.get(key);
                  if (state == null) {
                     state = new ImageRenderState(pose, texture, scissorArea);
                     state.add(quad);
                     this.frameBatches.put(key, state);
                     guiState.addSimpleElement(state);
                  } else {
                     state.add(quad);
                  }
               } catch (RuntimeException var16) {
               }
            }
         }
      }
   }

   private ImageTexture resolveTexture(String path, boolean nearestFilter) {
      Identifier id = this.resolveIdentifier(path);
      if (id == null) {
         return null;
      } else {
         ImageTexture classpathTexture = this.resolveClasspathTexture(id, nearestFilter);
         if (classpathTexture != null) {
            return classpathTexture;
         } else {
            MinecraftClient minecraft = MinecraftClient.getInstance();
            if (minecraft == null) {
               return null;
            } else {
               TextureManager textureManager = minecraft.getTextureManager();
               if (textureManager == null) {
                  return null;
               } else {
                  AbstractTexture texture = textureManager.getTexture(id);
                  if (texture != null && texture.getGlTextureView() != null && texture.getGlTexture() != null) {
                     ImageRenderer.TextureCacheKey key = new ImageRenderer.TextureCacheKey(id, nearestFilter);
                     ImageRenderer.CachedTexture cached = this.textures.get(key);
                     if (cached != null && cached.texture() == texture) {
                        return cached.value();
                     } else {
                        int width = Math.max(1, texture.getGlTexture().getWidth(0));
                        int height = Math.max(1, texture.getGlTexture().getHeight(0));
                        TextureSetup setup = TextureSetup.of(
                           texture.getGlTextureView(), RenderSystem.getSamplerCache().get(nearestFilter ? FilterMode.NEAREST : FilterMode.LINEAR)
                        );
                        ImageTexture value = new ImageTexture(id, setup, nearestFilter, width, height);
                        this.textures.put(key, new ImageRenderer.CachedTexture(texture, value, false));
                        return value;
                     }
                  } else {
                     return null;
                  }
               }
            }
         }
      }
   }

   private ImageTexture resolveClasspathTexture(Identifier id, boolean nearestFilter) {
      if (!"daamky".equals(id.getNamespace())) {
         return null;
      } else {
         ImageRenderer.TextureCacheKey key = new ImageRenderer.TextureCacheKey(id, nearestFilter);
         ImageRenderer.CachedTexture cached = this.textures.get(key);
         if (cached != null && cached.owned()) {
            return cached.value();
         } else if (!GuiResourceLoader.exists(id)) {
            return null;
         } else {
            NativeImageBackedTexture texture = GuiResourceLoader.loadTexture(id, "daamky_gui_image_");
            if (texture != null && texture.getGlTextureView() != null && texture.getGlTexture() != null) {
               int width = Math.max(1, texture.getGlTexture().getWidth(0));
               int height = Math.max(1, texture.getGlTexture().getHeight(0));
               TextureSetup setup = TextureSetup.of(
                  texture.getGlTextureView(), RenderSystem.getSamplerCache().get(nearestFilter ? FilterMode.NEAREST : FilterMode.LINEAR)
               );
               ImageTexture value = new ImageTexture(id, setup, nearestFilter, width, height);
               this.textures.put(key, new ImageRenderer.CachedTexture(texture, value, true));
               return value;
            } else {
               return null;
            }
         }
      }
   }

   private Identifier resolveIdentifier(String rawPath) {
      if (rawPath != null && !rawPath.isBlank()) {
         String path = rawPath.trim().replace('\\', '/');
         if (path.startsWith("/")) {
            path = path.substring(1);
         }

         if (path.startsWith("assets/")) {
            path = path.substring("assets/".length());
            int slash = path.indexOf(47);
            if (slash >= 0) {
               String namespace = path.substring(0, slash);
               String resourcePath = path.substring(slash + 1);
               return Identifier.of(namespace, resourcePath);
            }
         }

         if (path.indexOf(58) >= 0) {
            return Identifier.tryParse(path);
         } else {
            Identifier textureIdentifier = this.resolveTextureShortcut(path);
            if (textureIdentifier != null) {
               return textureIdentifier;
            } else {
               if (!path.startsWith("images/")) {
                  path = "images/" + path;
               }

               if (!path.contains(".")) {
                  path = path + ".png";
               }

               return Identifier.of("daamky", path);
            }
         }
      } else {
         return null;
      }
   }

   private Identifier resolveTextureShortcut(String rawPath) {
      if (rawPath.indexOf(47) >= 0) {
         return null;
      } else {
         String normalizedPath = rawPath.toLowerCase(Locale.ROOT);
         if (!normalizedPath.contains(".")) {
            normalizedPath = normalizedPath + ".png";
         }

         for (String root : TEXTURE_SEARCH_ROOTS) {
            Identifier identifier = Identifier.of("daamky", root + normalizedPath);
            if (this.resourceExists(identifier)) {
               return identifier;
            }
         }

         return null;
      }
   }

   private boolean resourceExists(Identifier identifier) {
      if ("daamky".equals(identifier.getNamespace()) && GuiResourceLoader.exists(identifier)) {
         return true;
      } else {
         MinecraftClient minecraft = MinecraftClient.getInstance();
         if (minecraft == null) {
            return false;
         } else {
            ResourceManager resourceManager = minecraft.getResourceManager();
            return resourceManager != null && resourceManager.getResource(identifier).isPresent();
         }
      }
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
               ByteBuffer uniformData = this.buildUniformData(stack);
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_image_params", 128, uniformData);
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
      if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 49152L) {
         return this.paramsBuffer;
      } else {
         this.closeParamsBuffer();

         try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_image_params", 136, 49152L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack) {
      int usedBytes = Math.max(1, this.preparedImages.size()) * 3 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < this.preparedImages.size(); i++) {
         ImageQuad image = this.preparedImages.get(i);
         int offset = i * 3 * 4 * 4;
         data.putFloat(offset, image.radius());
         data.putFloat(offset + 4, image.radius());
         data.putFloat(offset + 8, image.radius());
         data.putFloat(offset + 12, image.radius());
         data.putFloat(offset + 16, image.width());
         data.putFloat(offset + 20, image.height());
         data.putFloat(offset + 24, image.smoothness());
         data.putFloat(offset + 28, 0.0F);
         data.putFloat(offset + 32, image.u0());
         data.putFloat(offset + 36, image.v0());
         data.putFloat(offset + 40, image.u1());
         data.putFloat(offset + 44, image.v1());
      }

      data.position(0);
      return data;
   }

   private static int normalizeColor(int color) {
      return (color & 0xFF000000) == 0 && (color & 16777215) != 0 ? color | 0xFF000000 : color;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
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
      this.frameBatches.clear();
      this.preparedImages.clear();

      for (ImageRenderer.CachedTexture cached : this.textures.values()) {
         if (cached.owned()) {
            cached.texture().close();
         }
      }

      this.textures.clear();
      this.activeGraphics = null;
      this.closeParamsBuffer();
   }

   private static record CachedTexture(AbstractTexture texture, ImageTexture value, boolean owned) {
   }

   private static record FrameBatchKey(
      GuiRenderState state, int layerSerial, Identifier texture, boolean nearestFilter, ImageRenderer.PoseKey pose, ScreenRect scissorArea
   ) {
   }

   private static record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
      static ImageRenderer.PoseKey of(Matrix3x2f matrix) {
         return new ImageRenderer.PoseKey(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21());
      }
   }

   private static record TextureCacheKey(Identifier id, boolean nearestFilter) {
   }
}
