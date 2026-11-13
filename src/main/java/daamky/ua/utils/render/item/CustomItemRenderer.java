package daamky.ua.utils.render.item;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;
import daamky.ua.mixin.accessor.ItemLayerRenderStateAccessor;
import daamky.ua.mixin.accessor.ItemStackRenderStateAccessor;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;

public final class CustomItemRenderer implements AutoCloseable {
   private static final int MAX_ITEM_QUADS = 4096;
   private static final int MAX_GEOMETRY_CACHE_ENTRIES = 512;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 65536;
   private static final float MIN_VISIBLE_AREA = 2.0E-6F;
   private static final float MIN_BOUNDS_SPAN = 1.0E-4F;
   private static final float PRESERVED_MODEL_SCALE_CAP = 1.08F;
   private static final float PRESERVED_MODEL_DEPTH_EPSILON = 0.02F;
   private static final float SHADE_UP = 0.98F;
   private static final float SHADE_SIDE_LIGHT = 0.9F;
   private static final float SHADE_SIDE = 0.84F;
   private static final float SHADE_SIDE_DARK = 0.78F;
   private static final float SHADE_DOWN = 0.72F;
   private static volatile CustomItemRenderer instance;
   private static final VertexFormat ITEM_VERTEX_FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline ITEM_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/item"))
      .withVertexShader(id("ui/item/item"))
      .withFragmentShader(id("ui/item/item"))
      .withVertexFormat(ITEM_VERTEX_FORMAT, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ItemParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final Map<Identifier, CustomItemRenderer.CachedTexture> textures = new HashMap<>();
   private final Map<CustomItemRenderer.GeometryCacheKey, CachedItemGeometry> geometryCache = new LinkedHashMap<CustomItemRenderer.GeometryCacheKey, CachedItemGeometry>(
      256, 0.75F, true
   ) {
      @Override
      protected boolean removeEldestEntry(Entry<CustomItemRenderer.GeometryCacheKey, CachedItemGeometry> eldest) {
         return this.size() > 512;
      }
   };
   private final Map<CustomItemRenderer.FrameBatchKey, ItemRenderState> frameBatches = new LinkedHashMap<>(32);
   private final float[] preparedGlints = new float[4096];
   private int preparedQuadCount;
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty;
   private CustomItemRenderer.ServerCacheKey lastServerCacheKey;

   private CustomItemRenderer() {
   }

   public static CustomItemRenderer getInstance() {
      CustomItemRenderer local = instance;
      if (local == null) {
         synchronized (CustomItemRenderer.class) {
            local = instance;
            if (local == null) {
               local = new CustomItemRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      CustomItemRenderer local = instance;
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

   public void enqueue(BuiltRenderItem item) {
      this.submit(this.activeGraphics, item);
   }

   public void flush() {
      this.activeGraphics = null;
      this.frameBatches.clear();
   }

   public void beginGuiFrame() {
      this.preparedQuadCount = 0;
      this.paramsDirty = false;
   }

   public boolean isItemPipeline(RenderPipeline pipeline) {
      return pipeline == ITEM_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && this.preparedQuadCount != 0) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("ItemParamsArray", buffer);
         }
      }
   }

   public void prepareBuffers() {
      if (this.preparedQuadCount != 0 && this.paramsDirty) {
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

   int reserve(float glintStrength) {
      int index = this.preparedQuadCount;
      if (index == 4096) {
         return -1;
      } else {
         this.preparedGlints[index] = glintStrength;
         this.preparedQuadCount++;
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltRenderItem item) {
      if (graphics != null && item != null && item.visible()) {
         MinecraftClient minecraft = MinecraftClient.getInstance();
         if (minecraft != null && minecraft.getItemModelManager() != null) {
            this.refreshServerCacheKey(minecraft);

            try {
               this.submitSpecialItem(graphics, item);
            } catch (RuntimeException var5) {
            }
         }
      }
   }

   private CachedItemGeometry resolveGeometry(MinecraftClient minecraft, BuiltRenderItem item) {
      KeyedItemRenderState renderState = this.buildRenderState(minecraft, item.stack(), item.seed());
      CustomItemRenderer.GeometryCacheKey key = CustomItemRenderer.GeometryCacheKey.of(item.stack(), item.seed(), renderState.getModelKey());
      CachedItemGeometry cached = this.geometryCache.get(key);
      if (cached != null) {
         return cached;
      } else {
         CachedItemGeometry geometry = this.buildGeometry(item.stack(), renderState);
         if (!geometry.animated()) {
            this.geometryCache.put(key, geometry);
         }

         return geometry;
      }
   }

   private KeyedItemRenderState buildRenderState(MinecraftClient minecraft, ItemStack stack, int seed) {
      KeyedItemRenderState renderState = new KeyedItemRenderState();
      minecraft.getItemModelManager().clearAndUpdate(renderState, stack, ItemDisplayContext.GUI, minecraft.world, minecraft.player, seed);
      return renderState;
   }

   private CachedItemGeometry buildGeometry(ItemStack stack, net.minecraft.client.render.item.ItemRenderState renderState) {
      ItemStackRenderStateAccessor stateAccessor = (ItemStackRenderStateAccessor)renderState;
      LayerRenderState[] layers = stateAccessor.daamky$getLayers();
      int activeLayerCount = Math.min(stateAccessor.daamky$getActiveLayerCount(), layers.length);

      for (int i = 0; i < activeLayerCount; i++) {
         if (this.hasSpecialRenderer(layers[i])) {
            return new CachedItemGeometry(List.of(), renderState.isAnimated(), true);
         }
      }

      List<CustomItemRenderer.RawItemQuad> rawQuads = new ArrayList<>(Math.max(8, activeLayerCount * 8));
      CustomItemRenderer.Bounds bounds = new CustomItemRenderer.Bounds();

      for (int ix = 0; ix < activeLayerCount; ix++) {
         this.collectLayerGeometry(rawQuads, bounds, layers[ix], stack);
      }

      rawQuads.sort(Comparator.comparingDouble(CustomItemRenderer.RawItemQuad::depth));
      CustomItemRenderer.NormalizedBounds normalizedBounds = CustomItemRenderer.NormalizedBounds.of(bounds);
      List<CachedItemQuad> quads = new ArrayList<>(rawQuads.size());

      for (CustomItemRenderer.RawItemQuad rawQuad : rawQuads) {
         quads.add(this.buildCachedQuad(rawQuad, normalizedBounds));
      }

      return new CachedItemGeometry(List.copyOf(quads), renderState.isAnimated(), false);
   }

   private boolean hasSpecialRenderer(LayerRenderState layer) {
      if (layer == null) {
         return false;
      } else {
         ItemLayerRenderStateAccessor accessor = (ItemLayerRenderStateAccessor)layer;
         return accessor.daamky$getSpecialRenderer() != null;
      }
   }

   private void collectLayerGeometry(List<CustomItemRenderer.RawItemQuad> output, CustomItemRenderer.Bounds bounds, LayerRenderState layer, ItemStack stack) {
      if (layer != null) {
         List<BakedQuad> quads = layer.getQuads();
         if (!quads.isEmpty()) {
            ItemLayerRenderStateAccessor accessor = (ItemLayerRenderStateAccessor)layer;
            Glint foilType = accessor.daamky$getFoilType();
            boolean foil = foilType != Glint.NONE;
            int[] tints = preparedTints(layer, accessor, maxTintLayer(quads));
            Matrix4f transform = this.layerTransform(accessor);
            boolean preserveModelScale = accessor.daamky$getUsesBlockLight();
            if (preserveModelScale) {
               bounds.markPreserveModelScale();
            }

            for (BakedQuad quad : quads) {
               CustomItemRenderer.RawItemQuad rawQuad = this.buildRawQuad(quad, transform, tintColor(quad, tints, stack), foil, preserveModelScale);
               if (!(rawQuad.area() < 2.0E-6F)) {
                  bounds.include(rawQuad.x0(), rawQuad.y0(), rawQuad.z0());
                  bounds.include(rawQuad.x1(), rawQuad.y1(), rawQuad.z1());
                  bounds.include(rawQuad.x2(), rawQuad.y2(), rawQuad.z2());
                  bounds.include(rawQuad.x3(), rawQuad.y3(), rawQuad.z3());
                  output.add(rawQuad);
               }
            }
         }
      }
   }

   private void submitQuad(
      GuiRenderState guiState,
      int layerSerial,
      Matrix3x2f pose,
      CustomItemRenderer.PoseKey poseKey,
      ItemTexture texture,
      CachedItemQuad quad,
      float x,
      float y,
      float size,
      int color,
      float glintStrength
   ) {
      CustomItemRenderer.FrameBatchKey key = new CustomItemRenderer.FrameBatchKey(guiState, layerSerial, texture.id(), poseKey);
      ItemRenderState state = this.frameBatches.get(key);
      if (state == null) {
         state = new ItemRenderState(pose, texture);
         state.add(quad, x, y, size, color, glintStrength);
         this.frameBatches.put(key, state);
         guiState.addSimpleElement(state);
      } else {
         state.add(quad, x, y, size, color, glintStrength);
      }
   }

   private void submitSpecialItem(DrawContext graphics, BuiltRenderItem item) {
      float scale = item.size() / 16.0F;
      if (!(scale <= 0.0F)) {
         graphics.getMatrices().pushMatrix();

         try {
            Render2DCoordinateSpace.applyGuiScaleIndependence(graphics.getMatrices());
            graphics.getMatrices().translate(item.x(), item.y());
            graphics.getMatrices().scale(scale);
            graphics.drawItem(item.stack(), 0, 0, item.seed());
         } finally {
            graphics.getMatrices().popMatrix();
         }
      }
   }

   private CustomItemRenderer.RawItemQuad buildRawQuad(BakedQuad quad, Matrix4f transform, int tint, boolean foil, boolean shadeModel) {
      Vector3fc p0 = quad.position0();
      Vector3fc p1 = quad.position1();
      Vector3fc p2 = quad.position2();
      Vector3fc p3 = quad.position3();
      Vector3f t0 = transformPosition(p0, transform);
      Vector3f t1 = transformPosition(p1, transform);
      Vector3f t2 = transformPosition(p2, transform);
      Vector3f t3 = transformPosition(p3, transform);
      long uv0 = quad.packedUV0();
      long uv1 = quad.packedUV1();
      long uv2 = quad.packedUV2();
      long uv3 = quad.packedUV3();
      float depth = (t0.z + t1.z + t2.z + t3.z) * 0.25F;
      int shadedTint = shadeModel ? shadeColor(tint, faceShade(quad.face())) : tint;
      return new CustomItemRenderer.RawItemQuad(
         quad.sprite().getAtlasId(),
         t0.x,
         t0.y,
         Vector2f.getX(uv0),
         Vector2f.getY(uv0),
         t1.x,
         t1.y,
         Vector2f.getX(uv1),
         Vector2f.getY(uv1),
         t2.x,
         t2.y,
         Vector2f.getX(uv2),
         Vector2f.getY(uv2),
         t3.x,
         t3.y,
         Vector2f.getX(uv3),
         Vector2f.getY(uv3),
         shadedTint,
         foil,
         depth,
         projectedArea(t0.x, t0.y, t1.x, t1.y, t2.x, t2.y, t3.x, t3.y),
         t0.z,
         t1.z,
         t2.z,
         t3.z
      );
   }

   private CachedItemQuad buildCachedQuad(CustomItemRenderer.RawItemQuad quad, CustomItemRenderer.NormalizedBounds bounds) {
      return new CachedItemQuad(
         quad.atlas(),
         bounds.x(quad.x0()),
         bounds.y(quad.y0()),
         quad.u0(),
         quad.v0(),
         bounds.x(quad.x1()),
         bounds.y(quad.y1()),
         quad.u1(),
         quad.v1(),
         bounds.x(quad.x2()),
         bounds.y(quad.y2()),
         quad.u2(),
         quad.v2(),
         bounds.x(quad.x3()),
         bounds.y(quad.y3()),
         quad.u3(),
         quad.v3(),
         quad.tint(),
         quad.foil()
      );
   }

   private Matrix4f layerTransform(ItemLayerRenderStateAccessor accessor) {
      net.minecraft.client.util.math.MatrixStack.Entry pose = new net.minecraft.client.util.math.MatrixStack.Entry();
      Transformation transform = accessor.daamky$getItemTransform();
      if (transform == null) {
         transform = Transformation.IDENTITY;
      }

      transform.apply(false, pose);
      return new Matrix4f(pose.getPositionMatrix());
   }

   private static Vector3f transformPosition(Vector3fc position, Matrix4f transform) {
      return new Vector3f(position).mulPosition(transform);
   }

   private ItemTexture resolveTexture(Identifier atlas) {
      if (atlas == null) {
         return null;
      } else {
         MinecraftClient minecraft = MinecraftClient.getInstance();
         if (minecraft == null) {
            return null;
         } else {
            TextureManager textureManager = minecraft.getTextureManager();
            if (textureManager == null) {
               return null;
            } else {
               AbstractTexture texture = textureManager.getTexture(atlas);
               if (texture != null && texture.getGlTextureView() != null) {
                  CustomItemRenderer.CachedTexture cached = this.textures.get(atlas);
                  if (cached != null && cached.texture() == texture) {
                     return cached.value();
                  } else {
                     TextureSetup setup = TextureSetup.of(texture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                     ItemTexture value = new ItemTexture(atlas, setup);
                     this.textures.put(atlas, new CustomItemRenderer.CachedTexture(texture, value));
                     return value;
                  }
               } else {
                  return null;
               }
            }
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
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_item_params", 128, uniformData);
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
      if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 65536L) {
         return this.paramsBuffer;
      } else {
         this.closeParamsBuffer();

         try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_item_params", 136, 65536L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack) {
      int usedBytes = Math.max(1, this.preparedQuadCount) * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);
      float time = (float)(System.nanoTime() % 30000000000L) / 1.0E9F;

      for (int i = 0; i < this.preparedQuadCount; i++) {
         int offset = i * 4 * 4;
         data.putFloat(offset, this.preparedGlints[i]);
         data.putFloat(offset + 4, time);
         data.putFloat(offset + 8, 0.0F);
         data.putFloat(offset + 12, 0.0F);
      }

      data.position(0);
      return data;
   }

   private static int maxTintLayer(List<BakedQuad> quads) {
      int max = 0;

      for (BakedQuad quad : quads) {
         if (quad.hasTint()) {
            max = Math.max(max, quad.tintIndex() + 1);
         }
      }

      return max;
   }

   private static int[] preparedTints(LayerRenderState layer, ItemLayerRenderStateAccessor accessor, int tintLayerCount) {
      int[] tints = accessor.daamky$getTintLayers();
      if (tints == null || tints.length < tintLayerCount) {
         tints = layer.initTints(tintLayerCount);
      }

      return tints;
   }

   private static int tintColor(BakedQuad quad, int[] tints, ItemStack stack) {
      int tint = -1;
      if (quad.hasTint()) {
         int tintIndex = quad.tintIndex();
         if (tintIndex >= 0 && tintIndex < tints.length) {
            tint = normalizeColor(tints[tintIndex]);
         }

         if (isPotionStack(stack) && !isPotionOverlayQuad(quad)) {
            return -1;
         }

         if (isWhiteTint(tint) && isPotionOverlayQuad(quad)) {
            tint = potionTint(stack, tint);
         }
      }

      return tint;
   }

   private static boolean isPotionStack(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.contains(DataComponentTypes.POTION_CONTENTS);
   }

   private static boolean isPotionOverlayQuad(BakedQuad quad) {
      return quad != null && quad.sprite() != null && quad.sprite().getContents() != null && quad.sprite().getContents().getId() != null
         ? "minecraft:item/potion_overlay".equals(quad.sprite().getContents().getId().toString())
         : false;
   }

   private static int potionTint(ItemStack stack, int fallback) {
      if (stack != null && !stack.isEmpty()) {
         PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         return contents == null ? fallback : 0xFF000000 | contents.getColor() & 16777215;
      } else {
         return fallback;
      }
   }

   private static boolean isWhiteTint(int color) {
      return normalizeColor(color) == -1;
   }

   private static int multiplyColor(int base, int tint, float alphaMultiplier) {
      base = normalizeColor(base);
      tint = normalizeColor(tint);
      int a = Math.round((float)(base >>> 24 & 0xFF) * ((float)(tint >>> 24 & 0xFF) / 255.0F) * alphaMultiplier);
      int r = (base >>> 16 & 0xFF) * (tint >>> 16 & 0xFF) / 255;
      int g = (base >>> 8 & 0xFF) * (tint >>> 8 & 0xFF) / 255;
      int b = (base & 0xFF) * (tint & 0xFF) / 255;
      return clampInt(a, 0, 255) << 24 | r << 16 | g << 8 | b;
   }

   private static int normalizeColor(int color) {
      return (color & 0xFF000000) == 0 && (color & 16777215) != 0 ? color | 0xFF000000 : color;
   }

   private static float projectedArea(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
      return Math.abs(cross(x0, y0, x1, y1) + cross(x1, y1, x2, y2) + cross(x2, y2, x3, y3) + cross(x3, y3, x0, y0)) * 0.5F;
   }

   private static float cross(float ax, float ay, float bx, float by) {
      return ax * by - bx * ay;
   }

   private static float faceShade(Direction direction) {
      if (direction == null) {
         return 0.84F;
      } else {
         return switch (direction) {
            case UP -> 0.98F;
            case DOWN -> 0.72F;
            case NORTH -> 0.9F;
            case SOUTH -> 0.84F;
            case EAST, WEST -> 0.78F;
            default -> throw new MatchException(null, null);
         };
      }
   }

   private static int shadeColor(int color, float shade) {
      color = normalizeColor(color);
      int a = color >>> 24 & 0xFF;
      int r = clampInt(Math.round((float)(color >>> 16 & 0xFF) * shade), 0, 255);
      int g = clampInt(Math.round((float)(color >>> 8 & 0xFF) * shade), 0, 255);
      int b = clampInt(Math.round((float)(color & 0xFF) * shade), 0, 255);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static int clampInt(int value, int min, int max) {
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
      this.preparedQuadCount = 0;
      this.geometryCache.clear();
      this.textures.clear();
      this.activeGraphics = null;
      this.closeParamsBuffer();
   }

   public void clearCaches() {
      this.geometryCache.clear();
      this.textures.clear();
   }

   private void refreshServerCacheKey(MinecraftClient minecraft) {
      CustomItemRenderer.ServerCacheKey key = CustomItemRenderer.ServerCacheKey.of(minecraft);
      if (!key.equals(this.lastServerCacheKey)) {
         this.geometryCache.clear();
         this.textures.clear();
         this.lastServerCacheKey = key;
      }
   }

   private static final class Bounds {
      private float minX = Float.MAX_VALUE;
      private float minY = Float.MAX_VALUE;
      private float minZ = Float.MAX_VALUE;
      private float maxX = -Float.MAX_VALUE;
      private float maxY = -Float.MAX_VALUE;
      private float maxZ = -Float.MAX_VALUE;
      private boolean preserveModelScale;

      private Bounds() {
      }

      private void include(float x, float y, float z) {
         this.minX = Math.min(this.minX, x);
         this.minY = Math.min(this.minY, y);
         this.minZ = Math.min(this.minZ, z);
         this.maxX = Math.max(this.maxX, x);
         this.maxY = Math.max(this.maxY, y);
         this.maxZ = Math.max(this.maxZ, z);
      }

      private boolean defined() {
         return this.minX <= this.maxX && this.minY <= this.maxY;
      }

      private void markPreserveModelScale() {
         this.preserveModelScale = true;
      }

      private boolean preserveModelScale() {
         return this.preserveModelScale || this.maxZ - this.minZ > 0.02F;
      }
   }

   private static record CachedTexture(AbstractTexture texture, ItemTexture value) {
   }

   private static record FrameBatchKey(GuiRenderState state, int layerSerial, Identifier texture, CustomItemRenderer.PoseKey pose) {
   }

   private static record GeometryCacheKey(Item item, ComponentMap components, int count, int seed, Object modelIdentity) {
      static CustomItemRenderer.GeometryCacheKey of(ItemStack stack, int seed, Object modelIdentity) {
         return new CustomItemRenderer.GeometryCacheKey(
            stack.getItem(), stack.getImmutableComponents(), stack.getCount(), seed, stableModelIdentity(modelIdentity)
         );
      }

      private static Object stableModelIdentity(Object modelIdentity) {
         return modelIdentity instanceof List<?> list ? List.copyOf(list) : modelIdentity;
      }
   }

   private static record NormalizedBounds(float minX, float maxY, float scale, float offsetX, float offsetY) {
      private static CustomItemRenderer.NormalizedBounds of(CustomItemRenderer.Bounds bounds) {
         if (!bounds.defined()) {
            return new CustomItemRenderer.NormalizedBounds(0.0F, 1.0F, 1.0F, 0.0F, 0.0F);
         } else {
            float width = Math.max(1.0E-4F, bounds.maxX - bounds.minX);
            float height = Math.max(1.0E-4F, bounds.maxY - bounds.minY);
            float span = Math.max(width, height);
            if (bounds.preserveModelScale()) {
               float scale = Math.min(1.0F / span, 1.08F);
               return new CustomItemRenderer.NormalizedBounds((bounds.minX + bounds.maxX) * 0.5F, (bounds.minY + bounds.maxY) * 0.5F, scale, 0.5F, 0.5F);
            } else {
               float scale = 1.0F / span;
               return new CustomItemRenderer.NormalizedBounds(bounds.minX, bounds.maxY, scale, (1.0F - width * scale) * 0.5F, (1.0F - height * scale) * 0.5F);
            }
         }
      }

      private float x(float value) {
         return this.offsetX + (value - this.minX) * this.scale;
      }

      private float y(float value) {
         return this.offsetY + (this.maxY - value) * this.scale;
      }
   }

   private static record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
      static CustomItemRenderer.PoseKey of(Matrix3x2f matrix) {
         return new CustomItemRenderer.PoseKey(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21());
      }
   }

   private static record RawItemQuad(
      Identifier atlas,
      float x0,
      float y0,
      float u0,
      float v0,
      float x1,
      float y1,
      float u1,
      float v1,
      float x2,
      float y2,
      float u2,
      float v2,
      float x3,
      float y3,
      float u3,
      float v3,
      int tint,
      boolean foil,
      float depth,
      float area,
      float z0,
      float z1,
      float z2,
      float z3
   ) {
   }

   private static record ServerCacheKey(String address, int protocol) {
      static CustomItemRenderer.ServerCacheKey of(MinecraftClient minecraft) {
         return minecraft != null && minecraft.getNetworkHandler() != null && minecraft.getNetworkHandler().getServerInfo() != null
            ? new CustomItemRenderer.ServerCacheKey(
               minecraft.getNetworkHandler().getServerInfo().address, minecraft.getNetworkHandler().getServerInfo().protocolVersion
            )
            : new CustomItemRenderer.ServerCacheKey("", 0);
      }
   }
}
