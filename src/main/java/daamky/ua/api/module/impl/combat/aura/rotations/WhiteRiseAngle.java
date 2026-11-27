package daamky.ua.api.module.impl.combat.aura.rotations;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

/** Aspect's WhiteRise rotation, translated to the Daamky angle pipeline. */
public final class WhiteRiseAngle extends RotateConstructor {
    private static final State STATE = new State();

    public WhiteRiseAngle() {
        super("WhiteRise");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 aimPoint, Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || currentAngle == null || targetAngle == null || entity == null) {
            reset();
            return currentAngle == null ? Angle.DEFAULT : currentAngle;
        }

        State state = STATE;
        if (state.targetId != entity.getId() || state.world != client.level) {
            state.reset(entity.getId(), client.level, currentAngle);
        }

        if (client.player.isBlocking()) {
            state.yaw = client.player.getYRot();
            state.pitch = client.player.getXRot();
            return new Angle(state.yaw, state.pitch).adjustSensitivity();
        }

        state.tick++;
        state.noise = (float)((Math.sin((double)state.tick * 0.17) * 0.12 + (state.random.nextDouble() * 0.08 - 0.04)) * 0.7);
        float yawDifference = Math.abs(Mth.wrapDegrees(targetAngle.getYaw() - state.yaw));
        boolean attackReady = client.player.getAttackStrengthScale(1.0F) > 0.9F;

        if (!state.reversing) {
            float increase = 0.0055F;
            if (yawDifference > 60.0F) {
                increase += 0.028800001F;
            } else if (yawDifference > 30.0F) {
                increase += 0.014400001F;
            } else {
                increase += 0.0072000003F;
            }
            if (attackReady) {
                increase += 0.012857143F;
            }
            state.phase += increase * (1.6F + state.noise);
            if (state.phase >= 0.22F) {
                state.reversing = true;
            }
        } else {
            float decrease = attackReady ? 0.045F : 0.008F;
            state.phase -= decrease * (2.1F + state.noise);
            if (state.phase <= -0.04F) {
                state.reversing = false;
            }
        }

        float strength = Mth.clamp(state.phase, 0.0F, client.player.isFallFlying() ? 0.38F : 0.26F);
        if (attackReady) {
            strength = Math.min(strength + 0.1F, client.player.isFallFlying() ? 0.46F : 0.34F);
        }
        strength += state.noise * 0.5F;
        if (state.tick % 7 == 0) {
            strength += 0.03F;
        }

        float yawLimit = client.player.isFallFlying() ? 42.0F : (attackReady ? 28.0F : 20.0F);
        float pitchLimit = client.player.isFallFlying() ? 12.0F : (attackReady ? 4.5F : 2.8F);
        float yawDelta = Mth.clamp(Mth.wrapDegrees(targetAngle.getYaw() - state.yaw), -yawLimit, yawLimit);
        float pitchDelta = Mth.clamp(targetAngle.getPitch() - state.pitch, -pitchLimit, pitchLimit);
        float nextYaw = state.yaw + yawDelta * strength * (0.85F + state.noise * 0.4F);
        float nextPitch = state.pitch + pitchDelta * strength * 0.28F;

        Angle result = new Angle(Mth.wrapDegrees(nextYaw), Mth.clamp(nextPitch, -89.0F, 89.0F)).adjustSensitivity();
        state.yaw = result.getYaw();
        state.pitch = result.getPitch();
        return result;
    }

    public static void reset() {
        STATE.reset(Integer.MIN_VALUE, null, Angle.DEFAULT);
    }

    @Override
    public Vec3 randomValue() {
        return Vec3.ZERO;
    }

    private static final class State {
        private final Random random = new Random();
        private int targetId = Integer.MIN_VALUE;
        private Object world;
        private float yaw;
        private float pitch;
        private float phase;
        private float noise;
        private boolean reversing;
        private int tick;

        private void reset(int targetId, Object world, Angle current) {
            this.targetId = targetId;
            this.world = world;
            this.yaw = current.getYaw();
            this.pitch = current.getPitch();
            this.phase = 0.0F;
            this.noise = 0.0F;
            this.reversing = false;
            this.tick = 0;
        }
    }
}
