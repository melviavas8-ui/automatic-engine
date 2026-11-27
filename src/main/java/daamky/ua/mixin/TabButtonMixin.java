package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.TabButton;

@Mixin({TabButton.class})
public abstract class TabButtonMixin {
   public TabButtonMixin() {
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderThemedTab(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         TabButton tab = (TabButton)(Object)this;
         VanillaMenuTheme.renderButton(graphics, tab, !tab.isHoveredOrFocused() && !tab.isSelected() ? 0.0F : 1.0F);
         ci.cancel();
      }
   }
}

