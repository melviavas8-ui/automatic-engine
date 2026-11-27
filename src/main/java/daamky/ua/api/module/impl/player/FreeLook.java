package daamky.ua.api.module.impl.player;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.CameraEvent;
import daamky.ua.api.events.impl.MouseRotationEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class FreeLook extends Module {
   public static final BindSetting FREE_LOOK_BIND = new BindSetting("Free Look", "Free look key.", KeyBind.keyboard(342));
   private static FreeLook instance;
   private CameraType perspective;
   private Angle angle;

   public FreeLook() {
      super("Free Look", "Free camera look while holding bind.", ModuleCategory.PLAYER);
      instance = this;
      this.register(FREE_LOOK_BIND);
   }

   public static Angle getActiveAngle() {
      FreeLook freeLook = instance;
      Minecraft client = Minecraft.getInstance();
      if (freeLook != null && freeLook.isEnabled() && freeLook.isBindDown(client)) {
         freeLook.handleFreeLookActivation(client);
         return freeLook.angle;
      } else {
         return null;
      }
   }

   @Override
   protected void onDisable() {
      this.handleFreeLookDeactivation();
   }

   @Override
   public void onTick(Minecraft client) {
      if (this.isBindDown(client)) {
         this.handleFreeLookActivation(client);
      } else if (this.perspective != null) {
         this.handleFreeLookDeactivation();
      }
   }

   @SubscribeEvent
   private void onMouseRotation(MouseRotationEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (!this.isBindDown(client)) {
         this.angle = null;
      } else {
         if (this.angle == null) {
            this.angle = MathAngle.cameraAngle();
         }

         this.angle.setYaw(this.angle.getYaw() + event.getCursorDeltaX() * 0.15F);
         this.angle.setPitch(Mth.clamp(this.angle.getPitch() + event.getCursorDeltaY() * 0.15F, -90.0F, 90.0F));
         event.setCancelled(true);
      }
   }

   @SubscribeEvent
   private void onCamera(CameraEvent event) {
      if (this.isBindDown(Minecraft.getInstance()) && this.angle != null) {
         event.setAngle(this.angle);
         event.setCancelled(true);
      }
   }

   private void handleFreeLookActivation(Minecraft client) {
      if (this.perspective == null) {
         this.perspective = client.options.getCameraType();
      }

      if (client.options.getCameraType().isFirstPerson()) {
         client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      }

      if (this.angle == null) {
         this.angle = MathAngle.cameraAngle();
      }
   }

   private void handleFreeLookDeactivation() {
      Minecraft client = Minecraft.getInstance();
      if (this.perspective != null && client.options != null) {
         client.options.setCameraType(this.perspective);
      }

      this.perspective = null;
      this.angle = null;
   }

   private boolean isBindDown(Minecraft client) {
      return client != null && client.screen == null && client.getWindow() != null && FREE_LOOK_BIND.getValue().isDown(client.getWindow().handle());
   }
}
