package daamky.ua.api.drag.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import daamky.ua.api.drag.core.ElementComponent;
import daamky.ua.api.drag.core.ElementManager;
import daamky.ua.api.drag.core.ElementScreen;
import daamky.ua.api.drag.core.HudElement;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public abstract class HudPanel implements HudElement {
   protected static final FontType TITLE_FONT = FontType.BOLD;
   protected static final FontType TEXT_FONT = FontType.SEMIBOLD;
   protected static final int TEXT_COLOR = ColorUtil.rgba(255, 255, 255, 245);
   protected static final int MUTED_COLOR = ColorUtil.rgba(183, 190, 202, 215);
   protected static final int ACCENT_COLOR = ColorUtil.rgba(127, 242, 255, 230);
   private static final float CONTENT_ANIM = 0.24F;
   private static final String[] ASCII_CHARS = new String[128];
   protected final MinecraftClient mc = MinecraftClient.getInstance();
   protected final ElementComponent drag;
   private final String elementName;
   private float animatedWidth;
   private float animatedHeight;
   private long lastFrameMs = System.currentTimeMillis();
   private final SmoothAnimation contentAnimation = new SmoothAnimation();
   private boolean enabled = true;

   protected HudPanel(String id, String title, float defaultX, float defaultY, float width, float height) {
      this.elementName = title;
      this.drag = ElementManager.getInstance().register("hud." + id, title, defaultX, defaultY).minimumSize(12.0F, 12.0F);
      this.animatedWidth = width;
      this.animatedHeight = height;
   }

   @Override
   public String elementName() {
      return this.elementName;
   }

   public void setHudVisible(boolean visible) {
      this.enabled = visible;
      this.drag.visible(visible);
   }

   protected boolean selected() {
      return this.enabled;
   }

   protected void contentVisible(boolean visible) {
      this.drag.visible(this.enabled && visible);
   }

   protected float contentAlpha(boolean targetVisible) {
      this.contentAnimation.update();
      this.contentAnimation.run(targetVisible ? 1.0 : 0.0, 0.24F, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
      float alpha = this.contentAnimation.get();
      this.contentVisible(targetVisible || alpha > 0.01F || this.contentAnimation.isAlive());
      return alpha;
   }

   protected boolean editPreview() {
      return this.mc.currentScreen instanceof ChatScreen;
   }

   protected void size(float targetWidth, float targetHeight) {
      float delta = this.deltaSeconds();
      this.animatedWidth = smooth(this.animatedWidth, targetWidth, delta, 8.0F);
      this.animatedHeight = smooth(this.animatedHeight, targetHeight, delta, 8.0F);
      if (Math.abs(this.animatedWidth - targetWidth) < 0.2F) {
         this.animatedWidth = targetWidth;
      }

      if (Math.abs(this.animatedHeight - targetHeight) < 0.2F) {
         this.animatedHeight = targetHeight;
      }

      this.drag.size((float)Math.ceil((double)this.animatedWidth), (float)Math.ceil((double)this.animatedHeight));
      this.drag.clamp(ElementScreen.current());
   }

   protected String trimToWidth(String text, FontType font, float size, float width) {
      if (text == null) {
         return "";
      } else if (Render2D.textWidth(font, text, size) <= width) {
         return text;
      } else {
         String suffix = "..";
         if (Render2D.textWidth(font, suffix, size) > width) {
            return suffix;
         } else {
            int low = 0;
            int high = text.length();
            int best = 0;

            while (low <= high) {
               int mid = low + high >>> 1;
               String candidate = text.substring(0, mid) + suffix;
               if (Render2D.textWidth(font, candidate, size) <= width) {
                  best = mid;
                  low = mid + 1;
               } else {
                  high = mid - 1;
               }
            }

            return best <= 0 ? suffix : text.substring(0, best) + suffix;
         }
      }
   }

   protected String formatDurationTicks(int ticks) {
      if (ticks < 0) {
         return "inf";
      } else {
         int totalSeconds = Math.max(0, ticks / 20);
         return totalSeconds / 60 + ":" + twoDigits(totalSeconds % 60);
      }
   }

   protected static String twoDigits(int value) {
      int safe = Math.max(0, Math.min(99, value));
      return ASCII_CHARS[48 + safe / 10] + ASCII_CHARS[48 + safe % 10];
   }

   protected static String timerText(int ticks) {
      if (ticks < 0) {
         return "**:**";
      } else {
         int totalSeconds = Math.max(0, ticks / 20);
         int minutes = Math.min(99, totalSeconds / 60);
         int seconds = totalSeconds % 60;
         return twoDigits(minutes) + ":" + twoDigits(seconds);
      }
   }

   protected static String charText(char c) {
      return c < ASCII_CHARS.length ? ASCII_CHARS[c] : Character.toString(c);
   }

   protected String shortBind(KeyBind bind) {
      return bind != null && bind.isBound()
         ? bind.getDisplayName()
            .replace("MOUSE ", "M")
            .replace("L SHIFT", "LSH")
            .replace("R SHIFT", "RSH")
            .replace("L CTRL", "LCT")
            .replace("R CTRL", "RCT")
            .replace("SPACE", "SPC")
         : "None";
   }

   protected static float smooth(float current, float target, float deltaSeconds, float speed) {
      float factor = (float)(1.0 - Math.pow(0.001, (double)(Math.max(0.0F, deltaSeconds) * speed)));
      return current + (target - current) * factor;
   }

   protected static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private float deltaSeconds() {
      long now = System.currentTimeMillis();
      float delta = Math.min(0.1F, Math.max(0.0F, (float)(now - this.lastFrameMs) / 1000.0F));
      this.lastFrameMs = now;
      return delta;
   }

   static {
      for (int i = 0; i < ASCII_CHARS.length; i++) {
         ASCII_CHARS[i] = Character.toString((char)i);
      }
   }
}
