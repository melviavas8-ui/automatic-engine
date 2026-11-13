package daamky.ua.screens.clickgui.impl.options;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public class TextOption extends ClickGuiOption {
   private final SmoothAnimation scrollAnimation = new SmoothAnimation();
   private final SmoothAnimation outlineAnimation = new SmoothAnimation();
   private final StringSetting setting;
   private String value;
   private boolean editing;
   private boolean selectedAll;
   private int cursor;
   private String cachedCursorValue;
   private int cachedCursor = -1;
   private float cachedCursorWidth;
   private float controlX;
   private float controlY;
   private float controlWidth;
   private float controlHeight;

   public TextOption(StringSetting setting) {
      super(setting.getName());
      this.setting = setting;
      this.value = setting.getValue();
      this.cursor = this.value.length();
      this.scrollAnimation.set(0.0);
      this.outlineAnimation.set(0.0);
   }

   @Override
   protected void renderControl(DrawContext graphics, float x, float y, float width, float height, float scale, float alpha) {
      if (!this.editing) {
         this.value = this.setting.getValue();
         this.cursor = Math.min(this.cursor, this.value.length());
      }

      this.controlWidth = 50.0F * scale;
      this.controlHeight = 13.0F * scale;
      this.controlX = x + width - this.controlWidth - 5.0F * scale;
      this.controlY = y + 5.5F * scale;
      float textSize = 6.2F;
      float textX = this.controlX + 5.0F * scale;
      float textY = this.controlY + 3.0F * scale;
      float available = this.controlWidth - 10.0F * scale;
      float valueWidth = this.textWidth(this.value, textSize);
      float targetScroll = valueWidth > available ? available - valueWidth - 1.0F * scale : 0.0F;
      this.scrollAnimation.run((double)targetScroll, 0.2, Easings.CUBIC_OUT, true);
      this.scrollAnimation.update();
      this.outlineAnimation.run(this.editing ? 1.0 : 0.0, 0.18, Easings.CUBIC_OUT, true);
      this.outlineAnimation.update();
      this.renderGlassControl(this.controlX, this.controlY, this.controlWidth, this.controlHeight, 4.0F * scale, alpha);
      if (this.outlineAnimation.get() > 0.01F) {
         Render2D.outline(
            this.controlX,
            this.controlY,
            this.controlWidth,
            this.controlHeight,
            4.0F * scale,
            1.0F * scale,
            this.color(155, 162, 176, Math.round(165.0F * this.outlineAnimation.get()), alpha)
         );
      }

      if (this.editing) {
         Render2D.pushScissor(graphics, this.controlX + 4.0F * scale, this.controlY, available + 2.0F * scale, this.controlHeight);
         if (this.selectedAll && !this.value.isEmpty()) {
            Render2D.rect(
               textX + this.scrollAnimation.get() - 1.0F * scale,
               this.controlY + 3.0F * scale,
               valueWidth + 5.0F * scale,
               8.0F * scale,
               1.0F * scale,
               this.color(155, 162, 176, 85, alpha)
            );
         }

         Render2D.text(FontType.SEMIBOLD, this.value, textX + this.scrollAnimation.get(), textY, textSize, this.color(255, 255, 255, 235, alpha));
         if (!this.selectedAll && System.currentTimeMillis() / 420L % 2L == 0L) {
            float cursorX = textX + this.scrollAnimation.get() + this.cursorWidth(textSize) + 3.0F * scale;
            Render2D.rect(cursorX, this.controlY + 3.5F * scale, 1.0F * scale, 7.0F * scale, 0.0F * scale, this.color(24, 24, 27, 235, alpha));
         }

         Render2D.popScissor(graphics);
      } else {
         this.renderScrollingText(
            graphics,
            "text:value",
            this.value,
            textX,
            textY,
            textSize,
            this.color(255, 255, 255, 235, alpha),
            this.controlX + 4.0F * scale,
            this.controlY,
            available + 2.0F * scale,
            this.controlHeight,
            this.controlX,
            this.controlY,
            this.controlWidth,
            this.controlHeight
         );
      }

      float iconSize = 12.5F;
      this.renderGuiIcon("S", x + 3.5F * scale, this.controlY - 0.5F * scale, iconSize, scale);
      this.renderOptionLabel(graphics, "text", x + 15.5F * scale, this.controlY + 1.5F * scale, 7.2F, this.controlX - 4.0F * scale, y, height);
   }

   @Override
   public boolean mouseClicked(Click event, boolean doubled) {
      if (event.button() != 0) {
         return false;
      } else if (this.hovered(event.x(), event.y(), this.controlX, this.controlY, this.controlWidth, this.controlHeight)) {
         this.editing = true;
         this.selectedAll = false;
         this.cursor = this.value.length();
         return true;
      } else {
         this.editing = false;
         this.selectedAll = false;
         return false;
      }
   }

   @Override
   public boolean keyPressed(KeyInput event) {
      if (!this.editing) {
         return false;
      } else {
         int key = event.key();
         boolean ctrl = (event.modifiers() & 2) != 0;
         if (ctrl && key == 65) {
            this.selectedAll = true;
            this.cursor = this.value.length();
            return true;
         } else if (ctrl && key == 67) {
            this.setClipboard(this.value);
            return true;
         } else if (ctrl && key == 86) {
            this.insertText(this.getClipboard());
            return true;
         } else if (key == 259) {
            if (this.selectedAll) {
               this.value = "";
               this.cursor = 0;
               this.selectedAll = false;
            } else if (this.cursor > 0) {
               this.value = this.value.substring(0, this.cursor - 1) + this.value.substring(this.cursor);
               this.cursor--;
            }

            this.syncSetting();
            return true;
         } else if (key == 261) {
            if (this.selectedAll) {
               this.value = "";
               this.cursor = 0;
               this.selectedAll = false;
            } else if (this.cursor < this.value.length()) {
               this.value = this.value.substring(0, this.cursor) + this.value.substring(this.cursor + 1);
            }

            this.syncSetting();
            return true;
         } else if (key == 263) {
            this.cursor = Math.max(0, this.cursor - 1);
            this.selectedAll = false;
            return true;
         } else if (key == 262) {
            this.cursor = Math.min(this.value.length(), this.cursor + 1);
            this.selectedAll = false;
            return true;
         } else if (key == 268) {
            this.cursor = 0;
            this.selectedAll = false;
            return true;
         } else if (key == 269) {
            this.cursor = this.value.length();
            this.selectedAll = false;
            return true;
         } else if (key != 257 && key != 335 && key != 256) {
            return true;
         } else {
            this.editing = false;
            this.selectedAll = false;
            return true;
         }
      }
   }

   @Override
   public boolean charTyped(CharInput event) {
      if (this.editing && event.isValidChar()) {
         this.insertText(event.asString());
         return true;
      } else {
         return false;
      }
   }

   private void insertText(String text) {
      if (text != null && !text.isEmpty()) {
         String cleaned = text.replace("\r", "").replace("\n", " ");
         if (this.selectedAll) {
            this.value = cleaned;
            this.cursor = this.value.length();
            this.selectedAll = false;
            this.syncSetting();
         } else {
            this.value = this.value.substring(0, this.cursor) + cleaned + this.value.substring(this.cursor);
            this.cursor = this.cursor + cleaned.length();
            this.syncSetting();
         }
      }
   }

   private String getClipboard() {
      MinecraftClient client = MinecraftClient.getInstance();
      String clipboard = GLFW.glfwGetClipboardString(client.getWindow().getHandle());
      return clipboard == null ? "" : clipboard;
   }

   private void setClipboard(String text) {
      MinecraftClient client = MinecraftClient.getInstance();
      GLFW.glfwSetClipboardString(client.getWindow().getHandle(), text);
   }

   private void syncSetting() {
      this.setting.setValue(this.value);
      this.value = this.setting.getValue();
      this.cursor = Math.min(this.cursor, this.value.length());
      this.invalidateCursorWidth();
   }

   private float cursorWidth(float textSize) {
      int safeCursor = Math.min(this.cursor, this.value.length());
      if (this.cachedCursorValue != this.value || this.cachedCursor != safeCursor) {
         this.cachedCursorValue = this.value;
         this.cachedCursor = safeCursor;
         this.cachedCursorWidth = this.textWidth(this.value.substring(0, safeCursor), textSize);
      }

      return this.cachedCursorWidth;
   }

   private void invalidateCursorWidth() {
      this.cachedCursorValue = null;
      this.cachedCursor = -1;
      this.cachedCursorWidth = 0.0F;
   }
}
