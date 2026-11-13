package daamky.ua.api.module.impl.combat;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AutoExplosion extends Module {
    private static final Random RANDOM = new Random();

    private final BooleanSetting doubleCrystal = this.register(
        new BooleanSetting("Double Crystal", "Установить двойной кристалл", false)
    );
    private final BooleanSetting autoRotate = this.register(
        new BooleanSetting("Auto Rotate", "Автоматически наводиться на кристалл", true)
    );

    private BlockPos obsidianPos = null;
    private int tick = 0;
    private int prevSlot = -1;

    public AutoExplosion() {
        super("AutoExplosion", "Автоматически ставит кристалл на обсидиан и подрывает", ModuleCategory.COMBAT);
    }

    @SubscribeEvent
    private void onPacket(PacketEvent event) {
        if (!this.isEnabled() || !event.isSend()) return;
        if (mc.player == null) return;
        if (!(event.getPacket() instanceof PlayerInteractBlockC2SPacket packet)) return;
        if (mc.player.getMainHandStack().getItem() != Items.OBSIDIAN) return;

        BlockHitResult hit = packet.getBlockHitResult();
        this.obsidianPos = hit.getBlockPos().offset(hit.getSide());
        this.prevSlot = -1;
        this.tick = 0;
    }

    @SubscribeEvent
    private void onTick(TickEvent.Post event) {
        if (!this.isEnabled() || this.obsidianPos == null) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            this.reset();
            return;
        }

        int crystalSlot = findCrystalSlot();
        if (crystalSlot == -1
            || !mc.world.getBlockState(this.obsidianPos).isOf(Blocks.OBSIDIAN)
            || !mc.player.canInteractWithBlockAt(this.obsidianPos, 0.0)) {
            this.reset();
            return;
        }

        if (this.tick == 1 || (this.doubleCrystal.getValue() && this.tick == 5)) {
            this.placeCrystal(crystalSlot);
        }

        // Ищем кристалл в Box над обсидианом
        Box searchBox = new Box(this.obsidianPos.up()).expand(0.5);
        for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, searchBox, c -> true)) {
            if (this.autoRotate.getValue() && !isAuraControlling()) {
                this.rotateTo(aimPoint(crystal.getBoundingBox()));
            }
            if (this.tick >= 3 && mc.player.canInteractWithEntity(crystal, 0.0)) {
                mc.interactionManager.attackEntity(mc.player, crystal);
                mc.player.swingHand(Hand.MAIN_HAND);
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

    private boolean isAuraControlling() {
        AuraModule aura = AuraModule.getInstance();
        return aura != null && aura.isEnabled() && aura.canAttack(0);
    }

    private void placeCrystal(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;
        int current = mc.player.getInventory().getSelectedSlot();
        if (current != slot) {
            this.prevSlot = current;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            mc.player.getInventory().setSelectedSlot(slot);
        }
        Vec3d center = this.obsidianPos.toCenterPos();
        Vec3d hitVec = new Box(this.obsidianPos).raycast(mc.player.getEyePos(), center).orElse(center);
        this.rotateTo(center);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(hitVec, Direction.getFacing(hitVec.subtract(center)), this.obsidianPos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private Vec3d aimPoint(Box box) {
        Vec3d eye = mc.player.getEyePos();
        return new Vec3d(
            MathHelper.clamp(eye.x, box.minX, box.maxX) + rand(-0.1f, 0.1f),
            MathHelper.clamp(eye.y, box.minY, box.maxY) + rand(-0.1f, 0.1f),
            MathHelper.clamp(eye.z, box.minZ, box.maxZ) + rand(-0.1f, 0.1f)
        );
    }

    private void rotateTo(Vec3d point) {
        if (mc.player == null) return;
        Vec3d diff = point.subtract(mc.player.getEyePos());
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z))));
        mc.player.setYaw(yaw + rand(-3.0f, 3.0f));
        mc.player.setPitch(pitch + rand(-3.0f, 3.0f));
    }

    private int findCrystalSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) return i;
        }
        return -1;
    }

    private float rand(float min, float max) {
        return min + RANDOM.nextFloat() * (max - min);
    }

    private void reset() {
        if (this.prevSlot != -1 && mc.player != null) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.prevSlot));
            mc.player.getInventory().setSelectedSlot(this.prevSlot);
        }
        this.prevSlot = -1;
        this.obsidianPos = null;
        this.tick = 0;
    }

    @Override
    protected void onDisable() {
        this.reset();
    }
}