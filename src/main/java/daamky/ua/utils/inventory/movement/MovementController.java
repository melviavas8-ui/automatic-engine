package daamky.ua.utils.inventory.movement;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class MovementController {
   private static final Minecraft mc = Minecraft.getInstance();
   private boolean forward;
   private boolean back;
   private boolean left;
   private boolean right;
   private boolean jump;
   private boolean sneak;
   private boolean sprint;
   private boolean saved = false;
   private boolean blocked = false;

   public MovementController() {
   }

   public void saveState() {
      if (mc.player != null) {
         this.forward = this.isKeyPressed(mc.options.keyUp);
         this.back = this.isKeyPressed(mc.options.keyDown);
         this.left = this.isKeyPressed(mc.options.keyLeft);
         this.right = this.isKeyPressed(mc.options.keyRight);
         this.jump = this.isKeyPressed(mc.options.keyJump);
         this.sneak = this.isKeyPressed(mc.options.keyShift);
         this.sprint = mc.player.isSprinting();
         this.saved = true;
      }
   }

   public void block() {
      if (mc.player != null) {
         mc.options.keyUp.setDown(false);
         mc.options.keyDown.setDown(false);
         mc.options.keyLeft.setDown(false);
         mc.options.keyRight.setDown(false);
         mc.options.keyJump.setDown(false);
         mc.options.keyShift.setDown(false);
         mc.options.keySprint.setDown(false);
         this.blocked = true;
      }
   }

   public void stopSprint() {
      if (mc.player != null) {
         mc.player.setSprinting(false);
         mc.options.keySprint.setDown(false);
      }
   }

   public void restore() {
      if (this.saved) {
         mc.options.keyUp.setDown(this.forward && this.isCurrentlyPressed(mc.options.keyUp));
         mc.options.keyDown.setDown(this.back && this.isCurrentlyPressed(mc.options.keyDown));
         mc.options.keyLeft.setDown(this.left && this.isCurrentlyPressed(mc.options.keyLeft));
         mc.options.keyRight.setDown(this.right && this.isCurrentlyPressed(mc.options.keyRight));
         mc.options.keyJump.setDown(this.jump && this.isCurrentlyPressed(mc.options.keyJump));
         mc.options.keyShift.setDown(this.sneak && this.isCurrentlyPressed(mc.options.keyShift));
         this.blocked = false;
         this.saved = false;
      }
   }

   public void restoreFromCurrent() {
      mc.options.keyUp.setDown(this.isCurrentlyPressed(mc.options.keyUp));
      mc.options.keyDown.setDown(this.isCurrentlyPressed(mc.options.keyDown));
      mc.options.keyLeft.setDown(this.isCurrentlyPressed(mc.options.keyLeft));
      mc.options.keyRight.setDown(this.isCurrentlyPressed(mc.options.keyRight));
      mc.options.keyJump.setDown(this.isCurrentlyPressed(mc.options.keyJump));
      mc.options.keyShift.setDown(this.isCurrentlyPressed(mc.options.keyShift));
      mc.options.keySprint.setDown(this.isCurrentlyPressed(mc.options.keySprint));
      this.blocked = false;
   }

   public boolean isPlayerStopped(double threshold) {
      if (mc.player == null) {
         return true;
      } else {
         double vx = Math.abs(mc.player.getDeltaMovement().x);
         double vz = Math.abs(mc.player.getDeltaMovement().z);
         return vx < threshold && vz < threshold;
      }
   }

   public boolean isBlocked() {
      return this.blocked;
   }

   public void reset() {
      this.saved = false;
      this.blocked = false;
   }

   private boolean isKeyPressed(KeyMapping key) {
      return key.isDown();
   }

   private boolean isCurrentlyPressed(KeyMapping key) {
      return InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(key.saveString()).getValue());
   }
}
