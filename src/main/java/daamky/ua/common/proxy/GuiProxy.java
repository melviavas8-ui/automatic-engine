package daamky.ua.common.proxy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class GuiProxy extends Screen {
   private final Screen parentScreen;
   private TextFieldWidget ipPort;
   private TextFieldWidget username;
   private TextFieldWidget password;
   private CheckboxWidget enabledCheck;
   private boolean socks4;
   private String message = "";
   private int[] positionY;
   private int positionX;

   public GuiProxy(Screen parentScreen) {
      super(Text.literal("Proxy"));
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
      if (!isValidIpPort(this.ipPort.getText())) {
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

   public boolean keyPressed(KeyInput input) {
      if (input.key() == 256) {
         MinecraftClient.getInstance().setScreen(this.parentScreen);
         return true;
      } else {
         this.message = "";
         return super.keyPressed(input);
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      context.drawTextWithShadow(this.textRenderer, "Enter IP address and port", this.width / 2 - 80, this.positionY[3] - 14, 10526880);
      context.drawTextWithShadow(this.textRenderer, "IP:Port", this.width / 2 - 137, this.positionY[3] + 16, 10526880);
      context.drawTextWithShadow(this.textRenderer, "Username", this.width / 2 - 137, this.positionY[4] + 16, 10526880);
      context.drawTextWithShadow(this.textRenderer, "Password", this.width / 2 - 137, this.positionY[5] + 16, 10526880);
      if (!this.message.isEmpty()) {
         context.drawCenteredTextWithShadow(this.textRenderer, this.message, this.width / 2, this.positionY[8], 16733525);
      }
   }

   protected void init() {
      int buttonLength = 160;
      this.centerButtons(10, buttonLength, 26);
      this.socks4 = ProxyServer.proxy.type == Proxy.ProxyType.SOCKS4 && ProxyServer.proxy.password.isEmpty();
      this.ipPort = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[3] + 10, buttonLength, 20, Text.literal(""));
      this.ipPort.setText(ProxyServer.proxy.ipPort);
      this.ipPort.setMaxLength(1024);
      this.ipPort.setFocused(true);
      this.addDrawableChild(this.ipPort);
      this.username = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[4] + 10, buttonLength, 20, Text.literal(""));
      this.username.setMaxLength(255);
      this.username.setText(ProxyServer.proxy.username);
      this.addDrawableChild(this.username);
      this.password = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[5] + 10, buttonLength, 20, Text.literal(""));
      this.password.setMaxLength(255);
      this.password.setText(ProxyServer.proxy.password);
      this.addDrawableChild(this.password);
      int buttonWidth = buttonLength / 2 - 3;
      int leftButtonX = this.width / 2 - buttonLength / 2;
      int rightButtonX = this.width / 2 + 3;
      this.addDrawableChild(ButtonWidget.builder(this.typeMessage(), button -> {
         this.socks4 = !this.socks4;
         button.setMessage(this.typeMessage());
      }).dimensions(leftButtonX, this.positionY[7] - 10, buttonWidth, 20).build());
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), button -> {
         if (!this.enabledCheck.isChecked() || this.checkProxy()) {
            boolean useSocks4 = this.socks4 && this.password.getText().isEmpty();
            ProxyServer.proxy = new Proxy(useSocks4, this.ipPort.getText(), this.username.getText(), this.password.getText());
            ProxyServer.proxyEnabled = this.enabledCheck.isChecked();
            Config.setDefaultProxy(ProxyServer.proxy);
            Config.saveConfig();
            Screen target = (Screen)(this.parentScreen == null ? new MultiplayerScreen(new TitleScreen()) : this.parentScreen);
            MinecraftClient.getInstance().setScreen(target);
         }
      }).dimensions(rightButtonX, this.positionY[7] - 10, buttonWidth, 20).build());
      this.enabledCheck = CheckboxWidget.builder(Text.literal("Enable proxy"), this.textRenderer)
         .pos(this.width / 2 - 48, this.positionY[7] + 16)
         .checked(ProxyServer.proxyEnabled || ProxyServer.proxy.hasAddress())
         .build();
      this.addDrawableChild(this.enabledCheck);
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Cancel"), button -> MinecraftClient.getInstance().setScreen(this.parentScreen))
            .dimensions(this.width / 2 - buttonWidth / 2, this.positionY[8] + 12, buttonWidth, 20)
            .build()
      );
   }

   public void close() {
      this.message = "";
      MinecraftClient.getInstance().setScreen(this.parentScreen);
   }

   private Text typeMessage() {
      return Text.literal(this.socks4 ? "SOCKS4" : "SOCKS5");
   }
}
