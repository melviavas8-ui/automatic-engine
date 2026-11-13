package daamky.ua.utils.render.ui.font;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public enum FontType {
   SEMIBOLD("semibold", "semibold"),
   BOLD("bold", "bold"),
   ICONS("icons", "icons"),
   ICONSTYPETHO("iconstypetho", "iconstypetho"),
   GUI_ICONS("guiicons", "guiicons"),
   HUD_ICONS("hudicons", "hudicons"),
   CATEGORY_ICONS("categoryicons", "categoryicons"),
   DEFAULT("default", "default"),
   REGULAR("regular", "regular"),
   TEST("test", "test"),
   REGULARNEW("regularnew", "regularnew"),
   MAINMENUSCREEN("mainmenuicons", "mainmenuicons"),
   MEDIA_PLAYER_ICONS("mediaplayericons", "icons3"),
   SWORDS("swords", "swords"),
   SPEED("speed", "speed"),
   GLAS("glas", "glas"),
   MISC_ICON("misc_icon", "misc_icon"),
   WATERMARKICONS("watermarkicons", "watermarkicons");

   private static final Map<String, String> REGISTRY = new LinkedHashMap<>();
   private final String name;
   private final String path;

   private FontType(String name, String path) {
      this.name = name;
      this.path = path;
   }

   public String fontName() {
      return this.name;
   }

   public String path() {
      return this.path;
   }

   public static Map<String, String> registry() {
      return REGISTRY;
   }

   public static String normalizeName(String name) {
      return name != null && !name.isBlank() ? name.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_') : DEFAULT.fontName();
   }

   public boolean msdf() {
      return switch (this) {
         case SEMIBOLD, BOLD, ICONS, ICONSTYPETHO, GUI_ICONS, HUD_ICONS, CATEGORY_ICONS, DEFAULT, REGULAR, TEST, REGULARNEW, MAINMENUSCREEN, MEDIA_PLAYER_ICONS, SWORDS, SPEED, GLAS, MISC_ICON, WATERMARKICONS -> true;
         default -> false;
      };
   }

   static {
      for (FontType font : values()) {
         REGISTRY.put(font.name, font.path);
      }
   }
}
