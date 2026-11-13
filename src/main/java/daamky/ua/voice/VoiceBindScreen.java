package daamky.ua.voice;

import java.awt.Color;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import daamky.ua.api.module.Module;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class VoiceBindScreen extends Screen {
   private static final float PANEL_WIDTH = 390.0F;
   private static final float PANEL_HEIGHT = 250.0F;
   private static final float CARD_WIDTH = 170.0F;
   private static final float CARD_HEIGHT = 96.0F;
   private static final float BUTTON_HEIGHT = 22.0F;
   private final Screen parent;
   private final Module module;
   private final VoiceBindManager voice = VoiceBindManager.getInstance();
   private long openedAt;

   public VoiceBindScreen(Screen parent, Module module) {
      super(Text.literal("Голосовой бинд"));
      this.parent = parent;
      this.module = module;
   }

   protected void init() {
      this.openedAt = System.currentTimeMillis();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      float progress = Math.min(1.0F, (float)(System.currentTimeMillis() - this.openedAt) / 220.0F);
      progress = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);
      float panelX = ((float)this.width - 390.0F) * 0.5F;
      float panelY = ((float)this.height - 250.0F) * 0.5F + (1.0F - progress) * 12.0F;
      Render2D.beginFrame(context);
      Render2D.rect(-4.0F, -4.0F, (float)this.width + 8.0F, (float)this.height + 8.0F, 0.0F, rgba(3, 2, 8, Math.round(118.0F * progress)));
      Render2D.blur(
         panelX,
         panelY,
         390.0F,
         250.0F,
         12.0F,
         34.0F,
         1.4F,
         rgba(255, 255, 255, 255),
         rgba(255, 255, 255, 255),
         rgba(255, 255, 255, 255),
         rgba(255, 255, 255, 255)
      );
      Render2D.liquidGlass(panelX, panelY, 390.0F, 250.0F, 2.0F, 0.07F, 12.0F, rgba(255, 255, 255, 255));
      Render2D.rect(panelX, panelY, 390.0F, 250.0F, 12.0F, rgba(26, 21, 34, 210), rgba(19, 17, 27, 218), rgba(12, 12, 18, 226), rgba(21, 17, 28, 218));
      Render2D.outline(
         panelX, panelY, 390.0F, 250.0F, 12.0F, 0.6F, rgba(226, 167, 255, 170), rgba(255, 255, 255, 65), rgba(104, 82, 125, 80), rgba(188, 129, 226, 130)
      );
      this.renderHeader(panelX, panelY, mouseX, mouseY);
      this.renderSection(panelX + 18.0F, panelY + 60.0F, 170.0F, 96.0F, true, mouseX, mouseY);
      this.renderSection(panelX + 202.0F, panelY + 60.0F, 170.0F, 96.0F, false, mouseX, mouseY);
      this.renderStatus(panelX + 18.0F, panelY + 170.0F, 354.0F, 60.0F);
      Render2D.flush();
   }

   private void renderHeader(float panelX, float panelY, int mouseX, int mouseY) {
      float iconX = panelX + 17.0F;
      float iconY = panelY + 16.0F;
      Render2D.rect(iconX, iconY, 28.0F, 28.0F, 7.0F, rgba(186, 116, 225, 220), rgba(226, 151, 235, 225), rgba(119, 79, 152, 225), rgba(151, 91, 190, 220));
      Render2D.outline(iconX, iconY, 28.0F, 28.0F, 7.0F, 0.4F, rgba(255, 255, 255, 150));
      this.renderMicrophone(iconX + 8.5F, iconY + 6.0F, rgba(255, 255, 255, 255), this.voice.isRecording());
      Render2D.text(FontType.BOLD, "Voice Bind", panelX + 54.0F, panelY + 17.0F, 10.0F, rgba(255, 255, 255, 255));
      Render2D.text(FontType.SEMIBOLD, this.module.getName(), panelX + 54.0F, panelY + 32.0F, 6.5F, rgba(173, 159, 184, 255));
      float toggleX = panelX + 238.0F;
      float toggleY = panelY + 20.0F;
      boolean enabled = this.voice.isBackgroundEnabled();
      boolean toggleHovered = inside((double)mouseX, (double)mouseY, toggleX, toggleY, 110.0F, 20.0F);
      Render2D.rect(
         toggleX, toggleY, 110.0F, 20.0F, 6.0F, enabled ? rgba(130, 83, 157, toggleHovered ? 235 : 205) : rgba(65, 59, 72, toggleHovered ? 225 : 190)
      );
      Render2D.outline(toggleX, toggleY, 110.0F, 20.0F, 6.0F, 0.35F, enabled ? rgba(218, 158, 245, 180) : rgba(255, 255, 255, 55));
      Render2D.text(
         FontType.SEMIBOLD,
         enabled ? "Listening" : "Paused",
         toggleX + 10.0F,
         toggleY + 7.0F,
         6.2F,
         enabled ? rgba(245, 222, 255, 255) : rgba(165, 157, 171, 255)
      );
      Render2D.rect(toggleX + 88.0F, toggleY + 6.0F, 13.0F, 8.0F, 4.0F, enabled ? rgba(202, 132, 235, 255) : rgba(92, 85, 98, 255));
      Render2D.rect(toggleX + (enabled ? 95.0F : 89.0F), toggleY + 7.0F, 6.0F, 6.0F, 3.0F, rgba(255, 255, 255, 255));
      float closeX = panelX + 358.0F;
      float closeY = panelY + 20.0F;
      boolean closeHovered = inside((double)mouseX, (double)mouseY, closeX, closeY, 18.0F, 20.0F);
      Render2D.rect(closeX, closeY, 18.0F, 20.0F, 6.0F, closeHovered ? rgba(116, 77, 128, 210) : rgba(255, 255, 255, 18));
      Render2D.text(FontType.SEMIBOLD, "x", closeX + 6.2F, closeY + 6.0F, 7.0F, closeHovered ? rgba(255, 225, 255, 255) : rgba(170, 160, 176, 255));
   }

   private void renderSection(float x, float y, float cardWidth, float cardHeight, boolean wakeWord, int mouseX, int mouseY) {
      int count = wakeWord ? this.voice.getWakeRecordingCount() : this.voice.getModuleRecordingCount(this.module);
      String title = wakeWord ? "Ключевое слово" : "Название модуля";
      String hint = wakeWord ? "Общее для всех команд" : this.module.getName();
      Render2D.rect(x, y, cardWidth, cardHeight, 9.0F, rgba(255, 255, 255, 26), rgba(255, 255, 255, 18), rgba(85, 70, 98, 21), rgba(117, 86, 131, 25));
      Render2D.outline(
         x, y, cardWidth, cardHeight, 9.0F, 0.35F, rgba(255, 255, 255, 76), rgba(255, 255, 255, 35), rgba(118, 91, 132, 65), rgba(192, 138, 217, 82)
      );
      Render2D.rect(x + 10.0F, y + 10.0F, 20.0F, 20.0F, 5.0F, wakeWord ? rgba(153, 98, 184, 185) : rgba(109, 91, 144, 185));
      if (wakeWord) {
         Render2D.text(FontType.BOLD, "K", x + 16.2F, y + 16.2F, 7.0F, rgba(255, 255, 255, 255));
      } else {
         this.renderMicrophone(x + 16.0F, y + 13.0F, rgba(255, 255, 255, 245), false);
      }

      Render2D.text(FontType.BOLD, title, x + 38.0F, y + 10.5F, 7.2F, rgba(250, 247, 252, 255));
      Render2D.text(FontType.SEMIBOLD, hint, x + 38.0F, y + 22.0F, 5.5F, rgba(145, 134, 153, 255));
      if (wakeWord) {
         boolean required = this.voice.isWakeWordRequired();
         Render2D.rect(x + 9.0F, y + 37.0F, 76.0F, 15.0F, 5.0F, required ? rgba(142, 92, 169, 150) : rgba(76, 69, 84, 130));
         Render2D.outline(x + 9.0F, y + 37.0F, 76.0F, 15.0F, 5.0F, 0.3F, required ? rgba(221, 162, 244, 115) : rgba(255, 255, 255, 42));
         Render2D.text(
            FontType.SEMIBOLD,
            required ? "Обязательно: ВКЛ" : "Обязательно: ВЫКЛ",
            x + 15.0F,
            y + 42.0F,
            5.0F,
            required ? rgba(240, 211, 251, 255) : rgba(161, 151, 167, 255)
         );
      }

      String countText = count + (count == 1 ? " запись" : " записей");
      float countWidth = Render2D.textWidth(FontType.SEMIBOLD, countText, 5.5F) + 12.0F;
      Render2D.rect(x + cardWidth - countWidth - 9.0F, y + 37.0F, countWidth, 15.0F, 5.0F, rgba(156, 111, 180, count > 0 ? 115 : 48));
      Render2D.text(
         FontType.SEMIBOLD, countText, x + cardWidth - countWidth - 3.0F, y + 42.0F, 5.5F, count > 0 ? rgba(226, 193, 239, 255) : rgba(134, 125, 140, 255)
      );
      float recordX = x + 9.0F;
      float recordY = y + cardHeight - 31.0F;
      float recordWidth = 108.0F;
      float resetX = x + 123.0F;
      float resetWidth = cardWidth - 132.0F;
      boolean recording = this.voice.isRecording();
      this.renderButton(recordX, recordY, recordWidth, 22.0F, recording ? "Отменить" : "Записать", mouseX, mouseY, true);
      this.renderButton(resetX, recordY, resetWidth, 22.0F, "Сброс", mouseX, mouseY, false);
   }

   private void renderStatus(float x, float y, float statusWidth, float statusHeight) {
      Render2D.rect(x, y, statusWidth, statusHeight, 9.0F, rgba(115, 83, 132, 46), rgba(80, 66, 93, 35), rgba(36, 32, 44, 56), rgba(71, 51, 83, 46));
      Render2D.outline(x, y, statusWidth, statusHeight, 9.0F, 0.35F, rgba(198, 146, 224, 70));
      int dotColor;
      if (this.voice.isRecording()) {
         float pulse = 0.55F + 0.45F * (float)Math.sin((double)System.currentTimeMillis() * 0.009);
         dotColor = rgba(235, 128, 212, Math.round(160.0F + 95.0F * pulse));
      } else if (this.voice.isBackgroundEnabled()) {
         dotColor = rgba(172, 122, 213, 255);
      } else {
         dotColor = rgba(105, 98, 112, 255);
      }

      Render2D.rect(x + 12.0F, y + 13.0F, 7.0F, 7.0F, 3.5F, dotColor);
      Render2D.text(FontType.BOLD, this.voice.getStatus(), x + 27.0F, y + 10.5F, 7.0F, rgba(239, 231, 243, 255));
      Render2D.text(
         FontType.SEMIBOLD,
         this.voice.isWakeWordRequired() ? "Скажите ключевое слово, затем записанное название модуля" : "Ключевое слово выключено — сразу назовите модуль",
         x + 27.0F,
         y + 25.0F,
         5.7F,
         rgba(146, 135, 154, 255)
      );
      Render2D.text(FontType.SEMIBOLD, "Для точности сделайте 3–5 похожих записей каждой команды", x + 27.0F, y + 38.0F, 5.5F, rgba(112, 103, 120, 255));
   }

   private void renderButton(float x, float y, float buttonWidth, float buttonHeight, String text, int mouseX, int mouseY, boolean primary) {
      boolean hovered = inside((double)mouseX, (double)mouseY, x, y, buttonWidth, buttonHeight);
      int fill;
      int outline;
      if (primary) {
         fill = hovered ? rgba(159, 101, 190, 225) : rgba(126, 83, 151, 195);
         outline = hovered ? rgba(235, 181, 255, 190) : rgba(201, 145, 226, 112);
      } else {
         fill = hovered ? rgba(91, 73, 102, 210) : rgba(255, 255, 255, 22);
         outline = hovered ? rgba(207, 175, 220, 110) : rgba(255, 255, 255, 48);
      }

      Render2D.rect(x, y, buttonWidth, buttonHeight, 6.0F, fill);
      Render2D.outline(x, y, buttonWidth, buttonHeight, 6.0F, 0.35F, outline);
      float textSize = 6.0F;
      float textWidth = Render2D.textWidth(FontType.SEMIBOLD, text, textSize);
      Render2D.text(
         FontType.SEMIBOLD, text, x + (buttonWidth - textWidth) * 0.5F, y + 7.5F, textSize, primary ? rgba(255, 247, 255, 255) : rgba(201, 190, 207, 255)
      );
   }

   private void renderMicrophone(float x, float y, int color, boolean animated) {
      float pulse = animated ? 0.5F + 0.5F * (float)Math.sin((double)System.currentTimeMillis() * 0.01) : 0.0F;
      if (animated) {
         Render2D.rect(
            x - 2.0F - pulse, y - 2.0F - pulse, 13.0F + pulse * 2.0F, 16.0F + pulse * 2.0F, 6.0F, rgba(238, 164, 247, Math.round(25.0F + pulse * 35.0F))
         );
      }

      Render2D.rect(x + 2.5F, y, 5.0F, 8.0F, 2.5F, color);
      Render2D.rect(x, y + 5.0F, 1.2F, 4.0F, 0.6F, color);
      Render2D.rect(x + 8.8F, y + 5.0F, 1.2F, 4.0F, 0.6F, color);
      Render2D.rect(x + 1.0F, y + 8.0F, 8.0F, 1.2F, 0.6F, color);
      Render2D.rect(x + 4.4F, y + 9.0F, 1.2F, 3.0F, 0.6F, color);
      Render2D.rect(x + 2.0F, y + 11.5F, 6.0F, 1.2F, 0.6F, color);
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      if (click.button() != 0) {
         return true;
      } else {
         float panelX = ((float)this.width - 390.0F) * 0.5F;
         float panelY = ((float)this.height - 250.0F) * 0.5F;
         if (inside(click.x(), click.y(), panelX + 238.0F, panelY + 20.0F, 110.0F, 20.0F)) {
            this.voice.setBackgroundEnabled(!this.voice.isBackgroundEnabled());
            return true;
         } else if (inside(click.x(), click.y(), panelX + 358.0F, panelY + 20.0F, 18.0F, 20.0F)) {
            this.close();
            return true;
         } else {
            float wakeX = panelX + 18.0F;
            if (inside(click.x(), click.y(), wakeX + 9.0F, panelY + 97.0F, 76.0F, 15.0F)) {
               this.voice.setWakeWordRequired(!this.voice.isWakeWordRequired());
               return true;
            } else {
               float wakeY = panelY + 60.0F + 96.0F - 31.0F;
               if (inside(click.x(), click.y(), wakeX + 9.0F, wakeY, 108.0F, 22.0F)) {
                  this.toggleRecording(VoiceBindManager.RecordTarget.WAKE_WORD);
                  return true;
               } else if (inside(click.x(), click.y(), wakeX + 123.0F, wakeY, 38.0F, 22.0F)) {
                  this.voice.resetWakeRecordings();
                  return true;
               } else {
                  float moduleX = panelX + 202.0F;
                  float moduleY = panelY + 60.0F + 96.0F - 31.0F;
                  if (inside(click.x(), click.y(), moduleX + 9.0F, moduleY, 108.0F, 22.0F)) {
                     this.toggleRecording(VoiceBindManager.RecordTarget.MODULE_NAME);
                     return true;
                  } else if (inside(click.x(), click.y(), moduleX + 123.0F, moduleY, 38.0F, 22.0F)) {
                     this.voice.resetModuleRecordings(this.module);
                     return true;
                  } else {
                     return true;
                  }
               }
            }
         }
      }
   }

   private void toggleRecording(VoiceBindManager.RecordTarget target) {
      if (this.voice.isRecording()) {
         this.voice.cancelRecording();
      } else {
         this.voice.beginRecording(target, target == VoiceBindManager.RecordTarget.MODULE_NAME ? this.module : null);
      }
   }

   public void close() {
      if (this.voice.isRecording()) {
         this.voice.cancelRecording();
      }

      this.client.setScreen(this.parent);
   }

   public boolean shouldPause() {
      return false;
   }

   private static boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= (double)x && mouseX < (double)(x + width) && mouseY >= (double)y && mouseY < (double)(y + height);
   }

   private static int rgba(int red, int green, int blue, int alpha) {
      return new Color(red, green, blue, Math.max(0, Math.min(255, alpha))).getRGB();
   }
}
