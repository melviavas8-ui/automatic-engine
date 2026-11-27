package daamky.ua.mixin.accessor;

import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientPacketListener.class})
public interface ClientPacketListenerAccessor {
   @Accessor("level")
   void daamky$setLevel(ClientLevel var1);

   @Accessor("levelData")
   void daamky$setLevelData(ClientLevelData var1);

   @Accessor("levelData")
   ClientLevelData daamky$getLevelData();

   @Accessor("serverChunkRadius")
   void daamky$setServerChunkRadius(int var1);

   @Accessor("serverChunkRadius")
   int daamky$getServerChunkRadius();

   @Accessor("serverSimulationDistance")
   void daamky$setServerSimulationDistance(int var1);

   @Accessor("serverSimulationDistance")
   int daamky$getServerSimulationDistance();

   @Accessor("levels")
   void daamky$setLevels(Set<ResourceKey<Level>> var1);
}
