package daamky.ua.utils.inventory.bind;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.events.impl.HotBarScrollEvent;
import daamky.ua.api.settings.bind.InputType;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;

public final class InventoryBindHelper {
   public static final int SCROLL_UP_BIND = 1000;
   public static final int SCROLL_DOWN_BIND = 1001;
   public static final int MIDDLE_MOUSE_BIND = 1002;

   private InventoryBindHelper() {
   }

   public static boolean isHeld(MinecraftClient mc, BindSetting bind) {
      if (mc != null && mc.getWindow() != null && bind != null) {
         KeyBind keyBind = bind.getValue();
         if (keyBind != null && keyBind.isBound()) {
            return keyBind.getCode() == 1002 ? GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 2) == 1 : keyBind.isDown(mc.getWindow().getHandle());
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean matchesScroll(HotBarScrollEvent event, BindSetting bind) {
      if (event != null && bind != null) {
         KeyBind keyBind = bind.getValue();
         if (keyBind == null || keyBind.getType() != InputType.NONE) {
            return false;
         } else {
            return keyBind.getCode() == 1000 && event.getVertical() > 0.0 ? true : keyBind.getCode() == 1001 && event.getVertical() < 0.0;
         }
      } else {
         return false;
      }
   }
}
