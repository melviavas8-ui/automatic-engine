package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

/**
 * Aspect AttackAura's Shard-2.0 (AdaptiveRotationStrategy), adapted to
 * Daamky's RotateConstructor pipeline. The source strategy is stateful,
 * therefore state is shared between the per-tick constructor instances.
 */
public final class ShardAngle extends RotateConstructor {
    private static final State STATE = new State();

    public ShardAngle() {
        super("Shard");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d aimPoint, Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || currentAngle == null || targetAngle == null || entity == null) {
            reset();
            return currentAngle == null ? Angle.DEFAULT : currentAngle;
        }

        State state = STATE;
        if (state.targetId != entity.getId() || state.world != client.world) {
            state.reset(entity.getId(), client.world, currentAngle);
        }

        // Re-align if a different rotation owner has moved the active view.
        float externalYaw = Math.abs(MathHelper.wrapDegrees(currentAngle.getYaw() - state.yaw));
        float externalPitch = Math.abs(currentAngle.getPitch() - state.pitch);
        if (externalYaw > 45.0F || externalPitch > 35.0F) {
            state.yaw = currentAngle.getYaw();
            state.pitch = currentAngle.getPitch();
            state.yawMomentum = 0.0F;
            state.pitchMomentum = 0.0F;
        }

        Box box = entity.getBoundingBox();
        Vec3d eye = client.player.getEyePos();
        // The source strategy tests the player's eye position against the target box.
        boolean eyeInside = box.expand(-0.3).contains(eye) || box.expand(0.3).contains(eye);
        float desiredAimFactor = eyeInside ? 0.05F : 1.0F;
        float aimFactorBlend = eyeInside ? 1.0F : 0.01F;
        state.aimFactor += (desiredAimFactor - state.aimFactor) * aimFactorBlend;
        state.aimFactor = MathHelper.clamp(state.aimFactor, 0.05F, 1.0F);

        float yawDelta = MathHelper.wrapDegrees(targetAngle.getYaw() - state.yaw);
        float pitchDelta = targetAngle.getPitch() - state.pitch;
        boolean rayHitsTarget = rayHits(new Angle(state.yaw, state.pitch), box, eye);
        boolean attackWindow = client.player.getAttackCooldownProgress(0.5F) >= 0.65F
            || (!client.player.isOnGround() && client.player.getVelocity().y < 0.0 && client.player.fallDistance > 0.0F);

        if (state.attackPause && attackWindow) {
            state.attackPause = false;
            state.reversing = false;
            state.phase = 0.0F;
        }

        float nextYaw = state.yaw;
        float nextPitch = state.pitch;
        if (state.attackPause) {
            // Aspect freezes the adaptive phase between a hit and the next attack window.
            state.phase = Math.min(state.phase, 0.0F);
        } else if (!rayHitsTarget) {
            if (state.reversing) {
                float reverseStep = Math.abs(yawDelta) > 30.0F ? 0.01F : 0.046F;
                if (state.phase >= -0.1F) {
                    state.phase -= reverseStep;
                }
                if (state.phase <= -0.1F) {
                    state.reversing = false;
                }
            } else {
                state.phase += 0.003F;
                if (state.phase >= 0.25F) {
                    state.reversing = true;
                }
            }

            float progress = Math.max(state.phase, 0.0F) * state.aimFactor;
            nextYaw = state.yaw + yawDelta * Math.min(Math.max(progress * 1.8F, 0.0F), 1.0F);
            nextPitch = state.pitch + pitchDelta * Math.min(Math.max(progress * 0.4F, 0.0F), 1.0F);
        }

        if (Math.abs(yawDelta) > 18.0F && Math.abs(state.phase) > 0.08F) {
            state.yawMomentum = Math.signum(yawDelta) * Math.min(Math.abs(yawDelta) * 0.06F, 1.2F) * 0.9F;
            state.pitchMomentum = Math.signum(pitchDelta) * Math.min(Math.abs(pitchDelta) * 0.04F, 0.8F) * 0.9F;
        } else {
            state.yawMomentum *= 0.65F;
            state.pitchMomentum *= 0.65F;
        }

        nextYaw = MathHelper.wrapDegrees(nextYaw + state.yawMomentum);
        nextPitch = MathHelper.clamp(nextPitch + state.pitchMomentum, -89.0F, 89.0F);
        Angle result = new Angle(nextYaw, nextPitch).adjustSensitivity();
        state.yaw = result.getYaw();
        state.pitch = result.getPitch();
        return result;
    }

    /** Called only after Aura confirms that an attack was actually performed. */
    public static void onAttack() {
        STATE.attackPause = true;
        STATE.reversing = true;
        STATE.phase = -0.04F;
    }

    public static void reset() {
        STATE.reset(Integer.MIN_VALUE, null, Angle.DEFAULT);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    private static boolean rayHits(Angle angle, Box box, Vec3d eye) {
        Vec3d end = eye.add(angle.toVector().multiply(1950.0));
        return box.expand(-0.3).contains(eye) || box.expand(-0.3).raycast(eye, end).isPresent();
    }

    private static final class State {
        private int targetId = Integer.MIN_VALUE;
        private Object world;
        private float yaw;
        private float pitch;
        private float phase;
        private float aimFactor = 1.0F;
        private float yawMomentum;
        private float pitchMomentum;
        private boolean reversing;
        private boolean attackPause;

        private void reset(int targetId, Object world, Angle current) {
            this.targetId = targetId;
            this.world = world;
            this.yaw = current.getYaw();
            this.pitch = current.getPitch();
            this.phase = 0.0F;
            this.aimFactor = 1.0F;
            this.yawMomentum = 0.0F;
            this.pitchMomentum = 0.0F;
            this.reversing = false;
            this.attackPause = false;
        }
    }
}
