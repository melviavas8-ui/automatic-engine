package daamky.ua.api.module.impl.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;
import daamky.ua.utils.render.ui.font.FontType;

public final class CustomHotbar extends Module {
   private static CustomHotbar instance;
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static float animatedSelectedX;
   private static float lastHotbarX = Float.NaN;
   private static float lastHotbarY = Float.NaN;
   private static long lastAnimationMs;
   private static float offhandProgress;
   private static long lastOffhandAnimationMs;
   private static ItemStack lastOffhandStack = ItemStack.EMPTY;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   public CustomHotbar() {
      super("CustomHotbar", "Renders a custom hotbar.", ModuleCategory.VISUAL);
      instance = this;
   }

   public static boolean isActive() {
      return instance != null && instance.isEnabled();
   }

   public static void render(DrawContext graphics) {
      if (mc.player != null && mc.getWindow() != null) {
         Render2D.beginFrame(graphics);
         int sw = mc.getWindow().getScaledWidth();
         int sh = mc.getWindow().getScaledHeight();
         float slot = 17.5F;
         float gap = 2.5F;
         float width = slot * 9.0F + gap * 8.0F;
         float x = (float)sw / 2.0F - width / 2.0F;
         float y = (float)(sh - 21);
         float offhandX = x - gap - slot;
         int armorCount = armorCount();
         float armorSlot = 14.0F;
         float armorGap = 1.8F;
         float armorIcon = 11.5F;
         float armorGroupGap = armorCount > 0 ? gap : 0.0F;
         float armorWidth = armorCount > 0 ? (float)armorCount * armorSlot + (float)(armorCount - 1) * armorGap : 0.0F;
         float armorX = x + width + armorGroupGap;
         ItemStack offhandStack = mc.player.getOffHandStack();
         updateLastOffhandStack(offhandStack);
         float offhandAlpha = animatedOffhandProgress(!offhandStack.isEmpty());
         float offhandArea = (slot + gap) * offhandAlpha;
         float armorArea = armorGroupGap + armorWidth;
         float fullX = x - offhandArea;
         float fullWidth = width + offhandArea + armorArea;
         float paddingX = 4.0F;
         float paddingY = 3.0F;
         int blurMask = -1;
         int background = 1429222458;
         int tint = 570425344;
         int selectedColor = -1;
         Render2D.blur(fullX - paddingX, y - paddingY, fullWidth + paddingX * 2.0F, slot + paddingY * 2.0F, 6.0F, 18.0F, 11.0F, blurMask);
         Render2D.rect(fullX - paddingX, y - paddingY, fullWidth + paddingX * 2.0F, slot + paddingY * 2.0F, 6.0F, background);
         Render2D.rect(fullX - paddingX, y - paddingY, fullWidth + paddingX * 2.0F, slot + paddingY * 2.0F, 6.0F, tint);
         if (offhandAlpha > 0.01F) {
            Render2D.rect(x - gap * 0.5F - 0.5F, y + 3.0F, 1.0F, slot - 6.0F, 0.0F, ColorUtil.multAlpha(587202559, offhandAlpha));
         }

         if (armorCount > 0) {
            Render2D.rect(x + width + armorGroupGap * 0.5F - 0.5F, y + 3.0F, 1.0F, slot - 6.0F, 0.0F, 587202559);
         }

         for (int i = 0; i < 9; i++) {
            float sx = x + (float)i * (slot + gap);
            if (i > 0) {
               Render2D.rect(sx - gap * 0.5F - 0.5F, y + 3.0F, 1.0F, slot - 6.0F, 0.0F, 587202559);
            }
         }

         for (int ix = 1; ix < armorCount; ix++) {
            float sx = armorX + (float)ix * (armorSlot + armorGap);
            Render2D.rect(sx - armorGap * 0.5F - 0.5F, y + 4.0F, 1.0F, slot - 8.0F, 0.0F, 419430399);
         }

         Render2D.rect(animatedSelectedX(x, y, slot, gap), y, slot, slot, 5.0F, selectedColor);
         Render2D.flush();

         for (int ix = 0; ix < 9; ix++) {
            float sx = x + (float)ix * (slot + gap);
            ItemStack stack = mc.player.getInventory().getStack(ix);
            if (!stack.isEmpty()) {
               renderVanillaItem(graphics, stack, sx + 1.5F, y + 1.5F, 14.0F);
            }
         }

         if (offhandAlpha > 0.01F && !lastOffhandStack.isEmpty()) {
            renderVanillaItem(graphics, lastOffhandStack, offhandX + 1.5F, y + 1.5F, 14.0F);
         }

         renderArmorItems(graphics, armorX, y, slot, armorSlot, armorGap, armorIcon);
         graphics.createNewRootLayer();
         Render2D.beginFrame(graphics);

         for (int ixx = 0; ixx < 9; ixx++) {
            float sx = x + (float)ixx * (slot + gap);
            ItemStack stack = mc.player.getInventory().getStack(ixx);
            if (stack.isEmpty()) {
               drawSlotNumber(ixx, sx, y, slot);
            } else {
               drawItemDecorations(stack, sx + 1.5F, y + 1.5F, 14.0F, 1.0F);
               drawStackCount(stack, sx, y, slot);
            }
         }

         if (!lastOffhandStack.isEmpty()) {
            drawItemDecorations(lastOffhandStack, offhandX + 1.5F, y + 1.5F, 14.0F, offhandAlpha);
         }

         drawStackCount(lastOffhandStack, offhandX, y, slot, offhandAlpha);
         drawArmorDecorations(armorX, y, slot, armorSlot, armorGap, armorIcon);
         if (offhandAlpha <= 0.01F && offhandStack.isEmpty()) {
            lastOffhandStack = ItemStack.EMPTY;
         }

         Render2D.flush();
         graphics.createNewRootLayer();
      }
   }

   private static void renderVanillaItem(DrawContext graphics, ItemStack stack, float x, float y, float size) {
      if (graphics != null && !stack.isEmpty() && !(size <= 0.0F)) {
         float scale = size / 16.0F;
         graphics.getMatrices().pushMatrix();

         try {
            Render2DCoordinateSpace.applyGuiScaleIndependence(graphics.getMatrices());
            graphics.getMatrices().translate(x, y);
            graphics.getMatrices().scale(scale);
            graphics.drawItem(stack, 0, 0);
         } finally {
            graphics.getMatrices().popMatrix();
         }
      }
   }

   private static int armorCount() {
      int count = 0;

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack stack = mc.player.getEquippedStack(slot);
         if (!stack.isEmpty()) {
            count++;
         }
      }

      return count;
   }

   private static void renderArmorItems(DrawContext graphics, float x, float y, float slotSize, float armorSlot, float gap, float iconSize) {
      int index = 0;

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack stack = mc.player.getEquippedStack(slot);
         if (!stack.isEmpty()) {
            float sx = x + (float)index * (armorSlot + gap);
            renderVanillaItem(graphics, stack, sx + (armorSlot - iconSize) * 0.5F, y + (slotSize - iconSize) * 0.5F, iconSize);
            index++;
         }
      }
   }

   private static void drawArmorDecorations(float x, float y, float slotSize, float armorSlot, float gap, float iconSize) {
      int index = 0;

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack stack = mc.player.getEquippedStack(slot);
         if (!stack.isEmpty()) {
            float sx = x + (float)index * (armorSlot + gap);
            drawItemDecorations(stack, sx + (armorSlot - iconSize) * 0.5F, y + (slotSize - iconSize) * 0.5F, iconSize, 1.0F);
            index++;
         }
      }
   }

   private static void updateLastOffhandStack(ItemStack offhandStack) {
      if (!offhandStack.isEmpty()) {
         if (shouldRefreshCachedStack(lastOffhandStack, offhandStack)) {
            lastOffhandStack = offhandStack.copy();
         }
      }
   }

   private static boolean shouldRefreshCachedStack(ItemStack cached, ItemStack current) {
      return cached.isEmpty()
         || cached.getItem() != current.getItem()
         || cached.getCount() != current.getCount()
         || cached.getDamage() != current.getDamage()
         || cached.isItemBarVisible() != current.isItemBarVisible()
         || cached.getItemBarStep() != current.getItemBarStep()
         || cached.getItemBarColor() != current.getItemBarColor();
   }

   private static void drawItemDecorations(ItemStack stack, float x, float y, float size, float alpha) {
      if (!stack.isEmpty() && !(alpha <= 0.01F)) {
         drawCooldownOverlay(stack, x, y, size, alpha);
         drawDurabilityBar(stack, x, y, size, alpha);
      }
   }

   private static void drawDurabilityBar(ItemStack stack, float x, float y, float size, float alpha) {
      if (stack.isItemBarVisible()) {
         float barWidth = Math.max(7.0F, size * 0.78F);
         float barHeight = Math.max(1.2F, size * 0.08F);
         float barX = x + (size - barWidth) * 0.5F;
         float barY = y + size - barHeight - Math.max(0.8F, size * 0.08F);
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

   private static void drawCooldownOverlay(ItemStack stack, float x, float y, float size, float alpha) {
      float cooldown = mc.player.getItemCooldownManager().getCooldownProgress(stack, 0.0F);
      if (!(cooldown <= 0.0F)) {
         float overlayHeight = size * Math.max(0.0F, Math.min(1.0F, cooldown));
         Render2D.rect(x, y + size - overlayHeight, size, overlayHeight, 1.0F, ColorUtil.rgba(0, 0, 0, Math.round(145.0F * alpha)));
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
      float speed = 14.0F;
      float factor = 1.0F - (float)Math.exp((double)(-speed * delta));
      offhandProgress = offhandProgress + (target - offhandProgress) * factor;
      if (Math.abs(target - offhandProgress) < 0.01F) {
         offhandProgress = target;
      }

      return offhandProgress;
   }

   private static void drawSlotNumber(int index, float sx, float y, float slot) {
      String number = String.valueOf(index + 1);
      float textSize = 7.0F;
      float textWidth = Render2D.textWidth(FontType.SEMIBOLD, number, textSize);
      Render2D.text(FontType.SEMIBOLD, number, sx + (slot - textWidth) * 0.5F, y + (slot - textSize) * 0.4F, textSize, -6908266);
   }

   private static void drawStackCount(ItemStack stack, float sx, float y, float slot) {
      drawStackCount(stack, sx, y, slot, 1.0F);
   }

   private static void drawStackCount(ItemStack stack, float sx, float y, float slot, float alpha) {
      if (!stack.isEmpty() && stack.getCount() > 1) {
         String count = stack.getCount() > 99 ? "99+" : String.valueOf(stack.getCount());
         float textSize = 5.5F;
         float textWidth = Render2D.textWidth(FontType.SEMIBOLD, count, textSize);
         float textX = sx + slot - textWidth - 0.5F;
         float textY = y + slot - textSize - 0.5F;
         Render2D.text(FontType.SEMIBOLD, count, textX + 0.7F, textY + 0.7F, textSize, ColorUtil.multAlpha(-1442840576, alpha));
         Render2D.text(FontType.SEMIBOLD, count, textX, textY, textSize, ColorUtil.multAlpha(-1, alpha));
      }
   }
}
