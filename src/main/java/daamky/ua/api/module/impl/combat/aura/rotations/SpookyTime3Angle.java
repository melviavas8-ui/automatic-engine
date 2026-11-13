package daamky.ua.api.module.impl.combat.aura.rotations;

import java.security.SecureRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

/**
 * SpookyTime 3 — точный порт mell.stroy.yd (SpookyTime rotation).
 *
 * Логика:
 *  - Если игрок внутри хитбокса цели — не двигаемся
 *  - Считаем diff yaw/pitch, ограничиваем минимальным шагом
 *  - Шаг адаптивный: 40.2°/0.2° если луч УЖЕ попадает в box, 50.2°/16.2° если нет
 *  - Каждый шаг умножается на 0.977 (инерция)
 *  - Рандом-jitter: yaw ±7.5°, pitch ±2°, обновление раз в 30мс, smooth 0.7
 *  - GCD-квантование через mouse sensitivity (как в ваниле)
 *  - Fallback: если после шага луч вышел из box — пробуем 0.5x, 0.25x, 0.125x, 0.0625x
 */
public class SpookyTime3Angle extends RotateConstructor {
    private static final SecureRandom RANDOM = new SecureRandom();

    private static long lastRandomAt = 0L;
    private static float smoothedYaw = 0.0f;
    private static float smoothedPitch = 0.0f;
    private static float randomYaw = 0.0f;
    private static float randomPitch = 0.0f;
    private static int lastTick = Integer.MIN_VALUE;

    public SpookyTime3Angle() {
        super("SpookyTime3");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d aimPoint, Entity entity) {
        if (entity == null || targetAngle == null) return currentAngle.adjustSensitivity();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return currentAngle.adjustSensitivity();

        // Защита от повторного вызова в одном тике
        int tick = mc.player.age;
        if (lastTick == tick) return currentAngle.adjustSensitivity();
        lastTick = tick;

        Vec3d eyePos = mc.player.getEyePos();
        float margin = entity.getTargetingMargin();          // в оригинале было getPickRadius()
        Box box = entity.getBoundingBox().expand(margin);

        // Если мы уже внутри хитбокса — не двигаемся
        if (box.contains(eyePos)) return currentAngle.adjustSensitivity();

        double range = mc.player.distanceTo(entity);

        float curYaw   = currentAngle.getYaw();
        float curPitch = currentAngle.getPitch();
        float tgtYaw   = targetAngle.getYaw();
        float tgtPitch = targetAngle.getPitch();

        float yawDiff   = MathHelper.wrapDegrees(tgtYaw - curYaw);
        float pitchDiff = MathHelper.wrapDegrees(tgtPitch - curPitch);

        // Попадаем ли лучом в box прямо сейчас?
        boolean hit = intersects(eyePos, box, curYaw, curPitch, range);

        // Адаптивный шаг: маленький если уже hit, большой если нет
        float minYawStep   = Math.min(Math.abs(yawDiff),   hit ? 40.2f : 50.2f);
        float minPitchStep = Math.min(Math.abs(pitchDiff), hit ? 0.2f  : 16.2f);

        float newYaw   = curYaw   + (yawDiff   > 0 ?  minYawStep   : -minYawStep)   * 0.977f;
        float newPitch = curPitch + (pitchDiff > 0 ?  minPitchStep : -minPitchStep) * 0.977f;

        // Рандом-jitter (обновляется раз в 30мс)
        long now = System.currentTimeMillis();
        if (now - lastRandomAt > 30L) {
            lastRandomAt = now;
            randomYaw   = (float) (RANDOM.nextDouble() * 15.0 - 7.5);
            randomPitch = (float) (RANDOM.nextDouble() * 4.0  - 2.0);
        }
        smoothedYaw   += (randomYaw   - smoothedYaw)   * 0.7f;
        smoothedPitch += (randomPitch - smoothedPitch) * 0.7f;

        newYaw   += smoothedYaw;
        newPitch  = MathHelper.clamp(newPitch + smoothedPitch, -90.0f, 90.0f);

        // GCD-квантование через mouse sensitivity (как в оригинале)
        double sensitivity = mc.options.getMouseSensitivity().getValue();
        float f   = (float) (sensitivity * 0.6 + 0.2);
        float gcd = f * f * f * 8.0f * 0.15f;

        newYaw   -= (newYaw   - curYaw)   % gcd;
        newPitch -= (newPitch - curPitch) % gcd;

        // Fallback: если после шага луч потерял цель — уменьшаем шаг в 2x
        if (hit && !intersects(eyePos, box, newYaw, newPitch, range)) {
            for (float k = 0.5f; k >= 0.0625f; k *= 0.5f) {
                float tYaw   = curYaw   + (newYaw   - curYaw)   * k;
                float tPitch = curPitch + (newPitch - curPitch) * k;
                tYaw   -= (tYaw   - curYaw)   % gcd;
                tPitch -= (tPitch - curPitch) % gcd;
                if (intersects(eyePos, box, tYaw, tPitch, range)) {
                    return new Angle(tYaw, tPitch);
                }
            }
            return new Angle(curYaw, curPitch);
        }

        return new Angle(newYaw, newPitch);
    }

    /**
     * Портированный yd.intersects():
     * проверяет, попадает ли луч (eye → look * maxDist) в box
     */
    private static boolean intersects(Vec3d eye, Box box, float yaw, float pitch, double maxDist) {
        float pitchRad = (float) ( pitch * Math.PI / 180.0);
        float yawRad   = (float) (-yaw   * Math.PI / 180.0);

        double dx =  MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
        double dy = -MathHelper.sin(pitchRad);
        double dz =  MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);

        Vec3d look = new Vec3d(dx, dy, dz);
        if (!box.contains(eye) && box.raycast(eye, eye.add(look.multiply(maxDist))).isEmpty()) {
            return false;
        }
        return true;
    }

    public static void reset() {
        lastTick       = Integer.MIN_VALUE;
        lastRandomAt   = 0L;
        smoothedYaw    = 0f;
        smoothedPitch  = 0f;
        randomYaw      = 0f;
        randomPitch    = 0f;
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}