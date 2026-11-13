package daamky.ua.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.DaamkyMainMenuScreen;

@Mixin({MinecraftClient.class})
public abstract class TitleScreenMixin {
   public TitleScreenMixin() {
   }

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void replaceVanillaTitleScreen(Screen screen, CallbackInfo ci) {
      if (screen instanceof TitleScreen) {
         ci.cancel();
         ((MinecraftClient)(Object)this).setScreen(new DaamkyMainMenuScreen());
      }
   }
}

