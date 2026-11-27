package daamky.ua.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ItemInHandRenderer.class})
public interface HeldItemRendererAccessor {
   @Invoker("renderMapHand")
   void invokeRenderArm(PoseStack var1, SubmitNodeCollector var2, int var3, HumanoidArm var4);

   @Invoker("renderTwoHandedMap")
   void invokeRenderMapInBothHands(PoseStack var1, SubmitNodeCollector var2, int var3, float var4, float var5, float var6);

   @Invoker("renderOneHandedMap")
   void invokeRenderMapInOneHand(PoseStack var1, SubmitNodeCollector var2, int var3, float var4, HumanoidArm var5, float var6, ItemStack var7);

   @Invoker("applyItemArmTransform")
   void invokeApplyEquipOffset(PoseStack var1, HumanoidArm var2, float var3);

   @Invoker("swingArm")
   void invokeSwingArm(float var1, PoseStack var2, int var3, HumanoidArm var4);

   @Invoker("renderItem")
   void invokeRenderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, PoseStack var4, SubmitNodeCollector var5, int var6);
}
