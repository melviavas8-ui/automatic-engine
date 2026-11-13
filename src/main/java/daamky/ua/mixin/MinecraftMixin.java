package daamky.ua.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.utils.window.WindowStyle;

@Mixin({MinecraftClient.class})
public abstract class MinecraftMixin {
   public MinecraftMixin() {
   }

   @Inject(
      method = {"onResolutionChanged"},
      at = {@At("TAIL")}
   )
   private void applyDarkMode(CallbackInfo ci) {
      applyWindowTitle();
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.getWindow() != null) {
         WindowStyle.setDarkMode(client.getWindow().getHandle(), true);
      }
   }

   @Inject(
      method = {"updateWindowTitle"},
      at = {@At("TAIL")}
   )
   private void applyCustomTitle(CallbackInfo ci) {
      applyWindowTitle();
   }

   private static void applyWindowTitle() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.getWindow() != null) {
         String user = client.getSession() == null ? "User" : client.getSession().getUsername();
         if (user == null || user.isBlank()) {
            user = "User";
         }

         client.getWindow().setTitle("daamky Client - (" + user + ")");
      }
   }
}
