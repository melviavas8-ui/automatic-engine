package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.Ambience;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;

@Mixin({CloudRenderer.class})
public abstract class CloudRendererMixin {
   public CloudRendererMixin() {
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$hideAmbienceClouds(
      int color, CloudStatus cloudRenderMode, float cloudHeight, Vec3 cameraPos, long cloudTickCounter, float ticks, CallbackInfo ci
   ) {
      Ambience ambience = Ambience.getInstance();
      if (ambience != null && ambience.shouldHideClouds()) {
         ci.cancel();
      }
   }
}
