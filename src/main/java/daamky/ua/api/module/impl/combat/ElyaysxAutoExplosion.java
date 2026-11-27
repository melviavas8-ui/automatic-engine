package daamky.ua.api.module.impl.combat;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.types.EventPhase;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
    private AABB crystalSearchBounds;
    private BlockPos pendingCrystalBasePosition;
    private boolean deferExplosionUpdate;

    private Vec3 aimPoint;
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

        if (!(event.getPacket() instanceof ServerboundUseItemOnPacket packet)) {
            return;
        }

        BlockHitResult hit = packet.getHitResult();
        BlockPos clickedPos = hit.getBlockPos();
        Item mainItem = mc.player.getMainHandItem().getItem();

        // Detect placement of Obsidian / Bedrock base or Respawn Anchor
        if (mainItem == Items.OBSIDIAN || mainItem == Items.RESPAWN_ANCHOR) {
            BlockPos placedPos = clickedPos.relative(hit.getDirection());
            if (mainItem == Items.OBSIDIAN) {
                int crystalSlot = findItemSlot(Items.END_CRYSTAL, true);
                if (crystalSlot == -1) {
                    return;
                }
                this.pendingExplosionPosition = placedPos.immutable();
                this.activeExplosionItemSlot = crystalSlot;
                this.isRespawnAnchorBoost = false;
            } else {
                int glowstoneSlot = findItemSlot(Items.GLOWSTONE, true);
                if (glowstoneSlot == -1) {
                    return;
                }
                this.pendingExplosionPosition = placedPos.immutable();
                this.activeExplosionItemSlot = glowstoneSlot;
                this.isRespawnAnchorBoost = true;
            }
            this.deferExplosionUpdate = true;
            this.previousSelectedSlot = -1;
            return;
        }

        // Track right-click on existing Obsidian/Bedrock for crystal placement detection
        if (mc.level != null) {
            Block block = mc.level.getBlockState(clickedPos).getBlock();
            if ((block == Blocks.OBSIDIAN || block == Blocks.BEDROCK)
                && !mc.player.getCooldowns().isOnCooldown(Items.END_CRYSTAL.getDefaultInstance())) {
                this.pendingCrystalBasePosition = clickedPos.immutable();
                this.crystalSearchBounds = new AABB(clickedPos.above()).inflate(0.1);
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
        mc.player.setYRot(this.targetYaw);
        mc.player.setXRot(Mth.clamp(this.targetPitch, -90.0f, 90.0f));
    }

    @SubscribeEvent
    private void onTick(TickEvent.Post event) {
        if (!this.isEnabled()) {
            return;
        }
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            this.clearAll();
            return;
        }

        // Restore hotbar slot after interaction
        if (this.restoreSelectedSlotNextTick) {
            this.restoreSelectedSlotNextTick = false;
            if (this.previousSelectedSlot != -1) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(this.previousSelectedSlot));
                mc.player.getInventory().setSelectedSlot(this.previousSelectedSlot);
            }
            this.previousSelectedSlot = -1;
            return;
        }

        // Place crystal / charge anchor
        if (this.pendingExplosionPosition != null) {
            if (mc.level.getBlockState(this.pendingExplosionPosition).isAir()) {
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

        Vec3 eye = mc.player.getEyePosition();
        Vec3 closest = closestPointOnBox(eye, new AABB(this.pendingExplosionPosition));
        this.setAimTarget(closest);

        this.previousSelectedSlot = mc.player.getInventory().getSelectedSlot();
        if (this.previousSelectedSlot != this.activeExplosionItemSlot) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(this.activeExplosionItemSlot));
            mc.player.getInventory().setSelectedSlot(this.activeExplosionItemSlot);
        }

        Vec3 lookDir = closest.subtract(eye).reverse();
        Direction facing = Direction.getApproximateNearest(lookDir.x, lookDir.y, lookDir.z);

        mc.gameMode.useItemOn(
            mc.player,
            InteractionHand.MAIN_HAND,
            new BlockHitResult(closest, facing, this.pendingExplosionPosition, false)
        );
        mc.player.swing(InteractionHand.MAIN_HAND);

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
            this.pendingCrystalBasePosition = this.pendingExplosionPosition.immutable();
            this.crystalSearchBounds = new AABB(this.pendingExplosionPosition.above()).inflate(0.15);
            this.clearExplosionTarget();
        }
    }

    private void attackNearbyCrystals() {
        if (this.crystalSearchBounds == null) {
            return;
        }

        if (this.pendingCrystalBasePosition != null
            && mc.level.getBlockState(this.pendingCrystalBasePosition).isAir()) {
            this.clearCrystalTracking();
            return;
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) {
                continue;
            }
            if (!this.crystalSearchBounds.contains(crystal.getX(), crystal.getY(), crystal.getZ())) {
                continue;
            }

            BlockPos crystalBase = BlockPos.containing(crystal.getX(), crystal.getY() - 1.0, crystal.getZ());

            if (this.avoidSelfDamage.getValue()
                && Math.abs(mc.player.getY() - crystal.getY()) < 1.0) {
                this.clearCrystalTracking();
                return;
            }

            if (this.containsValuableDrops(crystal)) {
                this.clearCrystalTracking();
                return;
            }

            Vec3 eye = mc.player.getEyePosition();
            if (!crystal.getBoundingBox().contains(eye)) {
                Vec3 aim = closestPointOnBox(eye, crystal.getBoundingBox());
                this.setAimTarget(aim);
            }

            mc.gameMode.attack(mc.player, crystal);
            mc.player.swing(InteractionHand.MAIN_HAND);
            this.clearCrystalTracking();
            return;
        }
    }

    private boolean containsValuableDrops(EndCrystal crystal) {
        if (!this.preserveValuableDrops.getValue() || mc.level == null) {
            return false;
        }

        Vec3 pos = new Vec3(crystal.getX(), crystal.getY(), crystal.getZ());
        AABB search = new AABB(pos, pos).inflate(6.0);

        for (ItemEntity itemEntity : mc.level.getEntitiesOfClass(ItemEntity.class, search, Entity::isAlive)) {
            if (isValuableItem(itemEntity.getItem().getItem())) {
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
        String key = item.getDescriptionId();
        return key != null && key.contains("netherite");
    }

    private int findItemSlot(Item target, boolean hotbarOnly) {
        int limit = hotbarOnly ? 9 : 36;
        int found = -1;
        for (int i = 0; i < limit; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == target) {
                found = i;
            }
        }
        return found;
    }

    private void setAimTarget(Vec3 point) {
        if (mc.player == null) {
            return;
        }
        this.aimPoint = point;
        Vec3 diff = point.subtract(mc.player.getEyePosition());
        this.targetYaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        this.targetPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))));
    }

    private static Vec3 closestPointOnBox(Vec3 from, AABB box) {
        return new Vec3(
            Mth.clamp(from.x, box.minX, box.maxX),
            Mth.clamp(from.y, box.minY, box.maxY),
            Mth.clamp(from.z, box.minZ, box.maxZ)
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
