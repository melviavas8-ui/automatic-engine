package daamky.ua.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.KeyEvent;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.menu.MenuShaderBackgroundRenderer;
import daamky.ua.utils.render.world.blockoverlay.BlockOverlayShader3D;

@Mixin({Keyboard.class})
public abstract class KeyboardHandlerMixin {
   @Shadow
   @Final
   private MinecraftClient client;

   public KeyboardHandlerMixin() {
   }

   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$toggleClickGui(long window, int action, KeyInput event, CallbackInfo ci) {
      if (event.key() != -1 && window == this.client.getWindow().getHandle()) {
         Manager.postEvent(new KeyEvent(this.client.currentScreen, Type.KEYSYM, event.key(), action));
      }

      if (action == 1 && window == this.client.getWindow().getHandle() && event.key() == 297) {
         MenuShaderBackgroundRenderer.reload();
         BlockOverlayShader3D.reload();
         this.client.reloadResources().thenRun(() -> {
            MenuShaderBackgroundRenderer.reload();
            BlockOverlayShader3D.reload();
         });
         ci.cancel();
      } else {
         if (action == 1 && event.key() == 344 && this.client.player != null && this.client.world != null) {
            Manager.toggleClickGui(this.client);
            ci.cancel();
         }
      }
   }
}
