package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.events.impl.TextFactoryEvent;
import daamky.ua.manager.Manager;
import net.minecraft.util.StringDecomposer;

@Mixin({StringDecomposer.class})
public class TextVisitFactoryMixin {
   public TextVisitFactoryMixin() {
   }

   @ModifyArg(
      method = {"iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
         ordinal = 0
      ),
      index = 0,
      require = 0
   )
   private static String daamky$adjustText(String text) {
      TextFactoryEvent event = Manager.postEvent(new TextFactoryEvent(ClientLanguage.tr(text)));
      return event.getText();
   }
}
