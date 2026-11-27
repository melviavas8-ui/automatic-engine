package daamky.ua.mixin.accessor;

import java.util.List;
import net.minecraft.network.codec.IdDispatchCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({IdDispatchCodec.class})
public interface IdDispatchCodecAccessor {
   @Accessor("byId")
   List<?> daamky$getById();
}
