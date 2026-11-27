package daamky.ua.utils.render.ui.effecticon;

import daamky.ua.utils.render.color.ColorUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public record BuiltEffectIcon(Holder<MobEffect> effect, float x, float y, float size, int color) {
   public static final int DEFAULT_COLOR = ColorUtil.WHITE;

   public BuiltEffectIcon(Holder<MobEffect> effect, float x, float y, float size) {
      this(effect, x, y, size, DEFAULT_COLOR);
   }

   public BuiltEffectIcon(MobEffectInstance instance, float x, float y, float size) {
      this(instance == null ? null : instance.getEffect(), x, y, size, DEFAULT_COLOR);
   }

   public BuiltEffectIcon(MobEffectInstance instance, float x, float y, float size, int color) {
      this(instance == null ? null : instance.getEffect(), x, y, size, color);
   }

   public boolean visible() {
      return this.effect != null && this.effect.isBound() && this.size > 0.0F && effectiveAlpha(this.color) > 0;
   }

   private static int effectiveAlpha(int color) {
      int alpha = color >>> 24 & 0xFF;
      return alpha == 0 && (color & 16777215) != 0 ? 255 : alpha;
   }
}
