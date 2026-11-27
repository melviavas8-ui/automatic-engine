package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.DeathScreenEvent;
import daamky.ua.manager.Manager;
import net.minecraft.client.gui.screens.DeathScreen;

@Mixin({DeathScreen.class})
public abstract class DeathScreenMixin {
   public DeathScreenMixin() {
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void daamky$deathScreenTick(CallbackInfo ci) {
      Manager.postEvent(new DeathScreenEvent());
   }
}
