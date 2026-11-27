package daamky.ua.mixin.accessor;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ItemCooldowns.class})
public interface ItemCooldownsAccessor {
   @Accessor("cooldowns")
   Map<Identifier, ?> daamky$getCooldowns();

   @Accessor("tickCount")
   int daamky$getTickCount();
}
