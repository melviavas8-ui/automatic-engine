package daamky.ua.api.module.impl.combat.aura.rotations;

import java.security.SecureRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

public class SpookyTime2Angle extends RotateConstructor {
    private static final SecureRandom RANDOM = new SecureRandom();

    // ВАЖНО: static — иначе new SpookyTime2Angle() каждый тик сбрасывает состояние
    private static float interpolationProgress = 1.0f;
    private static float startYaw, startPitch;
    private static float endYaw,   endPitch;

    public SpookyTime2Angle() {
        super("SpookyTime2");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 aimPoint, Entity entity) {
        AuraModule aura = AuraModule.getInstance();
        Minecraft mc = Minecraft.getInstance();
        if (aura == null || targetAngle == null || mc.player == null) {
            interpolationProgress = 1.0f;
            return currentAngle.adjustSensitivity();
        }

        return stepWithOscillation(currentAngle, targetAngle).adjustSensitivity();
    }

    private Angle stepWithOscillation(Angle currentAngle, Angle targetAngle) {
        float yawDelta   = Mth.wrapDegrees(targetAngle.getYaw() - currentAngle.getYaw());
        float pitchDelta = targetAngle.getPitch() - currentAngle.getPitch();
        float length     = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (length < 1.0e-4f) length = 1.0e-4f;

        float yawSpeed   = randomLerp(25.0f, 360.0f);
        float pitchSpeed = randomLerp(4.0f, 10.0f);

        double time = System.currentTimeMillis() / 70.0;
        float oscYaw   = (float) (5.0 * Math.cos(time));
        float oscPitch = (float) (4.0 * Math.sin(time));

        if (shouldPauseTurn(currentAngle, targetAngle)) {
            yawSpeed = 0.0f; pitchSpeed = 0.0f;
            oscYaw   = 0.0f; oscPitch   = 0.0f;
        }

        float yawStep   = Math.abs(yawDelta   / length) * yawSpeed;
        float pitchStep = Math.abs(pitchDelta / length) * pitchSpeed;

        float newYaw = currentAngle.getYaw()
            + Mth.clamp(yawDelta, -yawStep, yawStep);

        float newPitch = Mth.clamp(
            currentAngle.getPitch() + Mth.clamp(pitchDelta, -pitchStep, pitchStep),
            -89.0f, 89.0f
        );

        return interpolateTurn(
            currentAngle,
            newYaw + oscYaw,
            newPitch + oscPitch,
            randomLerp(0.1f, 0.4f)
        );
    }

    private boolean shouldPauseTurn(Angle currentAngle, Angle targetAngle) {
        float yawDiff   = Math.abs(Mth.wrapDegrees(targetAngle.getYaw() - currentAngle.getYaw()));
        float pitchDiff = Math.abs(targetAngle.getPitch() - currentAngle.getPitch());
        if (yawDiff < 2.0f && pitchDiff < 2.0f) return true;
        return RANDOM.nextInt(8) == 0;
    }

    private Angle interpolateTurn(Angle currentAngle, float targetYaw, float targetPitch, float progressStep) {
        float yawDiff   = Math.abs(Mth.wrapDegrees(targetYaw - endYaw));
        float pitchDiff = Math.abs(targetPitch - endPitch);

        if (interpolationProgress >= 1.0f || yawDiff > 6.0f || pitchDiff > 6.0f) {
            startYaw   = currentAngle.getYaw();
            startPitch = currentAngle.getPitch();
            endYaw     = targetYaw;
            endPitch   = targetPitch;
            interpolationProgress = 0.0f;
        }

        interpolationProgress = Mth.clamp(interpolationProgress + progressStep, 0.0f, 1.0f);
        float eased = easeOutBack(interpolationProgress);

        float yawRange   = Mth.wrapDegrees(endYaw - startYaw);
        float pitchRange = endPitch - startPitch;

        float newYaw   = startYaw   + yawRange   * eased;
        float newPitch = Mth.clamp(startPitch + pitchRange * eased, -89.0f, 89.0f);
        return new Angle(newYaw, newPitch);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float f  = t - 1.0f;
        return 1.0f + c3 * f * f * f + c1 * f * f;
    }

    private float randomLerp(float min, float max) {
        return Mth.lerp(RANDOM.nextFloat(), min, max);
    }

    @Override
    public Vec3 randomValue() {
        return Vec3.ZERO;
    }
}