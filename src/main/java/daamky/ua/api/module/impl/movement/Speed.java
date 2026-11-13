package daamky.ua.api.module.impl.movement;

import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.PlayerInput;
import daamky.ua.IMinecraft;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.PlayerTravelEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.move.MoveUtil;

public final class Speed extends Module {
   private static final float REALLY_WORLD_CHARGE_MULTIPLIER = 0.2F;
   private static final float REALLY_WORLD_BOOST_MULTIPLIER = 1.8F;
   private static final long REALLY_WORLD_CHARGE_DURATION_NANOS = 1250000000L;
   private static final long REALLY_WORLD_MAX_BOOST_DURATION_NANOS = 2400000000L;
   private static final int REALLY_WORLD_FULL_BOOST_JUMPS = 3;
   private static final float HOLY_WORLD_CHARGE_MULTIPLIER = 0.1F;
   private static final float HOLY_WORLD_BOOST_MULTIPLIER = 2.6F;
   private static final long HOLY_WORLD_CHARGE_DURATION_NANOS = 1250000000L;
   private static final long HOLY_WORLD_MAX_BOOST_DURATION_NANOS = 2400000000L;
   private static final int HOLY_WORLD_FULL_BOOST_JUMPS = 4;
   private static final float FUN_TIME_CHARGE_MULTIPLIER = 0.2F;
   private static final float FUN_TIME_BOOST_MULTIPLIER = 1.8F;
   private static final long FUN_TIME_CHARGE_DURATION_NANOS = 1350000000L;
   private static final long FUN_TIME_MAX_BOOST_DURATION_NANOS = 2000000000L;
   private static final int FUN_TIME_FULL_BOOST_JUMPS = 4;
   private static final float SPOOKY_TIME_CHARGE_MULTIPLIER = 0.01F;
   private static final float SPOOKY_TIME_BOOST_MULTIPLIER = 4.2F;
   private static final long SPOOKY_TIME_CHARGE_DURATION_NANOS = 1000000000L;
   private static final long SPOOKY_TIME_MAX_BOOST_DURATION_NANOS = 3500000000L;
   private static final int SPOOKY_TIME_FULL_BOOST_JUMPS = 2;
   private static Speed instance;
   private final ModeSetting mode = this.register(
      new ModeSetting("Mode", "Speed mode.", "Grim", "Vanilla", "Grim", "ReallyWorld", "HolyWorld", "FunTime", "SpookyTime")
   );
   private final NumberSetting speed = this.register(new NumberSetting("Speed", "Vanilla speed.", 1.5, 1.0, 5.0, 0.1));
   private final Speed.BoostCycle reallyWorldCycle = new Speed.BoostCycle(Speed.BoostMode.REALLY_WORLD);
   private final Speed.BoostCycle holyWorldCycle = new Speed.BoostCycle(Speed.BoostMode.HOLY_WORLD);
   private final Speed.BoostCycle funTimeCycle = new Speed.BoostCycle(Speed.BoostMode.FUN_TIME);
   private final Speed.BoostCycle spookyTimeCycle = new Speed.BoostCycle(Speed.BoostMode.SPOOKY_TIME);

   public Speed() {
      super("Speed", "Changes player movement speed.", ModuleCategory.MOVEMENT);
      instance = this;
      this.speed.visibleWhen(() -> this.mode.is("Vanilla"));
   }

   @Override
   protected void onEnable() {
      this.stopAllBoostCycles();
   }

   @Override
   protected void onDisable() {
      this.stopAllBoostCycles();
   }

   @SubscribeEvent
   private void onTick(TickEvent event) {
      if (this.mode.is("Vanilla")) {
         MoveUtil.setVelocity(this.speed.getValue() / 3.0);
      }
   }

   @SubscribeEvent
   private void onMotion(PlayerTravelEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (event.isPre() && client.player != null && client.world != null && MoveUtil.hasPlayerMovement()) {
         if (this.mode.is("Grim")) {
            this.handleGrimMode(client);
         }
      }
   }

   private void handleGrimMode(MinecraftClient client) {
      int collisions = 0;

      for (Entity entity : client.world.getEntities()) {
         if (this.isValidCollisionBoostEntity(client, entity) && client.player.getBoundingBox().expand(0.5).intersects(entity.getBoundingBox())) {
            collisions++;
         }
      }

      double[] motion = MoveUtil.forward(0.07 * (double)collisions);
      client.player.addVelocity(motion[0], 0.0, motion[1]);
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      Speed.BoostCycle activeCycle = this.activeBoostCycle();
      this.stopInactiveBoostCycles(activeCycle);
      if (activeCycle != null) {
         ClientPlayerEntity player = mc.player;
         if (player != null && this.isMoving(player.input.playerInput)) {
            activeCycle.update(System.nanoTime());
         } else {
            activeCycle.stop();
         }
      }
   }

   @SubscribeEvent
   private void onBoostTick(TickEvent.Post event) {
      Speed.BoostCycle activeCycle = this.activeBoostCycle();
      if (activeCycle != null) {
         activeCycle.tick(event.getClient().player);
      }
   }

   @SubscribeEvent
   private void onBoostInput(InputEvent event) {
      Speed.BoostCycle activeCycle = this.activeBoostCycle();
      if (activeCycle != null) {
         activeCycle.applyInput(mc.player, event);
      }
   }

   @SubscribeEvent
   private void onBoostPacket(PacketEvent event) {
      Speed.BoostCycle activeCycle = this.activeBoostCycle();
      if (activeCycle != null) {
         activeCycle.handlePacket(event);
      }
   }

   public static float getReallyWorldMultiplier() {
      Speed speedModule = instance;
      if (speedModule != null && speedModule.isEnabled()) {
         Speed.BoostCycle cycle = speedModule.activeBoostCycle();
         return cycle == null ? 1.0F : cycle.currentMultiplier();
      } else {
         return 1.0F;
      }
   }

   public static boolean hasActiveBoostCycle() {
      Speed speedModule = instance;
      if (speedModule != null && speedModule.isEnabled()) {
         Speed.BoostCycle cycle = speedModule.activeBoostCycle();
         return cycle != null && cycle.active;
      } else {
         return false;
      }
   }

   private Speed.BoostCycle activeBoostCycle() {
      String var1 = this.mode.getValue();

      return switch (var1) {
         case "ReallyWorld" -> this.reallyWorldCycle;
         case "HolyWorld" -> this.holyWorldCycle;
         case "FunTime" -> this.funTimeCycle;
         case "SpookyTime" -> this.spookyTimeCycle;
         default -> null;
      };
   }

   private void stopInactiveBoostCycles(Speed.BoostCycle activeCycle) {
      if (activeCycle != this.reallyWorldCycle) {
         this.reallyWorldCycle.stop();
      }

      if (activeCycle != this.holyWorldCycle) {
         this.holyWorldCycle.stop();
      }

      if (activeCycle != this.funTimeCycle) {
         this.funTimeCycle.stop();
      }

      if (activeCycle != this.spookyTimeCycle) {
         this.spookyTimeCycle.stop();
      }
   }

   private void stopAllBoostCycles() {
      this.reallyWorldCycle.stop();
      this.holyWorldCycle.stop();
      this.funTimeCycle.stop();
      this.spookyTimeCycle.stop();
   }

   private boolean isMoving(PlayerInput input) {
      return input.forward() || input.backward() || input.left() || input.right();
   }

   private float chargeMultiplier(Speed.BoostMode boostMode) {
      return switch (boostMode) {
         case REALLY_WORLD -> 0.2F;
         case HOLY_WORLD -> 0.1F;
         case FUN_TIME -> 0.2F;
         case SPOOKY_TIME -> 0.01F;
      };
   }

   private float boostMultiplier(Speed.BoostMode boostMode) {
      return switch (boostMode) {
         case REALLY_WORLD -> 1.8F;
         case HOLY_WORLD -> 2.6F;
         case FUN_TIME -> 1.8F;
         case SPOOKY_TIME -> 4.2F;
      };
   }

   private long chargeDurationNanos(Speed.BoostMode boostMode) {
      return switch (boostMode) {
         case REALLY_WORLD -> 1250000000L;
         case HOLY_WORLD -> 1250000000L;
         case FUN_TIME -> 1350000000L;
         case SPOOKY_TIME -> 1000000000L;
      };
   }

   private long maxBoostDurationNanos(Speed.BoostMode boostMode) {
      return switch (boostMode) {
         case REALLY_WORLD -> 2400000000L;
         case HOLY_WORLD -> 2400000000L;
         case FUN_TIME -> 2000000000L;
         case SPOOKY_TIME -> 3500000000L;
      };
   }

   private int fullBoostJumps(Speed.BoostMode boostMode) {
      return switch (boostMode) {
         case REALLY_WORLD -> 3;
         case HOLY_WORLD -> 4;
         case FUN_TIME -> 4;
         case SPOOKY_TIME -> 2;
      };
   }

   private boolean isValidCollisionBoostEntity(MinecraftClient client, Entity entity) {
      return entity != null && entity != client.player && !(entity instanceof ArmorStandEntity)
         ? entity instanceof LivingEntity || entity.getType().toString().toLowerCase(Locale.ROOT).contains("boat")
         : false;
   }

   private final class BoostCycle {
      private final Speed.BoostMode boostMode;
      private final Queue<CommonPongC2SPacket> delayedTransactions = new ConcurrentLinkedQueue<>();
      private Speed.BoostPhase phase = Speed.BoostPhase.CHARGING;
      private boolean active;
      private boolean airborne;
      private boolean releasingTransactions;
      private int completedJumps;
      private int groundTicks;
      private long phaseStartedAt;

      private BoostCycle(Speed.BoostMode boostMode) {
         this.boostMode = boostMode;
      }

      private void update(long now) {
         if (!this.active) {
            this.active = true;
            this.beginCharging();
         } else if (this.phase == Speed.BoostPhase.CHARGING && now - this.phaseStartedAt >= Speed.this.chargeDurationNanos(this.boostMode)) {
            this.beginBoosting();
         } else if (this.phase == Speed.BoostPhase.BOOSTING && now - this.phaseStartedAt >= Speed.this.maxBoostDurationNanos(this.boostMode)) {
            this.beginCharging();
         }
      }

      private void tick(ClientPlayerEntity player) {
         if (player != null && this.active && this.phase == Speed.BoostPhase.BOOSTING) {
            if (player.verticalCollision) {
               if (this.airborne) {
                  this.airborne = false;
                  this.completedJumps++;
               }
            } else {
               this.airborne = true;
               if (this.completedJumps >= Speed.this.fullBoostJumps(this.boostMode) && player.getVelocity().y <= 0.0) {
                  this.beginCharging();
               }
            }
         }
      }

      private void applyInput(ClientPlayerEntity player, InputEvent event) {
         PlayerInput input = event.getInput();
         if (player != null && this.active && this.phase == Speed.BoostPhase.BOOSTING && Speed.this.isMoving(input)) {
            this.groundTicks = player.verticalCollision ? this.groundTicks + 1 : 0;
            event.setSprinting(true);
            if (this.groundTicks > 0) {
               event.setJumping(true);
            }
         } else {
            this.groundTicks = 0;
         }
      }

      private void handlePacket(PacketEvent event) {
         if (!this.releasingTransactions) {
            if (event.isSend() && this.active && event.getPacket() instanceof CommonPongC2SPacket packet) {
               this.delayedTransactions.add(packet);
               event.cancel();
            } else if (event.isReceive() && this.active && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
               this.beginCharging();
            }
         }
      }

      private void beginCharging() {
         this.flushTransactions();
         this.phase = Speed.BoostPhase.CHARGING;
         this.airborne = false;
         this.completedJumps = 0;
         this.groundTicks = 0;
         this.phaseStartedAt = System.nanoTime();
      }

      private void beginBoosting() {
         this.phase = Speed.BoostPhase.BOOSTING;
         this.airborne = false;
         this.completedJumps = 0;
         this.groundTicks = 0;
         this.phaseStartedAt = System.nanoTime();
      }

      private void stop() {
         this.flushTransactions();
         this.active = false;
         this.phase = Speed.BoostPhase.CHARGING;
         this.airborne = false;
         this.completedJumps = 0;
         this.groundTicks = 0;
         this.phaseStartedAt = 0L;
      }

      private float currentMultiplier() {
         if (!this.active) {
            return 1.0F;
         } else {
            return this.phase == Speed.BoostPhase.CHARGING ? Speed.this.chargeMultiplier(this.boostMode) : Speed.this.boostMultiplier(this.boostMode);
         }
      }

      private void flushTransactions() {
         CommonPongC2SPacket packet;
         while ((packet = this.delayedTransactions.poll()) != null) {
            if (IMinecraft.mc.getNetworkHandler() != null) {
               this.releasingTransactions = true;

               try {
                  IMinecraft.mc.getNetworkHandler().sendPacket(packet);
               } finally {
                  this.releasingTransactions = false;
               }
            }
         }
      }
   }

   private static enum BoostMode {
      REALLY_WORLD,
      HOLY_WORLD,
      FUN_TIME,
      SPOOKY_TIME;

      private BoostMode() {
      }
   }

   private static enum BoostPhase {
      CHARGING,
      BOOSTING;

      private BoostPhase() {
      }
   }
}
