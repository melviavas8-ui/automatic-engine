package daamky.ua.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.events.impl.HandledScreenEvent;
import daamky.ua.api.module.impl.player.AntiDrop;
import daamky.ua.api.module.impl.visual.Animations;
import daamky.ua.manager.Manager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;

@Mixin({AbstractContainerScreen.class})
public abstract class HandledScreenMixin {
   @Shadow
   @Final
   protected int imageWidth;
   @Shadow
   @Final
   protected int imageHeight;
   @Shadow
   @Nullable
   protected Slot hoveredSlot;

   public HandledScreenMixin() {
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")},
      require = 0
   )
   private void daamky$handledScreenRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      Manager.postEvent(new HandledScreenEvent(graphics, this.hoveredSlot, this.imageWidth, this.imageHeight));
   }

   @Inject(
      method = {"keyPressed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$preventProtectedItemDrop(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.options.keyDrop.matches(event)
         && this.hoveredSlot != null
         && this.hoveredSlot.hasItem()
         && AntiDrop.shouldBlock(this.hoveredSlot.getItem())) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"onClose"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$animateContainerClose(CallbackInfo ci) {
      if (Animations.deferScreenClose((AbstractContainerScreen)(Object)this)) {
         ci.cancel();
      }
   }
}

