package daamky.ua.screens.clickgui.impl.options;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.ui.Render2D;

public class BooleanOption extends ClickGuiOption {
   private final SmoothAnimation toggleAnimation = new SmoothAnimation();
   private final BooleanSetting setting;
   private boolean enabled;
   private float switchX;
   private float switchY;
   private float switchWidth;
   private float switchHeight;

   public BooleanOption(BooleanSetting setting) {
      super(setting.getName());
      this.setting = setting;
      this.enabled = setting.getValue();
      this.toggleAnimation.set(this.enabled ? 1.0 : 0.0);
   }

   @Override
   protected void renderControl(DrawContext graphics, float x, float y, float width, float height, float scale, float alpha) {
      this.enabled = this.setting.getValue();
      this.switchWidth = 22.0F * scale;
      this.switchHeight = 12.0F * scale;
      this.switchX = x + width - this.switchWidth - 6.0F * scale;
      this.switchY = y + 6.0F * scale;
      this.toggleAnimation.run(this.enabled ? 1.0 : 0.0, 0.18, Easings.CUBIC_OUT, true);
      this.toggleAnimation.update();
      float progress = this.toggleAnimation.get();
      float knobX = this.switchX + (2.0F + 10.0F * progress) * scale;
      this.renderBooleanGlass(this.switchX, this.switchY, this.switchWidth, this.switchHeight, 5.0F * scale, alpha, progress);
      Render2D.rect(knobX, this.switchY + 2.0F * scale, 8.0F * scale, 8.0F * scale, 3.0F * scale, this.color(255, 255, 255, 245, alpha));
      float iconSize = 15.0F;
      this.renderGuiIcon("T", x + 3.5F * scale, this.switchY - 4.0F * scale, iconSize, scale);
      this.renderOptionLabel(graphics, "boolean", x + 15.5F * scale, this.switchY + 1.0F * scale, 7.2F, this.switchX - 4.0F * scale, y, height);
   }

   @Override
   public boolean mouseClicked(Click event, boolean doubled) {
      if (event.button() != 0) {
         return false;
      } else if (this.hovered(
         event.x(),
         event.y(),
         this.switchX - 2.0F * this.scale,
         this.switchY - 2.0F * this.scale,
         this.switchWidth + 4.0F * this.scale,
         this.switchHeight + 4.0F * this.scale
      )) {
         this.enabled = !this.enabled;
         this.setting.setValue(Boolean.valueOf(this.enabled));
         this.toggleAnimation.run(this.enabled ? 1.0 : 0.0, 0.18, Easings.CUBIC_OUT);
         return true;
      } else {
         return false;
      }
   }

   private void renderBooleanGlass(float x, float y, float width, float height, float radius, float alpha, float progress) {
      Render2D.glass(
         x,
         y,
         width,
         height,
         radius,
         this.color(255, 255, 255, 72, alpha),
         0.64F * alpha,
         42.0F,
         this.color(255, 255, 255, 150, alpha),
         0.16F * alpha,
         true,
         0.22F,
         0.003F,
         1.0F,
         0.0F
      );
      Render2D.glassOutline(
         x,
         y,
         width,
         height,
         radius,
         0.9F,
         this.color(255, 255, 255, 176, alpha),
         1.0F,
         35.0F,
         this.color(168, 168, 174, 118, alpha),
         0.42F * alpha,
         false,
         0.45F,
         0.0015F,
         1.0F,
         0.0F
      );
   }
}
