package daamky.ua.utils.world;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.Arrays;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Incremental scanner for BlockESP. Chunks are scanned section by section inside a small time budget per tick,
 * packets only mark positions as dirty (they may arrive off the main thread), and the world is re-read a couple
 * of ticks later on the main thread.
 */
public final class BlockEspScanner {
   private static final int DELAY_TICKS = 2;
   private static final int MAX_RETRIES = 200;
   private static final int MAX_DELAYED = 65536;
   private static final long BUDGET_NORMAL = 400000L;
   private static final long BUDGET_BUSY = 2000000L;

   private static final class ChunkData {
      final Long2ByteOpenHashMap blocks = new Long2ByteOpenHashMap();

      ChunkData() {
         this.blocks.defaultReturnValue((byte)-1);
      }
   }

   private final Long2ObjectOpenHashMap<ChunkData> chunks = new Long2ObjectOpenHashMap<>();
   private final LongLinkedOpenHashSet queue = new LongLinkedOpenHashSet();
   private final Long2IntOpenHashMap retries = new Long2IntOpenHashMap();
   private final Long2IntOpenHashMap delayedBlocks = new Long2IntOpenHashMap();
   private final Long2IntOpenHashMap delayedChunks = new Long2IntOpenHashMap();
   private final Object lock = new Object();
   private final LongArrayList inboxBlocks = new LongArrayList();
   private final LongArrayList inboxChunks = new LongArrayList();
   private final LongArrayList inboxUnload = new LongArrayList();
   private final BlockPos.Mutable mutable = new BlockPos.Mutable();
   private long[] sortKeys = new long[1024];
   private long[] tmpPos = new long[1024];
   private byte[] tmpType = new byte[1024];
   private float[] tmpDist = new float[1024];
   private int tick;
   private int bottomSection = Integer.MIN_VALUE;
   private int lastChunkX = Integer.MIN_VALUE;
   private int lastChunkZ = Integer.MIN_VALUE;
   private int lastRadius = -1;
   private int version;

   public int version() {
      return this.version;
   }

   public void reset() {
      synchronized (this.lock) {
         this.inboxBlocks.clear();
         this.inboxChunks.clear();
         this.inboxUnload.clear();
      }

      this.chunks.clear();
      this.queue.clear();
      this.retries.clear();
      this.delayedBlocks.clear();
      this.delayedChunks.clear();
      this.lastChunkX = Integer.MIN_VALUE;
      this.lastChunkZ = Integer.MIN_VALUE;
      this.lastRadius = -1;
      this.bottomSection = Integer.MIN_VALUE;
      this.version++;
   }

   /** Thread safe. */
   public void markBlock(long pos) {
      synchronized (this.lock) {
         if (this.inboxBlocks.size() < MAX_DELAYED) {
            this.inboxBlocks.add(pos);
         }
      }
   }

   /** Thread safe. */
   public void markChunk(int chunkX, int chunkZ) {
      synchronized (this.lock) {
         this.inboxChunks.add(ChunkPos.toLong(chunkX, chunkZ));
      }
   }

   /** Thread safe. */
   public void markUnload(int chunkX, int chunkZ) {
      synchronized (this.lock) {
         this.inboxUnload.add(ChunkPos.toLong(chunkX, chunkZ));
      }
   }

   public void update(ClientWorld world, int playerChunkX, int playerChunkZ, int radius) {
      this.tick++;
      radius = MathHelper.clamp(radius, 1, 32);
      int bottom = world.getBottomSectionCoord();
      if (bottom != this.bottomSection) {
         this.chunks.clear();
         this.queue.clear();
         this.retries.clear();
         this.delayedBlocks.clear();
         this.delayedChunks.clear();
         this.bottomSection = bottom;
         this.lastChunkX = Integer.MIN_VALUE;
         this.version++;
      }

      this.drainInbox();
      boolean moved = playerChunkX != this.lastChunkX || playerChunkZ != this.lastChunkZ || radius != this.lastRadius;
      this.lastChunkX = playerChunkX;
      this.lastChunkZ = playerChunkZ;
      this.lastRadius = radius;
      if (moved || this.tick % 40 == 0) {
         this.ensureChunks(playerChunkX, playerChunkZ, radius);
      }

      this.processDelayed(world);
      this.processQueue(world);
   }

   private void drainInbox() {
      synchronized (this.lock) {
         for (int i = 0; i < this.inboxBlocks.size(); i++) {
            long pos = this.inboxBlocks.getLong(i);
            if (!this.delayedBlocks.containsKey(pos) && this.delayedBlocks.size() < MAX_DELAYED) {
               this.delayedBlocks.put(pos, this.tick + DELAY_TICKS);
            }
         }

         this.inboxBlocks.clear();

         for (int i = 0; i < this.inboxChunks.size(); i++) {
            this.delayedChunks.put(this.inboxChunks.getLong(i), this.tick + DELAY_TICKS);
         }

         this.inboxChunks.clear();

         for (int i = 0; i < this.inboxUnload.size(); i++) {
            long key = this.inboxUnload.getLong(i);
            ChunkData data = this.chunks.remove(key);
            this.queue.remove(key);
            this.retries.remove(key);
            this.delayedChunks.remove(key);
            if (data != null && !data.blocks.isEmpty()) {
               this.version++;
            }
         }

         this.inboxUnload.clear();
      }
   }

   private void ensureChunks(int pcx, int pcz, int radius) {
      LongArrayList far = null;
      LongIterator iterator = this.chunks.keySet().iterator();
      while (iterator.hasNext()) {
         long key = iterator.nextLong();
         if (Math.abs(ChunkPos.getPackedX(key) - pcx) > radius + 2 || Math.abs(ChunkPos.getPackedZ(key) - pcz) > radius + 2) {
            if (far == null) {
               far = new LongArrayList();
            }

            far.add(key);
         }
      }

      if (far != null) {
         for (int i = 0; i < far.size(); i++) {
            long key = far.getLong(i);
            ChunkData data = this.chunks.remove(key);
            this.queue.remove(key);
            this.retries.remove(key);
            if (data != null && !data.blocks.isEmpty()) {
               this.version++;
            }
         }
      }

      for (int ring = 0; ring <= radius; ring++) {
         for (int dx = -ring; dx <= ring; dx++) {
            for (int dz = -ring; dz <= ring; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                  long key = ChunkPos.toLong(pcx + dx, pcz + dz);
                  if (!this.chunks.containsKey(key) && !this.queue.contains(key)) {
                     this.queue.add(key);
                  }
               }
            }
         }
      }
   }

   private LongArrayList takeDue(Long2IntOpenHashMap map) {
      LongArrayList due = new LongArrayList();
      LongIterator iterator = map.keySet().iterator();
      while (iterator.hasNext()) {
         long key = iterator.nextLong();
         if (map.get(key) <= this.tick) {
            due.add(key);
         }
      }

      for (int i = 0; i < due.size(); i++) {
         map.remove(due.getLong(i));
      }

      return due;
   }

   private void processDelayed(ClientWorld world) {
      if (!this.delayedChunks.isEmpty()) {
         LongArrayList due = this.takeDue(this.delayedChunks);
         for (int i = 0; i < due.size(); i++) {
            this.queue.addAndMoveToFirst(due.getLong(i));
         }
      }

      if (!this.delayedBlocks.isEmpty()) {
         LongArrayList due = this.takeDue(this.delayedBlocks);
         for (int i = 0; i < due.size(); i++) {
            long pos = due.getLong(i);
            int x = BlockPos.unpackLongX(pos);
            int y = BlockPos.unpackLongY(pos);
            int z = BlockPos.unpackLongZ(pos);
            long key = ChunkPos.toLong(x >> 4, z >> 4);
            if (this.queue.contains(key)) {
               continue;
            }

            ChunkData data = this.chunks.get(key);
            if (data == null) {
               continue;
            }

            this.mutable.set(x, y, z);
            int type = BlockEspTypes.typeOf(world.getBlockState(this.mutable));
            int old = data.blocks.get(pos);
            if (type < 0) {
               if (old >= 0) {
                  data.blocks.remove(pos);
                  this.version++;
               }
            } else if (old != type) {
               data.blocks.put(pos, (byte)type);
               this.version++;
            }
         }
      }
   }

   private void processQueue(ClientWorld world) {
      if (this.queue.isEmpty()) {
         return;
      }

      long deadline = System.nanoTime() + (this.queue.size() >= 48 ? BUDGET_BUSY : BUDGET_NORMAL);
      int attempts = this.queue.size();
      while (!this.queue.isEmpty() && attempts-- > 0) {
         long key = this.queue.firstLong();
         int cx = ChunkPos.getPackedX(key);
         int cz = ChunkPos.getPackedZ(key);
         if (Math.abs(cx - this.lastChunkX) > this.lastRadius + 2 || Math.abs(cz - this.lastChunkZ) > this.lastRadius + 2) {
            this.queue.removeFirstLong();
            continue;
         }

         WorldChunk chunk = world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
         this.queue.removeFirstLong();
         if (chunk == null) {
            int tries = this.retries.get(key) + 1;
            if (tries > MAX_RETRIES) {
               this.retries.remove(key);
            } else {
               this.retries.put(key, tries);
               this.queue.add(key);
            }
         } else {
            this.retries.remove(key);
            this.scanChunk(chunk, key, cx, cz);
         }

         if (System.nanoTime() >= deadline) {
            break;
         }
      }
   }

   private void scanChunk(WorldChunk chunk, long key, int cx, int cz) {
      ChunkData old = this.chunks.get(key);
      ChunkData data = new ChunkData();
      ChunkSection[] sections = chunk.getSectionArray();
      int baseX = cx << 4;
      int baseZ = cz << 4;
      for (int i = 0; i < sections.length; i++) {
         ChunkSection section = sections[i];
         if (section == null || !section.hasAny(BlockEspTypes.predicate())) {
            continue;
         }

         int baseY = this.bottomSection + i << 4;
         for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  BlockState state = section.getBlockState(x, y, z);
                  int type = BlockEspTypes.typeOf(state);
                  if (type >= 0) {
                     data.blocks.put(BlockPos.asLong(baseX + x, baseY + y, baseZ + z), (byte)type);
                  }
               }
            }
         }
      }

      this.chunks.put(key, data);
      if (!data.blocks.isEmpty() || old != null && !old.blocks.isEmpty()) {
         this.version++;
      }
   }

   /**
    * Writes the nearest matching blocks inside range into outPos/outType (both at least limit long).
    * typeMask has bit i set for every enabled type.
    */
   public int collect(double px, double py, double pz, double range, int limit, int typeMask, long[] outPos, byte[] outType) {
      double rangeSq = range * range;
      int n = 0;
      for (ChunkData data : this.chunks.values()) {
         LongIterator iterator = data.blocks.keySet().iterator();
         while (iterator.hasNext()) {
            long pos = iterator.nextLong();
            int type = data.blocks.get(pos);
            if ((typeMask >> type & 1) == 0) {
               continue;
            }

            double dx = BlockPos.unpackLongX(pos) + 0.5 - px;
            double dy = BlockPos.unpackLongY(pos) + 0.5 - py;
            double dz = BlockPos.unpackLongZ(pos) + 0.5 - pz;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > rangeSq) {
               continue;
            }

            if (n == this.tmpPos.length) {
               int size = n << 1;
               this.tmpPos = Arrays.copyOf(this.tmpPos, size);
               this.tmpType = Arrays.copyOf(this.tmpType, size);
               this.tmpDist = Arrays.copyOf(this.tmpDist, size);
            }

            this.tmpPos[n] = pos;
            this.tmpType[n] = (byte)type;
            this.tmpDist[n] = (float)distSq;
            n++;
         }
      }

      if (n <= limit) {
         System.arraycopy(this.tmpPos, 0, outPos, 0, n);
         System.arraycopy(this.tmpType, 0, outType, 0, n);
         return n;
      }

      if (this.sortKeys.length < n) {
         this.sortKeys = new long[this.tmpPos.length];
      }

      for (int i = 0; i < n; i++) {
         this.sortKeys[i] = (long)Float.floatToIntBits(this.tmpDist[i]) << 32 | i;
      }

      Arrays.sort(this.sortKeys, 0, n);
      for (int i = 0; i < limit; i++) {
         int index = (int)(this.sortKeys[i] & 0xFFFFFFFFL);
         outPos[i] = this.tmpPos[index];
         outType[i] = this.tmpType[index];
      }

      return limit;
   }
}
