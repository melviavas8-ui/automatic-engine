package daamky.ua.common.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class GuiProxy extends Screen {
   private final Screen parentScreen;
   private EditBox ipPort;
   private EditBox username;
   private EditBox password;
   private Checkbox enabledCheck;
   private boolean socks4;
   private String message = "";
   private int[] positionY;
   private int positionX;

   public GuiProxy(Screen parentScreen) {
      super(Component.literal("Proxy"));
      this.parentScreen = parentScreen;
   }

   private static boolean isValidIpPort(String value) {
      if (value != null && !value.isBlank()) {
         int separator = value.lastIndexOf(58);
         if (separator > 0 && separator != value.length() - 1) {
            try {
               int port = Integer.parseInt(value.substring(separator + 1));
               return port > 0 && port <= 65535;
            } catch (NumberFormatException var3) {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean checkProxy() {
      if (!isValidIpPort(this.ipPort.getValue())) {
         this.message = "Invalid IP:Port";
         this.ipPort.setFocused(true);
         return false;
      } else {
         return true;
      }
   }

   private void centerButtons(int amount, int buttonLength, int gap) {
      this.positionX = this.width / 2 - buttonLength / 2;
      this.positionY = new int[amount];
      int center = (this.height + amount * gap) / 2;
      int buttonStarts = center - amount * gap;

      for (int i = 0; i != amount; i++) {
         this.positionY[i] = buttonStarts + gap * i;
      }
   }

   public boolean keyPressed(KeyEvent input) {
      if (input.key() == 256) {
         Minecraft.getInstance().setScreen(this.parentScreen);
         return true;
      } else {
         this.message = "";
         return super.keyPressed(input);
      }
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      context.drawString(this.font, "Enter IP address and port", this.width / 2 - 80, this.positionY[3] - 14, 10526880);
      context.drawString(this.font, "IP:Port", this.width / 2 - 137, this.positionY[3] + 16, 10526880);
      context.drawString(this.font, "Username", this.width / 2 - 137, this.positionY[4] + 16, 10526880);
      context.drawString(this.font, "Password", this.width / 2 - 137, this.positionY[5] + 16, 10526880);
      if (!this.message.isEmpty()) {
         context.drawCenteredString(this.font, this.message, this.width / 2, this.positionY[8], 16733525);
      }
   }

   protected void init() {
      int buttonLength = 160;
      this.centerButtons(10, buttonLength, 26);
      this.socks4 = ProxyServer.proxy.type == Proxy.ProxyType.SOCKS4 && ProxyServer.proxy.password.isEmpty();
      this.ipPort = new EditBox(this.font, this.positionX, this.positionY[3] + 10, buttonLength, 20, Component.literal(""));
      this.ipPort.setValue(ProxyServer.proxy.ipPort);
      this.ipPort.setMaxLength(1024);
      this.ipPort.setFocused(true);
      this.addRenderableWidget(this.ipPort);
      this.username = new EditBox(this.font, this.positionX, this.positionY[4] + 10, buttonLength, 20, Component.literal(""));
      this.username.setMaxLength(255);
      this.username.setValue(ProxyServer.proxy.username);
      this.addRenderableWidget(this.username);
      this.password = new EditBox(this.font, this.positionX, this.positionY[5] + 10, buttonLength, 20, Component.literal(""));
      this.password.setMaxLength(255);
      this.password.setValue(ProxyServer.proxy.password);
      this.addRenderableWidget(this.password);
      int buttonWidth = buttonLength / 2 - 3;
      int leftButtonX = this.width / 2 - buttonLength / 2;
      int rightButtonX = this.width / 2 + 3;
      this.addRenderableWidget(Button.builder(this.typeMessage(), button -> {
         this.socks4 = !this.socks4;
         button.setMessage(this.typeMessage());
      }).bounds(leftButtonX, this.positionY[7] - 10, buttonWidth, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Apply"), button -> {
         if (!this.enabledCheck.selected() || this.checkProxy()) {
            boolean useSocks4 = this.socks4 && this.password.getValue().isEmpty();
            ProxyServer.proxy = new Proxy(useSocks4, this.ipPort.getValue(), this.username.getValue(), this.password.getValue());
            ProxyServer.proxyEnabled = this.enabledCheck.selected();
            Config.setDefaultProxy(ProxyServer.proxy);
            Config.saveConfig();
            Screen target = (Screen)(this.parentScreen == null ? new JoinMultiplayerScreen(new TitleScreen()) : this.parentScreen);
            Minecraft.getInstance().setScreen(target);
         }
      }).bounds(rightButtonX, this.positionY[7] - 10, buttonWidth, 20).build());
      this.enabledCheck = Checkbox.builder(Component.literal("Enable proxy"), this.font)
         .pos(this.width / 2 - 48, this.positionY[7] + 16)
         .selected(ProxyServer.proxyEnabled || ProxyServer.proxy.hasAddress())
         .build();
      this.addRenderableWidget(this.enabledCheck);
      this.addRenderableWidget(
         Button.builder(Component.literal("Cancel"), button -> Minecraft.getInstance().setScreen(this.parentScreen))
            .bounds(this.width / 2 - buttonWidth / 2, this.positionY[8] + 12, buttonWidth, 20)
            .build()
      );
   }

   public void onClose() {
      this.message = "";
      Minecraft.getInstance().setScreen(this.parentScreen);
   }

   private Component typeMessage() {
      return Component.literal(this.socks4 ? "SOCKS4" : "SOCKS5");
   }
}
