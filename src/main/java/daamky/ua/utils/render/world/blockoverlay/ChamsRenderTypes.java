package daamky.ua.utils.render.world.blockoverlay;

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

public final class ChamsRenderTypes {
   private static final RenderPipeline CHAMS_ENTITY_THROUGH_WALLS_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.ENTITY_SNIPPET})
         .withLocation(Identifier.of("daamky", "pipeline/world/block_overlay/chams_entity_through_walls"))
         .withVertexShader(Identifier.of("daamky", "world/block_overlay/chams_entity"))
         .withFragmentShader(Identifier.of("daamky", "world/block_overlay/chams_entity"))
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
         .build()
   );
   private static final RenderPipeline CHAMS_ENTITY_DEPTH_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.ENTITY_SNIPPET})
         .withLocation(Identifier.of("daamky", "pipeline/world/block_overlay/chams_entity_depth"))
         .withVertexShader(Identifier.of("daamky", "world/block_overlay/chams_entity"))
         .withFragmentShader(Identifier.of("daamky", "world/block_overlay/chams_entity"))
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
         .build()
   );
   private static final Function<Identifier, RenderLayer> CHAMS_ENTITY_THROUGH_WALLS = Util.memoize(
      texture -> RenderLayerFactory.create("daamky_chams_entity_through_walls", 2048, CHAMS_ENTITY_THROUGH_WALLS_PIPELINE, texture)
   );
   private static final Function<Identifier, RenderLayer> CHAMS_ENTITY_DEPTH = Util.memoize(
      texture -> RenderLayerFactory.create("daamky_chams_entity_depth", 2048, CHAMS_ENTITY_DEPTH_PIPELINE, texture)
   );

   private ChamsRenderTypes() {
   }

   public static RenderLayer entity(Identifier texture, boolean throughWalls) {
      return throughWalls ? CHAMS_ENTITY_THROUGH_WALLS.apply(texture) : CHAMS_ENTITY_DEPTH.apply(texture);
   }
}
