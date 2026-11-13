package daamky.ua.utils.render.item;

import net.minecraft.item.ItemStack;

public record BuiltRenderItem(ItemStack stack, float x, float y, float size, RenderItemOptions options, int seed) {
   public BuiltRenderItem(ItemStack stack, float x, float y, float size, RenderItemOptions options, int seed) {
      stack = stack == null ? ItemStack.EMPTY : stack;
      options = options == null ? RenderItemOptions.defaults() : options;
      this.stack = stack;
      this.x = x;
      this.y = y;
      this.size = size;
      this.options = options;
      this.seed = seed;
   }

   public BuiltRenderItem(ItemStack stack, float x, float y, float size) {
      this(stack, x, y, size, RenderItemOptions.defaults(), 0);
   }

   public boolean visible() {
      return !this.stack.isEmpty() && this.size > 0.0F && this.options.alpha() > 0.0F && this.options.color() >>> 24 != 0;
   }
}
