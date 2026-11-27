package daamky.ua.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.MeshData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import daamky.ua.utils.render.hands.ShaderHandMaterialRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin({RenderType.class})
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
   private void daamky$bindShaderHandUniform(RenderPass renderPass, RenderPipeline pipeline, MeshData meshData) {
      renderPass.setPipeline(pipeline);
      if (ShaderHandMaterialRenderer.isMaterialPipeline(pipeline)) {
         ShaderHandMaterialRenderer.bindUniform(renderPass);
      }
   }
}
