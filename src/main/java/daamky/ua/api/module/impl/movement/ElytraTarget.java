package daamky.ua.api.module.impl.movement;

import net.minecraft.client.util.InputUtil.Type;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.KeyEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.bind.InputType;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.sounds.SoundManager;

public final class ElytraTarget extends Module {
   private static ElytraTarget instance;
   public static boolean shouldElytraTarget;
   private final NumberSetting elytraFindRange = this.register(
      new NumberSetting("Дистанция наводки", "Дальность поиска цели во время полета на элитре.", 32.0, 6.0, 64.0, 1.0)
   );
   private final NumberSetting elytraForward = this.register(
      new NumberSetting("Значение перегона", "Упреждение цели во время элитра таргета.", 3.0, 0.0, 6.0, 0.1)
   );
   private final BindSetting forward = this.register(new BindSetting("Кнопка вкл/выкл перегона", "", KeyBind.NONE));

   public ElytraTarget() {
      super("ElytraTarget", "Elytra Target", ModuleCategory.MOVEMENT);
      instance = this;
   }

   public static ElytraTarget getInstance() {
      return instance;
   }

   public static boolean isTargetingActive() {
      return instance != null && instance.isEnabled() && shouldElytraTarget;
   }

   @Override
   protected void onDisable() {
      shouldElytraTarget = false;
   }

   @SubscribeEvent
   private void onEventKey(KeyEvent event) {
      if (event.action() == 1 && this.isKeyDown(event)) {
         shouldElytraTarget = !shouldElytraTarget;
         SoundManager.playSound(shouldElytraTarget ? SoundManager.MODULE_ENABLE : SoundManager.MODULE_DISABLE, 1.0F, 1.0F);
      }
   }

   public float getRange() {
      return this.elytraFindRange.getFloat();
   }

   public float getForward() {
      return this.elytraForward.getFloat();
   }

   private boolean isKeyDown(KeyEvent event) {
      KeyBind bind = this.forward.getValue();
      if (!bind.isBound() || event.key() != bind.getCode()) {
         return false;
      } else if (bind.getType() == InputType.KEYBOARD) {
         return event.type() == Type.KEYSYM;
      } else {
         return bind.getType() == InputType.MOUSE ? event.type() == Type.MOUSE : false;
      }
   }
}
