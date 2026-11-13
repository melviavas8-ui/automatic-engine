package daamky.ua.screens.modernui.impl;

import java.awt.Color;
import net.minecraft.client.gui.Click;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.misc.ClientSounds;
import daamky.ua.api.theme.ClientTheme;
import daamky.ua.api.theme.ThemeManager;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.render.animation.modernfx.Decelerate;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class CategoryPanel {
   private static final int SWITCH_ANIMATION_MS = 180;
   private static ModuleCategory persistedCategory = ModuleCategory.COMBAT;
   private ModuleCategory selectedCategory = persistedCategory;
   private ModuleCategory previousCategory = persistedCategory;
   private final Decelerate switchAnimation = MathUtils.createAnimation(180, Direction.FORWARDS);

   public CategoryPanel() {
   }

   public void render(float x, float y, int mouseX, int mouseY) {
      float categoryStartY = 70.0F;
      float categoryStepY = 22.5F;
      float progress = this.headerProgress();
      ModuleCategory[] categories = ModuleCategory.values();
      this.renderSelectionIndicator(x, y, categoryStartY, categoryStepY, progress);

      for (int i = 0; i < categories.length; i++) {
         ModuleCategory category = categories[i];
         boolean selected = category == this.selectedCategory;
         boolean hovered = !selected
            && MathUtils.inside((double)mouseX, (double)mouseY, x + 17.5F, y + categoryStartY + (float)i * categoryStepY - 5.0F, 20.0F, 20.0F);
         this.renderCategoryItem(x, y + categoryStartY + (float)i * categoryStepY, category, selected, hovered);
      }
   }

   public boolean mouseClicked(Click event, float x, float y) {
      if (event.button() != 0) {
         return false;
      } else {
         ModuleCategory category = this.categoryAt(event.x(), event.y(), x, y);
         if (category == null) {
            return false;
         } else {
            this.selectCategory(category);
            return true;
         }
      }
   }

   public ModuleCategory previousCategory() {
      return this.previousCategory;
   }

   public ModuleCategory selectedCategory() {
      return this.selectedCategory;
   }

   public float headerProgress() {
      this.switchAnimation.setDirection(Direction.FORWARDS);
      return MathUtils.clamp(this.switchAnimation.getOutput().floatValue(), 0.0F, 1.0F);
   }

   private void renderCategoryItem(float x, float itemY, ModuleCategory category, boolean selected, boolean hovered) {
      ClientTheme theme = ThemeManager.current();
      float baseWidth = 20.0F;
      float baseHeight = 20.0F;
      float baseX = x + 17.5F;
      float baseY = itemY - 5.0F;
      if (hovered) {
         Render2D.rect(
            baseX,
            baseY,
            baseWidth,
            baseHeight,
            4.0F,
            this.alpha(theme.controlTop(), 95),
            this.alpha(theme.accent(), 70),
            this.alpha(theme.controlBottom(), 95),
            this.alpha(theme.accentDark(), 75)
         );
      }

      if (selected) {
         Render2D.outline(
            baseX,
            baseY,
            baseWidth,
            baseHeight,
            5.0F,
            0.2F,
            this.alpha(theme.outline(), 175),
            this.alpha(theme.accent(), 140),
            this.alpha(theme.accentDark(), 130),
            this.alpha(theme.outline(), 165)
         );
      }

      int color = selected ? new Color(234, 234, 234, 255).getRGB() : new Color(176, 176, 176, 215).getRGB();
      float iconX = x + 22.5F;
      Render2D.text(FontType.MAINMENUSCREEN, category.icon(), iconX, itemY, 10.0F, color);
   }

   private void renderSelectionIndicator(float x, float y, float categoryStartY, float categoryStepY, float progress) {
      ClientTheme theme = ThemeManager.current();
      int previousIndex = this.categoryIndex(this.previousCategory);
      int selectedIndex = this.categoryIndex(this.selectedCategory);
      float interpolatedIndex = (float)previousIndex + (float)(selectedIndex - previousIndex) * progress;
      float indicatorY = y + categoryStartY + interpolatedIndex * categoryStepY - 5.0F;
      float glowAlpha = 105.0F + 70.0F * progress;
      Render2D.blur(x + 17.5F, indicatorY, 20.0F, 20.0F, 5.0F, 16.0F, 1.0F, this.alpha(theme.accentBright(), Math.round(glowAlpha)));
      Render2D.rect(
         x + 17.5F,
         indicatorY,
         20.0F,
         20.0F,
         5.0F,
         this.alpha(theme.controlTop(), 175),
         this.alpha(theme.accent(), 138),
         this.alpha(theme.accentDark(), 150),
         this.alpha(theme.controlBottom(), 175)
      );
   }

   private int alpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha))).getRGB();
   }

   public boolean selectCategory(ModuleCategory category) {
      if (category != null && category != this.selectedCategory) {
         this.previousCategory = this.selectedCategory;
         this.selectedCategory = category;
         persistedCategory = category;
         this.switchAnimation.setDirection(Direction.BACKWARDS);
         this.switchAnimation.reset();
         this.switchAnimation.setDirection(Direction.FORWARDS);
         ClientSounds.playSwitchCategorySound();
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollY, float x, float y) {
      if (scrollY != 0.0 && (this.categoryAt(mouseX, mouseY, x, y) != null || MathUtils.inside(mouseX, mouseY, x + 7.5F, y + 60.0F, 40.0F, 175.0F))) {
         ModuleCategory[] categories = ModuleCategory.values();
         int index = this.categoryIndex(this.selectedCategory);
         int direction = scrollY < 0.0 ? 1 : -1;
         int nextIndex = Math.max(0, Math.min(categories.length - 1, index + direction));
         return nextIndex == index ? true : this.selectCategory(categories[nextIndex]);
      } else {
         return false;
      }
   }

   private int categoryIndex(ModuleCategory category) {
      ModuleCategory[] categories = ModuleCategory.values();

      for (int i = 0; i < categories.length; i++) {
         if (categories[i] == category) {
            return i;
         }
      }

      return 0;
   }

   private ModuleCategory categoryAt(double mouseX, double mouseY, float x, float y) {
      float categoryPanelX = 7.5F;
      float categoryPanelWidth = 83.0F;
      float categoryStartY = 70.0F;
      float categoryStepY = 22.5F;
      float categoryHitHeight = 14.0F;
      if (!(mouseX < (double)(x + categoryPanelX)) && !(mouseX > (double)(x + categoryPanelX + categoryPanelWidth))) {
         ModuleCategory[] categories = ModuleCategory.values();

         for (int i = 0; i < categories.length; i++) {
            float categoryY = y + categoryStartY + (float)i * categoryStepY - 5.0F;
            if (mouseY >= (double)categoryY && mouseY <= (double)(categoryY + categoryHitHeight + 6.0F)) {
               return categories[i];
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
