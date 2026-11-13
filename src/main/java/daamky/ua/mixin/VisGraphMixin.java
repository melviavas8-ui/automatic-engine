package daamky.ua.mixin;

import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.ChunkOcclusionEvent;
import daamky.ua.manager.Manager;

@Mixin({ChunkOcclusionDataBuilder.class})
public abstract class VisGraphMixin {
   public VisGraphMixin() {
   }

   @Inject(
      method = {"markClosed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$skipOpaqueOcclusion(BlockPos pos, CallbackInfo ci) {
      ChunkOcclusionEvent event = Manager.postEvent(new ChunkOcclusionEvent());
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
