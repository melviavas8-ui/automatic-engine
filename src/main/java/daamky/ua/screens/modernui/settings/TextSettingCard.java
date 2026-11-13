package daamky.ua.screens.modernui.settings;

import java.awt.Color;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

final class TextSettingCard extends SettingCardComponent<StringSetting> {
   TextSettingCard(StringSetting setting) {
      super(setting);
   }

   @Override
   int order() {
      return 6;
   }

   @Override
   float height(SettingCardContext context) {
      return 16.5F;
   }

   @Override
   void render(DrawContext graphics, SettingCardContext context, float cardX, float y, int mouseX, int mouseY) {
      this.markRow(cardX, y, this.height(context));
      float boxWidth = 62.0F;
      float contentRight = cardX + context.cardWidth - 10.0F;
      float boxX = contentRight - boxWidth;
      float boxY = y + 1.0F;
      float boxHeight = 9.5F;
      this.state.box(boxX, boxY, boxWidth, boxHeight);
      if (context.focusedText != this.setting) {
         this.state.text = this.setting.getValue();
         this.state.cursor = Math.min(this.state.cursor, this.state.text.length());
      }

      this.renderNameAndDescription(graphics, context, cardX + 10.0F, y, 54.0F, 62.0F);
      Render2D.rect(
         boxX,
         boxY,
         boxWidth,
         boxHeight,
         2.0F,
         new Color(42, 43, 52, 230).getRGB(),
         new Color(48, 49, 60, 220).getRGB(),
         new Color(33, 34, 42, 236).getRGB(),
         new Color(42, 39, 51, 245).getRGB()
      );
      Render2D.rect(
         boxX,
         boxY,
         boxWidth,
         boxHeight,
         2.0F,
         context.withAlpha(context.theme().outline(), 0.32F),
         context.withAlpha(context.theme().accentBright(), 0.28F),
         context.withAlpha(context.theme().accentDark(), 0.38F),
         context.withAlpha(context.theme().accent(), 0.34F)
      );
      Render2D.text(FontType.MAINMENUSCREEN, "D", boxX - 10.0F, boxY + 1.8F, 8.0F, context.color(154, 156, 166, 255));
      Render2D.pushScissor(graphics, boxX + 4.0F, boxY + 1.0F, boxWidth - 8.0F, boxHeight);
      String text = this.state.text != null && !this.state.text.isEmpty() ? this.state.text : "Text...";
      if (this.state.text != null && !this.state.text.isEmpty()) {
         short var16 = 225;
      } else {
         short var10000 = 165;
      }

      float textY = boxY + (boxHeight - 5.0F) * 0.5F;
      Render2D.text(FontType.SEMIBOLD, text, boxX + 4.0F, textY, 5.2F, new Color(255, 255, 255, 255).getRGB());
      if (context.focusedText == this.setting && !this.state.selectedAll && System.currentTimeMillis() / 420L % 2L == 0L) {
         float cursorX = boxX
            + 2.0F
            + Render2D.textWidth(FontType.SEMIBOLD, this.state.text.substring(0, Math.min(this.state.cursor, this.state.text.length())), 5.2F)
            + 2.0F;
         Render2D.rect(cursorX, boxY + 1.25F, 0.65F, 7.0F, 0.0F, context.color(230, 230, 230, 220));
      }

      Render2D.popScissor(graphics);
   }

   @Override
   boolean mouseClicked(Click event, boolean doubled, SettingCardContext context) {
      if (event.button() == 0 && context.inside(event.x(), event.y(), this.state.boxX, this.state.boxY, this.state.boxWidth, this.state.boxHeight)) {
         context.focusedText = this.setting;
         this.state.text = this.setting.getValue();
         this.state.cursor = this.state.text.length();
         this.state.selectedAll = doubled;
         context.activeColor = null;
         return true;
      } else {
         return false;
      }
   }

   @Override
   boolean keyPressed(KeyInput event, SettingCardContext context) {
      if (context.focusedText != this.setting) {
         return false;
      } else {
         if (this.state.text == null) {
            this.state.text = this.setting.getValue();
         }

         boolean control = (event.modifiers() & 2) != 0;
         if (control && event.key() == 65) {
            this.state.selectedAll = true;
            this.state.cursor = this.state.text.length();
            return true;
         } else if (control && event.key() == 67) {
            context.setClipboard(this.state.text);
            return true;
         } else if (control && event.key() == 86) {
            this.insertText(context.getClipboard());
            return true;
         } else if (event.key() == 259) {
            if (this.state.selectedAll) {
               this.state.text = "";
               this.state.cursor = 0;
               this.state.selectedAll = false;
            } else if (this.state.cursor > 0) {
               this.state.text = this.state.text.substring(0, this.state.cursor - 1) + this.state.text.substring(this.state.cursor);
               this.state.cursor--;
            }

            this.setting.setValue(this.state.text);
            return true;
         } else if (event.key() == 261) {
            if (this.state.selectedAll) {
               this.state.text = "";
               this.state.cursor = 0;
               this.state.selectedAll = false;
            } else if (this.state.cursor < this.state.text.length()) {
               this.state.text = this.state.text.substring(0, this.state.cursor) + this.state.text.substring(this.state.cursor + 1);
            }

            this.setting.setValue(this.state.text);
            return true;
         } else if (event.key() == 263) {
            this.state.cursor = Math.max(0, this.state.cursor - 1);
            this.state.selectedAll = false;
            return true;
         } else if (event.key() == 262) {
            this.state.cursor = Math.min(this.state.text.length(), this.state.cursor + 1);
            this.state.selectedAll = false;
            return true;
         } else if (event.key() != 257 && event.key() != 335 && event.key() != 256) {
            return true;
         } else {
            context.focusedText = null;
            this.state.selectedAll = false;
            return true;
         }
      }
   }

   @Override
   boolean charTyped(CharInput event, SettingCardContext context) {
      if (context.focusedText == this.setting && event.isValidChar()) {
         this.insertText(event.asString());
         return true;
      } else {
         return false;
      }
   }

   private void insertText(String text) {
      if (text != null && !text.isEmpty()) {
         if (this.state.text == null) {
            this.state.text = this.setting.getValue();
         }

         String cleaned = text.replace("\r", "").replace("\n", " ");
         if (this.state.selectedAll) {
            this.state.text = cleaned;
            this.state.selectedAll = false;
         } else {
            this.state.text = this.state.text.substring(0, this.state.cursor) + cleaned + this.state.text.substring(this.state.cursor);
         }

         this.state.cursor = Math.min(this.state.text.length(), this.state.cursor + cleaned.length());
         this.setting.setValue(this.state.text);
         this.state.text = this.setting.getValue();
         this.state.cursor = Math.min(this.state.cursor, this.state.text.length());
      }
   }
}
