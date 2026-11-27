package daamky.ua.mixin;

import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import daamky.ua.utils.render.hands.ShaderHandMaterialRenderer;

@Mixin({LayerRenderState.class})
public abstract class ItemLayerRenderStateMaterialMixin {
   public ItemLayerRenderStateMaterialMixin() {
   }

   @ModifyArg(
      method = {"submit"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"
      ),
      index = 7,
      require = 0
   )
   private RenderType daamky$useShaderHandMaterial(RenderType original) {
      List<BakedQuad> quads = ((LayerRenderState)(Object)this).prepareQuadList();
      return ShaderHandMaterialRenderer.itemLayer(original, quads);
   }
}

