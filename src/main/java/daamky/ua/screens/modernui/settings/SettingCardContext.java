package daamky.ua.screens.modernui.settings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.api.theme.ClientTheme;
import daamky.ua.api.theme.ThemeManager;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

final class SettingCardContext {
   NumberSetting draggingSlider;
   ColorSetting activeColor;
   ColorSetting draggingColor;
   int draggingColorPart;
   BindSetting listeningBind;
   StringSetting focusedText;
   float moduleClipX;
   float moduleClipY;
   float moduleClipWidth;
   float moduleClipHeight;
   double mouseX;
   double mouseY;
   float cardWidth = 150.0F;
   private float visualAlpha = 1.0F;

   SettingCardContext() {
   }

   void renderClippedText(DrawContext graphics, FontType font, String text, float x, float y, float width, float size, int color) {
      Render2D.pushScissor(graphics, x, y - 2.0F, width, size + 6.0F);
      Render2D.text(font, text == null ? "" : text, x, y, size, this.multiplyAlpha(color, this.visualAlpha));
      Render2D.popScissor(graphics);
   }

   List<String> wrapText(FontType font, String text, float size, float width, int maxLines) {
      List<String> lines = new ArrayList<>();
      if (text != null && !text.isBlank() && !(width <= 0.0F) && maxLines > 0) {
         String[] words = text.trim().split("\\s+");
         StringBuilder line = new StringBuilder();
         int index = 0;

         while (index < words.length && lines.size() < maxLines) {
            String word = words[index++];
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (Render2D.textWidth(font, candidate, size) <= width) {
               line.setLength(0);
               line.append(candidate);
            } else if (!line.isEmpty()) {
               lines.add(line.toString());
               line.setLength(0);
               index--;
            } else {
               lines.add(this.trimToWidth(font, word, size, width));
            }
         }

         if (!line.isEmpty() && lines.size() < maxLines) {
            lines.add(line.toString());
         }

         return lines;
      } else {
         return lines;
      }
   }

   void renderWrappedText(DrawContext graphics, FontType font, List<String> lines, float x, float y, float width, float size, float lineHeight, int color) {
      for (int i = 0; i < lines.size(); i++) {
         this.renderClippedText(graphics, font, lines.get(i), x, y + lineHeight * (float)i, width, size, color);
      }
   }

   private String trimToWidth(FontType font, String text, float size, float width) {
      if (text != null && !text.isEmpty()) {
         StringBuilder builder = new StringBuilder();

         for (int i = 0; i < text.length(); i++) {
            String candidate = "" + builder + text.charAt(i);
            if (Render2D.textWidth(font, candidate, size) > width && !builder.isEmpty()) {
               break;
            }

            builder.append(text.charAt(i));
         }

         return builder.toString();
      } else {
         return "";
      }
   }

   int withAlpha(Color color, float alpha) {
      return new Color(
            color.getRed(),
            color.getGreen(),
            color.getBlue(),
            this.clampInt(Math.round((float)color.getAlpha() * this.clamp(alpha, 0.0F, 1.0F) * this.visualAlpha))
         )
         .getRGB();
   }

   int color(int red, int green, int blue, int alpha) {
      return new Color(red, green, blue, this.clampInt(Math.round((float)alpha * this.visualAlpha))).getRGB();
   }

   int multiplyAlpha(int color, float alpha) {
      return new Color(red(color), green(color), blue(color), this.clampInt(Math.round((float)(color >>> 24 & 0xFF) * this.clamp(alpha, 0.0F, 1.0F)))).getRGB();
   }

   void setVisualAlpha(float visualAlpha) {
      this.visualAlpha = this.clamp(visualAlpha, 0.0F, 1.0F);
   }

   float visualAlpha() {
      return this.visualAlpha;
   }

   ClientTheme theme() {
      return ThemeManager.current();
   }

   float lerp(float current, float target, float speed) {
      return Math.abs(target - current) < 0.001F ? target : current + (target - current) * this.clamp(speed, 0.0F, 1.0F);
   }

   float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
   }

   String getClipboard() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      if (minecraft != null && minecraft.getWindow() != null) {
         String clipboard = GLFW.glfwGetClipboardString(minecraft.getWindow().getHandle());
         return clipboard == null ? "" : clipboard;
      } else {
         return "";
      }
   }

   void setClipboard(String text) {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      if (minecraft != null && minecraft.getWindow() != null) {
         GLFW.glfwSetClipboardString(minecraft.getWindow().getHandle(), text == null ? "" : text);
      }
   }

   int clampInt(int value) {
      return Math.max(0, Math.min(255, value));
   }

   static int red(int color) {
      return color >>> 16 & 0xFF;
   }

   static int green(int color) {
      return color >>> 8 & 0xFF;
   }

   static int blue(int color) {
      return color & 0xFF;
   }
}
