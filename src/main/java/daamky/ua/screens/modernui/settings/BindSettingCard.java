package daamky.ua.screens.modernui.settings;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

final class BindSettingCard extends SettingCardComponent<BindSetting> {
   BindSettingCard(BindSetting setting) {
      super(setting);
   }

   @Override
   int order() {
      return 5;
   }

   @Override
   float height(SettingCardContext context) {
      return 16.0F;
   }

   @Override
   void render(DrawContext graphics, SettingCardContext context, float cardX, float y, int mouseX, int mouseY) {
      this.markRow(cardX, y, this.height(context));
      boolean bound = this.setting.getValue() != null && this.setting.getValue().isBound();
      String text = context.listeningBind == this.setting ? "..." : (bound ? MathUtils.shortBind(this.setting.getValue()) : "n/a");
      float textSize = 5.5F;
      float textWidth = Render2D.textWidth(FontType.SEMIBOLD, text, textSize);
      float boxWidth = MathUtils.clamp(textWidth + 8.0F, 22.0F, 31.0F);
      float contentRight = cardX + context.cardWidth - 10.0F;
      float boxX = contentRight - boxWidth;
      float boxY = y + 1.0F;
      float boxHeight = 9.5F;
      this.state.box(boxX, boxY, boxWidth, boxHeight);
      this.renderNameAndDescription(graphics, context, cardX + 10.0F, y, 92.0F, 92.0F);
      Render2D.rect(
         boxX,
         boxY,
         boxWidth,
         boxHeight,
         2.0F,
         context.withAlpha(context.theme().controlTop(), 0.92F),
         context.withAlpha(context.theme().controlTop(), 0.98F),
         context.withAlpha(context.theme().controlBottom(), 0.98F),
         context.withAlpha(context.theme().controlBottom(), 0.94F)
      );
      Render2D.outline(
         boxX,
         boxY,
         boxWidth,
         boxHeight,
         2.0F,
         0.15F,
         context.withAlpha(context.theme().outline(), 0.68F),
         context.withAlpha(context.theme().outline(), 0.43F),
         context.withAlpha(context.theme().outline(), 0.56F),
         context.withAlpha(context.theme().outline(), 0.43F)
      );
      int textColor = !bound && context.listeningBind != this.setting ? context.color(255, 255, 255, 255) : context.color(255, 255, 255, 255);
      float textClipX = boxX + 3.0F;
      float textClipWidth = boxWidth - 6.0F;
      float textX = textClipX + Math.max(0.0F, (textClipWidth - textWidth) * 0.5F);
      float textY = boxY + (boxHeight - textSize) * 0.4F;
      Render2D.pushScissor(graphics, textClipX, boxY, textClipWidth, boxHeight);
      Render2D.text(FontType.SEMIBOLD, text, textX, textY, textSize, textColor);
      Render2D.popScissor(graphics);
      Render2D.text(FontType.GUI_ICONS, "L", boxX - 10.0F, boxY + 1.8F, 9.0F, context.color(154, 156, 166, 255));
   }

   @Override
   boolean mouseClicked(Click event, boolean doubled, SettingCardContext context) {
      if (!context.inside(event.x(), event.y(), this.state.boxX, this.state.boxY, this.state.boxWidth, this.state.boxHeight)) {
         return false;
      } else {
         if (event.button() == 1) {
            this.setting.setValue(KeyBind.NONE);
         } else {
            context.listeningBind = this.setting;
         }

         context.activeColor = null;
         return true;
      }
   }
}
