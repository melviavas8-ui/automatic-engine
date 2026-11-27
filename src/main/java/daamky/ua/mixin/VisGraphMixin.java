package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.ChunkOcclusionEvent;
import daamky.ua.manager.Manager;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;

@Mixin({VisGraph.class})
public abstract class VisGraphMixin {
   public VisGraphMixin() {
   }

   @Inject(
      method = {"setOpaque"},
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
