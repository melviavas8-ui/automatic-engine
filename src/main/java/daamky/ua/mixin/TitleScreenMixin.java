package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.newmenu.mainmenu.CustomMainMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

@Mixin({Minecraft.class})
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
         ((Minecraft)(Object)this).setScreen(new CustomMainMenuScreen());
      }
   }
}

