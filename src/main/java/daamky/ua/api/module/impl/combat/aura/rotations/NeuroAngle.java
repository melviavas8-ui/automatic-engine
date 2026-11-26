package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NeuroAngle extends RotateConstructor {
    private final List<Float> yawHistory = new ArrayList<>();
    private final List<Float> pitchHistory = new ArrayList<>();
    private final Random random = new Random();
    
    private float lastYaw;
    private float lastPitch;
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

        // 1. Сбор данных для обучения
        if (aura.isNeuroDatasetCaptureEnabled()) {
            collectData(mc, currentYaw, currentPitch, targetAngle, (LivingEntity) entity);
        }

        // 2. Инференс (Предсказание)
        if (aura.isNeuroModelInferenceEnabled()) {
            predictNextMove(currentYaw, currentPitch, targetAngle);
            
            // Исправлено: добавлено .floatValue()
            float blend = aura.getNeuroModelBlend().getValue().floatValue();
            float finalYaw = currentYaw + (predictedYaw - currentYaw) * blend;
            float finalPitch = currentPitch + (predictedPitch - currentPitch) * blend;
            
            this.lastYaw = finalYaw;
            this.lastPitch = finalPitch;
        } else {
            // Эмуляция стиля при выключенном инференсе
            float yawDelta = MathHelper.wrapDegrees(targetAngle.getYaw() - currentYaw);
            float pitchDelta = targetAngle.getPitch() - currentPitch;
            
            float noiseYaw = (random.nextFloat() - 0.5f) * 2.0f;
            float noisePitch = (random.nextFloat() - 0.5f) * 1.0f;
            
            this.lastYaw = currentYaw + yawDelta * 0.8f + noiseYaw;
            this.lastPitch = currentPitch + pitchDelta * 0.8f + noisePitch;
        }

        return new Angle(this.lastYaw, this.lastPitch);
    }

    private void collectData(MinecraftClient mc, float currentYaw, float currentPitch, Angle targetAngle, LivingEntity entity) {
        NeuroTrainingCollector collector = NeuroTrainingCollector.getInstance();
        
        float yawDelta = MathHelper.wrapDegrees(targetAngle.getYaw() - currentYaw);
        float pitchDelta = targetAngle.getPitch() - currentPitch;
        
        collector.captureDecision(
            AuraModule.getInstance(),
            entity,
            AuraModule.getInstance().getNeuroDatasetFormat().getValue(),
            "NeuroStyle",
            System.currentTimeMillis(),
            currentYaw, currentPitch,
            targetAngle.getYaw(), targetAngle.getPitch(),
            yawDelta, pitchDelta,
            Math.hypot(yawDelta, pitchDelta),
            mc.player.distanceTo(entity),
            0.5, 
            entity.getVelocity().length(),
            entity.getVelocity().horizontalLength(),
            0, 0, 
            true, 
            1.0, 1.0, 1.0, 1.0, 1.0, 
            0, 0, 
            new double[0], new double[0], 
            0, 0, 0, 0, 0, 
            yawDelta, pitchDelta, 
            yawDelta, pitchDelta  
        );
    }

    private void predictNextMove(float currentYaw, float currentPitch, Angle targetAngle) {
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