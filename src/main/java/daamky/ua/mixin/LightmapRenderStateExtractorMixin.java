package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.module.impl.visual.Ambience;
import daamky.ua.api.module.impl.visual.NoRender;
import net.minecraft.client.renderer.LightTexture;

@Mixin({LightTexture.class})
public class LightmapRenderStateExtractorMixin {
   public LightmapRenderStateExtractorMixin() {
   }

   @Redirect(
      method = {"updateLightTexture"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/Double;floatValue()F",
         ordinal = 1
      ),
      require = 0
   )
   private float daamky$getBrightness(Double value) {
      float baseValue = value.floatValue();
      Ambience ambience = Ambience.getInstance();
      if (ambience != null && ambience.isEnabled()) {
         float brightness = ambience.getBrightnessValue();
         return brightness >= 0.0F ? Math.max(baseValue, brightness * 10.0F) : Math.max(baseValue * (1.0F + brightness), 0.08F);
      } else {
         return baseValue;
      }
   }

   @Inject(
      method = {"calculateDarknessScale"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$removeDarkness(CallbackInfoReturnable<Float> cir) {
      if (NoRender.isActive("Darkness")) {
         cir.setReturnValue(0.0F);
      }
   }
}
