package daamky.ua.api.module.impl.movement;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;
import daamky.ua.utils.move.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;

public final class NoWeb extends Module {
   private final ModeSetting webMode = this.register(new ModeSetting("Mode", "Cobweb bypass mode.", "Grim", "Grim"));

   public NoWeb() {
      super("No Web", "Reduces cobweb slowdown.", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.level != null) {
         if (this.webMode.is("Grim") && PlayerInteractionHelper.isPlayerInBlock(Blocks.COBWEB)) {
            double[] speed = MoveUtil.calculateDirection(0.35);
            client.player.push(speed[0], 0.0, speed[1]);
            client.player.setDeltaMovement(speed[0], client.options.keyJump.isDown() ? 0.65 : (client.options.keyShift.isDown() ? -0.65 : 0.0), speed[1]);
         }
      }
   }
}
