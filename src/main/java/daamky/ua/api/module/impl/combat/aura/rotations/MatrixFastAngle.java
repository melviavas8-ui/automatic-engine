package daamky.ua.api.module.impl.combat.aura.rotations;

import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.movement.ElytraTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Aspect AttackAura's Matrix/Fast path, with Daamky sensitivity quantization. */
public final class MatrixFastAngle extends RotateConstructor {
    public MatrixFastAngle() {
        super("Matrix/Fast 2");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 aimPoint, Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || entity == null || currentAngle == null || targetAngle == null) {
            return currentAngle == null ? Angle.DEFAULT : currentAngle;
        }

        Vec3 targetPoint;
        if (entity instanceof LivingEntity living) {
            targetPoint = living.position().add(0.0, (double)living.getBbHeight() * 0.6, 0.0);
            if (client.player.isFallFlying() && living.isFallFlying() && ElytraTarget.isTargetingActive()) {
                ElytraTarget elytra = ElytraTarget.getInstance();
                if (elytra != null) {
                    targetPoint = targetPoint.add(living.getLookAngle().normalize().scale(elytra.getForward()));
                }
            }
        } else if (aimPoint != null) {
            targetPoint = aimPoint;
        } else {
            return new Angle(targetAngle.getYaw(), targetAngle.getPitch()).adjustSensitivity();
        }

        return lookAt(client.player.getEyePosition(), targetPoint).adjustSensitivity();
    }

    private static Angle lookAt(Vec3 eye, Vec3 point) {
        Vec3 delta = point.subtract(eye);
        float yaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
        return new Angle(yaw, pitch);
    }

    @Override
    public Vec3 randomValue() {
        return Vec3.ZERO;
    }
}
