package daamky.ua.mixin.accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MinecraftClient.class})
public interface MinecraftAccessor {
   @Mutable
   @Accessor("session")
   void daamky$setUser(Session var1);

   @Invoker("doAttack")
   boolean daamky$startAttack();

   @Invoker("doItemUse")
   void daamky$startUseItem();

   @Invoker("setWorld")
   void daamky$updateLevelInEngines(ClientWorld var1);

   @Accessor("itemUseCooldown")
   void daamky$setRightClickDelay(int var1);
}
