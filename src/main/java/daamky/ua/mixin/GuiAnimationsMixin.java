package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import daamky.ua.api.module.impl.visual.Animations;

@Mixin({InGameHud.class})
public abstract class GuiAnimationsMixin {
   public GuiAnimationsMixin() {
   }

   @ModifyExpressionValue(
      method = {"renderPlayerList"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"
      )}
   )
   private boolean daamky$keepTabVisibleForAnimation(boolean pressed) {
      return Animations.keepTabVisible(pressed);
   }
}
