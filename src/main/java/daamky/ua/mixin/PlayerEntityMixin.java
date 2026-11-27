package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.PlayerTravelEvent;
import daamky.ua.api.events.impl.SwimmingEvent;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.manager.Manager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Mixin({Player.class})
public abstract class PlayerEntityMixin {
   @Unique
   private boolean daamky$restoreAttackYaw;
   @Unique
   private float daamky$attackYawBefore;

   public PlayerEntityMixin() {
   }

   @Inject(
      method = {"attack"},
      at = {@At("HEAD")},
      require = 0
   )
   private void daamky$attackYawHead(Entity target, CallbackInfo ci) {
      this.daamky$restoreAttackYaw = false;
      if ((Object)this == Minecraft.getInstance().player) {
         AuraModule aura = AuraModule.getInstance();
         if (aura == null || !aura.isEnabled() || !aura.isMatrixMode()) {
            Angle angle = AngleConnection.INSTANCE.getMoveRotation();
            if (angle != null) {
               Player player = (Player)(Object)this;
               this.daamky$attackYawBefore = player.getYRot();
               this.daamky$restoreAttackYaw = true;
               player.setYRot(angle.getYaw());
            }
         }
      }
   }

   @Inject(
      method = {"attack"},
      at = {@At("RETURN")},
      require = 0
   )
   private void daamky$attackYawReturn(Entity target, CallbackInfo ci) {
      if (this.daamky$restoreAttackYaw) {
         ((Player)(Object)this).setYRot(this.daamky$attackYawBefore);
         this.daamky$restoreAttackYaw = false;
      }
   }

   @Inject(
      method = {"travel"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$onTravelPre(Vec3 movementInput, CallbackInfo ci) {
      if ((Object)this == Minecraft.getInstance().player) {
         PlayerTravelEvent event = Manager.postEvent(new PlayerTravelEvent(movementInput, true));
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @ModifyExpressionValue(
      method = {"travel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
      )},
      require = 0
   )
   private Vec3 daamky$travelLookAngle(Vec3 original) {
      if ((Object)this != Minecraft.getInstance().player) {
         return original;
      } else {
         SwimmingEvent event = Manager.postEvent(new SwimmingEvent(original));
         return event.getVector();
      }
   }

   @Inject(
      method = {"travel"},
      at = {@At("RETURN")},
      require = 0
   )
   private void daamky$onTravelPost(Vec3 movementInput, CallbackInfo ci) {
      if ((Object)this == Minecraft.getInstance().player) {
         Manager.postEvent(new PlayerTravelEvent(movementInput, false));
      }
   }
}

