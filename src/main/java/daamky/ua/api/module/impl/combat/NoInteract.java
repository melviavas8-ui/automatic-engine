package daamky.ua.api.module.impl.combat;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;

public class NoInteract extends Module {
   public NoInteract() {
      super("No Interact", "Blocks unwanted interactions.", ModuleCategory.COMBAT);
   }
}
