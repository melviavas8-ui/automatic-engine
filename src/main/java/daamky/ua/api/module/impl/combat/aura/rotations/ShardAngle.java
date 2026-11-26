package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

import java.util.Random;

public class ShardAngle extends RotateConstructor {
    private final Random random = new Random();
    private float lastYaw;
    private float lastPitch;
    private float acceleration = 0.0f;
    private Vec3d fixedPoint = null;
    private int fixedPointTicks = 0;

    public ShardAngle() {
        super("Shard");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d aimPoint, Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || entity == null) return currentAngle;

        // Мультипоинт (рандомизация точки на хитбоксе)
        Vec3d point = calculateMultiPoint(entity);
        Vec3d diff = point.subtract(mc.player.getEyePos());
        
        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z))));
        targetPitch = MathHelper.clamp(targetPitch, -89.0f, 89.0f);

        // Расчет дельты
        float yawDelta = MathHelper.wrapDegrees(targetYaw - this.lastYaw);
        float pitchDelta = targetPitch - this.lastPitch;
        
        // Логика ускорения (Acceleration)
        if (Math.abs(yawDelta) > 5.0f || Math.abs(pitchDelta) > 5.0f) {
            acceleration = Math.min(acceleration + 0.004f, 0.18f);
        } else {
            acceleration = Math.max(acceleration - 0.01f, 0.01f);
        }

        // Шейк (тряска) при наведении
        float shakeYaw = 0.0f;
        float shakePitch = 0.0f;
        if (Math.abs(yawDelta) < 10.0f && Math.abs(pitchDelta) < 10.0f) {
            shakeYaw = (random.nextFloat() - 0.5f) * 1.5f;
            shakePitch = (random.nextFloat() - 0.5f) * 0.8f;
        }

        // Применение
        float newYaw = this.lastYaw + yawDelta * acceleration + shakeYaw;
        float newPitch = this.lastPitch + pitchDelta * acceleration + shakePitch;

        this.lastYaw = (float) MathHelper.wrapDegrees(newYaw);
        this.lastPitch = MathHelper.clamp(newPitch, -89.0f, 89.0f);

        return new Angle(this.lastYaw, this.lastPitch);
    }

    private Vec3d calculateMultiPoint(Entity entity) {
        Box box = entity.getBoundingBox();
        if (fixedPoint != null && fixedPointTicks < 3) {
            fixedPointTicks++;
            return fixedPoint;
        } else {
            fixedPointTicks = 0;
        }

        double width = box.getLengthX();
        double height = box.getLengthY();
        double depth = box.getLengthZ();
        
        double time = System.currentTimeMillis() / 50.0;
        int id = entity.getId();
        
        // Органическая рандомизация точек
        double x = box.minX + width * 0.5 + Math.sin(time + id) * width * 0.45;
        double y = box.minY + height * 0.65 + Math.cos(time * 0.8 + id) * height * 0.2;
        double z = box.minZ + depth * 0.5 + Math.cos(time * 1.2 + id) * depth * 0.45;
        
        Vec3d newPoint = new Vec3d(x, y, z);
        fixedPoint = newPoint;
        return newPoint;
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}