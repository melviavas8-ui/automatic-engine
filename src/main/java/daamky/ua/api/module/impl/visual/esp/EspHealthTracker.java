package daamky.ua.api.module.impl.visual.esp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;

public final class EspHealthTracker {
   private static final long SERVER_HEALTH_TIMEOUT_MS = 3000L;
   private static final Pattern HEALTH_NUMBER_PATTERN = Pattern.compile("\\d+(?:[.,]\\d+)?");
   private final Map<UUID, EspHealthTracker.ServerHealth> serverHealthByPlayer = new HashMap<>();

   public EspHealthTracker() {
   }

   public void clear() {
      this.serverHealthByPlayer.clear();
   }

   public void prune() {
      long now = System.currentTimeMillis();
      this.serverHealthByPlayer.entrySet().removeIf(entry -> now - entry.getValue().timestampMs() > 3000L);
   }

   public void capture(Entity entity, Text scoreText) {
      if (entity instanceof PlayerEntity player) {
         if (scoreText == null) {
            this.serverHealthByPlayer.remove(player.getUuid());
         } else {
            Float health = this.parseServerHealth(scoreText.getString());
            if (health != null) {
               this.serverHealthByPlayer.put(player.getUuid(), new EspHealthTracker.ServerHealth(health, System.currentTimeMillis()));
            }
         }
      }
   }

   public float resolveDisplayHealth(LivingEntity entity) {
      if (entity instanceof PlayerEntity player) {
         Float scoreboardHealth = this.resolveScoreboardHealth(player);
         if (scoreboardHealth != null) {
            return scoreboardHealth;
         }

         EspHealthTracker.ServerHealth serverHealth = this.serverHealthByPlayer.get(player.getUuid());
         if (serverHealth != null && System.currentTimeMillis() - serverHealth.timestampMs() <= 3000L) {
            return Math.max(0.0F, serverHealth.health());
         }
      }

      return Math.max(0.0F, entity.getHealth() + entity.getAbsorptionAmount());
   }

   public String formatHealth(float health) {
      int scaled = Math.max(0, Math.round(health * 10.0F));
      int whole = scaled / 10;
      int fraction = scaled % 10;
      return fraction == 0 ? Integer.toString(whole) : whole + "." + fraction;
   }

   private Float parseServerHealth(String text) {
      if (text != null && !text.isBlank()) {
         Matcher matcher = HEALTH_NUMBER_PATTERN.matcher(text);
         if (!matcher.find()) {
            return null;
         } else {
            try {
               float value = Float.parseFloat(matcher.group().replace(',', '.'));
               return Float.isFinite(value) && value >= 0.0F ? value : null;
            } catch (NumberFormatException var4) {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private Float resolveScoreboardHealth(PlayerEntity player) {
      try {
         if (player.getEntityWorld() == null) {
            return null;
         } else {
            Scoreboard scoreboard = player.getEntityWorld().getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (objective == null) {
               return null;
            } else {
               ReadableScoreboardScore score = scoreboard.getScore(player, objective);
               if (score == null) {
                  return null;
               } else {
                  float health = (float)score.getScore();
                  return this.isValidServerHealth(player, health) ? health : null;
               }
            }
         }
      } catch (Exception var6) {
         return null;
      }
   }

   private boolean isValidServerHealth(PlayerEntity player, float health) {
      if (Float.isFinite(health) && !(health < 0.0F) && !(health > 500.0F)) {
         float maxHealth = Math.max(1.0F, player.getMaxHealth() + player.getAbsorptionAmount());
         return health <= Math.max(maxHealth * 8.0F, 200.0F);
      } else {
         return false;
      }
   }

   private static record ServerHealth(float health, long timestampMs) {
   }
}
