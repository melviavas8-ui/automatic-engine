package daamky.ua.mixin.accessor;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({AbstractSelectionList.class})
public interface AbstractSelectionListAccessor {
   @Invoker("swap")
   void daamky$swapEntries(int var1, int var2);
}
