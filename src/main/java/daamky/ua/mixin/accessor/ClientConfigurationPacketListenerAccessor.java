package daamky.ua.mixin.accessor;

import net.minecraft.client.gui.components.ChatComponent.State;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.client.multiplayer.RegistryDataCollector;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientConfigurationPacketListenerImpl.class})
public interface ClientConfigurationPacketListenerAccessor {
   @Accessor("registryDataCollector")
   RegistryDataCollector daamky$getRegistryDataCollector();

   @Accessor("receivedRegistries")
   Frozen daamky$getReceivedRegistries();

   @Accessor("enabledFeatures")
   FeatureFlagSet daamky$getEnabledFeatures();

   @Accessor("knownPacks")
   KnownPacksManager daamky$getKnownPacks();

   @Accessor("chatState")
   State daamky$getChatState();
}
