package daamky.ua.api.module.impl.visual;

import net.minecraft.client.MinecraftClient;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.hands.ShaderHandMaterialRenderer;

public final class ShaderHand extends Module {
   private final ModeSetting shader = this.register(
      new ModeSetting(
         "Shader",
         "First-person hand and held item shader.",
         "Cosmic Sweep",
         "Cosmic Sweep",
         "Plasma Veins",
         "Molten Glass",
         "Violet Aura",
         "Frost Shimmer",
         "Arcane Glint",
         "Emerald Enchant",
         "Golden Runes",
         "Azure Chaos"
      )
   );
   private final NumberSetting intensity = this.register(new NumberSetting("Intensity", "Shader color intensity.", 1.0, 0.25, 1.5, 0.05));
   private final NumberSetting speed = this.register(new NumberSetting("Speed", "Shader animation speed.", 1.0, 0.1, 2.5, 0.05));

   public ShaderHand() {
      super("Shader Hand", "Shaders the first-person hands and items in both hands.", ModuleCategory.VISUAL);
   }

   @Override
   protected void onEnable() {
      this.syncRenderer();
      ShaderHandMaterialRenderer.setEnabled(true);
   }

   @Override
   protected void onDisable() {
      ShaderHandMaterialRenderer.setEnabled(false);
   }

   @Override
   public void onTick(MinecraftClient client) {
      this.syncRenderer();
   }

   private void syncRenderer() {
      int mode = this.shader.is("Plasma Veins")
         ? 1
         : (
            this.shader.is("Molten Glass")
               ? 2
               : (
                  this.shader.is("Violet Aura")
                     ? 3
                     : (
                        this.shader.is("Frost Shimmer")
                           ? 4
                           : (
                              this.shader.is("Arcane Glint")
                                 ? 5
                                 : (this.shader.is("Emerald Enchant") ? 6 : (this.shader.is("Golden Runes") ? 7 : (this.shader.is("Azure Chaos") ? 8 : 0)))
                           )
                     )
               )
         );
      ShaderHandMaterialRenderer.configure(mode, this.intensity.getFloat(), this.speed.getFloat());
   }
}
