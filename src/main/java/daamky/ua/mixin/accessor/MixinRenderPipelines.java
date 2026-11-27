package daamky.ua.mixin.accessor;

import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderPipelines.class})
public interface MixinRenderPipelines {
   @Accessor("LINES_SNIPPET")
   static Snippet daamky$getLinesSnippet() {
      throw new AssertionError();
   }
}
