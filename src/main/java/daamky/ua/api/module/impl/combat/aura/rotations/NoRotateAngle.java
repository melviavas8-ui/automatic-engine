package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.movement.ElytraTarget;
import daamky.ua.utils.modules.warden.rotation.FreeLookController;

/** Aspect AttackAura's NoRotate branch, adapted to Daamky's free-look support. */
public final class NoRotateAngle extends RotateConstructor {
    public NoRotateAngle() {
        super("NoRotate");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d aimPoint, Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return currentAngle == null ? Angle.DEFAULT : currentAngle;
        }

        if (entity instanceof LivingEntity living && client.player.isGliding() && living.isGliding()) {
            Vec3d targetPoint;
            ElytraTarget elytra = ElytraTarget.getInstance();
            if (ElytraTarget.isTargetingActive() && elytra != null) {
                Vec3d center = living.getBoundingBox().getCenter();
                Vec3d targetMotion = new Vec3d(
                    living.getX() - living.prevX,
                    living.getY() - living.prevY,
                    living.getZ() - living.prevZ
                );
                double forward = (double)((int)elytra.getForward());
                Vec3d predictedOffset = living.getRotationVector().normalize().multiply(forward * 0.6)
                    .add(targetMotion.multiply(forward));
                Vec3d predicted = center.add(predictedOffset);
                double distance = client.player.getEyePos().distanceTo(predicted);
                double blend = Math.min(1.0, distance / 10.0);
                targetPoint = center.lerp(predicted, blend).add(0.0, (double)living.getHeight() * 0.3, 0.0);
            } else {
                targetPoint = living.getPos().add(0.0, (double)living.getHeight() * 0.6, 0.0);
            }
            return lookAt(client.player.getEyePos(), targetPoint).adjustSensitivity();
        }

        float yaw = FreeLookController.isActive() ? FreeLookController.getFreeYaw() : client.player.getYaw();
        float pitch = FreeLookController.isActive() ? FreeLookController.getFreePitch() : client.player.getPitch();
        return new Angle(yaw, MathHelper.clamp(pitch, -89.0F, 89.0F)).adjustSensitivity();
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
