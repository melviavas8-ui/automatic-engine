package daamky.ua.mixin.accessor;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({PlayerEntityRenderer.class})
public interface AvatarRendererAccessor {
   @Invoker("renderRightArm")
   void daamky$renderRightHand(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, Identifier var4, boolean var5);

   @Invoker("renderLeftArm")
   void daamky$renderLeftHand(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, Identifier var4, boolean var5);
}
