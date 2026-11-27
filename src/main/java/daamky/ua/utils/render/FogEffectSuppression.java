package daamky.ua.utils.render;

import daamky.ua.api.module.impl.visual.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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

   public static boolean hidesEffect(LivingEntity entity, Holder<MobEffect> effect) {
      if (depth <= 0 || effect != MobEffects.DARKNESS || !NoRender.isActive("Darkness")) {
         return false;
      }

      Entity camera = Minecraft.getInstance().getCameraEntity();
      return camera == null || entity == camera;
   }
}
