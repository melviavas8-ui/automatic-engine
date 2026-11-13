package daamky.ua.screens.clickgui.impl.module;

import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.module.Module;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.screens.clickgui.impl.options.ClickGuiOption;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public class ModuleOption {
   private static final float OPTION_GAP = 1.0F;
   private static final float AFTER_OPTIONS_GAP = -2.0F;
   private final Module module;
   private final List<ClickGuiOption> settings;
   private final SmoothAnimation settingsAnimation = new SmoothAnimation();
   private final SmoothAnimation bindAnimation = new SmoothAnimation();
   private final SmoothAnimation enabledAnimation = new SmoothAnimation();
   private final BindSetting bindSetting;
   private boolean settingsExpanded;
   private boolean binding;
   private float settingsProgress;
   private float bindProgress;
   private float rowX;
   private float rowY;
   private float rowWidth;
   private float rowHeight;
   private float rowClickX;
   private float rowClickY;
   private float rowClickWidth;
   private float rowClickHeight;
   private boolean rowInteractive;

   public ModuleOption(Module module, List<ClickGuiOption> settings) {
      this.module = module;
      this.settings = settings == null ? List.of() : settings;
      this.bindSetting = module.getBindSetting();
      this.settingsAnimation.set(0.0);
      this.bindAnimation.set(0.0);
      this.enabledAnimation.set(module.isEnabled() ? 1.0 : 0.0);
   }

   public void updateAnimation() {
      this.settingsAnimation.run(this.settingsExpanded ? 1.0 : 0.0, 0.24, Easings.CUBIC_OUT, true);
      this.settingsAnimation.update();
      this.settingsProgress = this.settingsAnimation.get();
      this.bindAnimation.run(this.binding ? 1.0 : 0.0, 0.18, Easings.CUBIC_OUT, true);
      this.bindAnimation.update();
      this.bindProgress = this.bindAnimation.get();
      this.enabledAnimation.run(this.module.isEnabled() ? 1.0 : 0.0, 0.22, Easings.CUBIC_OUT, true);
      this.enabledAnimation.update();

      for (ClickGuiOption option : this.settings) {
         option.updateVisibility();
      }
   }

   public void renderRow(float panelX, float rowY, float panelWidth, float rowHeight, float scale, float alpha) {
      this.rowX = panelX;
      this.rowY = rowY;
      this.rowWidth = panelWidth;
      this.rowHeight = rowHeight * scale;
      this.rowInteractive = alpha > 0.01F && panelWidth > 0.0F && this.rowHeight > 0.0F;
      float enabledProgress = this.enabledAnimation.get();
      int moduleColor = Math.round(185.0F + 70.0F * enabledProgress);
      int textColor = this.color(moduleColor, moduleColor, moduleColor, (int)(255.0F * (1.0F - this.bindProgress)), alpha);
      float moduleTextX = panelX + 11.0F * scale;
      float moduleTextY = rowY + 5.3F * scale;
      String moduleName = ClientLanguage.tr(this.module.getName());
      float moduleTextWidth = Render2D.textWidth(FontType.SEMIBOLD, moduleName, 8.0F);
      float clickPaddingY = 3.0F * scale;
      this.rowClickX = moduleTextX;
      this.rowClickY = moduleTextY - clickPaddingY;
      this.rowClickWidth = moduleTextWidth;
      this.rowClickHeight = 8.0F + clickPaddingY * 2.0F;
      Render2D.rect(
         panelX + 7.5F * scale,
         rowY + 6.5F * scale,
         1.0F * scale,
         8.0F * scale,
         0.0F,
         this.color(moduleColor, moduleColor, moduleColor, (int)(50.0F * (1.0F - this.bindProgress)), alpha)
      );
      Render2D.text(FontType.SEMIBOLD, moduleName, moduleTextX, moduleTextY, 8.0F, textColor);
      if (this.hasSettings()) {
         String dots = "...";
         float dotsSize = 9.0F;
         float dotsWidth = Render2D.textWidth(FontType.SEMIBOLD, dots, dotsSize);
         Render2D.text(
            FontType.SEMIBOLD, dots, panelX + panelWidth - dotsWidth - 14.0F * scale, rowY + 2.5F * scale, dotsSize, this.color(255, 255, 255, 210, alpha)
         );
      }

      if (this.bindProgress > 0.01F) {
         String bindText = this.bindText();
         float bindSize = 8.0F;
         Render2D.text(FontType.SEMIBOLD, bindText, moduleTextX, moduleTextY, bindSize, this.color(255, 255, 255, (int)(255.0F * this.bindProgress), alpha));
      }
   }

   public float renderSettings(DrawContext graphics, float panelX, float settingsTop, float width, float scale, float alpha, float clipBottom) {
      float animatedHeight = this.renderSettingsBackground(panelX, settingsTop, width, scale, alpha, clipBottom);
      this.renderSettingsContent(graphics, panelX, settingsTop, width, scale, alpha, clipBottom);
      return animatedHeight;
   }

   public float renderSettingsBackground(float panelX, float settingsTop, float width, float scale, float alpha, float clipBottom) {
      if (this.hasSettings() && !(this.settingsProgress <= 0.01F)) {
         float settingsX = panelX + 5.0F * scale;
         float settingsY = settingsTop + 2.0F * scale;
         float settingsWidth = width - 10.0F * scale;
         float settingsHeight = this.computeSettingsHeight();
         if (settingsHeight <= 0.01F) {
            return 0.0F;
         } else {
            float settingsFullHeight = settingsHeight * scale;
            float settingsVisibleHeight = settingsFullHeight * this.settingsProgress;
            float settingsAlpha = alpha * this.settingsProgress;
            float clippedHeight = Math.max(0.0F, Math.min(settingsVisibleHeight, clipBottom - settingsY));
            float animatedHeight = (settingsHeight + -2.0F + 2.0F) * scale * this.settingsProgress;
            if (clippedHeight <= 0.0F) {
               return animatedHeight;
            } else {
               Render2D.rect(settingsX, settingsY, settingsWidth, clippedHeight, 4.0F, this.color(12, 12, 21, (int)(25.0F * (1.0F - this.bindProgress)), alpha));
               return animatedHeight;
            }
         }
      } else {
         return 0.0F;
      }
   }

   public void renderSettingsContent(DrawContext graphics, float panelX, float settingsTop, float width, float scale, float alpha, float clipBottom) {
      if (this.hasSettings() && !(this.settingsProgress <= 0.01F)) {
         float settingsX = panelX + 5.0F * scale;
         float settingsY = settingsTop + 2.0F * scale;
         float settingsWidth = width - 10.0F * scale;
         float settingsHeight = this.computeSettingsHeight();
         if (!(settingsHeight <= 0.01F)) {
            float settingsFullHeight = settingsHeight * scale;
            float settingsVisibleHeight = settingsFullHeight * this.settingsProgress;
            float clippedHeight = Math.max(0.0F, Math.min(settingsVisibleHeight, clipBottom - settingsY));
            if (!(clippedHeight <= 0.0F)) {
               Render2D.pushScissor(graphics, settingsX, settingsY, settingsWidth, clippedHeight);

               try {
                  float optionY = settingsY;

                  for (ClickGuiOption option : this.settings) {
                     float visibility = option.visibilityProgress();
                     float slotHeight = (option.getHeight() + 1.0F) * scale;
                     if (visibility > 0.01F) {
                        option.setFadeMultiplier(this.settingsProgress * visibility);
                        float renderY = optionY - (1.0F - visibility) * 5.0F * scale;
                        option.render(graphics, panelX + 6.0F * scale, renderY, width / scale - 12.0F, scale, alpha);
                     }

                     optionY += slotHeight * visibility;
                  }
               } finally {
                  Render2D.popScissor(graphics);
               }
            }
         }
      }
   }

   public void renderOverlay(DrawContext graphics) {
      if (this.hasSettings() && !(this.settingsProgress <= 0.01F)) {
         for (ClickGuiOption option : this.settings) {
            float visibility = option.visibilityProgress();
            if (!(visibility <= 0.01F)) {
               option.setFadeMultiplier(this.settingsProgress * visibility);
               option.renderOverlay(graphics);
            }
         }
      }
   }

   public void beginInteractionFrame() {
      this.rowInteractive = false;
      this.rowClickX = 0.0F;
      this.rowClickY = 0.0F;
      this.rowClickWidth = 0.0F;
      this.rowClickHeight = 0.0F;

      for (ClickGuiOption option : this.settings) {
         option.resetInteractionFrame();
      }
   }

   public boolean mouseClicked(Click event, boolean doubled) {
      return this.mouseClickedSettings(event, doubled) ? true : this.mouseClickedRow(event, doubled);
   }

   public boolean mouseClickedRow(Click event, boolean doubled) {
      if (event.button() == 2 && this.isRowHovered(event.x(), event.y())) {
         this.startBinding();
         return true;
      } else if (this.binding) {
         if (event.button() != 2) {
            this.setBind(KeyBind.mouse(event.button()));
         }

         return true;
      } else if (event.button() == 0 && this.isRowHovered(event.x(), event.y())) {
         this.module.toggle();
         return true;
      } else if (event.button() == 1 && this.isRowHovered(event.x(), event.y()) && this.hasSettings()) {
         this.settingsExpanded = !this.settingsExpanded;
         this.settingsAnimation.run(this.settingsExpanded ? 1.0 : 0.0, 0.24, Easings.CUBIC_OUT);
         this.closePopupsExcept(null);
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseClickedSettings(Click event, boolean doubled) {
      if (this.hasSettings() && !(this.settingsProgress <= 0.85F)) {
         ClickGuiOption handled = null;

         for (ClickGuiOption option : this.settings) {
            if (option.isVisibleForInteraction() && option.wantsMousePriority(event) && option.mouseClicked(event, doubled)) {
               handled = option;
               break;
            }
         }

         if (handled == null) {
            for (int i = this.settings.size() - 1; i >= 0; i--) {
               ClickGuiOption optionx = this.settings.get(i);
               if (optionx.isVisibleForInteraction() && optionx.mouseClicked(event, doubled)) {
                  handled = optionx;
                  break;
               }
            }
         }

         if (handled != null) {
            this.closePopupsExcept(handled);
            return true;
         } else {
            this.closePopupsExcept(null);
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean mouseClickedPopup(Click event, boolean doubled) {
      if (this.hasSettings() && !(this.settingsProgress <= 0.01F)) {
         boolean hasOpenPopup = false;
         ClickGuiOption handled = null;

         for (int i = this.settings.size() - 1; i >= 0; i--) {
            ClickGuiOption option = this.settings.get(i);
            if (option.isVisibleForRender() && option.hasOpenPopup()) {
               hasOpenPopup = true;
               if (option.wantsMousePriority(event) && option.mouseClicked(event, doubled)) {
                  handled = option;
                  break;
               }
            }
         }

         if (handled != null) {
            this.closePopupsExcept(handled);
            return true;
         } else if (hasOpenPopup) {
            this.closePopupsExcept(null);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean mouseReleased(Click event) {
      if (this.hasSettings() && !(this.settingsProgress <= 0.01F)) {
         for (int i = this.settings.size() - 1; i >= 0; i--) {
            ClickGuiOption option = this.settings.get(i);
            if (option.isVisibleForInteraction() && option.mouseReleased(event)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean mouseDragged(Click event, double dragX, double dragY) {
      if (this.hasSettings() && !(this.settingsProgress <= 0.01F)) {
         for (int i = this.settings.size() - 1; i >= 0; i--) {
            ClickGuiOption option = this.settings.get(i);
            if (option.isVisibleForInteraction() && option.mouseDragged(event, dragX, dragY)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean keyPressed(KeyInput event) {
      if (!this.binding) {
         if (this.hasSettings() && !(this.settingsProgress <= 0.01F)) {
            for (int i = this.settings.size() - 1; i >= 0; i--) {
               ClickGuiOption option = this.settings.get(i);
               if (option.isVisibleForInteraction() && option.keyPressed(event)) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         if (event.key() != 261 && event.key() != 259 && event.key() != 256) {
            this.setBind(KeyBind.keyboard(event.key()));
         } else {
            this.setBind(KeyBind.NONE);
         }

         return true;
      }
   }

   public boolean charTyped(CharInput event) {
      if (this.hasSettings() && !(this.settingsProgress <= 0.01F)) {
         for (int i = this.settings.size() - 1; i >= 0; i--) {
            ClickGuiOption option = this.settings.get(i);
            if (option.isVisibleForInteraction() && option.charTyped(event)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public float getAnimatedExtraHeight() {
      if (!this.hasSettings()) {
         return 0.0F;
      } else {
         float settingsHeight = this.computeSettingsHeight();
         return settingsHeight <= 0.01F ? 0.0F : (settingsHeight + -2.0F + 2.0F) * this.settingsProgress;
      }
   }

   public boolean hasSettings() {
      return !this.settings.isEmpty();
   }

   public boolean isSettingsExpanded() {
      return this.settingsExpanded;
   }

   public boolean isSettingsAnimationAlive() {
      return this.settingsAnimation.isAlive();
   }

   public void revealSettings() {
      if (this.hasSettings()) {
         this.settingsExpanded = true;
         this.settingsAnimation.run(1.0, 0.18, Easings.CUBIC_OUT);
      }
   }

   private float computeSettingsHeight() {
      float height = 0.0F;

      for (ClickGuiOption option : this.settings) {
         float visibility = option.visibilityProgress();
         if (!(visibility <= 0.01F)) {
            height += (option.getHeight() + 1.0F) * visibility;
         }
      }

      return height <= 0.01F ? 0.0F : height + 1.0F;
   }

   private boolean isRowHovered(double mouseX, double mouseY) {
      return this.rowInteractive
         && mouseX >= (double)this.rowClickX
         && mouseX <= (double)(this.rowClickX + this.rowClickWidth)
         && mouseY >= (double)this.rowClickY
         && mouseY <= (double)(this.rowClickY + this.rowClickHeight);
   }

   private void startBinding() {
      this.binding = true;
      this.bindAnimation.run(1.0, 0.18, Easings.CUBIC_OUT);
      this.closePopupsExcept(null);
   }

   private void setBind(KeyBind bind) {
      this.bindSetting.setValue(bind);
      this.binding = false;
      this.bindAnimation.run(0.0, 0.18, Easings.CUBIC_OUT);
   }

   private String bindText() {
      return !this.bindSetting.getValue().isBound() ? "..." : this.bindSetting.getValue().getDisplayName() + "...";
   }

   private void closePopupsExcept(ClickGuiOption keepOpen) {
      for (ClickGuiOption option : this.settings) {
         if (option != keepOpen) {
            option.closePopup();
         }
      }
   }

   private int color(int red, int green, int blue, int alpha, float alphaMultiplier) {
      return ColorUtil.rgba(red, green, blue, (int)((float)alpha * alphaMultiplier));
   }
}
