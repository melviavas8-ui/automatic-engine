package daamky.ua.utils.player;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.PlayerInput;
import daamky.ua.IMinecraft;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.movement.AutoSprint;
import daamky.ua.manager.Manager;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;

public final class PlayerInventoryUtil implements IMinecraft {
   private PlayerInventoryUtil() {
   }

   public static void swapWithBypassGrim(Runnable runnable) {
      if (mc.player != null && mc.getNetworkHandler() != null && runnable != null) {
         if (mc.currentScreen instanceof InventoryScreen) {
            runnable.run();
         } else {
            PlayerInput previousInput = mc.player.input.playerInput;
            boolean wasSprinting = mc.player.isSprinting();

            try {
               mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
               mc.player.setSprinting(false);
               if (wasSprinting) {
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
               }

               ModuleManager modules = Manager.getModules();
               if ((modules == null || !modules.isEnabled(AutoSprint.class)) && mc.options != null) {
                  mc.options.sprintKey.setPressed(false);
               }

               runnable.run();
            } catch (Exception var7) {
               var7.printStackTrace();
            } finally {
               PlayerInteractionHelper.sendPacketWithOutEvent(new CloseHandledScreenC2SPacket(0));
               if (mc.getNetworkHandler() != null) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(previousInput));
               }
            }
         }
      }
   }
}
