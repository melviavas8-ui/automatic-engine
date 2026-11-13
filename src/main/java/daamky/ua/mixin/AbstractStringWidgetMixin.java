package daamky.ua.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AbstractTextWidget;
import net.minecraft.client.gui.widget.TextWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;

@Mixin({AbstractTextWidget.class})
public abstract class AbstractStringWidgetMixin {
   public AbstractStringWidgetMixin() {
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderThemedHeading(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         AbstractTextWidget widget = (AbstractTextWidget)(Object)this;
         if (widget instanceof TextWidget) {
            VanillaMenuTheme.renderHeading(graphics, widget);
            ci.cancel();
         }
      }
   }
}

