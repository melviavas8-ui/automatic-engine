package daamky.ua.screens.modernui.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.display.Appearance;
import daamky.ua.api.theme.ClientTheme;
import daamky.ua.api.theme.ThemeManager;
import daamky.ua.manager.Manager;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.render.animation.modernfx.Decelerate;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class Header {
   private static final float SEARCH_X = 262.0F;
   private static final float SEARCH_Y = 14.0F;
   private static final float SEARCH_WIDTH = 70.0F;
   private static final float SEARCH_HEIGHT = 20.0F;
   private static final float SEARCH_ICON_BUTTON_WIDTH = 20.0F;
   private static final float SEARCH_RADIUS = 3.0F;
   private static final float SEARCH_MENU_OFFSET_X = -180.0F;
   private static final float SEARCH_MENU_OFFSET_Y = -5.0F;
   private static final float SEARCH_MENU_EXTRA_WIDTH = 350.0F;
   private static final float SEARCH_MENU_EXTRA_HEIGHT = 250.0F;
   private static final float SEARCH_RESULT_ROW_HEIGHT = 20.0F;
   private static final float SEARCH_BACKDROP_HEIGHT = 300.0F;
   private static final float CLOSED_SEARCH_ICON_OFFSET_X = 32.0F;
   private static final float LANGUAGE_ICON_OFFSET_X = 48.0F;
   private static final float LANGUAGE_ICON_OFFSET_Y = 0.7F;
   private static final float LANGUAGE_MENU_OFFSET_FACTOR = -0.2F;
   private static final float LANGUAGE_MENU_OFFSET_X = 30.0F;
   private static final float THEME_MENU_RADIUS = 5.0F;
   private float searchResultsScroll;
   private float searchResultsScrollTarget;
   private float themeMenuScroll;
   private float themeMenuScrollTarget;
   private String lastSearchQuery = "";
   private final Map<String, Float> themeRowHoverAnimations = new HashMap<>();
   private float searchIconX;
   private float searchIconY;
   private float searchIconSize = 15.0F;
   private float closeIconX;
   private float closeIconY;
   private float closeIconSize = 10.0F;
   private float languageButtonX;
   private float languageButtonY;
   private float languageButtonWidth = 15.0F;
   private float languageButtonHeight = 15.0F;
   private boolean languageMenuOpen;
   private final Decelerate languageMenuAnimation = MathUtils.createAnimation(160, Direction.BACKWARDS);
   private boolean themeMenuOpen;
   private final Decelerate themeMenuAnimation = MathUtils.createAnimation(160, Direction.BACKWARDS);
   private long searchResultsAnimationStartMs = System.currentTimeMillis();

   public Header() {
   }

   public void render(
      DrawContext graphics,
      float x,
      float y,
      float panelWidth,
      ModuleCategory previousCategory,
      ModuleCategory selectedCategory,
      float transition,
      SearchHandler searchHandler,
      List<Module> modules,
      int mouseX,
      int mouseY
   ) {
      this.renderHeaderCategory(x, y, previousCategory, selectedCategory, transition);
      this.renderSearch(graphics, x, y, panelWidth, searchHandler, modules == null ? List.of() : modules, mouseX, mouseY);
   }

   private void renderSearch(
      DrawContext graphics, float x, float y, float panelWidth, SearchHandler searchHandler, List<Module> modules, int mouseX, int mouseY
   ) {
      ClientTheme theme = ThemeManager.current();
      float fullX = x + 262.0F;
      float boxY = y + 14.0F;
      float progress = this.searchOpenProgress(searchHandler);
      float boxWidth = 20.0F + 50.0F * progress;
      float boxX = fullX + 70.0F - boxWidth;
      Header.SearchMenuBounds menu = this.searchMenuBounds(boxX, boxY, boxWidth);
      if (progress > 0.01F) {
         this.renderSearchBackdrop(x, y, panelWidth, progress);
         int outlineAlpha = Math.round(45.0F + 70.0F * progress);
         Render2D.blur(menu.x, menu.y, menu.width, menu.height, 3.0F, 18.0F, 1.0F, this.alpha(theme.accentBright(), Math.round(165.0F * progress)));
         Render2D.rect(menu.x, menu.y, menu.width, menu.height, 4.0F, this.alpha(theme.surfaceDeep(), Math.round(240.0F * progress)));
         Render2D.outline(
            menu.x,
            menu.y,
            menu.width,
            menu.height,
            4.0F,
            0.25F,
            this.alpha(theme.outline(), outlineAlpha),
            this.alpha(theme.accent(), outlineAlpha),
            this.alpha(theme.accentDark(), outlineAlpha),
            this.alpha(theme.outline(), outlineAlpha)
         );
      }

      if (progress > 0.16F) {
         this.languageMenuOpen = false;
         this.themeMenuOpen = false;
         Render2D.text(FontType.ICONS, "U", menu.x + 11.0F, menu.y + 7.0F, 13.0F, new Color(166, 184, 190, Math.round(255.0F * progress)).getRGB());
         this.closeIconX = menu.x + menu.width - 20.0F;
         this.closeIconY = menu.y + 6.0F;
         Render2D.pushScissor(graphics, menu.x + 30.0F, menu.y + 5.0F, menu.width - 64.0F, 20.0F);
         this.renderSearchText(graphics, menu.x + 30.0F, menu.y + 5.0F, menu.width - 64.0F, 18.0F, searchHandler, progress);
         Render2D.popScissor(graphics);
         this.renderCloseIcon(mouseX, mouseY, progress);
         this.renderSearchResults(graphics, menu, modules, searchHandler, progress, mouseX, mouseY);
      } else {
         int iconAlpha = Math.round(180.0F + 75.0F * progress);
         float iconAnchorX = fullX + 70.0F + 190.0F;
         this.searchIconX = iconAnchorX + 32.0F;
         this.searchIconY = boxY - 5.0F;
         this.languageButtonX = iconAnchorX - 25.0F;
         this.languageButtonY = this.searchIconY - 1.0F;
         this.renderLanguageIcon(this.languageButtonX, this.languageButtonY, iconAlpha);
         this.renderThemesIcon(this.languageButtonX, this.languageButtonY, iconAlpha);
         this.renderLanguageMenu(mouseX, mouseY, iconAlpha);
         this.renderThemeMenu(graphics, mouseX, mouseY, iconAlpha);
         Render2D.text(FontType.ICONS, "U", this.searchIconX, this.searchIconY, this.searchIconSize, new Color(255, 255, 255, iconAlpha).getRGB());
      }
   }

   private void renderSearchBackdrop(float x, float y, float panelWidth, float progress) {
      ClientTheme theme = ThemeManager.current();
      float inset = 0.0F;
      float backdropX = x + inset;
      float backdropY = y + inset;
      float backdropWidth = panelWidth - inset * 2.0F;
      float backdropHeight = 300.0F - inset * 2.0F;
      float alpha = MathUtils.clamp(progress, 0.0F, 1.0F);
      Render2D.blur(backdropX, backdropY, backdropWidth, backdropHeight, 5.0F, 45.0F, 1.0F, this.alpha(theme.accentBright(), Math.round(245.0F * alpha)));
      Render2D.rect(backdropX, backdropY, backdropWidth, backdropHeight, 5.0F, this.alpha(theme.surfaceDeep(), Math.round(222.0F * alpha)));
   }

   private void renderLanguageIcon(float x, float y, int alpha) {
      Render2D.text(FontType.GUI_ICONS, "Z", this.languageIconX() + 2.0F, this.languageIconY(), 15.0F, new Color(255, 255, 255, alpha).getRGB());
   }

   private void renderThemesIcon(float x, float y, int alpha) {
      Render2D.text(FontType.GUI_ICONS, "R", this.themeIconX() + 10.0F, this.themeIconY(), 14.0F, new Color(255, 255, 255, alpha).getRGB());
   }

   private void renderCloseIcon(int mouseX, int mouseY, float progress) {
      boolean hovered = this.isCloseIconHovered((double)mouseX, (double)mouseY);
      int alpha = Math.round((hovered ? 255.0F : 185.0F) * progress);
      Render2D.text(
         FontType.MAINMENUSCREEN, "i", this.closeIconX + 5.0F, this.closeIconY + 2.0F, this.closeIconSize - 3.0F, new Color(255, 255, 255, alpha).getRGB()
      );
   }

   private void renderLanguageMenu(int mouseX, int mouseY, int alpha) {
      this.languageMenuAnimation.setDirection(this.languageMenuOpen ? Direction.FORWARDS : Direction.BACKWARDS);
      float progress = MathUtils.clamp(this.languageMenuAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      if (!(progress <= 0.01F)) {
         float menuWidth = this.languageMenuWidth();
         float menuX = this.languageMenuX();
         float menuY = this.languageMenuY() - (1.0F - progress) * 4.0F;
         float rowHeight = 14.0F;
         float menuHeight = rowHeight * (float)ClientLanguage.Lang.values().length;
         int backgroundAlpha = Math.round(225.0F * progress);
         int outlineAlpha = Math.round(75.0F * progress);
         ClientTheme theme = ThemeManager.current();
         Render2D.blur(menuX, menuY, menuWidth, menuHeight, 4.0F, 18.0F, 1.0F, this.alpha(theme.accentBright(), Math.round(150.0F * progress)));
         Render2D.rect(
            menuX,
            menuY,
            menuWidth,
            menuHeight,
            4.0F,
            this.alpha(theme.surfaceDark(), backgroundAlpha),
            this.alpha(theme.surface(), backgroundAlpha),
            this.alpha(theme.surfaceDeep(), backgroundAlpha),
            this.alpha(theme.surfaceDark(), backgroundAlpha)
         );
         Render2D.outline(
            menuX,
            menuY,
            menuWidth,
            menuHeight,
            4.0F,
            0.25F,
            this.alpha(theme.outline(), outlineAlpha),
            this.alpha(theme.accent(), outlineAlpha),
            this.alpha(theme.accentDark(), outlineAlpha),
            this.alpha(theme.outline(), outlineAlpha)
         );
         ClientLanguage.Lang[] values = ClientLanguage.Lang.values();

         for (int i = 0; i < values.length; i++) {
            ClientLanguage.Lang lang = values[i];
            float rowY = menuY + (float)i * rowHeight;
            boolean hovered = this.inside((double)mouseX, (double)mouseY, menuX, rowY, menuWidth, rowHeight);
            boolean selected = ClientLanguage.current() == lang;
            if (hovered || selected) {
               int rowAlpha = Math.round((hovered ? 85.0F : 65.0F) * progress);
               Render2D.rect(
                  menuX + 2.0F,
                  rowY + 2.0F,
                  menuWidth - 4.0F,
                  rowHeight - 4.0F,
                  3.0F,
                  this.alpha(theme.controlTop(), rowAlpha),
                  this.alpha(theme.accent(), rowAlpha),
                  this.alpha(theme.controlBottom(), rowAlpha),
                  this.alpha(theme.controlTop(), rowAlpha)
               );
            }

            String label = lang.name();
            float size = 5.8F;
            float labelWidth = Render2D.textWidth(FontType.SEMIBOLD, label, size);
            int textAlpha = Math.round((selected ? 255.0F : 185.0F) * progress * ((float)alpha / 255.0F));
            Render2D.text(FontType.SEMIBOLD, label, menuX + (menuWidth - labelWidth) * 0.5F, rowY + 4.0F, size, new Color(255, 255, 255, textAlpha).getRGB());
         }
      }
   }

   private void renderThemeMenu(DrawContext graphics, int mouseX, int mouseY, int alpha) {
      this.themeMenuAnimation.setDirection(this.themeMenuOpen ? Direction.FORWARDS : Direction.BACKWARDS);
      float progress = MathUtils.clamp(this.themeMenuAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      if (!(progress <= 0.01F)) {
         float menuWidth = this.themeMenuWidth();
         float menuX = this.themeMenuX();
         float menuY = this.themeMenuY() - (1.0F - progress) * 8.0F;
         float rowHeight = 15.0F;
         int visibleRows = Math.min(7, ThemeManager.themes().size());
         float viewportHeight = (float)visibleRows * rowHeight;
         float menuHeight = viewportHeight + 4.0F;
         float maxScroll = Math.max(0.0F, (float)ThemeManager.themes().size() * rowHeight - viewportHeight);
         this.themeMenuScrollTarget = MathUtils.clamp(this.themeMenuScrollTarget, 0.0F, maxScroll);
         this.themeMenuScroll = this.themeMenuScroll + (this.themeMenuScrollTarget - this.themeMenuScroll) * 0.28F;
         int backgroundAlpha = Math.round(225.0F * progress);
         int outlineAlpha = Math.round(75.0F * progress);
         ClientTheme activeTheme = ThemeManager.current();
         Render2D.blur(menuX, menuY, menuWidth, menuHeight, 5.0F, 20.0F, 1.0F, this.alpha(activeTheme.accentBright(), Math.round(160.0F * progress)));
         Render2D.rect(
            menuX,
            menuY,
            menuWidth,
            menuHeight,
            5.0F,
            this.alpha(activeTheme.surfaceDark(), backgroundAlpha),
            this.alpha(activeTheme.surface(), backgroundAlpha),
            this.alpha(activeTheme.surfaceDeep(), backgroundAlpha),
            this.alpha(activeTheme.surfaceDark(), backgroundAlpha)
         );
         Render2D.outline(
            menuX,
            menuY,
            menuWidth,
            menuHeight,
            5.0F,
            0.25F,
            this.alpha(activeTheme.outline(), outlineAlpha),
            this.alpha(activeTheme.accent(), outlineAlpha),
            this.alpha(activeTheme.accentDark(), outlineAlpha),
            this.alpha(activeTheme.outline(), outlineAlpha)
         );
         Render2D.pushScissor(graphics, menuX + 1.0F, menuY + 2.0F, menuWidth - 2.0F, viewportHeight);

         for (int i = 0; i < ThemeManager.themes().size(); i++) {
            ClientTheme theme = ThemeManager.themes().get(i);
            float rowAnimationOffset = (1.0F - progress) * (float)Math.min(i, 6) * 12.2F;
            float rowY = menuY + 2.0F + (float)i * rowHeight - this.themeMenuScroll + rowAnimationOffset;
            if (!(rowY + rowHeight < menuY + 2.0F) && !(rowY > menuY + 2.0F + viewportHeight)) {
               boolean hovered = this.inside((double)mouseX, (double)mouseY, menuX, rowY, menuWidth, rowHeight);
               boolean selected = ThemeManager.current().name().equals(theme.name());
               float hoverProgress = this.animateThemeRowHover(theme.name(), hovered || selected);
               float hoverShiftX = hoverProgress * 2.0F;
               float rowProgress = MathUtils.clamp(progress - (float)i * 0.045F, 0.0F, 1.0F);
               float contentOffsetX = (1.0F - rowProgress) * 7.0F + hoverShiftX;
               float badgeY = rowY + (rowHeight - 10.0F) * 0.5F;
               float labelY = rowY + 4.55F;
               float checkY = rowY + 4.55F;
               this.renderThemeBadge(menuX + 0.8F + contentOffsetX, badgeY, theme, rowProgress);
               String label = theme.name();
               float size = 5.8F;
               int textAlpha = Math.round(((selected ? 235.0F : 178.0F) + hoverProgress * 20.0F) * rowProgress * ((float)alpha / 255.0F));
               Render2D.text(FontType.SEMIBOLD, label, menuX + 19.0F + contentOffsetX, labelY, size, new Color(255, 255, 255, textAlpha).getRGB());
               if (hovered || selected) {
                  int rowAlpha = Math.round(((selected ? 68.0F : 34.0F) + hoverProgress * 38.0F) * progress);
                  Render2D.text(FontType.SEMIBOLD, label, menuX + 19.0F + contentOffsetX, labelY, size, new Color(255, 255, 255, 255).getRGB());
               }

               if (selected) {
                  Render2D.text(
                     FontType.MAINMENUSCREEN,
                     "F",
                     menuX + menuWidth - 14.0F + contentOffsetX * 0.25F,
                     checkY,
                     6.0F,
                     new Color(255, 255, 255, textAlpha).getRGB()
                  );
               }
            }
         }

         Render2D.popScissor(graphics);
         if (maxScroll > 0.5F) {
            float trackHeight = viewportHeight - 5.0F;
            float thumbHeight = Math.max(18.0F, trackHeight * viewportHeight / ((float)ThemeManager.themes().size() * rowHeight));
            float thumbY = menuY + 4.0F + (trackHeight - thumbHeight) * (this.themeMenuScroll / maxScroll);
            Render2D.rect(menuX + menuWidth - 4.0F, menuY + 4.0F, 2.0F, trackHeight, 1.0F, new Color(255, 255, 255, Math.round(24.0F * progress)).getRGB());
            Render2D.rect(menuX + menuWidth - 4.0F, thumbY, 2.0F, thumbHeight, 1.0F, new Color(220, 220, 224, Math.round(125.0F * progress)).getRGB());
         }
      }
   }

   private void renderThemeBadge(float x, float y, ClientTheme theme, float alpha) {
      Render2D.rect(
         x + 2.0F,
         y,
         14.0F,
         10.0F,
         3.0F,
         this.alpha(theme.badgeTop(), Math.round(255.0F * alpha)),
         this.alpha(theme.badgeTop(), Math.round(255.0F * alpha)),
         this.alpha(theme.badgeBottom(), Math.round(255.0F * alpha)),
         this.alpha(theme.badgeBottom(), Math.round(255.0F * alpha))
      );
      float width = Render2D.textWidth(FontType.SEMIBOLD, "Aa", 5.2F);
      Render2D.text(FontType.SEMIBOLD, "Aa", x + (17.0F - width) * 0.5F, y + 2.4F, 5.2F, new Color(255, 255, 255, Math.round(235.0F * alpha)).getRGB());
   }

   private float animateThemeRowHover(String themeName, boolean active) {
      float current = this.themeRowHoverAnimations.getOrDefault(themeName, 0.0F);
      float target = active ? 1.0F : 0.0F;
      current += (target - current) * 0.24F;
      if (Math.abs(target - current) < 0.01F) {
         current = target;
      }

      this.themeRowHoverAnimations.put(themeName, current);
      return current;
   }

   private void renderSearchText(
      DrawContext graphics, float boxX, float boxY, float boxWidth, float boxHeight, SearchHandler searchHandler, float alphaMultiplier
   ) {
      float textY = boxY + 3.0F;
      float textSize = 7.0F;
      String text = searchHandler.getSearchText();
      float focus = MathUtils.clamp(searchHandler.getSearchFocusAnimation(), 0.0F, 1.0F);
      float alpha = MathUtils.clamp(alphaMultiplier, 0.0F, 1.0F);
      if (text.isEmpty()) {
         if (searchHandler.isSearchActive()) {
            this.renderSearchCursor(boxX, boxY, boxHeight, searchHandler, alpha);
         } else {
            int placeholderAlpha = Math.round(160.0F * (1.0F - focus) * alpha);
            if (placeholderAlpha > 0) {
               Render2D.text(FontType.SEMIBOLD, ClientLanguage.tr("Search"), boxX, textY, textSize, new Color(128, 128, 128, placeholderAlpha).getRGB());
            }
         }
      } else {
         float textWidth = Render2D.textWidth(FontType.SEMIBOLD, text, textSize);
         searchHandler.updateSearchTextScroll(boxWidth, textWidth);
         Render2D.pushScissor(graphics, boxX, boxY, boxWidth, boxHeight);
         Render2D.text(
            FontType.SEMIBOLD,
            text,
            boxX - searchHandler.getSearchTextScrollOffset(),
            textY + 3.0F,
            textSize,
            new Color(255, 255, 255, Math.round(255.0F * alpha)).getRGB()
         );
         if (searchHandler.isSearchActive()) {
            this.renderSearchCursor(
               boxX - searchHandler.getSearchTextScrollOffset() + this.cursorTextWidth(searchHandler, textSize), boxY, boxHeight, searchHandler, alpha
            );
         }

         Render2D.popScissor(graphics);
      }
   }

   private void renderSearchCursor(float textAreaX, float boxY, float boxHeight, SearchHandler searchHandler, float alphaMultiplier) {
      float cursorAlpha = (float)(Math.sin((double)searchHandler.getSearchCursorBlink() * Math.PI * 2.0) * 0.5 + 0.5);
      if (!(cursorAlpha <= 0.3F)) {
         Render2D.rect(
            textAreaX, boxY + 4.0F, 0.5F, boxHeight - 8.0F, 0.0F, new Color(255, 255, 255, Math.round(255.0F * cursorAlpha * alphaMultiplier)).getRGB()
         );
      }
   }

   private void renderHeaderCategory(float x, float y, ModuleCategory previousCategory, ModuleCategory selectedCategory, float transition) {
      float progress = MathUtils.clamp(transition, 0.0F, 1.0F);
      float baseX = x + 100.0F;
      float baseY = y + 16.0F;
      if (previousCategory != null && previousCategory != selectedCategory && progress < 0.999F) {
         float outgoingProgress = 1.0F - progress;
         this.renderHeaderCategoryLabel(previousCategory, baseX - progress * 10.0F, baseY - progress * 4.0F, outgoingProgress);
      }

      if (selectedCategory != null) {
         float incomingOffsetX = (1.0F - progress) * 12.0F;
         float incomingOffsetY = (1.0F - progress) * 4.0F;
         this.renderHeaderCategoryLabel(selectedCategory, baseX + incomingOffsetX, baseY + incomingOffsetY, progress);
      }
   }

   private void renderHeaderCategoryLabel(ModuleCategory category, float x, float y, float alphaMultiplier) {
      if (category != null) {
         int alpha = Math.round(255.0F * MathUtils.clamp(alphaMultiplier, 0.0F, 1.0F));
         if (alpha > 0) {
            int color = new Color(255, 255, 255, alpha).getRGB();
            float iconSize = 8.5F;
            float iconWidth = Render2D.textWidth(FontType.MAINMENUSCREEN, category.icon(), iconSize);
            float iconOffsetX = -37.0F;
            float iconOffsetY = 4.5F;
            Render2D.text(FontType.MAINMENUSCREEN, category.icon(), x + iconOffsetX, y + 7.5F - iconOffsetY, iconSize, color);
            Render2D.rect(x + iconWidth + 2.5F + iconOffsetX, y + 10.0F - iconOffsetY, 4.0F, 4.0F, 1.5F, color);
            Render2D.text(FontType.BOLD, ClientLanguage.tr(category.getDisplayName()), x + iconWidth + 10.0F + iconOffsetX, y + 7.5F - iconOffsetY, 7.0F, color);
         }
      }
   }

   private void renderSearchResults(
      DrawContext graphics, Header.SearchMenuBounds menu, List<Module> modules, SearchHandler searchHandler, float progress, int mouseX, int mouseY
   ) {
      String query = searchHandler.getSearchText();
      if (!query.equals(this.lastSearchQuery)) {
         this.searchResultsScroll = 0.0F;
         this.searchResultsScrollTarget = 0.0F;
         this.lastSearchQuery = query;
         this.searchResultsAnimationStartMs = System.currentTimeMillis();
      }

      float labelX = menu.x + 10.0F;
      float labelY = menu.y + 34.0F;
      Render2D.text(FontType.SEMIBOLD, ClientLanguage.tr("Modules"), labelX, labelY, 5.5F, new Color(134, 148, 154, Math.round(230.0F * progress)).getRGB());
      float listX = menu.x + 8.0F;
      float listY = menu.y + 50.0F;
      float listWidth = menu.width - 16.0F;
      float listHeight = menu.height - 58.0F;
      float contentHeight = (float)modules.size() * 20.0F;
      float maxScroll = Math.max(0.0F, contentHeight - listHeight);
      this.searchResultsScrollTarget = MathUtils.clamp(this.searchResultsScrollTarget, 0.0F, maxScroll);
      this.searchResultsScroll = MathUtils.clamp(this.searchResultsScroll, 0.0F, maxScroll);
      this.searchResultsScroll = this.searchResultsScroll + (this.searchResultsScrollTarget - this.searchResultsScroll) * 0.28F;
      if (Math.abs(this.searchResultsScrollTarget - this.searchResultsScroll) < 0.08F) {
         this.searchResultsScroll = this.searchResultsScrollTarget;
      }

      if (modules.isEmpty()) {
         Render2D.text(
            FontType.SEMIBOLD,
            ClientLanguage.tr("No modules found"),
            listX + 2.0F,
            listY + 8.0F,
            6.0F,
            new Color(134, 148, 154, Math.round(210.0F * progress)).getRGB()
         );
      } else {
         Render2D.pushScissor(graphics, listX, listY, listWidth, listHeight);

         for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            float rowY = listY + (float)i * 20.0F - this.searchResultsScroll;
            if (!(rowY + 20.0F < listY) && !(rowY > listY + listHeight)) {
               this.renderSearchResultRow(graphics, module, listX, rowY, listWidth, progress, i, mouseX, mouseY);
            }
         }

         Render2D.popScissor(graphics);
         if (maxScroll > 0.5F) {
            this.renderSearchScrollbar(menu, listY, listHeight, contentHeight, maxScroll, progress);
         }
      }
   }

   private void renderSearchResultRow(DrawContext graphics, Module module, float x, float y, float width, float progress, int index, int mouseX, int mouseY) {
      float rowProgress = MathUtils.clamp((float)(System.currentTimeMillis() - this.searchResultsAnimationStartMs) / 220.0F - (float)index * 0.035F, 0.0F, 1.0F);
      rowProgress = 1.0F - (1.0F - rowProgress) * (1.0F - rowProgress);
      float rowAlpha = progress * rowProgress;
      y += (1.0F - rowProgress) * 5.0F;
      boolean hovered = mouseX != Integer.MIN_VALUE
         && mouseY != Integer.MIN_VALUE
         && (float)mouseX >= x
         && (float)mouseX <= x + width
         && (float)mouseY >= y
         && (float)mouseY <= y + 20.0F - 2.0F;
      boolean enabled = module.isEnabled();
      if (hovered || enabled) {
         int alpha = Math.round((hovered ? 26.0F : 14.0F) * rowAlpha);
         Render2D.rect(x, y + 1.0F, width, 17.0F, 3.0F, new Color(106, 142, 154, alpha).getRGB());
      }

      int iconAlpha = Math.round((enabled ? 245.0F : 165.0F) * rowAlpha);
      int textAlpha = Math.round((enabled ? 255.0F : 225.0F) * rowAlpha);
      int metaAlpha = Math.round(165.0F * rowAlpha);
      Render2D.text(FontType.HUD_ICONS, "o", x + 8.0F, y + 5.0F, 8.0F, new Color(158, 176, 182, iconAlpha).getRGB());
      ModuleCategory category = module.getCategory();
      if (category != null) {
         String categoryText = ClientLanguage.tr(category.getDisplayName());
         float categoryTextSize = 5.5F;
         float categoryTextWidth = Render2D.textWidth(FontType.SEMIBOLD, categoryText, categoryTextSize);
         float categoryIconSize = 6.5F;
         float categoryX = x + width - categoryTextWidth - 19.0F;
         float nameX = x + 24.0F;
         float nameWidth = Math.max(20.0F, categoryX - nameX - 12.0F);
         String name = this.fitText(FontType.SEMIBOLD, ClientLanguage.tr(module.getName()), 6.5F, nameWidth);
         Render2D.pushScissor(graphics, nameX, y + 1.0F, nameWidth, 17.0F);
         Render2D.text(FontType.SEMIBOLD, name, nameX, y + 5.0F, 6.5F, new Color(235, 235, 238, textAlpha).getRGB());
         Render2D.popScissor(graphics);
         Render2D.text(FontType.MAINMENUSCREEN, category.icon(), categoryX, y + 5.5F, categoryIconSize, new Color(136, 154, 160, metaAlpha).getRGB());
         Render2D.text(FontType.SEMIBOLD, categoryText, categoryX + 13.0F, y + 5.7F, categoryTextSize, new Color(136, 154, 160, metaAlpha).getRGB());
      }
   }

   private void renderSearchScrollbar(Header.SearchMenuBounds menu, float listY, float listHeight, float contentHeight, float maxScroll, float progress) {
      float trackX = menu.x + menu.width - 11.0F;
      float trackY = listY + 2.0F;
      float trackHeight = listHeight - 4.0F;
      float thumbHeight = Math.max(24.0F, trackHeight * (listHeight / contentHeight));
      float thumbY = trackY + (trackHeight - thumbHeight) * (this.searchResultsScroll / maxScroll);
      Render2D.rect(trackX, trackY, 2.0F, trackHeight, 1.0F, new Color(110, 134, 142, Math.round(28.0F * progress)).getRGB());
      Render2D.rect(trackX, thumbY, 2.0F, thumbHeight, 1.0F, new Color(118, 152, 162, Math.round(96.0F * progress)).getRGB());
   }

   public Module mouseClickedSearch(Click event, float x, float y, SearchHandler searchHandler, List<Module> modules) {
      return event.button() == 0 && this.isSearchBoxHovered(event.x(), event.y(), x, y, searchHandler)
         ? this.moduleAt(event.x(), event.y(), x, y, searchHandler, modules == null ? List.of() : modules)
         : null;
   }

   public boolean mouseScrolledSearch(double mouseX, double mouseY, double scrollY, float x, float y, SearchHandler searchHandler, List<Module> modules) {
      if (this.isSearchBoxHovered(mouseX, mouseY, x, y, searchHandler) && modules != null && !modules.isEmpty()) {
         Header.SearchMenuBounds menu = this.searchMenuBounds(x, y, searchHandler);
         float listHeight = menu.height - 58.0F;
         float maxScroll = Math.max(0.0F, (float)modules.size() * 20.0F - listHeight);
         if (maxScroll <= 0.5F) {
            return true;
         } else {
            this.searchResultsScrollTarget = MathUtils.clamp(this.searchResultsScrollTarget - (float)scrollY * 20.0F * 1.25F, 0.0F, maxScroll);
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean isSearchBoxHovered(double mouseX, double mouseY, float x, float y, SearchHandler searchHandler) {
      if (!(this.searchOpenProgress(searchHandler) > 0.01F)) {
         return this.isSearchIconHovered(mouseX, mouseY);
      } else {
         Header.SearchMenuBounds menu = this.searchMenuBounds(x, y, searchHandler);
         return mouseX >= (double)menu.x && mouseX <= (double)(menu.x + menu.width) && mouseY >= (double)menu.y && mouseY <= (double)(menu.y + menu.height);
      }
   }

   private boolean isSearchIconHovered(double mouseX, double mouseY) {
      float padding = 4.0F;
      float left = Math.max(this.searchIconX - padding, this.closedIconSplitX());
      return mouseX >= (double)left
         && mouseX <= (double)(this.searchIconX + this.searchIconSize + padding)
         && mouseY >= (double)(this.searchIconY - padding)
         && mouseY <= (double)(this.searchIconY + this.searchIconSize + padding);
   }

   public boolean isCloseIconClicked(double mouseX, double mouseY, SearchHandler searchHandler) {
      return this.searchOpenProgress(searchHandler) > 0.16F && this.isCloseIconHovered(mouseX, mouseY);
   }

   private boolean isCloseIconHovered(double mouseX, double mouseY) {
      float hitboxX = this.closeIconX + 3.0F;
      float hitboxY = this.closeIconY - 2.0F;
      float hitboxWidth = this.closeIconSize + 8.0F;
      float hitboxHeight = this.closeIconSize + 8.0F;
      return mouseX >= (double)hitboxX && mouseX <= (double)(hitboxX + hitboxWidth) && mouseY >= (double)hitboxY && mouseY <= (double)(hitboxY + hitboxHeight);
   }

   private String fitText(FontType font, String text, float size, float width) {
      if (text != null && !text.isEmpty() && !(Render2D.textWidth(font, text, size) <= width)) {
         String suffix = "...";
         float suffixWidth = Render2D.textWidth(font, suffix, size);
         if (suffixWidth >= width) {
            return "";
         } else {
            int low = 0;
            int high = text.length();

            while (low < high) {
               int mid = low + high + 1 >>> 1;
               String candidate = text.substring(0, mid) + suffix;
               if (Render2D.textWidth(font, candidate, size) <= width) {
                  low = mid;
               } else {
                  high = mid - 1;
               }
            }

            return text.substring(0, low) + suffix;
         }
      } else {
         return text == null ? "" : text;
      }
   }

   private float cursorTextWidth(SearchHandler searchHandler, float textSize) {
      String text = searchHandler.getSearchText();
      int cursor = Math.max(0, Math.min(searchHandler.getSearchCursorPosition(), text.length()));
      return Render2D.textWidth(FontType.SEMIBOLD, text.substring(0, cursor), textSize) + 1.0F;
   }

   private float searchOpenProgress(SearchHandler searchHandler) {
      float focus = MathUtils.clamp(searchHandler.getSearchFocusAnimation(), 0.0F, 1.0F);
      return !searchHandler.getSearchText().isEmpty() ? 1.0F : focus;
   }

   private Module moduleAt(double mouseX, double mouseY, float x, float y, SearchHandler searchHandler, List<Module> modules) {
      if (this.searchOpenProgress(searchHandler) <= 0.16F) {
         return null;
      } else {
         Header.SearchMenuBounds menu = this.searchMenuBounds(x, y, searchHandler);
         float listX = menu.x + 8.0F;
         float listY = menu.y + 50.0F;
         float listWidth = menu.width - 16.0F;
         float listHeight = menu.height - 58.0F;
         if (!(mouseX < (double)listX) && !(mouseX > (double)(listX + listWidth)) && !(mouseY < (double)listY) && !(mouseY > (double)(listY + listHeight))) {
            int index = (int)((mouseY - (double)listY + (double)this.searchResultsScroll) / 20.0);
            return index >= 0 && index < modules.size() ? modules.get(index) : null;
         } else {
            return null;
         }
      }
   }

   private Header.SearchMenuBounds searchMenuBounds(float panelX, float panelY, SearchHandler searchHandler) {
      float fullX = panelX + 262.0F;
      float boxY = panelY + 14.0F;
      float progress = this.searchOpenProgress(searchHandler);
      float boxWidth = 20.0F + 50.0F * progress;
      float boxX = fullX + 70.0F - boxWidth;
      return this.searchMenuBounds(boxX, boxY, boxWidth);
   }

   private Header.SearchMenuBounds searchMenuBounds(float boxX, float boxY, float boxWidth) {
      return new Header.SearchMenuBounds(boxX + -180.0F, boxY + -5.0F, boxWidth + 350.0F, 270.0F);
   }

   public boolean isSearchIconClicked(double mouseX, double mouseY, SearchHandler searchHandler) {
      if (this.searchOpenProgress(searchHandler) > 0.16F) {
         return false;
      } else {
         float clickPadding = 5.0F;
         return this.isSearchIconHovered(mouseX, mouseY);
      }
   }

   public boolean isLanguageIconClicked(double mouseX, double mouseY, SearchHandler searchHandler) {
      if (this.searchOpenProgress(searchHandler) > 0.16F) {
         return false;
      } else {
         float iconX = this.languageIconX();
         float iconY = this.languageIconY();
         float iconWidth = 15.0F;
         float iconHeight = 15.0F;
         float clickPadding = 2.0F;
         float right = Math.min(iconX + iconWidth + clickPadding, this.closedIconSplitX());
         return mouseX >= (double)(iconX - clickPadding)
            && mouseX <= (double)right
            && mouseY >= (double)(iconY - clickPadding)
            && mouseY <= (double)(iconY + iconHeight + clickPadding);
      }
   }

   public boolean isThemeIconClicked(double mouseX, double mouseY, SearchHandler searchHandler) {
      if (this.searchOpenProgress(searchHandler) > 0.16F) {
         return false;
      } else {
         float padding = 1.0F;
         return mouseX >= (double)(this.themeIconX() - padding)
            && mouseX <= (double)(this.themeIconX() + 13.0F + padding)
            && mouseY >= (double)(this.themeIconY() - padding)
            && mouseY <= (double)(this.themeIconY() + 13.0F + padding);
      }
   }

   public boolean mouseClickedLanguage(Click event, SearchHandler searchHandler) {
      if (event.button() == 0 && !(this.searchOpenProgress(searchHandler) > 0.16F)) {
         if (this.isLanguageIconClicked(event.x(), event.y(), searchHandler)) {
            this.languageMenuOpen = !this.languageMenuOpen;
            this.themeMenuOpen = false;
            return true;
         } else if (!this.languageMenuOpen) {
            return false;
         } else {
            ClientLanguage.Lang clicked = this.languageAt(event.x(), event.y());
            if (clicked != null) {
               ClientLanguage.set(clicked);
            }

            this.languageMenuOpen = false;
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean mouseClickedTheme(Click event, SearchHandler searchHandler) {
      if (event.button() == 0 && !(this.searchOpenProgress(searchHandler) > 0.16F)) {
         if (this.isThemeIconClicked(event.x(), event.y(), searchHandler)) {
            this.themeMenuOpen = !this.themeMenuOpen;
            this.languageMenuOpen = false;
            return true;
         } else if (!this.themeMenuOpen) {
            return false;
         } else {
            ClientTheme clicked = this.themeAt(event.x(), event.y());
            if (clicked != null) {
               ThemeManager.select(clicked.name());
               if (Manager.getModules() != null) {
                  Manager.getModules().getByType(Appearance.class).ifPresent(appearance -> appearance.theme().setValue(clicked.name()));
               }
            }

            this.themeMenuOpen = false;
            return true;
         }
      } else {
         return false;
      }
   }

   private ClientLanguage.Lang languageAt(double mouseX, double mouseY) {
      float rowHeight = 14.0F;
      ClientLanguage.Lang[] values = ClientLanguage.Lang.values();

      for (int i = 0; i < values.length; i++) {
         if (this.inside(mouseX, mouseY, this.languageMenuX(), this.languageMenuY() + (float)i * rowHeight, this.languageMenuWidth(), rowHeight)) {
            return values[i];
         }
      }

      return null;
   }

   private ClientTheme themeAt(double mouseX, double mouseY) {
      float menuWidth = this.themeMenuWidth();
      float menuX = this.themeMenuX();
      float menuY = this.themeMenuY();
      float rowHeight = 15.0F;
      float viewportHeight = (float)Math.min(7, ThemeManager.themes().size()) * rowHeight;
      if (!this.inside(mouseX, mouseY, menuX, menuY, menuWidth, viewportHeight + 4.0F)) {
         return null;
      } else {
         int index = (int)((mouseY - (double)menuY - 2.0 + (double)this.themeMenuScroll) / (double)rowHeight);
         return index >= 0 && index < ThemeManager.themes().size() ? ThemeManager.themes().get(index) : null;
      }
   }

   public boolean mouseScrolledTheme(double mouseX, double mouseY, double scrollY, SearchHandler searchHandler) {
      if (this.themeMenuOpen && !(this.searchOpenProgress(searchHandler) > 0.16F)) {
         float rowHeight = 15.0F;
         float viewportHeight = (float)Math.min(7, ThemeManager.themes().size()) * rowHeight;
         if (!this.inside(mouseX, mouseY, this.themeMenuX(), this.themeMenuY(), this.themeMenuWidth(), viewportHeight + 4.0F)) {
            return false;
         } else {
            float maxScroll = Math.max(0.0F, (float)ThemeManager.themes().size() * rowHeight - viewportHeight);
            this.themeMenuScrollTarget = MathUtils.clamp(this.themeMenuScrollTarget - (float)scrollY * rowHeight * 1.5F, 0.0F, maxScroll);
            return true;
         }
      } else {
         return false;
      }
   }

   private float languageIconX() {
      return this.languageButtonX + 48.0F;
   }

   private float languageIconY() {
      return this.languageButtonY + 0.7F;
   }

   private float themeIconX() {
      return this.languageIconX() - 18.0F;
   }

   private float themeIconY() {
      return this.languageIconY();
   }

   private float languageMenuWidth() {
      return 34.0F;
   }

   private float languageMenuX() {
      return this.languageButtonX - (this.languageMenuWidth() - this.languageButtonWidth) * -0.2F + 30.0F;
   }

   private float languageMenuY() {
      return this.languageButtonY + this.languageButtonHeight + 4.0F;
   }

   private float themeMenuWidth() {
      return 126.0F;
   }

   private float themeMenuX() {
      return this.themeIconX() + 60.0F;
   }

   private float themeMenuY() {
      return this.languageButtonY + this.languageButtonHeight + 4.0F;
   }

   private float closedIconSplitX() {
      return (this.languageIconX() + this.searchIconX) * 0.5F + this.searchIconSize * 0.5F;
   }

   private boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
   }

   private int alpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha))).getRGB();
   }

   private static record SearchMenuBounds(float x, float y, float width, float height) {
   }
}
