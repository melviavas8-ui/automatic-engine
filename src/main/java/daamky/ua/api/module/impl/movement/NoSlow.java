package daamky.ua.api.module.impl.movement;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.impl.UsingItemEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;
import daamky.ua.utils.inventory.script.Script;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

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
      Minecraft client = event.getClient();
      if (client.player != null) {
         if (!this.itemMode.is("ReallyWorld") && !this.itemMode.is("SpookyTime")) {
            if (client.player.getUsedItemHand() != InteractionHand.MAIN_HAND && client.player.getUsedItemHand() != InteractionHand.OFF_HAND) {
               this.ticks = 0;
            } else {
               this.ticks++;
            }

            this.resetEatSprintBoostIfNeeded();
         } else {
            if (!client.player.isAutoSpinAttack()) {
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
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         if (event.getType() != 1) {
            InteractionHand first = client.player.getUsedItemHand();
            InteractionHand second = first == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            this.handleItemUse(client, event, first, second);
         } else {
            while (!this.script.isFinished()) {
               this.script.update();
            }
         }
      }
   }

   private void handleItemUse(Minecraft client, UsingItemEvent event, InteractionHand first, InteractionHand second) {
      if (this.itemMode.is("Old Grim")) {
         if (client.player.getOffhandItem().getUseAnimation() == ItemUseAnimation.NONE || client.player.getMainHandItem().getUseAnimation() == ItemUseAnimation.NONE) {
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
            boolean mainHandCrossbow = client.player.getMainHandItem().getItem() instanceof CrossbowItem;
            boolean offHandCrossbow = client.player.getOffhandItem().getItem() instanceof CrossbowItem;
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

         if (this.itemMode.is("FunTime") && this.ticks > 0 && client.player.getTicksUsingItem() > 1) {
            boolean mainHandCrossbow = client.player.getMainHandItem().getItem() instanceof CrossbowItem;
            boolean offHandCrossbow = client.player.getOffhandItem().getItem() instanceof CrossbowItem;
            if (mainHandCrossbow || offHandCrossbow) {
               event.setCancelled(true);
               this.ticks = 0;
            } else if (client.player.onGround() && this.isOnSnowOrCarpet(client)) {
               client.player.connection.send(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, client.player.blockPosition().above(), Direction.DOWN));
               event.setCancelled(true);
               this.ticks = 0;
            }
         }
      }
   }

   private boolean isOnSnowOrCarpet(Minecraft client) {
      if (client.player != null && client.level != null) {
         BlockPos playerPos = client.player.blockPosition();
         Block block = client.level.getBlockState(playerPos).getBlock();
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

   private void updateEatSprintBoost(Minecraft client) {
      if (client.player != null && client.options != null) {
         boolean eating = client.player.isUsingItem() && client.player.getUseItem().getUseAnimation() == ItemUseAnimation.EAT;
         if (eating) {
            if (!this.forcedSprintDuringEat) {
               this.sprintWasPressedBeforeEat = client.options.keySprint.isDown();
               this.forcedSprintDuringEat = true;
            }

            client.options.keySprint.setDown(true);
            client.player.setSprinting(true);
         } else {
            this.resetEatSprintBoostIfNeeded();
         }
      }
   }

   private void resetEatSprintBoostIfNeeded() {
      Minecraft client = Minecraft.getInstance();
      if (this.forcedSprintDuringEat && client.options != null) {
         if (!this.sprintWasPressedBeforeEat) {
            client.options.keySprint.setDown(false);
         }

         this.forcedSprintDuringEat = false;
         this.sprintWasPressedBeforeEat = false;
      }
   }
}
