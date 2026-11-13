package daamky.ua.screens.modernui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.display.Appearance;
import daamky.ua.api.module.impl.misc.ClientSounds;
import daamky.ua.api.theme.ThemeManager;
import daamky.ua.manager.Manager;
import daamky.ua.screens.csgui.CsAutoBuyPanel;
import daamky.ua.screens.modernui.impl.CategoryPanel;
import daamky.ua.screens.modernui.impl.Header;
import daamky.ua.screens.modernui.impl.PanelBackground;
import daamky.ua.screens.modernui.impl.SearchHandler;
import daamky.ua.screens.modernui.impl.WorldAnimation;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.render.animation.modernfx.Decelerate;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.ui.Render2D;

public final class ClickGuiScreen extends Screen {
   private static final int OPEN_ANIMATION_MS = 220;
   private static final float BACKGROUND_DIM_MAX_ALPHA = 55.0F;
   private final PanelBackground background = new PanelBackground();
   private final Header header = new Header();
   private final CategoryPanel categoryPanel = new CategoryPanel();
   private final SearchHandler searchHandler = new SearchHandler();
   private final CsAutoBuyPanel autoBuyPanel = new CsAutoBuyPanel();
   private final Decelerate openAnimation = MathUtils.createAnimation(220, Direction.FORWARDS);
   private boolean closing;
   private boolean openedSoundPlayed;

   public ClickGuiScreen() {
      super(Text.literal("ClickGui"));
   }

   protected void init() {
      super.init();
      this.closing = false;
      WorldAnimation.stop();
      this.openAnimation.setDirection(Direction.FORWARDS);
      this.openAnimation.reset();
      this.playOpenSound();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      float progress = this.openProgress();
      Render2D.beginFrame(context);
      this.renderBackgroundDim(progress, (float)this.width, (float)this.height);
      Render2D.flush();
      context.createNewRootLayer();
      this.renderScaledContent(context, mouseX, mouseY, progress);
      this.finishClosingIfDone();
   }

   private void renderScaledContent(DrawContext graphics, int mouseX, int mouseY, float progress) {
      float panelWidth = 580.0F;
      float panelHeight = 300.0F;
      float panelX = ((float)this.width - panelWidth) * 0.5F;
      float panelY = ((float)this.height - panelHeight) * 0.5F;
      float centerX = panelX + panelWidth * 0.5F;
      float centerY = panelY + panelHeight * 0.5F;
      float scale = this.visualScale(progress);
      float offsetY = this.visualOffsetY(progress);
      int transformedMouseX = this.transformMouse(mouseX, centerX, scale, 0.0F);
      int transformedMouseY = this.transformMouse(mouseY, centerY, scale, offsetY);
      graphics.getMatrices().pushMatrix();
      graphics.getMatrices().translate(0.0F, offsetY);
      graphics.getMatrices().translate(centerX, centerY);
      graphics.getMatrices().scale(scale);
      graphics.getMatrices().translate(-centerX, -centerY);
      Render2D.beginFrame(graphics);
      this.renderContent(graphics, transformedMouseX, transformedMouseY);
      Render2D.flush();
      graphics.getMatrices().popMatrix();
   }

   public void renderDetachedBackground(DrawContext graphics, float progress, float screenWidth, float screenHeight) {
      Render2D.beginFrame(graphics);
      this.renderBackgroundDim(progress, screenWidth, screenHeight);
      Render2D.flush();
   }

   public void renderDetached(DrawContext graphics, float partialTick) {
      graphics.createNewRootLayer();
      Render2D.beginFrame(graphics);
      this.renderContent(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE);
      Render2D.flush();
   }

   private void renderBackgroundDim(float progress, float screenWidth, float screenHeight) {
      float alpha = 55.0F * this.clamp(progress, 0.0F, 1.0F);
      if (!(alpha <= 0.5F)) {
         Render2D.rect(0.0F, 0.0F, screenWidth, screenHeight, 0.0F, new Color(0, 0, 0, Math.round(alpha)).getRGB());
      }
   }

   private void renderContent(DrawContext graphics, int mouseX, int mouseY) {
      float panelWidth = 580.0F;
      float panelHeight = 300.0F;
      float x = ((float)this.width - panelWidth) * 0.5F;
      float y = ((float)this.height - panelHeight) * 0.5F;
      this.searchHandler.updateAnimations();
      List<Module> modules = this.currentModules();
      this.background
         .render(
            graphics,
            x,
            y,
            panelWidth,
            panelHeight,
            this.categoryPanel.selectedCategory(),
            !this.normalizeSearch(this.searchHandler.getSearchText()).isEmpty(),
            modules,
            mouseX,
            mouseY
         );
      if (this.isAutoBuyPanelActive()) {
         RenderItem.beginGuiFrame();
         this.autoBuyPanel
            .render(
               graphics, x + 55.0F, y + 9.0F, panelWidth - 64.0F, panelHeight - 18.0F, 1.0F, 0.82F, ThemeManager.current().accent().getRGB(), mouseX, mouseY
            );
      }

      this.categoryPanel.render(x, y, mouseX, mouseY);
      Render2D.flush();
      graphics.createNewRootLayer();
      Render2D.beginFrame(graphics);
      if (!this.isAutoBuyPanelActive()) {
         this.header
            .render(
               graphics,
               x,
               y,
               panelWidth,
               this.categoryPanel.previousCategory(),
               this.categoryPanel.selectedCategory(),
               this.categoryPanel.headerProgress(),
               this.searchHandler,
               modules,
               mouseX,
               mouseY
            );
      }
   }

   public boolean keyPressed(KeyInput input) {
      if (this.closing) {
         return false;
      } else if (this.isAutoBuyPanelActive() && this.autoBuyPanel.keyPressed(input.key(), input.scancode(), input.modifiers())) {
         return true;
      } else if (this.searchHandler.isSearchActive() && this.searchHandler.handleSearchKey(input)) {
         return true;
      } else if (!this.isAutoBuyPanelActive() && this.background.keyPressed(input)) {
         return true;
      } else if (input.key() != 345 && input.key() != 256) {
         return super.keyPressed(input);
      } else {
         this.close();
         return true;
      }
   }

   public boolean charTyped(CharInput input) {
      if (this.closing) {
         return false;
      } else if (this.isAutoBuyPanelActive() && this.autoBuyPanel.charTyped((char)input.codepoint(), input.modifiers())) {
         return true;
      } else if (this.searchHandler.isSearchActive() && this.searchHandler.handleSearchChar(input)) {
         return true;
      } else {
         return !this.isAutoBuyPanelActive() && this.background.charTyped(input) ? true : super.charTyped(input);
      }
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      if (this.closing) {
         return false;
      } else {
         float panelWidth = 580.0F;
         float panelHeight = 300.0F;
         float x = ((float)this.width - panelWidth) * 0.5F;
         float y = ((float)this.height - panelHeight) * 0.5F;
         List<Module> modules = this.currentModules();
         if (!this.isAutoBuyPanelActive()) {
            if (this.header.mouseClickedTheme(click, this.searchHandler)) {
               return true;
            }

            if (this.header.mouseClickedLanguage(click, this.searchHandler)) {
               return true;
            }

            if (click.button() == 0 && this.header.isSearchIconClicked(click.x(), click.y(), this.searchHandler)) {
               this.searchHandler.setSearchActive(true);
               return true;
            }

            if (click.button() == 0 && this.header.isCloseIconClicked(click.x(), click.y(), this.searchHandler)) {
               this.searchHandler.clearSearch();
               this.searchHandler.setSearchActive(false);
               return true;
            }

            boolean searchHovered = this.header.isSearchBoxHovered(click.x(), click.y(), x, y, this.searchHandler);
            if (click.button() == 0 && searchHovered) {
               this.searchHandler.setSearchActive(true);
               Module selectedModule = this.header.mouseClickedSearch(click, x, y, this.searchHandler, modules);
               if (selectedModule != null) {
                  this.categoryPanel.selectCategory(selectedModule.getCategory());
                  this.searchHandler.clearSearch();
                  this.searchHandler.setSearchActive(false);
                  this.background.focusModule(selectedModule);
               }

               return true;
            }

            if (this.searchHandler.isSearchActive() && !searchHovered) {
               this.searchHandler.setSearchActive(false);
            }
         }

         if (this.isAutoBuyPanelActive() && this.autoBuyPanel.mouseClicked(click.x(), click.y(), click.button())) {
            return true;
         } else if (!this.isAutoBuyPanelActive() && this.background.mouseClicked(click, doubled)) {
            return true;
         } else {
            return this.categoryPanel.mouseClicked(click, x, y) ? true : super.mouseClicked(click, doubled);
         }
      }
   }

   public boolean mouseReleased(Click click) {
      if (this.closing) {
         return false;
      } else {
         if (this.isAutoBuyPanelActive()) {
            this.autoBuyPanel.mouseReleased(click.x(), click.y(), click.button());
         }

         return this.background.mouseReleased(click) ? true : super.mouseReleased(click);
      }
   }

   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      if (this.closing) {
         return false;
      } else if (this.isAutoBuyPanelActive() && this.autoBuyPanel.mouseDragged(click.x(), click.y())) {
         return true;
      } else {
         return !this.isAutoBuyPanelActive() && this.background.mouseDragged(click, offsetX, offsetY) ? true : super.mouseDragged(click, offsetX, offsetY);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.closing) {
         return false;
      } else {
         float panelWidth = 580.0F;
         float panelHeight = 300.0F;
         float x = ((float)this.width - panelWidth) * 0.5F;
         float y = ((float)this.height - panelHeight) * 0.5F;
         if (!this.isAutoBuyPanelActive()) {
            if (this.header.mouseScrolledSearch(mouseX, mouseY, verticalAmount, x, y, this.searchHandler, this.currentModules())) {
               return true;
            }

            if (this.header.mouseScrolledTheme(mouseX, mouseY, verticalAmount, this.searchHandler)) {
               return true;
            }
         }

         if (this.categoryPanel.mouseScrolled(mouseX, mouseY, verticalAmount, x, y)) {
            return true;
         } else if (this.isAutoBuyPanelActive() && this.autoBuyPanel.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
         } else {
            return !this.isAutoBuyPanelActive() && this.background.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
               ? true
               : super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
         }
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      if (!this.closing) {
         this.autoBuyPanel.onGuiClosed();
         this.closing = true;
         this.playCloseSound();
         this.openAnimation.setDirection(Direction.BACKWARDS);
      }
   }

   private void playOpenSound() {
      if (!this.openedSoundPlayed) {
         this.openedSoundPlayed = true;
         ClientSounds.playClickGuiOpeningSound();
      }
   }

   private void playCloseSound() {
      ClientSounds.playClickGuiClosingSound();
   }

   private void finishClosingIfDone() {
      if (this.closing && this.openAnimation.isFinished(Direction.BACKWARDS)) {
         this.client.setScreen(null);
      }
   }

   private List<Module> currentModules() {
      String query = this.normalizeSearch(this.searchHandler.getSearchText());
      if (query.isEmpty()) {
         if (this.searchHandler.isSearchActive()) {
            ModuleManager manager = Manager.getModules();
            return manager == null
               ? List.of()
               : manager.getModules().stream().filter(modulex -> !(modulex instanceof Appearance)).collect(Collectors.toCollection(ArrayList::new));
         } else {
            return this.modulesForCategory(this.categoryPanel.selectedCategory());
         }
      } else {
         ModuleManager manager = Manager.getModules();
         if (manager == null) {
            return List.of();
         } else {
            List<Module> filtered = new ArrayList<>();

            for (Module module : manager.getModules()) {
               if (!(module instanceof Appearance)
                  && (this.normalizeSearch(module.getName()).contains(query) || this.normalizeSearch(ClientLanguage.tr(module.getName())).contains(query))) {
                  filtered.add(module);
               }
            }

            return filtered;
         }
      }
   }

   private List<Module> modulesForCategory(ModuleCategory category) {
      ModuleManager manager = Manager.getModules();
      return manager != null && category != null
         ? manager.getByCategory(category).stream().filter(module -> !(module instanceof Appearance)).toList()
         : List.of();
   }

   private boolean isAutoBuyPanelActive() {
      return this.categoryPanel.selectedCategory() == ModuleCategory.AUTOBUY;
   }

   private float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private String normalizeSearch(String value) {
      return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
   }

   private float openProgress() {
      float elapsed = this.clamp((float)this.openAnimation.counter.getTime() / 220.0F, 0.0F, 1.0F);
      if (this.openAnimation.getDirection() == Direction.FORWARDS) {
         return this.openAnimation.isDone() ? 1.0F : this.easeOutCubic(elapsed);
      } else {
         return this.openAnimation.isDone() ? 0.0F : 1.0F - this.easeInCubic(elapsed);
      }
   }

   private float easeOutCubic(float progress) {
      float x = this.clamp(progress, 0.0F, 1.0F);
      float inverted = 1.0F - x;
      return 1.0F - inverted * inverted * inverted;
   }

   private float easeInCubic(float progress) {
      float x = this.clamp(progress, 0.0F, 1.0F);
      return x * x * x;
   }

   private float visualScale(float progress) {
      return 0.96F + this.clamp(progress, 0.0F, 1.0F) * 0.04F;
   }

   private float visualOffsetY(float progress) {
      return (1.0F - this.clamp(progress, 0.0F, 1.0F)) * 8.0F;
   }

   private int transformMouse(int mousePosition, float center, float scale, float offset) {
      return scale <= 0.01F ? mousePosition : Math.round(((float)mousePosition - offset - center) / scale + center);
   }
}
