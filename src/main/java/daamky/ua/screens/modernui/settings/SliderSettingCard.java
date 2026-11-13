package daamky.ua.screens.modernui.settings;

import java.awt.Color;
import java.util.Locale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

final class SliderSettingCard extends SettingCardComponent<NumberSetting> {
   SliderSettingCard(NumberSetting setting) {
      super(setting);
   }

   @Override
   int order() {
      return 2;
   }

   @Override
   float height(SettingCardContext context) {
      return 20.0F;
   }

   @Override
   void render(DrawContext graphics, SettingCardContext context, float cardX, float y, int mouseX, int mouseY) {
      this.markRow(cardX, y, this.height(context));
      float sliderX = cardX + 8.0F;
      float sliderY = y + 11.5F;
      float contentRight = cardX + context.cardWidth - 10.0F;
      float sliderWidth = contentRight - sliderX;
      float progress = this.progress(context);
      this.state.sliderProgress = context.lerp(this.state.sliderProgress, progress, context.draggingSlider == this.setting ? 0.42F : 0.18F);
      this.state.slider(sliderX, sliderY - 5.0F, sliderWidth, 12.0F);
      this.renderNameAndDescription(graphics, context, cardX + 10.0F, y - 2.0F, 92.0F, 92.0F);
      String value = this.formatValue(this.setting.getValue(), this.setting.getStep());
      float valueWidth = Render2D.textWidth(FontType.SEMIBOLD, value, 6.0F);
      Render2D.text(FontType.SEMIBOLD, value, contentRight - 10.0F - valueWidth, y + 1.0F, 6.0F, context.color(225, 225, 225, 255));
      Render2D.text(FontType.GUI_ICONS, "A", contentRight - 8.0F, y + 0.4F, 9.0F, context.color(154, 156, 166, 255));
      Render2D.rect(
         sliderX,
         sliderY,
         sliderWidth,
         3.0F,
         0.5F,
         new Color(42, 43, 50, 255).getRGB(),
         new Color(48, 49, 58, 255).getRGB(),
         new Color(19, 20, 24, 255).getRGB(),
         new Color(39, 40, 48, 255).getRGB()
      );
      Render2D.rect(
         sliderX,
         sliderY,
         sliderWidth * this.state.sliderProgress,
         3.0F,
         0.5F,
         context.theme().accentBright().getRGB(),
         context.theme().accent().getRGB(),
         context.theme().accent().getRGB(),
         context.theme().accentDark().getRGB()
      );
      Render2D.rect(sliderX + sliderWidth * this.state.sliderProgress - 2.0F, sliderY - 2.0F, 6.0F, 6.0F, 2.0F, context.color(214, 216, 224, 255));
      Render2D.rect(sliderX + sliderWidth * this.state.sliderProgress - 1.0F, sliderY - 1.1F, 4.0F, 4.0F, 1.2F, new Color(26, 27, 33, 255).getRGB());
   }

   @Override
   boolean mouseClicked(Click event, boolean doubled, SettingCardContext context) {
      if (event.button() == 0 && context.inside(event.x(), event.y(), this.state.sliderX, this.state.sliderY, this.state.sliderWidth, this.state.sliderHeight)) {
         context.draggingSlider = this.setting;
         this.updateValue(context, event.x());
         context.activeColor = null;
         return true;
      } else {
         return false;
      }
   }

   @Override
   boolean mouseReleased(Click event, SettingCardContext context) {
      if (context.draggingSlider != this.setting) {
         return false;
      } else {
         context.draggingSlider = null;
         return true;
      }
   }

   @Override
   boolean mouseDragged(Click event, double dragX, double dragY, SettingCardContext context) {
      if (context.draggingSlider != this.setting) {
         return false;
      } else {
         this.updateValue(context, event.x());
         return true;
      }
   }

   private void updateValue(SettingCardContext context, double mouseX) {
      if (!(this.state.sliderWidth <= 0.0F)) {
         double min = this.setting.getMin();
         double max = this.setting.getMax();
         double nextProgress = (double)context.clamp((float)((mouseX - (double)this.state.sliderX) / (double)this.state.sliderWidth), 0.0F, 1.0F);
         this.setting.setValue(Double.valueOf(min + (max - min) * nextProgress));
      }
   }

   private float progress(SettingCardContext context) {
      double min = this.setting.getMin();
      double max = this.setting.getMax();
      return max == min ? 0.0F : context.clamp((float)((this.setting.getValue() - min) / (max - min)), 0.0F, 1.0F);
   }

   private String formatValue(double value, double step) {
      if (step >= 1.0) {
         return String.valueOf(Math.round(value));
      } else {
         String text = String.format(Locale.ROOT, "%.2f", value);

         while (text.contains(".") && text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
         }

         return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
      }
   }
}
