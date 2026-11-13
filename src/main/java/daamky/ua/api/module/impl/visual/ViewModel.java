package daamky.ua.api.module.impl.visual;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Hand;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.HandOffsetEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.NumberSetting;

public class ViewModel extends Module {
   private final NumberSetting mainHandX = this.register(new NumberSetting("Main Hand X", "Main hand X offset.", 0.0, -1.0, 1.0, 0.05));
   private final NumberSetting mainHandY = this.register(new NumberSetting("Main Hand Y", "Main hand Y offset.", 0.0, -1.0, 1.0, 0.05));
   private final NumberSetting mainHandZ = this.register(new NumberSetting("Main Hand Z", "Main hand Z offset.", 0.0, -2.5, 2.5, 0.05));
   private final NumberSetting offHandX = this.register(new NumberSetting("Off Hand X", "Off hand X offset.", 0.0, -1.0, 1.0, 0.05));
   private final NumberSetting offHandY = this.register(new NumberSetting("Off Hand Y", "Off hand Y offset.", 0.0, -1.0, 1.0, 0.05));
   private final NumberSetting offHandZ = this.register(new NumberSetting("Off Hand Z", "Off hand Z offset.", 0.0, -2.5, 2.5, 0.05));

   public ViewModel() {
      super("View Model", "Changes first-person hand and item offsets.", ModuleCategory.VISUAL);
   }

   @SubscribeEvent
   private void onHandOffset(HandOffsetEvent event) {
      if (event.getHand() != Hand.MAIN_HAND || !(event.getStack().getItem() instanceof CrossbowItem)) {
         MatrixStack matrices = event.getMatrices();
         if (event.getHand() == Hand.MAIN_HAND) {
            matrices.translate(this.mainHandX.getValue(), this.mainHandY.getValue(), this.mainHandZ.getValue());
         } else {
            matrices.translate(this.offHandX.getValue(), this.offHandY.getValue(), this.offHandZ.getValue());
         }
      }
   }
}
