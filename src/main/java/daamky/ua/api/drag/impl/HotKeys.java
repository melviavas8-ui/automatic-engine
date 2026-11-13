package daamky.ua.api.drag.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class HotKeys extends HudPanel {
   private static final float ROW_STEP = 11.0F;
   private static final float PANEL_ANIM = 0.24F;
   private static final float ROW_ANIM = 0.22F;
   private static final String VOICE_MARKER = "[Голос.]";
   private static final float VOICE_MARKER_SIZE = 4.0F;
   private static final Comparator<Module> MODULE_NAME_COMPARATOR = Comparator.comparing(Module::getName);
   private static final Comparator<HotKeys.RowState> ROW_STATE_COMPARATOR = Comparator.comparingDouble(HotKeys.RowState::offset);
   private final List<HotKeys.RowEntry> rowEntries = new ArrayList<>();
   private final List<Module> activeModules = new ArrayList<>();
   private final List<HotKeys.RowState> rowStates = new ArrayList<>();
   private final SmoothAnimation panelAnimation = new SmoothAnimation();
   private final SmoothAnimation iconAlphaAnimation = new SmoothAnimation();
   private boolean iconAlphaForward = true;

   public HotKeys() {
      super("hotkeys", "HotKeys", 300.0F, 40.0F, 80.0F, 23.0F);
   }

   @Override
   public void render() {
      HotKeys.HotKeysState state = this.logics();
      if (state != null) {
         this.renderPanel(state);
      }
   }

   private HotKeys.HotKeysState logics() {
      this.activeModules.clear();
      ModuleManager manager = Manager.getModules();
      if (manager != null) {
         for (Module module : manager.getModules()) {
            if (module.isEnabled() && !module.isHidden() && module.getBind() != null && module.getBind().isBound()) {
               this.activeModules.add(module);
            }
         }
      }

      if (this.activeModules.size() > 1) {
         this.activeModules.sort(MODULE_NAME_COMPARATOR);
      }

      boolean preview = this.activeModules.isEmpty() && this.editPreview();
      boolean targetVisible = !this.activeModules.isEmpty() || preview;
      this.drag.hitExpansion(0.0F, 17.0F, 0.0F, 0.0F);
      this.panelAnimation.update();
      this.panelAnimation.run(targetVisible ? 1.0 : 0.0, 0.24F, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
      this.iconAlphaAnimation.update();
      if (!this.iconAlphaAnimation.isAlive()) {
         this.iconAlphaAnimation.run(this.iconAlphaForward ? 1.0 : 0.0, 1.55, Easings.EXPO_IN_OUT);
         this.iconAlphaForward = !this.iconAlphaForward;
      }

      for (HotKeys.RowEntry entry : this.rowEntries) {
         entry.active = false;
         entry.alpha.update();
         entry.y.update();
      }

      int targetRows = 0;
      if (preview) {
         HotKeys.RowEntry entry = this.row("__preview", ClientLanguage.tr("Example"), "[R]", 5.0F, false, 0.0F);
         entry.active = true;
         entry.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
         entry.y.run(0.0, 0.22F, Easings.EXPO_OUT, true);
         targetRows = 1;
      } else {
         for (Module modulex : this.activeModules) {
            String bind = modulex.getBind() != null && modulex.getBind().isBound() ? "[" + this.shortBind(modulex.getBind()) + "]" : "";
            float targetY = (float)targetRows * 11.0F;
            HotKeys.RowEntry entry = this.row(modulex.getName(), ClientLanguage.tr(modulex.getName()), bind, 6.0F, false, targetY);
            entry.active = true;
            entry.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
            entry.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
            targetRows++;
         }
      }

      for (HotKeys.RowEntry entry : this.rowEntries) {
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
         float width = 80.0F;

         for (HotKeys.RowEntry entryx : this.rowEntries) {
            if (entryx.alpha.get() > 0.01F || entryx.active) {
               float markerWidth = entryx.voiceActivated ? Render2D.textWidth(TEXT_FONT, "[Голос.]", 4.0F) + 3.0F : 0.0F;
               width = Math.max(
                  width, Render2D.textWidth(TEXT_FONT, entryx.name, 6.0F) + markerWidth + Render2D.textWidth(TEXT_FONT, entryx.bind, 5.0F) + 35.0F
               );
            }
         }

         this.size(width, (float)Math.max(1, targetRows) * 10.0F + 10.0F);
         this.rowStates.clear();

         for (HotKeys.RowEntry entryxx : this.rowEntries) {
            float alpha = entryxx.alpha.get();
            if (alpha > 0.01F || entryxx.active) {
               this.rowStates.add(new HotKeys.RowState(entryxx.name, entryxx.bind, entryxx.bindSize, entryxx.voiceActivated, entryxx.y.get(), alpha));
            }
         }

         if (this.rowStates.size() > 1) {
            this.rowStates.sort(ROW_STATE_COMPARATOR);
         }

         return new HotKeys.HotKeysState(
            this.rowStates, this.iconAlphaAnimation.get(), panelAlpha, this.drag.x(), this.drag.y(), this.drag.width(), this.drag.height()
         );
      }
   }

   private void renderPanel(HotKeys.HotKeysState state) {
      float headerY = state.y - 17.0F;
      float headerHeight = 17.0F;
      float rowY = state.y + 5.0F;
      String headerText = ClientLanguage.tr("Hot Keys");
      float headerTextSize = 6.0F;
      float headerTextX = state.x + 10.0F;
      float headerTextY = headerY + (headerHeight - headerTextSize) * 0.5F - 0.5F;
      float headerTextWidth = Render2D.textWidth(TEXT_FONT, headerText, headerTextSize);
      float headerTextDotX = headerTextX + headerTextWidth + 3.0F;
      Hud hud = Hud.getInstance();
      int hudColor = hud != null ? hud.iconColor() : ColorUtil.rgba(247, 133, 255, 255);
      int fadedHudColor = ColorUtil.multAlpha(hudColor, state.alpha);
      int glowColor = ColorUtil.multAlpha(hudColor, 0.45F * state.alpha);
      float iconSize = 12.0F;
      float iconWidth = Render2D.textWidth(FontType.MAINMENUSCREEN, "m", iconSize);
      float iconX = state.x + state.width - iconWidth - 7.0F;
      float iconY = headerY + (headerHeight - iconSize) * 0.5F + 1.0F;
      float glowSize = 22.0F;
      float glowX = iconX + iconWidth * 0.5F - glowSize * 0.5F;
      float glowY = headerY + (headerHeight - glowSize) * 0.5F;
      float sideRectY = headerY + (headerHeight - 9.0F) * 0.5F + 0.5F;
      float sideDotY = headerY + (headerHeight - 4.0F) * 0.5F + 0.5F;
      int iconAlpha = 255;
      int glowAlpha = Math.round(128.0F * state.alpha);
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
         HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", glowX - 13.0F, glowY + 0.5F, glowSize + 25.0F, glowSize, 0.0F, glowColor);
      }

      Render2D.rect(state.x, sideRectY, 2.5F, 9.0F, 0.0F, 2.0F, 2.0F, 0.0F, fadedHudColor);
      Render2D.rect(state.x + 3.5F, sideDotY, 4.0F, 4.0F, 4.0F, fadedHudColor);
      Render2D.rect(headerTextDotX, sideDotY, 4.0F, 4.0F, 4.0F, fadedHudColor);
      Render2D.text(TEXT_FONT, headerText, headerTextX, headerTextY, headerTextSize, headerTextColor);
      Render2D.text(FontType.MAINMENUSCREEN, "m", iconX, iconY, iconSize, fadedHudColor);

      for (HotKeys.RowState row : state.rows) {
         float rowAlpha = state.alpha * row.alpha;
         float currentY = rowY + row.offset;
         float bindWidth = Render2D.textWidth(TEXT_FONT, row.bind, 5.0F);
         float nameX = state.x + 8.0F;
         float bindX = state.x + state.width - bindWidth - 10.0F;
         float markerWidth = row.voiceActivated ? Render2D.textWidth(TEXT_FONT, "[Голос.]", 4.0F) + 3.0F : 0.0F;
         String visibleName = this.trimToWidth(row.name, TEXT_FONT, 6.0F, bindX - nameX - markerWidth - 5.0F);
         Render2D.text(TEXT_FONT, visibleName, nameX, currentY + 1.5F, 6.0F, ColorUtil.multAlpha(TEXT_COLOR, rowAlpha));
         if (row.voiceActivated) {
            float markerX = nameX + Render2D.textWidth(TEXT_FONT, visibleName, 6.0F) + 3.0F;
            int markerColor = ColorUtil.rgba(247, 133, 255, Math.round(235.0F * rowAlpha));
            Render2D.text(TEXT_FONT, "[Голос.]", markerX, currentY + 2.5F, 4.0F, markerColor);
         }

         Render2D.text(TEXT_FONT, row.bind, bindX + 2.0F, currentY + 1.0F, row.bindSize, ColorUtil.rgba(255, 255, 255, Math.round(245.0F * rowAlpha)));
      }
   }

   private HotKeys.RowEntry row(String key, String name, String bind, float bindSize, boolean voiceActivated, float targetY) {
      for (HotKeys.RowEntry entry : this.rowEntries) {
         if (entry.key.equals(key)) {
            entry.name = name;
            entry.bind = bind;
            entry.bindSize = bindSize;
            entry.voiceActivated = voiceActivated;
            return entry;
         }
      }

      HotKeys.RowEntry entryx = new HotKeys.RowEntry(key, name, bind, bindSize, voiceActivated);
      entryx.alpha.set(0.0);
      entryx.y.set((double)(targetY + 4.0F));
      this.rowEntries.add(entryx);
      return entryx;
   }

   private static record HotKeysState(List<HotKeys.RowState> rows, float iconAlpha, float alpha, float x, float y, float width, float height) {
   }

   private static final class RowEntry {
      private final String key;
      private final SmoothAnimation alpha = new SmoothAnimation();
      private final SmoothAnimation y = new SmoothAnimation();
      private String name;
      private String bind;
      private float bindSize;
      private boolean voiceActivated;
      private boolean active;

      private RowEntry(String key, String name, String bind, float bindSize, boolean voiceActivated) {
         this.key = key;
         this.name = name;
         this.bind = bind;
         this.bindSize = bindSize;
         this.voiceActivated = voiceActivated;
      }
   }

   private static record RowState(String name, String bind, float bindSize, boolean voiceActivated, float offset, float alpha) {
   }
}
