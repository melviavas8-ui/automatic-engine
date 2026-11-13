package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.events.impl.CloseScreenEvent;
import daamky.ua.api.events.impl.PlayerTravelEvent;
import daamky.ua.api.events.impl.PushEvent;
import daamky.ua.api.events.impl.UsingItemEvent;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.player.AntiDrop;
import daamky.ua.manager.Manager;
import daamky.ua.utils.move.MoveUtil;

@Mixin({ClientPlayerEntity.class})
public abstract class ClientPlayerEntityMixin {
   @Shadow
   private float lastYawClient;
   @Shadow
   private float lastPitchClient;
   @Shadow
   @Final
   public ClientPlayNetworkHandler networkHandler;
   @Shadow
   @Final
   protected MinecraftClient client;
   @Unique
   private double daamky$prevX;
   @Unique
   private double daamky$prevZ;
   @Unique
   private float daamky$prevBodyYaw;

   public ClientPlayerEntityMixin() {
   }

   @Shadow
   public abstract boolean isUsingItem();

   @Inject(
      method = {"pushOutOfBlocks"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$pushOutOfBlocks(double x, double z, CallbackInfo ci) {
      PushEvent event = Manager.postEvent(new PushEvent(PushEvent.Type.BLOCK));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"dropSelectedItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$preventProtectedDrop(boolean dropEntireStack, CallbackInfoReturnable<Boolean> cir) {
      ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
      if (AntiDrop.shouldBlock(player.getMainHandStack())) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/input/Input;tick()V",
         shift = Shift.AFTER
      )},
      require = 0
   )
   private void daamky$onInputTick(CallbackInfo ci) {
      if (this.client.player != null) {
         Manager.postEvent(new PlayerTravelEvent(Vec3d.ZERO, false));
      }
   }

   @Redirect(
      method = {"applyMovementSpeedFactors"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/Vec2f;multiply(F)Lnet/minecraft/util/math/Vec2f;",
         ordinal = 1
      ),
      require = 0
   )
   private Vec2f daamky$cancelItemSlowdown(Vec2f vec, float multiplier) {
      UsingItemEvent event = Manager.postEvent(new UsingItemEvent((byte)0));
      ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
      return event.isCancelled() && this.isUsingItem() && !player.hasVehicle() ? vec.multiply(1.0F) : vec.multiply(multiplier);
   }

   @Inject(
      method = {"closeHandledScreen"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$closeHandledScreenHook(CallbackInfo ci) {
      Screen screen = this.client.currentScreen;
      CloseScreenEvent event = Manager.postEvent(new CloseScreenEvent(screen));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(
      method = {"sendMovementPackets"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"
      )},
      require = 0
   )
   private float daamky$packetYaw(float original) {
      ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
      AngleConnection controller = AngleConnection.INSTANCE;
      if (!controller.shouldApplyPacketRotation()) {
         this.daamky$syncBodyYawCache(player, original);
         return original;
      } else {
         float yaw = controller.getPacketYaw();
         float bodyYaw = MoveUtil.calculateBodyYaw(
            yaw, this.daamky$prevBodyYaw, this.daamky$prevX, this.daamky$prevZ, player.getX(), player.getZ(), player.getHandSwingProgress(1.0F)
         );
         this.daamky$prevBodyYaw = bodyYaw;
         this.daamky$prevX = player.getX();
         this.daamky$prevZ = player.getZ();
         player.setBodyYaw(bodyYaw);
         return yaw;
      }
   }

   @Unique
   private void daamky$syncBodyYawCache(ClientPlayerEntity player, float yaw) {
      this.daamky$prevBodyYaw = yaw;
      this.daamky$prevX = player.getX();
      this.daamky$prevZ = player.getZ();
   }

   @ModifyExpressionValue(
      method = {"sendMovementPackets"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"
      )},
      require = 0
   )
   private float daamky$packetPitch(float original) {
      AngleConnection controller = AngleConnection.INSTANCE;
      return controller.shouldApplyPacketRotation() ? controller.getPacketPitch() : original;
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("TAIL")}
   )
   private void daamky$ensureSilentRotationPacket(CallbackInfo ci) {
      AngleConnection controller = AngleConnection.INSTANCE;
      AuraModule aura = AuraModule.getInstance();
      boolean queuedAuraAttack = aura != null && aura.hasQueuedAttack();
      if (!controller.shouldApplyPacketRotation()) {
         if (queuedAuraAttack) {
            aura.flushQueuedAttack();
         }
      } else {
         ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
         float yaw = controller.getPacketYaw();
         float pitch = controller.getPacketPitch();
         boolean rotationChanged = Math.abs(yaw - this.lastYawClient) > 0.001F || Math.abs(pitch - this.lastPitchClient) > 0.001F;
         if (rotationChanged) {
            this.networkHandler.sendPacket(new LookAndOnGround(yaw, pitch, player.isOnGround(), player.horizontalCollision));
            this.lastYawClient = yaw;
            this.lastPitchClient = pitch;
         }

         if (queuedAuraAttack) {
            aura.flushQueuedAttack();
         }
      }
   }
}

