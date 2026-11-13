package daamky.ua.screens.modernui.settings;

import java.awt.Color;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.settings.impl.ThemeSetting;
import daamky.ua.api.theme.ClientTheme;
import daamky.ua.api.theme.ThemeManager;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

final class ThemeSettingCard extends SettingCardComponent<ThemeSetting> {
   private static final float ROW_HEIGHT = 15.0F;
   private static final int VISIBLE_ROWS = 7;
   private float listScroll;
   private float listScrollTarget;

   ThemeSettingCard(ThemeSetting setting) {
      super(setting);
   }

   @Override
   int order() {
      return -1;
   }

   @Override
   float height(SettingCardContext context) {
      return 26.0F;
   }

   @Override
   void render(DrawContext graphics, SettingCardContext context, float cardX, float y, int mouseX, int mouseY) {
      this.markRow(cardX, y, this.height(context));
      this.state.expandAnimation.setDirection(this.state.expanded ? Direction.FORWARDS : Direction.BACKWARDS);
      float boxX = cardX + 8.0F;
      float boxY = y + 7.0F;
      float boxWidth = context.cardWidth - 18.0F;
      float boxHeight = 13.0F;
      this.state.box(boxX, boxY, boxWidth, boxHeight);
      context.renderClippedText(
         graphics, FontType.SEMIBOLD, this.setting.getName(), cardX + 10.0F, y - 1.5F, context.cardWidth - 20.0F, 6.0F, context.color(225, 225, 225, 255)
      );
      ClientTheme theme = this.setting.selectedTheme();
      this.renderSurface(boxX, boxY, boxWidth, boxHeight, 3.0F, theme, context, 1.0F);
      this.renderBadge(boxX + 3.0F, boxY + 1.5F, 14.0F, 10.0F, theme, context, 1.0F);
      context.renderClippedText(graphics, FontType.SEMIBOLD, theme.name(), boxX + 21.0F, boxY + 4.0F, boxWidth - 37.0F, 5.8F, context.color(245, 245, 245, 255));
      float progress = context.clamp(this.state.expandAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      float arrowX = boxX + boxWidth - 11.0F;
      float arrowY = boxY + 3.5F;
      Render2D.text(FontType.MAINMENUSCREEN, "0", arrowX, arrowY, 6.0F, context.color(184, 185, 191, 255), 180.0F * progress, arrowX + 2.8F, arrowY + 3.0F);
   }

   @Override
   void renderOverlay(DrawContext graphics, SettingCardContext context) {
      float animation = context.clamp(this.state.expandAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      if (this.state.visible && !(animation <= 0.01F)) {
         List<ClientTheme> themes = ThemeManager.themes();
         int visibleRows = Math.min(7, themes.size());
         float viewportHeight = (float)visibleRows * 15.0F;
         float popupX = this.state.boxX;
         float popupY = this.state.boxY + this.state.boxHeight + 2.0F;
         float popupWidth = this.state.boxWidth;
         float popupHeight = viewportHeight + 4.0F;
         float maxScroll = Math.max(0.0F, (float)themes.size() * 15.0F - viewportHeight);
         this.listScrollTarget = context.clamp(this.listScrollTarget, 0.0F, maxScroll);
         this.listScroll = context.lerp(this.listScroll, this.listScrollTarget, 0.28F);
         this.state.options(popupX, popupY, popupWidth, 15.0F, themes.size());
         ClientTheme activeTheme = this.setting.selectedTheme();
         this.renderSurface(popupX, popupY, popupWidth, popupHeight, 4.0F, activeTheme, context, animation);
         Render2D.pushScissor(graphics, popupX + 1.0F, popupY + 2.0F, popupWidth - 2.0F, viewportHeight);

         for (int index = 0; index < themes.size(); index++) {
            ClientTheme theme = themes.get(index);
            float rowY = popupY + 2.0F + (float)index * 15.0F - this.listScroll;
            if (!(rowY + 15.0F < popupY + 2.0F) && !(rowY > popupY + 2.0F + viewportHeight)) {
               boolean selected = this.setting.is(theme.name());
               boolean hovered = context.inside(context.mouseX, context.mouseY, popupX + 2.0F, rowY, popupWidth - 4.0F, 15.0F);
               if (selected || hovered) {
                  int rowAlpha = Math.round((selected ? 74.0F : 38.0F) * animation);
                  Render2D.rect(popupX + 2.0F, rowY + 1.0F, popupWidth - 4.0F, 13.0F, 3.0F, context.color(255, 255, 255, rowAlpha));
               }

               this.renderBadge(popupX + 4.0F, rowY + 2.5F, 14.0F, 10.0F, theme, context, animation);
               context.renderClippedText(
                  graphics,
                  FontType.SEMIBOLD,
                  theme.name(),
                  popupX + 23.0F,
                  rowY + 5.0F,
                  popupWidth - 42.0F,
                  5.8F,
                  context.color(245, 245, 245, Math.round(255.0F * animation))
               );
               if (selected) {
                  Render2D.text(
                     FontType.MAINMENUSCREEN, "F", popupX + popupWidth - 12.0F, rowY + 4.7F, 6.0F, context.color(250, 250, 250, Math.round(255.0F * animation))
                  );
               }
            }
         }

         Render2D.popScissor(graphics);
         if (maxScroll > 0.5F) {
            float trackY = popupY + 4.0F;
            float trackHeight = viewportHeight - 4.0F;
            float thumbHeight = Math.max(18.0F, trackHeight * viewportHeight / ((float)themes.size() * 15.0F));
            float thumbY = trackY + (trackHeight - thumbHeight) * (this.listScroll / maxScroll);
            Render2D.rect(popupX + popupWidth - 4.0F, trackY, 2.0F, trackHeight, 1.0F, context.color(255, 255, 255, Math.round(22.0F * animation)));
            Render2D.rect(popupX + popupWidth - 4.0F, thumbY, 2.0F, thumbHeight, 1.0F, context.color(210, 210, 214, Math.round(120.0F * animation)));
         }
      }
   }

   @Override
   boolean mouseClickedOverlay(Click event, SettingCardContext context) {
      if (event.button() == 0 && this.state.visible && this.state.expanded) {
         int visibleRows = Math.min(7, ThemeManager.themes().size());
         float viewportY = this.state.optionY + 2.0F;
         float viewportHeight = (float)visibleRows * 15.0F;
         if (!context.inside(event.x(), event.y(), this.state.optionX, viewportY, this.state.optionWidth, viewportHeight)) {
            return false;
         } else {
            int index = (int)((event.y() - (double)viewportY + (double)this.listScroll) / 15.0);
            if (index >= 0 && index < ThemeManager.themes().size()) {
               this.setting.setValue(ThemeManager.themes().get(index).name());
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
   boolean mouseScrolled(double mouseX, double mouseY, double scrollY, SettingCardContext context) {
      if (this.state.visible && this.state.expanded) {
         float viewportHeight = (float)Math.min(7, ThemeManager.themes().size()) * 15.0F;
         if (!context.inside(mouseX, mouseY, this.state.optionX, this.state.optionY, this.state.optionWidth, viewportHeight + 4.0F)) {
            return false;
         } else {
            float maxScroll = Math.max(0.0F, (float)ThemeManager.themes().size() * 15.0F - viewportHeight);
            this.listScrollTarget = context.clamp(this.listScrollTarget - (float)scrollY * 15.0F * 1.5F, 0.0F, maxScroll);
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   boolean mouseClicked(Click event, boolean doubled, SettingCardContext context) {
      if (!context.inside(event.x(), event.y(), this.state.boxX, this.state.boxY, this.state.boxWidth, this.state.boxHeight)) {
         return false;
      } else {
         this.state.expanded = !this.state.expanded;
         return true;
      }
   }

   private void renderSurface(float x, float y, float width, float height, float radius, ClientTheme theme, SettingCardContext context, float alpha) {
      Color top = theme.controlTop();
      Color bottom = theme.controlBottom();
      Render2D.rect(
         x,
         y,
         width,
         height,
         radius,
         context.withAlpha(top, 0.94F * alpha),
         context.withAlpha(this.mix(top, theme.accent(), 0.08F), 0.94F * alpha),
         context.withAlpha(bottom, 0.98F * alpha),
         context.withAlpha(this.mix(bottom, theme.accent(), 0.06F), 0.98F * alpha)
      );
      Color outline = theme.outline();
      Render2D.outline(
         x,
         y,
         width,
         height,
         radius,
         0.2F,
         context.withAlpha(outline, 0.42F * alpha),
         context.withAlpha(outline, 0.28F * alpha),
         context.withAlpha(outline, 0.34F * alpha),
         context.withAlpha(outline, 0.28F * alpha)
      );
   }

   private void renderBadge(float x, float y, float width, float height, ClientTheme theme, SettingCardContext context, float alpha) {
      Render2D.rect(
         x,
         y,
         width,
         height,
         3.0F,
         context.withAlpha(theme.badgeTop(), alpha),
         context.withAlpha(theme.badgeTop(), alpha),
         context.withAlpha(theme.badgeBottom(), alpha),
         context.withAlpha(theme.badgeBottom(), alpha)
      );
      float textWidth = Render2D.textWidth(FontType.SEMIBOLD, "Aa", 5.2F);
      Render2D.text(FontType.SEMIBOLD, "Aa", x + (width - textWidth) * 0.5F, y + 2.4F, 5.2F, context.color(255, 255, 255, Math.round(235.0F * alpha)));
   }

   private Color mix(Color from, Color to, float amount) {
      return new Color(
         Math.round((float)from.getRed() + (float)(to.getRed() - from.getRed()) * amount),
         Math.round((float)from.getGreen() + (float)(to.getGreen() - from.getGreen()) * amount),
         Math.round((float)from.getBlue() + (float)(to.getBlue() - from.getBlue()) * amount)
      );
   }
}
