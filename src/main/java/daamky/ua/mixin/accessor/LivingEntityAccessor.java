package daamky.ua.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LivingEntity.class})
public interface LivingEntityAccessor {
   @Accessor("jumping")
   boolean daamky$isJumping();

   @Accessor("noJumpDelay")
   int daamky$getJumpingCooldown();

   @Accessor("noJumpDelay")
   void daamky$setJumpingCooldown(int var1);
}
