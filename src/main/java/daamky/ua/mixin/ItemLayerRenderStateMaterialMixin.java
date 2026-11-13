package daamky.ua.mixin;

import java.util.List;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.client.render.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import daamky.ua.utils.render.hands.ShaderHandMaterialRenderer;

@Mixin({LayerRenderState.class})
public abstract class ItemLayerRenderStateMaterialMixin {
   public ItemLayerRenderStateMaterialMixin() {
   }

   @ModifyArg(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"
      ),
      index = 7,
      require = 0
   )
   private RenderLayer daamky$useShaderHandMaterial(RenderLayer original) {
      List<BakedQuad> quads = ((LayerRenderState)(Object)this).getQuads();
      return ShaderHandMaterialRenderer.itemLayer(original, quads);
   }
}

