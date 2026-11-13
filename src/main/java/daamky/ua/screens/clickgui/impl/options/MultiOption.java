package daamky.ua.screens.clickgui.impl.options;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.ui.Render2D;

public class MultiOption extends ClickGuiOption {
   private final String[] values;
   private final SmoothAnimation openAnimation = new SmoothAnimation();
   private final MultiModeSetting setting;
   private boolean open;
   private float controlX;
   private float controlY;
   private float controlWidth;
   private float controlHeight;

   public MultiOption(MultiModeSetting setting) {
      super(setting.getName());
      this.setting = setting;
      this.values = setting.getModes().toArray(String[]::new);
      this.openAnimation.set(0.0);
   }

   @Override
   protected void renderControl(DrawContext graphics, float x, float y, float width, float height, float scale, float alpha) {
      this.controlWidth = 58.0F * scale;
      this.controlHeight = 13.0F * scale;
      this.controlX = x + width - this.controlWidth - 5.0F * scale;
      this.controlY = y + 5.5F * scale;
      this.renderGlassControl(this.controlX, this.controlY, this.controlWidth, this.controlHeight, 3.5F * scale, alpha);
      this.renderScrollingText(
         graphics,
         "multi:value",
         this.selectedLabel(),
         this.controlX + 4.0F * scale,
         this.controlY + 2.5F * scale,
         6.5F,
         this.color(255, 255, 255, 245, alpha),
         this.controlX + 4.0F * scale,
         this.controlY,
         this.controlWidth - 8.0F * scale,
         this.controlHeight,
         this.controlX,
         this.controlY,
         this.controlWidth,
         this.controlHeight
      );
      float iconSize = 9.0F;
      this.renderGuiIcon("I", x + 3.0F * scale, this.controlY + 2.5F * scale, iconSize, scale);
      this.renderOptionLabel(graphics, "multi", x + 15.5F * scale, this.controlY + 2.0F * scale, 7.2F, this.controlX - 4.0F * scale, y, height);
   }

   @Override
   public void renderOverlay(DrawContext graphics) {
      this.openAnimation.update();
      float progress = this.openAnimation.get();
      if (!(progress <= 0.01F)) {
         float itemHeight = 15.0F * this.scale;
         float popupY = this.controlY + this.controlHeight + 2.0F * this.scale;
         float visibleHeight = (float)this.values.length * itemHeight;
         float popupAlpha = this.alpha * progress;
         float radius = 5.0F * this.scale;
         Render2D.blur(this.controlX, popupY, this.controlWidth, visibleHeight, radius, 1.0F, 1.0F, this.color(255, 255, 255, 255, popupAlpha));
         Render2D.glass(
            this.controlX,
            popupY,
            this.controlWidth,
            visibleHeight,
            radius,
            this.color(255, 255, 255, 68, popupAlpha),
            0.58F * popupAlpha,
            42.0F,
            this.color(255, 255, 255, 142, popupAlpha),
            0.12F * popupAlpha,
            true,
            0.22F,
            0.003F,
            1.0F,
            0.0F
         );
         Render2D.rect(
            this.controlX,
            popupY,
            this.controlWidth,
            visibleHeight,
            radius,
            this.color(45, 45, 45, 72, popupAlpha),
            this.color(18, 18, 18, 28, popupAlpha),
            this.color(45, 45, 45, 72, popupAlpha),
            this.color(18, 18, 18, 28, popupAlpha)
         );
         Render2D.glassOutline(
            this.controlX,
            popupY,
            this.controlWidth,
            visibleHeight,
            radius,
            0.9F * this.scale,
            this.color(255, 255, 255, 160, popupAlpha),
            1.0F,
            35.0F,
            this.color(168, 168, 174, 118, popupAlpha),
            0.42F * popupAlpha,
            false,
            0.45F,
            0.0015F,
            1.0F,
            0.0F
         );
         Render2D.pushScissor(graphics, this.controlX, popupY, this.controlWidth, visibleHeight);

         for (int i = 0; i < this.values.length; i++) {
            float itemY = popupY + (float)i * itemHeight;
            boolean selected = this.setting.isSelected(this.values[i]);
            this.renderScrollingText(
               graphics,
               "multi:popup:" + i,
               ClientLanguage.tr(this.values[i]),
               this.controlX + 5.0F * this.scale,
               itemY + 4.0F * this.scale,
               6.4F,
               selected ? this.color(255, 255, 255, 255, popupAlpha) : this.color(205, 205, 210, 205, popupAlpha),
               this.controlX + 5.0F * this.scale,
               itemY,
               this.controlWidth - 10.0F * this.scale,
               itemHeight,
               this.controlX,
               itemY,
               this.controlWidth,
               itemHeight
            );
         }

         Render2D.popScissor(graphics);
      }
   }

   @Override
   public boolean mouseClicked(Click event, boolean doubled) {
      if (event.button() != 0) {
         return false;
      } else {
         double mouseX = event.x();
         double mouseY = event.y();
         if (this.isPopupVisible()) {
            float itemHeight = 15.0F * this.scale;
            float popupY = this.controlY + this.controlHeight + 2.0F * this.scale;
            if (this.hovered(mouseX, mouseY, this.controlX, popupY, this.controlWidth, (float)this.values.length * itemHeight)) {
               int index = Math.max(0, Math.min(this.values.length - 1, (int)((mouseY - (double)popupY) / (double)itemHeight)));
               this.setting.setSelected(this.values[index], !this.setting.isSelected(this.values[index]));
               return true;
            }
         }

         if (this.hovered(mouseX, mouseY, this.controlX, this.controlY, this.controlWidth, this.controlHeight)) {
            this.setOpen(!this.open);
            return true;
         } else {
            if (this.open) {
               this.setOpen(false);
            }

            return false;
         }
      }
   }

   @Override
   public boolean wantsMousePriority(Click event) {
      if (!this.isPopupVisible()) {
         return false;
      } else {
         float itemHeight = 15.0F * this.scale;
         float popupY = this.controlY + this.controlHeight + 2.0F * this.scale;
         return this.hovered(event.x(), event.y(), this.controlX, popupY, this.controlWidth, (float)this.values.length * itemHeight);
      }
   }

   @Override
   public boolean hasOpenPopup() {
      return this.isPopupVisible();
   }

   @Override
   public void closePopup() {
      if (this.open) {
         this.setOpen(false);
      }
   }

   private String selectedLabel() {
      int count = this.setting.selectedCount();
      return count == 1 ? "1 " + ClientLanguage.tr("selected") : count + " " + ClientLanguage.tr("selected");
   }

   private boolean isPopupVisible() {
      return this.open || this.openAnimation.get() > 0.01F || this.openAnimation.isAlive();
   }

   private void setOpen(boolean open) {
      if (this.open != open) {
         this.open = open;
         this.openAnimation.run(open ? 1.0 : 0.0, 0.16, Easings.CUBIC_OUT);
      }
   }
}
