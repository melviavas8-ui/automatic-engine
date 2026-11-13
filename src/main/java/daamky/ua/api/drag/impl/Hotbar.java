package daamky.ua.api.drag.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import daamky.ua.api.drag.core.HudElement;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.item.RenderItemOptions;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class Hotbar implements HudElement {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final RenderItemOptions ITEM_OPTIONS = RenderItemOptions.noDecorations(1.0F);
   private static final float CHAT_SCREEN_ALPHA = 0.82F;
   private static float animatedSelectedX;
   private static float lastHotbarX = Float.NaN;
   private static float lastHotbarY = Float.NaN;
   private static long lastAnimationMs;
   private static float offhandProgress;
   private static long lastOffhandAnimationMs;
   private static ItemStack lastOffhandStack = ItemStack.EMPTY;

   public Hotbar() {
   }

   @Override
   public String elementName() {
      return "Hotbar";
   }

   @Override
   public void render() {
      Hotbar.HotbarState state = this.logics();
      if (state != null) {
         this.renderHotbar(state);
      }
   }

   private Hotbar.HotbarState logics() {
      if (mc.player != null && mc.getWindow() != null) {
         int sw = mc.getWindow().getScaledWidth();
         int sh = mc.getWindow().getScaledHeight();
         float slot = 17.5F;
         float gap = 2.5F;
         float width = slot * 9.0F + gap * 8.0F;
         float x = (float)sw / 2.0F - width / 2.0F;
         float y = (float)(sh - 21);
         float offhandX = x - gap - slot;
         ItemStack offhandStack = mc.player.getOffHandStack();
         if (!offhandStack.isEmpty()) {
            lastOffhandStack = offhandStack.copy();
         }

         float offhandAlpha = animatedOffhandProgress(!offhandStack.isEmpty());
         float offhandArea = (slot + gap) * offhandAlpha;
         float fullX = x - offhandArea;
         float fullWidth = width + offhandArea;
         float paddingX = 4.0F;
         float paddingY = 3.0F;
         float selectedX = animatedSelectedX(x, y, slot, gap);
         ItemStack renderedOffhand = lastOffhandStack.copy();
         if (offhandAlpha <= 0.01F && offhandStack.isEmpty()) {
            lastOffhandStack = ItemStack.EMPTY;
         }

         return new Hotbar.HotbarState(x, y, slot, gap, fullX, fullWidth, paddingX, paddingY, selectedX, offhandX, offhandAlpha, renderedOffhand);
      } else {
         return null;
      }
   }

   private void renderHotbar(Hotbar.HotbarState state) {
      float alpha = hotbarAlpha();
      int background = ColorUtil.rgba(0, 0, 0, 255, alpha);
      int selected = ColorUtil.rgba(128, 128, 128, 55, alpha);
      RenderItemOptions itemOptions = ITEM_OPTIONS.alpha(alpha);
      HudRenderCompat.background(
         state.fullX - state.paddingX,
         state.y - state.paddingY,
         state.fullWidth + state.paddingX * 2.0F,
         state.slot + state.paddingY * 2.0F,
         6.0F,
         18.0F,
         1.2F,
         background
      );
      Render2D.rect(state.selectedX, state.y, state.slot, state.slot, 4.0F, selected);

      for (int i = 0; i < 9; i++) {
         float sx = state.x + (float)i * (state.slot + state.gap);
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isEmpty()) {
            renderSlotNumber(i, sx, state.y, state.slot, alpha);
         } else {
            RenderItem.item(stack, sx + 0.75F, state.y + 0.75F, 16.0F, itemOptions);
         }
      }

      if (state.offhandAlpha > 0.01F && !state.offhandStack.isEmpty()) {
         RenderItem.item(state.offhandStack, state.offhandX + 0.75F, state.y + 0.75F, 16.0F, ITEM_OPTIONS.alpha(state.offhandAlpha * alpha));
      }

      for (int ix = 0; ix < 9; ix++) {
         float sx = state.x + (float)ix * (state.slot + state.gap);
         ItemStack stack = mc.player.getInventory().getStack(ix);
         if (!stack.isEmpty()) {
            renderItemDecorations(stack, sx + 0.75F, state.y + 0.75F, 16.0F, alpha);
         }
      }

      if (state.offhandAlpha > 0.01F && !state.offhandStack.isEmpty()) {
         renderItemDecorations(state.offhandStack, state.offhandX + 0.75F, state.y + 0.75F, 16.0F, state.offhandAlpha * alpha);
      }
   }

   private static float hotbarAlpha() {
      return mc.currentScreen instanceof ChatScreen ? 0.82F : 1.0F;
   }

   private static void renderItemDecorations(ItemStack stack, float x, float y, float size, float alpha) {
      if (!stack.isEmpty() && !(alpha <= 0.01F)) {
         renderCooldownOverlay(stack, x, y, size, alpha);
         renderDurabilityBar(stack, x, y, size, alpha);
         renderStackCount(stack, x, y, size, alpha);
      }
   }

   private static void renderCooldownOverlay(ItemStack stack, float x, float y, float size, float alpha) {
      if (mc.player != null) {
         float cooldown = mc.player.getItemCooldownManager().getCooldownProgress(stack, 0.0F);
         if (!(cooldown <= 0.0F)) {
            float overlayHeight = size * Math.max(0.0F, Math.min(1.0F, cooldown));
            Render2D.rect(x, y + size - overlayHeight, size, overlayHeight, 1.0F, ColorUtil.rgba(0, 0, 0, Math.round(145.0F * alpha)));
         }
      }
   }

   private static void renderDurabilityBar(ItemStack stack, float x, float y, float size, float alpha) {
      if (stack.isItemBarVisible()) {
         float barWidth = Math.max(8.0F, size * 0.78F);
         float barHeight = Math.max(1.5F, size * 0.07F);
         float barX = x + (size - barWidth) * 0.5F;
         float barY = y + size - barHeight - Math.max(1.0F, size * 0.08F);
         float fill = Math.max(0.0F, Math.min(1.0F, (float)stack.getItemBarStep() / 13.0F));
         int barColor = stack.getItemBarColor();
         Render2D.rect(barX, barY, barWidth, barHeight, barHeight * 0.5F, ColorUtil.rgba(0, 0, 0, Math.round(150.0F * alpha)));
         Render2D.rect(
            barX,
            barY,
            Math.max(1.0F, barWidth * fill),
            barHeight,
            barHeight * 0.5F,
            ColorUtil.rgba(barColor >>> 16 & 0xFF, barColor >>> 8 & 0xFF, barColor & 0xFF, Math.round(240.0F * alpha))
         );
      }
   }

   private static void renderStackCount(ItemStack stack, float x, float y, float size, float alpha) {
      if (stack.getCount() > 1) {
         String text = Integer.toString(stack.getCount());
         float textSize = Math.max(4.0F, Math.min(6.0F, size * 0.5F));
         float textWidth = Render2D.textWidth(FontType.SEMIBOLD, text, textSize);
         float textX = x + size - textWidth + size * 0.08F;
         float textY = y + size - textSize + size * 0.04F;
         float shadowOffset = Math.max(0.35F, Math.min(0.75F, size * 0.07F));
         Render2D.text(FontType.SEMIBOLD, text, textX + shadowOffset, textY + shadowOffset, textSize, ColorUtil.rgba(0, 0, 0, Math.round(170.0F * alpha)));
         Render2D.text(FontType.SEMIBOLD, text, textX, textY, textSize, ColorUtil.rgba(255, 255, 255, Math.round(255.0F * alpha)));
      }
   }

   private static float animatedSelectedX(float x, float y, float slot, float gap) {
      int selectedSlot = mc.player.getInventory().getSelectedSlot();
      float targetX = x + (float)selectedSlot * (slot + gap);
      long now = System.currentTimeMillis();
      boolean layoutChanged = Float.isNaN(lastHotbarX) || Math.abs(lastHotbarX - x) > 1.0F || Math.abs(lastHotbarY - y) > 1.0F;
      if (!layoutChanged && lastAnimationMs != 0L) {
         float delta = Math.min((float)(now - lastAnimationMs) / 1000.0F, 0.05F);
         lastAnimationMs = now;
         lastHotbarX = x;
         lastHotbarY = y;
         float speed = 20.0F;
         float factor = 1.0F - (float)Math.exp((double)(-speed * delta));
         animatedSelectedX = animatedSelectedX + (targetX - animatedSelectedX) * factor;
         if (Math.abs(targetX - animatedSelectedX) < 0.05F) {
            animatedSelectedX = targetX;
         }

         return animatedSelectedX;
      } else {
         animatedSelectedX = targetX;
         lastAnimationMs = now;
         lastHotbarX = x;
         lastHotbarY = y;
         return animatedSelectedX;
      }
   }

   private static float animatedOffhandProgress(boolean visible) {
      float target = visible ? 1.0F : 0.0F;
      long now = System.currentTimeMillis();
      if (lastOffhandAnimationMs == 0L) {
         lastOffhandAnimationMs = now;
      }

      float delta = Math.min((float)(now - lastOffhandAnimationMs) / 1000.0F, 0.05F);
      lastOffhandAnimationMs = now;
      float speed = 50.0F;
      float factor = 1.0F - (float)Math.exp((double)(-speed * delta));
      offhandProgress = offhandProgress + (target - offhandProgress) * factor;
      if (Math.abs(target - offhandProgress) < 0.01F) {
         offhandProgress = target;
      }

      return offhandProgress;
   }

   private static void renderSlotNumber(int index, float sx, float y, float slot, float alpha) {
      String number = String.valueOf(index + 1);
      float textSize = 6.0F;
      float textWidth = Render2D.textWidth(FontType.SEMIBOLD, number, textSize);
      Render2D.text(
         FontType.SEMIBOLD, number, sx + (slot - textWidth) * 0.5F + 0.5F, y + (slot - textSize) * 0.4F, textSize, ColorUtil.multAlpha(-6908266, alpha)
      );
   }

   private static record HotbarState(
      float x,
      float y,
      float slot,
      float gap,
      float fullX,
      float fullWidth,
      float paddingX,
      float paddingY,
      float selectedX,
      float offhandX,
      float offhandAlpha,
      ItemStack offhandStack
   ) {
   }
}
