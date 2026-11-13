package daamky.ua.mixin.accessor;

import java.util.Set;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.ClientWorld.Properties;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientPlayNetworkHandler.class})
public interface ClientPacketListenerAccessor {
   @Accessor("world")
   void daamky$setLevel(ClientWorld var1);

   @Accessor("worldProperties")
   void daamky$setLevelData(Properties var1);

   @Accessor("worldProperties")
   Properties daamky$getLevelData();

   @Accessor("chunkLoadDistance")
   void daamky$setServerChunkRadius(int var1);

   @Accessor("chunkLoadDistance")
   int daamky$getServerChunkRadius();

   @Accessor("simulationDistance")
   void daamky$setServerSimulationDistance(int var1);

   @Accessor("simulationDistance")
   int daamky$getServerSimulationDistance();

   @Accessor("worldKeys")
   void daamky$setLevels(Set<RegistryKey<World>> var1);
}
