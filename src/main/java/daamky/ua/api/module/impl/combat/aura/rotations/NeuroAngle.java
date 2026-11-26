package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NeuroAngle extends RotateConstructor {
    private final List<Float> yawHistory = new ArrayList<>();
    private final List<Float> pitchHistory = new ArrayList<>();
    private final Random random = new Random();
    private float predictedYaw;
    private float predictedPitch;

    public NeuroAngle() {
        super("Neuro");
        for (int i = 0; i < 10; i++) {
            yawHistory.add(0.0f);
            pitchHistory.add(0.0f);
        }
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d aimPoint, Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        AuraModule aura = AuraModule.getInstance();
        
        if (mc.player == null || entity == null || aura == null) {
            return currentAngle;
        }

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        // Если включен сбор данных
        if (aura.isNeuroDatasetCaptureEnabled()) {
            // Здесь можно добавить вызов твоего коллектора, если нужно
        }

        // Если включен инференс (предсказание)
        if (aura.isNeuroModelInferenceEnabled()) {
            predictNextMove(currentYaw, currentPitch);
            float blend = aura.getNeuroModelBlend().getValue().floatValue();
            
            float finalYaw = currentYaw + (predictedYaw - currentYaw) * blend;
            float finalPitch = currentPitch + (predictedPitch - currentPitch) * blend;
            
            return new Angle(finalYaw, finalPitch);
        } else {
            // Эмуляция стиля, если модель не подключена
            float yawDelta = MathHelper.wrapDegrees(targetAngle.getYaw() - currentYaw);
            float pitchDelta = targetAngle.getPitch() - currentPitch;
            
            float noiseYaw = (random.nextFloat() - 0.5f) * 2.0f;
            float noisePitch = (random.nextFloat() - 0.5f) * 1.0f;
            
            return new Angle(currentYaw + yawDelta * 0.8f + noiseYaw, currentPitch + pitchDelta * 0.8f + noisePitch);
        }
    }

    private void predictNextMove(float currentYaw, float currentPitch) {
        yawHistory.add(currentYaw);
        pitchHistory.add(currentPitch);
        if (yawHistory.size() > 10) {
            yawHistory.remove(0);
            pitchHistory.remove(0);
        }

        float avgYawChange = 0;
        float avgPitchChange = 0;
        for (int i = 1; i < yawHistory.size(); i++) {
            avgYawChange += MathHelper.wrapDegrees(yawHistory.get(i) - yawHistory.get(i - 1));
            avgPitchChange += pitchHistory.get(i) - pitchHistory.get(i - 1);
        }
        
        if (yawHistory.size() > 1) {
            avgYawChange /= (yawHistory.size() - 1);
            avgPitchChange /= (pitchHistory.size() - 1);
        }

        this.predictedYaw = currentYaw + avgYawChange * 1.5f;
        this.predictedPitch = currentPitch + avgPitchChange * 1.5f;
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}