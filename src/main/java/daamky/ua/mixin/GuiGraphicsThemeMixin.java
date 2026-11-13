package daamky.ua.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;

@Mixin({DrawContext.class})
public abstract class GuiGraphicsThemeMixin {
   public GuiGraphicsThemeMixin() {
   }

   @Inject(
      method = {"drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderServerListControl(RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height, CallbackInfo ci) {
      if (width == 32 && height == 32 && VanillaMenuTheme.shouldHideServerListOverlay(texture)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderRoundedListIcon(
      RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, CallbackInfo ci
   ) {
      if (VanillaMenuTheme.shouldRoundListIcon(width, height, textureWidth, textureHeight)) {
         VanillaMenuTheme.renderRoundedListIcon((DrawContext)(Object)this, texture.toString(), (float)x, (float)y);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedCenteredSequence(TextRenderer font, OrderedText text, int centerX, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawCenteredText((DrawContext)(Object)this, plainText(text), (float)centerX, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedCenteredComponent(TextRenderer font, Text text, int centerX, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawCenteredText((DrawContext)(Object)this, text.getString(), (float)centerX, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedCenteredString(TextRenderer font, String text, int centerX, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawCenteredText((DrawContext)(Object)this, text, (float)centerX, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedComponent(TextRenderer font, Text text, int x, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawText((DrawContext)(Object)this, text.getString(), (float)x, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedString(TextRenderer font, String text, int x, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawText((DrawContext)(Object)this, text, (float)x, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedSequence(TextRenderer font, OrderedText text, int x, int y, int color, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawText((DrawContext)(Object)this, plainText(text), (float)x, (float)y, color);
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$drawThemedSequenceWithShadow(TextRenderer font, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         VanillaMenuTheme.drawText((DrawContext)(Object)this, plainText(text), (float)x, (float)y, color);
         ci.cancel();
      }
   }

   private static String plainText(OrderedText sequence) {
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

