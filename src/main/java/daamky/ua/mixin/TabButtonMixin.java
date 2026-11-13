package daamky.ua.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TabButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;

@Mixin({TabButtonWidget.class})
public abstract class TabButtonMixin {
   public TabButtonMixin() {
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderThemedTab(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         TabButtonWidget tab = (TabButtonWidget)(Object)this;
         VanillaMenuTheme.renderButton(graphics, tab, !tab.isSelected() && !tab.isCurrentTab() ? 0.0F : 1.0F);
         ci.cancel();
      }
   }
}

