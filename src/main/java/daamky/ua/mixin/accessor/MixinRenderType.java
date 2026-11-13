package daamky.ua.mixin.accessor;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({RenderLayer.class})
public interface MixinRenderType {
   @Invoker("of")
   static RenderLayer daamky$create(String name, RenderSetup setup) {
      throw new AssertionError();
   }
}
