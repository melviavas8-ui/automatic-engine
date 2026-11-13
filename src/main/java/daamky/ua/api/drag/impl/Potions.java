package daamky.ua.api.drag.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.blur.BuiltBlur;
import daamky.ua.utils.render.ui.font.FontType;

public final class Potions extends HudPanel {
   private static final float NO_BLUR_RECT_ALPHA_SCALE = 0.78431374F;
   private static final float ROW_STEP = 11.0F;
   private static final float SECTION_STEP = 8.0F;
   private static final float GROUP_GAP = 3.0F;
   private static final float PANEL_ANIM = 0.24F;
   private static final float ROW_ANIM = 0.22F;
   private static final List<RegistryEntry<StatusEffect>> PREVIEW_POSITIVE_EFFECTS = List.of(StatusEffects.SPEED);
   private static final List<RegistryEntry<StatusEffect>> PREVIEW_NEGATIVE_EFFECTS = List.of(StatusEffects.POISON);
   private static final Comparator<StatusEffectInstance> EFFECT_NAME_COMPARATOR = Comparator.comparing(
      effect -> ((StatusEffect)effect.getEffectType().value()).getName().getString()
   );
   private static final Comparator<Potions.RowState> ROW_STATE_COMPARATOR = Comparator.comparingDouble(Potions.RowState::offset);
   private final List<Potions.RowEntry> rowEntries = new ArrayList<>();
   private final List<StatusEffectInstance> effects = new ArrayList<>();
   private final List<StatusEffectInstance> positiveEffects = new ArrayList<>();
   private final List<StatusEffectInstance> negativeEffects = new ArrayList<>();
   private final List<Potions.RowState> rowStates = new ArrayList<>();
   private final SmoothAnimation panelAnimation = new SmoothAnimation();
   private final SmoothAnimation iconAlphaAnimation = new SmoothAnimation();
   private boolean iconAlphaForward = true;
   private long previewIconSecond = -1L;
   private RegistryEntry<StatusEffect> previewPositiveEffect = StatusEffects.SPEED;
   private RegistryEntry<StatusEffect> previewNegativeEffect = StatusEffects.POISON;

   public Potions() {
      super("potions", "Potions", 300.0F, 100.0F, 80.0F, 23.0F);
   }

   @Override
   public void render() {
      Potions.PotionsState state = this.logics();
      if (state != null) {
         this.renderPotions(state);
      }
   }

   private Potions.PotionsState logics() {
      this.effects.clear();
      if (this.mc.player != null) {
         for (StatusEffectInstance effect : this.mc.player.getStatusEffects()) {
            if (effect.shouldShowIcon()) {
               this.effects.add(effect);
            }
         }
      }

      if (this.effects.size() > 1) {
         this.effects.sort(EFFECT_NAME_COMPARATOR);
      }

      this.positiveEffects.clear();
      this.negativeEffects.clear();

      for (StatusEffectInstance effectx : this.effects) {
         if (this.isNegativeEffect(effectx)) {
            this.negativeEffects.add(effectx);
         } else {
            this.positiveEffects.add(effectx);
         }
      }

      boolean preview = this.effects.isEmpty() && this.editPreview();
      boolean targetVisible = !this.effects.isEmpty() || preview;
      this.drag.hitExpansion(0.0F, 17.0F, 0.0F, 0.0F);
      this.panelAnimation.update();
      this.panelAnimation.run(targetVisible ? 1.0 : 0.0, 0.24F, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
      this.iconAlphaAnimation.update();
      if (!this.iconAlphaAnimation.isAlive()) {
         this.iconAlphaAnimation.run(this.iconAlphaForward ? 1.0 : 0.0, 1.55, Easings.EXPO_IN_OUT);
         this.iconAlphaForward = !this.iconAlphaForward;
      }

      for (Potions.RowEntry entry : this.rowEntries) {
         entry.active = false;
         entry.alpha.update();
         entry.y.update();
      }

      float targetY = 0.0F;
      if (preview) {
         this.updatePreviewEffects();
         Potions.RowEntry positiveSection = this.row("__section_positive", "Positive", "", "", 5.5F, targetY, null, -1, true);
         positiveSection.active = true;
         positiveSection.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
         positiveSection.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
         targetY += 8.0F;
         Potions.RowEntry entry = this.row("__preview_positive", "Example", "", "**:**", 6.0F, targetY, this.previewPositiveEffect, -1, false);
         entry.active = true;
         entry.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
         entry.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
         targetY += 14.0F;
         Potions.RowEntry negativeSection = this.row("__section_negative", "Negative", "", "", 5.5F, targetY, null, -1, true);
         negativeSection.active = true;
         negativeSection.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
         negativeSection.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
         targetY += 8.0F;
         Potions.RowEntry negativeEntry = this.row("__preview_negative", "Example", "", "**:**", 6.0F, targetY, this.previewNegativeEffect, -1, false);
         negativeEntry.active = true;
         negativeEntry.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
         negativeEntry.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
         targetY += 11.0F;
      } else {
         if (!this.positiveEffects.isEmpty()) {
            Potions.RowEntry section = this.row("__section_positive", "Positive", "", "", 5.5F, targetY, null, -1, true);
            section.active = true;
            section.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
            section.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
            targetY += 8.0F;
         }

         for (StatusEffectInstance effectxx : this.positiveEffects) {
            String name = ((StatusEffect)effectxx.getEffectType().value()).getName().getString();
            String level = this.level(effectxx);
            String duration = this.formatPotionDurationTicks(effectxx.getDuration());
            Potions.RowEntry entry = this.row(
               "positive:" + name + ":" + effectxx.getAmplifier(),
               name,
               level,
               duration,
               6.0F,
               targetY,
               effectxx.getEffectType(),
               effectxx.getDuration(),
               false
            );
            entry.active = true;
            entry.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
            entry.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
            targetY += 11.0F;
         }

         if (!this.positiveEffects.isEmpty() && !this.negativeEffects.isEmpty()) {
            targetY += 3.0F;
         }

         if (!this.negativeEffects.isEmpty()) {
            Potions.RowEntry section = this.row("__section_negative", "Negative", "", "", 5.5F, targetY, null, -1, true);
            section.active = true;
            section.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
            section.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
            targetY += 8.0F;
         }

         for (StatusEffectInstance effectxx : this.negativeEffects) {
            String name = ((StatusEffect)effectxx.getEffectType().value()).getName().getString();
            String level = this.level(effectxx);
            String duration = this.formatPotionDurationTicks(effectxx.getDuration());
            Potions.RowEntry entry = this.row(
               "negative:" + name + ":" + effectxx.getAmplifier(),
               name,
               level,
               duration,
               6.0F,
               targetY,
               effectxx.getEffectType(),
               effectxx.getDuration(),
               false
            );
            entry.active = true;
            entry.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
            entry.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
            targetY += 11.0F;
         }
      }

      for (Potions.RowEntry entry : this.rowEntries) {
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
         float targetHeight = Math.max(23.0F, targetY + 9.0F);

         for (Potions.RowEntry entryx : this.rowEntries) {
            if (entryx.alpha.get() > 0.01F || entryx.active) {
               if (entryx.section) {
                  width = Math.max(width, Render2D.textWidth(TEXT_FONT, entryx.name, 5.5F) + 22.0F);
                  targetHeight = Math.max(targetHeight, entryx.y.get() + 16.0F);
               } else {
                  width = Math.max(width, Render2D.textWidth(TEXT_FONT, entryx.name, 6.0F) + Render2D.textWidth(TEXT_FONT, entryx.level, 6.0F) + 65.0F);
                  targetHeight = Math.max(targetHeight, entryx.y.get() + 20.0F);
               }
            }
         }

         this.size(width, targetHeight);
         this.drag.size(this.drag.width(), (float)Math.ceil((double)targetHeight));
         this.rowStates.clear();

         for (Potions.RowEntry entryxx : this.rowEntries) {
            float alpha = entryxx.alpha.get();
            if (alpha > 0.01F || entryxx.active) {
               this.rowStates
                  .add(
                     new Potions.RowState(
                        entryxx.section,
                        entryxx.effect,
                        entryxx.name,
                        entryxx.level,
                        entryxx.value,
                        entryxx.valueSize,
                        entryxx.remainingTicks,
                        entryxx.y.get(),
                        alpha
                     )
                  );
            }
         }

         if (this.rowStates.size() > 1) {
            this.rowStates.sort(ROW_STATE_COMPARATOR);
         }

         return new Potions.PotionsState(
            this.rowStates, this.iconAlphaAnimation.get(), panelAlpha, this.drag.x(), this.drag.y(), this.drag.width(), this.drag.height()
         );
      }
   }

   private void renderPotions(Potions.PotionsState state) {
      Hud hud = Hud.getInstance();
      int hudColor = hud != null ? hud.iconColor() : ColorUtil.rgba(247, 133, 255, 255);
      int fadedHudColor = ColorUtil.multAlpha(hudColor, state.alpha);
      float headerY = state.y - 17.0F;
      float headerHeight = 17.0F;
      float rowY = state.y + 5.0F;
      String headerText = "Potions";
      float headerTextSize = 6.0F;
      float headerTextX = state.x + 10.0F;
      float headerTextY = headerY + (headerHeight - headerTextSize) * 0.5F - 0.5F;
      float headerTextWidth = Render2D.textWidth(TEXT_FONT, headerText, headerTextSize);
      float headerTextDotX = headerTextX + headerTextWidth + 3.0F;
      float iconSize = 8.0F;
      float iconWidth = Render2D.textWidth(FontType.MAINMENUSCREEN, "q", iconSize);
      float iconX = state.x + state.width - iconWidth - 7.0F;
      float iconY = headerY + (headerHeight - iconSize) * 0.5F + 0.5F;
      float glowSize = 22.0F;
      float glowX = iconX + iconWidth * 0.5F - glowSize * 0.5F;
      float glowY = headerY + (headerHeight - glowSize) * 0.5F;
      float sideRectY = headerY + (headerHeight - 9.0F) * 0.5F + 0.5F;
      float sideDotY = headerY + (headerHeight - 4.0F) * 0.5F + 0.5F;
      int iconAlpha = 255;
      int glowAlpha = Math.round(128.0F * state.alpha);
      int glow = ColorUtil.rgba(247, 133, 255, glowAlpha);
      int purple = ColorUtil.rgba(247, 133, 255, Math.round((float)iconAlpha * state.alpha));
      int headerTextColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * state.alpha));
      int rowRectColor = ColorUtil.rgba(247, 133, 255, Math.round(255.0F * state.alpha));
      renderHudBackground(
         Render2D.blurBuilder()
            .rectangle(state.x, headerY, state.width, headerHeight)
            .radius(4.0F, 4.0F, 2.0F, 2.0F)
            .blurRadius(15.0F)
            .smoothness(1.2F)
            .color(ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)))
            .build()
      );
      renderHudBackground(
         Render2D.blurBuilder()
            .rectangle(state.x, state.y, state.width, state.height)
            .radius(2.0F, 2.0F, 4.0F, 4.0F)
            .blurRadius(15.0F)
            .smoothness(1.2F)
            .color(ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)))
            .build()
      );
      if (glowAlpha > 0) {
         renderHudGlow("daamky:textures/particles/ghost-glow.png", glowX - 13.0F, glowY + 0.5F, glowSize + 25.0F, glowSize, 0.0F, fadedHudColor);
      }

      Render2D.rect(state.x, sideRectY, 2.5F, 9.0F, 0.0F, 2.0F, 2.0F, 0.0F, fadedHudColor);
      Render2D.rect(state.x + 3.5F, sideDotY, 4.0F, 4.0F, 4.0F, fadedHudColor);
      Render2D.rect(headerTextDotX, sideDotY, 4.0F, 4.0F, 4.0F, fadedHudColor);
      Render2D.text(TEXT_FONT, headerText, headerTextX, headerTextY, headerTextSize, headerTextColor);
      Render2D.text(FontType.MAINMENUSCREEN, "q", iconX, iconY, iconSize, fadedHudColor);

      for (Potions.RowState row : state.rows) {
         float rowAlpha = state.alpha * row.alpha * this.expiringAlpha(row.remainingTicks);
         float currentY = rowY + row.offset;
         if (row.section) {
            float sectionTextX = state.x + 4.0F;
            float sectionTextY = currentY - 0.5F;
            float sectionLineX = sectionTextX + Render2D.textWidth(TEXT_FONT, row.name, row.valueSize) + 2.0F;
            Render2D.text(TEXT_FONT, row.name, sectionTextX, sectionTextY, row.valueSize, ColorUtil.rgba(128, 128, 128, Math.round(245.0F * rowAlpha)));
            Render2D.rect(
               sectionLineX,
               currentY + 3.0F,
               Math.max(0.0F, state.x + state.width - sectionLineX),
               1.0F,
               1.0F,
               ColorUtil.rgba(128, 128, 128, Math.round(35.0F * rowAlpha))
            );
         } else {
            float effectIconSize = 8.0F;
            float effectIconX = state.x + 5.0F;
            float effectIconY = currentY + 2.0F;
            float timerX = state.x + state.width - 27.0F;
            float levelWidth = row.level.isBlank() ? 0.0F : Render2D.textWidth(TEXT_FONT, row.level, 6.0F);
            float nameX = state.x + 18.5F;
            float nameWidth = timerX - nameX - levelWidth - (row.level.isBlank() ? 5.0F : 8.0F);
            String visibleName = this.trimToWidth(row.name, TEXT_FONT, 6.0F, nameWidth);
            float levelX = nameX + Render2D.textWidth(TEXT_FONT, visibleName, 6.0F) + 2.0F;
            Render2D.effectIcon(row.effect, effectIconX, effectIconY, effectIconSize, ColorUtil.rgba(255, 255, 255, Math.round(245.0F * rowAlpha)));
            Render2D.rect(effectIconX + 9.5F, effectIconY + 2.5F, 3.0F, 3.0F, 4.0F, ColorUtil.rgba(128, 128, 128, Math.round(245.0F * rowAlpha)));
            Render2D.text(TEXT_FONT, visibleName, nameX, currentY + 1.7F, 6.0F, ColorUtil.multAlpha(TEXT_COLOR, rowAlpha));
            if (!row.level.isBlank()) {
               Render2D.text(TEXT_FONT, row.level, levelX, currentY + 1.7F, 6.0F, ColorUtil.rgba(128, 128, 128, Math.round(245.0F * rowAlpha)));
            }

            this.renderTimer(row.value, timerX, currentY + 1.5F, row.valueSize, rowAlpha);
         }
      }
   }

   private Potions.RowEntry row(
      String key,
      String name,
      String level,
      String value,
      float valueSize,
      float targetY,
      RegistryEntry<StatusEffect> effect,
      int remainingTicks,
      boolean section
   ) {
      for (Potions.RowEntry entry : this.rowEntries) {
         if (entry.key.equals(key)) {
            entry.section = section;
            entry.effect = effect;
            entry.name = name;
            entry.level = level;
            entry.value = value;
            entry.valueSize = valueSize;
            entry.remainingTicks = remainingTicks;
            return entry;
         }
      }

      Potions.RowEntry entryx = new Potions.RowEntry(key, section, effect, name, level, value, valueSize, remainingTicks);
      entryx.alpha.set(0.0);
      entryx.y.set((double)(targetY + 4.0F));
      this.rowEntries.add(entryx);
      return entryx;
   }

   private float expiringAlpha(int remainingTicks) {
      if (remainingTicks >= 0 && remainingTicks <= 100) {
         float pulse = (float)((Math.sin((double)System.currentTimeMillis() / 135.0) + 1.0) * 0.5);
         return (155.0F + pulse * 100.0F) / 255.0F;
      } else {
         return 1.0F;
      }
   }

   private void renderTimer(String text, float x, float y, float size, float alpha) {
      float cellWidth = 3.25F;
      int color = ColorUtil.rgba(255, 255, 255, Math.round(245.0F * alpha));

      for (int i = 0; i < text.length(); i++) {
         String character = timerChar(text.charAt(i));
         float characterWidth = Render2D.textWidth(TEXT_FONT, character, size);
         float characterX = x + (float)i * cellWidth + (cellWidth - characterWidth) * 0.5F + 3.0F;
         Render2D.text(TEXT_FONT, character, characterX, y, size, color);
      }
   }

   private void updatePreviewEffects() {
      long second = System.currentTimeMillis() / 1000L;
      if (second != this.previewIconSecond) {
         this.previewIconSecond = second;
         this.previewPositiveEffect = PREVIEW_POSITIVE_EFFECTS.get(ThreadLocalRandom.current().nextInt(PREVIEW_POSITIVE_EFFECTS.size()));
         this.previewNegativeEffect = PREVIEW_NEGATIVE_EFFECTS.get(ThreadLocalRandom.current().nextInt(PREVIEW_NEGATIVE_EFFECTS.size()));
      }
   }

   private String formatPotionDurationTicks(int ticks) {
      if (ticks < 0) {
         return "**:**";
      } else {
         int totalSeconds = Math.max(0, ticks / 20);
         int minutes = Math.min(99, totalSeconds / 60);
         int seconds = totalSeconds % 60;
         return twoDigitsLocal(minutes) + ":" + twoDigitsLocal(seconds);
      }
   }

   private static void renderHudBackground(BuiltBlur blur) {
      if (blur != null) {
         if (hudBoolean("blur", true)) {
            Render2D.blur(blur);
         } else {
            Render2D.rect(
               blur.x(),
               blur.y(),
               blur.width(),
               blur.height(),
               blur.radiusTopLeft(),
               blur.radiusTopRight(),
               blur.radiusBottomRight(),
               blur.radiusBottomLeft(),
               noBlurRectColor(blur.color())
            );
         }
      }
   }

   private static void renderHudGlow(String texture, float x, float y, float width, float height, float radius, int color) {
      if (hudBoolean("glow", true)) {
         Render2D.image(texture, x, y, width, height, radius, color);
      }
   }

   private static boolean hudBoolean(String fieldName, boolean fallback) {
      try {
         Class<?> hudClass = Class.forName("daamky.ua.api.module.impl.visual.Hud");
         Object hud = hudClass.getMethod("getInstance").invoke(null);
         if (hud == null) {
            return fallback;
         } else {
            Field field = hudClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object setting = field.get(hud);
            return setting.getClass().getMethod("getValue").invoke(setting) instanceof Boolean booleanValue ? booleanValue : fallback;
         }
      } catch (RuntimeException | ReflectiveOperationException var8) {
         return fallback;
      }
   }

   private static int noBlurRectColor(int color) {
      return ColorUtil.multAlpha(color, 0.78431374F);
   }

   private static String timerChar(char character) {
      return Character.toString(character);
   }

   private static String twoDigitsLocal(int value) {
      return value < 10 ? "0" + value : Integer.toString(value);
   }

   private String level(StatusEffectInstance effect) {
      int level = effect.getAmplifier() + 1;
      return level <= 1 ? "" : Integer.toString(level);
   }

   private boolean isNegativeEffect(StatusEffectInstance effect) {
      return StatusEffects.SLOW_FALLING.equals(effect.getEffectType())
         || ((StatusEffect)effect.getEffectType().value()).getCategory() == StatusEffectCategory.HARMFUL;
   }

   private static record PotionsState(List<Potions.RowState> rows, float iconAlpha, float alpha, float x, float y, float width, float height) {
   }

   private static final class RowEntry {
      private final String key;
      private final SmoothAnimation alpha = new SmoothAnimation();
      private final SmoothAnimation y = new SmoothAnimation();
      private boolean section;
      private RegistryEntry<StatusEffect> effect;
      private String name;
      private String level;
      private String value;
      private float valueSize;
      private int remainingTicks;
      private boolean active;

      private RowEntry(
         String key, boolean section, RegistryEntry<StatusEffect> effect, String name, String level, String value, float valueSize, int remainingTicks
      ) {
         this.key = key;
         this.section = section;
         this.effect = effect;
         this.name = name;
         this.level = level;
         this.value = value;
         this.valueSize = valueSize;
         this.remainingTicks = remainingTicks;
      }
   }

   private static record RowState(
      boolean section,
      RegistryEntry<StatusEffect> effect,
      String name,
      String level,
      String value,
      float valueSize,
      int remainingTicks,
      float offset,
      float alpha
   ) {
   }
}
