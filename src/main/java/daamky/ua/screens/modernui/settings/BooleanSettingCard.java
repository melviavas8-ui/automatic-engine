package daamky.ua.screens.modernui.settings;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

final class BooleanSettingCard extends SettingCardComponent<BooleanSetting> {
   BooleanSettingCard(BooleanSetting setting) {
      super(setting);
   }

   @Override
   int order() {
      return 3;
   }

   @Override
   float height(SettingCardContext context) {
      return 16.0F;
   }

   @Override
   void render(DrawContext graphics, SettingCardContext context, float cardX, float y, int mouseX, int mouseY) {
      this.markRow(cardX, y, this.height(context));
      this.state.booleanAnimation.setDirection(this.setting.getValue() ? Direction.FORWARDS : Direction.BACKWARDS);
      float animation = context.clamp(this.state.booleanAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      float boxX = cardX + context.cardWidth - 20.0F;
      float boxY = y + 1.0F;
      this.state.box(boxX, boxY, 12.5F, 12.5F);
      this.renderNameAndDescription(graphics, context, cardX + 10.0F, y, 102.0F, 102.0F);
      Render2D.rect(
         boxX,
         boxY,
         10.0F,
         10.0F,
         2.0F,
         context.color(70, 72, 86, 175),
         context.color(82, 82, 96, 165),
         context.color(45, 46, 56, 185),
         context.color(62, 58, 74, 175)
      );
      float crossProgress = 1.0F - animation;
      if (crossProgress > 0.01F) {
         float crossSize = 5.0F + crossProgress;
         int crossColor = Math.round(145.0F + 65.0F * crossProgress);
         int crossAlpha = Math.round(255.0F * crossProgress);
         Render2D.text(
            FontType.MAINMENUSCREEN,
            "i",
            boxX + 2.0F - (crossSize - 6.0F) * 0.5F,
            boxY + 2.4F - (crossSize - 6.0F) * 0.5F,
            crossSize,
            context.color(crossColor, crossColor, crossColor, crossAlpha),
            20.0F * animation,
            boxX + 5.0F,
            boxY + 5.0F
         );
      }

      if (animation > 0.01F) {
         Render2D.rect(
            boxX,
            boxY,
            10.0F,
            10.0F,
            2.0F,
            context.withAlpha(context.theme().accentBright(), animation),
            context.withAlpha(context.theme().accent(), animation),
            context.withAlpha(context.theme().accentDark(), animation),
            context.withAlpha(context.theme().accentDark(), animation)
         );
         float checkSize = 5.0F + animation;
         int iconColor = Math.round(170.0F + 70.0F * animation);
         int checkAlpha = Math.round(255.0F * animation);
         Render2D.text(
            FontType.MAINMENUSCREEN,
            "F",
            boxX + 2.2F - (checkSize - 6.0F) * 0.5F,
            boxY + 2.3F - (checkSize - 6.0F) * 0.5F,
            checkSize,
            context.color(iconColor, iconColor, iconColor, checkAlpha),
            -20.0F * crossProgress,
            boxX + 5.0F,
            boxY + 5.0F
         );
      }

      Render2D.text(FontType.ICONS, "B", boxX - 12.0F, boxY, 10.0F, context.color(154, 156, 166, 255));
   }

   @Override
   boolean mouseClicked(Click event, boolean doubled, SettingCardContext context) {
      if (event.button() != 0) {
         return false;
      } else {
         float hitX = this.state.boxX - 13.0F;
         float hitY = this.state.boxY - 1.0F;
         float hitWidth = this.state.boxWidth + 13.0F;
         float hitHeight = this.state.boxHeight + 2.0F;
         if (!context.inside(event.x(), event.y(), hitX, hitY, hitWidth, hitHeight)) {
            return false;
         } else {
            this.setting.setValue(Boolean.valueOf(!this.setting.getValue()));
            context.activeColor = null;
            return true;
         }
      }
   }
}
