package daamky.ua.api.module.impl.combat.autoswaputil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import daamky.ua.api.module.impl.combat.AutoSwap;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.ui.Render2D;

public class AutoSwapItemSelectScreen extends Screen {
   private static final int ROWS = 4;
   private static final int COLS = 9;
   private static final float SLOT_SIZE = 18.0F;
   private static final float SLOT_GAP = 4.0F;
   private static final float PANEL_PADDING_X = 10.0F;
   private static final float PANEL_PADDING_Y = 10.0F;
   private final AutoSwap autoSwap;
   private final int wheelSlotIndex;
   private final AutoSwapItemSelectScreen.SlotRect[] slotRects = new AutoSwapItemSelectScreen.SlotRect[36];

   public AutoSwapItemSelectScreen(AutoSwap autoSwap, int wheelSlotIndex) {
      super(Text.empty());
      this.autoSwap = autoSwap;
      this.wheelSlotIndex = wheelSlotIndex;
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void close() {
      MinecraftClient mc = MinecraftClient.getInstance();
      mc.setScreen(new AutoSwapWheelScreen(this.autoSwap));
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         Render2D.beginFrame(context);
         RenderItem.beginFrame(context);
         Render2D.rect(0.0F, 0.0F, (float)this.width, (float)this.height, 369559563);
         float gridWidth = 194.0F;
         float gridHeight = 84.0F;
         float panelWidth = gridWidth + 20.0F;
         float panelHeight = gridHeight + 20.0F;
         float panelX = ((float)this.width - panelWidth) * 0.5F;
         float panelY = ((float)this.height - panelHeight) * 0.5F;
         Render2D.rect(panelX, panelY, panelWidth, panelHeight, 10.0F, -669377245);
         Render2D.glassOutline(panelX, panelY, panelWidth, panelHeight, 10.0F, 1.2F, -956301313, 1.0F, 40.0F, -1, 0.18F, false, 0.5F, 0.0015F, 1.0F, 0.0F);
         float startX = panelX + (panelWidth - gridWidth) * 0.5F;
         float startY = panelY + (panelHeight - gridHeight) * 0.5F;

         for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
               int index = row * 9 + col;
               float slotX = startX + (float)col * 22.0F;
               float slotY = startY + (float)row * 22.0F;
               boolean hovered = (float)mouseX >= slotX && (float)mouseX <= slotX + 18.0F && (float)mouseY >= slotY && (float)mouseY <= slotY + 18.0F;
               this.slotRects[index] = new AutoSwapItemSelectScreen.SlotRect(slotX, slotY, 18.0F, 18.0F, index);
               Render2D.rect(slotX, slotY, 18.0F, 18.0F, 4.0F, hovered ? -1976157114 : 1781149494);
               Render2D.glassOutline(
                  slotX,
                  slotY,
                  18.0F,
                  18.0F,
                  4.0F,
                  1.0F,
                  hovered ? -285212673 : -2000368425,
                  1.0F,
                  28.0F,
                  -1,
                  hovered ? 0.26F : 0.14F,
                  false,
                  0.42F,
                  0.0015F,
                  1.0F,
                  0.0F
               );
               ItemStack stack = mc.player.getInventory().getStack(index);
               if (!stack.isEmpty()) {
                  RenderItem.item(stack, slotX + 1.0F, slotY + 1.0F, 16.0F);
               }
            }
         }

         RenderItem.flush();
         Render2D.flush();
      }
   }

   public boolean keyPressed(KeyInput input) {
      if (input.key() == 256) {
         this.close();
         return true;
      } else {
         return super.keyPressed(input);
      }
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      if (click.button() != 0) {
         return super.mouseClicked(click, doubled);
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player == null) {
            return super.mouseClicked(click, doubled);
         } else {
            for (AutoSwapItemSelectScreen.SlotRect rect : this.slotRects) {
               if (rect != null && rect.contains(click.x(), click.y())) {
                  ItemStack stack = mc.player.getInventory().getStack(rect.index());
                  if (!stack.isEmpty()) {
                     this.autoSwap.setWheelSlotItem(this.wheelSlotIndex, stack.getItem(), stack.getName().getString());
                  }

                  this.close();
                  return true;
               }
            }

            return super.mouseClicked(click, doubled);
         }
      }
   }

   private static record SlotRect(float x, float y, float width, float height, int index) {
      private boolean contains(double mouseX, double mouseY) {
         return mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height);
      }
   }
}
