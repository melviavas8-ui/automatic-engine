package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.common.proxy.Config;
import daamky.ua.common.proxy.GuiProxy;
import daamky.ua.common.proxy.ProxyServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;

@Mixin({JoinMultiplayerScreen.class})
public abstract class JoinMultiplayerScreenMixin extends Screen {
   @Shadow
   protected ServerSelectionList serverSelectionList;

   protected JoinMultiplayerScreenMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void daamky$addProxyButton(CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      Config.selectProxyForPlayer(client.getUser().getName());
      JoinMultiplayerScreen screen = (JoinMultiplayerScreen)(Object)this;
      ProxyServer.proxyMenuButton = Button.builder(
            Component.literal(ProxyServer.getButtonText()), button -> Minecraft.getInstance().setScreen(new GuiProxy(screen))
         )
         .bounds(5, 5, 110, 20)
         .build();
      this.addRenderableWidget(ProxyServer.proxyMenuButton);
   }

   @Inject(
      method = {"repositionElements"},
      at = {@At("TAIL")}
   )
   private void daamky$keepServerListAboveButtons(CallbackInfo ci) {
      if (this.serverSelectionList != null) {
         int top = this.serverSelectionList.getY();
         int bottom = Math.max(top + 40, this.height - 80);
         this.serverSelectionList.updateSizeAndPosition(this.serverSelectionList.getWidth(), bottom - top, this.serverSelectionList.getX(), top);
      }
   }
}

