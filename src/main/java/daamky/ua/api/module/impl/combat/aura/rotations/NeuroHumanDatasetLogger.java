package daamky.ua.api.module.impl.combat.aura.rotations;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.MinecraftClient;

public final class NeuroHumanDatasetLogger {
   private static final NeuroHumanDatasetLogger INSTANCE = new NeuroHumanDatasetLogger();
   private static final int FLUSH_INTERVAL = 64;
   private static final DateTimeFormatter SESSION_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
   private static final String CSV_HEADER = String.join(
      ",",
      "timestamp_ms",
      "session_elapsed_ms",
      "session_clock",
      "target_id",
      "target_uuid",
      "target_type",
      "current_yaw",
      "current_pitch",
      "after_yaw",
      "after_pitch",
      "target_yaw",
      "target_pitch",
      "yaw_delta",
      "pitch_delta",
      "delta_length",
      "distance",
      "distance_ratio",
      "target_motion",
      "target_speed",
      "mouse_dx",
      "mouse_dy",
      "applied_yaw_step",
      "applied_pitch_step",
      "multi_point_enabled",
      "aim_offset_x",
      "aim_offset_y",
      "aim_offset_z",
      "aim_offset_length",
      "can_attack_now",
      "can_attack_soon",
      "raytrace_success",
      "attack_cooldown",
      "hit_chance",
      "attack_key_down",
      "use_key_down",
      "pick_key_down",
      "forward_key_down",
      "back_key_down",
      "left_key_down",
      "right_key_down",
      "jump_key_down",
      "sneak_key_down",
      "sprint_key_down",
      "mouse_left_down",
      "mouse_right_down",
      "mouse_middle_down",
      "input_mask",
      "input_changed",
      "target_hurt",
      "target_hurt_time",
      "registered_hit"
   );
   private NeuroHumanDatasetLogger.WriterState csvState;
   private NeuroHumanDatasetLogger.WriterState jsonlState;
   private String sessionId;
   private boolean failed;
   private long writtenSamples;

   private NeuroHumanDatasetLogger() {
   }

   public static NeuroHumanDatasetLogger getInstance() {
      return INSTANCE;
   }

   public synchronized long logSample(NeuroHumanDatasetLogger.Sample sample, String formatName) {
      if (sample == null) {
         return this.writtenSamples;
      } else {
         NeuroHumanDatasetLogger.Format format = NeuroHumanDatasetLogger.Format.fromSetting(formatName);
         NeuroHumanDatasetLogger.WriterState state = this.getState(format);
         if (state == null) {
            return this.writtenSamples;
         } else {
            try {
               state.writer.write(format == NeuroHumanDatasetLogger.Format.CSV ? sample.toCsv() : sample.toJson());
               state.writer.newLine();
               state.samplesSinceFlush++;
               this.writtenSamples++;
               if (state.samplesSinceFlush >= 64) {
                  state.writer.flush();
                  state.samplesSinceFlush = 0;
               }
            } catch (IOException var6) {
               this.handleFailure(var6);
            }

            return this.writtenSamples;
         }
      }
   }

   public synchronized long getWrittenSamples() {
      return this.writtenSamples;
   }

   public synchronized void flush() {
      this.flushWriter(this.csvState);
      this.flushWriter(this.jsonlState);
   }

   private NeuroHumanDatasetLogger.WriterState getState(NeuroHumanDatasetLogger.Format format) {
      NeuroHumanDatasetLogger.WriterState current = format == NeuroHumanDatasetLogger.Format.CSV ? this.csvState : this.jsonlState;
      if (current != null) {
         return current;
      } else {
         try {
            Path directory = this.resolveDatasetDirectory();
            Files.createDirectories(directory);
            if (this.sessionId == null) {
               this.sessionId = LocalDateTime.now().format(SESSION_FORMAT);
            }

            Path output = directory.resolve("neuro_human_dataset_" + this.sessionId + "." + format.extension);
            boolean writeHeader = format == NeuroHumanDatasetLogger.Format.CSV && (!Files.exists(output) || Files.size(output) == 0L);
            BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (writeHeader) {
               writer.write(CSV_HEADER);
               writer.newLine();
               writer.flush();
            }

            NeuroHumanDatasetLogger.WriterState created = new NeuroHumanDatasetLogger.WriterState(writer);
            if (format == NeuroHumanDatasetLogger.Format.CSV) {
               this.csvState = created;
            } else {
               this.jsonlState = created;
            }

            return created;
         } catch (IOException var8) {
            this.handleFailure(var8);
            return null;
         }
      }
   }

   private Path resolveDatasetDirectory() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client != null && client.runDirectory != null
         ? client.runDirectory.toPath().resolve("daamky").resolve("datasets").resolve("neuro_human")
         : Path.of("daamky", "datasets", "neuro_human");
   }

   private void handleFailure(IOException exception) {
      if (!this.failed) {
         this.failed = true;
         System.err.println("Failed to write Neuro human dataset: " + exception.getMessage());
         exception.printStackTrace();
      }
   }

   private void flushWriter(NeuroHumanDatasetLogger.WriterState state) {
      if (state != null) {
         try {
            state.writer.flush();
            state.samplesSinceFlush = 0;
         } catch (IOException var3) {
            this.handleFailure(var3);
         }
      }
   }

   private static enum Format {
      JSONL("jsonl"),
      CSV("csv");

      private final String extension;

      private Format(String extension) {
         this.extension = extension;
      }

      private static NeuroHumanDatasetLogger.Format fromSetting(String value) {
         return "CSV".equalsIgnoreCase(value) ? CSV : JSONL;
      }
   }

   public static final class Sample {
      private final long timestampMs;
      private final long sessionElapsedMs;
      private final String sessionClock;
      private final int targetId;
      private final String targetUuid;
      private final String targetType;
      private final double currentYaw;
      private final double currentPitch;
      private final double afterYaw;
      private final double afterPitch;
      private final double targetYaw;
      private final double targetPitch;
      private final double yawDelta;
      private final double pitchDelta;
      private final double deltaLength;
      private final double distance;
      private final double distanceRatio;
      private final double targetMotion;
      private final double targetSpeed;
      private final double mouseDx;
      private final double mouseDy;
      private final double appliedYawStep;
      private final double appliedPitchStep;
      private final double multiPointEnabled;
      private final double aimOffsetX;
      private final double aimOffsetY;
      private final double aimOffsetZ;
      private final double aimOffsetLength;
      private final boolean canAttackNow;
      private final boolean canAttackSoon;
      private final boolean raytraceSuccess;
      private final double attackCooldown;
      private final double hitChance;
      private final boolean attackKeyDown;
      private final boolean useKeyDown;
      private final boolean pickKeyDown;
      private final boolean forwardKeyDown;
      private final boolean backKeyDown;
      private final boolean leftKeyDown;
      private final boolean rightKeyDown;
      private final boolean jumpKeyDown;
      private final boolean sneakKeyDown;
      private final boolean sprintKeyDown;
      private final boolean mouseLeftDown;
      private final boolean mouseRightDown;
      private final boolean mouseMiddleDown;
      private final int inputMask;
      private final boolean inputChanged;
      private final boolean targetHurt;
      private final int targetHurtTime;
      private final boolean registeredHit;

      public Sample(
         long timestampMs,
         long sessionElapsedMs,
         String sessionClock,
         int targetId,
         String targetUuid,
         String targetType,
         double currentYaw,
         double currentPitch,
         double afterYaw,
         double afterPitch,
         double targetYaw,
         double targetPitch,
         double yawDelta,
         double pitchDelta,
         double deltaLength,
         double distance,
         double distanceRatio,
         double targetMotion,
         double targetSpeed,
         double mouseDx,
         double mouseDy,
         double appliedYawStep,
         double appliedPitchStep,
         double multiPointEnabled,
         double aimOffsetX,
         double aimOffsetY,
         double aimOffsetZ,
         double aimOffsetLength,
         boolean canAttackNow,
         boolean canAttackSoon,
         boolean raytraceSuccess,
         double attackCooldown,
         double hitChance,
         boolean attackKeyDown,
         boolean useKeyDown,
         boolean pickKeyDown,
         boolean forwardKeyDown,
         boolean backKeyDown,
         boolean leftKeyDown,
         boolean rightKeyDown,
         boolean jumpKeyDown,
         boolean sneakKeyDown,
         boolean sprintKeyDown,
         boolean mouseLeftDown,
         boolean mouseRightDown,
         boolean mouseMiddleDown,
         int inputMask,
         boolean inputChanged,
         boolean targetHurt,
         int targetHurtTime,
         boolean registeredHit
      ) {
         this.timestampMs = timestampMs;
         this.sessionElapsedMs = sessionElapsedMs;
         this.sessionClock = sessionClock;
         this.targetId = targetId;
         this.targetUuid = targetUuid;
         this.targetType = targetType;
         this.currentYaw = currentYaw;
         this.currentPitch = currentPitch;
         this.afterYaw = afterYaw;
         this.afterPitch = afterPitch;
         this.targetYaw = targetYaw;
         this.targetPitch = targetPitch;
         this.yawDelta = yawDelta;
         this.pitchDelta = pitchDelta;
         this.deltaLength = deltaLength;
         this.distance = distance;
         this.distanceRatio = distanceRatio;
         this.targetMotion = targetMotion;
         this.targetSpeed = targetSpeed;
         this.mouseDx = mouseDx;
         this.mouseDy = mouseDy;
         this.appliedYawStep = appliedYawStep;
         this.appliedPitchStep = appliedPitchStep;
         this.multiPointEnabled = multiPointEnabled;
         this.aimOffsetX = aimOffsetX;
         this.aimOffsetY = aimOffsetY;
         this.aimOffsetZ = aimOffsetZ;
         this.aimOffsetLength = aimOffsetLength;
         this.canAttackNow = canAttackNow;
         this.canAttackSoon = canAttackSoon;
         this.raytraceSuccess = raytraceSuccess;
         this.attackCooldown = attackCooldown;
         this.hitChance = hitChance;
         this.attackKeyDown = attackKeyDown;
         this.useKeyDown = useKeyDown;
         this.pickKeyDown = pickKeyDown;
         this.forwardKeyDown = forwardKeyDown;
         this.backKeyDown = backKeyDown;
         this.leftKeyDown = leftKeyDown;
         this.rightKeyDown = rightKeyDown;
         this.jumpKeyDown = jumpKeyDown;
         this.sneakKeyDown = sneakKeyDown;
         this.sprintKeyDown = sprintKeyDown;
         this.mouseLeftDown = mouseLeftDown;
         this.mouseRightDown = mouseRightDown;
         this.mouseMiddleDown = mouseMiddleDown;
         this.inputMask = inputMask;
         this.inputChanged = inputChanged;
         this.targetHurt = targetHurt;
         this.targetHurtTime = targetHurtTime;
         this.registeredHit = registeredHit;
      }

      private String toCsv() {
         StringBuilder builder = new StringBuilder(256);
         this.appendCsv(builder, this.timestampMs);
         this.appendCsv(builder, this.sessionElapsedMs);
         this.appendCsv(builder, this.sessionClock);
         this.appendCsv(builder, this.targetId);
         this.appendCsv(builder, this.targetUuid);
         this.appendCsv(builder, this.targetType);
         this.appendCsv(builder, this.currentYaw);
         this.appendCsv(builder, this.currentPitch);
         this.appendCsv(builder, this.afterYaw);
         this.appendCsv(builder, this.afterPitch);
         this.appendCsv(builder, this.targetYaw);
         this.appendCsv(builder, this.targetPitch);
         this.appendCsv(builder, this.yawDelta);
         this.appendCsv(builder, this.pitchDelta);
         this.appendCsv(builder, this.deltaLength);
         this.appendCsv(builder, this.distance);
         this.appendCsv(builder, this.distanceRatio);
         this.appendCsv(builder, this.targetMotion);
         this.appendCsv(builder, this.targetSpeed);
         this.appendCsv(builder, this.mouseDx);
         this.appendCsv(builder, this.mouseDy);
         this.appendCsv(builder, this.appliedYawStep);
         this.appendCsv(builder, this.appliedPitchStep);
         this.appendCsv(builder, this.multiPointEnabled);
         this.appendCsv(builder, this.aimOffsetX);
         this.appendCsv(builder, this.aimOffsetY);
         this.appendCsv(builder, this.aimOffsetZ);
         this.appendCsv(builder, this.aimOffsetLength);
         this.appendCsv(builder, this.canAttackNow ? 1 : 0);
         this.appendCsv(builder, this.canAttackSoon ? 1 : 0);
         this.appendCsv(builder, this.raytraceSuccess ? 1 : 0);
         this.appendCsv(builder, this.attackCooldown);
         this.appendCsv(builder, this.hitChance);
         this.appendCsv(builder, this.attackKeyDown ? 1 : 0);
         this.appendCsv(builder, this.useKeyDown ? 1 : 0);
         this.appendCsv(builder, this.pickKeyDown ? 1 : 0);
         this.appendCsv(builder, this.forwardKeyDown ? 1 : 0);
         this.appendCsv(builder, this.backKeyDown ? 1 : 0);
         this.appendCsv(builder, this.leftKeyDown ? 1 : 0);
         this.appendCsv(builder, this.rightKeyDown ? 1 : 0);
         this.appendCsv(builder, this.jumpKeyDown ? 1 : 0);
         this.appendCsv(builder, this.sneakKeyDown ? 1 : 0);
         this.appendCsv(builder, this.sprintKeyDown ? 1 : 0);
         this.appendCsv(builder, this.mouseLeftDown ? 1 : 0);
         this.appendCsv(builder, this.mouseRightDown ? 1 : 0);
         this.appendCsv(builder, this.mouseMiddleDown ? 1 : 0);
         this.appendCsv(builder, this.inputMask);
         this.appendCsv(builder, this.inputChanged ? 1 : 0);
         this.appendCsv(builder, this.targetHurt ? 1 : 0);
         this.appendCsv(builder, this.targetHurtTime);
         this.appendCsv(builder, this.registeredHit ? 1 : 0);
         return builder.toString();
      }

      private String toJson() {
         StringBuilder builder = new StringBuilder(384);
         builder.append('{');
         this.appendJson(builder, "timestampMs", this.timestampMs);
         this.appendJson(builder, "sessionElapsedMs", this.sessionElapsedMs);
         this.appendJson(builder, "sessionClock", this.sessionClock);
         this.appendJson(builder, "targetId", this.targetId);
         this.appendJson(builder, "targetUuid", this.targetUuid);
         this.appendJson(builder, "targetType", this.targetType);
         this.appendJson(builder, "currentYaw", this.currentYaw);
         this.appendJson(builder, "currentPitch", this.currentPitch);
         this.appendJson(builder, "afterYaw", this.afterYaw);
         this.appendJson(builder, "afterPitch", this.afterPitch);
         this.appendJson(builder, "targetYaw", this.targetYaw);
         this.appendJson(builder, "targetPitch", this.targetPitch);
         this.appendJson(builder, "yawDelta", this.yawDelta);
         this.appendJson(builder, "pitchDelta", this.pitchDelta);
         this.appendJson(builder, "deltaLength", this.deltaLength);
         this.appendJson(builder, "distance", this.distance);
         this.appendJson(builder, "distanceRatio", this.distanceRatio);
         this.appendJson(builder, "targetMotion", this.targetMotion);
         this.appendJson(builder, "targetSpeed", this.targetSpeed);
         this.appendJson(builder, "mouseDx", this.mouseDx);
         this.appendJson(builder, "mouseDy", this.mouseDy);
         this.appendJson(builder, "appliedYawStep", this.appliedYawStep);
         this.appendJson(builder, "appliedPitchStep", this.appliedPitchStep);
         this.appendJson(builder, "multiPointEnabled", this.multiPointEnabled);
         this.appendJson(builder, "aimOffsetX", this.aimOffsetX);
         this.appendJson(builder, "aimOffsetY", this.aimOffsetY);
         this.appendJson(builder, "aimOffsetZ", this.aimOffsetZ);
         this.appendJson(builder, "aimOffsetLength", this.aimOffsetLength);
         this.appendJson(builder, "canAttackNow", this.canAttackNow);
         this.appendJson(builder, "canAttackSoon", this.canAttackSoon);
         this.appendJson(builder, "raytraceSuccess", this.raytraceSuccess);
         this.appendJson(builder, "attackCooldown", this.attackCooldown);
         this.appendJson(builder, "hitChance", this.hitChance);
         this.appendJson(builder, "attackKeyDown", this.attackKeyDown);
         this.appendJson(builder, "useKeyDown", this.useKeyDown);
         this.appendJson(builder, "pickKeyDown", this.pickKeyDown);
         this.appendJson(builder, "forwardKeyDown", this.forwardKeyDown);
         this.appendJson(builder, "backKeyDown", this.backKeyDown);
         this.appendJson(builder, "leftKeyDown", this.leftKeyDown);
         this.appendJson(builder, "rightKeyDown", this.rightKeyDown);
         this.appendJson(builder, "jumpKeyDown", this.jumpKeyDown);
         this.appendJson(builder, "sneakKeyDown", this.sneakKeyDown);
         this.appendJson(builder, "sprintKeyDown", this.sprintKeyDown);
         this.appendJson(builder, "mouseLeftDown", this.mouseLeftDown);
         this.appendJson(builder, "mouseRightDown", this.mouseRightDown);
         this.appendJson(builder, "mouseMiddleDown", this.mouseMiddleDown);
         this.appendJson(builder, "inputMask", this.inputMask);
         this.appendJson(builder, "inputChanged", this.inputChanged);
         this.appendJson(builder, "targetHurt", this.targetHurt);
         this.appendJson(builder, "targetHurtTime", this.targetHurtTime);
         this.appendJson(builder, "registeredHit", this.registeredHit);
         builder.append('}');
         return builder.toString();
      }

      private void appendCsv(StringBuilder builder, String value) {
         if (!builder.isEmpty()) {
            builder.append(',');
         }

         builder.append('"').append(this.escape(value)).append('"');
      }

      private void appendCsv(StringBuilder builder, long value) {
         if (!builder.isEmpty()) {
            builder.append(',');
         }

         builder.append(value);
      }

      private void appendCsv(StringBuilder builder, int value) {
         if (!builder.isEmpty()) {
            builder.append(',');
         }

         builder.append(value);
      }

      private void appendCsv(StringBuilder builder, double value) {
         if (!builder.isEmpty()) {
            builder.append(',');
         }

         builder.append(value);
      }

      private void appendJson(StringBuilder builder, String key, String value) {
         this.appendJsonKey(builder, key);
         builder.append('"').append(this.escape(value)).append('"');
      }

      private void appendJson(StringBuilder builder, String key, long value) {
         this.appendJsonKey(builder, key);
         builder.append(value);
      }

      private void appendJson(StringBuilder builder, String key, int value) {
         this.appendJsonKey(builder, key);
         builder.append(value);
      }

      private void appendJson(StringBuilder builder, String key, double value) {
         this.appendJsonKey(builder, key);
         builder.append(value);
      }

      private void appendJson(StringBuilder builder, String key, boolean value) {
         this.appendJsonKey(builder, key);
         builder.append(value);
      }

      private void appendJsonKey(StringBuilder builder, String key) {
         if (builder.length() > 1) {
            builder.append(',');
         }

         builder.append('"').append(key).append('"').append(':');
      }

      private String escape(String value) {
         return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
      }
   }

   private static final class WriterState {
      private final BufferedWriter writer;
      private int samplesSinceFlush;

      private WriterState(BufferedWriter writer) {
         this.writer = writer;
      }
   }
}
