package daamky.ua.api.module.impl.visual;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.world.BlockEspMesh;
import daamky.ua.utils.world.BlockEspScanner;
import daamky.ua.utils.world.BlockEspTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Highlights chests, spawners, ores and similar blocks. Uses only the standard Render3D pipelines. */
public final class BlockESP extends Module {
   private static final int MAX_BOXES = 6000;
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Normal scans the whole view distance, Performance is limited.", "Normal", "Normal", "Performance"));
   private final NumberSetting radius = this.register(new NumberSetting("Radius", "Highlight radius in blocks.", 48.0, 16.0, 128.0, 8.0));
   private final NumberSetting boxLimit = this.register(new NumberSetting("Box limit", "Maximum number of highlighted blocks.", 512.0, 128.0, 2048.0, 64.0));
   private final BooleanSetting throughWalls = this.register(new BooleanSetting("Through walls", "Show blocks behind walls.", true));
   private final MultiModeSetting blocks = this.register(new MultiModeSetting("Blocks", "Blocks to highlight.", BlockEspTypes.NAMES, BlockEspTypes.NAMES));
   private final BlockEspScanner scanner = new BlockEspScanner();
   private final long[] foundPos = new long[MAX_BOXES];
   private final byte[] foundType = new byte[MAX_BOXES];
   private final List<Render3D.Quad> quads = new ArrayList<>();
   private final List<Render3D.Line> lines = new ArrayList<>();
   private ClientWorld lastWorld;
   private int lastVersion = -1;
   private int lastMask = -1;
   private int lastLimit = -1;
   private float lastRange = -1.0F;
   private long lastBuild;
   private Vec3d lastBuildPos = Vec3d.ZERO;

   public BlockESP() {
      super("Block ESP", "Highlights chests, spawners and ores.", ModuleCategory.VISUAL);
      this.radius.setVisible(() -> this.mode.is("Performance"));
      this.boxLimit.setVisible(() -> this.mode.is("Performance"));
   }

   @Override
   protected void onEnable() {
      this.scanner.reset();
      this.clearCache();
      this.lastWorld = null;
   }

   @Override
   protected void onDisable() {
      this.scanner.reset();
      this.clearCache();
      this.lastWorld = null;
   }

   private void clearCache() {
      this.quads.clear();
      this.lines.clear();
      this.lastVersion = -1;
   }

   private boolean performance() {
      return this.mode.is("Performance");
   }

   private int selectedMask() {
      int mask = 0;
      for (int i = 0; i < BlockEspTypes.COUNT; i++) {
         if (this.blocks.isSelected(BlockEspTypes.NAMES[i])) {
            mask |= 1 << i;
         }
      }

      return mask;
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (!this.isEnabled()) {
         return;
      }

      if (client == null || client.world == null || client.player == null) {
         if (this.lastWorld != null) {
            this.scanner.reset();
            this.clearCache();
            this.lastWorld = null;
         }

         return;
      }

      if (client.world != this.lastWorld) {
         this.scanner.reset();
         this.clearCache();
         this.lastWorld = client.world;
      }

      int view = client.options.getClampedViewDistance();
      int chunkRadius = this.performance() ? Math.min(view, 6) : view;
      this.scanner.update(client.world, client.player.getChunkPos().x, client.player.getChunkPos().z, chunkRadius);
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (!this.isEnabled()) {
         return;
      }

      Packet<?> packet = event.getPacket();
      if (event.isReceive()) {
         if (packet instanceof BlockUpdateS2CPacket update) {
            this.scanner.markBlock(update.getPos().asLong());
         } else if (packet instanceof BlockEntityUpdateS2CPacket update) {
            this.scanner.markBlock(update.getPos().asLong());
         } else if (packet instanceof ChunkDeltaUpdateS2CPacket delta) {
            delta.visitUpdates((pos, state) -> this.scanner.markBlock(pos.asLong()));
         } else if (packet instanceof ChunkDataS2CPacket data) {
            this.scanner.markChunk(data.getChunkX(), data.getChunkZ());
         } else if (packet instanceof UnloadChunkS2CPacket unload) {
            this.scanner.markUnload(unload.pos().x, unload.pos().z);
         }
      } else if (packet instanceof PlayerActionC2SPacket action) {
         Action type = action.getAction();
         if (type == Action.START_DESTROY_BLOCK || type == Action.STOP_DESTROY_BLOCK) {
            this.scanner.markBlock(action.getPos().asLong());
         }
      } else if (packet instanceof PlayerInteractBlockC2SPacket interact) {
         BlockHitResult hit = interact.getBlockHitResult();
         BlockPos pos = hit.getBlockPos();
         this.scanner.markBlock(pos.asLong());
         this.scanner.markBlock(pos.offset(hit.getSide()).asLong());
      }
   }

   @SubscribeEvent
   private void onRender3D(WorldRenderEvent event) {
      if (!this.isEnabled() || mc.world == null || mc.player == null || mc.world != this.lastWorld) {
         return;
      }

      boolean performance = this.performance();
      int view = mc.options.getClampedViewDistance();
      float range = performance ? this.radius.getFloat() : view * 16.0F * 1.5F + 48.0F;
      int limit = performance ? Math.min(MAX_BOXES, (int)this.boxLimit.getFloat()) : MAX_BOXES;
      int mask = this.selectedMask();
      Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
      long now = System.currentTimeMillis();
      boolean rebuild = this.scanner.version() != this.lastVersion
         || mask != this.lastMask
         || limit != this.lastLimit
         || range != this.lastRange
         || camera.squaredDistanceTo(this.lastBuildPos) > 9.0
         || now - this.lastBuild > 1000L;
      if (rebuild) {
         this.lastVersion = this.scanner.version();
         this.lastMask = mask;
         this.lastLimit = limit;
         this.lastRange = range;
         this.lastBuild = now;
         this.lastBuildPos = camera;
         this.quads.clear();
         this.lines.clear();
         int count = this.scanner.collect(camera.x, camera.y, camera.z, range, limit, mask, this.foundPos, this.foundType);
         BlockEspMesh.build(this.foundPos, this.foundType, count, camera, range * 0.88F, range, this.quads, this.lines);
      }

      if (this.throughWalls.getValue()) {
         Render3D.QUAD.addAll(this.quads);
         Render3D.LINE.addAll(this.lines);
      } else {
         Render3D.QUAD_DEPTH.addAll(this.quads);
         Render3D.LINE_DEPTH.addAll(this.lines);
      }
   }
}