package daamky.ua.api.command.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import daamky.ua.IMinecraft;
import daamky.ua.api.command.Command;
import daamky.ua.api.command.helpers.TabCompleteHelper;

public final class VClipCommand extends Command implements IMinecraft {
   private static final int CHEST_MENU_SLOT = 6;

   public VClipCommand() {
      super("vclip", "Вертикальное перемещение", "vc");
   }

   @Override
   public void execute(String label, String[] args) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (args.length != 1) {
            this.printUsage();
         } else {
            String value = args[0].toLowerCase(Locale.ROOT);
            if (value.equals("up")) {
               this.clipToFreeSpace(true);
            } else if (value.equals("down")) {
               this.clipToFreeSpace(false);
            } else {
               try {
                  this.clip(Double.parseDouble(value.replace(',', '.')));
               } catch (NumberFormatException var5) {
                  this.logDirect("Неверное расстояние: " + args[0], Formatting.RED);
                  this.printUsage();
               }
            }
         }
      } else {
         this.logDirect("Команда доступна только в мире.", Formatting.RED);
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      return args.length != 1 ? Stream.empty() : new TabCompleteHelper().append("up", "down", "1", "-1", "5", "-5").filterPrefix(args[0]).stream();
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Перемещает игрока по вертикали.", "Использование:", "> vclip <расстояние>", "> vclip up", "> vclip down");
   }

   private void clipToFreeSpace(boolean up) {
      double offset = this.scanVerticalOffset(mc.player.getBlockPos(), up);
      if (offset == 0.0) {
         this.logDirect("Не удалось найти свободное место.", Formatting.RED);
      } else {
         this.clip(offset);
      }
   }

   private void clip(double yOffset) {
      if (Double.isFinite(yOffset) && yOffset != 0.0) {
         boolean elytraEquipped = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
         int elytraInventorySlot = elytraEquipped ? -1 : this.findElytraInventorySlot();
         boolean useElytra = elytraEquipped || elytraInventorySlot >= 0;
         if (elytraInventorySlot >= 0) {
            this.swapInventoryWithChest(elytraInventorySlot);
         }

         try {
            this.teleport(yOffset, useElytra);
         } finally {
            if (elytraInventorySlot >= 0) {
               this.swapInventoryWithChest(elytraInventorySlot);
            }
         }

         this.logDirect(String.format(Locale.ROOT, "Перемещение по Y: %+.1f блока.", yOffset), Formatting.GREEN);
      } else {
         this.logDirect("Расстояние должно быть конечным числом и не может быть равно 0.", Formatting.RED);
      }
   }

   private void teleport(double yOffset, boolean elytra) {
      double x = mc.player.getX();
      double y = mc.player.getY();
      double z = mc.player.getZ();
      if (elytra) {
         for (int i = 0; i < 2; i++) {
            this.sendPosition(x, y, z);
         }

         this.startFallFlying();
         this.sendPosition(x, y + yOffset, z);
         this.startFallFlying();
      } else {
         int packets = Math.max((int)(Math.abs(yOffset) / 1000.0), 3);

         for (int i = 0; i < packets; i++) {
            mc.player.networkHandler.sendPacket(new OnGroundOnly(mc.player.isOnGround(), mc.player.horizontalCollision));
         }

         this.sendPosition(x, y + yOffset, z);
      }

      mc.player.setPosition(x, y + yOffset, z);
      mc.player.fallDistance = 0.0;
   }

   private void sendPosition(double x, double y, double z) {
      mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false, mc.player.horizontalCollision));
   }

   private void startFallFlying() {
      mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
   }

   private double scanVerticalOffset(BlockPos playerPos, boolean up) {
      int start = up ? 3 : -1;
      int end = up ? 320 : -320;
      int step = up ? 1 : -1;

      for (int offset = start; offset != end; offset += step) {
         BlockPos targetPos = playerPos.add(0, offset, 0);
         if (!up && mc.world.getBlockState(targetPos).isOf(Blocks.BEDROCK)) {
            this.logDirect("Нельзя переместиться ниже бедрока.", Formatting.RED);
            return 0.0;
         }

         if (mc.world.getBlockState(targetPos).isAir()) {
            return (double)offset + (up ? 1.0 : -1.0);
         }
      }

      return 0.0;
   }

   private int findElytraInventorySlot() {
      for (int slot = 0; slot < 36; slot++) {
         if (mc.player.getInventory().getStack(slot).isOf(Items.ELYTRA)) {
            return slot;
         }
      }

      return -1;
   }

   private void swapInventoryWithChest(int inventorySlot) {
      int menuSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
      int containerId = mc.player.playerScreenHandler.syncId;
      mc.interactionManager.clickSlot(containerId, menuSlot, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(containerId, 6, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(containerId, menuSlot, 0, SlotActionType.PICKUP, mc.player);
   }

   private void printUsage() {
      this.logDirect("Использование: .vclip <число|up|down>", Formatting.RED);
   }
}
