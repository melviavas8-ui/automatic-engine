package daamky.ua.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.world.level.storage.LevelSummary;
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
import daamky.ua.screens.mainmenu.WorldListEntryOrderAccess;
import daamky.ua.screens.mainmenu.WorldOrderStore;

@Mixin(
   targets = {"net/minecraft/client/gui/screen/world/WorldListWidget$WorldEntry"}
)
public abstract class WorldListEntryThemeMixin implements WorldListEntryOrderAccess, DraggableListEntryAccess {
   @Shadow
   @Final
   private WorldListWidget parent;
   @Shadow
   @Final
   private LevelSummary level;
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

   public WorldListEntryThemeMixin() {
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
      )
   )
   private void daamky$hideVanillaIconHover(DrawContext graphics, int left, int top, int right, int bottom, int color) {
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")}
   )
   private void daamky$startDragging(Click event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      if (VanillaMenuTheme.active() && event.button() == 0) {
         this.daamky$mouseDown = true;
         this.daamky$dragging = false;
         this.daamky$pressY = event.y();
         Widget entry = (Widget)(Object)this;
         this.daamky$grabX = event.x() - (double)entry.getX();
         this.daamky$grabY = event.y() - (double)entry.getY();
         this.daamky$cursorX = event.x();
         this.daamky$cursorY = event.y();
      }
   }

   public boolean mouseDragged(Click event, double dragX, double dragY) {
      if (this.daamky$mouseDown && event.button() == 0) {
         if (!this.daamky$dragging && Math.abs(event.y() - this.daamky$pressY) < 3.0) {
            return false;
         } else {
            this.daamky$dragging = true;
            this.daamky$cursorX = event.x();
            this.daamky$cursorY = event.y();
            int index = this.parent.children().indexOf(this);
            if (index < 0) {
               return true;
            } else {
               int target = index;
               Widget entry = (Widget)(Object)this;
               double draggedCenter = this.daamky$cursorY - this.daamky$grabY + (double)entry.getHeight() * 0.5;
               if (draggedCenter < (double)(Object)this.parent.getRowTop(index)
                  && index > 0
                  && this.parent.children().get(index - 1) instanceof WorldListEntryOrderAccess) {
                  target = index - 1;
               } else if (draggedCenter > (double)(Object)this.parent.getRowBottom(index)
                  && index + 1 < this.parent.children().size()
                  && this.parent.children().get(index + 1) instanceof WorldListEntryOrderAccess) {
                  target = index + 1;
               }

               if (target != index) {
                  double scroll = this.parent.getScrollY();
                  ((AbstractSelectionListAccessor)(Object)this.parent).daamky$swapEntries(index, target);
                  this.parent.setScrollY(scroll);
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   public boolean mouseReleased(Click event) {
      boolean handled = this.daamky$dragging;
      this.daamky$mouseDown = false;
      this.daamky$dragging = false;
      if (handled) {
         WorldOrderStore.save(this.parent);
      }

      return handled;
   }

   @Override
   public String daamky$getLevelId() {
      return this.level.getName();
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

