package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;

@Mixin({AbstractSliderButton.class})
public abstract class AbstractSliderButtonMixin {
   @Shadow
   protected double value;

   public AbstractSliderButtonMixin() {
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderThemedSlider(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         AbstractSliderButton slider = (AbstractSliderButton)(Object)this;
         VanillaMenuTheme.renderSlider(graphics, slider, this.value, slider.isHoveredOrFocused() ? 1.0F : 0.0F);
         ci.cancel();
      }
   }
}

