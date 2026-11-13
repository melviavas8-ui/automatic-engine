package daamky.ua.api.module;

public enum ModuleCategory {
   COMBAT("Combat", "r"),
   MOVEMENT("Movement", "j"),
   VISUAL("Visual", "a"),
   PLAYER("Player", "t"),
   DISPLAY("Display", "h"),
   WORLD("World", "w"),
   MISC("Misc", "d"),
   AUTOBUY("AutoBuy", "U");

   private final String displayName;
   private final String icon;

   private ModuleCategory(String displayName, String icon) {
      this.displayName = displayName;
      this.icon = icon;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String icon() {
      return this.icon;
   }
}
