package daamky.ua.api.module.impl.combat;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.types.EventPhase;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class AutoExplosion extends Module {
    private static final Random RANDOM = new Random();

    private final BooleanSetting doubleCrystal = this.register(
        new BooleanSetting("Double Crystal", "Установить двойной кристалл", false)
    );
    private final NumberSetting aimSpeed = this.register(
        new NumberSetting("Aim Speed", "Скорость наводки (0-1)", 0.5, 0.1, 1.0, 0.05)
    );
    private final BooleanSetting silent = this.register(
        new BooleanSetting("Silent Aim", "Поворот только на сервере (камера не двигается)", true)
    );
    private final BooleanSetting smooth = this.register(
        new BooleanSetting("Smooth Aim", "Плавная интерполяция (быстро, но не рывком)", true)
    );

    private BlockPos obsidianPos = null;
    private int tick = 0;
    private int prevSlot = -1;
    private Vec3 aimPoint = null;
    private float targetYaw = 0f;
    private float targetPitch = 0f;

    public AutoExplosion() {
        super("AutoExplosion", "Автоматически ставит кристалл на обсидиан и подрывает", ModuleCategory.COMBAT);
    }

    @SubscribeEvent
    private void onPacket(PacketEvent event) {
        if (!this.isEnabled() || !event.isSend()) return;
        if (mc.player == null) return;
        if (!(event.getPacket() instanceof ServerboundUseItemOnPacket packet)) return;
        if (mc.player.getMainHandItem().getItem() != Items.OBSIDIAN) return;

        BlockHitResult hit = packet.getHitResult();
        this.obsidianPos = hit.getBlockPos().relative(hit.getDirection());
        this.prevSlot = -1;
        this.tick = 0;
    }

    @SubscribeEvent
    private void onRotationUpdate(RotationUpdateEvent event) {
        if (!this.isEnabled() || event.getPhase() != EventPhase.PRE) return;
        if (mc.player == null || aimPoint == null) return;

        float curYaw = Mth.wrapDegrees(mc.player.getYRot());
        float curPitch = mc.player.getXRot();

        float newYaw;
        float newPitch;

        if (smooth.getValue()) {
            // Плавная интерполяция
            float deltaYaw = Mth.wrapDegrees(targetYaw - curYaw);
            float deltaPitch = targetPitch - curPitch;
            float speed = aimSpeed.getFloat();
            newYaw = curYaw + deltaYaw * speed;
            newPitch = Mth.clamp(curPitch + deltaPitch * speed, -90f, 90f);
        } else {
            // Мгновенный snap
            newYaw = targetYaw;
            newPitch = Mth.clamp(targetPitch, -90f, 90f);
        }

        if (silent.getValue()) {
            // Silent: меняем локально, но на следующем тике откатываем
            mc.player.setYRot(newYaw);
            mc.player.setXRot(newPitch);
            // Возврат будет в onTick Post — когда interactBlock уже отправил пакет
        } else {
            mc.player.setYRot(newYaw);
            mc.player.setXRot(newPitch);
        }
    }

    @SubscribeEvent
    private void onTick(TickEvent.Post event) {
        if (!this.isEnabled() || this.obsidianPos == null) return;
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            this.reset();
            return;
        }

        int crystalSlot = findCrystalSlot();
        if (crystalSlot == -1
            || !mc.level.getBlockState(this.obsidianPos).is(Blocks.OBSIDIAN)
            || !mc.player.isWithinBlockInteractionRange(this.obsidianPos, 0.0)) {
            this.reset();
            return;
        }

        if (this.tick == 1 || (this.doubleCrystal.getValue() && this.tick == 5)) {
            this.placeCrystal(crystalSlot);
        }

        AABB searchBox = new AABB(this.obsidianPos.above()).inflate(0.5);
        for (EndCrystal crystal : mc.level.getEntitiesOfClass(EndCrystal.class, searchBox, c -> true)) {
            this.setAimTarget(aimPoint(crystal.getBoundingBox()));

            if (this.tick >= 3 && mc.player.isWithinEntityInteractionRange(crystal, 0.0)) {
                mc.gameMode.attack(mc.player, crystal);
                mc.player.swing(InteractionHand.MAIN_HAND);
                if (this.doubleCrystal.getValue() && this.tick < 7) {
                    break;
                }
                this.reset();
                return;
            }
        }

        this.tick++;
        if (this.tick > (this.doubleCrystal.getValue() ? 8 : 4)) {
            this.reset();
        }
    }

    private void placeCrystal(int slot) {
        if (mc.player == null || mc.gameMode == null) return;
        int current = mc.player.getInventory().getSelectedSlot();
        if (current != slot) {
            this.prevSlot = current;
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            mc.player.getInventory().setSelectedSlot(slot);
        } else {
            // Уже на кристалле — запоминаем текущий слот как prevSlot тоже (на случай если не вернётся)
            if (this.prevSlot == -1) {
                this.prevSlot = current;
            }
        }
        Vec3 center = this.obsidianPos.getCenter();
        Vec3 hitVec = new AABB(this.obsidianPos).clip(mc.player.getEyePosition(), center).orElse(center);
        this.setAimTarget(center);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(hitVec, Direction.getApproximateNearest(hitVec.subtract(center)), this.obsidianPos, false));
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void setAimTarget(Vec3 point) {
        if (mc.player == null) return;
        this.aimPoint = point;
        Vec3 diff = point.subtract(mc.player.getEyePosition());
        this.targetYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f + rand(-3.0f, 3.0f);
        this.targetPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)))) + rand(-3.0f, 3.0f);
    }

    private Vec3 aimPoint(AABB box) {
        Vec3 eye = mc.player.getEyePosition();
        return new Vec3(
            Mth.clamp(eye.x, box.minX, box.maxX) + rand(-0.1f, 0.1f),
            Mth.clamp(eye.y, box.minY, box.maxY) + rand(-0.1f, 0.1f),
            Mth.clamp(eye.z, box.minZ, box.maxZ) + rand(-0.1f, 0.1f)
        );
    }

    private int findCrystalSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.END_CRYSTAL) return i;
        }
        return -1;
    }

    private float rand(float min, float max) {
        return min + RANDOM.nextFloat() * (max - min);
    }

    private void reset() {
        // Возврат слота
        if (this.prevSlot != -1 && mc.player != null) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(this.prevSlot));
            mc.player.getInventory().setSelectedSlot(this.prevSlot);
        }
        this.prevSlot = -1;
        this.obsidianPos = null;
        this.tick = 0;
        this.aimPoint = null;
    }

    @Override
    protected void onDisable() {
        this.reset();
    }
}