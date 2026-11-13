package daamky.ua.screens.clickgui.impl.options;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.ui.Render2D;

public class SliderOption extends ClickGuiOption {
   private final SmoothAnimation progressAnimation = new SmoothAnimation();
   private final NumberSetting setting;
   private final String suffix;
   private float value;
   private float min;
   private float max;
   private boolean dragging;
   private float sliderX;
   private float sliderY;
   private float sliderWidth;
   private float sliderHeight;

   public SliderOption(NumberSetting setting, String suffix) {
      super(setting.getName());
      this.setting = setting;
      this.value = setting.getFloat();
      this.min = (float)setting.getMin();
      this.max = (float)setting.getMax();
      this.suffix = suffix == null ? "" : suffix;
      this.progressAnimation.set((double)this.progress());
   }

   @Override
   protected void renderControl(DrawContext graphics, float x, float y, float width, float height, float scale, float alpha) {
      this.value = this.setting.getFloat();
      this.min = (float)this.setting.getMin();
      this.max = (float)this.setting.getMax();
      this.sliderWidth = 40.0F * scale;
      this.sliderX = x + width - this.sliderWidth - 7.0F * scale;
      this.sliderY = y + 14.0F * scale;
      this.sliderHeight = 3.0F * scale;
      this.progressAnimation.run((double)this.progress(), this.dragging ? 0.1 : 0.18, Easings.CUBIC_OUT, true);
      this.progressAnimation.update();
      float progress = this.progressAnimation.get();
      float displayValue = (float)Math.round((this.min + (this.max - this.min) * progress) * 10.0F) / 10.0F;
      String valueText = this.formatValue(displayValue);
      float valueSize = 6.2F;
      float valueClipX = this.sliderX;
      float valueClipWidth = this.sliderWidth;
      float valueX = valueClipX + 2.0F * scale;
      float knobSize = 7.0F * scale;
      float knobX = this.clamp(this.sliderX + this.sliderWidth * progress - knobSize / 2.0F, this.sliderX, this.sliderX + this.sliderWidth - knobSize);
      this.renderScrollingText(
         graphics,
         "slider:value",
         valueText,
         valueX,
         y + 5.0F * scale,
         valueSize,
         this.color(255, 255, 255, 245, alpha),
         valueClipX,
         y + 3.0F * scale,
         valueClipWidth,
         9.0F * scale,
         this.sliderX,
         y,
         this.sliderWidth,
         height
      );
      this.renderSliderGlass(this.sliderX, this.sliderY - 0.5F * scale, this.sliderWidth, this.sliderHeight + 1.0F * scale, 0.5F * scale, alpha);
      Render2D.rect(this.sliderX, this.sliderY, this.sliderWidth * progress, this.sliderHeight, 0.5F * scale, this.color(255, 255, 255, 230, alpha));
      Render2D.rect(knobX, this.sliderY - 2.0F * scale, knobSize, knobSize, 2.5F * scale, this.color(215, 215, 215, 245, alpha));
      float iconSize = 9.0F;
      this.renderGuiIcon("111H", x + 3.5F * scale, y + 6.5F * scale, iconSize, scale);
      this.renderOptionLabel(graphics, "slider", x + 15.5F * scale, y + 6.0F * scale, 7.2F, this.sliderX - 5.0F * scale, y, height);
   }

   @Override
   public boolean mouseClicked(Click event, boolean doubled) {
      if (event.button() != 0) {
         return false;
      } else if (this.hovered(
         event.x(), event.y(), this.sliderX - 3.0F * this.scale, this.sliderY - 6.0F * this.scale, this.sliderWidth + 6.0F * this.scale, 15.0F * this.scale
      )) {
         this.dragging = true;
         this.updateValue(event.x());
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseReleased(Click event) {
      if (!this.dragging) {
         return false;
      } else {
         this.dragging = false;
         return true;
      }
   }

   @Override
   public boolean mouseDragged(Click event, double dragX, double dragY) {
      if (!this.dragging) {
         return false;
      } else {
         this.updateValue(event.x());
         return true;
      }
   }

   private void updateValue(double mouseX) {
      float nextProgress = this.clamp((float)((mouseX - (double)this.sliderX) / (double)this.sliderWidth), 0.0F, 1.0F);
      float raw = this.min + (this.max - this.min) * nextProgress;
      this.value = (float)Math.round(raw * 10.0F) / 10.0F;
      this.setting.setValue(Double.valueOf((double)this.value));
   }

   private float progress() {
      return this.max == this.min ? 0.0F : this.clamp((this.value - this.min) / (this.max - this.min), 0.0F, 1.0F);
   }

   private void renderSliderGlass(float x, float y, float width, float height, float radius, float alpha) {
      Render2D.blur(x, y, width, height, radius, 1.0F, 1.0F, this.color(255, 255, 255, 255, alpha));
      Render2D.rect(
         x,
         y,
         width,
         height,
         radius,
         this.color(75, 75, 75, 140, alpha),
         this.color(75, 75, 75, 136, alpha),
         this.color(75, 75, 75, 132, alpha),
         this.color(75, 75, 75, 140, alpha)
      );
      Render2D.glass(
         x,
         y,
         width,
         height,
         radius,
         this.color(255, 255, 255, 62, alpha),
         0.64F * alpha,
         42.0F,
         this.color(255, 255, 255, 126, alpha),
         0.16F * alpha,
         true,
         0.22F,
         0.003F,
         1.0F,
         0.0F
      );
   }

   private String formatValue(float displayValue) {
      int scaled = Math.round(displayValue * 10.0F);
      String sign = scaled < 0 ? "-" : "";
      int absolute = Math.abs(scaled);
      return sign + absolute / 10 + "." + absolute % 10 + this.suffix;
   }
}
