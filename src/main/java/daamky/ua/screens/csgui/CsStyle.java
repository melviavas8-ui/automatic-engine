package daamky.ua.screens.csgui;

import java.awt.Color;
import daamky.ua.api.theme.ClientTheme;
import daamky.ua.api.theme.ThemeManager;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class CsStyle {
   public static final FontType FONT = FontType.SEMIBOLD;
   public static final FontType FONT_BOLD = FontType.BOLD;
   public static final float SCROLLBAR_W = 4.0F;
   public static final float SCROLLBAR_GAP = 5.0F;

   private CsStyle() {
   }

   public static int text(float alpha) {
      return themed(mix(theme().surface(), Color.WHITE, 0.88F), 255, alpha);
   }

   public static int muted(float alpha) {
      return themed(mix(theme().surface(), Color.WHITE, 0.62F), 255, alpha);
   }

   public static int faint(float alpha) {
      return themed(mix(theme().surface(), Color.WHITE, 0.44F), 255, alpha);
   }

   public static int hairline(float alpha) {
      return themed(theme().outline(), 22, alpha);
   }

   public static int accentAlpha(int accent, int alpha255, float alpha) {
      return ColorUtil.withAlpha(accent, a(alpha255, alpha));
   }

   private static int a(int value, float alpha) {
      return Math.max(0, Math.min(255, Math.round((float)value * alpha)));
   }

   public static int controlTop(int alpha255, float alpha) {
      return themed(theme().controlTop(), alpha255, alpha);
   }

   public static int controlBottom(int alpha255, float alpha) {
      return themed(theme().controlBottom(), alpha255, alpha);
   }

   public static int surfaceDark(int alpha255, float alpha) {
      return themed(theme().surfaceDark(), alpha255, alpha);
   }

   public static int surfaceDeep(int alpha255, float alpha) {
      return themed(theme().surfaceDeep(), alpha255, alpha);
   }

   public static int outline(int alpha255, float alpha) {
      return themed(theme().outline(), alpha255, alpha);
   }

   public static int warning(float alpha) {
      return themed(mix(theme().accentBright(), new Color(255, 194, 74), 0.58F), 255, alpha);
   }

   public static int danger(float alpha) {
      return themed(mix(theme().accent(), new Color(255, 96, 112), 0.72F), 255, alpha);
   }

   public static int white(float alpha) {
      return ColorUtil.rgba(255, 255, 255, a(255, alpha));
   }

   public static void panel(float x, float y, float w, float h, float radius, float alpha) {
      ClientTheme theme = theme();
      Render2D.rect(
         x,
         y,
         w,
         h,
         radius,
         themed(theme.surfaceDark(), 232, alpha),
         themed(theme.surface(), 230, alpha),
         themed(theme.surfaceDeep(), 240, alpha),
         themed(theme.surfaceDark(), 236, alpha)
      );
      Render2D.outline(
         x,
         y,
         w,
         h,
         radius,
         0.55F,
         themed(theme.outline(), 105, alpha),
         themed(theme.accent(), 78, alpha),
         themed(theme.accentDark(), 88, alpha),
         themed(theme.outline(), 92, alpha)
      );
      topHighlight(x, y, w, radius, a(28, alpha));
   }

   public static void sectionCard(float x, float y, float w, float h, float radius, int accent, float alpha) {
      Render2D.rect(x, y, w, h, radius, controlTop(126, alpha), surfaceDark(214, alpha), surfaceDeep(226, alpha), controlBottom(152, alpha));
      Render2D.outline(x, y, w, h, radius, 0.65F, outline(74, alpha), accentAlpha(accent, 62, alpha), accentAlpha(accent, 34, alpha), outline(58, alpha));
      float lineWidth = Math.max(20.0F, w * 0.34F);
      Render2D.rect(
         x + radius,
         y + 0.6F,
         lineWidth,
         1.2F,
         0.6F,
         accentAlpha(accent, 0, alpha),
         accentAlpha(accent, 150, alpha),
         accentAlpha(accent, 150, alpha),
         accentAlpha(accent, 0, alpha)
      );
   }

   public static void statusPill(float x, float y, float w, float h, int statusColor, float alpha) {
      float radius = h * 0.5F;
      Render2D.rect(x, y, w, h, radius, controlTop(112, alpha), controlTop(92, alpha), controlBottom(132, alpha), controlBottom(112, alpha));
      Render2D.outline(x, y, w, h, radius, 0.7F, ColorUtil.withAlpha(statusColor, a(78, alpha)));
   }

   private static ClientTheme theme() {
      return ThemeManager.current();
   }

   private static int themed(Color color, int alpha255, float alpha) {
      return ColorUtil.rgba(color.getRed(), color.getGreen(), color.getBlue(), a(alpha255, alpha));
   }

   private static Color mix(Color from, Color to, float amount) {
      float value = Math.max(0.0F, Math.min(1.0F, amount));
      return new Color(
         Math.round((float)from.getRed() + (float)(to.getRed() - from.getRed()) * value),
         Math.round((float)from.getGreen() + (float)(to.getGreen() - from.getGreen()) * value),
         Math.round((float)from.getBlue() + (float)(to.getBlue() - from.getBlue()) * value)
      );
   }

   public static void card(float x, float y, float w, float h, float radius, float alpha) {
      card(x, y, w, h, radius, alpha, false);
   }

   public static void card(float x, float y, float w, float h, float radius, float alpha, boolean emphasis) {
      int top = emphasis ? controlTop(205, alpha) : controlTop(158, alpha);
      int bottom = emphasis ? controlBottom(220, alpha) : controlBottom(185, alpha);
      Render2D.rect(x, y, w, h, radius, top, top, bottom, bottom);
      Render2D.outline(x, y, w, h, radius, 0.8F, outline(emphasis ? 92 : 58, alpha));
      topHighlight(x, y, w, radius, a(emphasis ? 34 : 24, alpha));
   }

   public static void inset(float x, float y, float w, float h, float radius, float alpha) {
      int top = surfaceDeep(205, alpha);
      int bottom = surfaceDark(178, alpha);
      Render2D.rect(x, y, w, h, radius, top, top, bottom, bottom);
      Render2D.outline(x, y, w, h, radius, 0.8F, outline(52, alpha));
      Render2D.rect(x + radius * 0.6F, y + 0.8F, w - radius * 1.2F, 1.0F, 0.0F, themed(theme().accentDark(), 36, alpha));
   }

   public static void topHighlight(float x, float y, float w, float radius, int alpha255) {
      float inset = Math.max(1.0F, radius * 0.7F);
      if (!(w - inset * 2.0F <= 0.0F)) {
         int mid = themed(theme().accentBright(), alpha255, 1.0F);
         int edge = themed(theme().accentBright(), 0, 1.0F);
         float half = (w - inset * 2.0F) * 0.5F;
         Render2D.rect(x + inset, y + 0.4F, half, 1.0F, 0.0F, edge, mid, mid, edge);
         Render2D.rect(x + inset + half, y + 0.4F, half, 1.0F, 0.0F, mid, edge, edge, mid);
      }
   }

   public static void divider(float x, float y, float w, float alpha) {
      if (!(w <= 2.0F)) {
         int mid = themed(theme().outline(), 34, alpha);
         int edge = themed(theme().accent(), 0, alpha);
         float half = w * 0.5F;
         Render2D.rect(x, y, half, 1.0F, 0.0F, edge, mid, mid, edge);
         Render2D.rect(x + half, y, half, 1.0F, 0.0F, mid, edge, edge, mid);
      }
   }

   public static void glow(float x, float y, float w, float h, float radius, int accent, float strength, float alpha) {
      if (!(strength <= 0.01F)) {
         for (int i = 3; i >= 1; i--) {
            float spread = (float)i * 2.2F;
            int col = ColorUtil.withAlpha(accent, a(Math.round(18.0F * strength / (float)i), alpha));
            Render2D.rect(x - spread, y - spread, w + spread * 2.0F, h + spread * 2.0F, radius + spread, col);
         }
      }
   }

   public static float sectionHeader(float x, float y, float w, String title, String badge, int accent, float alpha) {
      float tickH = 9.0F;
      float ty = y + 5.0F;
      Render2D.rect(
         x,
         ty,
         2.0F,
         tickH,
         1.0F,
         accentAlpha(accent, 235, alpha),
         accentAlpha(accent, 235, alpha),
         accentAlpha(accent, 90, alpha),
         accentAlpha(accent, 90, alpha)
      );
      Render2D.text(FONT_BOLD, title, x + 8.0F, ty - 0.5F, 9.5F, text(alpha));
      if (badge != null && !badge.isEmpty()) {
         float bw = Render2D.textWidth(FONT_BOLD, badge, 8.0F) + 10.0F;
         float bx = x + w - bw;
         Render2D.rect(bx, ty - 1.5F, bw, 13.0F, 6.5F, accentAlpha(accent, 34, alpha));
         Render2D.outline(bx, ty - 1.5F, bw, 13.0F, 6.5F, 0.7F, accentAlpha(accent, 90, alpha));
         float bt = Render2D.textWidth(FONT_BOLD, badge, 8.0F);
         Render2D.text(FONT_BOLD, badge, bx + (bw - bt) * 0.5F, ty + 1.5F, 8.0F, accentAlpha(accent, 255, alpha));
      }

      divider(x, y + 19.0F, w, alpha);
      return y + 24.0F;
   }

   public static void pill(float x, float y, float w, float h, boolean selected, boolean hovered, int accent, float alpha) {
      float r = h * 0.32F;
      if (selected) {
         glow(x, y, w, h, r, accent, 0.9F, alpha);
         Render2D.rect(
            x, y, w, h, r, accentAlpha(accent, 120, alpha), accentAlpha(accent, 120, alpha), accentAlpha(accent, 62, alpha), accentAlpha(accent, 62, alpha)
         );
         Render2D.outline(x, y, w, h, r, 0.9F, accentAlpha(accent, 190, alpha));
         Render2D.rect(x + w * 0.24F, y + h - 1.6F, w * 0.52F, 1.6F, 0.8F, accentAlpha(accent, 255, alpha));
      } else {
         int top = controlTop(hovered ? 132 : 76, alpha);
         int bottom = controlBottom(hovered ? 142 : 88, alpha);
         Render2D.rect(x, y, w, h, r, top, top, bottom, bottom);
         if (hovered) {
            Render2D.outline(x, y, w, h, r, 0.7F, outline(105, alpha));
         }
      }
   }

   public static void statusDot(float cx, float cy, float radius, int color, float alpha) {
      Render2D.rect(cx - radius * 2.2F, cy - radius * 2.2F, radius * 4.4F, radius * 4.4F, radius * 2.2F, ColorUtil.withAlpha(color, a(45, alpha)));
      Render2D.rect(cx - radius, cy - radius, radius * 2.0F, radius * 2.0F, radius, ColorUtil.withAlpha(color, a(255, alpha)));
   }

   public static void switchTrack(float x, float y, float w, float h, boolean on, boolean hovered, int accent, float alpha) {
      float r = h * 0.5F;
      if (on) {
         glow(x, y, w, h, r, accent, 0.7F, alpha);
         Render2D.rect(
            x, y, w, h, r, accentAlpha(accent, 210, alpha), accentAlpha(accent, 210, alpha), accentAlpha(accent, 140, alpha), accentAlpha(accent, 140, alpha)
         );
      } else {
         int top = controlTop(hovered ? 130 : 84, alpha);
         int bottom = controlBottom(hovered ? 146 : 96, alpha);
         Render2D.rect(x, y, w, h, r, top, top, bottom, bottom);
         Render2D.outline(x, y, w, h, r, 0.7F, outline(78, alpha));
      }

      float knobR = h * 0.5F - 2.0F;
      float knobCx = on ? x + w - knobR - 2.0F : x + knobR + 2.0F;
      Render2D.rect(knobCx - knobR, y + 2.0F, knobR * 2.0F, knobR * 2.0F, knobR, text(alpha));
   }

   public static float[] scrollbar(float viewX, float viewY, float viewW, float viewH, float contentH, float scroll, int accent, float alpha) {
      if (!(contentH <= viewH + 1.0F) && !(viewH <= 8.0F)) {
         float trackW = 4.0F;
         float trackX = viewX + viewW - trackW - 2.0F;
         float thumbH = Math.max(18.0F, viewH * (viewH / contentH));
         float travel = viewH - thumbH;
         float progress = Math.max(0.0F, Math.min(1.0F, scroll / Math.max(1.0F, contentH - viewH)));
         float thumbY = viewY + travel * progress;
         Render2D.rect(trackX, viewY, trackW, viewH, trackW * 0.5F, controlTop(60, alpha));
         Render2D.rect(
            trackX,
            thumbY,
            trackW,
            thumbH,
            trackW * 0.5F,
            accentAlpha(accent, 190, alpha),
            accentAlpha(accent, 190, alpha),
            accentAlpha(accent, 110, alpha),
            accentAlpha(accent, 110, alpha)
         );
         return new float[]{trackX, viewY, trackW, viewH, thumbY, thumbH};
      } else {
         return null;
      }
   }

   public static void emptyState(float x, float y, float w, float h, String title, String hint, float alpha) {
      float tw = Render2D.textWidth(FONT_BOLD, title, 10.0F);
      float cy = y + h * 0.42F;
      Render2D.text(FONT_BOLD, title, x + (w - tw) * 0.5F, cy, 10.0F, muted(alpha));
      if (hint != null && !hint.isEmpty()) {
         float hw = Render2D.textWidth(FONT, hint, 8.5F);
         Render2D.text(FONT, hint, x + (w - hw) * 0.5F, cy + 14.0F, 8.5F, faint(alpha));
      }
   }

   public static void valueBar(float x, float y, float w, float h, float fraction, int accent, float alpha) {
      fraction = Math.max(0.0F, Math.min(1.0F, fraction));
      Render2D.rect(x, y, w, h, h * 0.5F, controlTop(72, alpha));
      float fill = Math.max(h, w * fraction);
      if (fraction > 0.001F) {
         Render2D.rect(
            x,
            y,
            fill,
            h,
            h * 0.5F,
            accentAlpha(accent, 230, alpha),
            accentAlpha(accent, 160, alpha),
            accentAlpha(accent, 160, alpha),
            accentAlpha(accent, 230, alpha)
         );
      }
   }

   public static void chip(float x, float y, float size, String glyph, boolean hot, int accent, float alpha) {
      int top = hot ? accentAlpha(accent, 120, alpha) : controlTop(94, alpha);
      int bottom = hot ? accentAlpha(accent, 70, alpha) : controlBottom(104, alpha);
      Render2D.rect(x, y, size, size, size * 0.3F, top, top, bottom, bottom);
      if (hot) {
         Render2D.outline(x, y, size, size, size * 0.3F, 0.7F, accentAlpha(accent, 175, alpha));
      }

      float gw = Render2D.textWidth(FONT_BOLD, glyph, 8.0F);
      Render2D.text(FONT_BOLD, glyph, x + (size - gw) * 0.5F, y + size * 0.5F - 4.0F, 8.0F, hot ? text(alpha) : muted(alpha));
   }
}
