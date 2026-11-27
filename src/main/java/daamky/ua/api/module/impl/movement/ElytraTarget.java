package daamky.ua.api.module.impl.movement;

import com.mojang.blaze3d.platform.InputConstants.Type;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.KeyEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.settings.bind.InputType;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.sounds.SoundManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class ElytraTarget extends Module {
   private static ElytraTarget instance;
   public static boolean shouldElytraTarget;
   private final StopWatch fireworkTimer = new StopWatch();
   private final NumberSetting elytraFindRange = this.register(
      new NumberSetting("Дистанция наводки", "Дальность поиска цели во время полета на элитре.", 32.0, 6.0, 64.0, 1.0)
   );
   private final NumberSetting elytraForward = this.register(
      new NumberSetting("Значение перегона", "Упреждение цели во время элитра таргета.", 3.0, 0.0, 6.0, 0.1)
   );
   private final BindSetting forward = this.register(new BindSetting("Кнопка вкл/выкл перегона", "", KeyBind.NONE));
   private final BooleanSetting autoFirework = this.register(
      new BooleanSetting("Авто фейерверк", "Сам берет фейерверки и использует их, пока Аура держит цель в полете.", true)
   );
   private final BindSetting fireworkToggleBind = this.register(new BindSetting("Кнопка вкл/выкл фейерверков", "", KeyBind.NONE));
   private final ModeSetting fireworkSlot = this.register(
      new ModeSetting(
         "Слот фейерверка",
         "Вторая рука: фейерверк кладется во вторую руку. Скрытно: основная рука не меняется, вторая рука не трогается.",
         "Вторая рука",
         "Вторая рука",
         "Скрытно"
      )
   );
   private final NumberSetting fireworkDelay = this.register(
      new NumberSetting("Задержка фейерверка", "Минимальная пауза между использованиями, мс.", 150.0, 50.0, 1000.0, 10.0)
   );
   private final NumberSetting fireworkSpeed = this.register(
      new NumberSetting("Скорость для буста", "Фейерверк используется, пока скорость ниже этого значения.", 1.5, 0.5, 3.0, 0.1)
   );

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
      if (event.action() != 1) {
         return;
      }

      if (this.isKeyDown(event, this.forward)) {
         shouldElytraTarget = !shouldElytraTarget;
         SoundManager.playSound(shouldElytraTarget ? SoundManager.MODULE_ENABLE : SoundManager.MODULE_DISABLE, 1.0F, 1.0F);
      } else if (this.isKeyDown(event, this.fireworkToggleBind)) {
         boolean enabled = !this.autoFirework.getValue();
         this.autoFirework.setValue(enabled);
         SoundManager.playSound(enabled ? SoundManager.MODULE_ENABLE : SoundManager.MODULE_DISABLE, 1.0F, 1.0F);
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent.Pre event) {
      if (!this.autoFirework.getValue()) {
         return;
      }

      if (mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null) {
         return;
      }

      if (!mc.player.isFallFlying()) {
         return;
      }

      AuraModule aura = AuraModule.getInstance();
      if (aura == null || !aura.isEnabled() || AuraModule.target == null) {
         return;
      }

      if (!this.fireworkTimer.finished(this.fireworkDelay.getValue())) {
         return;
      }

      boolean silent = this.fireworkSlot.is("Скрытно") || this.offhandHoldsTotem();
      if (silent) {
         if (mc.player.getDeltaMovement().length() < this.fireworkSpeed.getValue()) {
            InventoryUtils.silentUseItem(Items.FIREWORK_ROCKET);
            this.fireworkTimer.reset();
         }

         return;
      }

      if (!mc.player.getOffhandItem().is(Items.FIREWORK_ROCKET)) {
         this.moveFireworkToOffhand();
         this.fireworkTimer.reset();
      } else if (mc.player.getDeltaMovement().length() < this.fireworkSpeed.getValue()) {
         this.useOffhandFirework();
         this.fireworkTimer.reset();
      }
   }

   public float getRange() {
      return this.elytraFindRange.getFloat();
   }

   public float getForward() {
      return this.elytraForward.getFloat();
   }

   private boolean offhandHoldsTotem() {
      return mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
   }

   private void moveFireworkToOffhand() {
      int slot = InventoryUtils.findItemAnywhere(Items.FIREWORK_ROCKET);
      if (slot == -1) {
         return;
      }

      InventoryUtils.swapToOffhand(InventoryUtils.wrapSlot(slot));
      InventoryUtils.closeScreen();
   }

   private void useOffhandFirework() {
      Angle rotation = AngleConnection.INSTANCE.getRotation();
      if (rotation == null) {
         rotation = MathAngle.cameraAngle();
      }

      InventoryUtils.sendUsePacket(InteractionHand.OFF_HAND, rotation.getYaw(), rotation.getPitch());
      mc.player.swing(InteractionHand.OFF_HAND);
   }

   private boolean isKeyDown(KeyEvent event, BindSetting setting) {
      KeyBind bind = setting.getValue();
      if (!bind.isBound() || event.key() != bind.getCode()) {
         return false;
      } else if (bind.getType() == InputType.KEYBOARD) {
         return event.type() == Type.KEYSYM;
      } else {
         return bind.getType() == InputType.MOUSE ? event.type() == Type.MOUSE : false;
      }
   }
}
