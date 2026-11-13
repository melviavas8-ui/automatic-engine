package daamky.ua.api.module.impl.combat;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.types.EventPhase;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
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

/**
 * Port of Elyaysx AutoExplosion module.
 * Automatically places End Crystal (or charges Respawn Anchor) after placing the base block
 * and detonates the crystal while optionally protecting the player and valuable drops.
 */
public final class ElyaysxAutoExplosion extends Module {
    private final BooleanSetting avoidSelfDamage = this.register(
        new BooleanSetting("Не взрывать себя", "Не атаковать кристалл, если игрок слишком близко по высоте", true)
    );
    private final BooleanSetting preserveValuableDrops = this.register(
        new BooleanSetting("Не взрывать ресурсы", "Не взрывать, если рядом лежат ценные предметы (незерит, яблоки, элитры и т.д.)", true)
    );

    private BlockPos pendingExplosionPosition;
    private int activeExplosionItemSlot = -1;
    private boolean isRespawnAnchorBoost;
    private int previousSelectedSlot = -1;
    private boolean restoreSelectedSlotNextTick;
    private Box crystalSearchBounds;
    private BlockPos pendingCrystalBasePosition;
    private boolean deferExplosionUpdate;

    private Vec3d aimPoint;
    private float targetYaw;
    private float targetPitch;

    public ElyaysxAutoExplosion() {
        super("ElyaysxAutoExplosion", "Автоматически взрывает кристаллы (порт Elyaysx)", ModuleCategory.COMBAT);
    }

    @SubscribeEvent
    private void onPacket(PacketEvent event) {
        if (!this.isEnabled() || !event.isSend() || mc.player == null) {
            return;
        }

        if (!(event.getPacket() instanceof PlayerInteractBlockC2SPacket packet)) {
            return;
        }

        BlockHitResult hit = packet.getBlockHitResult();
        BlockPos clickedPos = hit.getBlockPos();
        Item mainItem = mc.player.getMainHandStack().getItem();

        // Detect placement of Obsidian / Bedrock base or Respawn Anchor
        if (mainItem == Items.OBSIDIAN || mainItem == Items.RESPAWN_ANCHOR) {
            BlockPos placedPos = clickedPos.offset(hit.getSide());
            if (mainItem == Items.OBSIDIAN) {
                int crystalSlot = findItemSlot(Items.END_CRYSTAL, true);
                if (crystalSlot == -1) {
                    return;
                }
                this.pendingExplosionPosition = placedPos.toImmutable();
                this.activeExplosionItemSlot = crystalSlot;
                this.isRespawnAnchorBoost = false;
            } else {
                int glowstoneSlot = findItemSlot(Items.GLOWSTONE, true);
                if (glowstoneSlot == -1) {
                    return;
                }
                this.pendingExplosionPosition = placedPos.toImmutable();
                this.activeExplosionItemSlot = glowstoneSlot;
                this.isRespawnAnchorBoost = true;
            }
            this.deferExplosionUpdate = true;
            this.previousSelectedSlot = -1;
            return;
        }

        // Track right-click on existing Obsidian/Bedrock for crystal placement detection
        if (mc.world != null) {
            Block block = mc.world.getBlockState(clickedPos).getBlock();
            if ((block == Blocks.OBSIDIAN || block == Blocks.BEDROCK)
                && !mc.player.getItemCooldownManager().isCoolingDown(Items.END_CRYSTAL.getDefaultStack())) {
                this.pendingCrystalBasePosition = clickedPos.toImmutable();
                this.crystalSearchBounds = new Box(clickedPos.up()).expand(0.1);
            }
        }
    }

    @SubscribeEvent
    private void onRotationUpdate(RotationUpdateEvent event) {
        if (!this.isEnabled() || event.getPhase() != EventPhase.PRE) {
            return;
        }
        if (mc.player == null || this.aimPoint == null) {
            return;
        }

        // Instant aim for precise placement / attack (can be made smooth later if needed)
        mc.player.setYaw(this.targetYaw);
        mc.player.setPitch(MathHelper.clamp(this.targetPitch, -90.0f, 90.0f));
    }

    @SubscribeEvent
    private void onTick(TickEvent.Post event) {
        if (!this.isEnabled()) {
            return;
        }
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            this.clearAll();
            return;
        }

        // Restore hotbar slot after interaction
        if (this.restoreSelectedSlotNextTick) {
            this.restoreSelectedSlotNextTick = false;
            if (this.previousSelectedSlot != -1) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.previousSelectedSlot));
                mc.player.getInventory().setSelectedSlot(this.previousSelectedSlot);
            }
            this.previousSelectedSlot = -1;
            return;
        }

        // Place crystal / charge anchor
        if (this.pendingExplosionPosition != null) {
            if (mc.world.getBlockState(this.pendingExplosionPosition).isAir()) {
                this.clearExplosionTarget();
            } else if (this.deferExplosionUpdate) {
                this.deferExplosionUpdate = false;
            } else {
                this.performPlace();
            }
            return;
        }

        // Attack placed crystals
        this.attackNearbyCrystals();
    }

    private void performPlace() {
        if (this.activeExplosionItemSlot < 0 || this.activeExplosionItemSlot > 8) {
            this.clearExplosionTarget();
            return;
        }

        Vec3d eye = mc.player.getEyePos();
        Vec3d closest = closestPointOnBox(eye, new Box(this.pendingExplosionPosition));
        this.setAimTarget(closest);

        this.previousSelectedSlot = mc.player.getInventory().getSelectedSlot();
        if (this.previousSelectedSlot != this.activeExplosionItemSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.activeExplosionItemSlot));
            mc.player.getInventory().setSelectedSlot(this.activeExplosionItemSlot);
        }

        Vec3d lookDir = closest.subtract(eye).negate();
        Direction facing = Direction.getFacing(lookDir.x, lookDir.y, lookDir.z);

        mc.interactionManager.interactBlock(
            mc.player,
            Hand.MAIN_HAND,
            new BlockHitResult(closest, facing, this.pendingExplosionPosition, false)
        );
        mc.player.swingHand(Hand.MAIN_HAND);

        this.restoreSelectedSlotNextTick = true;

        if (this.isRespawnAnchorBoost) {
            // After charging, keep the position so next interaction can detonate if desired.
            // For now we clear and let the player / next cycle handle detonation.
            this.activeExplosionItemSlot = this.previousSelectedSlot;
            this.isRespawnAnchorBoost = false;
            // Do not clear pending immediately — allow one more cycle if needed
            this.clearExplosionTarget();
        } else {
            // Crystal was placed — now look for it to explode
            this.pendingCrystalBasePosition = this.pendingExplosionPosition.toImmutable();
            this.crystalSearchBounds = new Box(this.pendingExplosionPosition.up()).expand(0.15);
            this.clearExplosionTarget();
        }
    }

    private void attackNearbyCrystals() {
        if (this.crystalSearchBounds == null) {
            return;
        }

        if (this.pendingCrystalBasePosition != null
            && mc.world.getBlockState(this.pendingCrystalBasePosition).isAir()) {
            this.clearCrystalTracking();
            return;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) {
                continue;
            }
            if (!this.crystalSearchBounds.contains(crystal.getX(), crystal.getY(), crystal.getZ())) {
                continue;
            }

            BlockPos crystalBase = BlockPos.ofFloored(crystal.getX(), crystal.getY() - 1.0, crystal.getZ());

            if (this.avoidSelfDamage.getValue()
                && Math.abs(mc.player.getY() - crystal.getY()) < 1.0) {
                this.clearCrystalTracking();
                return;
            }

            if (this.containsValuableDrops(crystal)) {
                this.clearCrystalTracking();
                return;
            }

            Vec3d eye = mc.player.getEyePos();
            if (!crystal.getBoundingBox().contains(eye)) {
                Vec3d aim = closestPointOnBox(eye, crystal.getBoundingBox());
                this.setAimTarget(aim);
            }

            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.clearCrystalTracking();
            return;
        }
    }

    private boolean containsValuableDrops(EndCrystalEntity crystal) {
        if (!this.preserveValuableDrops.getValue() || mc.world == null) {
            return false;
        }

        Vec3d pos = new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ());
        Box search = new Box(pos, pos).expand(6.0);

        for (ItemEntity itemEntity : mc.world.getEntitiesByClass(ItemEntity.class, search, Entity::isAlive)) {
            if (isValuableItem(itemEntity.getStack().getItem())) {
                return true;
            }
        }
        return false;
    }

    private boolean isValuableItem(Item item) {
        if (item == Items.GOLDEN_APPLE
            || item == Items.ENCHANTED_GOLDEN_APPLE
            || item == Items.ELYTRA
            || item == Items.PLAYER_HEAD
            || item == Items.END_CRYSTAL) {
            return true;
        }
        String key = item.getTranslationKey();
        return key != null && key.contains("netherite");
    }

    private int findItemSlot(Item target, boolean hotbarOnly) {
        int limit = hotbarOnly ? 9 : 36;
        int found = -1;
        for (int i = 0; i < limit; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == target) {
                found = i;
            }
        }
        return found;
    }

    private void setAimTarget(Vec3d point) {
        if (mc.player == null) {
            return;
        }
        this.aimPoint = point;
        Vec3d diff = point.subtract(mc.player.getEyePos());
        this.targetYaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        this.targetPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))));
    }

    private static Vec3d closestPointOnBox(Vec3d from, Box box) {
        return new Vec3d(
            MathHelper.clamp(from.x, box.minX, box.maxX),
            MathHelper.clamp(from.y, box.minY, box.maxY),
            MathHelper.clamp(from.z, box.minZ, box.maxZ)
        );
    }

    private void clearExplosionTarget() {
        this.pendingExplosionPosition = null;
        this.isRespawnAnchorBoost = false;
        this.deferExplosionUpdate = false;
        this.activeExplosionItemSlot = -1;
        this.aimPoint = null;
    }

    private void clearCrystalTracking() {
        this.crystalSearchBounds = null;
        this.pendingCrystalBasePosition = null;
        this.aimPoint = null;
    }

    private void clearAll() {
        this.clearExplosionTarget();
        this.clearCrystalTracking();
        this.restoreSelectedSlotNextTick = false;
        this.previousSelectedSlot = -1;
    }

    @Override
    protected void onDisable() {
        this.clearAll();
    }
}
