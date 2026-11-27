package daamky.ua.mixin.accessor;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Set;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Entity.class})
public interface EntityAccessor {
   @Accessor("horizontalCollision")
   boolean daamky$isHorizontalCollision();

   @Accessor("verticalCollision")
   boolean daamky$isVerticalCollision();

   @Accessor("fluidHeight")
   Object2DoubleMap<TagKey<Fluid>> daamky$getFluidHeight();

   @Accessor("fluidOnEyes")
   Set<TagKey<Fluid>> daamky$getSubmergedFluidTag();
}
