package daamky.ua.api.drag.impl;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.sound.SoundEvent;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.utils.sounds.SoundManager;

public final class Notifications extends HudPanel {
   private static final long STAY_MS = 2500L;
   private static final float ANIM_S = 0.35F;
   private static final float ROW_HEIGHT = 23.0F;
   private static final float ROW_GAP = 3.0F;
   private static final float MIN_WIDTH = 118.0F;
   private static final float MAX_WIDTH = 220.0F;
   private static final float TEXT_X = 20.0F;
   private static final float RIGHT_PADDING = 8.0F;
   private static final float TITLE_SIZE = 6.4F;
   private static final float BODY_SIZE = 5.6F;
   private static final float SPRING_K = 180.0F;
   private static final float SPRING_D = 12.0F;
   private static final float SPRING_DT = 0.016F;
   private static final int MAX_NOTIFICATIONS = 8;
   private static final List<Notifications.NotifEntry> NOTIFICATIONS = new ArrayList<>();
   private final SmoothAnimation previewAnim = new SmoothAnimation();
   private boolean wasPreview = false;

   public Notifications() {
      super("notifications", "Notifications", 10.0F, 330.0F, 118.0F, 24.0F);
   }

   public static void push(String title, String text) {
      push(title, text, 2500L);
   }

   public static void push(String title, String text, long stayMs) {
      push(title, text, stayMs, null);
   }

   public static void push(String title, String text, long stayMs, SoundEvent sound) {
      if (NOTIFICATIONS.size() >= 8) {
         NOTIFICATIONS.subList(0, NOTIFICATIONS.size() - 8 + 1).clear();
      }

      Notifications.NotifEntry e = new Notifications.NotifEntry(
         title != null && !title.isBlank() ? title : "Notification", text == null ? "" : text, System.currentTimeMillis() + Math.max(1L, stayMs)
      );
      float startY = stackHeight();
      e.currentY = Math.max(0.0F, startY - 3.0F);
      e.targetY = startY;
      e.velocityY = 0.0F;
      NOTIFICATIONS.add(e);
      e.anim.run(1.0, 0.35F, Easings.EXPO_OUT);
      if (sound != null) {
         SoundManager.playSoundDirect(sound, 0.7F, 1.0F);
      }
   }

   @Override
   public void render() {
      Notifications.NotificationsState state = this.logics();
      if (state != null) {
         this.renderNotifications(state);
      }
   }

   private Notifications.NotificationsState logics() {
      long now = System.currentTimeMillis();

      for (Notifications.NotifEntry e : NOTIFICATIONS) {
         e.anim.update();
         if (!e.exiting && now > e.removeTime) {
            e.exiting = true;
            e.anim.run(0.0, 0.35F, Easings.EXPO_IN);
         }
      }

      boolean preview = NOTIFICATIONS.isEmpty() && this.editPreview();
      float width = 118.0F;
      if (preview) {
         width = fitWidth(width, "Notification", TITLE_FONT, 6.4F);
         width = fitWidth(width, "Example notification", TEXT_FONT, 5.6F);
      }

      for (Notifications.NotifEntry ex : NOTIFICATIONS) {
         width = fitWidth(width, ex.title, TITLE_FONT, 6.4F);
         width = fitWidth(width, ex.text, TEXT_FONT, 5.6F);
      }

      this.drag.size((float)Math.ceil((double)width), 23.0F);
      NOTIFICATIONS.removeIf(ex -> ex.exiting && ex.anim.isFinished());
      float offsetY = 0.0F;

      for (Notifications.NotifEntry ex : NOTIFICATIONS) {
         float a = clamp(ex.anim.get(), 0.0F, 1.0F);
         ex.targetY = offsetY;
         offsetY += 26.0F * a;
      }

      for (Notifications.NotifEntry ex : NOTIFICATIONS) {
         float diff = ex.targetY - ex.currentY;
         ex.velocityY = ex.velocityY + (diff * 180.0F - ex.velocityY * 12.0F) * 0.016F;
         ex.currentY = ex.currentY + ex.velocityY * 0.016F;
         if (Math.abs(diff) < 0.01F && Math.abs(ex.velocityY) < 0.01F) {
            ex.currentY = ex.targetY;
            ex.velocityY = 0.0F;
         }
      }

      this.previewAnim.update();
      if (preview && !this.wasPreview) {
         this.previewAnim.run(1.0, 0.35F, Easings.EXPO_OUT);
      } else if (!preview && this.wasPreview) {
         this.previewAnim.run(0.0, 0.35F, Easings.EXPO_IN);
      }

      this.wasPreview = preview;
      boolean visible = !NOTIFICATIONS.isEmpty() || this.previewAnim.get() > 0.01F;
      this.contentVisible(visible);
      return !visible ? null : new Notifications.NotificationsState(preview, this.previewAnim.get(), this.drag.x(), this.drag.y(), this.drag.width());
   }

   private void renderNotifications(Notifications.NotificationsState state) {
      if (state.previewAlpha > 0.01F) {
         Hud hud = Hud.getInstance();
         int hudColor = hud != null ? hud.iconColor() : ColorUtil.rgba(247, 133, 255, 255);
         int fadedHudColor = ColorUtil.multAlpha(hudColor, state.previewAlpha);
         int glowColor = ColorUtil.multAlpha(hudColor, 0.55F * state.previewAlpha);
         String title = "Notification";
         String text = "Example notification";
         float x = state.x;
         float y = state.y;
         float width = state.width;
         float alpha = clamp(state.previewAlpha, 0.0F, 1.0F);
         int background = ColorUtil.rgba(0, 0, 0, Math.round(255.0F * alpha));
         int glow = ColorUtil.rgba(247, 133, 255, Math.round(130.0F * alpha));
         int icon = ColorUtil.rgba(247, 133, 255, Math.round(255.0F * alpha));
         int titleColor = ColorUtil.rgba(255, 255, 255, Math.round(245.0F * alpha));
         int textColor = ColorUtil.rgba(183, 190, 202, Math.round(215.0F * alpha));
         float textWidth = width - 20.0F - 8.0F;
         String titleText = this.trimToWidth(title, TITLE_FONT, 6.4F, textWidth);
         String bodyText = this.trimToWidth(text, TEXT_FONT, 5.6F, textWidth);
         HudRenderCompat.background(x, y, width, 23.0F, 4.0F, 15.0F, 1.2F, background);
         HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", x - 6.0F, y - 6.0F, 38.0F, 38.0F, 0.0F, glowColor);
         Render2D.text(FontType.MAINMENUSCREEN, "v", x + 5.0F, y + 5.0F, 14.0F, fadedHudColor);
         Render2D.text(TITLE_FONT, titleText, x + 20.0F, y + 4.5F, 6.4F, titleColor);
         Render2D.text(TEXT_FONT, bodyText, x + 20.0F, y + 11.5F, 5.6F, textColor);
      }

      if (!state.preview) {
         for (Notifications.NotifEntry e : NOTIFICATIONS) {
            float anim = e.anim.get();
            if (!(anim < 0.01F)) {
               Hud hud = Hud.getInstance();
               int hudColor = hud != null ? hud.iconColor() : ColorUtil.rgba(247, 133, 255, 255);
               int fadedHudColor = ColorUtil.multAlpha(hudColor, anim);
               int glowColor = ColorUtil.multAlpha(hudColor, 0.55F * anim);
               String title = e.title;
               String text = e.text;
               float x = state.x;
               float y = state.y + e.currentY;
               float width = state.width;
               float alpha = clamp(anim, 0.0F, 1.0F);
               int background = ColorUtil.rgba(0, 0, 0, Math.round(255.0F * alpha));
               int glow = ColorUtil.rgba(247, 133, 255, Math.round(130.0F * alpha));
               int icon = ColorUtil.rgba(247, 133, 255, Math.round(255.0F * alpha));
               int titleColor = ColorUtil.rgba(255, 255, 255, Math.round(245.0F * alpha));
               int textColor = ColorUtil.rgba(183, 190, 202, Math.round(215.0F * alpha));
               float textWidth = width - 20.0F - 8.0F;
               String titleText = this.trimToWidth(title, TITLE_FONT, 6.4F, textWidth);
               String bodyText = this.trimToWidth(text, TEXT_FONT, 5.6F, textWidth);
               HudRenderCompat.background(x, y, width, 23.0F, 4.0F, 15.0F, 1.2F, background);
               HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", x - 6.0F, y - 6.0F, 38.0F, 38.0F, 0.0F, glowColor);
               Render2D.text(FontType.MAINMENUSCREEN, "v", x + 5.0F, y + 5.0F, 14.0F, fadedHudColor);
               Render2D.text(TITLE_FONT, titleText, x + 20.0F, y + 4.5F, 6.4F, titleColor);
               Render2D.text(TEXT_FONT, bodyText, x + 20.0F, y + 11.5F, 5.6F, textColor);
            }
         }
      }
   }

   private static float stackHeight() {
      float height = 0.0F;

      for (Notifications.NotifEntry e : NOTIFICATIONS) {
         height += 26.0F * clamp(e.anim.get(), 0.0F, 1.0F);
      }

      return height;
   }

   private static float fitWidth(float current, String text, FontType font, float size) {
      float needed = Render2D.textWidth(font, text, size) + 20.0F + 8.0F;
      return Math.min(220.0F, Math.max(current, needed));
   }

   private static final class NotifEntry {
      final String title;
      final String text;
      final long removeTime;
      final SmoothAnimation anim = new SmoothAnimation();
      boolean exiting = false;
      float currentY = 0.0F;
      float targetY = 0.0F;
      float velocityY = 0.0F;

      NotifEntry(String title, String text, long removeTime) {
         this.title = title;
         this.text = text;
         this.removeTime = removeTime;
      }
   }

   private static record NotificationsState(boolean preview, float previewAlpha, float x, float y, float width) {
   }
}
