package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@Mixin({LocalPlayer.class})
public abstract class ClientPlayerEntityMixin {
   @Shadow
   private float yRotLast;
   @Shadow
   private float xRotLast;
   @Shadow
   @Final
   public ClientPacketListener connection;
   @Shadow
   @Final
   protected Minecraft minecraft;
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
      method = {"moveTowardsClosestSpace"},
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
      method = {"drop(Z)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$preventProtectedDrop(boolean dropEntireStack, CallbackInfoReturnable<Boolean> cir) {
      LocalPlayer player = (LocalPlayer)(Object)this;
      if (AntiDrop.shouldBlock(player.getMainHandItem())) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"aiStep"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/ClientInput;tick()V",
         shift = Shift.AFTER
      )},
      require = 0
   )
   private void daamky$onInputTick(CallbackInfo ci) {
      if (this.minecraft.player != null) {
         Manager.postEvent(new PlayerTravelEvent(Vec3.ZERO, false));
      }
   }

   @Redirect(
      method = {"modifyInput"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec2;scale(F)Lnet/minecraft/world/phys/Vec2;",
         ordinal = 1
      ),
      require = 0
   )
   private Vec2 daamky$cancelItemSlowdown(Vec2 vec, float multiplier) {
      UsingItemEvent event = Manager.postEvent(new UsingItemEvent((byte)0));
      LocalPlayer player = (LocalPlayer)(Object)this;
      return event.isCancelled() && this.isUsingItem() && !player.isPassenger() ? vec.scale(1.0F) : vec.scale(multiplier);
   }

   @Inject(
      method = {"closeContainer"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$closeHandledScreenHook(CallbackInfo ci) {
      Screen screen = this.minecraft.screen;
      CloseScreenEvent event = Manager.postEvent(new CloseScreenEvent(screen));
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(
      method = {"sendPosition"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"
      )},
      require = 0
   )
   private float daamky$packetYaw(float original) {
      LocalPlayer player = (LocalPlayer)(Object)this;
      AngleConnection controller = AngleConnection.INSTANCE;
      if (!controller.shouldApplyPacketRotation()) {
         this.daamky$syncBodyYawCache(player, original);
         return original;
      } else {
         float yaw = controller.getPacketYaw();
         float bodyYaw = MoveUtil.calculateBodyYaw(
            yaw, this.daamky$prevBodyYaw, this.daamky$prevX, this.daamky$prevZ, player.getX(), player.getZ(), player.getAttackAnim(1.0F)
         );
         this.daamky$prevBodyYaw = bodyYaw;
         this.daamky$prevX = player.getX();
         this.daamky$prevZ = player.getZ();
         player.setYBodyRot(bodyYaw);
         return yaw;
      }
   }

   @Unique
   private void daamky$syncBodyYawCache(LocalPlayer player, float yaw) {
      this.daamky$prevBodyYaw = yaw;
      this.daamky$prevX = player.getX();
      this.daamky$prevZ = player.getZ();
   }

   @ModifyExpressionValue(
      method = {"sendPosition"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"
      )},
      require = 0
   )
   private float daamky$packetPitch(float original) {
      AngleConnection controller = AngleConnection.INSTANCE;
      return controller.shouldApplyPacketRotation() ? controller.getPacketPitch() : original;
   }

   @Inject(
      method = {"sendPosition"},
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
         LocalPlayer player = (LocalPlayer)(Object)this;
         float yaw = controller.getPacketYaw();
         float pitch = controller.getPacketPitch();
         boolean rotationChanged = Math.abs(yaw - this.yRotLast) > 0.001F || Math.abs(pitch - this.xRotLast) > 0.001F;
         if (rotationChanged) {
            this.connection.send(new Rot(yaw, pitch, player.onGround(), player.horizontalCollision));
            this.yRotLast = yaw;
            this.xRotLast = pitch;
         }

         if (queuedAuraAttack) {
            aura.flushQueuedAttack();
         }
      }
   }
}

