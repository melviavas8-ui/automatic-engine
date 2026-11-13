package daamky.ua.utils.render.item;

import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.item.ItemStack;

public enum RenderItemGlintMode {
   AUTO,
   OFF,
   ON;

   private RenderItemGlintMode() {
   }

   boolean enabled(ItemStack stack, Glint foilType) {
      return this.enabled(stack, foilType != Glint.NONE);
   }

   boolean enabled(ItemStack stack, boolean foil) {
      return switch (this) {
         case AUTO -> stack != null && (stack.hasGlint() || foil);
         case OFF -> false;
         case ON -> true;
      };
   }
}
