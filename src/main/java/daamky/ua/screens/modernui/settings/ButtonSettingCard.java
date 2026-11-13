package daamky.ua.screens.modernui.settings;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.settings.impl.ButtonSetting;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

final class ButtonSettingCard extends SettingCardComponent<ButtonSetting> {
   ButtonSettingCard(ButtonSetting setting) {
      super(setting);
   }

   @Override
   int order() {
      return 7;
   }

   @Override
   float height(SettingCardContext context) {
      return 16.0F;
   }

   @Override
   void render(DrawContext graphics, SettingCardContext context, float cardX, float y, int mouseX, int mouseY) {
      this.markRow(cardX, y, this.height(context));
      float boxX = cardX + 111.0F;
      float boxY = y - 2.5F;
      float boxWidth = 28.5F;
      float boxHeight = 12.5F;
      this.state.box(boxX, boxY, boxWidth, boxHeight);
      this.renderNameAndDescription(graphics, context, cardX + 10.0F, y, 96.0F, 96.0F);
      Render2D.rect(
         boxX,
         boxY,
         boxWidth,
         boxHeight,
         2.0F,
         context.withAlpha(context.theme().controlTop(), 0.78F),
         context.withAlpha(context.theme().accent(), 0.72F),
         context.withAlpha(context.theme().controlBottom(), 0.82F),
         context.withAlpha(context.theme().accentDark(), 0.76F)
      );
      Render2D.text(FontType.GUI_ICONS, "U", boxX - 10.0F, boxY + 2.0F, 9.0F, context.color(154, 156, 166, 255));
      String text = ClientLanguage.tr("Click");
      float textWidth = Render2D.textWidth(FontType.SEMIBOLD, text, 6.0F);
      Render2D.text(FontType.SEMIBOLD, text, boxX + (boxWidth - textWidth) * 0.5F, y + 2.5F, 6.0F, context.color(225, 225, 225, 255));
   }

   @Override
   boolean mouseClicked(Click event, boolean doubled, SettingCardContext context) {
      if (event.button() == 0 && context.inside(event.x(), event.y(), this.state.boxX, this.state.boxY, this.state.boxWidth, this.state.boxHeight)) {
         this.setting.press();
         context.activeColor = null;
         return true;
      } else {
         return false;
      }
   }
}
