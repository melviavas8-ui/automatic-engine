package daamky.ua.mixin;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.Ambience;

@Mixin({CloudRenderer.class})
public abstract class CloudRendererMixin {
   public CloudRendererMixin() {
   }

   @Inject(
      method = {"renderClouds"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$hideAmbienceClouds(
      int color, CloudRenderMode cloudRenderMode, float cloudHeight, Vec3d cameraPos, long cloudTickCounter, float ticks, CallbackInfo ci
   ) {
      Ambience ambience = Ambience.getInstance();
      if (ambience != null && ambience.shouldHideClouds()) {
         ci.cancel();
      }
   }
}
