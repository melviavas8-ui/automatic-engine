package daamky.ua.utils.render.ui.font;

import java.awt.Font;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

final class FontManager implements AutoCloseable {
   private static final int MAX_STRIKES = 96;
   private static final String FONT_ROOT = "/assets/daamky/ui/fonts/";
   private static final String DEFAULT_NAME = "default";
   private final Map<String, FontFamily> families = new LinkedHashMap<>();
   private final List<Font> unicodeFallbacks = new ArrayList<>();
   private final Map<String, Font> lazyFonts = new HashMap<>();
   private final Map<FontManager.StrikeKey, FontStrike> strikes = new LinkedHashMap<FontManager.StrikeKey, FontStrike>(64, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<FontManager.StrikeKey, FontStrike> eldest) {
         if (this.size() <= 96) {
            return false;
         } else {
            eldest.getValue().close();
            return true;
         }
      }
   };
   private Font bundledSansFallback;

   FontManager() {
      this.load();
   }

   FontStrike strike(String name, float size) {
      String normalizedName = normalizeName(name);
      FontFamily family = this.family(normalizedName);
      float normalizedSize = normalizeSize(size);
      FontManager.StrikeKey key = new FontManager.StrikeKey(family.name(), normalizedSize);
      return this.strikes.computeIfAbsent(key, ignored -> new FontStrike(family, normalizedSize));
   }

   FontFamily family(String name) {
      FontFamily family = this.families.get(normalizeName(name));
      return family != null ? family : this.families.get("default");
   }

   private void load() {
      Font segoe = this.loadFont("SegoeProDisplay-Semibold.ttf", new Font("Segoe UI Semibold", 0, 1));
      Font semiBold = this.loadFont("semi_bold.otf", segoe);
      Font openSansBold = this.loadFont("open-sans.bold.ttf", new Font("SansSerif", 1, 1));
      Font openSansSemiBold = this.loadFont("open-sans.semibold.ttf", openSansBold);
      Font bold = this.loadFont("bold.otf", openSansBold.deriveFont(1, 1.0F));
      Font medium = this.loadFont("medium.otf", semiBold);
      Font zenter = this.loadFont("ZenterSPDemo-Black.otf", segoe);
      Font icons = this.loadFont("icons.ttf", segoe);
      Font notoSans = this.loadFont("NotoSans-Regular.ttf", segoe);
      Font notoSymbols = this.loadFont("NotoSansSymbols-Regular.ttf", segoe);
      Font notoSymbols2 = this.loadFont("NotoSansSymbols2-Regular.ttf", segoe);
      Font notoArabic = this.loadFont("NotoNaskhArabic-Regular.ttf", segoe);
      this.bundledSansFallback = notoSans;
      this.loadSystemFallbacks(icons, notoSymbols, notoSymbols2, notoArabic, segoe, notoSans);
      this.register("default", segoe);
      this.register("segoe", segoe);
      this.register("segoe_semibold", segoe);
      this.register("semi_bold", semiBold);
      this.register("semibold", semiBold);
      this.register("open_sans_bold", openSansBold);
      this.register("opensans_bold", openSansBold);
      this.register("open_sans_semibold", openSansSemiBold);
      this.register("opensans_semibold", openSansSemiBold);
      this.register("open_sans", openSansSemiBold);
      this.register("opensans", openSansSemiBold);
      this.register("bold", bold);
      this.register("bold_otf", bold);
      this.register("medium", medium);
      this.register("medium_otf", medium);
      this.register("zenter", zenter);
      this.register("zenter_black", zenter);
      this.register("noto", notoSans);
      this.register("noto_sans", notoSans);
      this.register("noto_symbols", notoSymbols);
      this.register("noto_symbols_1", notoSymbols);
      this.register("noto_symbols_2", notoSymbols2);
      this.register("noto_symbols2", notoSymbols2);
      this.register("noto_arabic", notoArabic);
      this.register("arabic", notoArabic);
      this.register("noto_cjk_sc", notoSans);
      this.register("noto_chinese", notoSans);
      this.register("chinese", notoSans);
      this.register("noto_cjk_kr", notoSans);
      this.register("noto_korean", notoSans);
      this.register("korean", notoSans);
      this.register("noto_cjk_jp", notoSans);
      this.register("noto_japanese", notoSans);
      this.register("japanese", notoSans);
      this.register("icons", icons);
      this.register("icon", icons);
      this.register(FontType.SEMIBOLD, semiBold);
      this.register(FontType.BOLD, bold);
      this.register(FontType.ICONS, icons);
      this.register(FontType.ICONSTYPETHO, icons);
      this.register(FontType.GUI_ICONS, icons);
      this.register(FontType.HUD_ICONS, icons);
      this.register(FontType.CATEGORY_ICONS, icons);
      this.register(FontType.DEFAULT, segoe);
      this.register(FontType.REGULAR, notoSans);
      this.register(FontType.TEST, notoSans);
      this.register(FontType.REGULARNEW, notoSans);
      this.register(FontType.MAINMENUSCREEN, icons);
      this.register(FontType.MEDIA_PLAYER_ICONS, icons);
      this.register(FontType.SWORDS, icons);
      this.register(FontType.SPEED, icons);
      this.register(FontType.GLAS, icons);
      this.register(FontType.MISC_ICON, icons);
      this.register(FontType.WATERMARKICONS, icons);
   }

   private void register(String name, Font primary) {
      this.families.put(normalizeName(name), new FontFamily(normalizeName(name), primary, this.unicodeFallbacks, this::lazyUnicodeFallbacks));
   }

   private void register(FontType font, Font primary) {
      this.register(font.fontName(), primary);
      if (!font.path().equals(font.fontName())) {
         this.register(font.path(), primary);
      }
   }

   private Font loadFont(String fileName, Font fallback) {
      try {
         Font var4;
         try (InputStream input = FontManager.class.getResourceAsStream("/assets/daamky/ui/fonts/" + fileName)) {
            if (input == null) {
               return fallback;
            }

            var4 = Font.createFont(0, input).deriveFont(0, 1.0F);
         }

         return var4;
      } catch (Exception var8) {
         return fallback;
      }
   }

   private void loadSystemFallbacks(Font... bundledFallbacks) {
      this.unicodeFallbacks.clear();

      for (Font font : bundledFallbacks) {
         this.addFallback(font);
      }

      this.addFallback(new Font("SansSerif", 0, 1));
   }

   private void addFallback(Font font) {
      if (font != null) {
         String key = FontFamily.fontKey(font);

         for (Font fallback : this.unicodeFallbacks) {
            if (FontFamily.fontKey(fallback).equals(key)) {
               return;
            }
         }

         this.unicodeFallbacks.add(font);
      }
   }

   private List<Font> lazyUnicodeFallbacks(int codePoint) {
      if (isHangul(codePoint)) {
         return Collections.singletonList(this.lazyFont("NotoSansCJKkr-Regular.otf"));
      } else if (isJapanese(codePoint)) {
         return Collections.singletonList(this.lazyFont("NotoSansCJKjp-Regular.otf"));
      } else {
         return isCjk(codePoint) ? Collections.singletonList(this.lazyFont("NotoSansCJKsc-Regular.otf")) : List.of();
      }
   }

   private Font lazyFont(String fileName) {
      return this.lazyFonts
         .computeIfAbsent(fileName, name -> this.loadFont(name, this.bundledSansFallback != null ? this.bundledSansFallback : new Font("SansSerif", 0, 1)));
   }

   private static float normalizeSize(float size) {
      return Math.max(1.0F, Math.min(512.0F, (float)Math.round(size * 4.0F) / 4.0F));
   }

   private static String normalizeName(String name) {
      return name != null && !name.isBlank() ? name.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_') : "default";
   }

   private static boolean isHangul(int codePoint) {
      return codePoint >= 4352 && codePoint <= 4607
         || codePoint >= 12592 && codePoint <= 12687
         || codePoint >= 43360 && codePoint <= 43391
         || codePoint >= 44032 && codePoint <= 55215;
   }

   private static boolean isJapanese(int codePoint) {
      return codePoint >= 12352 && codePoint <= 12543 || codePoint >= 12784 && codePoint <= 12799;
   }

   private static boolean isCjk(int codePoint) {
      return codePoint >= 13312 && codePoint <= 19903
         || codePoint >= 19968 && codePoint <= 40959
         || codePoint >= 63744 && codePoint <= 64255
         || codePoint >= 131072 && codePoint <= 195103;
   }

   @Override
   public void close() {
      for (FontStrike strike : this.strikes.values()) {
         strike.close();
      }

      this.strikes.clear();
      this.families.clear();
      this.unicodeFallbacks.clear();
      this.lazyFonts.clear();
   }

   private static record StrikeKey(String name, float size) {
   }
}
