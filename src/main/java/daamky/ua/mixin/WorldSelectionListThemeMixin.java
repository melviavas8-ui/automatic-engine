package daamky.ua.mixin;

import java.util.List;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.screens.mainmenu.WorldOrderStore;

@Mixin({WorldSelectionList.class})
public abstract class WorldSelectionListThemeMixin {
   public WorldSelectionListThemeMixin() {
   }

   @Inject(
      method = {"fillLevels"},
      at = {@At("TAIL")}
   )
   private void daamky$restoreWorldOrder(String filter, List<?> levels, CallbackInfo ci) {
      WorldOrderStore.apply((WorldSelectionList)(Object)this);
   }
}

