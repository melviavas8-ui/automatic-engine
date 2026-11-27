package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.mixin.accessor.AbstractSelectionListAccessor;
import daamky.ua.screens.mainmenu.DraggableListEntryAccess;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

@Mixin(
   targets = {"net/minecraft/client/gui/screens/multiplayer/ServerSelectionList$OnlineServerEntry"}
)
public abstract class OnlineServerEntryThemeMixin implements DraggableListEntryAccess {
   @Unique
   private boolean daamky$mouseDown;
   @Unique
   private boolean daamky$dragging;
   @Unique
   private double daamky$pressY;
   @Unique
   private double daamky$grabX;
   @Unique
   private double daamky$grabY;
   @Unique
   private double daamky$cursorX;
   @Unique
   private double daamky$cursorY;
   @Shadow
   @Final
   private JoinMultiplayerScreen screen;
   @Shadow
   @Final
   private ServerSelectionList serverListWidget;

   public OnlineServerEntryThemeMixin() {
   }

   @Redirect(
      method = {"renderContent"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
      )
   )
   private void daamky$alignPlayerCountWithPing(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
      int vanillaRightEdge = x + font.width(text);
      VanillaMenuTheme.drawRightAlignedText(graphics, text.getString(), (float)vanillaRightEdge, (float)y, color);
   }

   @Redirect(
      method = {"renderContent"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
      )
   )
   private void daamky$hideVanillaIconHover(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")}
   )
   private void daamky$startDragging(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      if (VanillaMenuTheme.active() && event.button() == 0) {
         this.daamky$mouseDown = true;
         this.daamky$dragging = false;
         this.daamky$pressY = event.y();
         LayoutElement entry = (LayoutElement)(Object)this;
         this.daamky$grabX = event.x() - (double)entry.getX();
         this.daamky$grabY = event.y() - (double)entry.getY();
         this.daamky$cursorX = event.x();
         this.daamky$cursorY = event.y();
      }
   }

   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      if (this.daamky$mouseDown && event.button() == 0) {
         if (!this.daamky$dragging && Math.abs(event.y() - this.daamky$pressY) < 3.0) {
            return false;
         } else {
            this.daamky$dragging = true;
            this.daamky$cursorX = event.x();
            this.daamky$cursorY = event.y();
            int index = this.serverListWidget.children().indexOf(this);
            if (index < 0) {
               return true;
            } else {
               int target = index;
               LayoutElement entry = (LayoutElement)(Object)this;
               double draggedCenter = this.daamky$cursorY - this.daamky$grabY + (double)entry.getHeight() * 0.5;
               if (draggedCenter < (double)(Object)this.serverListWidget.getRowTop(index) && index > 0) {
                  target = index - 1;
               } else if (draggedCenter > (double)(Object)this.serverListWidget.getRowBottom(index) && index < this.screen.getServers().size() - 1) {
                  target = index + 1;
               }

               if (target != index) {
                  double scroll = this.serverListWidget.scrollAmount();
                  this.screen.getServers().swap(index, target);
                  ((AbstractSelectionListAccessor)(Object)this.serverListWidget).daamky$swapEntries(index, target);
                  this.serverListWidget.setScrollAmount(scroll);
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      boolean handled = this.daamky$dragging;
      this.daamky$mouseDown = false;
      this.daamky$dragging = false;
      return handled;
   }

   @Override
   public boolean daamky$isDragging() {
      return this.daamky$dragging;
   }

   @Override
   public float daamky$getDragOffsetX() {
      return !this.daamky$dragging ? 0.0F : (float)(this.daamky$cursorX - this.daamky$grabX);
   }

   @Override
   public float daamky$getDragOffsetY() {
      return !this.daamky$dragging ? 0.0F : (float)(this.daamky$cursorY - this.daamky$grabY);
   }
}

