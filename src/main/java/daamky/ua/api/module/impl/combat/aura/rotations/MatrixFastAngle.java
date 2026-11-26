package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.movement.ElytraTarget;

/** Aspect AttackAura's Matrix/Fast path, with Daamky sensitivity quantization. */
public final class MatrixFastAngle extends RotateConstructor {
    public MatrixFastAngle() {
        super("Matrix/Fast 2");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d aimPoint, Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || entity == null || currentAngle == null || targetAngle == null) {
            return currentAngle == null ? Angle.DEFAULT : currentAngle;
        }

        Vec3d targetPoint;
        if (entity instanceof LivingEntity living) {
            targetPoint = living.getEntityPos().add(0.0, (double)living.getHeight() * 0.6, 0.0);
            if (client.player.isGliding() && living.isGliding() && ElytraTarget.isTargetingActive()) {
                ElytraTarget elytra = ElytraTarget.getInstance();
                if (elytra != null) {
                    targetPoint = targetPoint.add(living.getRotationVector().normalize().multiply(elytra.getForward()));
                }
            }
        } else if (aimPoint != null) {
            targetPoint = aimPoint;
        } else {
            return new Angle(targetAngle.getYaw(), targetAngle.getPitch()).adjustSensitivity();
        }

        return lookAt(client.player.getEyePos(), targetPoint).adjustSensitivity();
    }

    private static Angle lookAt(Vec3d eye, Vec3d point) {
        Vec3d delta = point.subtract(eye);
        float yaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(delta.y, delta.horizontalLength())));
        return new Angle(yaw, pitch);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}
