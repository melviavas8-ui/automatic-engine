package daamky.ua.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LivingEntity.class})
public interface LivingEntityAccessor {
   @Accessor("jumping")
   boolean daamky$isJumping();

   @Accessor("jumpingCooldown")
   int daamky$getJumpingCooldown();

   @Accessor("jumpingCooldown")
   void daamky$setJumpingCooldown(int var1);
}
