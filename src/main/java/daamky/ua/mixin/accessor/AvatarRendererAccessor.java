package daamky.ua.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({AvatarRenderer.class})
public interface AvatarRendererAccessor {
   @Invoker("renderRightHand")
   void daamky$renderRightHand(PoseStack var1, SubmitNodeCollector var2, int var3, Identifier var4, boolean var5);

   @Invoker("renderLeftHand")
   void daamky$renderLeftHand(PoseStack var1, SubmitNodeCollector var2, int var3, Identifier var4, boolean var5);
}
