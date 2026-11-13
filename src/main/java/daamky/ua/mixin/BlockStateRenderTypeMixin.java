package daamky.ua.mixin;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.module.impl.visual.NoRender;

@Mixin({AbstractBlockState.class})
public abstract class BlockStateRenderTypeMixin {
   public BlockStateRenderTypeMixin() {
   }

   @Inject(
      method = {"getRenderType"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$getRenderShape(CallbackInfoReturnable<BlockRenderType> cir) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHidePlants() && noRender.isPlantState((BlockState)(Object)this)) {
         cir.setReturnValue(BlockRenderType.INVISIBLE);
      }
   }
}

