package daamky.ua.mixin.accessor;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({HeldItemRenderer.class})
public interface HeldItemRendererAccessor {
   @Invoker("renderArm")
   void invokeRenderArm(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, Arm var4);

   @Invoker("renderMapInBothHands")
   void invokeRenderMapInBothHands(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, float var5, float var6);

   @Invoker("renderMapInOneHand")
   void invokeRenderMapInOneHand(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, Arm var5, float var6, ItemStack var7);

   @Invoker("applyEquipOffset")
   void invokeApplyEquipOffset(MatrixStack var1, Arm var2, float var3);

   @Invoker("swingArm")
   void invokeSwingArm(float var1, MatrixStack var2, int var3, Arm var4);

   @Invoker("renderItem")
   void invokeRenderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, MatrixStack var4, OrderedRenderCommandQueue var5, int var6);
}
