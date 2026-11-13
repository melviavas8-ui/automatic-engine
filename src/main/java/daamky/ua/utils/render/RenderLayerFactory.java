package daamky.ua.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderSetup.Builder;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import daamky.ua.mixin.accessor.MixinRenderType;

public final class RenderLayerFactory {
   private RenderLayerFactory() {
   }

   public static RenderLayer create(String name, int expectedBufferSize, RenderPipeline pipeline) {
      return create(name, expectedBufferSize, pipeline, null);
   }

   public static RenderLayer create(String name, int expectedBufferSize, RenderPipeline pipeline, @Nullable Identifier texture) {
      Builder builder = RenderSetup.builder(pipeline).expectedBufferSize(expectedBufferSize);
      if (texture != null) {
         builder.texture("Sampler0", texture);
      }

      return MixinRenderType.daamky$create(name, builder.build());
   }
}
