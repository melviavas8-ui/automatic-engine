package daamky.ua.screens.clickgui.impl.options;

import java.awt.Color;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;

public class ColorOption extends ClickGuiOption {
   private static final int[] HUE_COLORS = new int[]{
      Color.HSBtoRGB(0.0F, 1.0F, 1.0F),
      Color.HSBtoRGB(0.16666667F, 1.0F, 1.0F),
      Color.HSBtoRGB(0.33333334F, 1.0F, 1.0F),
      Color.HSBtoRGB(0.5F, 1.0F, 1.0F),
      Color.HSBtoRGB(0.6666667F, 1.0F, 1.0F),
      Color.HSBtoRGB(0.8333333F, 1.0F, 1.0F),
      Color.HSBtoRGB(1.0F, 1.0F, 1.0F)
   };
   private final SmoothAnimation openAnimation = new SmoothAnimation();
   private final ColorSetting setting;
   private float hue;
   private float saturation;
   private float brightness;
   private float alphaValue;
   private boolean open;
   private int dragMode;
   private float swatchX;
   private float swatchY;
   private float swatchSize;
   private float popupX;
   private float popupY;
   private float popupWidth;
   private float popupHeight;
   private float paletteX;
   private float paletteY;
   private float paletteSize;
   private float hueX;
   private float hueY;
   private float hueWidth;
   private float hueHeight;

   public ColorOption(ColorSetting setting) {
      super(setting.getName());
      this.setting = setting;
      this.applyColor(setting.getValue());
      this.openAnimation.set(0.0);
   }

   @Override
   protected void renderControl(DrawContext graphics, float x, float y, float width, float height, float scale, float alpha) {
      if (this.dragMode == 0) {
         this.applyColor(this.setting.getValue());
      }

      this.swatchSize = 10.0F * scale;
      this.swatchX = x + width - this.swatchSize - 8.0F * scale;
      this.swatchY = y + 9.0F * scale;
      int current = this.currentRgb();
      Render2D.glassOutline(
         this.swatchX - 3.0F * scale,
         this.swatchY - 3.0F * scale,
         this.swatchSize + 5.5F * scale,
         this.swatchSize + 5.0F * scale,
         6.5F * scale,
         0.9F * scale,
         this.color(232, 232, 235, 170, alpha),
         1.0F,
         35.0F,
         this.color(162, 162, 168, 122, alpha),
         0.42F * alpha,
         false,
         0.45F,
         0.0015F,
         1.0F,
         0.0F
      );
      Render2D.rect(
         this.swatchX + 0.25F,
         this.swatchY,
         this.swatchSize - 1.0F,
         this.swatchSize - 1.0F,
         3.5F * scale,
         this.color(red(current), green(current), blue(current), Math.round(this.alphaValue * 255.0F), alpha)
      );
      float iconSize = 15.0F;
      this.renderGuiIcon("R", x + 3.5F * scale, this.swatchY - 6.0F * scale, iconSize, scale);
      this.renderOptionLabel(graphics, "color", x + 15.5F * scale, this.swatchY - 1.0F * scale, 7.2F, this.swatchX - 5.0F * scale, y, height);
   }

   @Override
   public void renderOverlay(DrawContext graphics) {
      this.openAnimation.update();
      float progress = this.openAnimation.get();
      if (!(progress <= 0.01F)) {
         float popupAlpha = this.alpha * progress;
         this.popupWidth = 87.0F * this.scale;
         this.popupHeight = 76.0F * this.scale;
         this.popupX = this.x + this.width + 7.0F * this.scale;
         this.popupY = this.y - 5.0F * this.scale;
         this.paletteX = this.popupX + 7.0F * this.scale;
         this.paletteY = this.popupY + 7.0F * this.scale;
         this.paletteSize = 62.0F * this.scale;
         this.hueX = this.paletteX + this.paletteSize + 5.0F * this.scale;
         this.hueY = this.paletteY;
         this.hueWidth = 7.0F * this.scale;
         this.hueHeight = this.paletteSize;
         float radius = 9.0F * this.scale;
         Render2D.blur(this.popupX, this.popupY, this.popupWidth, this.popupHeight, radius, 1.0F, 1.0F, this.color(255, 255, 255, 255, popupAlpha));
         Render2D.glass(
            this.popupX,
            this.popupY,
            this.popupWidth,
            this.popupHeight,
            radius,
            this.color(255, 255, 255, 82, popupAlpha),
            0.64F * popupAlpha,
            42.0F,
            this.color(255, 255, 255, 154, popupAlpha),
            0.16F * popupAlpha,
            true,
            0.22F,
            0.003F,
            1.0F,
            0.0F
         );
         Render2D.glassOutline(
            this.popupX,
            this.popupY,
            this.popupWidth,
            this.popupHeight,
            radius,
            0.9F * this.scale,
            this.color(255, 255, 255, 170, popupAlpha),
            1.0F,
            35.0F,
            this.color(162, 162, 168, 122, popupAlpha),
            0.42F * popupAlpha,
            false,
            0.45F,
            0.0015F,
            1.0F,
            0.0F
         );
         this.drawPalette(popupAlpha);
         this.drawHueSlider(popupAlpha);
      }
   }

   @Override
   public boolean mouseClicked(Click event, boolean doubled) {
      if (event.button() != 0) {
         return false;
      } else {
         double mouseX = event.x();
         double mouseY = event.y();
         if (this.isPopupVisible()) {
            if (this.hovered(mouseX, mouseY, this.paletteX, this.paletteY, this.paletteSize, this.paletteSize)) {
               this.dragMode = 1;
               this.updatePalette(mouseX, mouseY);
               return true;
            }

            if (this.hovered(mouseX, mouseY, this.hueX, this.hueY, this.hueWidth, this.hueHeight)) {
               this.dragMode = 2;
               this.updateHue(mouseY);
               return true;
            }
         }

         if (this.hovered(
            mouseX,
            mouseY,
            this.swatchX - 3.0F * this.scale,
            this.swatchY - 3.0F * this.scale,
            this.swatchSize + 6.0F * this.scale,
            this.swatchSize + 6.0F * this.scale
         )) {
            this.setOpen(!this.open);
            return true;
         } else {
            if (this.open) {
               this.setOpen(false);
            }

            return false;
         }
      }
   }

   @Override
   public boolean wantsMousePriority(Click event) {
      return this.isPopupVisible() && this.hovered(event.x(), event.y(), this.popupX, this.popupY, this.popupWidth, this.popupHeight);
   }

   @Override
   public boolean hasOpenPopup() {
      return this.isPopupVisible();
   }

   @Override
   public boolean mouseReleased(Click event) {
      if (this.dragMode == 0) {
         return false;
      } else {
         this.dragMode = 0;
         return true;
      }
   }

   @Override
   public boolean mouseDragged(Click event, double dragX, double dragY) {
      if (this.dragMode == 0) {
         return false;
      } else {
         if (this.dragMode == 1) {
            this.updatePalette(event.x(), event.y());
         } else if (this.dragMode == 2) {
            this.updateHue(event.y());
         }

         return true;
      }
   }

   @Override
   public void closePopup() {
      if (this.open) {
         this.setOpen(false);
      }
   }

   private void drawPalette(float popupAlpha) {
      int hueColor = Color.HSBtoRGB(this.hue, 1.0F, 1.0F);
      int topLeft = this.color(255, 255, 255, 255, popupAlpha);
      int topRight = this.color(red(hueColor), green(hueColor), blue(hueColor), 255, popupAlpha);
      int bottom = this.color(0, 0, 0, 255, popupAlpha);
      Render2D.rect(this.paletteX, this.paletteY, this.paletteSize, this.paletteSize, 4.0F * this.scale, topLeft, topRight, bottom, bottom);
      float selectorX = this.paletteX + this.saturation * this.paletteSize;
      float selectorY = this.paletteY + (1.0F - this.brightness) * this.paletteSize;
      Render2D.outline(
         selectorX - 2.5F * this.scale,
         selectorY - 2.5F * this.scale,
         5.0F * this.scale,
         5.0F * this.scale,
         1.5F * this.scale,
         1.0F * this.scale,
         this.color(255, 255, 255, 245, popupAlpha)
      );
      Render2D.rect(
         selectorX - 1.0F * this.scale,
         selectorY - 1.0F * this.scale,
         2.0F * this.scale,
         2.0F * this.scale,
         0.5F * this.scale,
         this.color(20, 24, 32, 180, popupAlpha)
      );
   }

   private void drawHueSlider(float popupAlpha) {
      float segmentHeight = this.hueHeight / (float)(HUE_COLORS.length - 1);

      for (int i = 0; i < HUE_COLORS.length - 1; i++) {
         int top = HUE_COLORS[i];
         int bottom = HUE_COLORS[i + 1];
         float overlap = 0.8F * this.scale;
         float segmentY = this.hueY + (float)i * segmentHeight - (i == 0 ? 0.0F : overlap);
         float segmentBottom = this.hueY + (float)(i + 1) * segmentHeight + (i == HUE_COLORS.length - 2 ? 0.0F : overlap);
         float radius = 1.5F * this.scale;
         Render2D.rect(
            this.hueX,
            segmentY,
            this.hueWidth,
            segmentBottom - segmentY,
            i == 0 ? radius : 0.0F,
            i == 0 ? radius : 0.0F,
            i == HUE_COLORS.length - 2 ? radius : 0.0F,
            i == HUE_COLORS.length - 2 ? radius : 0.0F,
            this.color(red(top), green(top), blue(top), 255, popupAlpha),
            this.color(red(top), green(top), blue(top), 255, popupAlpha),
            this.color(red(bottom), green(bottom), blue(bottom), 255, popupAlpha),
            this.color(red(bottom), green(bottom), blue(bottom), 255, popupAlpha)
         );
      }

      float selectorY = this.hueY + this.hue * this.hueHeight;
      Render2D.rect(
         this.hueX - 1.0F * this.scale,
         selectorY - 1.0F * this.scale,
         this.hueWidth + 2.0F * this.scale,
         2.0F * this.scale,
         0.0F * this.scale,
         this.color(255, 255, 255, 255, popupAlpha)
      );
   }

   private void updatePalette(double mouseX, double mouseY) {
      this.saturation = this.clamp((float)((mouseX - (double)this.paletteX) / (double)this.paletteSize), 0.0F, 1.0F);
      this.brightness = 1.0F - this.clamp((float)((mouseY - (double)this.paletteY) / (double)this.paletteSize), 0.0F, 1.0F);
      float alphaZoneTop = this.paletteY + this.paletteSize - 12.0F * this.scale;
      this.alphaValue = mouseY >= (double)alphaZoneTop ? this.clamp((float)((mouseX - (double)this.paletteX) / (double)this.paletteSize), 0.0F, 1.0F) : 1.0F;
      this.syncSetting();
   }

   private void updateHue(double mouseY) {
      this.hue = this.clamp((float)((mouseY - (double)this.hueY) / (double)this.hueHeight), 0.0F, 1.0F);
      this.syncSetting();
   }

   private void applyColor(Color color) {
      float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
      this.hue = hsb[0];
      this.saturation = hsb[1];
      this.brightness = hsb[2];
      this.alphaValue = (float)color.getAlpha() / 255.0F;
   }

   private int currentRgb() {
      return Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
   }

   private void syncSetting() {
      int color = this.currentRgb();
      this.setting.setValue(new Color(red(color), green(color), blue(color), Math.round(this.alphaValue * 255.0F)));
   }

   private boolean isPopupVisible() {
      return this.open || this.openAnimation.get() > 0.01F || this.openAnimation.isAlive();
   }

   private void setOpen(boolean open) {
      if (this.open != open) {
         this.open = open;
         this.openAnimation.run(open ? 1.0 : 0.0, 0.16, Easings.CUBIC_OUT);
      }
   }

   private static int red(int color) {
      return color >>> 16 & 0xFF;
   }

   private static int green(int color) {
      return color >>> 8 & 0xFF;
   }

   private static int blue(int color) {
      return color & 0xFF;
   }

   @Override
   protected int color(int red, int green, int blue, int alpha, float alphaMultiplier) {
      return ColorUtil.rgba(red, green, blue, (int)((float)alpha * alphaMultiplier));
   }
}
