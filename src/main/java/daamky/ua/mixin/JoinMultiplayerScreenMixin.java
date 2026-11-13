package daamky.ua.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.common.proxy.Config;
import daamky.ua.common.proxy.GuiProxy;
import daamky.ua.common.proxy.ProxyServer;

@Mixin({MultiplayerScreen.class})
public abstract class JoinMultiplayerScreenMixin extends Screen {
   @Shadow
   protected MultiplayerServerListWidget serverListWidget;

   protected JoinMultiplayerScreenMixin(Text title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void daamky$addProxyButton(CallbackInfo ci) {
      MinecraftClient client = MinecraftClient.getInstance();
      Config.selectProxyForPlayer(client.getSession().getUsername());
      MultiplayerScreen screen = (MultiplayerScreen)(Object)this;
      ProxyServer.proxyMenuButton = ButtonWidget.builder(
            Text.literal(ProxyServer.getButtonText()), button -> MinecraftClient.getInstance().setScreen(new GuiProxy(screen))
         )
         .dimensions(5, 5, 110, 20)
         .build();
      this.addDrawableChild(ProxyServer.proxyMenuButton);
   }

   @Inject(
      method = {"refreshWidgetPositions"},
      at = {@At("TAIL")}
   )
   private void daamky$keepServerListAboveButtons(CallbackInfo ci) {
      if (this.serverListWidget != null) {
         int top = this.serverListWidget.getY();
         int bottom = Math.max(top + 40, this.height - 80);
         this.serverListWidget.position(this.serverListWidget.getWidth(), bottom - top, this.serverListWidget.getX(), top);
      }
   }
}

