package daamky.ua.api.module.impl.visual.frageffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Запоминает цели, по которым мы били, и определяет момент убийства.
 * Смерть ловится тремя способами, чтобы эффект срабатывал и на игроках в броне / на серверах,
 * которые убирают сущность сразу:
 *  1) здоровье <= 0 / deathTime > 0;
 *  2) пакет смерти (статус 3) через {@link #markDeathPacket(LivingEntity)};
 *  3) сущность исчезла из мира вскоре после нашего удара (см. {@link DeathMemory#looksLikeKill}).
 */
public final class FragEffectDeathMemoryTracker {
   private static final int MAX_MEMORIES = 12;
   private static final long MEMORY_LIFETIME_MS = 2500L;
   private static final long CONFIRMED_DEATH_GRACE_MS = 300L;
   private final Minecraft minecraft = Minecraft.getInstance();
   private final List<DeathMemory> memories = new ArrayList<>();
   public String lastReason = "";

   public FragEffectDeathMemoryTracker() {
   }

   public void remember(LivingEntity entity, boolean allowNonPlayer) {
      if (entity != null && entity != this.minecraft.player && entity.tickCount >= 2 && entity.isAlive()) {
         if (allowNonPlayer || entity instanceof Player) {
            for (DeathMemory memory : this.memories) {
               if (memory.matches(entity)) {
                  memory.refresh(entity);
                  return;
               }
            }

            while (this.memories.size() >= MAX_MEMORIES) {
               this.memories.removeFirst();
            }

            this.memories.add(new DeathMemory(entity));
         }
      }
   }

   /** Сервер прислал пакет смерти для этой сущности. */
   public void markDeathPacket(LivingEntity entity) {
      if (entity == null) {
         return;
      }
      for (DeathMemory memory : this.memories) {
         if (memory.matches(entity)) {
            memory.deathPacket = true;
            memory.entity = entity;
            return;
         }
      }
   }

   public void tick(Consumer<LivingEntity> onConfirmedDeath) {
      if (!this.memories.isEmpty()) {
         Iterator<DeathMemory> iterator = this.memories.iterator();

         while (iterator.hasNext()) {
            DeathMemory memory = iterator.next();
            if (!memory.isActive()) {
               iterator.remove();
               continue;
            }
            memory.observe(this.minecraft);
            if (!memory.triggered && memory.shouldTrigger(this.minecraft)) {
               this.lastReason = memory.deathPacket ? "death packet" : isConfirmedDead(memory.entity) ? "health/deathTime" : "entity removed";
               onConfirmedDeath.accept(memory.entity);
               memory.markTriggered();
            } else if (!memory.triggered && memory.shouldDiscard(this.minecraft)) {
               iterator.remove();
               continue;
            }

            if (!memory.isActive()) {
               iterator.remove();
            }
         }
      }
   }

   public void clear() {
      this.memories.clear();
   }

   private static boolean isConfirmedDead(LivingEntity living) {
      return DeathMemory.isConfirmedDead(living);
   }

   private static final class DeathMemory {
      private final int entityId;
      private LivingEntity entity;
      private long expiresAt;
      private long lastHitAt;
      private float lastHealth;
      private double lastDistSq;
      private boolean triggered;
      private boolean deathPacket;

      private DeathMemory(LivingEntity entity) {
         this.entity = entity;
         this.entityId = entity.getId();
         this.expiresAt = System.currentTimeMillis() + MEMORY_LIFETIME_MS;
         this.lastHitAt = System.currentTimeMillis();
         this.lastHealth = entity.getHealth() + entity.getAbsorptionAmount();
      }

      private boolean matches(LivingEntity other) {
         return other.getId() == this.entityId;
      }

      private void refresh(LivingEntity entity) {
         if (!this.triggered && !this.deathPacket && !isConfirmedDead(this.entity)) {
            this.entity = entity;
            this.expiresAt = System.currentTimeMillis() + MEMORY_LIFETIME_MS;
            this.lastHitAt = System.currentTimeMillis();
         }
      }

      /** Каждый тик запоминаем последнее известное состояние живой цели. */
      private void observe(Minecraft mc) {
         if (this.entity != null && !this.entity.isRemoved() && this.entity.getHealth() > 0.0F) {
            this.lastHealth = this.entity.getHealth() + this.entity.getAbsorptionAmount();
            if (mc.player != null) {
               this.lastDistSq = mc.player.distanceToSqr(this.entity);
            }
         }
      }

      private boolean shouldTrigger(Minecraft mc) {
         return this.deathPacket || isConfirmedDead(this.entity) || this.looksLikeKill(mc);
      }

      /**
       * Сервер убрал сущность сразу после удара (без анимации смерти).
       * Считаем это убийством, если причина удаления KILLED, либо цель была рядом и почти мертва.
       */
      private boolean looksLikeKill(Minecraft mc) {
         if (this.entity == null || mc.level == null) {
            return false;
         }
         boolean gone = this.entity.isRemoved() || mc.level.getEntity(this.entityId) == null;
         if (!gone) {
            return false;
         }
         if (System.currentTimeMillis() - this.lastHitAt > 1800L) {
            return false;
         }
         Entity.RemovalReason reason = this.entity.getRemovalReason();
         if (reason == Entity.RemovalReason.KILLED) {
            return true;
         }
         return reason != Entity.RemovalReason.CHANGED_DIMENSION && reason != Entity.RemovalReason.UNLOADED_TO_CHUNK
            && this.lastDistSq <= 49.0 && this.lastHealth <= 10.0F;
      }

      private boolean shouldDiscard(Minecraft minecraft) {
         return minecraft.level == null;
      }

      private void markTriggered() {
         this.triggered = true;
         this.expiresAt = System.currentTimeMillis() + CONFIRMED_DEATH_GRACE_MS;
      }

      private boolean isActive() {
         return System.currentTimeMillis() <= this.expiresAt;
      }

      private static boolean isConfirmedDead(LivingEntity living) {
         return living != null && (living.getHealth() <= 0.0F || living.isDeadOrDying() || living.deathTime > 0);
      }
   }
}
