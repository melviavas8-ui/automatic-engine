package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

@Mixin({EditBox.class})
public abstract class EditBoxMixin {
   public EditBoxMixin() {
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderThemedEditBox(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.renderEditBox(graphics, (EditBox)(Object)this);
         ci.cancel();
      }
   }
}

