package daamky.ua.utils.render.item;

import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.world.item.ItemStack;

public enum RenderItemGlintMode {
   AUTO,
   OFF,
   ON;

   private RenderItemGlintMode() {
   }

   boolean enabled(ItemStack stack, FoilType foilType) {
      return this.enabled(stack, foilType != FoilType.NONE);
   }

   boolean enabled(ItemStack stack, boolean foil) {
      return switch (this) {
         case AUTO -> stack != null && (stack.hasFoil() || foil);
         case OFF -> false;
         case ON -> true;
      };
   }
}
