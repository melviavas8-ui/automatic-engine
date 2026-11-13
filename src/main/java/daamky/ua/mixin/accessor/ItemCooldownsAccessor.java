package daamky.ua.mixin.accessor;

import java.util.Map;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ItemCooldownManager.class})
public interface ItemCooldownsAccessor {
   @Accessor("entries")
   Map<Identifier, ?> daamky$getCooldowns();

   @Accessor("tick")
   int daamky$getTickCount();
}
