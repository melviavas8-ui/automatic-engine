package daamky.ua.screens.modernui.settings;

import java.awt.Color;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

final class ColorSettingCard extends SettingCardComponent<ColorSetting> {
   ColorSettingCard(ColorSetting setting) {
      super(setting);
   }

   @Override
   int order() {
      return 4;
   }

   @Override
   float height(SettingCardContext context) {
      return 16.5F;
   }

   @Override
   void render(DrawContext graphics, SettingCardContext context, float cardX, float y, int mouseX, int mouseY) {
      float rowHeight = this.height(context);
      this.markRow(cardX, y, rowHeight);
      float rowX = cardX + 8.0F;
      float rowY = y - 3.0F;
      this.state.box(rowX, rowY, context.cardWidth - 18.0F, rowHeight);
      this.state.colorX = cardX + context.cardWidth - 19.5F;
      this.state.colorY = rowY + 5.0F;
      this.state.colorWidth = 9.0F;
      this.state.colorHeight = 9.0F;
      this.updateStateFromSetting(context);
      Render2D.outline(
         this.state.colorX,
         this.state.colorY,
         9.0F,
         9.0F,
         3.5F,
         0.5F,
         context.color(255, 255, 255, 255),
         context.color(255, 255, 255, 50),
         context.color(255, 255, 255, 255),
         context.color(255, 255, 255, 50)
      );
      Render2D.rect(
         this.state.colorX,
         this.state.colorY,
         9.0F,
         9.0F,
         3.5F,
         context.multiplyAlpha(this.setting.getValue().getRGB(), 1.0F),
         context.multiplyAlpha(this.setting.getValue().getRGB(), 1.0F),
         context.multiplyAlpha(this.setting.getValue().getRGB(), 1.0F),
         context.multiplyAlpha(this.setting.getValue().getRGB(), 1.0F)
      );
      Render2D.text(FontType.GUI_ICONS, "R", this.state.colorX - 11.0F, this.state.colorY - 3.5F, 12.0F, context.color(154, 156, 166, 255));
      float textY = this.state.colorY + 1.5F;
      context.renderClippedText(
         graphics,
         FontType.SEMIBOLD,
         ClientLanguage.tr(this.setting.getName()),
         cardX + 10.0F,
         textY,
         context.cardWidth - 49.0F,
         6.0F,
         new Color(225, 225, 225, 255).getRGB()
      );
   }

   @Override
   void renderFloatingOverlay(DrawContext graphics, SettingCardContext context) {
      boolean active = context.activeColor == this.setting && this.state.visible;
      this.state.colorPickerAnimation.setDirection(active ? Direction.FORWARDS : Direction.BACKWARDS);
      float animation = context.clamp(this.state.colorPickerAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      if (!(animation <= 0.01F)) {
         if (active) {
            this.state.popupWidth = 87.0F;
            this.state.popupHeight = 76.0F;
            this.state.popupX = this.state.colorX - this.state.popupWidth - 6.0F;
            this.state.popupY = this.state.colorY - 4.0F;
            this.state.paletteX = this.state.popupX + 7.0F;
            this.state.paletteY = this.state.popupY + 7.0F;
            this.state.paletteSize = 62.0F;
            this.state.hueX = this.state.paletteX + this.state.paletteSize + 5.0F;
            this.state.hueY = this.state.paletteY;
            this.state.hueWidth = 7.0F;
            this.state.hueHeight = this.state.paletteSize;
         } else if (this.state.popupWidth <= 0.0F || this.state.popupHeight <= 0.0F) {
            return;
         }

         Render2D.rect(
            this.state.popupX,
            this.state.popupY,
            this.state.popupWidth,
            this.state.popupHeight,
            5.0F,
            context.withAlpha(context.theme().controlTop(), 0.94F * animation),
            context.withAlpha(context.theme().surfaceBright(), 0.94F * animation),
            context.withAlpha(context.theme().surfaceDeep(), 0.94F * animation),
            context.withAlpha(context.theme().controlBottom(), 0.94F * animation)
         );
         Render2D.outline(
            this.state.popupX,
            this.state.popupY,
            this.state.popupWidth,
            this.state.popupHeight,
            5.0F,
            0.35F,
            context.withAlpha(context.theme().outline(), 0.5F * animation),
            context.withAlpha(context.theme().accent(), 0.32F * animation),
            context.withAlpha(context.theme().accentDark(), 0.42F * animation),
            context.withAlpha(context.theme().outline(), 0.36F * animation)
         );
         int hueColor = Color.HSBtoRGB(this.state.hue, 1.0F, 1.0F);
         Render2D.rect(
            this.state.paletteX,
            this.state.paletteY,
            this.state.paletteSize,
            this.state.paletteSize,
            4.0F,
            this.colorWithAlpha(255, 255, 255, animation),
            this.colorWithAlpha(SettingCardContext.red(hueColor), SettingCardContext.green(hueColor), SettingCardContext.blue(hueColor), animation),
            this.colorWithAlpha(0, 0, 0, animation),
            this.colorWithAlpha(0, 0, 0, animation)
         );
         float selectorX = this.state.paletteX + this.state.saturation * this.state.paletteSize;
         float selectorY = this.state.paletteY + (1.0F - this.state.brightness) * this.state.paletteSize;
         Render2D.outline(selectorX - 2.5F, selectorY - 2.5F, 5.0F, 5.0F, 1.5F, 1.0F, context.withAlpha(new Color(255, 255, 255, 245), animation));
         int[] hueColors = new int[]{
            Color.HSBtoRGB(0.0F, 1.0F, 1.0F),
            Color.HSBtoRGB(0.16666667F, 1.0F, 1.0F),
            Color.HSBtoRGB(0.33333334F, 1.0F, 1.0F),
            Color.HSBtoRGB(0.5F, 1.0F, 1.0F),
            Color.HSBtoRGB(0.6666667F, 1.0F, 1.0F),
            Color.HSBtoRGB(0.8333333F, 1.0F, 1.0F),
            Color.HSBtoRGB(1.0F, 1.0F, 1.0F)
         };
         float segmentHeight = this.state.hueHeight / (float)(hueColors.length - 1);

         for (int i = 0; i < hueColors.length - 1; i++) {
            int top = hueColors[i];
            int bottom = hueColors[i + 1];
            float segmentY = this.state.hueY + (float)i * segmentHeight;
            Render2D.rect(
               this.state.hueX,
               segmentY,
               this.state.hueWidth,
               segmentHeight + 0.8F,
               i == 0 ? 1.5F : 0.0F,
               i == 0 ? 1.5F : 0.0F,
               i == hueColors.length - 2 ? 1.5F : 0.0F,
               i == hueColors.length - 2 ? 1.5F : 0.0F,
               this.colorWithAlpha(SettingCardContext.red(top), SettingCardContext.green(top), SettingCardContext.blue(top), animation),
               this.colorWithAlpha(SettingCardContext.red(top), SettingCardContext.green(top), SettingCardContext.blue(top), animation),
               this.colorWithAlpha(SettingCardContext.red(bottom), SettingCardContext.green(bottom), SettingCardContext.blue(bottom), animation),
               this.colorWithAlpha(SettingCardContext.red(bottom), SettingCardContext.green(bottom), SettingCardContext.blue(bottom), animation)
            );
         }

         Render2D.rect(
            this.state.hueX - 1.0F,
            this.state.hueY + this.state.hue * this.state.hueHeight - 1.0F,
            this.state.hueWidth + 2.0F,
            2.0F,
            0.0F,
            this.colorWithAlpha(255, 255, 255, animation)
         );
      }
   }

   @Override
   boolean mouseClickedOverlay(Click event, SettingCardContext context) {
      if (context.activeColor != this.setting || event.button() != 0) {
         return false;
      } else if (!this.state.visible) {
         context.activeColor = null;
         return false;
      } else if (context.inside(event.x(), event.y(), this.state.paletteX, this.state.paletteY, this.state.paletteSize, this.state.paletteSize)) {
         context.draggingColor = this.setting;
         context.draggingColorPart = 1;
         this.updateValue(context, event.x(), event.y(), 1);
         return true;
      } else if (context.inside(event.x(), event.y(), this.state.hueX, this.state.hueY, this.state.hueWidth, this.state.hueHeight)) {
         context.draggingColor = this.setting;
         context.draggingColorPart = 2;
         this.updateValue(context, event.x(), event.y(), 2);
         return true;
      } else {
         if (!context.inside(event.x(), event.y(), this.state.colorX, this.state.colorY, this.state.colorWidth, this.state.colorHeight)) {
            context.activeColor = null;
         }

         return false;
      }
   }

   @Override
   boolean mouseClicked(Click event, boolean doubled, SettingCardContext context) {
      if (event.button() == 0 && context.inside(event.x(), event.y(), this.state.boxX, this.state.boxY, this.state.boxWidth, this.state.boxHeight)) {
         context.activeColor = context.activeColor == this.setting ? null : this.setting;
         return true;
      } else {
         return false;
      }
   }

   @Override
   boolean mouseReleased(Click event, SettingCardContext context) {
      if (context.draggingColor != this.setting) {
         return false;
      } else {
         context.draggingColor = null;
         context.draggingColorPart = 0;
         return true;
      }
   }

   @Override
   boolean mouseDragged(Click event, double dragX, double dragY, SettingCardContext context) {
      if (context.draggingColor != this.setting) {
         return false;
      } else {
         this.updateValue(context, event.x(), event.y(), context.draggingColorPart);
         return true;
      }
   }

   private void updateValue(SettingCardContext context, double mouseX, double mouseY, int part) {
      if (part == 1) {
         this.state.saturation = context.clamp((float)((mouseX - (double)this.state.paletteX) / (double)this.state.paletteSize), 0.0F, 1.0F);
         this.state.brightness = 1.0F - context.clamp((float)((mouseY - (double)this.state.paletteY) / (double)this.state.paletteSize), 0.0F, 1.0F);
      } else if (part == 2) {
         this.state.hue = context.clamp((float)((mouseY - (double)this.state.hueY) / (double)this.state.hueHeight), 0.0F, 1.0F);
      }

      int rgb = Color.HSBtoRGB(this.state.hue, this.state.saturation, this.state.brightness);
      this.setting
         .setValue(new Color(SettingCardContext.red(rgb), SettingCardContext.green(rgb), SettingCardContext.blue(rgb), this.setting.getValue().getAlpha()));
      this.state.colorSynced = true;
   }

   private void updateStateFromSetting(SettingCardContext context) {
      if (context.draggingColor != this.setting) {
         Color color = this.setting.getValue();
         float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
         this.state.hue = hsb[0];
         this.state.saturation = hsb[1];
         this.state.brightness = hsb[2];
         this.state.colorSynced = true;
      }
   }

   private int colorWithAlpha(int red, int green, int blue, float alpha) {
      return new Color(red, green, blue, Math.max(0, Math.min(255, Math.round(255.0F * alpha)))).getRGB();
   }
}
