package daamky.ua.api.module.impl.player;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.DeathScreenEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import net.minecraft.client.Minecraft;

public final class AutoRespawn extends Module {
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Respawn mode.", "Default", "Default"));

   public AutoRespawn() {
      super("Auto Respawn", "Automatically respawns after death.", ModuleCategory.PLAYER);
   }

   @SubscribeEvent
   private void onDeathScreen(DeathScreenEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (this.mode.is("Default") && client.player != null) {
         client.player.respawn();
         client.setScreen(null);
      }
   }
}
