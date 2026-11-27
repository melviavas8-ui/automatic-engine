package daamky.ua.api.module.impl.combat;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.AttackEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class ShiftTap extends Module {
   private long shiftTapEndTime = 0L;
   private boolean isModuleControllingSneak = false;
   private int shiftTapDuration = 1;
   private final Minecraft mc = Minecraft.getInstance();

   public ShiftTap() {
      super("Shift Tap", "Автоматический шифт в бою", ModuleCategory.COMBAT);
   }

   private void startShiftTap() {
      this.shiftTapEndTime = System.currentTimeMillis() + (long)this.shiftTapDuration;
      if (!this.isModuleControllingSneak) {
         this.mc.options.keyShift.setDown(true);
         this.isModuleControllingSneak = true;
      }
   }

   private void stopShiftTap() {
      if (this.isModuleControllingSneak) {
         this.mc.options.keyShift.setDown(this.isKeyPhysicallyPressed(this.mc.options.keyShift));
         this.isModuleControllingSneak = false;
      }
   }

   @SubscribeEvent
   private void onAttack(AttackEvent event) {
      if (this.mc.player != null && this.mc.options != null) {
         this.startShiftTap();
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null && !this.mc.player.isSpectator()) {
         if (this.isModuleControllingSneak && System.currentTimeMillis() > this.shiftTapEndTime) {
            this.stopShiftTap();
         }
      } else {
         this.stopShiftTap();
      }
   }

   @Override
   protected void onDisable() {
      this.stopShiftTap();
   }

   private boolean isKeyPhysicallyPressed(KeyMapping key) {
      if (this.mc.getWindow() == null) {
         return false;
      } else {
         Key inputKey = InputConstants.getKey(key.saveString());
         long windowHandle = this.mc.getWindow().handle();

         return switch (inputKey.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(this.mc.getWindow(), inputKey.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(windowHandle, inputKey.getValue()) == 1;
            default -> false;
         };
      }
   }
}
