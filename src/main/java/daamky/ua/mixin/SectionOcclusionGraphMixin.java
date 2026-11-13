package daamky.ua.mixin;

import net.minecraft.client.render.ChunkRenderingDataPreparer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import daamky.ua.api.events.impl.ChunkOcclusionEvent;
import daamky.ua.manager.Manager;

@Mixin({ChunkRenderingDataPreparer.class})
public abstract class SectionOcclusionGraphMixin {
   public SectionOcclusionGraphMixin() {
   }

   @ModifyVariable(
      method = {"updateSectionOcclusionGraph"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private boolean daamky$disableAdvancedCulling(boolean advancedCulling) {
      ChunkOcclusionEvent event = Manager.postEvent(new ChunkOcclusionEvent());
      return event.isCancelled() ? false : advancedCulling;
   }
}
