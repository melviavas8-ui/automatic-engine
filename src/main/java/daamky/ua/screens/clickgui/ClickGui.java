package daamky.ua.screens.clickgui;

import daamky.ua.api.module.impl.misc.ClientSounds;
import daamky.ua.screens.clickgui.impl.ClickGuiController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ClickGui extends Screen {
   private boolean openedSoundPlayed;

   public ClickGui() {
      super(Component.literal("daamky ClickGui"));
   }

   protected void init() {
      ClickGuiController.init();
      this.playOpenSound();
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
   }

   public boolean keyPressed(KeyEvent input) {
      return ClickGuiController.keyPressed(input) ? true : super.keyPressed(input);
   }

   public boolean charTyped(CharacterEvent input) {
      return ClickGuiController.charTyped(input) ? true : super.charTyped(input);
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      return ClickGuiController.mouseClicked(click, doubled);
   }

   public boolean mouseReleased(MouseButtonEvent click) {
      return ClickGuiController.mouseReleased(click);
   }

   public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
      return ClickGuiController.mouseDragged(click, offsetX, offsetY);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      return ClickGuiController.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
         ? true
         : super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public void onClose() {
      ClickGuiController.startClosing();
   }

   public boolean isPauseScreen() {
      return false;
   }

   public static void renderPanels(GuiGraphics graphics, int screenWidth, int screenHeight) {
      ClickGuiController.renderPanels(graphics, screenWidth, screenHeight);
   }

   public static void warmupText() {
      ClickGuiController.warmupText();
   }

   public static void startClosing() {
      ClickGuiController.startClosing();
   }

   public static void resetOverlayState() {
      ClickGuiController.resetOverlayState();
   }

   public static boolean shouldRenderOverlay() {
      return ClickGuiController.shouldRenderOverlay();
   }

   public static void tickMovementKeys() {
      ClickGuiController.tickMovementKeys();
   }

   private void playOpenSound() {
      if (!this.openedSoundPlayed) {
         this.openedSoundPlayed = true;
         ClientSounds.playClickGuiOpeningSound();
      }
   }
}
