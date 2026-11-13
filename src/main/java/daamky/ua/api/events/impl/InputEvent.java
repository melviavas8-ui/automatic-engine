package daamky.ua.api.events.impl;

import net.minecraft.util.PlayerInput;
import daamky.ua.api.events.CancellableEvent;

public class InputEvent extends CancellableEvent {
   private PlayerInput input;

   public InputEvent(PlayerInput input) {
      this.input = input;
   }

   public PlayerInput getInput() {
      return this.input;
   }

   public void setInput(PlayerInput input) {
      this.input = input;
   }

   public void setJumping(boolean jump) {
      this.input = new PlayerInput(
         this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), jump, this.input.sneak(), this.input.sprint()
      );
   }

   public void setSprinting(boolean sprint) {
      this.input = new PlayerInput(
         this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), this.input.jump(), this.input.sneak(), sprint
      );
   }

   public void setDirectional(boolean forward, boolean backward, boolean left, boolean right, boolean sneak, boolean sprint, boolean jump) {
      this.input = new PlayerInput(forward, backward, left, right, jump, sneak, sprint);
   }

   public void setDirectionalLow(boolean forward, boolean backward, boolean left, boolean right) {
      this.input = new PlayerInput(forward, backward, left, right, this.input.jump(), this.input.sneak(), this.input.sprint());
   }

   public void inputNone() {
      this.input = new PlayerInput(false, false, false, false, false, false, false);
   }

   public int forward() {
      return this.input.forward() ? 1 : (this.input.backward() ? -1 : 0);
   }

   public float sideways() {
      return this.input.left() ? 1.0F : (this.input.right() ? -1.0F : 0.0F);
   }
}
