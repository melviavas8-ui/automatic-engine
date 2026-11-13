package daamky.ua.screens.modernui.settings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.render.animation.modernfx.Decelerate;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.voice.VoiceBindManager;
import daamky.ua.voice.VoiceBindScreen;

public final class SettingCardController {
   private static final boolean MODULE_SETTINGS_ALWAYS_EXPANDED = true;
   private static final int MODULE_LIST_ANIMATION_MS = 2;
   private static final int MODULE_TOGGLE_ANIMATION_MS = 180;
   private static final int CATEGORY_SWITCH_TRANSITION_MS = 210;
   private static final float CATEGORY_RENDER_THRESHOLD = 0.02F;
   private static final float CATEGORY_SWITCH_SLIDE_DISTANCE = 26.0F;
   private static final float MODULE_CARD_RADIUS = 3.5F;
   private static final Map<ModuleCategory, SettingCardController.ScrollSnapshot> CATEGORY_SCROLLS = new EnumMap<>(ModuleCategory.class);
   private static final SettingCardController.ScrollSnapshot SEARCH_SCROLL = new SettingCardController.ScrollSnapshot();
   private final Map<Module, SettingCardController.ModuleState> moduleStates = new HashMap<>();
   private final Map<Setting<?>, SettingCardComponent<?>> settingComponents = new HashMap<>();
   private final SettingCardContext context = new SettingCardContext();
   private final Decelerate categorySwitchAnimation = MathUtils.createAnimation(210, Direction.FORWARDS);
   private float moduleScroll;
   private float moduleScrollTarget;
   private float moduleContentHeight;
   private boolean moduleClipReady;
   private boolean moduleListInitialized;
   private ModuleCategory activeCategory;
   private boolean activeSearchMode;
   private ModuleCategory pendingCategory;
   private boolean pendingSearchMode;
   private List<Module> displayedModules = List.of();
   private List<Module> pendingModules = List.of();
   private Module listeningModuleBind;
   private Module pendingFocusModule;
   private List<Module> lastRenderModules = List.of();
   private boolean snapNextModuleLayout;
   private int categoryTransitionDirection = 1;

   public SettingCardController() {
   }

   public void render(
      DrawContext graphics,
      float x,
      float y,
      float panelWidth,
      float panelHeight,
      ModuleCategory category,
      boolean searchMode,
      List<Module> modules,
      int mouseX,
      int mouseY
   ) {
      this.updateModuleScope(category, searchMode, modules == null ? List.of() : modules);
      this.renderModuleCards(graphics, x, y, panelWidth, panelHeight, this.displayedModules, 1.0F, mouseX, mouseY);
   }

   public boolean mouseClicked(Click event, boolean doubled) {
      if (this.listeningModuleBind != null) {
         this.finishModuleBind(KeyBind.mouse(event.button()));
         return true;
      } else if (this.context.listeningBind != null) {
         this.context.listeningBind.setValue(KeyBind.mouse(event.button()));
         this.context.listeningBind = null;
         return true;
      } else if (event.button() == 2 && this.handleModuleBindClick(event)) {
         return true;
      } else if (event.button() != 0 && event.button() != 1) {
         return false;
      } else if (this.dispatchOverlayClick(event)) {
         return true;
      } else if (this.dispatchSettingClick(event, doubled)) {
         return true;
      } else if (this.handleModuleStarClick(event)) {
         return true;
      } else if (this.handleModuleClick(event)) {
         return true;
      } else {
         if (this.context.focusedText != null) {
            this.context.focusedText = null;
         }

         return false;
      }
   }

   public boolean mouseReleased(Click event) {
      boolean consumed = false;

      for (SettingCardComponent<?> component : this.settingComponents.values()) {
         consumed |= component.mouseReleased(event, this.context);
      }

      return consumed;
   }

   public boolean mouseDragged(Click event, double dragX, double dragY) {
      for (SettingCardComponent<?> component : this.settingComponents.values()) {
         if (component.mouseDragged(event, dragX, dragY, this.context)) {
            return true;
         }
      }

      return false;
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.moduleClipReady
         && this.context
            .inside(mouseX, mouseY, this.context.moduleClipX, this.context.moduleClipY, this.context.moduleClipWidth, this.context.moduleClipHeight)) {
         for (SettingCardComponent<?> component : this.settingComponents.values()) {
            if (component.mouseScrolled(mouseX, mouseY, scrollY, this.context)) {
               return true;
            }
         }

         this.moduleScrollTarget = this.context
            .clamp(this.moduleScrollTarget - (float)scrollY * 20.0F, 0.0F, Math.max(0.0F, this.moduleContentHeight - this.context.moduleClipHeight));
         this.saveScrollScope();
         return true;
      } else {
         return false;
      }
   }

   public void focusModule(Module module) {
      if (module != null) {
         this.pendingFocusModule = module;
      }
   }

   public boolean keyPressed(KeyInput event) {
      if (this.listeningModuleBind != null) {
         if (event.key() != 256 && event.key() != 259 && event.key() != 261) {
            this.finishModuleBind(KeyBind.keyboard(event.key()));
         } else {
            this.finishModuleBind(KeyBind.NONE);
         }

         return true;
      } else if (this.context.listeningBind != null) {
         if (event.key() != 256 && event.key() != 259 && event.key() != 261) {
            this.context.listeningBind.setValue(KeyBind.keyboard(event.key()));
         } else {
            this.context.listeningBind.setValue(KeyBind.NONE);
         }

         this.context.listeningBind = null;
         return true;
      } else if (this.context.focusedText == null) {
         return false;
      } else {
         SettingCardComponent<?> component = this.settingComponents.get(this.context.focusedText);
         return component != null && component.keyPressed(event, this.context);
      }
   }

   public boolean charTyped(CharInput event) {
      if (this.context.focusedText == null) {
         return false;
      } else {
         SettingCardComponent<?> component = this.settingComponents.get(this.context.focusedText);
         return component != null && component.charTyped(event, this.context);
      }
   }

   private void renderModuleCards(
      DrawContext graphics, float x, float y, float panelWidth, float panelHeight, List<Module> modules, float categoryProgress, int mouseX, int mouseY
   ) {
      this.context.mouseX = (double)mouseX;
      this.context.mouseY = (double)mouseY;
      boolean firstListRender = !this.moduleListInitialized;
      if (this.modulesChanged(modules)) {
         this.lastRenderModules = new ArrayList<>(modules);
      }

      for (SettingCardController.ModuleState state : this.moduleStates.values()) {
         state.visible = false;
         state.includedThisFrame = false;
      }

      for (SettingCardComponent<?> component : this.settingComponents.values()) {
         component.hide();
      }

      for (Module module : modules) {
         for (Setting<?> setting : module.getSettings()) {
            SettingCardComponent<?> component = this.componentFor(setting);
            if (component != null) {
               component.updateVisibility();
            }
         }
      }

      this.context.moduleClipX = x + 53.0F;
      this.context.moduleClipY = y + 41.5F;
      this.context.moduleClipWidth = panelWidth - 60.0F;
      this.context.moduleClipHeight = panelHeight - 49.0F;
      this.moduleClipReady = true;
      float cardGap = 6.0F;
      float contentX = this.context.moduleClipX + 7.0F;
      float contentWidth = Math.max(0.0F, this.context.moduleClipWidth - 14.0F);
      int columnCount = this.columnCount(contentWidth, cardGap);
      float cardWidth = this.moduleCardWidth(contentWidth, columnCount, cardGap);
      List<SettingCardController.ModuleLayoutEntry> layoutEntries = this.layoutEntries(modules);
      boolean snapLayout = firstListRender || this.snapNextModuleLayout;
      float transitionProgress = this.context.clamp(this.categorySwitchAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      boolean transitionActive = transitionProgress < 0.999F;
      float incomingOffsetX = transitionActive ? (float)this.categoryTransitionDirection * (1.0F - transitionProgress) * 26.0F : 0.0F;
      float incomingAlpha = transitionActive ? 0.88F + 0.12F * transitionProgress : 1.0F;
      this.moduleContentHeight = this.measureModuleLayoutHeight(layoutEntries, columnCount, cardGap) + 15.0F;
      this.moduleScrollTarget = this.context.clamp(this.moduleScrollTarget, 0.0F, Math.max(0.0F, this.moduleContentHeight - this.context.moduleClipHeight));
      this.applyPendingModuleFocus(layoutEntries, columnCount, cardGap);
      if (snapLayout) {
         this.moduleScroll = this.moduleScrollTarget;
      } else {
         this.moduleScroll = this.moduleScroll + (this.moduleScrollTarget - this.moduleScroll) * 0.2F;
      }

      this.saveScrollScope();
      Render2D.pushScissor(graphics, this.context.moduleClipX, this.context.moduleClipY, this.context.moduleClipWidth, this.context.moduleClipHeight);
      this.renderModuleLayoutPass(
         graphics, x, y, panelWidth, modules, incomingOffsetX, incomingAlpha, mouseX, mouseY, categoryProgress, firstListRender, snapLayout, transitionActive
      );
      this.snapNextModuleLayout = false;
      if (!snapLayout) {
         this.renderExitingModules(graphics, categoryProgress * incomingAlpha, mouseX, mouseY);
      }

      for (SettingCardController.ModuleState state : this.moduleStates.values()) {
         float progress = this.context.clamp(state.entryAnimation.getOutput().floatValue(), 0.0F, 1.0F);
         state.includedLastFrame = state.includedThisFrame || progress > 0.01F;
         if (!state.includedLastFrame) {
            state.column = -1;
         }
      }

      this.moduleListInitialized = true;
      Render2D.popScissor(graphics);
      Render2D.flush();
      Render2D.beginFrame(graphics);
      this.renderFloatingOverlays(graphics, categoryProgress * incomingAlpha);
      Render2D.flush();
      Render2D.beginFrame(graphics);
   }

   private void renderModuleLayoutPass(
      DrawContext graphics,
      float x,
      float y,
      float panelWidth,
      List<Module> modules,
      float offsetX,
      float alpha,
      int mouseX,
      int mouseY,
      float categoryProgress,
      boolean firstListRender,
      boolean snapLayout,
      boolean flattenEntryAnimation
   ) {
      if (!(alpha <= 0.02F)) {
         if (modules.isEmpty()) {
            if (this.activeCategory == ModuleCategory.DISPLAY || this.activeCategory == ModuleCategory.WORLD) {
               String text = "Скоро...";
               float textSize = 12.0F;
               float textWidth = Render2D.textWidth(FontType.BOLD, text, textSize);
               float textX = this.context.moduleClipX + (this.context.moduleClipWidth - textWidth) * 0.5F;
               float textY = this.context.moduleClipY + this.context.moduleClipHeight * 0.42F;
               Render2D.text(FontType.BOLD, text, textX, textY, textSize, new Color(255, 255, 255, 255).getRGB());
            }
         } else {
            float startY = y + 48.0F + (1.0F - categoryProgress) * 10.0F - this.moduleScroll;
            float contentX = this.context.moduleClipX + 7.0F + offsetX;
            float contentWidth = Math.max(0.0F, this.context.moduleClipWidth - 14.0F);
            float cardGap = 6.0F;
            int columnCount = this.columnCount(contentWidth, cardGap);
            float cardWidth = this.moduleCardWidth(contentWidth, columnCount, cardGap);
            List<SettingCardController.ModuleLayoutEntry> layoutEntries = this.layoutEntries(modules);
            float[] columnOffsets = new float[columnCount];
            float[] columnLayoutOffsets = new float[columnCount];

            for (SettingCardController.ModuleLayoutEntry entry : layoutEntries) {
               SettingCardController.ModuleState state = this.moduleState(entry.module);
               int column = this.preferredColumn(state.column, columnLayoutOffsets);
               state.column = column;
               float cardX = contentX + (float)column * (cardWidth + cardGap);
               float cardY = startY + columnOffsets[column];
               state.includedThisFrame = true;
               if (firstListRender && !state.includedLastFrame) {
                  this.completeListAnimation(state.entryAnimation, Direction.FORWARDS);
               } else {
                  state.entryAnimation.setDirection(Direction.FORWARDS);
               }

               float entryAnimation = flattenEntryAnimation ? 1.0F : this.context.clamp(state.entryAnimation.getOutput().floatValue(), 0.0F, 1.0F);
               if (!snapLayout && state.positionReady) {
                  state.cardX = this.context.lerp(state.cardX, cardX, 0.24F);
                  state.cardY = this.context.lerp(state.cardY, cardY, 0.24F);
               } else {
                  state.cardX = cardX;
                  state.cardY = cardY;
                  state.positionReady = true;
               }

               state.cardWidth = cardWidth;
               state.cardHeight = entry.height;
               state.visible = state.cardY + entry.height >= this.context.moduleClipY
                  && state.cardY <= this.context.moduleClipY + this.context.moduleClipHeight;
               if (state.visible) {
                  this.renderModuleCard(graphics, entry.module, entry.settings, state, entryAnimation * alpha, mouseX, mouseY);
               }

               columnOffsets[column] += entry.height + cardGap;
               columnLayoutOffsets[column] += entry.layoutHeight + cardGap;
            }
         }
      }
   }

   private void renderModuleCard(
      DrawContext graphics, Module module, List<SettingCardComponent<?>> settings, SettingCardController.ModuleState state, float alpha, int mouseX, int mouseY
   ) {
      float cardX = state.cardX;
      float cardY = state.cardY;
      this.context.cardWidth = state.cardWidth;
      float previousAlpha = this.context.visualAlpha();
      this.context.setVisualAlpha(previousAlpha * alpha);
      this.renderInnerCard(cardX, cardY, state.cardWidth, state.cardHeight);
      this.renderModuleFocusHighlight(state);
      this.renderModuleHeader(graphics, module, state);
      float expandProgress = this.moduleExpandProgress(state);
      if (!settings.isEmpty() && !(expandProgress <= 0.01F)) {
         float settingY = cardY + 24.0F;
         float settingsClipHeight = Math.max(0.0F, state.cardHeight - 26.0F);
         Render2D.pushScissor(graphics, cardX + 4.0F, cardY + 22.0F, state.cardWidth - 8.0F, settingsClipHeight);
         this.context.setVisualAlpha(previousAlpha * alpha * expandProgress);

         for (SettingCardComponent<?> component : settings) {
            float height = component.animatedHeight(this.context);
            if (settingY + height >= this.context.moduleClipY - 5.0F && settingY <= this.context.moduleClipY + this.context.moduleClipHeight + 5.0F) {
               component.renderAnimated(graphics, this.context, cardX, settingY, mouseX, mouseY);
            }

            settingY += height;
         }

         Render2D.popScissor(graphics);

         for (SettingCardComponent<?> component : settings) {
            component.renderOverlay(graphics, this.context);
         }

         this.context.setVisualAlpha(previousAlpha);
      } else {
         this.context.setVisualAlpha(previousAlpha);
      }
   }

   private void renderExitingModules(DrawContext graphics, float categoryProgress, int mouseX, int mouseY) {
      for (SettingCardController.ModuleState state : this.moduleStates.values()) {
         if (!state.includedThisFrame && state.includedLastFrame) {
            state.entryAnimation.setDirection(Direction.BACKWARDS);
            float progress = this.context.clamp(state.entryAnimation.getOutput().floatValue(), 0.0F, 1.0F);
            if (!(progress <= 0.01F)) {
               state.visible = state.cardY + state.cardHeight >= this.context.moduleClipY
                  && state.cardY <= this.context.moduleClipY + this.context.moduleClipHeight;
               if (state.visible) {
                  this.renderModuleCard(graphics, state.module, this.visibleComponents(state.module), state, progress * categoryProgress, mouseX, mouseY);
               }
            }
         }
      }
   }

   private void renderFloatingOverlays(DrawContext graphics, float alpha) {
      if (!(alpha <= 0.02F)) {
         float previousAlpha = this.context.visualAlpha();
         this.context.setVisualAlpha(previousAlpha * alpha);

         for (SettingCardComponent<?> component : this.settingComponents.values()) {
            component.renderFloatingOverlay(graphics, this.context);
         }

         this.context.setVisualAlpha(previousAlpha);
      }
   }

   private void renderModuleFocusHighlight(SettingCardController.ModuleState state) {
      float progress = this.context.clamp(state.focusAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      if (state.focusAnimation.isFinished(Direction.FORWARDS)) {
         state.focusAnimation.setDirection(Direction.BACKWARDS);
      }

      if (!(progress <= 0.01F)) {
         float grow = 1.5F * progress;
         int fillAlpha = Math.round(38.0F * progress);
         int outlineAlpha = Math.round(130.0F * progress);
         Render2D.rect(
            state.cardX - grow,
            state.cardY - grow,
            state.cardWidth + grow * 2.0F,
            state.cardHeight + grow * 2.0F,
            5.0F,
            this.context.color(104, 112, 150, fillAlpha),
            this.context.color(138, 122, 154, fillAlpha),
            this.context.color(90, 96, 126, fillAlpha),
            this.context.color(104, 94, 126, fillAlpha)
         );
         Render2D.outline(
            state.cardX - grow,
            state.cardY - grow,
            state.cardWidth + grow * 2.0F,
            state.cardHeight + grow * 2.0F,
            5.0F,
            0.35F,
            this.context.color(255, 255, 255, outlineAlpha),
            this.context.color(255, 255, 255, outlineAlpha),
            this.context.color(255, 255, 255, outlineAlpha),
            this.context.color(255, 255, 255, outlineAlpha)
         );
      }
   }

   private void renderModuleHeader(DrawContext graphics, Module module, SettingCardController.ModuleState state) {
      float cardX = state.cardX;
      float cardY = state.cardY;
      state.toggleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
      float animation = this.context.clamp(state.toggleAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      float headerClipWidth = Math.max(0.0F, state.cardWidth - 59.0F);
      float togglePaddingRight = 10.0F;
      float toggleWidth = 17.0F;
      float starGap = 15.0F;
      float expandGap = 0.0F;
      float toggleX = cardX + state.cardWidth - toggleWidth - togglePaddingRight;
      float starX = toggleX - starGap;
      float expandX = starX - expandGap;
      Render2D.pushScissor(graphics, cardX + 5.0F, cardY + 4.0F, headerClipWidth, 15.0F);
      Render2D.text(FontType.HUD_ICONS, "o", cardX + 6.0F, cardY + 5.5F, 10.0F, this.context.color(128, 128, 128, 250));
      float moduleNameX = cardX + 18.0F;
      float moduleNameY = cardY + 7.0F;
      float moduleNameSize = 6.0F;
      Render2D.text(FontType.SEMIBOLD, ClientLanguage.tr(module.getName()), moduleNameX, moduleNameY, moduleNameSize, this.context.color(255, 255, 255, 255));
      this.renderModuleBindIndicator(module, state, moduleNameX, moduleNameY, moduleNameSize);
      this.renderModuleExpandIndicator(state, expandX, cardY + 7.0F);
      Render2D.popScissor(graphics);
      state.headerX = cardX + 6.0F;
      state.headerY = cardY + 4.0F;
      state.headerWidth = Math.max(0.0F, expandX - state.headerX - 4.0F);
      state.headerHeight = 15.0F;
      state.toggleX = toggleX;
      state.toggleY = cardY + 5.0F;
      state.toggleWidth = toggleWidth;
      state.toggleHeight = 11.0F;
      this.renderModuleToggle(state, animation);
      this.renderModuleStar(module, state, starX, cardY + 6.0F);
   }

   private void renderModuleToggle(SettingCardController.ModuleState state, float animation) {
      float x = state.toggleX;
      float y = state.toggleY;
      float width = state.toggleWidth;
      float height = state.toggleHeight;
      float radius = height * 0.4F;
      Render2D.rect(
         x,
         y,
         width,
         height,
         radius,
         new Color(70, 72, 82, 255).getRGB(),
         new Color(82, 84, 96, 255).getRGB(),
         new Color(50, 52, 60, 255).getRGB(),
         new Color(62, 64, 74, 255).getRGB()
      );
      if (animation > 0.01F) {
         Render2D.rect(
            x,
            y,
            width,
            height,
            radius,
            this.context.withAlpha(this.context.theme().accentBright(), animation),
            this.context.withAlpha(this.context.theme().accent(), animation),
            this.context.withAlpha(this.context.theme().accentDark(), animation),
            this.context.withAlpha(this.context.theme().accentDark(), animation)
         );
      }

      int outlineAlpha = Math.round(70.0F + 45.0F * animation);
      Render2D.outline(
         x,
         y,
         width,
         height,
         radius,
         0.25F,
         this.context.color(255, 255, 255, outlineAlpha),
         this.context.color(255, 255, 255, outlineAlpha),
         this.context.color(255, 255, 255, outlineAlpha),
         this.context.color(255, 255, 255, outlineAlpha)
      );
      float knobPadding = 2.0F;
      float knobSize = Math.max(8.0F, height - knobPadding * 2.0F);
      float knobStartX = x + 0.0F;
      float knobEndX = x + width - knobSize - 1.5F;
      float knobX = knobStartX + (knobEndX - knobStartX) * animation;
      float knobTravel = Math.max(0.0F, width - knobSize - knobPadding * 1.5F);
      knobX = x + knobPadding + knobTravel * animation;
      float knobY = y + (height - knobSize) * 0.5F;
      float knobRadius = knobSize * 0.38F;
      if (animation > 0.01F) {
      }

      int knobTone = Math.round(230.0F + 25.0F * animation);
      Render2D.rect(knobX, knobY + 0.55F, knobSize, knobSize, knobRadius, this.context.color(0, 0, 0, 42));
      Render2D.rect(knobX, knobY, knobSize, knobSize, knobRadius, this.context.color(knobTone, knobTone, knobTone, 255));
      Render2D.rect(
         knobX + 1.6F, knobY + 1.25F, Math.max(1.0F, knobSize - 3.2F), 1.0F, 0.5F, this.context.color(255, 255, 255, Math.round(65.0F + 70.0F * animation))
      );
   }

   private boolean dispatchOverlayClick(Click event) {
      for (SettingCardComponent<?> component : this.settingComponents.values()) {
         if (component.mouseClickedOverlay(event, this.context)) {
            return true;
         }
      }

      return false;
   }

   private boolean dispatchSettingClick(Click event, boolean doubled) {
      for (SettingCardComponent<?> component : this.settingComponents.values()) {
         if (component.state().visible && component.mouseClicked(event, doubled, this.context)) {
            return true;
         }
      }

      return false;
   }

   private boolean handleModuleClick(Click event) {
      for (SettingCardController.ModuleState state : this.moduleStates.values()) {
         if (state.visible
            && event.button() == 0
            && this.context.inside(event.x(), event.y(), state.toggleX, state.toggleY, state.toggleWidth, state.toggleHeight)) {
            state.module.toggle();
            return true;
         }
      }

      return false;
   }

   private boolean handleModuleStarClick(Click event) {
      if (event.button() != 0) {
         return false;
      } else {
         for (SettingCardController.ModuleState state : this.moduleStates.values()) {
            if (state.visible && this.context.inside(event.x(), event.y(), state.starX, state.starY, state.starWidth, state.starHeight)) {
               state.module.setStarred(!state.module.isStarred());
               state.starAnimation.setDirection(state.module.isStarred() ? Direction.FORWARDS : Direction.BACKWARDS);
               return true;
            }
         }

         return false;
      }
   }

   private boolean handleModuleVoiceClick(Click event) {
      if (event.button() != 0) {
         return false;
      } else {
         for (SettingCardController.ModuleState state : this.moduleStates.values()) {
            if (state.visible && this.context.inside(event.x(), event.y(), state.micX, state.micY, state.micWidth, state.micHeight)) {
               MinecraftClient minecraft = MinecraftClient.getInstance();
               minecraft.setScreen(new VoiceBindScreen(minecraft.currentScreen, state.module));
               return true;
            }
         }

         return false;
      }
   }

   private boolean handleModuleBindClick(Click event) {
      for (SettingCardController.ModuleState state : this.moduleStates.values()) {
         if (state.visible && this.context.inside(event.x(), event.y(), state.cardX, state.cardY, state.cardWidth, Math.min(22.0F, state.cardHeight))) {
            this.listeningModuleBind = state.module;
            state.bindAnimation.setDirection(Direction.FORWARDS);
            this.context.listeningBind = null;
            this.context.focusedText = null;
            return true;
         }
      }

      return false;
   }

   private void finishModuleBind(KeyBind bind) {
      Module module = this.listeningModuleBind;
      if (module != null) {
         SettingCardController.ModuleState state = this.moduleState(module);
         module.setBind(bind);
         state.bindAnimation.setDirection(bind != null && bind.isBound() ? Direction.FORWARDS : Direction.BACKWARDS);
         this.listeningModuleBind = null;
      }
   }

   private void renderModuleBindIndicator(Module module, SettingCardController.ModuleState state, float nameX, float nameY, float nameSize) {
      KeyBind bind = module.getBind();
      boolean listening = this.listeningModuleBind == module;
      boolean bound = bind != null && bind.isBound();
      boolean visible = listening || bound;
      state.bindAnimation.setDirection(visible ? Direction.FORWARDS : Direction.BACKWARDS);
      float progress = this.context.clamp(state.bindAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      if (!(progress <= 0.01F)) {
         float nameWidth = Render2D.textWidth(FontType.SEMIBOLD, ClientLanguage.tr(module.getName()), nameSize);
         float offsetX = (1.0F - progress) * -4.0F;
         float dotX = nameX + nameWidth + 3.0F + offsetX;
         float dotY = nameY + (nameSize - 1.5F) * 0.5F;
         int alpha = Math.round(255.0F * progress);
         int color = new Color(255, 255, 255, 255).getRGB();
         Render2D.rect(dotX, dotY, 3.0F, 3.0F, 1.5F, color);
         String bindText = bound && !listening ? MathUtils.shortBind(bind) : "...";
         Render2D.text(FontType.SEMIBOLD, bindText, dotX + 6.0F, nameY, nameSize, color);
      }
   }

   private void renderModuleStar(Module module, SettingCardController.ModuleState state, float x, float y) {
      state.starX = x - 2.0F;
      state.starY = y - 2.0F;
      state.starWidth = 14.0F;
      state.starHeight = 14.0F;
      state.starAnimation.setDirection(module.isStarred() ? Direction.FORWARDS : Direction.BACKWARDS);
      float progress = this.context.clamp(state.starAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      int grey = new Color(45, 45, 45, 255).getRGB();
      int topLeft = ColorUtil.lerpColor(grey, this.context.color(255, 224, 92, 255), progress);
      int topRight = ColorUtil.lerpColor(grey, this.context.color(255, 224, 92, 255), progress);
      int bottomRight = ColorUtil.lerpColor(grey, this.context.color(255, 181, 35, 255), progress);
      int bottomLeft = ColorUtil.lerpColor(grey, this.context.color(190, 132, 18, 255), progress);
      Render2D.image("daamky:textures/particles/star.png", x - 1.0F, y - 1.0F, 10.0F, 10.0F, 2.0F, topLeft, topRight, bottomRight, bottomLeft);
   }

   private void renderModuleMicrophone(Module module, SettingCardController.ModuleState state, float x, float y) {
      state.micX = x - 2.0F;
      state.micY = y - 2.0F;
      state.micWidth = 13.0F;
      state.micHeight = 15.0F;
      boolean configured = VoiceBindManager.getInstance().getModuleRecordingCount(module) > 0;
      int color = configured ? this.context.color(205, 145, 235, 255) : this.context.color(128, 128, 128, 255);
      Render2D.rect(x + 3.0F, y, 4.0F, 7.0F, 2.0F, color);
      Render2D.rect(x + 1.5F, y + 4.0F, 1.0F, 4.0F, 0.5F, color);
      Render2D.rect(x + 7.5F, y + 4.0F, 1.0F, 4.0F, 0.5F, color);
      Render2D.rect(x + 2.0F, y + 7.0F, 6.0F, 1.0F, 0.5F, color);
      Render2D.rect(x + 4.5F, y + 8.0F, 1.0F, 2.0F, 0.5F, color);
      Render2D.rect(x + 2.5F, y + 10.0F, 5.0F, 1.0F, 0.5F, color);
   }

   private void renderInnerCard(float x, float y, float width, float height) {
      Render2D.rect(
         x,
         y,
         width - 2.0F,
         height,
         3.5F,
         this.context.theme().surfaceBright().getRGB(),
         this.context.theme().surface().getRGB(),
         this.context.theme().surfaceDeep().getRGB(),
         this.context.theme().surfaceDark().getRGB()
      );
      Render2D.outline(
         x,
         y,
         width - 2.0F,
         height,
         3.5F,
         1.0F,
         this.context.withAlpha(this.context.theme().outline(), 0.55F),
         this.context.withAlpha(this.context.theme().accent(), 0.34F),
         this.context.theme().surfaceDeep().getRGB(),
         this.context.withAlpha(this.context.theme().accentDark(), 0.42F)
      );
   }

   private void renderModuleExpandIndicator(SettingCardController.ModuleState state, float x, float y) {
   }

   private void updateModuleScope(ModuleCategory category, boolean searchMode, List<Module> modules) {
      if (!this.moduleListInitialized && this.activeCategory == null) {
         this.activeCategory = category;
         this.activeSearchMode = searchMode;
         this.displayedModules = new ArrayList<>(modules);
         this.restoreScrollScope(category, searchMode);
      } else {
         boolean scopeChanged = this.activeSearchMode != searchMode || !searchMode && this.activeCategory != category;
         if (scopeChanged) {
            this.pendingCategory = category;
            this.pendingSearchMode = searchMode;
            this.pendingModules = new ArrayList<>(modules);
            this.switchToPendingScope();
         } else {
            this.displayedModules = new ArrayList<>(modules);
         }
      }
   }

   private void switchToPendingScope() {
      this.saveScrollScope();
      this.context.activeColor = null;
      this.context.draggingColor = null;
      this.context.draggingColorPart = 0;
      this.context.draggingSlider = null;
      this.context.focusedText = null;
      this.context.listeningBind = null;
      this.listeningModuleBind = null;
      this.categoryTransitionDirection = this.transitionDirection(this.activeCategory, this.pendingCategory, this.activeSearchMode, this.pendingSearchMode);
      this.activeCategory = this.pendingCategory;
      this.activeSearchMode = this.pendingSearchMode;
      this.displayedModules = new ArrayList<>(this.pendingModules);
      this.pendingModules = List.of();
      this.lastRenderModules = List.of();
      this.restoreScrollScope(this.activeCategory, this.activeSearchMode);
      this.snapNextModuleLayout = true;
      this.categorySwitchAnimation.setDirection(Direction.FORWARDS);
      this.categorySwitchAnimation.reset();

      for (Module module : this.displayedModules) {
         SettingCardController.ModuleState state = this.moduleState(module);
         this.completeListAnimation(state.entryAnimation, Direction.FORWARDS);
         state.includedLastFrame = false;
         state.includedThisFrame = false;
         state.column = -1;
         state.positionReady = false;
      }

      for (SettingCardController.ModuleState state : this.moduleStates.values()) {
         if (!this.displayedModules.contains(state.module)) {
            state.includedLastFrame = false;
            state.includedThisFrame = false;
            state.visible = false;
            state.column = -1;
         }
      }
   }

   private int transitionDirection(ModuleCategory fromCategory, ModuleCategory toCategory, boolean fromSearchMode, boolean toSearchMode) {
      if (fromSearchMode == toSearchMode && fromCategory != null && toCategory != null) {
         return toCategory.ordinal() >= fromCategory.ordinal() ? 1 : -1;
      } else {
         return 1;
      }
   }

   private void applyPendingModuleFocus(List<SettingCardController.ModuleLayoutEntry> entries, int columnCount, float cardGap) {
      if (this.pendingFocusModule != null && !this.activeSearchMode && this.pendingFocusModule.getCategory() == this.activeCategory) {
         SettingCardController.ModuleFocusTarget target = this.moduleFocusTarget(entries, columnCount, cardGap, this.pendingFocusModule);
         if (target != null) {
            float maxScroll = Math.max(0.0F, this.moduleContentHeight - this.context.moduleClipHeight);
            float targetScroll = target.offset - Math.max(8.0F, (this.context.moduleClipHeight - target.height) * 0.35F);
            this.moduleScrollTarget = this.context.clamp(targetScroll, 0.0F, maxScroll);
            SettingCardController.ModuleState state = this.moduleState(this.pendingFocusModule);
            state.focusAnimation.setDirection(Direction.FORWARDS);
            state.focusAnimation.reset();
            this.pendingFocusModule = null;
         }
      }
   }

   private SettingCardController.ModuleFocusTarget moduleFocusTarget(
      List<SettingCardController.ModuleLayoutEntry> entries, int columnCount, float cardGap, Module module
   ) {
      float[] columnOffsets = new float[Math.max(1, columnCount)];
      float[] columnLayoutOffsets = new float[Math.max(1, columnCount)];

      for (SettingCardController.ModuleLayoutEntry entry : entries) {
         SettingCardController.ModuleState state = this.moduleState(entry.module);
         int column = this.preferredColumn(state.column, columnLayoutOffsets);
         if (entry.module == module) {
            return new SettingCardController.ModuleFocusTarget(columnOffsets[column], entry.height);
         }

         columnOffsets[column] += entry.height + cardGap;
         columnLayoutOffsets[column] += entry.layoutHeight + cardGap;
      }

      return null;
   }

   private void restoreScrollScope(ModuleCategory category, boolean searchMode) {
      SettingCardController.ScrollSnapshot snapshot = searchMode
         ? SEARCH_SCROLL
         : CATEGORY_SCROLLS.computeIfAbsent(category, ignored -> new SettingCardController.ScrollSnapshot());
      this.moduleScroll = snapshot.current;
      this.moduleScrollTarget = snapshot.target;
   }

   private void saveScrollScope() {
      SettingCardController.ScrollSnapshot snapshot = this.activeSearchMode
         ? SEARCH_SCROLL
         : (this.activeCategory == null ? null : CATEGORY_SCROLLS.computeIfAbsent(this.activeCategory, ignored -> new SettingCardController.ScrollSnapshot()));
      if (snapshot != null) {
         snapshot.current = this.moduleScroll;
         snapshot.target = this.moduleScrollTarget;
      }
   }

   private void completeListAnimation(Decelerate animation, Direction direction) {
      this.completeListAnimation(animation, direction, 2);
   }

   private void completeListAnimation(Decelerate animation, Direction direction, int ms) {
      animation.setDirection(direction);
      animation.counter.setTime(System.currentTimeMillis() - (long)ms - 1L);
   }

   private void startListAnimation(Decelerate animation) {
      animation.setDirection(Direction.FORWARDS);
      animation.reset();
   }

   private List<SettingCardComponent<?>> visibleComponents(Module module) {
      List<SettingCardComponent<?>> components = new ArrayList<>();

      for (Setting<?> setting : module.getSettings()) {
         SettingCardComponent<?> component = this.componentFor(setting);
         if (component != null && component.alive()) {
            components.add(component);
         }
      }

      components.sort(Comparator.comparingInt(SettingCardComponent::order));
      return components;
   }

   private SettingCardComponent<?> componentFor(Setting<?> setting) {
      SettingCardComponent<?> component = this.settingComponents.get(setting);
      if (component != null) {
         return component;
      } else {
         component = SettingCardFactory.create(setting);
         if (component != null) {
            this.settingComponents.put(setting, component);
         }

         return component;
      }
   }

   private float measureSettings(List<SettingCardComponent<?>> settings) {
      float height = 0.0F;

      for (SettingCardComponent<?> component : settings) {
         height += component.animatedHeight(this.context);
      }

      return height;
   }

   private float measureLayoutSettings(List<SettingCardComponent<?>> settings) {
      float height = 0.0F;

      for (SettingCardComponent<?> component : settings) {
         height += component.animatedLayoutHeight(this.context);
      }

      return height;
   }

   private List<SettingCardController.ModuleLayoutEntry> layoutEntries(List<Module> modules) {
      List<SettingCardController.ModuleLayoutEntry> entries = new ArrayList<>();

      for (Module module : modules) {
         List<SettingCardComponent<?>> settings = this.visibleComponents(module);
         entries.add(
            new SettingCardController.ModuleLayoutEntry(
               module, settings, this.moduleCardHeight(module, settings), this.moduleCardLayoutHeight(module, settings)
            )
         );
      }

      entries.sort(
         Comparator.<SettingCardController.ModuleLayoutEntry>comparingInt(entry -> entry.module.isStarred() ? 0 : 1)
            .thenComparing(entry -> entry.module.getName(), String.CASE_INSENSITIVE_ORDER)
      );
      return entries;
   }

   private float measureModuleLayoutHeight(List<SettingCardController.ModuleLayoutEntry> entries, int columnCount, float cardGap) {
      float[] columnHeights = new float[Math.max(1, columnCount)];
      float[] columnLayoutHeights = new float[Math.max(1, columnCount)];

      for (SettingCardController.ModuleLayoutEntry entry : entries) {
         int column = this.preferredColumn(-1, columnLayoutHeights);
         columnHeights[column] += entry.height + cardGap;
         columnLayoutHeights[column] += entry.layoutHeight + cardGap;
      }

      float totalHeight = 0.0F;

      for (float columnHeight : columnHeights) {
         totalHeight = Math.max(totalHeight, columnHeight);
      }

      return totalHeight <= 0.0F ? 0.0F : totalHeight - cardGap;
   }

   private int columnCount(float contentWidth, float cardGap) {
      float minCardWidth = 150.0F;
      int columns = (int)Math.floor((double)((contentWidth + cardGap) / (minCardWidth + cardGap)));
      return Math.max(1, columns);
   }

   private float moduleCardWidth(float contentWidth, int columnCount, float cardGap) {
      return Math.max(150.0F, (contentWidth - cardGap * (float)(columnCount - 1)) / (float)Math.max(1, columnCount));
   }

   private int preferredColumn(int currentColumn, float[] columnLayoutOffsets) {
      if (currentColumn >= 0 && currentColumn < columnLayoutOffsets.length) {
         return currentColumn;
      } else {
         int targetColumn = 0;
         float minOffset = columnLayoutOffsets[0];

         for (int i = 1; i < columnLayoutOffsets.length; i++) {
            if (columnLayoutOffsets[i] < minOffset) {
               minOffset = columnLayoutOffsets[i];
               targetColumn = i;
            }
         }

         return targetColumn;
      }
   }

   private float moduleCardHeight(Module module, List<SettingCardComponent<?>> settings) {
      return settings.isEmpty() ? 21.0F : Math.max(21.0F, 24.0F + this.moduleSettingsHeight(this.moduleState(module), settings) + 4.0F);
   }

   private float moduleCardLayoutHeight(Module module, List<SettingCardComponent<?>> settings) {
      return settings.isEmpty() ? 20.0F : Math.max(20.0F, 24.0F + this.moduleSettingsLayoutHeight(this.moduleState(module), settings) + 4.0F);
   }

   private float moduleSettingsHeight(SettingCardController.ModuleState state, List<SettingCardComponent<?>> settings) {
      return this.measureSettings(settings) * this.moduleExpandProgress(state);
   }

   private float moduleSettingsLayoutHeight(SettingCardController.ModuleState state, List<SettingCardComponent<?>> settings) {
      return this.measureLayoutSettings(settings) * this.moduleExpandProgress(state);
   }

   private float moduleExpandProgress(SettingCardController.ModuleState state) {
      state.expanded = true;
      state.expandAnimation.setDirection(Direction.FORWARDS);
      return 1.0F;
   }

   private SettingCardController.ModuleState moduleState(Module module) {
      return this.moduleStates.computeIfAbsent(module, SettingCardController.ModuleState::new);
   }

   private boolean modulesChanged(List<Module> modules) {
      if (modules.size() != this.lastRenderModules.size()) {
         return true;
      } else {
         for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i) != this.lastRenderModules.get(i)) {
               return true;
            }
         }

         return false;
      }
   }

   private static record ModuleFocusTarget(float offset, float height) {
   }

   private static record ModuleLayoutEntry(Module module, List<SettingCardComponent<?>> settings, float height, float layoutHeight) {
   }

   private static final class ModuleState {
      private final Module module;
      private final Decelerate toggleAnimation;
      private final Decelerate entryAnimation;
      private final Decelerate bindAnimation;
      private final Decelerate starAnimation;
      private final Decelerate expandAnimation;
      private final Decelerate focusAnimation;
      private boolean visible;
      private boolean includedLastFrame;
      private boolean includedThisFrame;
      private boolean positionReady;
      private boolean expanded;
      private float cardX;
      private float cardY;
      private float cardWidth;
      private float cardHeight;
      private int column = -1;
      private float headerX;
      private float headerY;
      private float headerWidth;
      private float headerHeight;
      private float starX;
      private float starY;
      private float starWidth;
      private float starHeight;
      private float toggleX;
      private float toggleY;
      private float toggleWidth;
      private float toggleHeight;
      private float micX;
      private float micY;
      private float micWidth;
      private float micHeight;

      private ModuleState(Module module) {
         this.module = module;
         this.toggleAnimation = MathUtils.createAnimation(180, module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
         this.entryAnimation = MathUtils.createAnimation(2, Direction.BACKWARDS);
         this.bindAnimation = MathUtils.createAnimation(110, module.getBind() != null && module.getBind().isBound() ? Direction.FORWARDS : Direction.BACKWARDS);
         this.starAnimation = MathUtils.createAnimation(130, module.isStarred() ? Direction.FORWARDS : Direction.BACKWARDS);
         this.expandAnimation = MathUtils.createAnimation(110, Direction.BACKWARDS);
         this.focusAnimation = MathUtils.createAnimation(520, Direction.BACKWARDS);
         this.expanded = true;
      }
   }

   private static final class ScrollSnapshot {
      private float current;
      private float target;

      private ScrollSnapshot() {
      }
   }
}
