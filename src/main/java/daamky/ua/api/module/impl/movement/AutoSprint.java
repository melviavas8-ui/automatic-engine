package daamky.ua.api.module.impl.movement;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class AutoSprint extends Module {
   StopWatch stopWatch = new StopWatch();

   public AutoSprint() {
      super("Auto Sprint", "Keeps sprint enabled while moving.", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.options != null && client.getWindow() != null) {
         boolean moving = this.isPressed(client, client.options.keyUp)
            || this.isPressed(client, client.options.keyDown)
            || this.isPressed(client, client.options.keyLeft)
            || this.isPressed(client, client.options.keyRight);
         client.options.keySprint.setDown(moving);
         if (!moving && client.screen != null && client.player.isSprinting()) {
            client.player.setSprinting(false);
            this.stopWatch.every(50.0);
            client.player.setSprinting(true);
         }
      }
   }

   private boolean isPressed(Minecraft client, KeyMapping key) {
      Key inputKey = InputConstants.getKey(key.saveString());
      long handle = client.getWindow().handle();

      return switch (inputKey.getType()) {
         case KEYSYM -> InputConstants.isKeyDown(client.getWindow(), inputKey.getValue());
         case MOUSE -> GLFW.glfwGetMouseButton(handle, inputKey.getValue()) == 1;
         default -> false;
      };
   }
}
