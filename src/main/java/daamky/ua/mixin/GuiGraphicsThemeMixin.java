package daamky.ua.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

@Mixin({GuiGraphics.class})
public abstract class GuiGraphicsThemeMixin {
   public GuiGraphicsThemeMixin() {
   }

   @Inject(
      method = {"blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderServerListControl(RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height, CallbackInfo ci) {
      if (width == 32 && height == 32 && VanillaMenuTheme.shouldHideServerListOverlay(texture)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderRoundedListIcon(
      RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, CallbackInfo ci
   ) {
      if (VanillaMenuTheme.shouldRoundListIcon(width, height, textureWidth, textureHeight)) {
         VanillaMenuTheme.renderRoundedListIcon((GuiGraphics)(Object)this, texture.toString(), (float)x, (float)y);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedCenteredSequence(Font font, FormattedCharSequence text, int centerX, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawCenteredText((GuiGraphics)(Object)this, plainText(text), (float)centerX, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedCenteredComponent(Font font, Component text, int centerX, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawCenteredText((GuiGraphics)(Object)this, text.getString(), (float)centerX, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedCenteredString(Font font, String text, int centerX, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawCenteredText((GuiGraphics)(Object)this, text, (float)centerX, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedComponent(Font font, Component text, int x, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawText((GuiGraphics)(Object)this, text.getString(), (float)x, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedString(Font font, String text, int x, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawText((GuiGraphics)(Object)this, text, (float)x, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedSequence(Font font, FormattedCharSequence text, int x, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawText((GuiGraphics)(Object)this, plainText(text), (float)x, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedSequenceWithShadow(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawText((GuiGraphics)(Object)this, plainText(text), (float)x, (float)y, color);
         ci.cancel();
      }
   }

   private static String plainText(FormattedCharSequence sequence) {
      if (sequence == null) {
         return "";
      } else {
         StringBuilder text = new StringBuilder();
         sequence.accept((index, style, codePoint) -> {
            text.appendCodePoint(codePoint);
            return true;
         });
         return text.toString();
      }
   }
}

