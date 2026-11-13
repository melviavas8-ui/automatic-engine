package daamky.ua.api.module.impl.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.AttackEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;

public final class ShiftTap extends Module {
   private long shiftTapEndTime = 0L;
   private boolean isModuleControllingSneak = false;
   private int shiftTapDuration = 1;
   private final MinecraftClient mc = MinecraftClient.getInstance();

   public ShiftTap() {
      super("Shift Tap", "Автоматический шифт в бою", ModuleCategory.COMBAT);
   }

   private void startShiftTap() {
      this.shiftTapEndTime = System.currentTimeMillis() + (long)this.shiftTapDuration;
      if (!this.isModuleControllingSneak) {
         this.mc.options.sneakKey.setPressed(true);
         this.isModuleControllingSneak = true;
      }
   }

   private void stopShiftTap() {
      if (this.isModuleControllingSneak) {
         this.mc.options.sneakKey.setPressed(this.isKeyPhysicallyPressed(this.mc.options.sneakKey));
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

   private boolean isKeyPhysicallyPressed(KeyBinding key) {
      if (this.mc.getWindow() == null) {
         return false;
      } else {
         Key inputKey = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
         long windowHandle = this.mc.getWindow().getHandle();

         return switch (inputKey.getCategory()) {
            case KEYSYM -> InputUtil.isKeyPressed(this.mc.getWindow(), inputKey.getCode());
            case MOUSE -> GLFW.glfwGetMouseButton(windowHandle, inputKey.getCode()) == 1;
            default -> false;
         };
      }
   }
}
