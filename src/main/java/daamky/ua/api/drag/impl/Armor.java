package daamky.ua.api.drag.impl;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.item.RenderItemOptions;
import daamky.ua.utils.render.ui.Render2D;

public final class Armor extends HudPanel {
   private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final ItemStack[] PREVIEW = new ItemStack[]{
      Items.NETHERITE_HELMET.getDefaultStack(),
      Items.NETHERITE_CHESTPLATE.getDefaultStack(),
      Items.NETHERITE_LEGGINGS.getDefaultStack(),
      Items.NETHERITE_BOOTS.getDefaultStack()
   };
   private static final float ANIM_S = 0.35F;
   private static final float PANEL_RADIUS = 6.0F;
   private static final float PANEL_BLUR_RADIUS = 18.0F;
   private static final float PANEL_BLUR_SMOOTHNESS = 1.2F;
   private final SmoothAnimation[] slotAnim = new SmoothAnimation[]{new SmoothAnimation(), new SmoothAnimation(), new SmoothAnimation(), new SmoothAnimation()};
   private final boolean[] wasEmpty = new boolean[]{true, true, true, true};
   private final SmoothAnimation panelAnim = new SmoothAnimation();
   private boolean wasPreview = false;

   public Armor() {
      super("armor", "Armor", 210.0F, 10.0F, 75.0F, 20.0F);
   }

   @Override
   public void render() {
      Armor.ArmorState state = this.logics();
      if (state != null) {
         this.renderArmor(state);
      }
   }

   private Armor.ArmorState logics() {
      for (int i = 0; i < 4; i++) {
         this.slotAnim[i].update();
         boolean empty = this.mc.player == null || this.mc.player.getEquippedStack(SLOTS[i]).isEmpty();
         if (empty != this.wasEmpty[i]) {
            this.slotAnim[i].run(empty ? 0.0 : 1.0, 0.35F, Easings.EXPO_OUT);
            this.wasEmpty[i] = empty;
         }
      }

      boolean anySlotVisible = false;

      for (SmoothAnimation a : this.slotAnim) {
         if (a.get() > 0.01F) {
            anySlotVisible = true;
            break;
         }
      }

      boolean preview = !anySlotVisible && this.editPreview();
      this.panelAnim.update();
      if (preview && !this.wasPreview) {
         this.panelAnim.run(1.0, 0.35F, Easings.EXPO_OUT);
      } else if (!preview && this.wasPreview) {
         this.panelAnim.run(0.0, 0.35F, Easings.EXPO_IN);
      }

      this.wasPreview = preview;
      boolean targetVisible = anySlotVisible || preview || this.panelAnim.get() > 0.01F;
      float alpha = this.contentAlpha(targetVisible);
      if (alpha <= 0.0F) {
         return null;
      } else {
         int visibleCount = preview ? PREVIEW.length : 0;
         if (!preview) {
            for (SmoothAnimation ax : this.slotAnim) {
               if (ax.get() > 0.01F) {
                  visibleCount++;
               }
            }
         }

         this.size(Math.max(24.0F, (float)visibleCount * 18.0F + 7.0F), 20.0F);
         return new Armor.ArmorState(preview, alpha, this.drag.x(), this.drag.y(), visibleCount);
      }
   }

   private void renderArmor(Armor.ArmorState state) {
      if (!state.preview) {
         if (this.mc.player != null) {
            float itemX = state.x + 5.0F;
            this.renderPanelBackground(state.x, state.y, this.panelWidth(state.visibleCount), state.alpha);

            for (int i = 0; i < 4; i++) {
               float a = clamp(this.slotAnim[i].get() * state.alpha, 0.0F, 1.0F);
               ItemStack stack = this.mc.player.getEquippedStack(SLOTS[i]);
               if (!(a < 0.01F)) {
                  RenderItem.item(stack, itemX + 1.7F, state.y + 3.0F, 12.0F, RenderItemOptions.noDecorations(a));
                  if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                     int percent = Math.round((1.0F - (float)stack.getDamage() / (float)stack.getMaxDamage()) * 100.0F);
                     String text = Integer.toString(Math.max(0, percent));
                     float tw = Render2D.textWidth(TEXT_FONT, text, 4.5F);
                     int color = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * a));
                     Render2D.text(TEXT_FONT, text, itemX + 7.0F - tw * 0.5F, state.y + 13.0F, 6.0F, color);
                  }

                  itemX += 18.0F;
               }
            }
         }
      } else {
         float pa = clamp(this.panelAnim.get() * state.alpha, 0.0F, 1.0F);
         float itemX = state.x + 4.0F;
         this.renderPanelBackground(state.x, state.y, this.panelWidth(state.visibleCount), pa);

         for (ItemStack stack : PREVIEW) {
            RenderItem.item(stack, itemX + 1.7F, state.y + 5.0F, 12.0F, RenderItemOptions.noDecorations(pa));
            itemX += 18.0F;
         }
      }
   }

   private void renderPanelBackground(float x, float y, float width, float alpha) {
      HudRenderCompat.background(x, y, width, 20.0F, 6.0F, 18.0F, 1.2F, ColorUtil.rgba(0, 0, 0, 255, alpha));
   }

   private float panelWidth(int visibleCount) {
      return Math.max(24.0F, (float)visibleCount * 18.0F + 7.0F);
   }

   private static record ArmorState(boolean preview, float alpha, float x, float y, int visibleCount) {
   }
}
