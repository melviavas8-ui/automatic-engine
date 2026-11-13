package daamky.ua.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import daamky.ua.utils.render.hands.ShaderHandMaterialRenderer;

@Mixin({RenderLayer.class})
public abstract class RenderTypeMaterialMixin {
   public RenderTypeMaterialMixin() {
   }

   @Redirect(
      method = {"draw"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"
      ),
      require = 0
   )
   private void daamky$bindShaderHandUniform(RenderPass renderPass, RenderPipeline pipeline, BuiltBuffer meshData) {
      renderPass.setPipeline(pipeline);
      if (ShaderHandMaterialRenderer.isMaterialPipeline(pipeline)) {
         ShaderHandMaterialRenderer.bindUniform(renderPass);
      }
   }
}
