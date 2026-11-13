package daamky.ua.mixin.accessor;

import net.minecraft.client.gui.hud.ChatHud.ChatState;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
import net.minecraft.client.network.ClientRegistries;
import net.minecraft.client.resource.ClientDataPackManager;
import net.minecraft.registry.DynamicRegistryManager.Immutable;
import net.minecraft.resource.featuretoggle.FeatureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientConfigurationNetworkHandler.class})
public interface ClientConfigurationPacketListenerAccessor {
   @Accessor("clientRegistries")
   ClientRegistries daamky$getRegistryDataCollector();

   @Accessor("registryManager")
   Immutable daamky$getReceivedRegistries();

   @Accessor("enabledFeatures")
   FeatureSet daamky$getEnabledFeatures();

   @Accessor("dataPackManager")
   ClientDataPackManager daamky$getKnownPacks();

   @Accessor("chatState")
   ChatState daamky$getChatState();
}
