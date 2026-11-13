package daamky.ua.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.slot.Slot;
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

@Mixin({HandledScreen.class})
public abstract class HandledScreenMixin {
   @Shadow
   @Final
   protected int backgroundWidth;
   @Shadow
   @Final
   protected int backgroundHeight;
   @Shadow
   @Nullable
   protected Slot focusedSlot;

   public HandledScreenMixin() {
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")},
      require = 0
   )
   private void daamky$handledScreenRender(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      Manager.postEvent(new HandledScreenEvent(graphics, this.focusedSlot, this.backgroundWidth, this.backgroundHeight));
   }

   @Inject(
      method = {"keyPressed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$preventProtectedItemDrop(KeyInput event, CallbackInfoReturnable<Boolean> cir) {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      if (minecraft.options.dropKey.matchesKey(event)
         && this.focusedSlot != null
         && this.focusedSlot.hasStack()
         && AntiDrop.shouldBlock(this.focusedSlot.getStack())) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"close"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$animateContainerClose(CallbackInfo ci) {
      if (Animations.deferScreenClose((HandledScreen)(Object)this)) {
         ci.cancel();
      }
   }
}

