package daamky.ua.mixin;

import java.util.List;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.WorldOrderStore;

@Mixin({WorldListWidget.class})
public abstract class WorldSelectionListThemeMixin {
   public WorldSelectionListThemeMixin() {
   }

   @Inject(
      method = {"showSummaries"},
      at = {@At("TAIL")}
   )
   private void daamky$restoreWorldOrder(String filter, List<?> levels, CallbackInfo ci) {
      WorldOrderStore.apply((WorldListWidget)(Object)this);
   }
}

