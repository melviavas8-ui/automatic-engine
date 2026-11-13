package daamky.ua.screens.modernui.settings;

import java.awt.Color;
import java.util.StringJoiner;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.utils.render.animation.modernfx.Decelerate;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

final class MultiSettingCard extends SettingCardComponent<MultiModeSetting> {
   MultiSettingCard(MultiModeSetting setting) {
      super(setting);
   }

   @Override
   int order() {
      return 1;
   }

   @Override
   float height(SettingCardContext context) {
      this.state.expandAnimation.setDirection(this.state.expanded ? Direction.FORWARDS : Direction.BACKWARDS);
      return 25.0F + (float)this.setting.getModes().size() * 15.0F * context.clamp(this.state.expandAnimation.getOutput().floatValue(), 0.0F, 1.0F);
   }

   @Override
   float layoutHeight(SettingCardContext context) {
      return 25.0F;
   }

   @Override
   void render(DrawContext graphics, SettingCardContext context, float cardX, float y, int mouseX, int mouseY) {
      this.markRow(cardX, y, this.height(context));
      this.state.expandAnimation.setDirection(this.state.expanded ? Direction.FORWARDS : Direction.BACKWARDS);
      float boxX = cardX + 8.0F;
      float boxY = y + 6.5F;
      float boxWidth = context.cardWidth - 18.0F;
      float boxHeight = 12.0F;
      this.state.box(boxX, boxY, boxWidth, boxHeight);
      context.renderClippedText(
         graphics,
         FontType.SEMIBOLD,
         ClientLanguage.tr(this.setting.getName()),
         cardX + 10.0F,
         y - 2.0F,
         context.cardWidth - 50.0F,
         6.0F,
         new Color(225, 225, 225, 255).getRGB()
      );
      String count = this.setting.selectedCount() + "/" + this.setting.getModes().size();
      float countWidth = Render2D.textWidth(FontType.SEMIBOLD, count, 6.0F);
      Render2D.text(FontType.SEMIBOLD, count, cardX + context.cardWidth - 10.0F - countWidth, y - 2.0F, 6.0F, context.color(225, 225, 225, 255));
      Render2D.rect(
         boxX,
         boxY,
         boxWidth,
         boxHeight,
         2.0F,
         context.withAlpha(context.theme().controlTop(), 0.88F),
         context.withAlpha(context.theme().controlTop(), 0.96F),
         context.withAlpha(context.theme().controlBottom(), 0.94F),
         context.withAlpha(context.theme().controlBottom(), 1.0F)
      );
      Render2D.outline(
         boxX,
         boxY,
         boxWidth,
         boxHeight,
         2.0F,
         0.15F,
         context.withAlpha(context.theme().outline(), 0.65F),
         context.withAlpha(context.theme().outline(), 0.48F),
         context.withAlpha(context.theme().outline(), 0.54F),
         context.withAlpha(context.theme().outline(), 0.48F)
      );
      Render2D.pushScissor(graphics, boxX, boxY, boxWidth - 15.0F, boxHeight);
      context.renderClippedText(
         graphics, FontType.BOLD, this.valueText(), boxX + 14.0F, boxY + 3.0F, boxWidth - 31.0F, 5.5F, new Color(255, 255, 255, 255).getRGB()
      );
      Render2D.popScissor(graphics);
      float arrowAnimation = context.clamp(this.state.expandAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      float arrowX = boxX + boxWidth - 12.0F;
      float arrowY = boxY + 3.0F;
      float arrowSize = 6.0F;
      float arrowWidth = Render2D.textWidth(FontType.MAINMENUSCREEN, "0", arrowSize);
      Render2D.text(
         FontType.MAINMENUSCREEN,
         "0",
         arrowX,
         arrowY,
         arrowSize,
         context.color(161, 162, 167, 255),
         180.0F * arrowAnimation,
         arrowX + arrowWidth * 0.5F,
         arrowY + arrowSize * 0.5F
      );
      Render2D.text(FontType.CATEGORY_ICONS, "g", boxX + 4.0F, boxY + 2.0F, 8.0F, new Color(255, 255, 255, 255).getRGB());
   }

   @Override
   void renderOverlay(DrawContext graphics, SettingCardContext context) {
      float animation = context.clamp(this.state.expandAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      if (this.state.visible && !(animation <= 0.01F)) {
         float optionHeight = 15.0F;
         float popupHeight = (float)this.setting.getModes().size() * optionHeight * animation;
         float popupY = this.state.boxY + this.state.boxHeight + 1.0F;
         this.state.options(this.state.boxX, popupY, this.state.boxWidth, optionHeight, this.setting.getModes().size());
         Render2D.rect(
            this.state.boxX,
            popupY,
            this.state.boxWidth,
            popupHeight,
            2.0F,
            context.withAlpha(context.theme().controlTop(), 0.98F),
            context.withAlpha(context.theme().controlTop(), 1.0F),
            context.withAlpha(context.theme().controlBottom(), 1.0F),
            context.withAlpha(context.theme().controlBottom(), 0.98F)
         );
         Render2D.outline(
            this.state.boxX,
            popupY,
            this.state.boxWidth,
            popupHeight,
            2.0F,
            0.15F,
            context.withAlpha(context.theme().outline(), 0.68F),
            context.withAlpha(context.theme().outline(), 0.5F),
            context.withAlpha(context.theme().outline(), 0.58F),
            context.withAlpha(context.theme().outline(), 0.5F)
         );
         Render2D.pushScissor(graphics, this.state.boxX, popupY, this.state.boxWidth, popupHeight);
         float y = popupY;

         for (String mode : this.setting.getModes()) {
            boolean selected = this.setting.isSelected(mode);
            Decelerate optionAnimation = this.state.optionAnimation(mode, selected);
            optionAnimation.setDirection(selected ? Direction.FORWARDS : Direction.BACKWARDS);
            float optionProgress = context.clamp(optionAnimation.getOutput().floatValue(), 0.0F, 1.0F);
            if (optionProgress > 0.01F) {
            }

            int textAlpha = Math.round((155.0F + 100.0F * optionProgress) * animation);
            int iconTone = Math.round(161.0F + 94.0F * optionProgress);
            context.renderClippedText(
               graphics,
               FontType.SEMIBOLD,
               ClientLanguage.tr(mode),
               this.state.boxX + 14.0F,
               y + 5.5F,
               this.state.boxWidth - 23.0F,
               5.5F,
               new Color(245, 245, 245, textAlpha).getRGB()
            );
            Render2D.text(
               FontType.CATEGORY_ICONS,
               "g",
               this.state.boxX + 4.0F,
               y + 4.5F,
               8.0F,
               context.color(iconTone, iconTone, iconTone, Math.round(255.0F * animation))
            );
            if (optionProgress > 0.01F) {
               float checkSize = 4.7F + 1.3F * optionProgress;
               float checkX = this.state.boxX + this.state.boxWidth - 9.0F - checkSize * 0.5F;
               float checkY = y + 8.5F - checkSize * 0.5F;
               Render2D.text(
                  FontType.MAINMENUSCREEN, "F", checkX, checkY, checkSize, context.color(245, 245, 245, Math.round(255.0F * optionProgress * animation))
               );
            }

            y += optionHeight;
         }

         Render2D.popScissor(graphics);
      }
   }

   @Override
   boolean mouseClickedOverlay(Click event, SettingCardContext context) {
      if (event.button() == 0 && this.state.visible && this.state.expanded) {
         if (!context.inside(
            event.x(), event.y(), this.state.optionX, this.state.optionY, this.state.optionWidth, this.state.optionHeight * (float)this.state.optionCount
         )) {
            return false;
         } else {
            int index = (int)((event.y() - (double)this.state.optionY) / (double)this.state.optionHeight);
            if (index >= 0 && index < this.setting.getModes().size()) {
               String mode = this.setting.getModes().get(index);
               this.setting.setSelected(mode, !this.setting.isSelected(mode));
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   @Override
   boolean mouseClicked(Click event, boolean doubled, SettingCardContext context) {
      if (context.inside(event.x(), event.y(), this.state.boxX, this.state.boxY, this.state.boxWidth, this.state.boxHeight)) {
         this.state.expanded = !this.state.expanded;
         context.activeColor = null;
         return true;
      } else {
         return false;
      }
   }

   private String valueText() {
      if (this.setting.selectedCount() <= 0) {
         return ClientLanguage.tr("None");
      } else {
         StringJoiner joiner = new StringJoiner(", ");

         for (String mode : this.setting.getModes()) {
            if (this.setting.isSelected(mode)) {
               joiner.add(ClientLanguage.tr(mode));
            }
         }

         return joiner.toString();
      }
   }

   @Override
   protected boolean hasDescription() {
      return this.setting.shouldRenderDescription() && super.hasDescription();
   }
}
