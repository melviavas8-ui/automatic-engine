package daamky.ua.api.module.impl.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.CameraEvent;
import daamky.ua.api.events.impl.MouseRotationEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;

public final class FreeLook extends Module {
   public static final BindSetting FREE_LOOK_BIND = new BindSetting("Free Look", "Free look key.", KeyBind.keyboard(342));
   private static FreeLook instance;
   private Perspective perspective;
   private Angle angle;

   public FreeLook() {
      super("Free Look", "Free camera look while holding bind.", ModuleCategory.PLAYER);
      instance = this;
      this.register(FREE_LOOK_BIND);
   }

   public static Angle getActiveAngle() {
      FreeLook freeLook = instance;
      MinecraftClient client = MinecraftClient.getInstance();
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
   public void onTick(MinecraftClient client) {
      if (this.isBindDown(client)) {
         this.handleFreeLookActivation(client);
      } else if (this.perspective != null) {
         this.handleFreeLookDeactivation();
      }
   }

   @SubscribeEvent
   private void onMouseRotation(MouseRotationEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (!this.isBindDown(client)) {
         this.angle = null;
      } else {
         if (this.angle == null) {
            this.angle = MathAngle.cameraAngle();
         }

         this.angle.setYaw(this.angle.getYaw() + event.getCursorDeltaX() * 0.15F);
         this.angle.setPitch(MathHelper.clamp(this.angle.getPitch() + event.getCursorDeltaY() * 0.15F, -90.0F, 90.0F));
         event.setCancelled(true);
      }
   }

   @SubscribeEvent
   private void onCamera(CameraEvent event) {
      if (this.isBindDown(MinecraftClient.getInstance()) && this.angle != null) {
         event.setAngle(this.angle);
         event.setCancelled(true);
      }
   }

   private void handleFreeLookActivation(MinecraftClient client) {
      if (this.perspective == null) {
         this.perspective = client.options.getPerspective();
      }

      if (client.options.getPerspective().isFirstPerson()) {
         client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
      }

      if (this.angle == null) {
         this.angle = MathAngle.cameraAngle();
      }
   }

   private void handleFreeLookDeactivation() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.perspective != null && client.options != null) {
         client.options.setPerspective(this.perspective);
      }

      this.perspective = null;
      this.angle = null;
   }

   private boolean isBindDown(MinecraftClient client) {
      return client != null && client.currentScreen == null && client.getWindow() != null && FREE_LOOK_BIND.getValue().isDown(client.getWindow().getHandle());
   }
}
