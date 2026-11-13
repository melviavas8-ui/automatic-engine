package daamky.ua.screens.clickgui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import daamky.ua.api.module.impl.misc.ClientSounds;
import daamky.ua.screens.clickgui.impl.ClickGuiController;

public final class ClickGui extends Screen {
   private boolean openedSoundPlayed;

   public ClickGui() {
      super(Text.literal("daamky ClickGui"));
   }

   protected void init() {
      ClickGuiController.init();
      this.playOpenSound();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
   }

   public boolean keyPressed(KeyInput input) {
      return ClickGuiController.keyPressed(input) ? true : super.keyPressed(input);
   }

   public boolean charTyped(CharInput input) {
      return ClickGuiController.charTyped(input) ? true : super.charTyped(input);
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      return ClickGuiController.mouseClicked(click, doubled);
   }

   public boolean mouseReleased(Click click) {
      return ClickGuiController.mouseReleased(click);
   }

   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      return ClickGuiController.mouseDragged(click, offsetX, offsetY);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      return ClickGuiController.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
         ? true
         : super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public void close() {
      ClickGuiController.startClosing();
   }

   public boolean shouldPause() {
      return false;
   }

   public static void renderPanels(DrawContext graphics, int screenWidth, int screenHeight) {
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
