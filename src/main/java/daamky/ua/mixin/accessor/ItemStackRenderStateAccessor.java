package daamky.ua.mixin.accessor;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ItemStackRenderState.class})
public interface ItemStackRenderStateAccessor {
   @Accessor("activeLayerCount")
   int daamky$getActiveLayerCount();

   @Accessor("layers")
   LayerRenderState[] daamky$getLayers();
}
