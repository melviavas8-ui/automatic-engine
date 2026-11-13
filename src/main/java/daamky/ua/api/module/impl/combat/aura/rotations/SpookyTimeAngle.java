package daamky.ua.api.module.impl.combat.aura.rotations;

import java.security.SecureRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

public class SpookyTimeAngle extends RotateConstructor {
    private static final SecureRandom RANDOM = new SecureRandom();

    public SpookyTimeAngle() {
        super("SpookyTime");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d aimPoint, Entity entity) {
        AuraModule aura = AuraModule.getInstance();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (aura == null || targetAngle == null || mc.player == null) {
            return currentAngle.adjustSensitivity();
        }

        float yawDelta   = MathHelper.wrapDegrees(targetAngle.getYaw() - currentAngle.getYaw());
        float pitchDelta = targetAngle.getPitch() - currentAngle.getPitch();
        float length     = (float) Math.hypot(yawDelta, pitchDelta);
        if (length < 1.0e-4f) return currentAngle.adjustSensitivity();

        StrikeManager handler = aura.getAttackHandlerRaw();
        int attackCount = handler != null ? handler.getCount() : 0;
        boolean boosted = (attackCount % 8) != 3;

        float yawWeight   = boosted ? randomLerp(0.6f, 0.8f) : randomLerp(0.3f, 0.4f);
        float pitchWeight = boosted ? randomLerp(0.6f, 0.8f) : randomLerp(0.3f, 0.4f);

        float lerpTo = randomLerp(50.0f, 55.0f);
        float interp = MathHelper.lerp(RANDOM.nextFloat(), lerpTo, 0.1f);

        float cooldown = MathHelper.clamp(mc.player.getAttackCooldownProgress(0.0f), 0.0f, 1.0f);
        float ease     = bounceInOut(cooldown);

        float yawStep   = yawWeight * interp * 0.1f;
        float pitchStep = pitchWeight * randomLerp(0.05f, 0.5f) * 0.1f;
        if (boosted) yawStep *= 0.5f;

        float jitterYaw   = randomLerp(-0.14f, 0.14f);
        float jitterPitch = randomLerp(-0.14f, 0.14f);

        float scale = 0.1f + 0.5f * ease;

        float yawMove   = (yawDelta   + jitterYaw)   * yawStep   * scale;
        float pitchMove = (pitchDelta + jitterPitch) * pitchStep * scale;

        float newYaw   = currentAngle.getYaw() + yawMove;
        float newPitch = MathHelper.clamp(currentAngle.getPitch() + pitchMove, -89.0f, 90.0f);

        return new Angle(newYaw, newPitch).adjustSensitivity();
    }

    private static float bounceInOut(float t) {
        if (t < 0.5f) return 8.0f * t * t * t * t;
        float f = t - 1.0f;
        return 1.0f - 8.0f * f * f * f * f;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(RANDOM.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}