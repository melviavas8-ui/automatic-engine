package daamky.ua.mixin;

import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.access.GuiRenderStateLayerAccessor;

@Mixin({GuiRenderState.class})
public abstract class GuiRenderStateMixin implements GuiRenderStateLayerAccessor {
   @Unique
   private int daamky$layerSerial;

   public GuiRenderStateMixin() {
   }

   @Override
   public int daamky$getLayerSerial() {
      return this.daamky$layerSerial;
   }

   @Inject(
      method = {"createNewRootLayer"},
      at = {@At("RETURN")}
   )
   private void daamky$trackNextStratum(CallbackInfo ci) {
      this.daamky$layerSerial++;
   }

   @Inject(
      method = {"goUpLayer"},
      at = {@At("RETURN")}
   )
   private void daamky$trackUpLayer(CallbackInfo ci) {
      this.daamky$layerSerial++;
   }

   @Inject(
      method = {"clear"},
      at = {@At("HEAD")}
   )
   private void daamky$resetLayerSerial(CallbackInfo ci) {
      this.daamky$layerSerial = 0;
   }
}
