package daamky.ua.api.module.impl.combat.aura.rotations;

import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.movement.ElytraTarget;
import daamky.ua.utils.modules.warden.rotation.FreeLookController;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Aspect AttackAura's NoRotate branch, adapted to Daamky's free-look support. */
public final class NoRotateAngle extends RotateConstructor {
    public NoRotateAngle() {
        super("NoRotate");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 aimPoint, Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return currentAngle == null ? Angle.DEFAULT : currentAngle;
        }

        if (entity instanceof LivingEntity living && client.player.isFallFlying() && living.isFallFlying()) {
            Vec3 targetPoint;
            ElytraTarget elytra = ElytraTarget.getInstance();
            if (ElytraTarget.isTargetingActive() && elytra != null) {
                Vec3 center = living.getBoundingBox().getCenter();
                Vec3 targetMotion = new Vec3(
                    living.getDeltaMovement().x,
                    living.getDeltaMovement().y,
                    living.getDeltaMovement().z
                );
                double forward = (double)((int)elytra.getForward());
                Vec3 predictedOffset = living.getLookAngle().normalize().scale(forward * 0.6)
                    .add(targetMotion.scale(forward));
                Vec3 predicted = center.add(predictedOffset);
                double distance = client.player.getEyePosition().distanceTo(predicted);
                double blend = Math.min(1.0, distance / 10.0);
                targetPoint = center.lerp(predicted, blend).add(0.0, (double)living.getBbHeight() * 0.3, 0.0);
            } else {
                targetPoint = living.position().add(0.0, (double)living.getBbHeight() * 0.6, 0.0);
            }
            return lookAt(client.player.getEyePosition(), targetPoint).adjustSensitivity();
        }

        float yaw = FreeLookController.isActive() ? FreeLookController.getFreeYaw() : client.player.getYRot();
        float pitch = FreeLookController.isActive() ? FreeLookController.getFreePitch() : client.player.getXRot();
        return new Angle(yaw, Mth.clamp(pitch, -89.0F, 89.0F)).adjustSensitivity();
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
