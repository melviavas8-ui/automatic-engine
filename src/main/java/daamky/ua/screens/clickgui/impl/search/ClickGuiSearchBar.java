package daamky.ua.screens.clickgui.impl.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.settings.Setting;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class ClickGuiSearchBar {
   private static final int MAX_RESULTS = 7;
   private static final float WIDTH = 200.0F;
   private static final float HEIGHT = 21.0F;
   private static final float RESULT_HEIGHT = 21.0F;
   private static final float TOP = 12.0F;
   private static final float OPEN_OFFSET = 18.0F;
   private static final Comparator<ClickGuiSearchBar.ScoredResult> RESULT_COMPARATOR = Comparator.comparingInt(ClickGuiSearchBar.ScoredResult::score)
      .reversed()
      .thenComparing(result -> result.result().module().getName(), String.CASE_INSENSITIVE_ORDER)
      .thenComparing(result -> result.result().settingName() == null ? "" : result.result().settingName(), String.CASE_INSENSITIVE_ORDER);
   private final SmoothAnimation focusAnimation = new SmoothAnimation();
   private final List<ClickGuiSearchBar.Result> visibleResults = new ArrayList<>(7);
   private final List<ClickGuiSearchBar.ScoredResult> scoredResults = new ArrayList<>(64);
   private String query = "";
   private String lastNormalizedQuery;
   private boolean focused;
   private boolean selectedAll;
   private float x;
   private float y;
   private float width;
   private float height;
   private float scale = 1.0F;

   public ClickGuiSearchBar() {
      this.focusAnimation.set(0.0);
   }

   public void render(DrawContext graphics, int screenWidth, float openProgress) {
      this.render(graphics, screenWidth, openProgress, 0.0F, 0.0F);
   }

   public void render(DrawContext graphics, int screenWidth, float openProgress, float offsetX, float offsetY) {
      this.focusAnimation.run(this.focused ? 1.0 : 0.0, 0.18, Easings.CUBIC_OUT, true);
      this.focusAnimation.update();
      this.refreshResults();
      float alpha = this.clamp(openProgress);
      if (!(alpha <= 0.01F)) {
         this.scale = 1.08F - 0.08F * alpha;
         this.width = 200.0F * this.scale;
         this.height = 21.0F * this.scale;
         this.x = ((float)screenWidth - this.width) / 2.0F + offsetX;
         this.y = 12.0F - (1.0F - alpha) * 18.0F + offsetY;
         float radius = 5.5F * this.scale;
         Render2D.blur(this.x, this.y, this.width, this.height, radius, 1.0F, 1.0F, this.color(255, 255, 255, 255, alpha));
         Render2D.blur(this.x, this.y, this.width, this.height, radius, 1.0F, 1.0F, this.color(0, 0, 0, 255, alpha));
         float textAlpha = alpha;
         boolean placeholderVisible = this.query.isEmpty() && !this.focused;
         String display = placeholderVisible ? ClientLanguage.tr("Search...") : this.query;
         int textColor = placeholderVisible ? this.color(255, 255, 255, 255, alpha) : this.color(255, 255, 255, 245, alpha);
         Render2D.pushScissor(graphics, this.x + 5.0F * this.scale, this.y, this.width - 25.0F * this.scale, this.height);

         try {
            if (this.selectedAll && this.focused && !this.query.isEmpty()) {
               float selectionX = this.x + 8.0F * this.scale;
               float textX = this.x + 10.0F * this.scale;
               float textWidth = Render2D.textWidth(FontType.SEMIBOLD, this.query, 8.0F * this.scale);
               float selectionRight = Math.min(textX + textWidth + 4.0F * this.scale, this.x + this.width - 25.0F * this.scale);
               Render2D.rect(
                  selectionX,
                  this.y + 53.0F * this.scale,
                  Math.max(0.0F, selectionRight - selectionX),
                  13.0F * this.scale,
                  2.0F * this.scale,
                  this.color(155, 162, 176, 80, textAlpha)
               );
            }

            Render2D.text(FontType.SEMIBOLD, display, this.x + 10.0F * this.scale, this.y + 6.0F * this.scale, 8.0F * this.scale, textColor);
            this.renderCaret(display, alpha);
         } finally {
            Render2D.popScissor(graphics);
         }

         Render2D.text(
            FontType.ICONS,
            "U",
            this.x + this.width - 20.0F * this.scale,
            this.y + 1.5F * this.scale,
            16.0F * this.scale,
            this.color(255, 255, 255, 255, alpha)
         );
         this.renderDropdown(graphics, alpha);
      }
   }

   public boolean mouseClicked(Click event, boolean doubled, Consumer<ClickGuiSearchBar.Result> onSelect) {
      double mouseX = event.x();
      double mouseY = event.y();
      if (this.hovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
         if (event.button() == 0) {
            this.focused = true;
         }

         return true;
      } else {
         if (this.dropdownOpen()) {
            float resultHeight = 21.0F * this.scale;
            float dropdownY = this.y + this.height + 4.0F * this.scale;

            for (int i = 0; i < this.visibleResults.size(); i++) {
               float rowY = dropdownY + (float)i * resultHeight;
               if (this.hovered(mouseX, mouseY, this.x, rowY, this.width, resultHeight)) {
                  if (event.button() == 0) {
                     this.focused = false;
                     ClickGuiSearchBar.Result result = this.visibleResults.get(i);
                     if (onSelect != null) {
                        onSelect.accept(result);
                     }
                  }

                  return true;
               }
            }
         }

         if (event.button() != 0) {
            return false;
         } else {
            this.focused = false;
            this.selectedAll = false;
            return false;
         }
      }
   }

   public boolean keyPressed(KeyInput event, Consumer<ClickGuiSearchBar.Result> onSelect) {
      if (!this.focused) {
         return false;
      } else {
         int key = event.key();
         boolean ctrl = (event.modifiers() & 2) != 0;
         if (ctrl && key == 65) {
            this.selectedAll = !this.query.isEmpty();
            return true;
         } else if (key == 256) {
            this.focused = false;
            this.selectedAll = false;
            return true;
         } else if (key == 259) {
            if (this.selectedAll) {
               this.query = "";
               this.selectedAll = false;
            } else if (!this.query.isEmpty()) {
               this.query = this.query.substring(0, this.query.length() - 1);
            }

            return true;
         } else if (key == 261) {
            this.query = "";
            this.selectedAll = false;
            return true;
         } else if (key != 257 && key != 335) {
            return true;
         } else {
            this.refreshResults();
            if (!this.visibleResults.isEmpty() && onSelect != null) {
               onSelect.accept(this.visibleResults.getFirst());
            }

            return true;
         }
      }
   }

   public boolean charTyped(CharInput event) {
      if (this.focused && event.isValidChar()) {
         String text = event.asString();
         if (text != null && !text.isBlank() && this.query.length() < 32) {
            if (this.selectedAll) {
               this.query = "";
               this.selectedAll = false;
            }

            if (this.query.length() + text.length() <= 32) {
               this.query = this.query + text;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean isFocused() {
      return this.focused;
   }

   private void renderCaret(String display, float alpha) {
      float focus = this.focusAnimation.get();
      if (!(focus <= 0.01F) && !this.selectedAll) {
         long now = System.currentTimeMillis();
         float blink = (float)(0.35 + 0.65 * Math.abs(Math.sin((double)now / 240.0)));
         float textWidth = this.query.isEmpty() ? 0.0F : Render2D.textWidth(FontType.SEMIBOLD, display, 8.0F * this.scale);
         float maxCaretX = this.x + this.width - 25.0F * this.scale;
         float caretX = Math.min(this.x + 10.0F * this.scale + textWidth + 1.0F * this.scale, maxCaretX);
         Render2D.rect(
            caretX, this.y + 5.5F * this.scale, 1.0F * this.scale, 11.0F * this.scale, 0.5F * this.scale, this.color(255, 255, 255, 235, alpha * focus * blink)
         );
      }
   }

   private void renderDropdown(DrawContext graphics, float alpha) {
      if (this.dropdownOpen()) {
         float resultHeight = 21.0F * this.scale;
         float dropdownY = this.y + this.height + 5.0F * this.scale;
         float dropdownHeight = (float)this.visibleResults.size() * resultHeight;
         float radius = 6.0F * this.scale;
         float dropdownAlpha = alpha;
         Render2D.blur(this.x, dropdownY, this.width, dropdownHeight, radius, 1.0F, 1.0F, this.color(255, 255, 255, 255, alpha));
         Render2D.glass(
            this.x,
            dropdownY,
            this.width,
            dropdownHeight,
            radius,
            this.color(0, 0, 0, 118, alpha),
            alpha,
            0.1F,
            this.color(0, 0, 0, 168, alpha),
            0.98F * alpha,
            false,
            0.74F * alpha,
            0.002F * alpha,
            1.0F,
            0.0F
         );
         Render2D.glassOutline(
            this.x,
            dropdownY,
            this.width,
            dropdownHeight,
            radius,
            0.25F * this.scale,
            this.color(255, 255, 255, 135, alpha),
            0.75F,
            35.0F,
            this.color(255, 255, 255, 170, alpha),
            0.85F * alpha,
            false,
            0.75F,
            0.0015F,
            1.0F,
            0.0F
         );
         Render2D.pushScissor(graphics, this.x, dropdownY, this.width, dropdownHeight);

         try {
            for (int i = 0; i < this.visibleResults.size(); i++) {
               ClickGuiSearchBar.Result result = this.visibleResults.get(i);
               float rowY = dropdownY + (float)i * resultHeight + 2.5F * this.scale;
               int mainColor = this.color(255, 255, 255, 238, dropdownAlpha);
               int metaColor = this.color(165, 165, 172, 205, dropdownAlpha);
               if (result.settingName() == null) {
                  String meta = ClientLanguage.tr(result.module().getCategory().getDisplayName());
                  float metaWidth = Render2D.textWidth(FontType.SEMIBOLD, meta, 5.8F * this.scale);
                  Render2D.text(
                     FontType.SEMIBOLD,
                     ClientLanguage.tr(result.module().getName()),
                     this.x + 9.0F * this.scale,
                     rowY + 4.0F * this.scale,
                     6.8F * this.scale,
                     mainColor
                  );
                  Render2D.text(
                     FontType.SEMIBOLD, meta, this.x + this.width - metaWidth - 9.0F * this.scale, rowY + 4.7F * this.scale, 5.8F * this.scale, metaColor
                  );
               } else {
                  String title = ClientLanguage.tr(result.settingName());
                  String meta = ClientLanguage.tr(result.module().getName()) + " " + ClientLanguage.tr("setting");
                  Render2D.text(FontType.SEMIBOLD, title, this.x + 9.0F * this.scale, rowY + 3.0F * this.scale, 6.7F * this.scale, mainColor);
                  Render2D.text(FontType.SEMIBOLD, meta, this.x + 9.0F * this.scale, rowY + 11.0F * this.scale, 5.2F * this.scale, metaColor);
               }
            }
         } finally {
            Render2D.popScissor(graphics);
         }
      }
   }

   private void refreshResults() {
      String normalizedQuery = this.normalize(this.query);
      if (!normalizedQuery.equals(this.lastNormalizedQuery)) {
         this.lastNormalizedQuery = normalizedQuery;
         this.visibleResults.clear();
         this.scoredResults.clear();
         if (!normalizedQuery.isBlank()) {
            ModuleManager moduleManager = Manager.getModules();
            if (moduleManager != null) {
               for (Module module : moduleManager.getModules()) {
                  this.addScored(this.scoredResults, module, null, normalizedQuery, module.getName());
                  this.addScored(this.scoredResults, module, null, normalizedQuery, ClientLanguage.tr(module.getName()));

                  for (Setting<?> setting : module.getSettings()) {
                     if (setting.isVisible()) {
                        this.addScored(this.scoredResults, module, setting.getName(), normalizedQuery, setting.getName());
                        this.addScored(this.scoredResults, module, setting.getName(), normalizedQuery, ClientLanguage.tr(setting.getName()));
                     }
                  }
               }

               this.scoredResults.sort(RESULT_COMPARATOR);

               for (ClickGuiSearchBar.ScoredResult scoredResult : this.scoredResults) {
                  if (this.visibleResults.size() == 7) {
                     break;
                  }

                  this.visibleResults.add(scoredResult.result());
               }
            }
         }
      }
   }

   private void addScored(List<ClickGuiSearchBar.ScoredResult> scored, Module module, String settingName, String normalizedQuery, String candidate) {
      int score = this.score(normalizedQuery, this.normalize(candidate));
      if (score > 0) {
         scored.add(new ClickGuiSearchBar.ScoredResult(new ClickGuiSearchBar.Result(module, settingName), score + (settingName == null ? 15 : 0)));
      }
   }

   private int score(String query, String candidate) {
      if (!query.isEmpty() && !candidate.isEmpty()) {
         if (candidate.equals(query)) {
            return 1000;
         } else if (candidate.startsWith(query)) {
            return 850 - candidate.length();
         } else {
            int index = candidate.indexOf(query);
            if (index >= 0) {
               return 650 - index * 8 - candidate.length();
            } else {
               int matched = 0;
               int lastIndex = -1;
               int penalty = 0;

               for (int i = 0; i < query.length(); i++) {
                  int next = candidate.indexOf(query.charAt(i), lastIndex + 1);
                  if (next < 0) {
                     return 0;
                  }

                  matched++;
                  penalty += Math.max(0, next - lastIndex - 1);
                  lastIndex = next;
               }

               return 420 + matched * 18 - penalty * 6 - candidate.length();
            }
         }
      } else {
         return 0;
      }
   }

   private boolean dropdownOpen() {
      return this.focused && !this.query.isBlank() && !this.visibleResults.isEmpty();
   }

   private String normalize(String value) {
      return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
   }

   private boolean hovered(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
   }

   private int color(int red, int green, int blue, int alpha, float alphaMultiplier) {
      return ColorUtil.rgba(red, green, blue, (int)((float)alpha * alphaMultiplier));
   }

   private float clamp(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   public static record Result(Module module, String settingName) {
   }

   private static record ScoredResult(ClickGuiSearchBar.Result result, int score) {
   }
}
