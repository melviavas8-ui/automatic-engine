package daamky.ua.mixin.accessor;

import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LayerRenderState.class})
public interface ItemLayerRenderStateAccessor {
   @Accessor("glint")
   Glint daamky$getFoilType();

   @Accessor("transform")
   Transformation daamky$getItemTransform();

   @Accessor("useLight")
   boolean daamky$getUsesBlockLight();

   @Accessor("specialModelType")
   SpecialModelRenderer<Object> daamky$getSpecialRenderer();

   @Accessor("tints")
   int[] daamky$getTintLayers();
}
