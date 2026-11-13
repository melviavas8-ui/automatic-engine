package daamky.ua.mixin.accessor;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Key;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({KeyBinding.class})
public interface KeyMappingAccessor {
   @Accessor("boundKey")
   Key daamky$getKey();
}
