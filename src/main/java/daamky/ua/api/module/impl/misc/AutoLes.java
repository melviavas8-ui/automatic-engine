package daamky.ua.api.module.impl.misc;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.utils.network.Network;

public final class AutoLes extends Module {
   private static final int SEARCH_RANGE = 6;
   private static final double SEARCH_RANGE_SQUARED = 36.0;
   private static final int STOP_PACKETS_PER_TICK = 3;
   private static final double MILLIS_PER_MINUTE = 60000.0;
   private final StopWatch sellWatch = new StopWatch();
   private final StopWatch payWatch = new StopWatch();
   private final StopWatch restartWatch = new StopWatch();
   private final BooleanSetting autoPay = this.register(new BooleanSetting("Auto Pay", "Send money.", false));
   private final StringSetting payName = this.register(new StringSetting("Name", "Pay target nickname.", "nick", 32));
   private final NumberSetting payAmount = this.register(new NumberSetting("Amount", "Pay amount.", 1000.0, 500.0, 25000.0, 100.0));
   private final NumberSetting delay = this.register(new NumberSetting("Delay", "Delay in seconds.", 20.0, 1.0, 60.0, 1.0));
   private final BooleanSetting restart = this.register(new BooleanSetting("Restart", "Restart module by timer.", false));
   private final NumberSetting restartDelay = this.register(new NumberSetting("Restart Time", "Restart delay in minutes.", 5.0, 1.0, 30.0, 1.0));
   private BlockPos currentTarget;
   private BlockPos breakingTarget;

   public AutoLes() {
      super("Auto Les", "Automates routines on /warp les.", ModuleCategory.MISC);
      this.payName.visibleWhen(this.autoPay::getValue);
      this.payAmount.visibleWhen(this.autoPay::getValue);
      this.delay.visibleWhen(this.autoPay::getValue);
      this.restartDelay.visibleWhen(this.restart::getValue);
   }

   @Override
   public void onTick(Minecraft client) {
      if (!Network.isReallyWorld()) {
         this.setEnabled(false);
      } else {
         LocalPlayer player = client.player;
         ClientLevel level = client.level;
         ClientPacketListener connection = client.getConnection();
         if (player != null && level != null && connection != null) {
            this.runRestartTimer();
            if (this.isEnabled()) {
               this.runSell(player);
               this.runPay(player);
               this.runNuker(player, level, connection);
            }
         } else {
            this.resetBreakState();
         }
      }
   }

   @Override
   protected void onEnable() {
      Minecraft client = Minecraft.getInstance();
      if (Network.isReallyWorld() && client.player != null && client.level != null && client.getConnection() != null) {
         this.resetTimers();
         this.resetBreakState();
      } else {
         this.setEnabled(false);
      }
   }

   @Override
   protected void onDisable() {
      this.resetTimers();
      this.resetBreakState();
   }

   private void runRestartTimer() {
      if (!this.restart.getValue()) {
         this.restartWatch.reset();
      } else {
         if (this.restartWatch.finished(this.restartDelay.getValue() * 60000.0)) {
            this.restartModule();
         }
      }
   }

   private void runSell(LocalPlayer player) {
      if (this.sellWatch.finished(70500.0)) {
         player.connection.sendCommand("sellwood");
         this.sellWatch.reset();
      }
   }

   private void runPay(LocalPlayer player) {
      if (this.autoPay.getValue()) {
         String targetName = this.payName.getValue() == null ? "" : this.payName.getValue().trim();
         if (!targetName.isEmpty()) {
            if (this.payWatch.finished(this.delay.getValue() * 500.0 + 200.0)) {
               player.connection.sendCommand("pay " + targetName + " " + this.payAmount.getValue().intValue());
               this.payWatch.reset();
            }
         }
      }
   }

   private void runNuker(LocalPlayer player, ClientLevel level, ClientPacketListener connection) {
      BlockPos target = this.resolveTarget(player.blockPosition(), level);
      if (target == null) {
         this.currentTarget = null;
         this.clearBreakingState();
      } else {
         if (!target.equals(this.currentTarget)) {
            this.clearBreakingState();
            this.currentTarget = target;
         }

         BlockPos activeTarget = this.currentTarget;
         if (activeTarget != null && this.isValidWoodTarget(activeTarget, level)) {
            if (!activeTarget.equals(this.breakingTarget)) {
               this.sendBreakPacket(connection, Action.START_DESTROY_BLOCK, activeTarget);
               this.breakingTarget = activeTarget;
            }

            for (int i = 0; i < 3; i++) {
               if (level.getBlockState(activeTarget).isAir()) {
                  this.currentTarget = null;
                  this.clearBreakingState(false);
                  return;
               }

               this.sendBreakPacket(connection, Action.STOP_DESTROY_BLOCK, activeTarget);
            }

            if (level.getBlockState(activeTarget).isAir()) {
               this.currentTarget = null;
               this.clearBreakingState(false);
            } else {
               player.swing(InteractionHand.MAIN_HAND);
            }
         } else {
            this.currentTarget = null;
            this.clearBreakingState();
         }
      }
   }

   private BlockPos resolveTarget(BlockPos playerPos, ClientLevel level) {
      return this.currentTarget != null && this.isValidWoodTarget(this.currentTarget, level) ? this.currentTarget : this.findNearestWood(playerPos, level);
   }

   private BlockPos findNearestWood(BlockPos playerPos, ClientLevel level) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return null;
      } else {
         BlockPos closest = null;
         double closestDistance = Double.MAX_VALUE;

         for (int x = -6; x <= 6; x++) {
            for (int y = -6; y <= 6; y++) {
               for (int z = -6; z <= 6; z++) {
                  BlockPos position = playerPos.offset(x, y, z);
                  if (this.isValidWoodTarget(position, level)) {
                     double distance = player.distanceToSqr(Vec3.atCenterOf(position));
                     if (distance < closestDistance) {
                        closest = position;
                        closestDistance = distance;
                     }
                  }
               }
            }
         }

         return closest;
      }
   }

   private boolean isValidWoodTarget(BlockPos position, ClientLevel level) {
      if (!this.isWithinRange(position)) {
         return false;
      } else {
         BlockState state = level.getBlockState(position);
         return this.isWoodState(state) && this.canBreak(state, level, position);
      }
   }

   private boolean isWoodState(BlockState state) {
      String name = state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
      return name.contains("log") || name.contains("wood") || name.contains("stem") || state.is(BlockTags.LOGS);
   }

   private boolean canBreak(BlockState state, ClientLevel level, BlockPos position) {
      return !state.isAir() && state.getDestroySpeed(level, position) >= 0.0F;
   }

   private void sendBreakPacket(ClientPacketListener connection, Action action, BlockPos target) {
      connection.send(new ServerboundPlayerActionPacket(action, target, Direction.UP));
   }

   private boolean isWithinRange(BlockPos position) {
      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && player.distanceToSqr(Vec3.atCenterOf(position)) <= 36.0;
   }

   private void clearBreakingState() {
      this.clearBreakingState(true);
   }

   private void clearBreakingState(boolean abort) {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      if (abort && this.breakingTarget != null && connection != null) {
         this.sendBreakPacket(connection, Action.ABORT_DESTROY_BLOCK, this.breakingTarget);
      }

      this.breakingTarget = null;
   }

   private void resetBreakState() {
      this.currentTarget = null;
      this.clearBreakingState();
   }

   private void resetTimers() {
      this.sellWatch.reset();
      this.payWatch.reset();
      this.restartWatch.reset();
   }

   private void restartModule() {
      this.setEnabled(false);
      this.setEnabled(true);
   }
}
