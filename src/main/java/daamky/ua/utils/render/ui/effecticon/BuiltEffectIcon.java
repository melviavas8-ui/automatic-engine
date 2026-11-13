package daamky.ua.utils.render.ui.effecticon;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import daamky.ua.utils.render.color.ColorUtil;

public record BuiltEffectIcon(RegistryEntry<StatusEffect> effect, float x, float y, float size, int color) {
   public static final int DEFAULT_COLOR = ColorUtil.WHITE;

   public BuiltEffectIcon(RegistryEntry<StatusEffect> effect, float x, float y, float size) {
      this(effect, x, y, size, DEFAULT_COLOR);
   }

   public BuiltEffectIcon(StatusEffectInstance instance, float x, float y, float size) {
      this(instance == null ? null : instance.getEffectType(), x, y, size, DEFAULT_COLOR);
   }

   public BuiltEffectIcon(StatusEffectInstance instance, float x, float y, float size, int color) {
      this(instance == null ? null : instance.getEffectType(), x, y, size, color);
   }

   public boolean visible() {
      return this.effect != null && this.effect.hasKeyAndValue() && this.size > 0.0F && effectiveAlpha(this.color) > 0;
   }

   private static int effectiveAlpha(int color) {
      int alpha = color >>> 24 & 0xFF;
      return alpha == 0 && (color & 16777215) != 0 ? 255 : alpha;
   }
}
