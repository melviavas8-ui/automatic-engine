package daamky.ua.api.module.impl.movement;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class RageElytraTarget extends Module {
    private static RageElytraTarget instance;

    private final NumberSetting range = this.register(
        new NumberSetting("Range", "Максимальная дистанция наводки", 32.0, 6.0, 96.0, 1.0)
    );
    private final NumberSetting closeRange = this.register(
        new NumberSetting("Close Range", "Радиус 'близко к цели' (тогда lead-точка)", 4.0, 1.5, 8.0, 0.1)
    );
    private final NumberSetting lead = this.register(
        new NumberSetting("Lead", "Упреждение (блоков вперёд)", 4.0, 0.0, 8.0, 0.1)
    );
    private final NumberSetting aimSpeed = this.register(
        new NumberSetting("Aim Speed", "Скорость наводки (0-1)", 0.6, 0.05, 1.0, 0.05)
    );
    private final NumberSetting attackYawSpeed = this.register(
        new NumberSetting("Attack Yaw Speed", "Скорость yaw во время удара", 1.0, 0.2, 1.0, 0.05)
    );
    private final NumberSetting fireworkDelay = this.register(
        new NumberSetting("Firework Delay", "Задержка между фейерверками (мс)", 200.0, 50.0, 1000.0, 10.0)
    );
    private final NumberSetting minSpeed = this.register(
        new NumberSetting("Min Speed", "Мин. скорость для запуска фейерверка", 1.5, 0.5, 3.0, 0.1)
    );
    private final BooleanSetting autoFirework = this.register(
        new BooleanSetting("Auto Firework", "Автоматически запускать фейерверк", true)
    );
    private final BooleanSetting fireworkDuringAttack = this.register(
        new BooleanSetting("Firework During Attack", "Кидать фейерверк даже во время удара", true)
    );
    private final BooleanSetting autoSwapFirework = this.register(
        new BooleanSetting("Auto Swap", "Автоматически свапать фейерверк в оффхенд", true)
    );

    private long lastFireworkAt = 0L;

    public RageElytraTarget() {
        super("RageElytraTarget", "Rage-версия ElytraTarget: летит на цель, кидает фейерверк, наводится", ModuleCategory.MOVEMENT);
        instance = this;
    }

    public static RageElytraTarget getInstance() {
        return instance;
    }

    @SubscribeEvent
    private void onTick(TickEvent.Post event) {
        MinecraftClient mc = event.getClient();
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isGliding()) return;

        AuraModule aura = AuraModule.getInstance();
        boolean auraActive = aura != null && aura.isEnabled();
        boolean attacking = auraActive && aura.canAttack(0);

        // 1. Фейерверк — всегда, даже во время удара (если опция включена)
        if (autoFirework.getValue() && (!attacking || fireworkDuringAttack.getValue())) {
            handleFirework(mc);
        }

        if (!auraActive) return;

        // 2. Цель
        LivingEntity target = AuraModule.target;
        if (target == null || !target.isAlive()) return;
        double distance = mc.player.distanceTo(target);
        if (distance > range.getFloat()) return;

        // 3. Точка прицела
        Vec3d eye = mc.player.getEyePos();
        Vec3d enemyCenter = target.getBoundingBox().getCenter();

        Vec3d aimPoint;
        if (distance > closeRange.getFloat()) {
            // Далеко — летим в центр цели (chase)
            aimPoint = enemyCenter;
        } else {
            // Близко — в lead-точку (intercept), чтобы лететь с упреждением
            double dx = enemyCenter.x - eye.x;
            double dz = enemyCenter.z - eye.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double nx = horizontal == 0.0 ? 0.0 : dx / horizontal;
            double nz = horizontal == 0.0 ? 0.0 : dz / horizontal;
            double lift = Math.max(0.0, 3.0 - getGroundDistance(mc));
            aimPoint = new Vec3d(
                enemyCenter.x + nx * lead.getFloat(),
                enemyCenter.y + lift,
                enemyCenter.z + nz * lead.getFloat()
            );
        }

        // 4. Расчёт target yaw/pitch
        Vec3d diff = aimPoint.subtract(eye);
        float targetYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z))));

        float currentYaw = MathHelper.wrapDegrees(mc.player.getYaw());
        float currentPitch = mc.player.getPitch();

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = targetPitch - currentPitch;

        float speed = attacking ? attackYawSpeed.getFloat() : aimSpeed.getFloat();

        float newYaw = currentYaw + deltaYaw * speed;
        float newPitch;
        if (attacking) {
            // Во время атаки — только yaw, pitch оставляем килауре
            newPitch = currentPitch;
        } else {
            newPitch = MathHelper.clamp(currentPitch + deltaPitch * speed, -90.0f, 90.0f);
        }

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
    }

    private void handleFirework(MinecraftClient mc) {
        if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            long now = System.currentTimeMillis();
            if (now - lastFireworkAt < fireworkDelay.getValue().longValue()) return;
            if (mc.player.getVelocity().length() >= minSpeed.getFloat()) return;
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            mc.player.swingHand(Hand.OFF_HAND);
            lastFireworkAt = now;
            return;
        }

        if (!autoSwapFirework.getValue()) return;
        if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) return;

        int slot = findFireworkSlot(mc);
        if (slot == -1) return;

        int syncId = mc.player.playerScreenHandler.syncId;
        if (slot < 9) {
            mc.interactionManager.clickSlot(syncId, 36 + slot, 40, SlotActionType.SWAP, mc.player);
        } else {
            mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
        }
    }

    private int findFireworkSlot(MinecraftClient mc) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }

    private double getGroundDistance(MinecraftClient mc) {
        Vec3d start = mc.player.getPos();
        Vec3d end = start.subtract(0.0, 2.0, 0.0);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
            start, end,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        ));
        if (hit.getType() == HitResult.Type.MISS) return 2.0;
        return start.getY() - hit.getPos().y;
    }
}