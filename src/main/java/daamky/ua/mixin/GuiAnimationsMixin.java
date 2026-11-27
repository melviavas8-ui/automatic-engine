package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import daamky.ua.api.module.impl.visual.Animations;
import net.minecraft.client.gui.Gui;

@Mixin({Gui.class})
public abstract class GuiAnimationsMixin {
   public GuiAnimationsMixin() {
   }

   @ModifyExpressionValue(
      method = {"renderTabList"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/KeyMapping;isDown()Z"
      )}
   )
   private boolean daamky$keepTabVisibleForAnimation(boolean pressed) {
      return Animations.keepTabVisible(pressed);
   }
}
