package daamky.ua.api.drag.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.item.RenderItemOptions;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class Cooldowns extends HudPanel {
   private static final float ROW_STEP = 9.0F;
   private static final float PANEL_ANIM = 0.24F;
   private static final float ROW_ANIM = 0.22F;
   private static final ItemStack[] PREVIEW_STACKS = new ItemStack[]{
      Items.SUGAR.getDefaultStack(), Items.MACE.getDefaultStack(), Items.GOLDEN_APPLE.getDefaultStack()
   };
   private static final Comparator<Cooldowns.RowState> ROW_STATE_COMPARATOR = Comparator.comparingDouble(Cooldowns.RowState::offset);
   private final Map<Cooldowns.CooldownKey, Cooldowns.CooldownInfo> infoByItem = new LinkedHashMap<>();
   private final List<Cooldowns.RowEntry> rowEntries = new ArrayList<>();
   private final List<Cooldowns.CooldownInfo> activeCooldowns = new ArrayList<>();
   private final List<Cooldowns.RowState> rowStates = new ArrayList<>();
   private final Set<Cooldowns.CooldownKey> seenKeysScratch = new HashSet<>();
   private final SmoothAnimation panelAnimation = new SmoothAnimation();
   private final SmoothAnimation iconAlphaAnimation = new SmoothAnimation();
   private boolean iconAlphaForward = true;

   public Cooldowns() {
      super("cooldowns", "Cooldowns", 10.0F, 40.0F, 96.0F, 23.0F);
   }

   @Override
   public void render() {
      Cooldowns.CooldownsState state = this.logics();
      if (state != null) {
         this.renderCooldowns(state);
      }
   }

   private Cooldowns.CooldownsState logics() {
      List<Cooldowns.CooldownInfo> active = this.collectActive();
      boolean preview = active.isEmpty() && this.editPreview();
      boolean targetVisible = !active.isEmpty() || preview;
      this.drag.hitExpansion(0.0F, 17.0F, 0.0F, 0.0F);
      this.panelAnimation.update();
      this.panelAnimation.run(targetVisible ? 1.0 : 0.0, 0.24F, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
      this.iconAlphaAnimation.update();
      if (!this.iconAlphaAnimation.isAlive()) {
         this.iconAlphaAnimation.run(this.iconAlphaForward ? 1.0 : 0.0, 1.55, Easings.EXPO_IN_OUT);
         this.iconAlphaForward = !this.iconAlphaForward;
      }

      for (Cooldowns.RowEntry entry : this.rowEntries) {
         entry.active = false;
         entry.alpha.update();
         entry.y.update();
      }

      int targetRows = 0;
      if (preview) {
         Cooldowns.RowEntry entry = this.row("__preview", PREVIEW_STACKS[0], "Sugar", "**:**", 6.0F, 0.0F);
         entry.active = true;
         entry.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
         entry.y.run(0.0, 0.22F, Easings.EXPO_OUT, true);
         targetRows = 1;
      } else {
         for (Cooldowns.CooldownInfo info : active) {
            float targetY = (float)targetRows * 9.0F;
            Cooldowns.RowEntry entry = this.row(info.key.rowKey(), info.stack, info.displayName, info.remainingText(), 6.0F, targetY);
            entry.active = true;
            entry.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
            entry.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
            targetRows++;
         }
      }

      for (Cooldowns.RowEntry entry : this.rowEntries) {
         if (!entry.active) {
            entry.alpha.run(0.0, 0.22F, Easings.EXPO_IN, true);
         }
      }

      this.rowEntries.removeIf(entryx -> !entryx.active && entryx.alpha.get() <= 0.01F && !entryx.alpha.isAlive());
      float panelAlpha = this.panelAnimation.get();
      boolean visible = targetVisible || panelAlpha > 0.01F || !this.rowEntries.isEmpty();
      this.contentVisible(visible);
      if (!visible) {
         return null;
      } else {
         float width = 96.0F;
         float rowsHeight = 0.0F;

         for (Cooldowns.RowEntry entryx : this.rowEntries) {
            if (entryx.alpha.get() > 0.01F || entryx.active) {
               width = Math.max(width, Render2D.textWidth(TEXT_FONT, entryx.name, 6.0F) + 65.0F);
               rowsHeight = Math.max(rowsHeight, entryx.y.get() + 9.0F);
            }
         }

         float targetHeight = Math.max((float)Math.max(1, targetRows) * 9.0F, rowsHeight) + 10.0F;
         this.size(width, targetHeight);
         this.rowStates.clear();

         for (Cooldowns.RowEntry entryxx : this.rowEntries) {
            float alpha = entryxx.alpha.get();
            if (alpha > 0.01F || entryxx.active) {
               this.rowStates.add(new Cooldowns.RowState(entryxx.stack, entryxx.name, entryxx.time, entryxx.timeSize, entryxx.y.get(), alpha));
            }
         }

         if (this.rowStates.size() > 1) {
            this.rowStates.sort(ROW_STATE_COMPARATOR);
         }

         return new Cooldowns.CooldownsState(
            this.rowStates, this.iconAlphaAnimation.get(), panelAlpha, this.drag.x(), this.drag.y(), this.drag.width(), this.drag.height()
         );
      }
   }

   private void renderCooldowns(Cooldowns.CooldownsState state) {
      Hud hud = Hud.getInstance();
      int hudColor = hud != null ? hud.iconColor() : ColorUtil.rgba(247, 133, 255, 255);
      int fadedHudColor = ColorUtil.multAlpha(hudColor, state.alpha);
      int glowColor = ColorUtil.multAlpha(hudColor, 0.45F * state.alpha);
      float headerY = state.y - 17.0F;
      float headerHeight = 17.0F;
      float rowY = state.y + 5.0F;
      String headerText = "Cooldowns";
      float headerTextSize = 6.0F;
      float headerTextX = state.x + 10.0F;
      float headerTextY = headerY + (headerHeight - headerTextSize) * 0.5F - 0.5F;
      float headerTextWidth = Render2D.textWidth(TEXT_FONT, headerText, headerTextSize);
      float headerTextDotX = headerTextX + headerTextWidth + 3.0F;
      float iconSize = 10.0F;
      float iconWidth = Render2D.textWidth(FontType.MAINMENUSCREEN, "l", iconSize);
      float iconX = state.x + state.width - iconWidth - 8.0F;
      float iconY = headerY + (headerHeight - iconSize) * 0.5F + 1.0F;
      float glowSize = 22.0F;
      float glowX = iconX + iconWidth * 0.5F - glowSize * 0.5F;
      float glowY = headerY + (headerHeight - glowSize) * 0.5F;
      float sideRectY = headerY + (headerHeight - 9.0F) * 0.5F + 0.5F;
      float sideDotY = headerY + (headerHeight - 4.0F) * 0.5F + 0.5F;
      int glowAlpha = Math.round(128.0F * state.alpha);
      int glow = ColorUtil.rgba(0, 0, 0, 0);
      int purple = ColorUtil.rgba(247, 133, 255, Math.round(255.0F * state.alpha));
      int headerTextColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * state.alpha));
      int rowRectColor = ColorUtil.rgba(247, 133, 255, Math.round(255.0F * state.alpha));
      HudRenderCompat.background(
         Render2D.blurBuilder()
            .rectangle(state.x, headerY, state.width, headerHeight)
            .radius(4.0F, 4.0F, 2.0F, 2.0F)
            .blurRadius(15.0F)
            .smoothness(1.2F)
            .color(ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)))
            .build()
      );
      HudRenderCompat.background(
         Render2D.blurBuilder()
            .rectangle(state.x, state.y, state.width, state.height)
            .radius(2.0F, 2.0F, 4.0F, 4.0F)
            .blurRadius(15.0F)
            .smoothness(1.2F)
            .color(ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)))
            .build()
      );
      if (glowAlpha > 0) {
         HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", glowX, glowY - 7.5F, glowSize, glowSize + 15.0F, 0.0F, glowColor);
      }

      Render2D.rect(state.x, sideRectY, 2.5F, 9.0F, 0.0F, 2.0F, 2.0F, 0.0F, fadedHudColor);
      Render2D.rect(state.x + 3.5F, sideDotY, 4.0F, 4.0F, 4.0F, fadedHudColor);
      Render2D.rect(headerTextDotX, sideDotY, 4.0F, 4.0F, 4.0F, fadedHudColor);
      Render2D.text(TEXT_FONT, headerText, headerTextX, headerTextY, headerTextSize, headerTextColor);
      Render2D.text(FontType.MAINMENUSCREEN, "l", iconX, iconY, iconSize, fadedHudColor);

      for (Cooldowns.RowState row : state.rows) {
         float rowAlpha = state.alpha * row.alpha;
         float currentY = rowY + row.offset;
         float itemX = state.x + 5.0F;
         float itemY = currentY + 1.0F;
         float itemSize = 8.0F;
         float dotX = itemX + itemSize + 1.5F;
         float dotY = currentY + 3.5F;
         float nameX = dotX + 4.5F;
         float timeX = state.x + state.width - 27.0F;
         String visibleName = this.trimToWidth(row.name, TEXT_FONT, 6.0F, timeX - nameX - 5.0F);
         RenderItem.item(row.stack, itemX, itemY, itemSize, RenderItemOptions.noDecorations(rowAlpha));
         Render2D.rect(dotX, dotY, 3.0F, 3.0F, 3.0F, ColorUtil.rgba(128, 128, 128, Math.round(245.0F * rowAlpha)));
         Render2D.text(TEXT_FONT, visibleName, nameX, currentY + 1.5F, 6.0F, ColorUtil.multAlpha(TEXT_COLOR, rowAlpha));
         this.renderTimer(row.time, timeX, currentY + 1.0F, row.timeSize, rowAlpha);
      }
   }

   private Cooldowns.RowEntry row(String key, ItemStack stack, String name, String time, float timeSize, float targetY) {
      for (Cooldowns.RowEntry entry : this.rowEntries) {
         if (entry.key.equals(key)) {
            entry.stack = stack == null ? ItemStack.EMPTY : stack.copy();
            entry.name = name;
            entry.time = time;
            entry.timeSize = timeSize;
            return entry;
         }
      }

      Cooldowns.RowEntry entryx = new Cooldowns.RowEntry(key, stack, name, time, timeSize);
      entryx.alpha.set(0.0);
      entryx.y.set((double)(targetY + 4.0F));
      this.rowEntries.add(entryx);
      return entryx;
   }

   private List<Cooldowns.CooldownInfo> collectActive() {
      this.activeCooldowns.clear();
      if (this.mc.player == null) {
         this.infoByItem.clear();
         return this.activeCooldowns;
      } else {
         Set<Cooldowns.CooldownKey> seenKeys = this.seenKeysScratch;
         seenKeys.clear();

         for (int i = 0; i < this.mc.player.getInventory().size(); i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && this.mc.player.getItemCooldownManager().isCoolingDown(stack)) {
               Cooldowns.CooldownKey key = Cooldowns.CooldownKey.of(stack);
               if (seenKeys.add(key)) {
                  float progress = this.mc.player.getItemCooldownManager().getCooldownProgress(stack, 0.0F);
                  Cooldowns.CooldownInfo info = this.infoByItem.computeIfAbsent(key, ignored -> new Cooldowns.CooldownInfo());
                  info.key = key;
                  info.stack = stack.copy();
                  info.displayName = this.displayName(stack);
                  info.update(progress, (long)this.mc.player.age);
                  this.activeCooldowns.add(info);
               }
            }
         }

         this.infoByItem.entrySet().removeIf(entry -> !this.containsActiveKey(entry.getKey()));
         return this.activeCooldowns;
      }
   }

   private boolean containsActiveKey(Cooldowns.CooldownKey key) {
      for (Cooldowns.CooldownInfo info : this.activeCooldowns) {
         if (info.key.equals(key)) {
            return true;
         }
      }

      return false;
   }

   private String displayName(ItemStack stack) {
      String id = Registries.ITEM.getId(stack.getItem()).toString();

      return switch (id) {
         case "minecraft:ender_eye" -> "Disorientation";
         case "minecraft:sugar" -> "Sugar";
         case "minecraft:netherite_scrap" -> "Trap";
         case "minecraft:dried_kelp" -> "Plast";
         case "minecraft:trident" -> "Trident";
         case "minecraft:mace" -> "Mace";
         case "minecraft:wind_charge" -> "Wind Charge";
         case "minecraft:enchanted_golden_apple" -> "Ench. Gap";
         case "minecraft:golden_apple" -> "Golden Apple";
         default -> stack.getName().getString();
      };
   }

   private void renderTimer(String text, float x, float y, float size, float alpha) {
      float cellWidth = 3.25F;
      int color = ColorUtil.rgba(255, 255, 255, Math.round(245.0F * alpha));

      for (int i = 0; i < text.length(); i++) {
         String character = charText(text.charAt(i));
         float characterWidth = Render2D.textWidth(TEXT_FONT, character, size);
         float characterX = x + (float)i * cellWidth + (cellWidth - characterWidth) * 0.5F + 3.0F;
         Render2D.text(TEXT_FONT, character, characterX, y, size, color);
      }
   }

   private String formatCooldownDurationTicks(int ticks) {
      if (ticks < 0) {
         return "**:**";
      } else {
         int totalSeconds = Math.max(0, ticks / 20);
         int minutes = Math.min(99, totalSeconds / 60);
         int seconds = totalSeconds % 60;
         return twoDigits(minutes) + ":" + twoDigits(seconds);
      }
   }

   private final class CooldownInfo {
      private Cooldowns.CooldownKey key;
      private ItemStack stack = ItemStack.EMPTY;
      private String displayName = "";
      private float progress;
      private float lastProgress = -1.0F;
      private long lastTick = -1L;
      private int totalTicks = -1;
      private int remainingTicks;

      private CooldownInfo() {
      }

      private void update(float nextProgress, long tick) {
         this.progress = HudPanel.clamp(nextProgress, 0.0F, 1.0F);
         if (this.lastTick >= 0L && tick > this.lastTick && this.lastProgress > this.progress) {
            float diff = this.lastProgress - this.progress;
            if (diff > 1.0E-5F) {
               int estimate = Math.round((float)(tick - this.lastTick) / diff);
               if (estimate > 0 && estimate < 12000) {
                  this.totalTicks = this.totalTicks <= 0 ? estimate : Math.round((float)this.totalTicks * 0.75F + (float)estimate * 0.25F);
               }
            }
         }

         this.lastProgress = this.progress;
         this.lastTick = tick;
         this.remainingTicks = this.totalTicks <= 0
            ? Math.max(1, Math.round(this.progress * 20.0F))
            : Math.max(1, Math.round(this.progress * (float)this.totalTicks));
      }

      private String remainingText() {
         return Cooldowns.this.formatCooldownDurationTicks(this.remainingTicks);
      }
   }

   private static record CooldownKey(Item item, ComponentMap components, String name) {
      private static Cooldowns.CooldownKey of(ItemStack stack) {
         return new Cooldowns.CooldownKey(stack.getItem(), stack.getImmutableComponents(), stack.getName().getString());
      }

      private String rowKey() {
         return Registries.ITEM.getId(this.item) + "|" + this.name + "|" + this.components.hashCode();
      }
   }

   private static record CooldownsState(List<Cooldowns.RowState> rows, float iconAlpha, float alpha, float x, float y, float width, float height) {
   }

   private static final class RowEntry {
      private final String key;
      private final SmoothAnimation alpha = new SmoothAnimation();
      private final SmoothAnimation y = new SmoothAnimation();
      private ItemStack stack;
      private String name;
      private String time;
      private float timeSize;
      private boolean active;

      private RowEntry(String key, ItemStack stack, String name, String time, float timeSize) {
         this.key = key;
         this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
         this.name = name;
         this.time = time;
         this.timeSize = timeSize;
      }
   }

   private static record RowState(ItemStack stack, String name, String time, float timeSize, float offset, float alpha) {
   }
}
