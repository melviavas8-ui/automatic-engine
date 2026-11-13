package daamky.ua.api.drag.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.Click;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.utils.render.ScissorUtil;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;
import daamky.ua.utils.render.ui.font.FontType;

public final class MediaPlayer extends HudPanel {
   private static final long POLL_INTERVAL_MS = 500L;
   private static final ExecutorService MEDIA_EXECUTOR = Executors.newSingleThreadExecutor(thread -> {
      Thread worker = new Thread(thread, "daamky-MediaPlayer");
      worker.setDaemon(true);
      return worker;
   });
   private volatile MediaPlayer.MediaInfo media = MediaPlayer.MediaInfo.empty();
   private volatile Object session;
   private volatile long lastPositionPollMs;
   private volatile boolean unavailable;
   private final AtomicBoolean pollScheduled = new AtomicBoolean();
   private long lastPollRequestMs;
   private boolean iconPulseWasPlaying;
   private long iconPulseStartedAt;
   private float smoothedIconAlpha = 100.0F;
   private float smoothDurationWidth = 1.0F;
   private String scrollingTitle = "";
   private long titleScrollStartedAt;
   private final SmoothAnimation panelAnimation = new SmoothAnimation();

   public MediaPlayer() {
      super("mediaplayer", "MediaPlayer", 10.0F, 120.0F, 132.0F, 42.0F);
   }

   @Override
   public void render() {
      MediaPlayer.MediaPlayerState state = this.logics();
      if (state != null) {
         this.renderMediaPlayer(state);
      }
   }

   @Override
   public boolean mouseClicked(Click event, boolean doubled) {
      if (event != null && event.button() == 0 && this.drag.visible() && this.session != null) {
         float x = this.drag.x();
         float y = this.drag.y();
         float w = 110.0F;
         float textX = x + 34.0F;
         float textWidth = w - 50.0F;
         float controlsCenterX = textX + textWidth * 0.5F;
         float controlsY = y + 26.0F;
         float hitHalf = 8.0F;
         if (this.hit(event, controlsCenterX - 9.0F, controlsY, hitHalf)) {
            this.invokeSession("previous");
            return true;
         } else if (this.hit(event, controlsCenterX, controlsY, hitHalf)) {
            this.invokeSession("playPause");
            return true;
         } else if (this.hit(event, controlsCenterX + 9.0F, controlsY, hitHalf)) {
            this.invokeSession("next");
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private MediaPlayer.MediaPlayerState logics() {
      this.pollMedia();
      boolean preview = this.media.isEmpty() && this.editPreview();
      boolean visible = !this.media.isEmpty() || preview;
      this.panelAnimation.update();
      this.panelAnimation.run(visible ? 1.0 : 0.0, 0.24F, visible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
      float alpha = this.panelAnimation.get();
      this.contentVisible(visible || alpha > 0.01F || this.panelAnimation.isAlive());
      if (alpha <= 0.0F && !this.panelAnimation.isAlive()) {
         return null;
      } else {
         MediaPlayer.MediaInfo shown = preview ? new MediaPlayer.MediaInfo("No track", "No artist", 0L, 0L, false) : this.media;
         this.size(132.0F, 42.0F);
         return new MediaPlayer.MediaPlayerState(shown, alpha, this.drag.x(), this.drag.y(), this.drag.width(), this.drag.height());
      }
   }

   private void renderMediaPlayer(MediaPlayer.MediaPlayerState state) {
      float x = state.x;
      float y = state.y;
      float w = 110.0F;
      float h = 35.0F;
      float artworkSize = 25.5F;
      float textX = x + 34.0F;
      float textWidth = w - 50.0F;
      int duration = this.safeDuration(state.media);
      int position = this.interpolatedPosition(state.media, duration);
      int iconAlpha = this.resolveMusicIconAlpha(state.media.playing, state.alpha);
      String title = this.safe(state.media.title);
      String artist = this.trimToWidth(this.safe(state.media.artist), TEXT_FONT, 5.5F, textWidth);
      String posText = this.formatDuration(position);
      String durText = this.formatDuration(duration);
      float titleInset = Render2D.textVisualLeftInset(TITLE_FONT, title, 6.0F);
      float barX = textX + titleInset;
      float contentWidth = Math.max(1.0F, textWidth - titleInset);
      float targetWidth = duration > 0 ? (float)position / (float)duration * contentWidth : 0.0F;
      targetWidth = Math.max(0.0F, Math.min(targetWidth, contentWidth));
      float durWidth = Render2D.textWidth(TEXT_FONT, durText, 5.0F);
      float controlsCenterX = barX + contentWidth * 0.5F;
      float controlsY = y + 26.0F;
      int coverColor = ColorUtil.rgba(80, 80, 80, Math.round(50.0F * state.alpha));
      int white = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * state.alpha));
      int muted = ColorUtil.rgba(187, 187, 187, Math.round(255.0F * state.alpha));
      int barBack = ColorUtil.rgba(58, 58, 58, Math.round(255.0F * state.alpha));
      int purple = ColorUtil.rgba(158, 107, 255, Math.round(255.0F * state.alpha));
      Hud hud = Hud.getInstance();
      int hudColor = hud != null ? hud.iconColor() : ColorUtil.rgba(247, 133, 255, 255);
      int pulsingIconColor = ColorUtil.multAlpha(hudColor, (float)iconAlpha / 255.0F);
      int accentColor = ColorUtil.multAlpha(hudColor, state.alpha);
      this.smoothDurationWidth = clamp(this.smoothDurationWidth + (targetWidth - this.smoothDurationWidth) * 0.2F, 1.0F, contentWidth);
      HudRenderCompat.background(
         Render2D.blurBuilder()
            .rectangle(x, y, w, h)
            .radius(2.0F, 7.0F, 2.0F, 7.0F)
            .blurRadius(15.0F)
            .smoothness(1.2F)
            .color(ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)))
            .build()
      );
      HudRenderCompat.background(
         Render2D.blurBuilder()
            .rectangle(x + 5.0F, y + 5.0F, artworkSize, artworkSize)
            .radius(2.0F, 4.0F, 2.0F, 4.0F)
            .blurRadius(15.0F)
            .smoothness(1.2F)
            .color(coverColor)
            .build()
      );
      this.drawIconCentered("u", x + 18.0F, y + 12.5F, 12.0F, pulsingIconColor);
      float titleX = barX - 0.5F;
      float artistXX = this.visualTextX(barX + 0.5F, TEXT_FONT, artist, 5.5F);
      float artistX = this.visualTextX(barX + 0.5F, TEXT_FONT, artist, 5.5F);
      float positionX = this.visualTextX(barX, TEXT_FONT, posText, 5.2F);
      this.renderScrollingTitle(title, artistXX, y + 5.0F, contentWidth, white);
      Render2D.text(TEXT_FONT, artist, artistX, y + 12.0F, 5.5F, muted);
      Render2D.text(TEXT_FONT, posText, positionX, y + 24.0F, 5.2F, white);
      Render2D.text(TEXT_FONT, durText, barX + contentWidth - durWidth, y + 24.0F, 5.2F, white);
      Render2D.rect(barX - 0.5F, y + h - 15.0F, contentWidth, 2.0F, 4.0F, barBack);
      Render2D.rect(barX - 0.5F, y + h - 15.0F, this.smoothDurationWidth + 0.5F, 2.0F, 4.0F, accentColor);
      this.drawIconCentered("s", controlsCenterX - 9.0F, controlsY - 1.1F, 6.0F, white);
      this.drawIconCentered(state.media.playing ? "o" : "p", controlsCenterX, controlsY - 1.5F, 7.5F, white);
      this.drawIconCentered("n", controlsCenterX + 9.0F, controlsY - 1.1F, 6.0F, white);
   }

   private void renderScrollingTitle(String title, float x, float y, float width, int color) {
      float titleX = this.visualTextX(x, TITLE_FONT, title, 6.0F);
      float titleWidth = Render2D.textWidth(TITLE_FONT, title, 6.0F);
      if (titleWidth <= width) {
         this.scrollingTitle = title;
         this.titleScrollStartedAt = 0L;
         Render2D.text(TITLE_FONT, title, titleX, y, 6.0F, color);
      } else {
         long now = System.currentTimeMillis();
         if (!title.equals(this.scrollingTitle)) {
            this.scrollingTitle = title;
            this.titleScrollStartedAt = now;
         }

         float overflow = titleWidth - width;
         long pauseMs = 900L;
         long travelMs = Math.max(1200L, (long)Math.round(overflow / 18.0F * 1000.0F));
         long cycleMs = pauseMs + travelMs + pauseMs + travelMs;
         long cycleTime = Math.floorMod(now - this.titleScrollStartedAt, cycleMs);
         float offset;
         if (cycleTime < pauseMs) {
            offset = 0.0F;
         } else if (cycleTime < pauseMs + travelMs) {
            offset = overflow * ((float)(cycleTime - pauseMs) / (float)travelMs);
         } else if (cycleTime < pauseMs + travelMs + pauseMs) {
            offset = overflow;
         } else {
            offset = overflow * (1.0F - (float)(cycleTime - pauseMs - travelMs - pauseMs) / (float)travelMs);
         }

         ScissorUtil.push(
            Render2DCoordinateSpace.toGuiInt(x),
            Render2DCoordinateSpace.toGuiInt(y - 1.0F),
            Render2DCoordinateSpace.toGuiInt(x + width),
            Render2DCoordinateSpace.toGuiInt(y + 8.0F)
         );
         Render2D.text(TITLE_FONT, title, titleX - offset, y, 6.0F, color);
         ScissorUtil.pop();
      }
   }

   private float visualTextX(float barX, FontType font, String text, float size) {
      return barX - Render2D.textVisualLeftInset(font, text, size);
   }

   private void pollMedia() {
      if (!this.unavailable && this.canPollMedia()) {
         long now = System.currentTimeMillis();
         if (now - this.lastPollRequestMs >= 500L && this.pollScheduled.compareAndSet(false, true)) {
            this.lastPollRequestMs = now;
            MEDIA_EXECUTOR.execute(() -> {
               try {
                  MediaPlayer.PollResult result = this.fetchBestSession();
                  if (result != null) {
                     this.session = result.session();
                     this.media = result.media();
                     this.lastPositionPollMs = System.currentTimeMillis();
                     return;
                  }

                  this.session = null;
                  this.media = MediaPlayer.MediaInfo.empty();
               } catch (Throwable var5) {
                  this.unavailable = true;
                  this.session = null;
                  this.media = MediaPlayer.MediaInfo.empty();
                  return;
               } finally {
                  this.pollScheduled.set(false);
               }
            });
         }
      }
   }

   private boolean canPollMedia() {
      if (this.mc.getWindow() == null) {
         return false;
      } else {
         long handle = this.mc.getWindow().getHandle();
         if (handle == 0L) {
            return false;
         } else {
            return GLFW.glfwGetWindowAttrib(handle, 131074) == 1 ? false : GLFW.glfwGetWindowAttrib(handle, 131076) == 1;
         }
      }
   }

   private MediaPlayer.PollResult fetchBestSession() throws Exception {
      Class<?> api = Class.forName("dev.redstones.mediaplayerinfo.MediaPlayerInfo");
      Field instanceField = api.getField("Instance");
      Object instance = instanceField.get(null);
      Method sessionsMethod = instance.getClass().getMethod("getMediaSessions");
      if (sessionsMethod.invoke(instance) instanceof List<?> sessions && !sessions.isEmpty()) {
         Object best = null;
         Object bestMedia = null;
         int bestPlaying = -1;
         int bestTitle = -1;
         int bestArtist = -1;
         long bestDuration = -1L;

         for (Object candidate : sessions) {
            Object candidateMedia = this.mediaObject(candidate);
            if (candidateMedia != null) {
               String title = this.safe(this.string(candidateMedia, "getTitle", ""));
               String artist = this.safe(this.string(candidateMedia, "getArtist", ""));
               if (!title.isBlank() || !artist.isBlank()) {
                  int playingScore = this.bool(candidateMedia, "getPlaying") ? 1 : 0;
                  int titleScore = title.isEmpty() ? 0 : 1;
                  int artistScore = artist.isEmpty() ? 0 : 1;
                  long durationScore = this.number(candidateMedia, "getDuration");
                  if (best == null
                     || playingScore > bestPlaying
                     || playingScore == bestPlaying && titleScore > bestTitle
                     || playingScore == bestPlaying && titleScore == bestTitle && artistScore > bestArtist
                     || playingScore == bestPlaying && titleScore == bestTitle && artistScore == bestArtist && durationScore > bestDuration) {
                     best = candidate;
                     bestMedia = candidateMedia;
                     bestPlaying = playingScore;
                     bestTitle = titleScore;
                     bestArtist = artistScore;
                     bestDuration = durationScore;
                  }
               }
            }
         }

         if (bestMedia == null) {
            return null;
         }

         return new MediaPlayer.PollResult(
            best,
            new MediaPlayer.MediaInfo(
               this.string(bestMedia, "getTitle", "No track"),
               this.string(bestMedia, "getArtist", "No artist"),
               this.number(bestMedia, "getPosition"),
               this.number(bestMedia, "getDuration"),
               this.bool(bestMedia, "getPlaying")
            )
         );
      }

      return null;
   }

   private int interpolatedPosition(MediaPlayer.MediaInfo info, int duration) {
      int position = this.safePosition(info, duration);
      if (info != null && info.playing && this.lastPositionPollMs > 0L) {
         long elapsedSeconds = (System.currentTimeMillis() - this.lastPositionPollMs) / 1000L;
         position = (int)Math.min((long)position + elapsedSeconds, (long)duration);
      }

      return position;
   }

   private Object mediaObject(Object session) {
      if (session == null) {
         return null;
      } else {
         try {
            return session.getClass().getMethod("getMedia").invoke(session);
         } catch (Throwable var3) {
            return null;
         }
      }
   }

   private void invokeSession(String method) {
      Object activeSession = this.session;
      if (activeSession != null) {
         MEDIA_EXECUTOR.execute(() -> {
            try {
               activeSession.getClass().getMethod(method).invoke(activeSession);
               this.lastPollRequestMs = 0L;
            } catch (Throwable var4) {
            }
         });
      }
   }

   private boolean hit(Click event, float centerX, float centerY, float half) {
      return event.x() >= (double)(centerX - half)
         && event.x() <= (double)(centerX + half)
         && event.y() >= (double)(centerY - half)
         && event.y() <= (double)(centerY + half);
   }

   private void drawIconCentered(String icon, float centerX, float y, float size, int color) {
      float width = Render2D.textWidth(FontType.MAINMENUSCREEN, icon, size);
      Render2D.text(FontType.MAINMENUSCREEN, icon, centerX - width * 0.5F, y, size, color);
   }

   private String formatDuration(int seconds) {
      int safe = Math.max(0, seconds);
      return twoDigits(safe / 60) + ":" + twoDigits(safe % 60);
   }

   private int safeDuration(MediaPlayer.MediaInfo info) {
      long dur = info == null ? 0L : info.duration;
      return dur <= 0L ? 1 : (int)dur;
   }

   private int safePosition(MediaPlayer.MediaInfo info, int duration) {
      long pos = info == null ? 0L : info.position;
      int position = (int)Math.max(0L, pos);
      return Math.min(position, duration);
   }

   private String safe(String text) {
      return text == null ? "" : text;
   }

   private String string(Object object, String method, String fallback) {
      try {
         Object value = object.getClass().getMethod(method).invoke(object);
         String text = value == null ? "" : value.toString();
         return text.isBlank() ? fallback : text;
      } catch (Throwable var6) {
         return fallback;
      }
   }

   private long number(Object object, String method) {
      try {
         return object.getClass().getMethod(method).invoke(object) instanceof Number number ? Math.max(0L, number.longValue()) : 0L;
      } catch (Throwable var5) {
         return 0L;
      }
   }

   private boolean bool(Object object, String method) {
      try {
         if (object.getClass().getMethod(method).invoke(object) instanceof Boolean bool && bool) {
            return true;
         }

         return false;
      } catch (Throwable var5) {
         return false;
      }
   }

   private int resolveMusicIconAlpha(boolean playing, float widgetAlpha) {
      long now = System.currentTimeMillis();
      if (playing != this.iconPulseWasPlaying) {
         this.iconPulseWasPlaying = playing;
         this.iconPulseStartedAt = now;
      }

      float targetAlpha;
      if (!playing) {
         targetAlpha = 100.0F;
      } else {
         long elapsed = Math.max(0L, now - this.iconPulseStartedAt);
         double phase = (double)(elapsed % 1800L) / 1800.0 * (Math.PI * 2);
         double wave = Math.sin(phase - (Math.PI / 2));
         targetAlpha = 150.0F + (float)(50.0 * wave);
      }

      this.smoothedIconAlpha = this.smoothedIconAlpha + (targetAlpha - this.smoothedIconAlpha) * 0.14F;
      if (Math.abs(targetAlpha - this.smoothedIconAlpha) < 0.5F) {
         this.smoothedIconAlpha = targetAlpha;
      }

      return Math.round(clamp(this.smoothedIconAlpha * widgetAlpha, 0.0F, 255.0F));
   }

   private static record MediaInfo(String title, String artist, long position, long duration, boolean playing) {
      private static MediaPlayer.MediaInfo empty() {
         return new MediaPlayer.MediaInfo("", "", 0L, 0L, false);
      }

      private boolean isEmpty() {
         return this.title == null || this.title.isBlank();
      }
   }

   private static record MediaPlayerState(MediaPlayer.MediaInfo media, float alpha, float x, float y, float width, float height) {
   }

   private static record PollResult(Object session, MediaPlayer.MediaInfo media) {
   }
}
