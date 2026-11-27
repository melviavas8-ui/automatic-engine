package daamky.ua.mixin.accessor;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LayerRenderState.class})
public interface ItemLayerRenderStateAccessor {
   @Accessor("foilType")
   FoilType daamky$getFoilType();

   @Accessor("transform")
   ItemTransform daamky$getItemTransform();

   @Accessor("usesBlockLight")
   boolean daamky$getUsesBlockLight();

   @Accessor("specialRenderer")
   SpecialModelRenderer<Object> daamky$getSpecialRenderer();

   @Accessor("tintLayers")
   int[] daamky$getTintLayers();
}
