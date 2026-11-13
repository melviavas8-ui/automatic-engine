package daamky.ua.utils.render.post.entitybloom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState.LeashData;
import net.minecraft.client.render.entity.state.EntityRenderState.ShadowPiece;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.visual.EntityBloom;

public final class EntityBloomWorldTrail {
   private static final int MAX_STORED_SNAPSHOTS = 28;
   private static final int MAX_RENDERED_GHOSTS = 7;
   private static final long CAPTURE_INTERVAL_NANOS = 38000000L;
   private static final long STALE_HISTORY_NANOS = 2500000000L;
   private static final double MAX_TRAIL_DISTANCE_SQUARED = 2.4025000000000003;
   private static final Map<EntityRenderState, Integer> stateEntityIds = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<Integer, EntityBloomWorldTrail.TrailHistory> histories = new HashMap<>();
   private static ClientWorld trackedLevel;
   private static boolean renderingGhost;
   private static long lastPruneNanos;

   private EntityBloomWorldTrail() {
   }

   public static void capture(LivingEntity entity, LivingEntityRenderState state) {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      EntityBloom bloom = EntityBloom.getInstance();
      if (minecraft.world != null && bloom != null && bloom.shouldRenderTrail(entity)) {
         if (trackedLevel != minecraft.world) {
            clear();
            trackedLevel = minecraft.world;
         }

         long now = System.nanoTime();
         stateEntityIds.put(state, entity.getId());
         EntityBloomWorldTrail.TrailHistory history = histories.computeIfAbsent(entity.getId(), ignored -> new EntityBloomWorldTrail.TrailHistory());
         history.lastSeenNanos = now;
         if (history.lastCaptureNanos == 0L || now - history.lastCaptureNanos >= 38000000L) {
            history.snapshots.addLast(new EntityBloomWorldTrail.Snapshot(state.x, state.y, state.z, state.bodyYaw, state.relativeHeadYaw, state.pitch, now));
            history.lastCaptureNanos = now;
         }

         float duration = bloom.getTrailLength();
         long oldestAllowed = now - (long)(Math.max(0.25F, duration) * 1.0E9F);

         while (!history.snapshots.isEmpty() && (history.snapshots.peekFirst().capturedAt < oldestAllowed || history.snapshots.size() > 28)) {
            history.snapshots.removeFirst();
         }

         if (now - lastPruneNanos > 1000000000L) {
            histories.entrySet().removeIf(entry -> now - entry.getValue().lastSeenNanos > 2500000000L);
            lastPruneNanos = now;
         }
      }
   }

   public static void submitGhosts(
      EntityRenderManager dispatcher,
      EntityRenderState state,
      CameraRenderState cameraState,
      double currentRelativeX,
      double currentRelativeY,
      double currentRelativeZ,
      MatrixStack poseStack,
      OrderedRenderCommandQueue collector
   ) {
      if (!renderingGhost && state instanceof LivingEntityRenderState livingState) {
         EntityBloom bloom = EntityBloom.getInstance();
         Integer entityId = stateEntityIds.get(state);
         EntityBloomWorldTrail.TrailHistory history = entityId != null ? histories.get(entityId) : null;
         if (bloom != null && bloom.isAfterimageTrailEnabled() && history != null && history.snapshots.size() >= 2) {
            long now = System.nanoTime();
            float duration = Math.max(0.25F, bloom.getTrailLength());
            float minimumAgeGap = Math.max(0.045F, duration / 19.0F);
            List<EntityBloomWorldTrail.GhostFrame> frames = selectFrames(history, livingState, now, duration, minimumAgeGap);
            if (!frames.isEmpty()) {
               EntityRenderer renderer = dispatcher.getRenderer(state);
               Vec3d renderOffset = renderer.getPositionOffset(state);
               boolean oldInvisible = state.invisible;
               boolean oldInvisibleToPlayer = livingState.invisibleToPlayer;
               boolean oldFire = state.onFire;
               int oldOutlineColor = state.outlineColor;
               Text oldNameTag = state.displayName;
               List<LeashData> oldLeashes = state.leashDatas;
               float oldShadowRadius = state.shadowRadius;
               float oldBodyRot = livingState.bodyYaw;
               float oldYRot = livingState.relativeHeadYaw;
               float oldXRot = livingState.pitch;
               Text oldScoreText = state instanceof PlayerEntityRenderState avatar ? avatar.playerName : null;
               List<ShadowPiece> oldShadowPieces = new ArrayList<>(state.shadowPieces);

               try {
                  state.invisible = true;
                  livingState.invisibleToPlayer = true;
                  state.onFire = false;
                  state.displayName = null;
                  state.leashDatas = List.of();
                  state.shadowRadius = 0.0F;
                  state.shadowPieces.clear();
                  if (state instanceof PlayerEntityRenderState avatarx) {
                     avatarx.playerName = null;
                  }

                  for (EntityBloomWorldTrail.GhostFrame frame : frames) {
                     EntityBloomWorldTrail.Snapshot snapshot = frame.snapshot;
                     state.outlineColor = bloom.getWorldTrailOutlineColor(frame.opacity);
                     livingState.bodyYaw = snapshot.bodyRot;
                     livingState.relativeHeadYaw = snapshot.yRot;
                     livingState.pitch = snapshot.xRot;
                     poseStack.push();

                     try {
                        poseStack.translate(
                           currentRelativeX + snapshot.x - state.x + renderOffset.getX(),
                           currentRelativeY + snapshot.y - state.y + renderOffset.getY(),
                           currentRelativeZ + snapshot.z - state.z + renderOffset.getZ()
                        );
                        renderingGhost = true;
                        renderer.render(state, poseStack, collector, cameraState);
                     } finally {
                        renderingGhost = false;
                        poseStack.pop();
                     }
                  }
               } finally {
                  state.invisible = oldInvisible;
                  livingState.invisibleToPlayer = oldInvisibleToPlayer;
                  state.onFire = oldFire;
                  state.outlineColor = oldOutlineColor;
                  state.displayName = oldNameTag;
                  state.leashDatas = oldLeashes;
                  state.shadowRadius = oldShadowRadius;
                  livingState.bodyYaw = oldBodyRot;
                  livingState.relativeHeadYaw = oldYRot;
                  livingState.pitch = oldXRot;
                  state.shadowPieces.clear();
                  state.shadowPieces.addAll(oldShadowPieces);
                  if (state instanceof PlayerEntityRenderState avatarx) {
                     avatarx.playerName = oldScoreText;
                  }

                  renderingGhost = false;
               }
            }
         }
      }
   }

   public static boolean isRenderingGhost() {
      return renderingGhost;
   }

   public static void clear() {
      stateEntityIds.clear();
      histories.clear();
      trackedLevel = null;
      lastPruneNanos = 0L;
      renderingGhost = false;
   }

   private static List<EntityBloomWorldTrail.GhostFrame> selectFrames(
      EntityBloomWorldTrail.TrailHistory history, LivingEntityRenderState current, long now, float duration, float minimumAgeGap
   ) {
      List<EntityBloomWorldTrail.GhostFrame> selected = new ArrayList<>(7);
      Iterator<EntityBloomWorldTrail.Snapshot> iterator = history.snapshots.descendingIterator();
      float lastSelectedAge = -100.0F;

      while (iterator.hasNext() && selected.size() < 7) {
         EntityBloomWorldTrail.Snapshot snapshot = iterator.next();
         float age = Math.max(0.0F, (float)(now - snapshot.capturedAt) * 1.0E-9F);
         if (!(age < 0.065F) && !(age >= duration) && !(age - lastSelectedAge < minimumAgeGap)) {
            double dx = snapshot.x - current.x;
            double dy = snapshot.y - current.y;
            double dz = snapshot.z - current.z;
            double movementSq = dx * dx + dy * dy + dz * dz;
            if (!(movementSq > 2.4025000000000003)) {
               float rotationDelta = Math.max(
                  Math.abs(MathHelper.wrapDegrees(snapshot.bodyRot - current.bodyYaw)),
                  Math.abs(MathHelper.wrapDegrees(snapshot.yRot - current.relativeHeadYaw))
               );
               if (!(movementSq < 9.0E-4) || !(rotationDelta < 2.5F)) {
                  float life = 1.0F - age / duration;
                  float opacity = (float)Math.pow((double)Math.clamp(life, 0.0F, 1.0F), 1.32);
                  selected.add(new EntityBloomWorldTrail.GhostFrame(snapshot, opacity));
                  lastSelectedAge = age;
               }
            }
         }
      }

      return selected;
   }

   private static record GhostFrame(EntityBloomWorldTrail.Snapshot snapshot, float opacity) {
   }

   private static record Snapshot(double x, double y, double z, float bodyRot, float yRot, float xRot, long capturedAt) {
   }

   private static final class TrailHistory {
      private final Deque<EntityBloomWorldTrail.Snapshot> snapshots = new ArrayDeque<>();
      private long lastCaptureNanos;
      private long lastSeenNanos;

      private TrailHistory() {
      }
   }
}
