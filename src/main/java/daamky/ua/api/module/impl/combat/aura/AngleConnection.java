package daamky.ua.api.module.impl.combat.aura;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.PlayerVelocityStrafeEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.types.EventPhase;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.impl.combat.aura.back.BackAngle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.util.TaskPriority;
import daamky.ua.api.module.impl.combat.aura.util.TaskProcessor;
import daamky.ua.manager.Manager;
import daamky.ua.utils.player.BaritoneMovementHelper;

public class AngleConnection {
   public static final AngleConnection INSTANCE = new AngleConnection();
   private static final RotateConstructor USE_ITEM_RETURN_SMOOTH = new BackAngle(0.95F);
   private final TaskProcessor<AngleConstructor> rotationPlanTaskProcessor = new TaskProcessor<>();
   private AngleConstructor lastRotationPlan;
   private Angle currentAngle;
   private Angle previousAngle;
   private Angle serverAngle = Angle.DEFAULT;
   private Angle fakeAngle;
   private Angle previousFakeAngle;
   private Float fakeBodyYaw;
   private Float previousFakeBodyYaw;
   private boolean returning;
   private int forcePacketRotationTicks;
   private boolean useItemReturnPending;
   private int staleInactiveTicks;
   private boolean rotationRequestedThisTick;
   private boolean queuedRotationTaskThisTick;

   public AngleConnection() {
   }

   public void setRotation(Angle value) {
      if (value == null) {
         this.previousAngle = this.currentAngle != null ? this.currentAngle : MathAngle.cameraAngle();
      } else {
         this.previousAngle = this.currentAngle;
      }

      this.currentAngle = value;
   }

   public Angle getCurrentAngle() {
      return this.currentAngle;
   }

   public Angle getRotation() {
      return this.currentAngle != null ? this.currentAngle : MathAngle.cameraAngle();
   }

   public Angle getServerAngle() {
      return this.serverAngle;
   }

   public float getPacketYaw() {
      return this.getPacketRotation().getYaw();
   }

   public float getPacketPitch() {
      return MathHelper.clamp(this.getPacketRotation().getPitch(), -90.0F, 90.0F);
   }

   public boolean shouldApplyPacketRotation() {
      return BaritoneMovementHelper.isBaritoneActive(MinecraftClient.getInstance().player)
         ? false
         : this.currentAngle != null || this.fakeAngle != null || this.forcePacketRotationTicks > 0 && this.previousAngle != null;
   }

   public void forcePacketRotation(int ticks) {
      if (ticks > 0) {
         this.forcePacketRotationTicks = Math.max(this.forcePacketRotationTicks, ticks);
      }
   }

   public Angle getFakeRotation() {
      if (this.fakeAngle != null) {
         return this.fakeAngle;
      } else {
         return this.currentAngle != null ? this.currentAngle : (this.previousAngle != null ? this.previousAngle : MathAngle.cameraAngle());
      }
   }

   public Angle getFakeAngle() {
      return this.fakeAngle;
   }

   public Angle getPreviousFakeRotation() {
      return this.previousFakeAngle != null ? this.previousFakeAngle : this.getFakeRotation();
   }

   public float getFakeBodyYaw() {
      return this.fakeBodyYaw != null ? this.fakeBodyYaw : this.getFakeRotation().getYaw();
   }

   public float getPreviousFakeBodyYaw() {
      return this.previousFakeBodyYaw != null ? this.previousFakeBodyYaw : this.getFakeBodyYaw();
   }

   public void setFakeRotation(Angle angle) {
      angle = this.normalizeAngle(angle, this.fakeAngle);
      if (angle == null) {
         this.previousFakeAngle = null;
         this.fakeAngle = null;
         this.previousFakeBodyYaw = null;
         this.fakeBodyYaw = null;
      } else {
         this.previousFakeAngle = this.fakeAngle != null ? this.fakeAngle : angle;
         this.fakeAngle = angle;
         float targetYaw = angle.getYaw();
         if (this.fakeBodyYaw == null) {
            this.fakeBodyYaw = targetYaw;
            this.previousFakeBodyYaw = targetYaw;
         } else {
            this.previousFakeBodyYaw = this.fakeBodyYaw;
            float diff = MathHelper.wrapDegrees(targetYaw - this.fakeBodyYaw);
            this.fakeBodyYaw = this.fakeBodyYaw + MathHelper.clamp(diff, -8.0F, 8.0F);
         }
      }
   }

   public Angle getPreviousRotation() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null) {
         return this.currentAngle != null ? this.currentAngle : Angle.DEFAULT;
      } else {
         return this.currentAngle != null && this.previousAngle != null ? this.previousAngle : new Angle(client.player.lastYaw, client.player.lastPitch);
      }
   }

   public Angle getMoveRotation() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (BaritoneMovementHelper.isBaritoneActive(client.player)) {
         return MathAngle.cameraAngle();
      } else {
         AngleConstructor rotationPlan = this.getCurrentRotationPlan();
         return this.currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection() ? this.currentAngle : MathAngle.cameraAngle();
      }
   }

   public boolean isMoveCorrectionActive() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (BaritoneMovementHelper.isBaritoneActive(client.player)) {
         return false;
      } else {
         AngleConstructor rotationPlan = this.getCurrentRotationPlan();
         return this.currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection();
      }
   }

   public boolean isFreeCorrectionActive() {
      AngleConstructor rotationPlan = this.getCurrentRotationPlan();
      return this.isMoveCorrectionActive() && rotationPlan != null && rotationPlan.isFreeCorrection();
   }

   public boolean isReturning() {
      return this.returning;
   }

   public boolean isUseItemReturnPending() {
      return this.useItemReturnPending;
   }

   public AngleConstructor getCurrentRotationPlan() {
      AngleConstructor activePlan = this.rotationPlanTaskProcessor.fetchActiveTaskValue();
      return activePlan != null ? activePlan : this.lastRotationPlan;
   }

   public void rotateTo(Angle.VecRotation vecRotation, LivingEntity entity, int reset, AngleConfig configurable, TaskPriority taskPriority, Module provider) {
      this.rotateTo(configurable.createRotationPlan(vecRotation.getAngle(), vecRotation.getVec(), entity, reset), taskPriority, provider);
   }

   public void rotateTo(Angle angle, int reset, AngleConfig configurable, TaskPriority taskPriority, Module provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, reset), taskPriority, provider);
   }

   public void rotateTo(Angle angle, AngleConfig configurable, TaskPriority taskPriority, Module provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, 1), taskPriority, provider);
   }

   public void rotateTo(AngleConstructor plan, TaskPriority taskPriority, Module provider) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (!BaritoneMovementHelper.isBaritoneActive(client.player)) {
         this.returning = false;
         this.useItemReturnPending = false;
         this.rotationRequestedThisTick = true;
         this.rotationPlanTaskProcessor.addTask(new TaskProcessor.Task<>(1, taskPriority.getPriority(), provider, plan));
      }
   }

   public void update(MinecraftClient client) {
      this.tick(client);
   }

   public void tick(MinecraftClient client) {
      if (client == null || client.player == null || client.world == null) {
         this.reset();
      } else if (BaritoneMovementHelper.isBaritoneActive(client.player)) {
         if (this.hasAnyRotationState()) {
            this.reset();
         }
      } else {
         if (this.forcePacketRotationTicks > 0) {
            this.forcePacketRotationTicks--;
         }

         this.rotationRequestedThisTick = false;
         this.queuedRotationTaskThisTick = false;
         Manager.postEvent(new RotationUpdateEvent(EventPhase.PRE));
         this.queuedRotationTaskThisTick = this.rotationPlanTaskProcessor.fetchActiveTaskValue() != null;
         this.advanceRotation(client);
         Manager.postEvent(new RotationUpdateEvent(EventPhase.POST));
         if (this.useItemReturnPending && !this.shouldDelayUseAction()) {
            this.useItemReturnPending = false;
         }

         if (!this.hasActiveRotationOwner() && this.hasAnyRotationState()) {
            this.staleInactiveTicks++;
            if (this.staleInactiveTicks >= 6) {
               this.hardResetStaleRotationState();
               return;
            }
         } else {
            this.staleInactiveTicks = 0;
         }
      }
   }

   private void advanceRotation(MinecraftClient client) {
      AngleConstructor activePlan = this.getCurrentRotationPlan();
      if (activePlan != null) {
         Angle clientAngle = new Angle(client.player.getYaw(), client.player.getPitch());
         if (this.returning && this.currentAngle != null && computeRotationDifference(this.currentAngle, clientAngle) < 1.0) {
            this.restoreVanillaLook();
         } else {
            if (this.lastRotationPlan != null) {
               double differenceFromCurrentToPlayer = computeRotationDifference(this.serverAngle, clientAngle);
               if (activePlan.getTicksUntilReset() <= this.rotationPlanTaskProcessor.tickCounter() && differenceFromCurrentToPlayer < 1.0) {
                  if (activePlan.isChangeLook()) {
                     client.player.setYaw(this.getRotation().getYaw());
                     client.player.setPitch(this.getRotation().getPitch());
                  }

                  this.setRotation(null);
                  this.setFakeRotation(null);
                  this.lastRotationPlan = null;
                  this.rotationPlanTaskProcessor.clear();
                  this.returning = false;
                  this.useItemReturnPending = false;
                  return;
               }
            }

            Angle baseAngle = this.currentAngle != null ? this.currentAngle : clientAngle;
            Angle newAngle = activePlan.nextRotation(baseAngle, this.rotationPlanTaskProcessor.fetchActiveTaskValue() == null).adjustSensitivity();
            this.setRotation(newAngle);
            this.lastRotationPlan = activePlan;
            this.rotationPlanTaskProcessor.tick(1);
         }
      }
   }

   public void applyPlayerRotation(MinecraftClient client) {
      if (client != null && client.player != null && this.currentAngle != null) {
         client.player.setYaw(this.currentAngle.getYaw());
         client.player.setPitch(this.currentAngle.getPitch());
         client.player.setHeadYaw(this.currentAngle.getYaw());
         client.player.setBodyYaw(this.currentAngle.getYaw());
      }
   }

   public void clear() {
      this.rotationPlanTaskProcessor.clear();
      this.currentAngle = null;
      this.previousAngle = null;
      this.lastRotationPlan = null;
      this.useItemReturnPending = false;
   }

   public void startReturning() {
      this.startReturning(null);
   }

   public void startReturningForUse() {
      this.useItemReturnPending = true;
      this.startReturning(USE_ITEM_RETURN_SMOOTH);
   }

   public boolean requestUseRotationRestore() {
      if (!this.shouldDelayUseAction()) {
         this.useItemReturnPending = false;
         return false;
      } else {
         if (!this.useItemReturnPending) {
            this.startReturningForUse();
         }

         return true;
      }
   }

   public boolean shouldDelayUseAction() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null) {
         return false;
      } else {
         Angle cameraAngle = MathAngle.cameraAngle();
         return this.returning
            || this.currentAngle != null && computeRotationDifference(this.currentAngle, cameraAngle) > 0.35
            || this.fakeAngle != null && computeRotationDifference(this.fakeAngle, cameraAngle) > 0.35
            || this.forcePacketRotationTicks > 0 && this.previousAngle != null && computeRotationDifference(this.previousAngle, cameraAngle) > 0.35;
      }
   }

   public void reset() {
      this.clear();
      this.fakeAngle = null;
      this.previousFakeAngle = null;
      this.fakeBodyYaw = null;
      this.previousFakeBodyYaw = null;
      this.serverAngle = Angle.DEFAULT;
      this.returning = false;
      this.forcePacketRotationTicks = 0;
      this.staleInactiveTicks = 0;
      this.rotationRequestedThisTick = false;
      this.queuedRotationTaskThisTick = false;
   }

   public void restoreVanillaLook() {
      this.reset();
      this.syncVanillaHeadAndBody();
   }

   public void syncAfterFirstPersonRestore() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null) {
         this.reset();
      } else {
         Angle playerAngle = MathAngle.cameraAngle();
         Angle syncedAngle = this.normalizeAngle(playerAngle, this.currentAngle != null ? this.currentAngle : playerAngle);
         this.currentAngle = syncedAngle;
         this.previousAngle = syncedAngle;
         this.fakeAngle = null;
         this.previousFakeAngle = null;
         this.fakeBodyYaw = null;
         this.previousFakeBodyYaw = null;
         this.returning = false;
         this.forcePacketRotationTicks = 0;
         this.useItemReturnPending = false;
      }
   }

   public void onMovePacket(PlayerMoveC2SPacket packet) {
      if (packet != null && packet.changesLook()) {
         this.serverAngle = new Angle(packet.getYaw(1.0F), packet.getPitch(1.0F));
      }
   }

   public void onPlayerPosition(PlayerPositionLookS2CPacket packet) {
      if (packet != null) {
         this.serverAngle = new Angle(packet.change().yaw(), packet.change().pitch());
      }
   }

   public static double computeRotationDifference(Angle a, Angle b) {
      return Math.hypot((double)Math.abs(computeAngleDifference(a.getYaw(), b.getYaw())), (double)Math.abs(a.getPitch() - b.getPitch()));
   }

   public static float computeAngleDifference(float a, float b) {
      return MathHelper.wrapDegrees(a - b);
   }

   private Vec3d fixVelocity(Vec3d currentVelocity, Vec3d movementInput, float speed) {
      if (this.currentAngle == null) {
         return currentVelocity;
      } else {
         double length = movementInput.lengthSquared();
         if (length < 1.0E-7) {
            return Vec3d.ZERO;
         } else {
            Vec3d scaled = (length > 1.0 ? movementInput.normalize() : movementInput).multiply((double)speed);
            float sin = MathHelper.sin((double)(this.currentAngle.getYaw() * (float) (Math.PI / 180.0)));
            float cos = MathHelper.cos((double)(this.currentAngle.getYaw() * (float) (Math.PI / 180.0)));
            return new Vec3d(scaled.x * (double)cos - scaled.z * (double)sin, scaled.y, scaled.z * (double)cos + scaled.x * (double)sin);
         }
      }
   }

   private Angle getPacketRotation() {
      if (this.currentAngle != null) {
         return this.currentAngle;
      } else if (this.fakeAngle != null) {
         return this.fakeAngle;
      } else {
         return this.forcePacketRotationTicks > 0 && this.previousAngle != null ? this.previousAngle : MathAngle.cameraAngle();
      }
   }

   private Angle normalizeAngle(Angle angle, Angle reference) {
      if (angle == null) {
         return null;
      } else {
         float yaw = angle.getYaw();
         if (reference != null) {
            yaw = reference.getYaw() + MathHelper.wrapDegrees(yaw - reference.getYaw());
         } else {
            yaw = MathHelper.wrapDegrees(yaw);
         }

         return new Angle(yaw, MathHelper.clamp(angle.getPitch(), -90.0F, 90.0F));
      }
   }

   public void startReturning(RotateConstructor returnSmooth) {
      AngleConstructor sourcePlan = this.rotationPlanTaskProcessor.fetchActiveTaskValue();
      AngleConstructor returnPlan = sourcePlan != null ? sourcePlan : this.lastRotationPlan;
      if (returnPlan != null && returnSmooth != null) {
         returnPlan = this.cloneRotationPlan(returnPlan, returnSmooth);
      } else if (returnPlan == null && returnSmooth != null && this.currentAngle != null) {
         returnPlan = new AngleConstructor(this.currentAngle, this.currentAngle.toVector(), null, returnSmooth, 1, 1.0F, false, false);
      }

      this.lastRotationPlan = returnPlan;
      this.rotationPlanTaskProcessor.clear();
      this.returning = this.currentAngle != null && this.lastRotationPlan != null;
      if (!this.returning && !this.shouldDelayUseAction()) {
         this.useItemReturnPending = false;
      }
   }

   private AngleConstructor cloneRotationPlan(AngleConstructor sourcePlan, RotateConstructor returnSmooth) {
      return new AngleConstructor(
         sourcePlan.getAngle(),
         sourcePlan.getVec3d(),
         sourcePlan.getEntity(),
         returnSmooth,
         sourcePlan.getTicksUntilReset(),
         sourcePlan.getResetThreshold(),
         sourcePlan.isMoveCorrection(),
         sourcePlan.isFreeCorrection()
      );
   }

   private boolean hasAnyRotationState() {
      return this.currentAngle != null
         || this.fakeAngle != null
         || this.previousAngle != null
         || this.previousFakeAngle != null
         || this.lastRotationPlan != null
         || this.returning
         || this.useItemReturnPending;
   }

   private boolean hasActiveRotationOwner() {
      return this.rotationRequestedThisTick
         || this.queuedRotationTaskThisTick
         || this.returning
         || this.useItemReturnPending
         || this.forcePacketRotationTicks > 0;
   }

   private void hardResetStaleRotationState() {
      this.restoreVanillaLook();
   }

   private void syncVanillaHeadAndBody() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         float yaw = client.player.getYaw();
         client.player.setHeadYaw(yaw);
         client.player.setBodyYaw(yaw);
      }
   }

   @SubscribeEvent
   private void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (!BaritoneMovementHelper.isBaritoneActive(client.player)) {
         if (this.isMoveCorrectionActive()) {
            event.setVelocity(this.fixVelocity(event.getVelocity(), event.getMovementInput(), event.getSpeed()));
         }
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent.Pre event) {
      this.tick(event.getClient());
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (!event.isCancelled()) {
         if (event.isSend()) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket movePacket) {
               this.onMovePacket(movePacket);
            }
         } else {
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket positionPacket) {
               this.onPlayerPosition(positionPacket);
            }
         }
      }
   }
}
