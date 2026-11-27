package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.command.CommandManager;
import daamky.ua.api.module.impl.visual.Animations;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;
import daamky.ua.utils.string.chat.ChatMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.CopyToClipboard;
import net.minecraft.network.chat.ClickEvent.RunCommand;

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
      method = {"renderWithTooltipAndSubtitles"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
      )}
   )
   private void daamky$beginOpeningTransform(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      Screen screen = (Screen)(Object)this;
      boolean menuAnimation = VanillaMenuTheme.usesOpeningAnimation(screen);
      boolean chatAnimation = screen instanceof ChatScreen && Animations.chatEnabled();
      boolean containerAnimation = Animations.screenEnabled(screen);
      if (menuAnimation || chatAnimation || containerAnimation) {
         float centerX = (float)screen.width * 0.5F;
         float centerY = (float)screen.height * 0.5F;
         graphics.pose().pushMatrix();
         if (menuAnimation) {
            float progress = Math.min(1.0F, (float)(System.nanoTime() - this.daamky$openedAtNanos) / 4.2E8F);
            float eased = 1.0F - (float)Math.pow((double)(1.0F - progress), 3.0);
            float scale = 0.925F + eased * 0.075F;
            graphics.pose().translate(centerX, centerY + (1.0F - eased) * 11.0F);
            graphics.pose().scale(scale, scale);
            graphics.pose().translate(-centerX, -centerY);
         }

         if (chatAnimation) {
            graphics.pose().translate(0.0F, Animations.chatOffset());
         }

         if (containerAnimation) {
            float scale = Animations.screenScale(screen);
            graphics.pose().translate(centerX, centerY);
            graphics.pose().scale(scale, scale);
            graphics.pose().translate(-centerX, -centerY);
         }

         this.daamky$openingPosePushed = true;
      }
   }

   @Inject(
      method = {"renderWithTooltipAndSubtitles"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;renderDeferredElements()V"
      )}
   )
   private void daamky$finishOpeningTransform(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (this.daamky$openingPosePushed) {
         graphics.pose().popMatrix();
         this.daamky$openingPosePushed = false;
      }
   }

   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderThemedBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      Screen screen = (Screen)(Object)this;
      if (VanillaMenuTheme.usesNeonBackground(screen)) {
         VanillaMenuTheme.renderBackground(graphics, screen.width, screen.height);
         ci.cancel();
      }
   }

   @Inject(
      method = {"defaultHandleClickEvent"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private static void daamky$handleClientClickEvent(ClickEvent clickEvent, Minecraft client, Screen screen, CallbackInfo ci) {
      if (clickEvent instanceof CopyToClipboard copyToClipboard) {
         String value = copyToClipboard.value();
         if (client != null && client.keyboardHandler != null && value != null && !value.isEmpty()) {
            client.keyboardHandler.setClipboard(value);
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
      method = {"onClose"},
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

