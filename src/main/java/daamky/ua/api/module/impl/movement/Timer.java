package daamky.ua.api.module.impl.movement;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;

public final class Timer extends Module {
   private static final float DEFAULT_MULTIPLIER = 1.0F;
   private static final float MAX_MULTIPLIER = 10.0F;
   private static final float CHARGE_MULTIPLIER = 0.25F;
   private static final float CYCLE_BOOST_MULTIPLIER = 2.0F;
   private static final long CHARGE_DURATION_NANOS = 1200000000L;
   private static final long MAX_BOOST_DURATION_NANOS = 2400000000L;
   private static final int FULL_BOOST_JUMPS = 5;
   private static Timer instance;
   private static float boostMultiplier = 1.0F;
   private final ModeSetting mode = this.register(
      new ModeSetting("Mode", "Custom sets a constant speed; Boost charges and releases timer speed while moving.", "Custom", "Custom")
   );
   private final NumberSetting speed = this.register(new NumberSetting("Speed", "Game speed multiplier for Custom mode.", 1.0, 0.1, 10.0, 0.1));
   private final Queue<ServerboundPongPacket> delayedTransactions = new ConcurrentLinkedQueue<>();
   private Timer.Phase phase = Timer.Phase.CHARGING;
   private boolean cycleActive;
   private boolean airborne;
   private boolean releasingTransactions;
   private int completedJumps;
   private int groundTicks;
   private long phaseStartedAt;

   public Timer() {
      super("Timer", "Speeds up client ticks directly or with a charged movement boost.", ModuleCategory.MOVEMENT);
      instance = this;
      this.speed.visibleWhen(() -> this.mode.is("Custom"));
   }

   public NumberSetting getSpeed() {
      return this.speed;
   }

   @Override
   protected void onEnable() {
      this.stopCycle();
   }

   @Override
   protected void onDisable() {
      this.stopCycle();
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      if (!this.mode.is("Boost")) {
         if (this.cycleActive || !this.delayedTransactions.isEmpty()) {
            this.stopCycle();
         }
      } else {
         LocalPlayer player = mc.player;
         if (player != null && this.isMoving(player.input.keyPresses)) {
            long now = System.nanoTime();
            if (!this.cycleActive) {
               this.cycleActive = true;
               this.beginCharging();
            } else if (this.phase == Timer.Phase.CHARGING && now - this.phaseStartedAt >= 1200000000L) {
               this.beginBoosting();
            } else if (this.phase == Timer.Phase.BOOSTING && now - this.phaseStartedAt >= 2400000000L) {
               this.beginCharging();
            }
         } else {
            this.stopCycle();
         }
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      LocalPlayer player = event.getClient().player;
      if (player != null && this.cycleActive && this.phase == Timer.Phase.BOOSTING) {
         if (player.verticalCollision) {
            if (this.airborne) {
               this.airborne = false;
               this.completedJumps++;
            }
         } else {
            this.airborne = true;
            if (this.completedJumps >= 5 && player.getDeltaMovement().y <= 0.0) {
               this.beginCharging();
            }
         }
      }
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      LocalPlayer player = mc.player;
      Input input = event.getInput();
      if (player != null && this.cycleActive && this.phase == Timer.Phase.BOOSTING && this.isMoving(input)) {
         this.groundTicks = player.verticalCollision ? this.groundTicks + 1 : 0;
         event.setSprinting(true);
         if (this.groundTicks > 0) {
            event.setJumping(true);
         }
      } else {
         this.groundTicks = 0;
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (this.mode.is("Boost") && !this.releasingTransactions) {
         if (event.isSend() && this.cycleActive && event.getPacket() instanceof ServerboundPongPacket packet) {
            this.delayedTransactions.add(packet);
            event.cancel();
         } else if (event.isReceive() && this.cycleActive && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.beginCharging();
         }
      }
   }

   public static float getCurrentMultiplier() {
      Timer timer = instance;
      float timerMultiplier = 1.0F;
      if (timer != null && timer.isEnabled() && timer.mode.is("Boost")) {
         timerMultiplier = timer.cycleActive ? (timer.phase == Timer.Phase.CHARGING ? 0.25F : 2.0F) : 1.0F;
      } else if (timer != null && timer.isEnabled()) {
         Minecraft client = Minecraft.getInstance();
         boolean startingJump = client.player != null && client.options != null && client.player.onGround() && client.options.keyJump.isDown();
         timerMultiplier = startingJump ? 1.0F : Math.max(0.1F, timer.speed.getFloat());
      }

      return Mth.clamp(timerMultiplier * boostMultiplier, 0.25F, 10.0F);
   }

   public static void setBoostMultiplier(float multiplier) {
      boostMultiplier = Mth.clamp(multiplier, 0.25F, 10.0F);
   }

   public static void resetBoostMultiplier() {
      boostMultiplier = 1.0F;
   }

   private boolean isMoving(Input input) {
      return input.forward() || input.backward() || input.left() || input.right();
   }

   private void beginCharging() {
      this.flushTransactions();
      this.phase = Timer.Phase.CHARGING;
      this.airborne = false;
      this.completedJumps = 0;
      this.groundTicks = 0;
      this.phaseStartedAt = System.nanoTime();
   }

   private void beginBoosting() {
      this.phase = Timer.Phase.BOOSTING;
      this.airborne = false;
      this.completedJumps = 0;
      this.groundTicks = 0;
      this.phaseStartedAt = System.nanoTime();
   }

   private void stopCycle() {
      this.flushTransactions();
      this.cycleActive = false;
      this.phase = Timer.Phase.CHARGING;
      this.airborne = false;
      this.completedJumps = 0;
      this.groundTicks = 0;
      this.phaseStartedAt = 0L;
   }

   private void flushTransactions() {
      ServerboundPongPacket packet;
      while ((packet = this.delayedTransactions.poll()) != null) {
         if (mc.getConnection() != null) {
            this.releasingTransactions = true;

            try {
               mc.getConnection().send(packet);
            } finally {
               this.releasingTransactions = false;
            }
         }
      }
   }

   private static enum Phase {
      CHARGING,
      BOOSTING;

      private Phase() {
      }
   }
}
