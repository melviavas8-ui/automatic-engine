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

final class NeuroDatasetLogger {
   private static final NeuroDatasetLogger INSTANCE = new NeuroDatasetLogger();
   private static final int FLUSH_INTERVAL = 64;
   private static final DateTimeFormatter SESSION_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
   private static final String CSV_HEADER = String.join(
      ",",
      "decision_id",
      "timestamp_ms",
      "target_id",
      "target_uuid",
      "target_type",
      "pattern",
      "current_yaw",
      "current_pitch",
      "target_yaw",
      "target_pitch",
      "yaw_delta",
      "pitch_delta",
      "delta_length",
      "distance",
      "distance_ratio",
      "target_motion",
      "target_speed",
      "yaw_velocity",
      "pitch_velocity",
      "attack_window",
      "tick_scale",
      "smoothness",
      "warmup",
      "angle_response",
      "sample_shift",
      "previous_yaw_step",
      "previous_pitch_step",
      "prev_yaw_step_1",
      "prev_yaw_step_2",
      "prev_yaw_step_3",
      "prev_yaw_step_4",
      "prev_pitch_step_1",
      "prev_pitch_step_2",
      "prev_pitch_step_3",
      "prev_pitch_step_4",
      "multi_point_enabled",
      "aim_offset_x",
      "aim_offset_y",
      "aim_offset_z",
      "aim_offset_length",
      "raw_yaw_step",
      "raw_pitch_step",
      "applied_yaw_step",
      "applied_pitch_step",
      "raytrace_success",
      "hit_success",
      "decision_to_attack_ticks",
      "time_to_hit_ticks",
      "jerk_score",
      "sign_flip_score",
      "clamp_gap_score",
      "suspicion_score",
      "reward"
   );
   private String sessionId;
   private NeuroDatasetLogger.WriterState csvState;
   private NeuroDatasetLogger.WriterState jsonlState;
   private boolean failed;

   private NeuroDatasetLogger() {
   }

   static NeuroDatasetLogger getInstance() {
      return INSTANCE;
   }

   synchronized void logSample(NeuroDatasetLogger.Sample sample, String formatName) {
      if (sample != null) {
         NeuroDatasetLogger.Format format = NeuroDatasetLogger.Format.fromSetting(formatName);
         NeuroDatasetLogger.WriterState state = this.getState(format);
         if (state != null) {
            try {
               state.writer.write(format == NeuroDatasetLogger.Format.CSV ? sample.toCsv() : sample.toJson());
               state.writer.newLine();
               state.samplesSinceFlush++;
               if (state.samplesSinceFlush >= 64) {
                  state.writer.flush();
                  state.samplesSinceFlush = 0;
               }
            } catch (IOException var6) {
               this.handleFailure(var6);
            }
         }
      }
   }

   synchronized void flush() {
      this.flushWriter(this.csvState);
      this.flushWriter(this.jsonlState);
   }

   private NeuroDatasetLogger.WriterState getState(NeuroDatasetLogger.Format format) {
      NeuroDatasetLogger.WriterState current = format == NeuroDatasetLogger.Format.CSV ? this.csvState : this.jsonlState;
      if (current != null) {
         return current;
      } else {
         try {
            Path directory = this.resolveDatasetDirectory();
            Files.createDirectories(directory);
            if (this.sessionId == null) {
               this.sessionId = LocalDateTime.now().format(SESSION_FORMAT);
            }

            Path output = directory.resolve("neuro_dataset_" + this.sessionId + "." + format.extension);
            boolean writeHeader = format == NeuroDatasetLogger.Format.CSV && (!Files.exists(output) || Files.size(output) == 0L);
            BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (writeHeader) {
               writer.write(CSV_HEADER);
               writer.newLine();
               writer.flush();
            }

            NeuroDatasetLogger.WriterState created = new NeuroDatasetLogger.WriterState(output, writer);
            if (format == NeuroDatasetLogger.Format.CSV) {
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
         ? client.runDirectory.toPath().resolve("daamky").resolve("datasets").resolve("neuro")
         : Path.of("daamky", "datasets", "neuro");
   }

   private void handleFailure(IOException exception) {
      if (!this.failed) {
         this.failed = true;
         System.err.println("Failed to write Neuro dataset: " + exception.getMessage());
         exception.printStackTrace();
      }
   }

   private void flushWriter(NeuroDatasetLogger.WriterState state) {
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

      private static NeuroDatasetLogger.Format fromSetting(String value) {
         return "CSV".equalsIgnoreCase(value) ? CSV : JSONL;
      }
   }

   static final class Sample {
      private final long decisionId;
      private final long timestampMs;
      private final int targetId;
      private final String targetUuid;
      private final String targetType;
      private final String pattern;
      private final double currentYaw;
      private final double currentPitch;
      private final double targetYaw;
      private final double targetPitch;
      private final double yawDelta;
      private final double pitchDelta;
      private final double deltaLength;
      private final double distance;
      private final double distanceRatio;
      private final double targetMotion;
      private final double targetSpeed;
      private final double yawVelocity;
      private final double pitchVelocity;
      private final boolean attackWindow;
      private final double tickScale;
      private final double smoothness;
      private final double warmup;
      private final double angleResponse;
      private final double sampleShift;
      private final double previousYawStep;
      private final double previousPitchStep;
      private final double[] yawHistory;
      private final double[] pitchHistory;
      private final double multiPointEnabled;
      private final double aimOffsetX;
      private final double aimOffsetY;
      private final double aimOffsetZ;
      private final double aimOffsetLength;
      private final double rawYawStep;
      private final double rawPitchStep;
      private final double appliedYawStep;
      private final double appliedPitchStep;
      private final boolean raytraceSuccess;
      private final boolean hitSuccess;
      private final int decisionToAttackTicks;
      private final int timeToHitTicks;
      private final double jerkScore;
      private final double signFlipScore;
      private final double clampGapScore;
      private final double suspicionScore;
      private final double reward;

      Sample(
         long decisionId,
         long timestampMs,
         int targetId,
         String targetUuid,
         String targetType,
         String pattern,
         double currentYaw,
         double currentPitch,
         double targetYaw,
         double targetPitch,
         double yawDelta,
         double pitchDelta,
         double deltaLength,
         double distance,
         double distanceRatio,
         double targetMotion,
         double targetSpeed,
         double yawVelocity,
         double pitchVelocity,
         boolean attackWindow,
         double tickScale,
         double smoothness,
         double warmup,
         double angleResponse,
         double sampleShift,
         double previousYawStep,
         double previousPitchStep,
         double[] yawHistory,
         double[] pitchHistory,
         double multiPointEnabled,
         double aimOffsetX,
         double aimOffsetY,
         double aimOffsetZ,
         double aimOffsetLength,
         double rawYawStep,
         double rawPitchStep,
         double appliedYawStep,
         double appliedPitchStep,
         boolean raytraceSuccess,
         boolean hitSuccess,
         int decisionToAttackTicks,
         int timeToHitTicks,
         double jerkScore,
         double signFlipScore,
         double clampGapScore,
         double suspicionScore,
         double reward
      ) {
         this.decisionId = decisionId;
         this.timestampMs = timestampMs;
         this.targetId = targetId;
         this.targetUuid = targetUuid;
         this.targetType = targetType;
         this.pattern = pattern;
         this.currentYaw = currentYaw;
         this.currentPitch = currentPitch;
         this.targetYaw = targetYaw;
         this.targetPitch = targetPitch;
         this.yawDelta = yawDelta;
         this.pitchDelta = pitchDelta;
         this.deltaLength = deltaLength;
         this.distance = distance;
         this.distanceRatio = distanceRatio;
         this.targetMotion = targetMotion;
         this.targetSpeed = targetSpeed;
         this.yawVelocity = yawVelocity;
         this.pitchVelocity = pitchVelocity;
         this.attackWindow = attackWindow;
         this.tickScale = tickScale;
         this.smoothness = smoothness;
         this.warmup = warmup;
         this.angleResponse = angleResponse;
         this.sampleShift = sampleShift;
         this.previousYawStep = previousYawStep;
         this.previousPitchStep = previousPitchStep;
         this.yawHistory = yawHistory == null ? new double[0] : (double[])yawHistory.clone();
         this.pitchHistory = pitchHistory == null ? new double[0] : (double[])pitchHistory.clone();
         this.multiPointEnabled = multiPointEnabled;
         this.aimOffsetX = aimOffsetX;
         this.aimOffsetY = aimOffsetY;
         this.aimOffsetZ = aimOffsetZ;
         this.aimOffsetLength = aimOffsetLength;
         this.rawYawStep = rawYawStep;
         this.rawPitchStep = rawPitchStep;
         this.appliedYawStep = appliedYawStep;
         this.appliedPitchStep = appliedPitchStep;
         this.raytraceSuccess = raytraceSuccess;
         this.hitSuccess = hitSuccess;
         this.decisionToAttackTicks = decisionToAttackTicks;
         this.timeToHitTicks = timeToHitTicks;
         this.jerkScore = jerkScore;
         this.signFlipScore = signFlipScore;
         this.clampGapScore = clampGapScore;
         this.suspicionScore = suspicionScore;
         this.reward = reward;
      }

      private String toCsv() {
         StringBuilder builder = new StringBuilder(512);
         this.appendCsv(builder, this.decisionId);
         this.appendCsv(builder, this.timestampMs);
         this.appendCsv(builder, this.targetId);
         this.appendCsv(builder, this.targetUuid);
         this.appendCsv(builder, this.targetType);
         this.appendCsv(builder, this.pattern);
         this.appendCsv(builder, this.currentYaw);
         this.appendCsv(builder, this.currentPitch);
         this.appendCsv(builder, this.targetYaw);
         this.appendCsv(builder, this.targetPitch);
         this.appendCsv(builder, this.yawDelta);
         this.appendCsv(builder, this.pitchDelta);
         this.appendCsv(builder, this.deltaLength);
         this.appendCsv(builder, this.distance);
         this.appendCsv(builder, this.distanceRatio);
         this.appendCsv(builder, this.targetMotion);
         this.appendCsv(builder, this.targetSpeed);
         this.appendCsv(builder, this.yawVelocity);
         this.appendCsv(builder, this.pitchVelocity);
         this.appendCsv(builder, this.attackWindow ? 1 : 0);
         this.appendCsv(builder, this.tickScale);
         this.appendCsv(builder, this.smoothness);
         this.appendCsv(builder, this.warmup);
         this.appendCsv(builder, this.angleResponse);
         this.appendCsv(builder, this.sampleShift);
         this.appendCsv(builder, this.previousYawStep);
         this.appendCsv(builder, this.previousPitchStep);
         this.appendHistory(builder, this.yawHistory);
         this.appendHistory(builder, this.pitchHistory);
         this.appendCsv(builder, this.multiPointEnabled);
         this.appendCsv(builder, this.aimOffsetX);
         this.appendCsv(builder, this.aimOffsetY);
         this.appendCsv(builder, this.aimOffsetZ);
         this.appendCsv(builder, this.aimOffsetLength);
         this.appendCsv(builder, this.rawYawStep);
         this.appendCsv(builder, this.rawPitchStep);
         this.appendCsv(builder, this.appliedYawStep);
         this.appendCsv(builder, this.appliedPitchStep);
         this.appendCsv(builder, this.raytraceSuccess ? 1 : 0);
         this.appendCsv(builder, this.hitSuccess ? 1 : 0);
         this.appendCsv(builder, this.decisionToAttackTicks);
         this.appendCsv(builder, this.timeToHitTicks);
         this.appendCsv(builder, this.jerkScore);
         this.appendCsv(builder, this.signFlipScore);
         this.appendCsv(builder, this.clampGapScore);
         this.appendCsv(builder, this.suspicionScore);
         this.appendCsv(builder, this.reward);
         return builder.toString();
      }

      private String toJson() {
         StringBuilder builder = new StringBuilder(768);
         builder.append('{');
         this.appendJson(builder, "decisionId", this.decisionId);
         this.appendJson(builder, "timestampMs", this.timestampMs);
         this.appendJson(builder, "targetId", this.targetId);
         this.appendJson(builder, "targetUuid", this.targetUuid);
         this.appendJson(builder, "targetType", this.targetType);
         this.appendJson(builder, "pattern", this.pattern);
         this.appendJson(builder, "currentYaw", this.currentYaw);
         this.appendJson(builder, "currentPitch", this.currentPitch);
         this.appendJson(builder, "targetYaw", this.targetYaw);
         this.appendJson(builder, "targetPitch", this.targetPitch);
         this.appendJson(builder, "yawDelta", this.yawDelta);
         this.appendJson(builder, "pitchDelta", this.pitchDelta);
         this.appendJson(builder, "deltaLength", this.deltaLength);
         this.appendJson(builder, "distance", this.distance);
         this.appendJson(builder, "distanceRatio", this.distanceRatio);
         this.appendJson(builder, "targetMotion", this.targetMotion);
         this.appendJson(builder, "targetSpeed", this.targetSpeed);
         this.appendJson(builder, "yawVelocity", this.yawVelocity);
         this.appendJson(builder, "pitchVelocity", this.pitchVelocity);
         this.appendJson(builder, "attackWindow", this.attackWindow);
         this.appendJson(builder, "tickScale", this.tickScale);
         this.appendJson(builder, "smoothness", this.smoothness);
         this.appendJson(builder, "warmup", this.warmup);
         this.appendJson(builder, "angleResponse", this.angleResponse);
         this.appendJson(builder, "sampleShift", this.sampleShift);
         this.appendJson(builder, "previousYawStep", this.previousYawStep);
         this.appendJson(builder, "previousPitchStep", this.previousPitchStep);
         this.appendJsonArray(builder, "yawHistory", this.yawHistory);
         this.appendJsonArray(builder, "pitchHistory", this.pitchHistory);
         this.appendJson(builder, "multiPointEnabled", this.multiPointEnabled);
         this.appendJson(builder, "aimOffsetX", this.aimOffsetX);
         this.appendJson(builder, "aimOffsetY", this.aimOffsetY);
         this.appendJson(builder, "aimOffsetZ", this.aimOffsetZ);
         this.appendJson(builder, "aimOffsetLength", this.aimOffsetLength);
         this.appendJson(builder, "rawYawStep", this.rawYawStep);
         this.appendJson(builder, "rawPitchStep", this.rawPitchStep);
         this.appendJson(builder, "appliedYawStep", this.appliedYawStep);
         this.appendJson(builder, "appliedPitchStep", this.appliedPitchStep);
         this.appendJson(builder, "raytraceSuccess", this.raytraceSuccess);
         this.appendJson(builder, "hitSuccess", this.hitSuccess);
         this.appendJson(builder, "decisionToAttackTicks", this.decisionToAttackTicks);
         this.appendJson(builder, "timeToHitTicks", this.timeToHitTicks);
         this.appendJson(builder, "jerkScore", this.jerkScore);
         this.appendJson(builder, "signFlipScore", this.signFlipScore);
         this.appendJson(builder, "clampGapScore", this.clampGapScore);
         this.appendJson(builder, "suspicionScore", this.suspicionScore);
         this.appendJson(builder, "reward", this.reward);
         builder.append('}');
         return builder.toString();
      }

      private void appendHistory(StringBuilder builder, double[] values) {
         for (int index = 0; index < 4; index++) {
            this.appendCsv(builder, index < values.length ? values[index] : 0.0);
         }
      }

      private void appendCsv(StringBuilder builder, String value) {
         if (!builder.isEmpty()) {
            builder.append(',');
         }

         builder.append('"').append(this.escapeJson(value)).append('"');
      }

      private void appendCsv(StringBuilder builder, double value) {
         if (!builder.isEmpty()) {
            builder.append(',');
         }

         builder.append(value);
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

      private void appendJson(StringBuilder builder, String key, String value) {
         this.appendJsonKey(builder, key);
         builder.append('"').append(this.escapeJson(value)).append('"');
      }

      private void appendJson(StringBuilder builder, String key, double value) {
         this.appendJsonKey(builder, key);
         builder.append(value);
      }

      private void appendJson(StringBuilder builder, String key, long value) {
         this.appendJsonKey(builder, key);
         builder.append(value);
      }

      private void appendJson(StringBuilder builder, String key, int value) {
         this.appendJsonKey(builder, key);
         builder.append(value);
      }

      private void appendJson(StringBuilder builder, String key, boolean value) {
         this.appendJsonKey(builder, key);
         builder.append(value);
      }

      private void appendJsonArray(StringBuilder builder, String key, double[] values) {
         this.appendJsonKey(builder, key);
         builder.append('[');

         for (int index = 0; index < values.length; index++) {
            if (index > 0) {
               builder.append(',');
            }

            builder.append(values[index]);
         }

         builder.append(']');
      }

      private void appendJsonKey(StringBuilder builder, String key) {
         if (builder.length() > 1) {
            builder.append(',');
         }

         builder.append('"').append(key).append('"').append(':');
      }

      private String escapeJson(String value) {
         return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
      }
   }

   private static final class WriterState {
      private final Path path;
      private final BufferedWriter writer;
      private int samplesSinceFlush;

      private WriterState(Path path, BufferedWriter writer) {
         this.path = path;
         this.writer = writer;
      }
   }
}
