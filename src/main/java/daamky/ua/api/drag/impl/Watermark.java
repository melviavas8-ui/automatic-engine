package daamky.ua.api.drag.impl;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lightning.sdk.LightningSDK;
import net.minecraft.util.math.MathHelper;
import daamky.ua.IMinecraft;
import daamky.ua.api.drag.core.ElementComponent;
import daamky.ua.api.drag.core.ElementManager;
import daamky.ua.api.drag.core.ElementScreen;
import daamky.ua.api.drag.core.HudElement;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.utils.network.Network;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class Watermark implements HudElement {
   private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
   private static final long TEXT_SWAP_DURATION_MS = 220L;
   private static final float TEXT_SWAP_OFFSET = 3.0F;
   private final ElementComponent drag = ElementManager.getInstance().register("hud.watermark", "Watermark", 10.0F, 10.0F).minimumSize(112.0F, 28.0F);
   private float displayedTps = 20.0F;
   private final Watermark.AnimatedText timeAnimation = new Watermark.AnimatedText();
   private final Watermark.AnimatedText tpsAnimation = new Watermark.AnimatedText();

   public Watermark() {
   }

   @Override
   public String elementName() {
      return "Watermark";
   }

   public void setHudVisible(boolean visible) {
      this.drag.visible(visible);
   }

   @Override
   public void render() {
      Watermark.WatermarkState state = this.logics();
      this.renderWatermark(state);
   }

   private Watermark.WatermarkState logics() {
      int ping = 0;
      if (IMinecraft.mc.player != null
         && IMinecraft.mc.getNetworkHandler() != null
         && IMinecraft.mc.getNetworkHandler().getPlayerListEntry(IMinecraft.mc.player.getUuid()) != null) {
         ping = Math.max(0, IMinecraft.mc.getNetworkHandler().getPlayerListEntry(IMinecraft.mc.player.getUuid()).getLatency());
      }

      String username = LightningSDK.get("Username");
      if (username == null || username.isBlank()) {
         username = "Kolodeev";
      }

      String serverText = "mc.reallyworld.ru";
      if (IMinecraft.mc.getCurrentServerEntry() != null
         && IMinecraft.mc.getCurrentServerEntry().address != null
         && !IMinecraft.mc.getCurrentServerEntry().address.isBlank()) {
         serverText = IMinecraft.mc.getCurrentServerEntry().address;
      }

      String pingText = "Ping " + ping;
      float targetTps = MathHelper.clamp(Math.max(Network.getTPS(), 0.1F), 0.0F, 20.0F);
      this.displayedTps = MathHelper.lerp(0.12F, this.displayedTps, targetTps);
      String tpsText = "Tps " + Math.round(this.displayedTps);
      String fpsText = "Fps " + IMinecraft.mc.getCurrentFps();
      String timeText = LocalTime.now().format(TIME_FORMATTER);
      this.tpsAnimation.update(tpsText);
      this.timeAnimation.update(timeText);
      float usernameWidth = Render2D.textWidth(FontType.BOLD, username, 6.0F);
      float fpsTextWidth = Render2D.textWidth(FontType.BOLD, fpsText, 6.0F);
      float timeTextWidth = this.timeAnimation.textWidth(FontType.BOLD, 6.0F);
      float serverTextWidth = Render2D.textWidth(FontType.BOLD, serverText, 6.0F);
      float pingTextWidth = Render2D.textWidth(FontType.BOLD, pingText, 6.0F);
      float tpsTextWidth = this.tpsAnimation.textWidth(FontType.BOLD, 6.0F);
      float topUserIconX = 23.0F;
      float topTextOffset = 24.0F;
      float topSectionGap = 3.0F;
      float topFpsIconX = topUserIconX + topTextOffset + usernameWidth + topSectionGap;
      float topTimeIconX = topFpsIconX + topTextOffset + fpsTextWidth + topSectionGap;
      float topWidth = Math.max(60.0F, topTimeIconX + timeTextWidth + 6.0F);
      float pingIconX = 24.0F + serverTextWidth + 1.0F;
      float tpsIconX = pingIconX + 26.0F + pingTextWidth + 2.0F;
      float bottomWidth = Math.max(154.0F, tpsIconX + 26.0F + tpsTextWidth + 1.0F);
      float width = Math.max(20.0F + topWidth, bottomWidth);
      float height = 42.0F;
      this.drag.size(width, height);
      this.drag.clamp(ElementScreen.current());
      float x = this.drag.x();
      float y = this.drag.y();
      float radius = 4.0F;
      float xc = x - 2.0F;
      return new Watermark.WatermarkState(
         x,
         y,
         xc,
         radius,
         topWidth,
         topUserIconX,
         topFpsIconX,
         topTimeIconX,
         bottomWidth,
         pingIconX,
         tpsIconX,
         username,
         serverText,
         pingText,
         tpsText,
         fpsText,
         timeText
      );
   }

   private void renderWatermark(Watermark.WatermarkState state) {
      Hud hud = Hud.getInstance();
      int hudColor = hud != null ? hud.iconColor() : ColorUtil.rgba(247, 133, 255, 255);
      int fadedHudColor = ColorUtil.multAlpha(hudColor, 255.0F);
      int background = ColorUtil.rgba(0, 0, 0, 255);
      int purple = ColorUtil.rgba(247, 133, 255, 255);
      int white = ColorUtil.rgba(255, 255, 255, 255);
      float topIconY = state.y + 6.5F;
      float topDotY = state.y + 9.0F;
      float topTextY = state.y + 7.0F;
      float bottomGlowY = state.y + 22.0F;
      float bottomIconY = state.y + 28.0F;
      float bottomDotY = state.y + 30.5F;
      float bottomTextY = state.y + 28.0F;
      float iconOffset = 7.0F;
      float dotOffset = 18.0F;
      float textOffset = 24.0F;
      float bottomIconOffset = 6.5F;
      HudRenderCompat.background(state.x, state.y, 20.0F, 20.0F, state.radius, 15.0F, 1.2F, background);
      HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", state.x - 1.0F, state.y - 1.0F, 22.0F, 22.0F, 0.0F, fadedHudColor);
      Render2D.text(FontType.MAINMENUSCREEN, "f", state.x + 3.0F, state.y + 4.0F, 14.0F, fadedHudColor);
      HudRenderCompat.background(state.x + 22.0F, state.y, state.topWidth, 20.0F, state.radius, 15.0F, 1.2F, background);
      HudRenderCompat.background(state.x, state.y + 22.0F, state.bottomWidth, 20.0F, state.radius, 15.0F, 1.2F, background);
      HudRenderCompat.glow(
         "daamky:textures/particles/ghost-glow.png", state.xc + state.topUserIconX, state.y, 22.0F, 22.0F, 0.0F, ColorUtil.rgba(247, 133, 255, 130)
      );
      Render2D.text(FontType.MAINMENUSCREEN, "t", state.xc + state.topUserIconX + iconOffset, topIconY, 9.0F, fadedHudColor);
      Render2D.rect(state.xc + state.topUserIconX + dotOffset, topDotY - 0.5F, 4.0F, 4.0F, 2.0F, white);
      Render2D.text(FontType.BOLD, state.username, state.xc + state.topUserIconX + textOffset, topTextY, 6.0F, white);
      HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", state.xc + state.topFpsIconX, state.y, 22.0F, 22.0F, 0.0F, fadedHudColor);
      Render2D.text(FontType.MAINMENUSCREEN, "b", state.xc + state.topFpsIconX + iconOffset, topIconY, 9.0F, fadedHudColor);
      Render2D.rect(state.xc + state.topFpsIconX + dotOffset, topDotY - 0.5F, 4.0F, 4.0F, 4.0F, white);
      Render2D.text(FontType.BOLD, state.fpsText, state.xc + state.topFpsIconX + textOffset, topTextY, 6.0F, white);
      HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", state.xc + state.topTimeIconX, state.y, 22.0F, 22.0F, 0.0F, fadedHudColor);
      Render2D.text(FontType.MAINMENUSCREEN, "h", state.xc + state.topTimeIconX + iconOffset, topIconY, 9.0F, fadedHudColor);
      Render2D.rect(state.xc + state.topTimeIconX + dotOffset, topDotY - 0.5F, 4.0F, 4.0F, 4.0F, white);
      this.renderAnimatedText(FontType.BOLD, this.timeAnimation, state.xc + state.topTimeIconX + textOffset, topTextY, 6.0F, white);
      HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", state.xc + 0.5F, bottomGlowY, 22.0F, 22.0F, 0.0F, fadedHudColor);
      Render2D.text(FontType.MAINMENUSCREEN, "x", state.xc + 0.5F + bottomIconOffset, bottomIconY, 9.0F, fadedHudColor);
      Render2D.rect(state.xc + 0.5F + dotOffset, bottomDotY - 0.5F, 4.0F, 4.0F, 4.0F, white);
      Render2D.text(FontType.BOLD, state.serverText, state.xc + 0.5F + textOffset, bottomTextY, 6.0F, white);
      HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", state.xc + state.pingIconX, bottomGlowY, 22.0F, 22.0F, 0.0F, fadedHudColor);
      Render2D.text(FontType.MAINMENUSCREEN, "y", state.xc + state.pingIconX + bottomIconOffset, bottomIconY, 8.5F, fadedHudColor);
      Render2D.rect(state.xc + state.pingIconX + dotOffset, bottomDotY - 0.5F, 4.0F, 4.0F, 4.0F, white);
      Render2D.text(FontType.BOLD, state.pingText, state.xc + state.pingIconX + textOffset, bottomTextY, 6.0F, white);
      HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", state.xc + state.tpsIconX, bottomGlowY, 22.0F, 22.0F, 0.0F, fadedHudColor);
      Render2D.text(FontType.MAINMENUSCREEN, "z", state.xc + state.tpsIconX + bottomIconOffset, bottomIconY + 1.0F, 7.0F, fadedHudColor);
      Render2D.rect(state.xc + state.tpsIconX + dotOffset, bottomDotY - 0.5F, 4.0F, 4.0F, 4.0F, white);
      this.renderAnimatedText(FontType.BOLD, this.tpsAnimation, state.xc + state.tpsIconX + textOffset, bottomTextY, 6.0F, white);
   }

   private void renderAnimatedText(FontType font, Watermark.AnimatedText animation, float x, float y, float size, int color) {
      float progress = animation.progress();
      if (progress >= 1.0F) {
         Render2D.text(font, animation.currentText(), x, y, size, color);
      } else {
         float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
         float inverse = 1.0F - eased;
         int fromColor = ColorUtil.withAlpha(color, Math.round((float)ColorUtil.getAlpha(color) * inverse));
         int toColor = ColorUtil.withAlpha(color, Math.round((float)ColorUtil.getAlpha(color) * eased));
         Render2D.text(font, animation.previousText(), x, y - 3.0F * eased, size, fromColor);
         Render2D.text(font, animation.currentText(), x, y + 3.0F * inverse, size, toColor);
      }
   }

   private static final class AnimatedText {
      private String currentText = "";
      private String previousText = "";
      private long animationStartedAt;

      private AnimatedText() {
      }

      private void update(String nextText) {
         if (this.currentText.isEmpty()) {
            this.currentText = nextText;
            this.previousText = nextText;
         } else {
            if (!this.currentText.equals(nextText)) {
               this.previousText = this.currentText;
               this.currentText = nextText;
               this.animationStartedAt = System.currentTimeMillis();
            }
         }
      }

      private float textWidth(FontType font, float size) {
         String oldText = this.previousText.isEmpty() ? this.currentText : this.previousText;
         return Math.max(Render2D.textWidth(font, oldText, size), Render2D.textWidth(font, this.currentText, size));
      }

      private float progress() {
         if (this.animationStartedAt != 0L && !this.previousText.equals(this.currentText)) {
            float progress = (float)(System.currentTimeMillis() - this.animationStartedAt) / 220.0F;
            if (progress >= 1.0F) {
               this.previousText = this.currentText;
               this.animationStartedAt = 0L;
               return 1.0F;
            } else {
               return MathHelper.clamp(progress, 0.0F, 1.0F);
            }
         } else {
            return 1.0F;
         }
      }

      private String currentText() {
         return this.currentText;
      }

      private String previousText() {
         return this.previousText;
      }
   }

   private static record WatermarkState(
      float x,
      float y,
      float xc,
      float radius,
      float topWidth,
      float topUserIconX,
      float topFpsIconX,
      float topTimeIconX,
      float bottomWidth,
      float pingIconX,
      float tpsIconX,
      String username,
      String serverText,
      String pingText,
      String tpsText,
      String fpsText,
      String timeText
   ) {
   }
}
