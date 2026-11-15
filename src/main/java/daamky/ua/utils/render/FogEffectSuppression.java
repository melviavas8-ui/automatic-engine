package daamky.ua.utils.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import daamky.ua.api.module.impl.visual.NoRender;

public final class FogEffectSuppression {
   private static int depth;

   private FogEffectSuppression() {
   }

   public static void begin() {
      depth++;
   }

   public static void end() {
      if (depth > 0) {
         depth--;
      }
   }

   public static boolean hidesEffect(LivingEntity entity, RegistryEntry<StatusEffect> effect) {
      if (depth <= 0 || effect != StatusEffects.DARKNESS || !NoRender.isActive("Darkness")) {
         return false;
      }

      Entity camera = MinecraftClient.getInstance().getCameraEntity();
      return camera == null || entity == camera;
   }
}
