package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

import java.util.Random;

public class ShardAngle extends RotateConstructor {
    private final Random random = new Random();
    
    // Состояние ротации
    private float lastYaw;
    private float lastPitch;
    private float acceleration = 0.0f;
    private boolean isAccelerating = false;
    private Vec3d fixedPoint = null;
    private int fixedPointTicks = 0;

    public ShardAngle() {
        super("Shard");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d aimPoint, Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || entity == null) return currentAngle;

        // 1. Выбор точки на хитбоксе (Мультипоинт как в Aspect)
        Vec3d point = calculateMultiPoint(entity);
        
        // 2. Расчет целевых углов
        Vec3d diff = point.subtract(mc.player.getEyePos());
        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z))));
        targetPitch = MathHelper.clamp(targetPitch, -89.0f, 89.0f);

        // 3. Логика ускорения (Acceleration)
        float yawDelta = MathHelper.wrapDegrees(targetYaw - this.lastYaw);
        float pitchDelta = targetPitch - this.lastPitch;
        
        // Если угол большой, начинаем разгон
        if (Math.abs(yawDelta) > 5.0f || Math.abs(pitchDelta) > 5.0f) {
            if (!isAccelerating) {
                isAccelerating = true;
                acceleration = 0.01f; // Начальное ускорение
            }
            acceleration = Math.min(acceleration + 0.004f, 0.18f); // Максимальное ускорение
        } else {
            isAccelerating = false;
            acceleration = Math.max(acceleration - 0.01f, 0.01f); // Спад
        }

        // 4. Применение скорости и шейка
        float speedFactor = acceleration;
        
        // Добавляем шейк (тряску) если мы близко к цели
        float shakeYaw = 0.0f;
        float shakePitch = 0.0f;
        if (Math.abs(yawDelta) < 10.0f && Math.abs(pitchDelta) < 10.0f) {
            shakeYaw = (random.nextFloat() - 0.5f) * 1.5f;
            shakePitch = (random.nextFloat() - 0.5f) * 0.8f;
        }

        // 5. Интерполяция
        float newYaw = this.lastYaw + yawDelta * speedFactor + shakeYaw;
        float newPitch = this.lastPitch + pitchDelta * speedFactor + shakePitch;

        // Сохраняем состояние для следующего тика
        this.lastYaw = (float) MathHelper.wrapDegrees(newYaw);
        this.lastPitch = MathHelper.clamp(newPitch, -89.0f, 89.0f);

        return new Angle(this.lastYaw, this.lastPitch);
    }

    private Vec3d calculateMultiPoint(Entity entity) {
        Box box = entity.getBoundingBox();
        
        // Если включена фиксация точки (как в SC3)
        if (fixedPoint != null && fixedPointTicks < 3) {
            fixedPointTicks++;
            return fixedPoint;
        } else {
            fixedPointTicks = 0;
        }

        // Рандомизация внутри хитбокса (органический режим)
        double width = box.getLengthX();
        double height = box.getLengthY();
        double depth = box.getLengthZ();
        
        // Смещение от центра с использованием синусов времени для плавности
        double time = System.currentTimeMillis() / 50.0;
        int id = entity.getId();
        
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