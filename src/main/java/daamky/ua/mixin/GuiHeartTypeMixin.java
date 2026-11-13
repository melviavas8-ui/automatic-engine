package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import daamky.ua.api.module.impl.visual.NoRender;

@Mixin(
   targets = {"net/minecraft/client/gui/hud/InGameHud$HeartType"}
)
public abstract class GuiHeartTypeMixin {
   public GuiHeartTypeMixin() {
   }

   @WrapOperation(
      method = {"fromPlayerState"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"
      )},
      require = 0
   )
   private static boolean daamky$witherHearts(PlayerEntity player, RegistryEntry<StatusEffect> effect, Operation<Boolean> original) {
      return effect == StatusEffects.WITHER && NoRender.isActive("Wither Hearts") ? false : (Boolean)original.call(new Object[]{player, effect});
   }
}
