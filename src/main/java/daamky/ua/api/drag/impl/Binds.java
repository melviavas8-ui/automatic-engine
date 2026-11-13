package daamky.ua.api.drag.impl;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import daamky.ua.api.drag.core.ElementScreen;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.misc.ServerHelper;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.item.RenderItemOptions;
import daamky.ua.utils.render.ui.Render2D;

public final class Binds extends HudPanel {
   private static final int PER_ROW = 5;
   private static final float CARD_WIDTH = 20.0F;
   private static final float CARD_HEIGHT = 16.0F;
   private static final float CARD_GAP = 3.0F;
   private static final float ITEM_SIZE = 8.0F;
   private static final ItemStack[] PREVIEW_STACKS = new ItemStack[]{
      Items.ENDER_EYE.getDefaultStack(), Items.SUGAR.getDefaultStack(), Items.DRIED_KELP.getDefaultStack()
   };
   private static final String[] PREVIEW_KEYS = new String[]{"A", "B", "C"};
   private final SmoothAnimation panelAnimation = new SmoothAnimation();
   private final List<Binds.BindEntry> bindEntries = new ArrayList<>();
   private final List<Binds.BindEntry> lastBinds = new ArrayList<>();
   private boolean lastPreview;
   private int lastCount;
   private int lastRows = 1;
   private float lastWidth = 22.0F;
   private float lastHeight = 19.0F;

   public Binds() {
      super("binds", "Binds", 10.0F, 180.0F, 48.0F, 18.0F);
   }

   @Override
   public void render() {
      Binds.BindsState state = this.logics();
      if (state != null) {
         this.renderBinds(state);
      }
   }

   private Binds.BindsState logics() {
      List<Binds.BindEntry> binds = this.hudBinds();
      boolean preview = binds.isEmpty() && this.editPreview();
      boolean targetVisible = !binds.isEmpty() || preview;
      if (targetVisible) {
         this.rememberLayout(binds, preview);
      }

      this.panelAnimation.update();
      this.panelAnimation.run(targetVisible ? 1.0 : 0.0, 0.24F, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
      float alpha = this.panelAnimation.get();
      boolean fading = !targetVisible && this.lastCount > 0 && (alpha > 0.01F || this.panelAnimation.isAlive());
      boolean renderVisible = targetVisible || fading;
      this.contentVisible(renderVisible);
      if (!renderVisible) {
         return null;
      } else {
         List<Binds.BindEntry> renderBinds = targetVisible ? binds : this.lastBinds;
         boolean renderPreview = targetVisible ? preview : this.lastPreview;
         int count = targetVisible ? (preview ? PREVIEW_STACKS.length : binds.size()) : this.lastCount;
         int rows = targetVisible ? Math.max(1, (int)Math.ceil((double)count / 5.0)) : this.lastRows;
         float width = targetVisible ? this.layoutWidth(binds, preview, count, rows) : this.lastWidth;
         float height = targetVisible ? (float)rows * 19.0F : this.lastHeight;
         this.sizeInstant(width, height);
         return new Binds.BindsState(renderBinds, renderPreview, count, rows, alpha, this.drag.x(), this.drag.y(), this.drag.width());
      }
   }

   private void rememberLayout(List<Binds.BindEntry> binds, boolean preview) {
      int count = preview ? PREVIEW_STACKS.length : binds.size();
      int rows = Math.max(1, (int)Math.ceil((double)count / 5.0));
      float width = this.layoutWidth(binds, preview, count, rows);
      this.lastBinds.clear();
      if (!preview) {
         this.lastBinds.addAll(binds);
      }

      this.lastPreview = preview;
      this.lastCount = count;
      this.lastRows = rows;
      this.lastWidth = width;
      this.lastHeight = (float)rows * 19.0F;
   }

   private float layoutWidth(List<Binds.BindEntry> binds, boolean preview, int count, int rows) {
      float width = 0.0F;

      for (int row = 0; row < rows; row++) {
         int start = row * 5;
         int end = Math.min(count, start + 5);
         float rowWidth = 0.0F;

         for (int i = start; i < end; i++) {
            rowWidth += this.cardWidth();
            if (i + 1 < end) {
               rowWidth += 3.0F;
            }
         }

         width = Math.max(width, rowWidth);
      }

      return Math.max(22.0F, width + 6.0F);
   }

   private void sizeInstant(float width, float height) {
      this.drag.size((float)Math.ceil((double)width), (float)Math.ceil((double)height));
      this.drag.clamp(ElementScreen.current());
   }

   private void renderBinds(Binds.BindsState state) {
      int index = 0;

      for (int row = 0; row < state.rows; row++) {
         int start = row * 5;
         int end = Math.min(state.count, start + 5);
         float rowWidth = 0.0F;

         for (int i = start; i < end; i++) {
            rowWidth += this.cardWidth();
            if (i + 1 < end) {
               rowWidth += 3.0F;
            }
         }

         float cardX = state.x + (state.width - rowWidth) * 0.5F;
         float cardY = state.y + (float)row * 19.0F + 1.5F;

         for (int ix = start; ix < end && index < state.count; ix++) {
            ItemStack stack;
            String key;
            int itemCount;
            float cooldownProgress;
            if (state.preview) {
               stack = PREVIEW_STACKS[index];
               key = PREVIEW_KEYS[index];
               itemCount = 64;
               cooldownProgress = 0.0F;
            } else {
               Binds.BindEntry bind = state.binds.get(index);
               stack = bind.stack();
               key = this.shortBind(bind.bind());
               itemCount = bind.inventoryCount();
               cooldownProgress = bind.cooldownProgress();
            }

            float cw = this.cardWidth();
            boolean hasStack = stack != null && !stack.isEmpty();
            String countText = itemCount > 99 ? "99+" : Integer.toString(Math.max(0, itemCount));
            float countWidth = Render2D.textWidth(TEXT_FONT, countText, 5.0F);
            float overlayHeight = 16.0F * clamp(cooldownProgress, 0.0F, 1.0F);
            float itemX = cardX + (cw - 8.0F) * 0.5F;
            float itemY = cardY + 4.0F;
            String visibleKey = this.trimToWidth(key, TEXT_FONT, 4.8F, cw - 4.0F);
            HudRenderCompat.background(cardX, cardY, cw, 16.0F, 4.0F, 15.0F, 1.2F, ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)));
            if (hasStack) {
               RenderItem.item(stack, itemX, itemY, 8.0F, RenderItemOptions.noDecorations(state.alpha));
            }

            Render2D.text(TEXT_FONT, countText, cardX + cw - countWidth - 2.0F, cardY + 8.0F, 5.0F, ColorUtil.multAlpha(TEXT_COLOR, state.alpha));
            Render2D.text(TEXT_FONT, visibleKey, cardX - 1.0F, cardY + 1.0F, 4.8F, ColorUtil.multAlpha(MUTED_COLOR, state.alpha));
            if (cooldownProgress > 0.0F) {
               Render2D.rect(cardX, cardY + 16.0F - overlayHeight, cw, overlayHeight, 4.0F, ColorUtil.rgba(0, 0, 0, Math.round(80.0F * state.alpha)));
            }

            cardX += cw + 3.0F;
            index++;
         }
      }
   }

   private List<Binds.BindEntry> hudBinds() {
      ModuleManager manager = Manager.getModules();
      if (manager == null) {
         this.bindEntries.clear();
         return List.of();
      } else {
         return manager.getByType(ServerHelper.class).map(this::readHudBinds).orElse(List.of());
      }
   }

   private List<Binds.BindEntry> readHudBinds(ServerHelper helper) {
      List<ServerHelper.HudBind> hudBinds = helper.getHudBinds(0.0F);
      this.bindEntries.clear();
      if (hudBinds != null && !hudBinds.isEmpty()) {
         for (ServerHelper.HudBind hudBind : hudBinds) {
            if (hudBind != null) {
               ItemStack stack = hudBind.stack() == null ? ItemStack.EMPTY : hudBind.stack().copy();
               KeyBind bind = hudBind.setting() == null ? KeyBind.NONE : hudBind.setting().getValue();
               this.bindEntries.add(new Binds.BindEntry(stack, bind == null ? KeyBind.NONE : bind, hudBind.inventoryCount(), hudBind.cooldownProgress()));
            }
         }

         return this.bindEntries;
      } else {
         return this.bindEntries;
      }
   }

   private float cardWidth() {
      return 20.0F;
   }

   private static record BindEntry(ItemStack stack, KeyBind bind, int inventoryCount, float cooldownProgress) {
   }

   private static record BindsState(List<Binds.BindEntry> binds, boolean preview, int count, int rows, float alpha, float x, float y, float width) {
   }
}
