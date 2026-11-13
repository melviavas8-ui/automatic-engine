package daamky.ua.mixin.accessor;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
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

   @Accessor("submergedFluidTag")
   Set<TagKey<Fluid>> daamky$getSubmergedFluidTag();
}
