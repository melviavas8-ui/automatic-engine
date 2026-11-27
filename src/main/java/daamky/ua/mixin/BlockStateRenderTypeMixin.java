package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.module.impl.visual.NoRender;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;

@Mixin({BlockStateBase.class})
public abstract class BlockStateRenderTypeMixin {
   public BlockStateRenderTypeMixin() {
   }

   @Inject(
      method = {"getRenderShape"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$getRenderShape(CallbackInfoReturnable<RenderShape> cir) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHidePlants() && noRender.isPlantState((BlockState)(Object)this)) {
         cir.setReturnValue(RenderShape.INVISIBLE);
      }
   }
}

