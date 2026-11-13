package daamky.ua.api.drag.impl;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.item.RenderItemOptions;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class Inventory extends HudPanel {
   private static final ItemStack[] PREVIEW = new ItemStack[]{
      Items.ENDER_PEARL.getDefaultStack(), Items.GOLDEN_APPLE.getDefaultStack(), Items.TOTEM_OF_UNDYING.getDefaultStack(), Items.SUGAR.getDefaultStack()
   };
   private final ItemStack[] inventoryStacks = new ItemStack[27];

   public Inventory() {
      super("inventory", "Inventory", 10.0F, 260.0F, 126.0F, 62.0F);

      for (int i = 0; i < this.inventoryStacks.length; i++) {
         this.inventoryStacks[i] = ItemStack.EMPTY;
      }
   }

   @Override
   public void render() {
      Inventory.InventoryState state = this.logics();
      if (state != null) {
         this.renderInventory(state);
      }
   }

   private Inventory.InventoryState logics() {
      boolean hasItems = this.inventoryItems();
      boolean preview = !hasItems && this.editPreview();
      boolean visible = hasItems || preview;
      float alpha = this.contentAlpha(visible);
      if (alpha <= 0.0F) {
         return null;
      } else {
         this.size(126.0F, 45.0F);
         return new Inventory.InventoryState(
            preview ? PREVIEW : this.inventoryStacks,
            preview ? PREVIEW.length : this.inventoryStacks.length,
            alpha,
            this.drag.x(),
            this.drag.y(),
            this.drag.width(),
            this.drag.height()
         );
      }
   }

   private void renderInventory(Inventory.InventoryState state) {
      float iconSize = 18.0F;
      float iconWidth = Render2D.textWidth(FontType.MAINMENUSCREEN, "B", iconSize);
      int index = 0;
      HudRenderCompat.background(state.x, state.y, state.width, state.height, 4.0F, 15.0F, 1.2F, ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)));
      Render2D.text(
         FontType.MAINMENUSCREEN,
         "B",
         state.x + (state.width - iconWidth) * 0.5F,
         state.y + (state.height - iconSize) * 0.5F,
         iconSize,
         ColorUtil.rgba(255, 255, 255, Math.round(36.0F * state.alpha))
      );

      for (int i = 0; i < state.count; i++) {
         ItemStack stack = state.items[i];
         int column = index % 9;
         int row = index / 9;
         RenderItem.item(
            stack, state.x + 7.0F + (float)column * 13.0F, state.y + 6.0F + (float)row * 12.5F, 8.0F, RenderItemOptions.countNoDurability(state.alpha)
         );
         index++;
      }
   }

   private boolean inventoryItems() {
      if (this.mc.player == null) {
         this.clearInventoryCache();
         return false;
      } else {
         boolean hasItems = false;

         for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = this.mc.player.getInventory().getStack(slot);
            ItemStack safeStack = stack == null ? ItemStack.EMPTY : stack;
            this.inventoryStacks[slot - 9] = safeStack;
            if (!safeStack.isEmpty()) {
               hasItems = true;
            }
         }

         return hasItems;
      }
   }

   private void clearInventoryCache() {
      for (int i = 0; i < this.inventoryStacks.length; i++) {
         this.inventoryStacks[i] = ItemStack.EMPTY;
      }
   }

   private static record InventoryState(ItemStack[] items, int count, float alpha, float x, float y, float width, float height) {
   }
}
