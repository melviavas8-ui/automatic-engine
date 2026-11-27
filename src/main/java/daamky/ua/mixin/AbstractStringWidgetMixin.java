package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.StringWidget;

@Mixin({AbstractStringWidget.class})
public abstract class AbstractStringWidgetMixin {
   public AbstractStringWidgetMixin() {
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderThemedHeading(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         AbstractStringWidget widget = (AbstractStringWidget)(Object)this;
         if (widget instanceof StringWidget) {
            VanillaMenuTheme.renderHeading(graphics, widget);
            ci.cancel();
         }
      }
   }
}

