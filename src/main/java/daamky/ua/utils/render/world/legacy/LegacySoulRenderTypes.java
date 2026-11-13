package daamky.ua.utils.render.world.legacy;

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
import daamky.ua.utils.render.RenderLayerFactory;

public final class LegacySoulRenderTypes {
   private static final RenderPipeline LEGACY_SOUL_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.ENTITY_SNIPPET})
         .withLocation(Identifier.of("daamky", "pipeline/world/legacy/legacy_soul_entity"))
         .withVertexShader(Identifier.of("daamky", "world/legacy/legacy_soul_entity"))
         .withFragmentShader(Identifier.of("daamky", "world/legacy/legacy_soul_entity"))
         .withSampler("Sampler0")
         .withBlend(BlendFunction.LIGHTNING)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
         .build()
   );
   private static final Function<Identifier, RenderLayer> LEGACY_SOUL = Util.memoize(
      texture -> RenderLayerFactory.create("daamky_legacy_soul_entity", 2048, LEGACY_SOUL_PIPELINE, texture)
   );
   private static final RenderPipeline GLASS_ENTITY_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.ENTITY_SNIPPET})
         .withLocation(Identifier.of("daamky", "pipeline/world/legacy/glass_entity"))
         .withVertexShader(Identifier.of("daamky", "world/legacy/legacy_soul_entity"))
         .withFragmentShader(Identifier.of("daamky", "world/legacy/glass_entity"))
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
         .build()
   );
   private static final Function<Identifier, RenderLayer> GLASS_ENTITY = Util.memoize(
      texture -> RenderLayerFactory.create("daamky_glass_entity", 2048, GLASS_ENTITY_PIPELINE, texture)
   );

   private LegacySoulRenderTypes() {
   }

   public static RenderLayer entity(Identifier texture) {
      return LEGACY_SOUL.apply(texture);
   }

   public static RenderLayer glassEntity(Identifier texture) {
      return GLASS_ENTITY.apply(texture);
   }
}
