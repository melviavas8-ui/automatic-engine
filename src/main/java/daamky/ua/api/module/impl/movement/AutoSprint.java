package daamky.ua.api.module.impl.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;

public final class AutoSprint extends Module {
   StopWatch stopWatch = new StopWatch();

   public AutoSprint() {
      super("Auto Sprint", "Keeps sprint enabled while moving.", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.options != null && client.getWindow() != null) {
         boolean moving = this.isPressed(client, client.options.forwardKey)
            || this.isPressed(client, client.options.backKey)
            || this.isPressed(client, client.options.leftKey)
            || this.isPressed(client, client.options.rightKey);
         client.options.sprintKey.setPressed(moving);
         if (!moving && client.currentScreen != null && client.player.isSprinting()) {
            client.player.setSprinting(false);
            this.stopWatch.every(50.0);
            client.player.setSprinting(true);
         }
      }
   }

   private boolean isPressed(MinecraftClient client, KeyBinding key) {
      Key inputKey = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
      long handle = client.getWindow().getHandle();

      return switch (inputKey.getCategory()) {
         case KEYSYM -> InputUtil.isKeyPressed(client.getWindow(), inputKey.getCode());
         case MOUSE -> GLFW.glfwGetMouseButton(handle, inputKey.getCode()) == 1;
         default -> false;
      };
   }
}
