package daamky.ua.utils.render.pipeline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.function.Function;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import daamky.ua.utils.render.RenderCompatibility;
import daamky.ua.utils.render.RenderLayerFactory;

public final class ClientPipelines {
   public static final RenderPipeline ROMB_ESP_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/wtex")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(worldBlend())
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> ROMB_ESP = Util.memoize(texture -> RenderLayerFactory.create("wtex", 1536, ROMB_ESP_PIPELINE, texture));
   public static final RenderPipeline CLICKGUI_CLOSE_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/clickgui_close")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> CLICKGUI_CLOSE = Util.memoize(
      texture -> RenderLayerFactory.create("clickgui_close", 1536, CLICKGUI_CLOSE_PIPELINE, texture)
   );
   public static final RenderPipeline GHOSTS_ESP_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/wtex")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(worldBlend())
         .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> GHOSTS_ESP = Util.memoize(
      texture -> RenderLayerFactory.create("wtex", 1536, GHOSTS_ESP_PIPELINE, texture)
   );
   public static final RenderPipeline CHAIN_ESP_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/wtex")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(worldBlend())
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> CHAIN_ESP = Util.memoize(
      texture -> RenderLayerFactory.create("wtex", 1536, CHAIN_ESP_PIPELINE, texture)
   );
   public static final RenderPipeline TARGET_LIGHTNING_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation(Identifier.of("daamky", "pipeline/targetesp_lightning"))
         .withVertexShader(Identifier.of("daamky", "world/targetesp/lightning"))
         .withFragmentShader(Identifier.of("daamky", "world/targetesp/lightning"))
         .withBlend(worldBlend())
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final RenderLayer TARGET_LIGHTNING = RenderLayerFactory.create("targetesp_lightning", 16384, TARGET_LIGHTNING_PIPELINE);
   public static final RenderPipeline TARGET_LIGHTNING_GLOW_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation(Identifier.of("daamky", "pipeline/targetesp_lightning_glow"))
         .withVertexShader(Identifier.of("daamky", "world/targetesp/lightning"))
         .withFragmentShader(Identifier.of("daamky", "world/targetesp/lightning_glow"))
         .withBlend(worldBlend())
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final RenderLayer TARGET_LIGHTNING_GLOW = RenderLayerFactory.create("targetesp_lightning_glow", 16384, TARGET_LIGHTNING_GLOW_PIPELINE);
   public static final RenderPipeline CRYSTAL_FILLED_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/crystal_filled")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final RenderLayer CRYSTAL_FILLED = RenderLayerFactory.create("crystal_filled", 8192, CRYSTAL_FILLED_PIPELINE);
   public static final RenderPipeline CRYSTAL_FILLED_TRIANGLES_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/crystal_filled_triangles")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.TRIANGLES)
         .build()
   );
   public static final RenderLayer CRYSTAL_FILLED_TRIANGLES = RenderLayerFactory.create("crystal_filled_triangles", 8192, CRYSTAL_FILLED_TRIANGLES_PIPELINE);
   public static final RenderPipeline CRYSTAL_GLOW_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/crystal_glow")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(worldBlend())
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final RenderLayer CRYSTAL_GLOW = RenderLayerFactory.create("crystal_glow", 4096, CRYSTAL_GLOW_PIPELINE);
   public static final RenderPipeline BLOOM_ESP_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/bloom_esp")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(worldBlend())
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> BLOOM_ESP = Util.memoize(
      texture -> RenderLayerFactory.create("bloom_esp", 2048, BLOOM_ESP_PIPELINE, texture)
   );
   public static final RenderPipeline CHINA_HAT_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/china_hat")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(true)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.TRIANGLE_FAN)
         .build()
   );
   public static final RenderLayer CHINA_HAT = RenderLayerFactory.create("china_hat", 8192, CHINA_HAT_PIPELINE);
   public static final RenderPipeline CHINA_HAT_OUTLINE_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/china_hat_outline")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(true)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINE_STRIP)
         .build()
   );
   public static final RenderLayer CHINA_HAT_OUTLINE = RenderLayerFactory.create("china_hat_outline", 4096, CHINA_HAT_OUTLINE_PIPELINE);
   public static final RenderPipeline WORLD_PARTICLES_COLOR_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
         .withLocation(Identifier.of("daamky", "world_particles_color"))
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withBlend(worldBlend())
         .build()
   );
   public static final RenderLayer WORLD_PARTICLES_QUADS = RenderLayerFactory.create("world_particles_cube", 2048, WORLD_PARTICLES_COLOR_PIPELINE);
   public static final RenderPipeline WORLD_PARTICLES_LINES_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
         .withLocation(Identifier.of("daamky", "world_particles_lines"))
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withBlend(worldBlend())
         .build()
   );
   public static final RenderLayer WORLD_PARTICLES_LINES = RenderLayerFactory.create("world_particles_lines", 2048, WORLD_PARTICLES_LINES_PIPELINE);
   public static final RenderPipeline WORLD_PARTICLES_GLOW_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_TEX_COLOR_SNIPPET})
         .withLocation(Identifier.of("daamky", "world_particles_glow"))
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withBlend(worldBlend())
         .withSampler("Sampler0")
         .build()
   );
   public static final Function<Identifier, RenderLayer> WORLD_PARTICLES_GLOW = Util.memoize(
      texture -> RenderLayerFactory.create("world_particles_glow", 2048, WORLD_PARTICLES_GLOW_PIPELINE, texture)
   );
   public static final RenderPipeline GUI_ARROW_BLEND_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/gui_arrow_blend")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.LIGHTNING)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> GUI_ARROW_BLEND = Util.memoize(
      texture -> RenderLayerFactory.create("gui_arrow_blend", 256, GUI_ARROW_BLEND_PIPELINE, texture)
   );
   public static final RenderPipeline TRAILS_ALPHA_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
         .withLocation(Identifier.of("daamky", "trails_alpha"))
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withBlend(BlendFunction.TRANSLUCENT)
         .build()
   );
   public static final RenderLayer TRAILS_ALPHA = RenderLayerFactory.create("trails_alpha", 16384, TRAILS_ALPHA_PIPELINE);

   private ClientPipelines() {
   }

   private static BlendFunction worldBlend() {
      return RenderCompatibility.useSafeWorldEffects() ? BlendFunction.TRANSLUCENT : BlendFunction.LIGHTNING;
   }
}
