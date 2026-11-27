package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.platform.InputConstants.Type;
import daamky.ua.api.events.impl.KeyEvent;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.menu.MenuShaderBackgroundRenderer;
import daamky.ua.utils.render.world.blockoverlay.BlockOverlayShader3D;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;

@Mixin({KeyboardHandler.class})
public abstract class KeyboardHandlerMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   public KeyboardHandlerMixin() {
   }

   @Inject(
      method = {"keyPress"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$toggleClickGui(long window, int action, net.minecraft.client.input.KeyEvent event, CallbackInfo ci) {
      if (event.key() != -1 && window == this.minecraft.getWindow().handle()) {
         Manager.postEvent(new KeyEvent(this.minecraft.screen, Type.KEYSYM, event.key(), action));
      }

      if (action == 1 && window == this.minecraft.getWindow().handle() && event.key() == 297) {
         MenuShaderBackgroundRenderer.reload();
         BlockOverlayShader3D.reload();
         this.minecraft.reloadResourcePacks().thenRun(() -> {
            MenuShaderBackgroundRenderer.reload();
            BlockOverlayShader3D.reload();
         });
         ci.cancel();
      } else {
         if (action == 1 && event.key() == 344 && this.minecraft.player != null && this.minecraft.level != null) {
            Manager.toggleClickGui(this.minecraft);
            ci.cancel();
         }
      }
   }
}
