package daamky.ua.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.drag.core.ElementManager;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;

@Mixin({ChatScreen.class})
public abstract class ChatScreenMixin {
   public ChatScreenMixin() {
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$dragMouseClicked(Click event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      // Legacy HUD elements are no longer rendered or interactive; DaamkyHud handles its own dragging.
   }

   @Inject(
      method = {"removed"},
      at = {@At("HEAD")},
      require = 0
   )
   private void daamky$cancelDragOnRemoved(CallbackInfo ci) {
      ElementManager.getInstance().cancelActiveElement();
   }

   @Inject(
      method = {"close"},
      at = {@At("HEAD")},
      require = 0
   )
   private void daamky$cancelDragOnClose(CallbackInfo ci) {
      ElementManager.getInstance().cancelActiveElement();
   }

   @Unique
   private Click daamky$toDragEvent(Click event) {
      float scale = Render2DCoordinateSpace.guiIndependentScale();
      if (Math.abs(scale - 1.0F) <= 1.0E-4F) {
         return event;
      } else {
         MouseInput info = new MouseInput(event.button(), event.modifiers());
         return new Click(event.x() / (double)scale, event.y() / (double)scale, info);
      }
   }
}
