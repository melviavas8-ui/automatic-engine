package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.drag.core.ElementManager;
import daamky.ua.api.events.impl.HotBarScrollEvent;
import daamky.ua.api.events.impl.KeyEvent;
import daamky.ua.api.events.impl.MouseAppliedRotationEvent;
import daamky.ua.api.events.impl.MouseRotationEvent;
import daamky.ua.manager.Manager;
import daamky.ua.screens.clickgui.ClickGui;
import daamky.ua.screens.clickgui.impl.ClickGuiController;
import daamky.ua.utils.modules.warden.rotation.FreeLookController;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;

@Mixin({MouseHandler.class})
public class MouseMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   public MouseMixin() {
   }

   @Inject(
      method = {"onButton"},
      at = {@At("HEAD")}
   )
   private void daamky$onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
      int button = buttonInfo.button();
      if (button != -1 && window == this.minecraft.getWindow().handle()) {
         Manager.postEvent(new KeyEvent(this.minecraft.screen, Type.MOUSE, button, action));
         if (action == 0 && this.minecraft.screen instanceof ChatScreen) {
            MouseButtonEvent event = new MouseButtonEvent(this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow()), this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow()), buttonInfo);
            ElementManager.getInstance().handleMouseReleased(daamky$toDragEvent(event));
         }
      }
   }

   @Inject(
      method = {"onScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      if (window == this.minecraft.getWindow().handle() && this.minecraft.screen instanceof ClickGui) {
         ClickGuiController.mouseScrolled(
            this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow()), this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow()), horizontal, vertical
         );
         ci.cancel();
      } else if (window != this.minecraft.getWindow().handle() || this.minecraft.screen == null) {
         HotBarScrollEvent event = Manager.postEvent(new HotBarScrollEvent(horizontal, vertical));
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @WrapOperation(
      method = {"turnPlayer"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
      )},
      require = 0
   )
   private void daamky$modifyMouseRotationInput(LocalPlayer player, double cursorDeltaX, double cursorDeltaY, Operation<Void> original) {
      float beforeYaw = player.getYRot();
      float beforePitch = player.getXRot();
      MouseRotationEvent event = Manager.postEvent(new MouseRotationEvent((float)cursorDeltaX, (float)cursorDeltaY));
      if (!event.isCancelled()) {
         double deltaX = (double)event.getCursorDeltaX();
         double deltaY = (double)event.getCursorDeltaY();
         if (FreeLookController.isActive()) {
            FreeLookController.applyMouseDelta(deltaX, deltaY);
         } else {
            original.call(new Object[]{player, deltaX, deltaY});
            Manager.postEvent(
               new MouseAppliedRotationEvent(event.getCursorDeltaX(), event.getCursorDeltaY(), beforeYaw, beforePitch, player.getYRot(), player.getXRot())
            );
         }
      }
   }

   @Unique
   private static MouseButtonEvent daamky$toDragEvent(MouseButtonEvent event) {
      float scale = Render2DCoordinateSpace.guiIndependentScale();
      if (Math.abs(scale - 1.0F) <= 1.0E-4F) {
         return event;
      } else {
         MouseButtonInfo info = new MouseButtonInfo(event.button(), event.modifiers());
         return new MouseButtonEvent(event.x() / (double)scale, event.y() / (double)scale, info);
      }
   }
}
