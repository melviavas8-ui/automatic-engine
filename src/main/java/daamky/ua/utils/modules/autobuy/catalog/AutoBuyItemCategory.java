package daamky.ua.utils.modules.autobuy.catalog;

public enum AutoBuyItemCategory {
   KRUSH("Крушитель"),
   SPHERES("Сферы"),
   TALISMANS("Талисманы"),
   POTIONS("Зелья"),
   HOLYWORLD("HolyWorld"),
   MISC("Разное");

   private final String displayName;

   private AutoBuyItemCategory(String displayName) {
      this.displayName = displayName;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public boolean isHolyWorld() {
      return this == HOLYWORLD;
   }

   public boolean matchesServer(String serverMode) {
      if (serverMode != null && !serverMode.isBlank()) {
         boolean holy = serverMode.equalsIgnoreCase("HolyWorld");
         return this.isHolyWorld() == holy;
      } else {
         return !this.isHolyWorld();
      }
   }
}
