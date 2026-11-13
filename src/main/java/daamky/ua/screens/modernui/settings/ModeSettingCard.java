package daamky.ua.screens.modernui.settings;

import java.awt.Color;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

final class ModeSettingCard extends SettingCardComponent<ModeSetting> {
   private static final float MODE_BOX_RADIUS = 3.0F;

   ModeSettingCard(ModeSetting setting) {
      super(setting);
   }

   @Override
   int order() {
      return 0;
   }

   @Override
   float height(SettingCardContext context) {
      this.state.expandAnimation.setDirection(this.state.expanded ? Direction.FORWARDS : Direction.BACKWARDS);
      return 25.0F + (float)this.setting.getVisibleModes().size() * 15.0F * context.clamp(this.state.expandAnimation.getOutput().floatValue(), 0.0F, 1.0F);
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
      Render2D.text(FontType.SEMIBOLD, ClientLanguage.tr(this.setting.getName()), cardX + 10.0F, y - 1.5F, 6.0F, context.color(225, 225, 225, 255));
      Render2D.rect(
         boxX,
         boxY,
         boxWidth,
         boxHeight,
         3.0F,
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
         3.0F,
         0.15F,
         context.withAlpha(context.theme().outline(), 0.65F),
         context.withAlpha(context.theme().outline(), 0.48F),
         context.withAlpha(context.theme().outline(), 0.54F),
         context.withAlpha(context.theme().outline(), 0.48F)
      );
      context.renderClippedText(
         graphics,
         FontType.BOLD,
         ClientLanguage.tr(this.setting.getValue()),
         boxX + 14.0F,
         boxY + 3.0F,
         boxWidth - 31.0F,
         5.5F,
         new Color(255, 255, 255, 255).getRGB()
      );
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
         List<String> visibleModes = this.setting.getVisibleModes();
         float optionHeight = 15.0F;
         float popupHeight = (float)visibleModes.size() * optionHeight * animation;
         float popupY = this.state.boxY + this.state.boxHeight + 1.0F;
         this.state.options(this.state.boxX, popupY, this.state.boxWidth, optionHeight, visibleModes.size());
         Render2D.rect(
            this.state.boxX,
            popupY,
            this.state.boxWidth,
            popupHeight,
            3.0F,
            context.withAlpha(context.theme().controlTop(), 0.98F * animation),
            context.withAlpha(context.theme().controlTop(), animation),
            context.withAlpha(context.theme().controlBottom(), animation),
            context.withAlpha(context.theme().controlBottom(), 0.98F * animation)
         );
         Render2D.outline(
            this.state.boxX,
            popupY,
            this.state.boxWidth,
            popupHeight,
            3.0F,
            0.15F,
            context.withAlpha(context.theme().outline(), 0.68F * animation),
            context.withAlpha(context.theme().outline(), 0.5F * animation),
            context.withAlpha(context.theme().outline(), 0.58F * animation),
            context.withAlpha(context.theme().outline(), 0.5F * animation)
         );
         Render2D.pushScissor(graphics, this.state.boxX, popupY, this.state.boxWidth, popupHeight);
         float y = popupY;

         for (String mode : visibleModes) {
            boolean selected = this.setting.isSelected(mode);
            int alpha = Math.round((selected ? 255.0F : 155.0F) * animation);
            context.renderClippedText(
               graphics,
               FontType.SEMIBOLD,
               ClientLanguage.tr(mode),
               this.state.boxX + 14.0F,
               y + 5.5F,
               this.state.boxWidth - 23.0F,
               5.5F,
               new Color(245, 245, 245, alpha).getRGB()
            );
            if (this.setting.isSelected(mode)) {
               Render2D.text(FontType.CATEGORY_ICONS, "g", this.state.boxX + 4.0F, y + 4.5F, 8.0F, new Color(255, 255, 255, 255).getRGB());
            } else if (!this.setting.isSelected(mode)) {
               Render2D.text(FontType.CATEGORY_ICONS, "g", this.state.boxX + 4.0F, y + 4.5F, 8.0F, new Color(180, 180, 180, 225).getRGB());
            }

            if (this.setting.isSelected(mode)) {
               Render2D.text(FontType.MAINMENUSCREEN, "F", this.state.boxX + this.state.boxWidth - 12.0F, y + 4.5F, 6.0F, context.color(245, 245, 245, alpha));
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
            List<String> visibleModes = this.setting.getVisibleModes();
            int index = (int)((event.y() - (double)this.state.optionY) / (double)this.state.optionHeight);
            if (index >= 0 && index < visibleModes.size()) {
               this.setting.setValue(visibleModes.get(index));
               this.state.expanded = false;
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

   @Override
   protected boolean hasDescription() {
      return this.setting.shouldRenderDescription() && super.hasDescription();
   }
}
