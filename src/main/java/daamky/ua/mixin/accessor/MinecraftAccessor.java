package daamky.ua.mixin.accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Minecraft.class})
public interface MinecraftAccessor {
   @Mutable
   @Accessor("user")
   void daamky$setUser(User var1);

   @Invoker("startAttack")
   boolean daamky$startAttack();

   @Invoker("startUseItem")
   void daamky$startUseItem();

   @Invoker("updateLevelInEngines")
   void daamky$updateLevelInEngines(ClientLevel var1);

   @Accessor("rightClickDelay")
   void daamky$setRightClickDelay(int var1);
}
