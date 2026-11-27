package daamky.ua.mixin.accessor;

import net.minecraft.core.Holder.Reference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Reference.class})
public interface HolderReferenceAccessor<T> {
   @Invoker("bindValue")
   void daamky$bindValue(T var1);
}
