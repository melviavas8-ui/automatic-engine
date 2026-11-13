package daamky.ua.screens.clickgui.impl.options;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;

public class BindOption extends ClickGuiOption {
   private final BindSetting setting;
   private String key;
   private boolean listening;
   private float boxX;
   private float boxY;
   private float boxWidth;
   private float boxHeight;

   public BindOption(BindSetting setting) {
      super(setting.getName());
      this.setting = setting;
      this.key = setting.getValue().getDisplayName();
   }

   @Override
   protected void renderControl(DrawContext graphics, float x, float y, float width, float height, float scale, float alpha) {
      if (!this.listening) {
         this.key = this.setting.getValue().getDisplayName();
      }

      String text = this.listening ? "..." : this.key;
      float textSize = 6.0F;
      float textWidth = this.textWidth(text, textSize);
      this.boxWidth = Math.max(15.0F * scale, Math.min(39.0F * scale, textWidth + 10.0F * scale));
      this.boxHeight = 13.0F * scale;
      this.boxX = x + width - this.boxWidth - 6.0F * scale;
      this.boxY = y + 5.5F * scale;
      this.renderGlassControl(this.boxX + 1.0F, this.boxY + 1.0F, this.boxWidth, this.boxHeight, 4.0F * scale, alpha);
      float keyClipX = this.boxX + 4.0F * scale;
      float keyClipWidth = this.boxWidth - 8.0F * scale;
      float keyTextX = keyClipX + Math.max(0.0F, (keyClipWidth - textWidth) / 2.0F);
      this.renderScrollingText(
         graphics,
         "bind:value",
         text,
         keyTextX,
         this.boxY + 3.5F * scale,
         textSize,
         this.color(255, 255, 255, 235, alpha),
         keyClipX,
         this.boxY,
         keyClipWidth,
         this.boxHeight,
         this.boxX,
         this.boxY,
         this.boxWidth,
         this.boxHeight
      );
      float iconSize = 8.0F;
      this.renderGuiIcon("L", x + 3.5F * scale, this.boxY + 3.5F * scale, iconSize, scale);
      this.renderOptionLabel(graphics, "bind", x + 15.5F * scale, this.boxY + 2.0F * scale, 7.2F, this.boxX - 4.0F * scale, y, height);
   }

   @Override
   public boolean mouseClicked(Click event, boolean doubled) {
      if (this.listening) {
         KeyBind bind = KeyBind.mouse(event.button());
         this.key = bind.getDisplayName();
         this.setting.setValue(bind);
         this.listening = false;
         return true;
      } else if (event.button() == 0 && this.hovered(event.x(), event.y(), this.boxX, this.boxY, this.boxWidth, this.boxHeight)) {
         this.listening = true;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyPressed(KeyInput event) {
      if (!this.listening) {
         return false;
      } else {
         if (event.key() != 256 && event.key() != 259) {
            this.setting.setValue(KeyBind.keyboard(event.key()));
         } else {
            this.setting.setValue(KeyBind.NONE);
         }

         this.key = this.setting.getValue().getDisplayName();
         this.listening = false;
         return true;
      }
   }
}
