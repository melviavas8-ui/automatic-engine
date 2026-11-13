package daamky.ua.api.module.impl.visual.frageffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class FragEffectDeathMemoryTracker {
   private static final int MAX_MEMORIES = 12;
   private static final long MEMORY_LIFETIME_MS = 950L;
   private static final long CONFIRMED_DEATH_GRACE_MS = 300L;
   private final MinecraftClient minecraft = MinecraftClient.getInstance();
   private final List<FragEffectDeathMemoryTracker.DeathMemory> memories = new ArrayList<>();

   public FragEffectDeathMemoryTracker() {
   }

   public void remember(LivingEntity entity, boolean allowNonPlayer) {
      if (entity != null && entity != this.minecraft.player && entity.age >= 2 && entity.isAlive()) {
         if (allowNonPlayer || entity instanceof PlayerEntity) {
            for (FragEffectDeathMemoryTracker.DeathMemory memory : this.memories) {
               if (memory.matches(entity)) {
                  memory.refresh(entity);
                  return;
               }
            }

            while (this.memories.size() >= 12) {
               this.memories.removeFirst();
            }

            this.memories.add(new FragEffectDeathMemoryTracker.DeathMemory(entity));
         }
      }
   }

   public void tick(Consumer<LivingEntity> onConfirmedDeath) {
      if (!this.memories.isEmpty()) {
         Iterator<FragEffectDeathMemoryTracker.DeathMemory> iterator = this.memories.iterator();

         while (iterator.hasNext()) {
            FragEffectDeathMemoryTracker.DeathMemory memory = iterator.next();
            if (!memory.isActive()) {
               iterator.remove();
            } else if (memory.shouldDiscard(this.minecraft)) {
               iterator.remove();
            } else {
               if (!memory.triggered && memory.shouldTrigger()) {
                  onConfirmedDeath.accept(memory.entity);
                  memory.markTriggered();
               }

               if (!memory.isActive()) {
                  iterator.remove();
               }
            }
         }
      }
   }

   public void clear() {
      this.memories.clear();
   }

   private static final class DeathMemory {
      private final int entityId;
      private LivingEntity entity;
      private long expiresAt;
      private boolean triggered;

      private DeathMemory(LivingEntity entity) {
         this.entity = entity;
         this.entityId = entity.getId();
         this.expiresAt = System.currentTimeMillis() + 950L;
      }

      private boolean matches(LivingEntity other) {
         return other.getId() == this.entityId;
      }

      private void refresh(LivingEntity entity) {
         if (!isConfirmedDead(this.entity)) {
            this.entity = entity;
            this.triggered = false;
            this.expiresAt = System.currentTimeMillis() + 950L;
         }
      }

      private boolean shouldTrigger() {
         return isConfirmedDead(this.entity);
      }

      private boolean shouldDiscard(MinecraftClient minecraft) {
         if (isConfirmedDead(this.entity)) {
            return false;
         } else if (minecraft.world == null) {
            return true;
         } else {
            return this.entity.isRemoved() ? true : minecraft.world.getEntityById(this.entityId) == null;
         }
      }

      private void markTriggered() {
         this.triggered = true;
         this.expiresAt = System.currentTimeMillis() + 300L;
      }

      private boolean isActive() {
         return System.currentTimeMillis() <= this.expiresAt;
      }

      private static boolean isConfirmedDead(LivingEntity living) {
         return living != null && (living.getHealth() <= 0.0F || living.isDead() || living.deathTime > 0);
      }
   }
}
