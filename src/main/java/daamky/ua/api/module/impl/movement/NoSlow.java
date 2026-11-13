package daamky.ua.api.module.impl.movement;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.impl.UsingItemEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;
import daamky.ua.utils.inventory.script.Script;

public final class NoSlow extends Module {
   private final ModeSetting itemMode = this.register(
      new ModeSetting("Item Mode", "Item slowdown bypass mode.", "ReallyWorld", "Old Grim", "ReallyWorld", "FunTime", "SpookyTime", "FunTimeCrossBow")
   );
   private final Script script = new Script();
   private int ticks;
   private int cycleCounter;
   private boolean forcedSprintDuringEat;
   private boolean sprintWasPressedBeforeEat;

   public NoSlow() {
      super("No Slow", "Prevents item-use movement slowdown.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onDisable() {
      this.resetEatSprintBoostIfNeeded();
      this.ticks = 0;
      this.cycleCounter = 0;
      this.script.cleanup();
   }

   @SubscribeEvent
   private void onUpdate(TickEvent event) {
      MinecraftClient client = event.getClient();
      if (client.player != null) {
         if (!this.itemMode.is("ReallyWorld") && !this.itemMode.is("SpookyTime")) {
            if (client.player.getActiveHand() != Hand.MAIN_HAND && client.player.getActiveHand() != Hand.OFF_HAND) {
               this.ticks = 0;
            } else {
               this.ticks++;
            }

            this.resetEatSprintBoostIfNeeded();
         } else {
            if (!client.player.isUsingRiptide()) {
               if (client.player.isUsingItem()) {
                  this.ticks++;
               } else {
                  this.ticks = 0;
                  this.cycleCounter = 0;
               }
            }

            this.updateEatSprintBoost(client);
         }
      }
   }

   @SubscribeEvent
   private void onUsingItem(UsingItemEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         if (event.getType() != 1) {
            Hand first = client.player.getActiveHand();
            Hand second = first == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
            this.handleItemUse(client, event, first, second);
         } else {
            while (!this.script.isFinished()) {
               this.script.update();
            }
         }
      }
   }

   private void handleItemUse(MinecraftClient client, UsingItemEvent event, Hand first, Hand second) {
      if (this.itemMode.is("Old Grim")) {
         if (client.player.getOffHandStack().getUseAction() == UseAction.NONE || client.player.getMainHandStack().getUseAction() == UseAction.NONE) {
            PlayerInteractionHelper.interactItem(first);
            PlayerInteractionHelper.interactItem(second);
            event.setCancelled(true);
         }
      } else if (this.itemMode.is("ReallyWorld")) {
         int[] thresholds = client.player.isJumping() ? new int[]{2, 2, 4} : new int[]{2, 3, 4};
         int threshold = thresholds[this.cycleCounter % thresholds.length];
         if (this.ticks >= threshold) {
            event.setCancelled(true);
            this.ticks = 0;
            this.cycleCounter++;
         }
      } else if (this.itemMode.is("SpookyTime")) {
         int[] thresholds = new int[]{2, 2, 3};
         int threshold = thresholds[this.cycleCounter % 2];
         if (this.ticks >= threshold) {
            event.setCancelled(true);
            this.ticks = 0;
            this.cycleCounter++;
         }
      } else {
         if (this.itemMode.is("FunTimeCrossBow")) {
            boolean mainHandCrossbow = client.player.getMainHandStack().getItem() instanceof CrossbowItem;
            boolean offHandCrossbow = client.player.getOffHandStack().getItem() instanceof CrossbowItem;
            if (mainHandCrossbow || offHandCrossbow) {
               int[] thresholds = new int[]{1, 1, 1};
               int threshold = thresholds[this.cycleCounter % 1];
               if (this.ticks >= threshold) {
                  event.setCancelled(true);
                  this.ticks = 0;
                  this.cycleCounter++;
               }

               return;
            }
         }

         if (this.itemMode.is("FunTime") && this.ticks > 0 && client.player.getItemUseTime() > 1) {
            boolean mainHandCrossbow = client.player.getMainHandStack().getItem() instanceof CrossbowItem;
            boolean offHandCrossbow = client.player.getOffHandStack().getItem() instanceof CrossbowItem;
            if (mainHandCrossbow || offHandCrossbow) {
               event.setCancelled(true);
               this.ticks = 0;
            } else if (client.player.isOnGround() && this.isOnSnowOrCarpet(client)) {
               client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, client.player.getBlockPos().up(), Direction.DOWN));
               event.setCancelled(true);
               this.ticks = 0;
            }
         }
      }
   }

   private boolean isOnSnowOrCarpet(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         BlockPos playerPos = client.player.getBlockPos();
         Block block = client.world.getBlockState(playerPos).getBlock();
         return block == Blocks.SNOW
            || block == Blocks.WHITE_CARPET
            || block == Blocks.ORANGE_CARPET
            || block == Blocks.MAGENTA_CARPET
            || block == Blocks.LIGHT_BLUE_CARPET
            || block == Blocks.YELLOW_CARPET
            || block == Blocks.LIME_CARPET
            || block == Blocks.PINK_CARPET
            || block == Blocks.GRAY_CARPET
            || block == Blocks.LIGHT_GRAY_CARPET
            || block == Blocks.CYAN_CARPET
            || block == Blocks.PURPLE_CARPET
            || block == Blocks.BLUE_CARPET
            || block == Blocks.BROWN_CARPET
            || block == Blocks.GREEN_CARPET
            || block == Blocks.RED_CARPET
            || block == Blocks.BLACK_CARPET;
      } else {
         return false;
      }
   }

   private void updateEatSprintBoost(MinecraftClient client) {
      if (client.player != null && client.options != null) {
         boolean eating = client.player.isUsingItem() && client.player.getActiveItem().getUseAction() == UseAction.EAT;
         if (eating) {
            if (!this.forcedSprintDuringEat) {
               this.sprintWasPressedBeforeEat = client.options.sprintKey.isPressed();
               this.forcedSprintDuringEat = true;
            }

            client.options.sprintKey.setPressed(true);
            client.player.setSprinting(true);
         } else {
            this.resetEatSprintBoostIfNeeded();
         }
      }
   }

   private void resetEatSprintBoostIfNeeded() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.forcedSprintDuringEat && client.options != null) {
         if (!this.sprintWasPressedBeforeEat) {
            client.options.sprintKey.setPressed(false);
         }

         this.forcedSprintDuringEat = false;
         this.sprintWasPressedBeforeEat = false;
      }
   }
}
