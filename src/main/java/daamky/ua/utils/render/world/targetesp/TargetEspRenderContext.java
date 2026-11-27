package daamky.ua.utils.render.world.targetesp;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public record TargetEspRenderContext(
   LivingEntity target,
   Vec3 targetPos,
   Vec3 cameraPos,
   Quaternionf cameraRotation,
   float alpha,
   float partialTicks,
   long frameTimeMs,
   int primaryColor,
   int secondaryColor,
   float hurtProgress,
   float chainImpactProgress
) {
}
