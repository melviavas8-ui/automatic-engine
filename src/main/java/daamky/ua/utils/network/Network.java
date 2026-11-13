package daamky.ua.utils.network;

import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.MathHelper;
import daamky.ua.mixin.accessor.BossHealthOverlayAccessor;

public final class Network {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static long lastPvpMs;
   private static long lastTpsPacketNs;
   private static float tps = 20.0F;
   private static String server = "Vanilla";

   private Network() {
   }

   public static void tick() {
      server = detectServer();
      updatePvpState();
   }

   public static void handleTimePacket() {
      long now = System.nanoTime();
      if (lastTpsPacketNs != 0L) {
         float raw = 20.0F * (1.0E9F / (float)(now - lastTpsPacketNs));
         tps = MathHelper.clamp(raw, 0.0F, 20.0F);
      }

      lastTpsPacketNs = now;
   }

   public static float getTPS() {
      return tps;
   }

   public static String getServer() {
      server = detectServer();
      return server;
   }

   public static boolean isPvp() {
      updatePvpState();
      return System.currentTimeMillis() - lastPvpMs <= 500L;
   }

   public static boolean isCopyTime() {
      String current = getServer();
      return "CopyTime".equals(current) || "SpookyTime".equals(current) || "FunTime".equals(current);
   }

   public static boolean isFunTime() {
      return "FunTime".equals(getServer());
   }

   public static boolean isReallyWorld() {
      return "ReallyWorld".equals(getServer());
   }

   public static boolean isGulPvP() {
      return "GulPvP".equals(getServer());
   }

   public static boolean isHolyWorld() {
      return "HolyWorld".equals(getServer());
   }

   public static boolean isSpookyTime() {
      return "SpookyTime".equals(getServer());
   }

   public static boolean isVanilla() {
      return "Vanilla".equals(getServer());
   }

   public static float getResolvedHealth(LivingEntity entity, boolean includeAbsorption) {
      if (entity == null) {
         return 0.0F;
      } else {
         float health = Math.max(0.0F, entity.getHealth());
         return includeAbsorption ? health + Math.max(0.0F, entity.getAbsorptionAmount()) : health;
      }
   }

   public static String formatHealthValue(float hp) {
      if (hp >= 100.0F) {
         return Integer.toString((int)hp);
      } else {
         return hp >= 10.0F ? String.format(Locale.ROOT, "%.1f", hp) : String.format(Locale.ROOT, "%.2f", hp);
      }
   }

   public static int getAnarchyMode() {
      if (MC.world == null) {
         return -1;
      } else {
         Scoreboard scoreboard = MC.world.getScoreboard();
         ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (objective == null) {
            return -1;
         } else {
            boolean funTimeHeader = "FunTime".equals(getServer());
            String header = clean(objective.getDisplayName().getString());
            int fromHeader = extractAnarchyNumber(header, funTimeHeader);
            if (fromHeader != -1) {
               return fromHeader;
            } else {
               for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
                  String row = clean(Team.decorateName(scoreboard.getScoreHolderTeam(entry.owner()), entry.name()).getString());
                  int value = extractAnarchyNumber(row, false);
                  if (value != -1) {
                     return value;
                  }
               }

               return -1;
            }
         }
      }
   }

   private static int extractAnarchyNumber(String value, boolean allowDashFallback) {
      if (value != null && !value.isEmpty()) {
         int fromHash = extractNumberAfter(value, "#");
         if (fromHash != -1) {
            return fromHash;
         } else {
            String lower = value.toLowerCase(Locale.ROOT);
            boolean looksLikeAnarchy = lower.contains("анарх") || lower.contains("anarchy") || lower.contains("гриф") || lower.contains("grief");
            if (!looksLikeAnarchy && !allowDashFallback) {
               return -1;
            } else {
               int fromDash = extractNumberAfterLast(value, "-");
               return fromDash != -1 ? fromDash : extractNumberAfterLast(value, ":");
            }
         }
      } else {
         return -1;
      }
   }

   private static String detectServer() {
      if (MC.getNetworkHandler() != null && MC.getNetworkHandler().getServerInfo() != null) {
         String address = safeLower(MC.getNetworkHandler().getServerInfo().address);
         String brand = safeLower(MC.getNetworkHandler().getBrand());
         String normalizedBrand = normalizeServerToken(MC.getNetworkHandler().getBrand());
         if (brand.contains("botfilter") || normalizedBrand.contains("botfilter")) {
            return "FunTime";
         } else if (address.contains("spooky")
            || address.contains("pookie")
            || brand.contains("spooky")
            || brand.contains("pookie")
            || normalizedBrand.contains("spookycore")
            || normalizedBrand.contains("spookytime")
            || normalizedBrand.contains("pookietime")) {
            return "SpookyTime";
         } else if (address.contains("funtime") || address.contains("skytime") || address.contains("space-times") || address.contains("funsky")) {
            return "CopyTime";
         } else if (brand.contains("holyworld") || normalizedBrand.contains("holyworld") || brand.contains("vk.com/idwok")) {
            return "HolyWorld";
         } else if (address.contains("reallyworld")) {
            return "ReallyWorld";
         } else {
            return address.contains("gulpvp") ? "GulPvP" : "Vanilla";
         }
      } else {
         return "Vanilla";
      }
   }

   private static void updatePvpState() {
      if (MC.inGameHud != null && MC.inGameHud.getBossBarHud() != null) {
         Map<?, ClientBossBar> events = ((BossHealthOverlayAccessor)MC.inGameHud.getBossBarHud()).daamky$getEvents();

         for (ClientBossBar event : events.values()) {
            String name = safeLower(event.getName().getString());
            if (name.contains("pvp") || name.contains("пвп")) {
               lastPvpMs = System.currentTimeMillis();
               return;
            }
         }
      }
   }

   private static int extractNumberAfter(String value, String marker) {
      int markerIndex = value.indexOf(marker);
      return markerIndex == -1 ? -1 : extractNumberFrom(value, markerIndex + marker.length());
   }

   private static int extractNumberAfterLast(String value, String marker) {
      int markerIndex = value.lastIndexOf(marker);
      return markerIndex == -1 ? -1 : extractNumberFrom(value, markerIndex + marker.length());
   }

   private static int extractNumberFrom(String value, int start) {
      StringBuilder digits = new StringBuilder();

      for (int i = start; i < value.length(); i++) {
         char c = value.charAt(i);
         if (Character.isDigit(c)) {
            digits.append(c);
         } else if (!digits.isEmpty()) {
            break;
         }
      }

      if (digits.isEmpty()) {
         return -1;
      } else {
         try {
            return Integer.parseInt(digits.toString());
         } catch (NumberFormatException var5) {
            return -1;
         }
      }
   }

   private static String normalizeServerToken(String value) {
      if (value != null && !value.isEmpty()) {
         StringBuilder out = new StringBuilder(value.length());
         boolean skip = false;

         for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (skip) {
               skip = false;
            } else if (c == 167) {
               skip = true;
            } else if (c != 194 && !Character.isWhitespace(c)) {
               out.append(Character.toLowerCase(c));
            }
         }

         return out.toString();
      } else {
         return "";
      }
   }

   private static String clean(String value) {
      return normalizeServerToken(value).replace(' ', ' ').trim();
   }

   private static String safeLower(String value) {
      return value == null ? "" : value.toLowerCase(Locale.ROOT);
   }
}
