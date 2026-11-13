package daamky.ua.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.CopyToClipboard;
import net.minecraft.text.ClickEvent.RunCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.command.CommandManager;
import daamky.ua.api.module.impl.visual.Animations;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;
import daamky.ua.utils.string.chat.ChatMessage;

@Mixin({Screen.class})
public class ScreenMixin {
   @Unique
   private long daamky$openedAtNanos;
   @Unique
   private boolean daamky$openingPosePushed;

   public ScreenMixin() {
   }

   @Inject(
      method = {"init(II)V"},
      at = {@At("HEAD")}
   )
   private void daamky$startOpeningAnimation(int width, int height, CallbackInfo ci) {
      Screen screen = (Screen)(Object)this;
      if (VanillaMenuTheme.usesOpeningAnimation(screen)) {
         this.daamky$openedAtNanos = System.nanoTime();
      }
   }

   @Inject(
      method = {"renderWithTooltip"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"
      )}
   )
   private void daamky$beginOpeningTransform(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      Screen screen = (Screen)(Object)this;
      boolean menuAnimation = VanillaMenuTheme.usesOpeningAnimation(screen);
      boolean chatAnimation = screen instanceof ChatScreen && Animations.chatEnabled();
      boolean containerAnimation = Animations.screenEnabled(screen);
      if (menuAnimation || chatAnimation || containerAnimation) {
         float centerX = (float)screen.width * 0.5F;
         float centerY = (float)screen.height * 0.5F;
         graphics.getMatrices().pushMatrix();
         if (menuAnimation) {
            float progress = Math.min(1.0F, (float)(System.nanoTime() - this.daamky$openedAtNanos) / 4.2E8F);
            float eased = 1.0F - (float)Math.pow((double)(1.0F - progress), 3.0);
            float scale = 0.925F + eased * 0.075F;
            graphics.getMatrices().translate(centerX, centerY + (1.0F - eased) * 11.0F);
            graphics.getMatrices().scale(scale, scale);
            graphics.getMatrices().translate(-centerX, -centerY);
         }

         if (chatAnimation) {
            graphics.getMatrices().translate(0.0F, Animations.chatOffset());
         }

         if (containerAnimation) {
            float scale = Animations.screenScale(screen);
            graphics.getMatrices().translate(centerX, centerY);
            graphics.getMatrices().scale(scale, scale);
            graphics.getMatrices().translate(-centerX, -centerY);
         }

         this.daamky$openingPosePushed = true;
      }
   }

   @Inject(
      method = {"renderWithTooltip"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawDeferredElements()V"
      )}
   )
   private void daamky$finishOpeningTransform(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (this.daamky$openingPosePushed) {
         graphics.getMatrices().popMatrix();
         this.daamky$openingPosePushed = false;
      }
   }

   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderThemedBackground(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      Screen screen = (Screen)(Object)this;
      if (VanillaMenuTheme.usesNeonBackground(screen)) {
         VanillaMenuTheme.renderBackground(graphics, screen.width, screen.height);
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleBasicClickEvent"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private static void daamky$handleClientClickEvent(ClickEvent clickEvent, MinecraftClient client, Screen screen, CallbackInfo ci) {
      if (clickEvent instanceof CopyToClipboard copyToClipboard) {
         String value = copyToClipboard.value();
         if (client != null && client.keyboard != null && value != null && !value.isEmpty()) {
            client.keyboard.setClipboard(value);
            ChatMessage.brandmessage("Copied to clipboard.");
            ci.cancel();
            return;
         }
      }

      if (clickEvent instanceof RunCommand runCommand) {
         String command = runCommand.command();
         CommandManager manager = CommandManager.getInstance();
         if (manager != null && command != null && command.startsWith(manager.getPrefix())) {
            manager.execute(command.substring(manager.getPrefix().length()));
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"close"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$animateChatClose(CallbackInfo ci) {
      Screen screen = (Screen)(Object)this;
      if (screen instanceof ChatScreen && Animations.deferChatClose()) {
         ci.cancel();
      }
   }
}

