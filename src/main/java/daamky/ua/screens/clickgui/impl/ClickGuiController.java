package daamky.ua.screens.clickgui.impl;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.misc.ClientSounds;
import daamky.ua.screens.clickgui.ClickGui;
import daamky.ua.screens.clickgui.impl.panel.ClickGuiPanel;
import daamky.ua.screens.clickgui.impl.panel.ClickGuiPanelSlot;
import daamky.ua.screens.clickgui.impl.search.ClickGuiSearchBar;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;
import daamky.ua.utils.render.ui.font.FontType;

public final class ClickGuiController {
   private static final SmoothAnimation OPEN_ANIMATION = new SmoothAnimation();
   private static final float OPEN_OFFSET = 18.0F;
   private static final float PANEL_WIDTH = 130.0F;
   private static final float PANEL_HEIGHT = 280.0F;
   private static final float MARGIN = 22.0F;
   private static final float GAP = 7.0F;
   private static final float LANGUAGE_BUTTON_X = 14.0F;
   private static final float LANGUAGE_BUTTON_Y = 13.0F;
   private static final float LANGUAGE_BUTTON_WIDTH = 34.0F;
   private static final float LANGUAGE_BUTTON_HEIGHT = 18.0F;
   private static final ClickGuiSearchBar SEARCH_BAR = new ClickGuiSearchBar();
   private static final List<ClickGuiPanel> PANELS = new ArrayList<>(
      List.of(
         new ClickGuiPanel(ModuleCategory.COMBAT),
         new ClickGuiPanel(ModuleCategory.MOVEMENT),
         new ClickGuiPanel(ModuleCategory.VISUAL),
         new ClickGuiPanel(ModuleCategory.PLAYER),
         new ClickGuiPanel(ModuleCategory.MISC)
      )
   );
   private static final List<ClickGuiPanelSlot> PANEL_SLOTS = new ArrayList<>(5);
   private static final List<ClickGuiPanelSlot> PANEL_SLOT_POOL = new ArrayList<>(5);
   private static boolean closing;
   private static boolean overlayActive;
   private static boolean textWarmed;
   private static boolean pendingOpenAnimation;
   private static double mouseX;
   private static double mouseY;

   private ClickGuiController() {
   }

   public static void init() {
      closing = false;
      overlayActive = true;
      pendingOpenAnimation = true;
      OPEN_ANIMATION.set(0.0);
      stopGuiMovement(Minecraft.getInstance());
   }

   public static void renderPanels(GuiGraphics graphics, int screenWidth, int screenHeight) {
      updateMousePosition();
      float coordinateScale = coordinateScale();
      int designScreenWidth = Math.max(1, Math.round((float)screenWidth / coordinateScale));
      int designScreenHeight = Math.max(1, Math.round((float)screenHeight / coordinateScale));
      if (pendingOpenAnimation) {
         pendingOpenAnimation = false;
         OPEN_ANIMATION.set(0.0);
         OPEN_ANIMATION.run(1.0, 0.28, Easings.CUBIC_OUT);
      }

      OPEN_ANIMATION.update();
      float openProgress = clamp(OPEN_ANIMATION.get(), 0.0F, 1.0F);
      if (closing && !OPEN_ANIMATION.isAlive() && openProgress <= 0.01F) {
         closing = false;
         overlayActive = false;
      } else if (!(openProgress <= 0.01F) || closing) {
         float panelWidth = 130.0F;
         float scale = 1.08F - 0.08F * openProgress;
         float scaledPanelWidth = panelWidth * scale;
         float scaledPanelHeight = 280.0F * scale;
         float guiWidth = scaledPanelWidth * (float)PANELS.size() + 7.0F * (float)(PANELS.size() - 1);
         float startX = ((float)designScreenWidth - guiWidth) / 2.0F;
         float centeredY = ((float)designScreenHeight - scaledPanelHeight) / 2.0F;
         float baseY = Math.max(22.0F, centeredY) - (1.0F - openProgress) * 18.0F;
         List<ClickGuiPanelSlot> slots = panelSlots(designScreenHeight, startX, baseY, panelWidth, scaledPanelWidth, scale, openProgress);
         Render2D.beginFrame(graphics);
         renderCornerDimming(designScreenWidth, designScreenHeight, openProgress);
         graphics.nextStratum();

         for (ClickGuiPanelSlot slot : slots) {
            slot.panel().renderShell(graphics, slot.x(), slot.y(), slot.width(), slot.height(), slot.scale(), slot.alpha());
            graphics.nextStratum();
            slot.panel().renderContent(graphics, slot.x(), slot.y(), slot.width(), slot.height(), slot.scale(), slot.alpha());
            graphics.nextStratum();
         }

         for (ClickGuiPanelSlot slot : slots) {
            slot.panel().renderOverlays(graphics);
            graphics.nextStratum();
         }

         SEARCH_BAR.render(graphics, designScreenWidth, openProgress, 0.0F, 0.0F);
         renderLanguageButton(openProgress);
         Render2D.flush();
      }
   }

   public static void warmupText() {
      if (!textWarmed) {
         textWarmed = true;

         for (ClickGuiPanel panel : PANELS) {
            panel.warmupText();
         }
      }
   }

   public static boolean keyPressed(KeyEvent event) {
      if (SEARCH_BAR.keyPressed(event, ClickGuiController::selectSearchResult)) {
         return true;
      } else if (event.key() != 256 && event.key() != 344) {
         return !closing && handlePanelKeyPressed(event);
      } else {
         startClosing();
         return true;
      }
   }

   public static boolean charTyped(CharacterEvent event) {
      return SEARCH_BAR.charTyped(event) ? true : !closing && handlePanelCharTyped(event);
   }

   public static boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      event = toDesignEvent(event);
      mouseX = event.x();
      mouseY = event.y();
      if (!closing && event.button() == 0 && languageButtonClicked(event.x(), event.y())) {
         ClientLanguage.toggle();
         return true;
      } else if (!closing && SEARCH_BAR.mouseClicked(event, doubled, ClickGuiController::selectSearchResult)) {
         return true;
      } else {
         return !closing && handlePanelMouseClicked(event, doubled) ? true : true;
      }
   }

   public static boolean mouseReleased(MouseButtonEvent event) {
      event = toDesignEvent(event);
      mouseX = event.x();
      mouseY = event.y();
      return handlePanelMouseReleased(event) ? true : true;
   }

   public static boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      event = toDesignEvent(event);
      dragX /= (double)coordinateScale();
      dragY /= (double)coordinateScale();
      mouseX = event.x();
      mouseY = event.y();
      return handlePanelMouseDragged(event, dragX, dragY) ? true : true;
   }

   public static boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (closing) {
         return true;
      } else {
         mouseX /= (double)coordinateScale();
         mouseY /= (double)coordinateScale();
         ClickGuiController.mouseX = mouseX;
         ClickGuiController.mouseY = mouseY;
         return handlePanelMouseScrolled(mouseX, mouseY, scrollY);
      }
   }

   public static double mouseX() {
      return mouseX;
   }

   public static double mouseY() {
      return mouseY;
   }

   public static void startClosing() {
      if (!closing) {
         ClientSounds.playClickGuiClosingSound();
         closing = true;
         overlayActive = true;
         OPEN_ANIMATION.run(0.0, 0.22, Easings.CUBIC_OUT);
         Minecraft client = Minecraft.getInstance();
         if (client.screen instanceof ClickGui) {
            client.setScreen(null);
         }
      }
   }

   public static void resetOverlayState() {
      closing = false;
      overlayActive = false;
      pendingOpenAnimation = false;
      OPEN_ANIMATION.set(0.0);
   }

   public static boolean shouldRenderOverlay() {
      Minecraft client = Minecraft.getInstance();
      if (client.screen != null && !(client.screen instanceof ClickGui)) {
         resetOverlayState();
         return false;
      } else {
         return overlayActive || client.screen instanceof ClickGui;
      }
   }

   public static void tickMovementKeys() {
      Minecraft client = Minecraft.getInstance();
      if (client.screen instanceof ClickGui) {
         if (client.player != null && client.getWindow() != null) {
            long handle = client.getWindow().handle();
            if (SEARCH_BAR.isFocused()) {
               stopGuiMovement(client);
            } else {
               boolean forward = updateKey(client.options.keyUp, handle);
               boolean left = updateKey(client.options.keyLeft, handle);
               boolean back = updateKey(client.options.keyDown, handle);
               boolean right = updateKey(client.options.keyRight, handle);
               updateKey(client.options.keyJump, handle);
               updateKey(client.options.keyShift, handle);
            }
         }
      }
   }

   private static List<ClickGuiPanelSlot> panelSlots(
      int screenHeight, float startX, float baseY, float panelWidth, float scaledPanelWidth, float scale, float openProgress
   ) {
      List<ClickGuiPanelSlot> slots = PANEL_SLOTS;
      slots.clear();
      int slotIndex = 0;

      for (int i = 0; i < PANELS.size(); i++) {
         ClickGuiPanel panel = PANELS.get(i);
         panel.updateAnimations();
         float targetX = startX + (float)i * (scaledPanelWidth + 7.0F);
         panel.snapX(targetX);
         slots.add(slot(slotIndex++).set(panel, targetX, baseY, panelWidth, 280.0F, scale, openProgress));
      }

      return slots;
   }

   private static ClickGuiPanelSlot slot(int index) {
      while (PANEL_SLOT_POOL.size() <= index) {
         PANEL_SLOT_POOL.add(new ClickGuiPanelSlot());
      }

      return PANEL_SLOT_POOL.get(index);
   }

   private static boolean updateKey(KeyMapping keyMapping, long windowHandle) {
      Key key = InputConstants.getKey(keyMapping.saveString());
      if (key.getType() != Type.KEYSYM) {
         return false;
      } else {
         boolean down = GLFW.glfwGetKey(windowHandle, key.getValue()) == 1;
         KeyMapping.set(key, down);
         return down;
      }
   }

   private static void stopGuiMovement(Minecraft client) {
      releaseMovementKeys(client);
   }

   private static void releaseMovementKeys(Minecraft client) {
      releaseKey(client.options.keyUp);
      releaseKey(client.options.keyLeft);
      releaseKey(client.options.keyDown);
      releaseKey(client.options.keyRight);
      releaseKey(client.options.keyJump);
      releaseKey(client.options.keyShift);
   }

   private static void releaseKey(KeyMapping keyMapping) {
      Key key = InputConstants.getKey(keyMapping.saveString());
      if (key.getType() == Type.KEYSYM) {
         KeyMapping.set(key, false);
      }
   }

   private static boolean isKeyPressed(KeyMapping keyMapping) {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.getWindow() != null && keyMapping != null) {
         Key key = InputConstants.getKey(keyMapping.saveString());
         return key.getType() == Type.KEYSYM && GLFW.glfwGetKey(client.getWindow().handle(), key.getValue()) == 1;
      } else {
         return false;
      }
   }

   private static boolean handlePanelMouseClicked(MouseButtonEvent event, boolean doubled) {
      for (int i = PANELS.size() - 1; i >= 0; i--) {
         if (PANELS.get(i).mouseClickedPopup(event, doubled)) {
            return true;
         }
      }

      for (int ix = PANELS.size() - 1; ix >= 0; ix--) {
         if (PANELS.get(ix).mouseClickedControls(event, doubled)) {
            return true;
         }
      }

      return false;
   }

   private static boolean handlePanelMouseReleased(MouseButtonEvent event) {
      for (int i = PANELS.size() - 1; i >= 0; i--) {
         if (PANELS.get(i).mouseReleased(event)) {
            return true;
         }
      }

      return false;
   }

   private static boolean handlePanelMouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      for (int i = PANELS.size() - 1; i >= 0; i--) {
         if (PANELS.get(i).mouseDragged(event, dragX, dragY)) {
            return true;
         }
      }

      return false;
   }

   private static boolean handlePanelMouseScrolled(double mouseX, double mouseY, double scrollY) {
      for (int i = PANELS.size() - 1; i >= 0; i--) {
         if (PANELS.get(i).mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
         }
      }

      return true;
   }

   private static boolean handlePanelKeyPressed(KeyEvent event) {
      for (int i = PANELS.size() - 1; i >= 0; i--) {
         if (PANELS.get(i).keyPressed(event)) {
            return true;
         }
      }

      return false;
   }

   private static boolean handlePanelCharTyped(CharacterEvent event) {
      for (int i = PANELS.size() - 1; i >= 0; i--) {
         if (PANELS.get(i).charTyped(event)) {
            return true;
         }
      }

      return false;
   }

   private static void selectSearchResult(ClickGuiSearchBar.Result result) {
      if (result != null) {
         Module module = result.module();

         for (ClickGuiPanel panel : PANELS) {
            if (panel.category() == module.getCategory()) {
               panel.reveal(module);
               return;
            }
         }
      }
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static MouseButtonEvent toDesignEvent(MouseButtonEvent event) {
      float scale = coordinateScale();
      if (Math.abs(scale - 1.0F) <= 1.0E-4F) {
         return event;
      } else {
         MouseButtonInfo info = new MouseButtonInfo(event.button(), event.modifiers());
         return new MouseButtonEvent(event.x() / (double)scale, event.y() / (double)scale, info);
      }
   }

   private static float coordinateScale() {
      return Render2DCoordinateSpace.guiIndependentScale();
   }

   private static void updateMousePosition() {
      Minecraft client = Minecraft.getInstance();
      if (client.mouseHandler != null && client.getWindow() != null) {
         float scale = coordinateScale();
         mouseX = client.mouseHandler.getScaledXPos(client.getWindow()) / (double)scale;
         mouseY = client.mouseHandler.getScaledYPos(client.getWindow()) / (double)scale;
      }
   }

   private static void renderCornerDimming(int screenWidth, int screenHeight, float alpha) {
      float overscan = 5.0F;
      float topHeight = 92.0F;
      float leftWidth = 122.0F;
      float fadeAlpha = alpha * alpha * (3.0F - 2.0F * alpha);
      Render2D.rect(
         -overscan,
         -overscan,
         (float)screenWidth + overscan * 2.0F,
         topHeight + overscan,
         0.0F,
         color(0, 0, 0, 112, fadeAlpha),
         color(0, 0, 0, 112, fadeAlpha),
         color(0, 0, 0, 0, fadeAlpha),
         color(0, 0, 0, 0, fadeAlpha)
      );
      Render2D.rect(
         -overscan,
         -overscan,
         leftWidth + overscan,
         (float)screenHeight + overscan * 2.0F,
         0.0F,
         color(0, 0, 0, 96, fadeAlpha),
         color(0, 0, 0, 0, fadeAlpha),
         color(0, 0, 0, 0, fadeAlpha),
         color(0, 0, 0, 96, fadeAlpha)
      );
   }

   private static void renderLanguageButton(float alpha) {
      float textAlpha = clamp(alpha, 0.0F, 1.0F);
      if (!(textAlpha <= 0.01F)) {
         int background = color(12, 12, 18, 210, textAlpha);
         int outline = color(255, 255, 255, 65, textAlpha);
         int icon = color(255, 255, 255, 245, textAlpha);
         Render2D.rect(14.0F, 13.0F, 34.0F, 18.0F, 5.0F, background);
         Render2D.outline(14.0F, 13.0F, 34.0F, 18.0F, 5.0F, 0.5F, outline, outline, outline, outline);
         String label = "Z";
         float iconSize = 13.0F;
         float iconWidth = Render2D.textWidth(FontType.GUI_ICONS, label, iconSize);
         Render2D.text(FontType.GUI_ICONS, label, 14.0F + (34.0F - iconWidth) * 0.5F, 15.5F, iconSize, icon);
      }
   }

   private static boolean languageButtonClicked(double mouseX, double mouseY) {
      return mouseX >= 14.0 && mouseX <= 48.0 && mouseY >= 13.0 && mouseY <= 31.0;
   }

   private static int color(int red, int green, int blue, int alpha, float alphaMultiplier) {
      return ColorUtil.rgba(red, green, blue, Math.round((float)alpha * alphaMultiplier));
   }
}
