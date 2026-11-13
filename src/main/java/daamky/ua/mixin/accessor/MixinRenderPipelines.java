package daamky.ua.mixin.accessor;

import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import net.minecraft.client.gl.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderPipelines.class})
public interface MixinRenderPipelines {
   @Accessor("RENDERTYPE_LINES_SNIPPET")
   static Snippet daamky$getLinesSnippet() {
      throw new AssertionError();
   }
}
