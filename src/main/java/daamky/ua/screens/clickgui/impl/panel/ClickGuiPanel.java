package daamky.ua.screens.clickgui.impl.panel;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.manager.Manager;
import daamky.ua.screens.clickgui.impl.module.ModuleOption;
import daamky.ua.screens.clickgui.impl.module.ModuleOptionFactory;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class ClickGuiPanel {
   private static final float HEADER_HEIGHT = 34.0F;
   private static final float ROW_HEIGHT = 18.0F;
   private static final float BOTTOM_PADDING = 7.0F;
   private static final float FIXED_HEIGHT = 280.0F;
   private static final float SCROLL_STEP = 24.0F;
   private final ModuleCategory category;
   private final Map<Module, ModuleOption> options = new IdentityHashMap<>();
   private final SmoothAnimation xAnimation = new SmoothAnimation();
   private final SmoothAnimation scrollAnimation = new SmoothAnimation();
   private float scrollTarget;
   private float scrollLimit;
   private float lastX;
   private float lastY;
   private float lastWidth;
   private float lastHeight;
   private float lastScale = 1.0F;

   public ClickGuiPanel(ModuleCategory category) {
      this.category = category;
   }

   public ModuleCategory category() {
      return this.category;
   }

   public void snapX(float x) {
      this.xAnimation.set((double)x);
   }

   public float animatedX(float targetX) {
      this.xAnimation.run((double)targetX, 0.18, Easings.CUBIC_OUT, true);
      this.xAnimation.update();
      return this.xAnimation.get();
   }

   public float currentX() {
      return this.xAnimation.get();
   }

   public void updateAnimations() {
      for (Module module : this.modules()) {
         this.optionFor(module).updateAnimation();
      }

      this.updateScroll();
   }

   public float height() {
      return 280.0F;
   }

   public float contentHeight() {
      float rowsHeight = 0.0F;

      for (Module module : this.modules()) {
         ModuleOption option = this.optionFor(module);
         rowsHeight += 18.0F + option.getAnimatedExtraHeight();
      }

      return rowsHeight;
   }

   public void renderShell(DrawContext graphics, float x, float y, float width, float height, float scale, float alpha) {
      this.rememberBounds(x, y, width, height, scale);
      width *= scale;
      height *= scale;
      Render2D.blur(x, y, width, height, 12.0F * scale, 1.0F, 1.0F, color(0, 0, 0, 255, alpha));
      float contentTop = y + 34.0F * scale;
      float bottom = y + height - 7.0F * scale;
      float rowY = contentTop - this.scrollAnimation.get() * scale;
      float rowHeight = 18.0F * scale;
      List<Module> modules = this.modules();
      Render2D.pushScissor(graphics, x, contentTop, width, Math.max(0.0F, bottom - contentTop));

      try {
         for (Module module : modules) {
            ModuleOption option = this.optionFor(module);
            float extraHeight = option.getAnimatedExtraHeight() * scale;
            float blockBottom = rowY + rowHeight + extraHeight;
            if (blockBottom > contentTop && rowY < bottom) {
               option.renderSettingsBackground(x, rowY + rowHeight, width, scale, textAlpha(alpha), bottom);
            }

            rowY = blockBottom;
         }
      } finally {
         Render2D.popScissor(graphics);
      }
   }

   public void renderContent(DrawContext graphics, float x, float y, float width, float height, float scale, float alpha) {
      this.rememberBounds(x, y, width, height, scale);
      width *= scale;
      height *= scale;
      List<Module> modules = this.modules();

      for (Module module : modules) {
         this.optionFor(module).beginInteractionFrame();
      }

      float textAlpha = textAlpha(alpha);
      if (!(textAlpha <= 0.0F)) {
         Render2D.text(
            FontType.SEMIBOLD,
            ClientLanguage.tr(this.category.getDisplayName()),
            x + 11.0F * scale,
            y + 10.0F * scale,
            9.0F,
            color(255, 255, 255, 255, textAlpha)
         );
         String categoryIcon = this.categoryIcon();
         if (!categoryIcon.isEmpty()) {
            float iconSize = 12.0F;
            float iconWidth = Render2D.textWidth(FontType.MAINMENUSCREEN, categoryIcon, iconSize);
            Render2D.text(
               FontType.MAINMENUSCREEN, categoryIcon, x + width - iconWidth - 11.0F * scale, y + 8.0F * scale, iconSize, color(255, 255, 255, 230, textAlpha)
            );
         }

         float contentTop = y + 34.0F * scale;
         float bottom = y + height - 7.0F * scale;
         Render2D.pushScissor(graphics, x, contentTop, width, Math.max(0.0F, bottom - contentTop));

         try {
            float rowY = contentTop - this.scrollAnimation.get() * scale;
            float rowHeight = 18.0F * scale;

            for (Module module : modules) {
               ModuleOption option = this.optionFor(module);
               float extraHeight = option.getAnimatedExtraHeight() * scale;
               if (rowY + rowHeight > contentTop && rowY < bottom) {
                  option.renderRow(x, rowY, width, 18.0F, scale, textAlpha);
               }

               if (rowY + rowHeight + extraHeight > contentTop && rowY + rowHeight < bottom) {
                  option.renderSettingsContent(graphics, x, rowY + rowHeight, width, scale, textAlpha, bottom);
               }

               rowY += rowHeight + extraHeight;
            }
         } finally {
            Render2D.popScissor(graphics);
         }
      }
   }

   public void renderOverlays(DrawContext graphics) {
      for (Module module : this.modules()) {
         this.optionFor(module).renderOverlay(graphics);
      }
   }

   public boolean reveal(Module target) {
      if (target != null && target.getCategory() == this.category) {
         this.optionFor(target).revealSettings();
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseClicked(Click event, boolean doubled) {
      return this.mouseClickedPopup(event, doubled) ? true : this.mouseClickedControls(event, doubled);
   }

   public boolean mouseClickedPopup(Click event, boolean doubled) {
      List<Module> modules = this.modules();

      for (int i = modules.size() - 1; i >= 0; i--) {
         if (this.optionFor(modules.get(i)).mouseClickedPopup(event, doubled)) {
            return true;
         }
      }

      return false;
   }

   public boolean mouseClickedControls(Click event, boolean doubled) {
      if (!this.containsContent(event.x(), event.y())) {
         return false;
      } else {
         List<Module> modules = this.modules();

         for (int i = modules.size() - 1; i >= 0; i--) {
            if (this.optionFor(modules.get(i)).mouseClickedSettings(event, doubled)) {
               return true;
            }
         }

         for (int ix = modules.size() - 1; ix >= 0; ix--) {
            if (this.optionFor(modules.get(ix)).mouseClickedRow(event, doubled)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
      if (!this.containsPanel(mouseX, mouseY)) {
         return false;
      } else {
         this.scrollTarget = clamp(this.scrollTarget - (float)scrollY * 24.0F, 0.0F, this.scrollLimit);
         this.scrollAnimation.run((double)this.scrollTarget, 0.22, Easings.CUBIC_OUT, true);
         return true;
      }
   }

   public boolean mouseReleased(Click event) {
      List<Module> modules = this.modules();

      for (int i = modules.size() - 1; i >= 0; i--) {
         if (this.optionFor(modules.get(i)).mouseReleased(event)) {
            return true;
         }
      }

      return false;
   }

   public boolean mouseDragged(Click event, double dragX, double dragY) {
      List<Module> modules = this.modules();

      for (int i = modules.size() - 1; i >= 0; i--) {
         if (this.optionFor(modules.get(i)).mouseDragged(event, dragX, dragY)) {
            return true;
         }
      }

      return false;
   }

   public boolean keyPressed(KeyInput event) {
      List<Module> modules = this.modules();

      for (int i = modules.size() - 1; i >= 0; i--) {
         if (this.optionFor(modules.get(i)).keyPressed(event)) {
            return true;
         }
      }

      return false;
   }

   public boolean charTyped(CharInput event) {
      List<Module> modules = this.modules();

      for (int i = modules.size() - 1; i >= 0; i--) {
         if (this.optionFor(modules.get(i)).charTyped(event)) {
            return true;
         }
      }

      return false;
   }

   public void warmupText() {
      Render2D.warmupText(FontType.SEMIBOLD, ClientLanguage.tr(this.category.getDisplayName()), 9.0F);
      String categoryIcon = this.categoryIcon();
      if (!categoryIcon.isEmpty()) {
         Render2D.warmupText(FontType.MAINMENUSCREEN, categoryIcon, 12.0F);
      }

      for (Module module : this.modules()) {
         Render2D.warmupText(FontType.SEMIBOLD, ClientLanguage.tr(module.getName()), 8.0F);

         for (Setting<?> setting : module.getSettings()) {
            Render2D.warmupText(FontType.SEMIBOLD, ClientLanguage.tr(setting.getName()), 7.2F);
            warmupSettingValue(setting);
         }
      }
   }

   private List<Module> modules() {
      ModuleManager moduleManager = Manager.getModules();
      return moduleManager == null ? List.of() : moduleManager.getByCategory(this.category);
   }

   private ModuleOption optionFor(Module module) {
      return this.options.computeIfAbsent(module, ModuleOptionFactory::create);
   }

   private void updateScroll() {
      this.scrollLimit = Math.max(0.0F, this.contentHeight() - this.contentViewportHeight());
      this.scrollTarget = clamp(this.scrollTarget, 0.0F, this.scrollLimit);
      this.scrollAnimation.run((double)this.scrollTarget, 0.22, Easings.CUBIC_OUT, true);
      this.scrollAnimation.update();
   }

   private float contentViewportHeight() {
      return Math.max(1.0F, 239.0F);
   }

   private void rememberBounds(float x, float y, float width, float height, float scale) {
      this.lastX = x;
      this.lastY = y;
      this.lastWidth = width * scale;
      this.lastHeight = height * scale;
      this.lastScale = scale;
   }

   private boolean containsPanel(double mouseX, double mouseY) {
      return mouseX >= (double)this.lastX
         && mouseX <= (double)(this.lastX + this.lastWidth)
         && mouseY >= (double)this.lastY
         && mouseY <= (double)(this.lastY + this.lastHeight);
   }

   private boolean containsContent(double mouseX, double mouseY) {
      float top = this.lastY + 34.0F * this.lastScale;
      float bottom = this.lastY + this.lastHeight - 7.0F * this.lastScale;
      return mouseX >= (double)this.lastX && mouseX <= (double)(this.lastX + this.lastWidth) && mouseY >= (double)top && mouseY <= (double)bottom;
   }

   private String categoryIcon() {
      return switch (this.category) {
         case COMBAT -> "r";
         case MOVEMENT -> "j";
         case VISUAL -> "a";
         case PLAYER -> "t";
         case MISC -> "d";
         default -> "";
      };
   }

   private static void warmupSettingValue(Setting<?> setting) {
      if (setting instanceof ModeSetting modeSetting) {
         Render2D.warmupText(FontType.SEMIBOLD, modeSetting.getValue(), 6.5F);

         for (String mode : modeSetting.getModes()) {
            Render2D.warmupText(FontType.SEMIBOLD, mode, 6.4F);
         }
      } else if (!(setting instanceof MultiModeSetting multiModeSetting)) {
         Object value = setting.getValue();
         if (value != null) {
            Render2D.warmupText(FontType.SEMIBOLD, String.valueOf(value), 6.5F);
         }
      } else {
         Render2D.warmupText(FontType.SEMIBOLD, multiModeSetting.selectedCount() + " selected", 6.5F);

         for (String mode : multiModeSetting.getModes()) {
            Render2D.warmupText(FontType.SEMIBOLD, mode, 6.4F);
         }
      }
   }

   private float outlineShinePhase() {
      int index = this.category.ordinal();
      long cycle = 2900L + (long)index * 240L;
      float basePhase = (float)(System.currentTimeMillis() % cycle) / (float)cycle;
      float stagger = (float)index * 0.173F % 1.0F;
      float phase = (basePhase + stagger) % 1.0F;
      return phase * 0.998F;
   }

   private static float textAlpha(float alpha) {
      return alpha <= 0.001F ? 0.0F : alpha * alpha * (3.0F - 2.0F * alpha);
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static int color(int red, int green, int blue, int alpha, float alphaMultiplier) {
      return ColorUtil.rgba(red, green, blue, (int)((float)alpha * alphaMultiplier));
   }
}
