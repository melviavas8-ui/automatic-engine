package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import daamky.ua.api.module.impl.visual.NoRender;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

@Mixin(
   targets = {"net/minecraft/client/gui/Gui$HeartType"}
)
public abstract class GuiHeartTypeMixin {
   public GuiHeartTypeMixin() {
   }

   @WrapOperation(
      method = {"forPlayer"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/core/Holder;)Z"
      )},
      require = 0
   )
   private static boolean daamky$witherHearts(Player player, Holder<MobEffect> effect, Operation<Boolean> original) {
      return effect == MobEffects.WITHER && NoRender.isActive("Wither Hearts") ? false : (Boolean)original.call(new Object[]{player, effect});
   }
}
