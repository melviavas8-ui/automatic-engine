package daamky.ua.mixin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.DraggableListEntryAccess;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;

@Mixin({EntryListWidget.class})
public abstract class AbstractSelectionListMixin {
   @Unique
   private final Map<Object, Float> daamky$entryVisibility = new IdentityHashMap<>();
   @Unique
   private final Map<Object, AbstractSelectionListMixin.CardSnapshot> daamky$lastCards = new IdentityHashMap<>();
   @Unique
   private final Map<Object, Float> daamky$dragOffsetX = new IdentityHashMap<>();
   @Unique
   private final Map<Object, Float> daamky$dragOffsetY = new IdentityHashMap<>();
   @Unique
   private final Map<Object, Float> daamky$entryVisualY = new IdentityHashMap<>();
   @Unique
   private final List<AbstractSelectionListMixin.FadingCard> daamky$fadingCards = new ArrayList<>();
   @Unique
   private long daamky$lastFrameNanos;
   @Unique
   private float daamky$deltaSeconds;
   @Unique
   private boolean daamky$posePushed;

   public AbstractSelectionListMixin() {
   }

   @Inject(
      method = {"drawMenuListBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderThemedListBackground(DrawContext graphics, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"drawHeaderAndFooterSeparators"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$hideVanillaListSeparators(DrawContext graphics, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderList"},
      at = {@At("HEAD")}
   )
   private void daamky$prepareListAnimations(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (!VanillaMenuTheme.active()) {
         this.daamky$entryVisibility.clear();
         this.daamky$lastCards.clear();
         this.daamky$fadingCards.clear();
         this.daamky$dragOffsetX.clear();
         this.daamky$dragOffsetY.clear();
         this.daamky$entryVisualY.clear();
      } else {
         long now = System.nanoTime();
         this.daamky$deltaSeconds = this.daamky$lastFrameNanos == 0L ? 0.0F : Math.min(0.05F, (float)(now - this.daamky$lastFrameNanos) / 1.0E9F);
         this.daamky$lastFrameNanos = now;
         EntryListWidget<?> list = (EntryListWidget<?>)(Object)this;
         List<?> currentEntries = list.children();
         Iterator<Entry<Object, AbstractSelectionListMixin.CardSnapshot>> snapshots = this.daamky$lastCards.entrySet().iterator();

         while (snapshots.hasNext()) {
            Entry<Object, AbstractSelectionListMixin.CardSnapshot> snapshot = snapshots.next();
            boolean stillPresent = currentEntries.stream().anyMatch(current -> current == snapshot.getKey());
            if (!stillPresent) {
               this.daamky$fadingCards.add(new AbstractSelectionListMixin.FadingCard(snapshot.getValue(), 1.0F));
               this.daamky$entryVisibility.remove(snapshot.getKey());
               this.daamky$dragOffsetX.remove(snapshot.getKey());
               this.daamky$dragOffsetY.remove(snapshot.getKey());
               this.daamky$entryVisualY.remove(snapshot.getKey());
               snapshots.remove();
            }
         }

         Iterator<AbstractSelectionListMixin.FadingCard> fading = this.daamky$fadingCards.iterator();

         while (fading.hasNext()) {
            AbstractSelectionListMixin.FadingCard card = fading.next();
            card.visibility = Math.max(0.0F, card.visibility - this.daamky$deltaSeconds * 4.8F);
            if (card.visibility <= 0.01F) {
               fading.remove();
            } else {
               AbstractSelectionListMixin.CardSnapshot snapshot = card.snapshot;
               VanillaMenuTheme.renderListCard(graphics, snapshot.x, snapshot.y, snapshot.width, snapshot.height, false, snapshot.selected, card.visibility);
            }
         }
      }
   }

   @Inject(
      method = {"renderEntry"},
      at = {@At("HEAD")}
   )
   private void daamky$renderThemedListCard(DrawContext graphics, int mouseX, int mouseY, float partialTick, @Coerce Object entry, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         EntryListWidget<?> list = (EntryListWidget<?>)(Object)this;
         if (!(list instanceof OptionListWidget)) {
            int index = list.children().indexOf(entry);
            if (index >= 0) {
               int x = list.getRowLeft();
               int y = list.getRowTop(index);
               int width = list.getRowWidth();
               int height = Math.max(1, list.getRowBottom(index) - y);
               boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
               boolean selected = list.getSelectedOrNull() == entry;
               float dragX = 0.0F;
               float dragY = 0.0F;
               if (entry instanceof DraggableListEntryAccess draggable) {
                  float visualY = this.daamky$entryVisualY.getOrDefault(entry, (float)y);
                  float slotResponse = Math.min(1.0F, this.daamky$deltaSeconds * 13.0F);
                  visualY = lerp(visualY, (float)y, slotResponse);
                  this.daamky$entryVisualY.put(entry, visualY);
                  float slotOffsetY = visualY - (float)y;
                  float targetX = draggable.daamky$isDragging() ? draggable.daamky$getDragOffsetX() - (float)x : 0.0F;
                  float targetY = draggable.daamky$isDragging() ? draggable.daamky$getDragOffsetY() - (float)y : slotOffsetY;
                  float response = Math.min(1.0F, this.daamky$deltaSeconds * (draggable.daamky$isDragging() ? 30.0F : 12.0F));
                  dragX = lerp(this.daamky$dragOffsetX.getOrDefault(entry, 0.0F), targetX, response);
                  dragY = lerp(this.daamky$dragOffsetY.getOrDefault(entry, 0.0F), targetY, response);
                  this.daamky$dragOffsetX.put(entry, dragX);
                  this.daamky$dragOffsetY.put(entry, dragY);
                  hovered |= draggable.daamky$isDragging();
               }

               float visibility = Math.min(1.0F, this.daamky$entryVisibility.getOrDefault(entry, 0.0F) + this.daamky$deltaSeconds * 4.6F);
               this.daamky$entryVisibility.put(entry, visibility);
               this.daamky$lastCards.put(entry, new AbstractSelectionListMixin.CardSnapshot((float)x, (float)y, (float)width, (float)height, selected));
               float smooth = visibility * visibility * (3.0F - 2.0F * visibility);
               float scale = 0.88F + smooth * 0.12F;
               float centerX = (float)x + (float)width * 0.5F;
               float centerY = (float)y + (float)height * 0.5F;
               graphics.getMatrices().pushMatrix();
               graphics.getMatrices().translate(dragX, dragY);
               graphics.getMatrices().translate(centerX, centerY);
               graphics.getMatrices().scale(scale, scale);
               graphics.getMatrices().translate(-centerX, -centerY);
               this.daamky$posePushed = true;
               VanillaMenuTheme.renderListCard(graphics, (float)x, (float)y, (float)width, (float)height, hovered, selected, visibility);
            }
         }
      }
   }

   @Inject(
      method = {"renderEntry"},
      at = {@At("TAIL")}
   )
   private void daamky$finishThemedListCard(DrawContext graphics, int mouseX, int mouseY, float partialTick, @Coerce Object entry, CallbackInfo ci) {
      if (this.daamky$posePushed) {
         graphics.getMatrices().popMatrix();
         this.daamky$posePushed = false;
      }
   }

   @Inject(
      method = {"drawSelectionHighlight"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$hideVanillaSelection(DrawContext graphics, @Coerce Object entry, int y, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         ci.cancel();
      }
   }

   @Unique
   private static float lerp(float current, float target, float amount) {
      return current + (target - current) * amount;
   }

   @Unique
   private static record CardSnapshot(float x, float y, float width, float height, boolean selected) {
   }

   @Unique
   private static final class FadingCard {
      private final AbstractSelectionListMixin.CardSnapshot snapshot;
      private float visibility;

      private FadingCard(AbstractSelectionListMixin.CardSnapshot snapshot, float visibility) {
         this.snapshot = snapshot;
         this.visibility = visibility;
      }
   }
}

