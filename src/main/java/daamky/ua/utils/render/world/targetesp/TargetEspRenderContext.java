package daamky.ua.utils.render.world.targetesp;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

public record TargetEspRenderContext(
   LivingEntity target,
   Vec3d targetPos,
   Vec3d cameraPos,
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
