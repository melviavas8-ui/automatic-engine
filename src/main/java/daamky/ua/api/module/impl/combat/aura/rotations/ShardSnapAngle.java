package daamky.ua.api.module.impl.combat.aura.rotations;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

/** Aspect AttackAura's Shard + Snap-1.0 SmoothRotationStrategy profile. */
public final class ShardSnapAngle extends RotateConstructor {
    private static final State STATE = new State();

    public ShardSnapAngle() {
        super("Shard Snap-1.0");
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

        AABB box = entity.getBoundingBox();
        Vec3 eye = client.player.getEyePosition();
        float yawDelta = Mth.wrapDegrees(targetAngle.getYaw() - state.yaw);
        float pitchDelta = targetAngle.getPitch() - state.pitch;
        boolean rayHitsTarget = rayHits(new Angle(state.yaw, state.pitch), box, eye);
        float cooldown = client.player.getAttackStrengthScale(1.5F);

        float nextYaw = state.yaw;
        float nextPitch = state.pitch;
        boolean alreadyOnTarget = rayHitsTarget && Math.abs(yawDelta) < 5.0F && Math.abs(pitchDelta) < 4.0F;
        if (!alreadyOnTarget) {
            if (cooldown < 0.85F) {
                state.pausing = true;
            }

            if (state.pausing) {
                float reverseStep = Math.abs(yawDelta) > 180.0F ? 0.01F : 0.001F;
                if (state.phase >= -0.08F) {
                    state.phase -= reverseStep;
                }
                if (state.phase <= -0.08F) {
                    state.pausing = false;
                }
            } else {
                state.phase += 0.012F;
                if (state.phase >= 0.38F || rayHitsTarget) {
                    state.pausing = true;
                }
            }

            float progress = Math.max(state.phase, 0.0F);
            nextYaw = state.yaw + yawDelta * Math.min(Math.max(progress * 1.9F, 0.0F), 1.0F);
            nextPitch = state.pitch + pitchDelta * Math.min(Math.max(progress * 1.5F, 0.0F), 1.0F);
            if (cooldown < 0.85F) {
                nextYaw = state.yaw;
                nextPitch = state.pitch;
            }
        }

        // Snap-1.0's short three-step movement pulse near the target.
        if (state.burstTicks == 0 && Math.abs(yawDelta) < 8.0F && Math.abs(pitchDelta) < 6.0F && state.phase > 0.04F) {
            state.burstYaw = Math.signum(yawDelta) * (0.6F + state.random.nextFloat() * 0.5F) * 2.0F;
            state.burstPitch = Math.signum(pitchDelta) * (0.3F + state.random.nextFloat() * 0.3F) * 2.0F;
            state.burstTicks = 3;
        }

        if (state.burstTicks > 0) {
            nextYaw += state.burstYaw;
            nextPitch += state.burstPitch;
            state.burstYaw *= 0.5F;
            state.burstPitch *= 0.5F;
            state.burstTicks--;
            if (state.burstTicks == 0) {
                state.burstYaw = 0.0F;
                state.burstPitch = 0.0F;
            }
        }

        if (state.noiseYaw != 0.0F || state.noisePitch != 0.0F) {
            nextYaw += state.noiseYaw;
            nextPitch += state.noisePitch;
            state.noiseYaw *= 0.4F;
            state.noisePitch *= 0.4F;
            if (Math.abs(state.noiseYaw) < 0.005F) state.noiseYaw = 0.0F;
            if (Math.abs(state.noisePitch) < 0.005F) state.noisePitch = 0.0F;
        }

        Angle result = new Angle(
            Mth.wrapDegrees(nextYaw),
            Mth.clamp(nextPitch, -89.0F, 89.0F)
        ).adjustSensitivity();
        state.yaw = result.getYaw();
        state.pitch = result.getPitch();
        return result;
    }

    /** Mirrors SmoothRotationStrategy.c(), called after a confirmed hit. */
    public static void onAttack() {
        State state = STATE;
        state.pausing = true;
        state.phase = Math.min(state.phase, 0.03F);
        state.noisePitch = 0.08F + state.random.nextFloat() * 0.08F;
        state.noiseYaw = (state.random.nextFloat() - 0.5F) * 0.08F;
    }

    public static void reset() {
        STATE.reset(Integer.MIN_VALUE, null, Angle.DEFAULT);
    }

    @Override
    public Vec3 randomValue() {
        return Vec3.ZERO;
    }

    private static boolean rayHits(Angle angle, AABB box, Vec3 eye) {
        Vec3 end = eye.add(angle.toVector().scale(100.0));
        return box.inflate(-0.3).contains(eye) || box.inflate(-0.3).clip(eye, end).isPresent();
    }

    private static final class State {
        private final Random random = new Random();
        private int targetId = Integer.MIN_VALUE;
        private Object world;
        private float yaw;
        private float pitch;
        private float phase;
        private boolean pausing;
        private int burstTicks;
        private float burstYaw;
        private float burstPitch;
        private float noiseYaw;
        private float noisePitch;

        private void reset(int targetId, Object world, Angle current) {
            this.targetId = targetId;
            this.world = world;
            this.yaw = current.getYaw();
            this.pitch = current.getPitch();
            this.phase = 0.0F;
            this.pausing = false;
            this.burstTicks = 0;
            this.burstYaw = 0.0F;
            this.burstPitch = 0.0F;
            this.noiseYaw = 0.0F;
            this.noisePitch = 0.0F;
        }
    }
}
