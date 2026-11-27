package daamky.ua.utils.player;

import daamky.ua.IMinecraft;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.movement.AutoSprint;
import daamky.ua.manager.Manager;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;

public final class PlayerInventoryUtil implements IMinecraft {
   private PlayerInventoryUtil() {
   }

   public static void swapWithBypassGrim(Runnable runnable) {
      if (mc.player != null && mc.getConnection() != null && runnable != null) {
         if (mc.screen instanceof InventoryScreen) {
            runnable.run();
         } else {
            Input previousInput = mc.player.input.keyPresses;
            boolean wasSprinting = mc.player.isSprinting();

            try {
               mc.getConnection().send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, false)));
               mc.player.setSprinting(false);
               if (wasSprinting) {
                  mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
               }

               ModuleManager modules = Manager.getModules();
               if ((modules == null || !modules.isEnabled(AutoSprint.class)) && mc.options != null) {
                  mc.options.keySprint.setDown(false);
               }

               runnable.run();
            } catch (Exception var7) {
               var7.printStackTrace();
            } finally {
               PlayerInteractionHelper.sendPacketWithOutEvent(new ServerboundContainerClosePacket(0));
               if (mc.getConnection() != null) {
                  mc.getConnection().send(new ServerboundPlayerInputPacket(previousInput));
               }
            }
         }
      }
   }
}
