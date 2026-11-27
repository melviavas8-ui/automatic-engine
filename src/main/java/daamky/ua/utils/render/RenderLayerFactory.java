package daamky.ua.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jetbrains.annotations.Nullable;
import daamky.ua.mixin.accessor.MixinRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderSetup.RenderSetupBuilder;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class RenderLayerFactory {
   private RenderLayerFactory() {
   }

   public static RenderType create(String name, int expectedBufferSize, RenderPipeline pipeline) {
      return create(name, expectedBufferSize, pipeline, null);
   }

   public static RenderType create(String name, int expectedBufferSize, RenderPipeline pipeline, @Nullable Identifier texture) {
      RenderSetupBuilder builder = RenderSetup.builder(pipeline).bufferSize(expectedBufferSize);
      if (texture != null) {
         builder.withTexture("Sampler0", texture);
      }

      return MixinRenderType.daamky$create(name, builder.createRenderSetup());
   }
}
