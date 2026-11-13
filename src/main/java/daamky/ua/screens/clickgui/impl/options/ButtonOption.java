package daamky.ua.screens.clickgui.impl.options;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.settings.impl.ButtonSetting;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;

public class ButtonOption extends ClickGuiOption {
   private final String action;
   private final SmoothAnimation scaleAnimation = new SmoothAnimation();
   private final ButtonSetting setting;
   private float controlX;
   private float controlY;
   private float controlWidth;
   private float controlHeight;

   public ButtonOption(ButtonSetting setting, String action) {
      super(setting.getName());
      this.setting = setting;
      this.action = action;
      this.scaleAnimation.set(1.0);
   }

   @Override
   protected void renderControl(DrawContext graphics, float x, float y, float width, float height, float scale, float alpha) {
      this.controlWidth = 39.0F * scale;
      this.controlHeight = 13.0F * scale;
      this.controlX = x + width - this.controlWidth - 6.0F * scale;
      this.controlY = y + 5.5F * scale;
      this.scaleAnimation.update();
      float visualScale = this.scaleAnimation.get();
      float renderWidth = this.controlWidth * visualScale;
      float renderHeight = this.controlHeight * visualScale;
      float renderX = this.controlX + (this.controlWidth - renderWidth) / 2.0F;
      float renderY = this.controlY + (this.controlHeight - renderHeight) / 2.0F;
      float textSize = 6.2F * visualScale;
      String actionText = ClientLanguage.tr(this.action);
      float actionWidth = this.textWidth(actionText, textSize);
      float actionClipX = renderX + 4.0F * scale * visualScale;
      float actionClipWidth = renderWidth - 8.0F * scale * visualScale;
      float actionX = actionClipX + Math.max(0.0F, (actionClipWidth - actionWidth) / 2.0F);
      this.renderGlassControl(renderX, renderY, renderWidth, renderHeight, 4.0F * scale * visualScale, alpha);
      this.renderScrollingText(
         graphics,
         "button:value",
         actionText,
         actionX,
         renderY + 2.5F * scale * visualScale,
         textSize,
         this.color(255, 255, 255, 245, alpha),
         actionClipX,
         renderY,
         actionClipWidth,
         renderHeight,
         this.controlX,
         this.controlY,
         this.controlWidth,
         this.controlHeight
      );
      float iconSize = 14.5F;
      this.renderGuiIcon("U", x + 3.5F * scale, this.controlY - 3.0F * scale, iconSize, scale);
      this.renderOptionLabel(graphics, "button", x + 15.5F * scale, this.controlY + 1.5F * scale, 7.2F, this.controlX - 4.0F * scale, y, height);
   }

   @Override
   public boolean mouseClicked(Click event, boolean doubled) {
      if (event.button() != 0) {
         return false;
      } else if (this.hovered(event.x(), event.y(), this.controlX, this.controlY, this.controlWidth, this.controlHeight)) {
         this.scaleAnimation.set(0.88);
         this.scaleAnimation.run(1.0, 0.22, Easings.CUBIC_OUT);
         this.setting.press();
         return true;
      } else {
         return false;
      }
   }
}
