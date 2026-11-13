package daamky.ua.api.module.impl.combat.aura.rotations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import daamky.ua.api.module.impl.combat.AuraModule;

final class NeuroRuntimeModel {
   private static final String[] LEGACY_INPUT_ORDER = new String[]{
      "yawDelta",
      "pitchDelta",
      "deltaLength",
      "distance",
      "distanceRatio",
      "targetMotion",
      "targetSpeed",
      "yawVelocity",
      "pitchVelocity",
      "attackWindow",
      "tickScale",
      "smoothness",
      "warmup",
      "angleResponse",
      "sampleShift",
      "previousYawStep",
      "previousPitchStep",
      "multiPointEnabled",
      "aimOffsetX",
      "aimOffsetY",
      "aimOffsetZ",
      "aimOffsetLength",
      "rawYawStep",
      "rawPitchStep",
      "candidateYawStep",
      "candidatePitchStep",
      "candidateJerkScore",
      "candidateSignFlipScore",
      "candidateClampGapScore"
   };
   private static final NeuroRuntimeModel INSTANCE = new NeuroRuntimeModel();
   private NeuroRuntimeModel.RewardModel cachedModel;
   private Path cachedPath;
   private long cachedLastModified = Long.MIN_VALUE;
   private boolean loadFailed;

   private NeuroRuntimeModel() {
   }

   static NeuroRuntimeModel getInstance() {
      return INSTANCE;
   }

   NeuroRuntimeModel.Selection selectStep(AuraModule aura, NeuroRuntimeModel.InferenceContext context) {
      if (aura != null && aura.isNeuroModelInferenceEnabled()) {
         NeuroRuntimeModel.RewardModel model = this.loadModel(this.resolveModelPath(aura));
         if (model == null) {
            return NeuroRuntimeModel.Selection.passThrough(context.rawYawStep, context.rawPitchStep, false);
         } else {
            double blend = aura.getNeuroModelBlend().getValue();
            double yawUnit = Math.max(context.yawMicro, Math.abs(context.rawYawStep) * 0.08 + 0.012);
            double pitchUnit = Math.max(context.pitchMicro, Math.abs(context.rawPitchStep) * 0.08 + 0.009);
            double[][] candidates = this.buildCandidates(context.rawYawStep, context.rawPitchStep, yawUnit, pitchUnit);
            double bestYaw = context.rawYawStep;
            double bestPitch = context.rawPitchStep;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (double[] candidate : candidates) {
               double unclampedYaw = candidate[0];
               double unclampedPitch = candidate[1];
               double clampedYaw = this.clampToRemaining(unclampedYaw, context.yawDelta);
               double clampedPitch = this.clampToRemaining(unclampedPitch, context.pitchDelta);
               NeuroRuntimeModel.CandidateMetrics metrics = NeuroRuntimeModel.CandidateMetrics.from(
                  context, unclampedYaw, unclampedPitch, clampedYaw, clampedPitch
               );
               double score = model.score(this.buildInputVector(model.inputOrder(), context, clampedYaw, clampedPitch, metrics));
               if (Double.isFinite(score) && score > bestScore) {
                  bestScore = score;
                  bestYaw = clampedYaw;
                  bestPitch = clampedPitch;
               }
            }

            double finalYaw = this.lerp(context.rawYawStep, bestYaw, blend);
            double finalPitch = this.lerp(context.rawPitchStep, bestPitch, blend);
            finalYaw = this.clampToRemaining(finalYaw, context.yawDelta);
            finalPitch = this.clampToRemaining(finalPitch, context.pitchDelta);
            return NeuroRuntimeModel.Selection.passThrough(finalYaw, finalPitch, true);
         }
      } else {
         return NeuroRuntimeModel.Selection.passThrough(context.rawYawStep, context.rawPitchStep, false);
      }
   }

   private Path resolveModelPath(AuraModule aura) {
      String configuredPath = aura.getNeuroModelPath().getValue();
      Path path = configuredPath != null && !configuredPath.isBlank() ? Path.of(configuredPath) : Path.of("daamky", "models", "neuro_reward_model.json");
      if (path.isAbsolute()) {
         return path;
      } else {
         MinecraftClient client = MinecraftClient.getInstance();
         return client != null && client.runDirectory != null ? client.runDirectory.toPath().resolve(path).normalize() : path.toAbsolutePath().normalize();
      }
   }

   private NeuroRuntimeModel.RewardModel loadModel(Path path) {
      if (path == null) {
         return null;
      } else {
         try {
            if (!Files.exists(path)) {
               this.cachedModel = null;
               this.cachedPath = path;
               this.cachedLastModified = Long.MIN_VALUE;
               this.loadFailed = false;
               return null;
            } else {
               long lastModified = Files.getLastModifiedTime(path).toMillis();
               if (this.cachedModel != null && path.equals(this.cachedPath) && lastModified == this.cachedLastModified) {
                  return this.cachedModel;
               } else {
                  NeuroRuntimeModel.RewardModel var7;
                  try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                     JsonElement rootElement = JsonParser.parseReader(reader);
                     if (rootElement == null || !rootElement.isJsonObject()) {
                        throw new IOException("Neuro model root must be a JSON object");
                     }

                     NeuroRuntimeModel.RewardModel loaded = NeuroRuntimeModel.RewardModel.fromJson(rootElement.getAsJsonObject());
                     this.cachedModel = loaded;
                     this.cachedPath = path;
                     this.cachedLastModified = lastModified;
                     this.loadFailed = false;
                     var7 = loaded;
                  }

                  return var7;
               }
            }
         } catch (Exception var10) {
            this.cachedModel = null;
            this.cachedPath = path;
            this.cachedLastModified = Long.MIN_VALUE;
            if (!this.loadFailed) {
               this.loadFailed = true;
               System.err.println("Failed to load Neuro reward model from " + path + ": " + var10.getMessage());
               var10.printStackTrace();
            }

            return null;
         }
      }
   }

   private double[] buildInputVector(
      String[] inputOrder,
      NeuroRuntimeModel.InferenceContext context,
      double candidateYawStep,
      double candidatePitchStep,
      NeuroRuntimeModel.CandidateMetrics metrics
   ) {
      double[] values = new double[inputOrder.length];

      for (int index = 0; index < inputOrder.length; index++) {
         values[index] = this.featureValue(inputOrder[index], context, candidateYawStep, candidatePitchStep, metrics);
      }

      return values;
   }

   private double featureValue(
      String name, NeuroRuntimeModel.InferenceContext context, double candidateYawStep, double candidatePitchStep, NeuroRuntimeModel.CandidateMetrics metrics
   ) {
      return switch (name) {
         case "yawDelta" -> context.yawDelta;
         case "pitchDelta" -> context.pitchDelta;
         case "deltaLength" -> context.deltaLength;
         case "distance" -> context.distance;
         case "distanceRatio" -> context.distanceRatio;
         case "targetMotion" -> context.targetMotion;
         case "targetSpeed" -> context.targetSpeed;
         case "yawVelocity" -> context.yawVelocity;
         case "pitchVelocity" -> context.pitchVelocity;
         case "attackWindow" -> context.attackWindow ? 1.0 : 0.0;
         case "tickScale" -> context.tickScale;
         case "smoothness" -> context.smoothness;
         case "warmup" -> context.warmup;
         case "angleResponse" -> context.angleResponse;
         case "sampleShift" -> context.sampleShift;
         case "previousYawStep" -> context.previousYawStep;
         case "previousPitchStep" -> context.previousPitchStep;
         case "multiPointEnabled" -> context.multiPointEnabled;
         case "aimOffsetX" -> context.aimOffsetX;
         case "aimOffsetY" -> context.aimOffsetY;
         case "aimOffsetZ" -> context.aimOffsetZ;
         case "aimOffsetLength" -> context.aimOffsetLength;
         case "rawYawStep" -> context.rawYawStep;
         case "rawPitchStep" -> context.rawPitchStep;
         case "candidateYawStep" -> candidateYawStep;
         case "candidatePitchStep" -> candidatePitchStep;
         case "candidateJerkScore" -> metrics.jerkScore;
         case "candidateSignFlipScore" -> metrics.signFlipScore;
         case "candidateClampGapScore" -> metrics.clampGapScore;
         default -> 0.0;
      };
   }

   private boolean isSupportedFeature(String name) {
      return switch (name) {
         case "yawDelta", "pitchDelta", "deltaLength", "distance", "distanceRatio", "targetMotion", "targetSpeed", "yawVelocity", "pitchVelocity", "attackWindow", "tickScale", "smoothness", "warmup", "angleResponse", "sampleShift", "previousYawStep", "previousPitchStep", "multiPointEnabled", "aimOffsetX", "aimOffsetY", "aimOffsetZ", "aimOffsetLength", "rawYawStep", "rawPitchStep", "candidateYawStep", "candidatePitchStep", "candidateJerkScore", "candidateSignFlipScore", "candidateClampGapScore" -> true;
         default -> false;
      };
   }

   private double[][] buildCandidates(double baseYaw, double basePitch, double yawUnit, double pitchUnit) {
      return new double[][]{
         {baseYaw, basePitch},
         {baseYaw + yawUnit, basePitch},
         {baseYaw - yawUnit, basePitch},
         {baseYaw + yawUnit * 2.0, basePitch},
         {baseYaw - yawUnit * 2.0, basePitch},
         {baseYaw, basePitch + pitchUnit},
         {baseYaw, basePitch - pitchUnit},
         {baseYaw, basePitch + pitchUnit * 2.0},
         {baseYaw, basePitch - pitchUnit * 2.0},
         {baseYaw + yawUnit, basePitch + pitchUnit},
         {baseYaw + yawUnit, basePitch - pitchUnit},
         {baseYaw - yawUnit, basePitch + pitchUnit},
         {baseYaw - yawUnit, basePitch - pitchUnit}
      };
   }

   private double lerp(double from, double to, double alpha) {
      double clamped = MathHelper.clamp(alpha, 0.0, 1.0);
      return from + (to - from) * clamped;
   }

   private double clampToRemaining(double step, double remaining) {
      if (Math.abs(remaining) < 1.0E-6) {
         return 0.0;
      } else if (Math.signum(step) != Math.signum(remaining)) {
         return 0.0;
      } else {
         return Math.abs(step) > Math.abs(remaining) ? remaining : step;
      }
   }

   private static record CandidateMetrics(double jerkScore, double signFlipScore, double clampGapScore) {
      private static NeuroRuntimeModel.CandidateMetrics from(
         NeuroRuntimeModel.InferenceContext context, double unclampedYaw, double unclampedPitch, double candidateYaw, double candidatePitch
      ) {
         double jerk = Math.abs(candidateYaw - context.previousYawStep()) + Math.abs(candidatePitch - context.previousPitchStep());
         double normalizer = Math.max(1.0, Math.abs(context.yawDelta()) + Math.abs(context.pitchDelta()));
         double jerkScore = MathHelper.clamp(jerk / normalizer, 0.0, 1.0);
         int flips = 0;
         if (hasSignFlip(context.previousYawStep(), candidateYaw)) {
            flips++;
         }

         if (hasSignFlip(context.previousPitchStep(), candidatePitch)) {
            flips++;
         }

         double signFlipScore = (double)flips / 2.0;
         double clampGap = Math.abs(unclampedYaw - candidateYaw) + Math.abs(unclampedPitch - candidatePitch);
         double clampNormalizer = Math.max(0.25, Math.abs(unclampedYaw) + Math.abs(unclampedPitch));
         double clampGapScore = MathHelper.clamp(clampGap / clampNormalizer, 0.0, 1.0);
         return new NeuroRuntimeModel.CandidateMetrics(jerkScore, signFlipScore, clampGapScore);
      }

      private static boolean hasSignFlip(double previous, double current) {
         return previous != 0.0 && current != 0.0 && Math.signum(previous) != Math.signum(current);
      }
   }

   static record InferenceContext(
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
      double multiPointEnabled,
      double aimOffsetX,
      double aimOffsetY,
      double aimOffsetZ,
      double aimOffsetLength,
      double rawYawStep,
      double rawPitchStep,
      double yawMicro,
      double pitchMicro
   ) {
   }

   private static final class Layer {
      private final double[][] weights;
      private final double[] bias;
      private final String activation;

      private Layer(double[][] weights, double[] bias, String activation) {
         this.weights = weights;
         this.bias = bias;
         this.activation = activation;
      }

      private static NeuroRuntimeModel.Layer fromJson(JsonObject root, int expectedInputSize) throws IOException {
         JsonArray weightsJson = root.getAsJsonArray("weights");
         JsonArray biasJson = root.getAsJsonArray("bias");
         if (weightsJson != null && biasJson != null) {
            int outputSize = weightsJson.size();
            if (outputSize != 0 && biasJson.size() == outputSize) {
               double[][] weights = new double[outputSize][expectedInputSize];

               for (int row = 0; row < outputSize; row++) {
                  JsonArray rowJson = weightsJson.get(row).getAsJsonArray();
                  if (rowJson.size() != expectedInputSize) {
                     throw new IOException("Neuro model layer expected input size " + expectedInputSize + " but got " + rowJson.size());
                  }

                  for (int column = 0; column < expectedInputSize; column++) {
                     weights[row][column] = rowJson.get(column).getAsDouble();
                  }
               }

               double[] bias = new double[outputSize];

               for (int index = 0; index < outputSize; index++) {
                  bias[index] = biasJson.get(index).getAsDouble();
               }

               String activation = root.has("activation") ? root.get("activation").getAsString() : "none";
               return new NeuroRuntimeModel.Layer(weights, bias, activation);
            } else {
               throw new IOException("Neuro model layer bias size must match output size");
            }
         } else {
            throw new IOException("Neuro model layer is missing weights or bias");
         }
      }

      private double[] forward(double[] input) {
         double[] output = new double[this.bias.length];

         for (int row = 0; row < this.bias.length; row++) {
            double sum = this.bias[row];

            for (int column = 0; column < input.length; column++) {
               sum += this.weights[row][column] * input[column];
            }

            output[row] = this.applyActivation(sum);
         }

         return output;
      }

      private int outputSize() {
         return this.bias.length;
      }

      private double applyActivation(double value) {
         String var3 = this.activation.toLowerCase();

         return switch (var3) {
            case "relu" -> Math.max(0.0, value);
            case "tanh" -> Math.tanh(value);
            case "sigmoid" -> 1.0 / (1.0 + Math.exp(-value));
            default -> value;
         };
      }
   }

   private static final class RewardModel {
      private final String[] inputOrder;
      private final double[] mean;
      private final double[] std;
      private final List<NeuroRuntimeModel.Layer> layers;

      private RewardModel(String[] inputOrder, double[] mean, double[] std, List<NeuroRuntimeModel.Layer> layers) {
         this.inputOrder = inputOrder;
         this.mean = mean;
         this.std = std;
         this.layers = layers;
      }

      private static NeuroRuntimeModel.RewardModel fromJson(JsonObject root) throws IOException {
         JsonArray inputOrderJson = root.has("inputOrder") && root.get("inputOrder").isJsonArray() ? root.getAsJsonArray("inputOrder") : null;
         String[] inputOrder = new String[inputOrderJson == null ? NeuroRuntimeModel.LEGACY_INPUT_ORDER.length : inputOrderJson.size()];
         if (inputOrderJson == null) {
            System.arraycopy(NeuroRuntimeModel.LEGACY_INPUT_ORDER, 0, inputOrder, 0, NeuroRuntimeModel.LEGACY_INPUT_ORDER.length);
         } else {
            for (int index = 0; index < inputOrder.length; index++) {
               inputOrder[index] = inputOrderJson.get(index).getAsString();
            }
         }

         NeuroRuntimeModel runtime = NeuroRuntimeModel.getInstance();

         for (String inputName : inputOrder) {
            if (!runtime.isSupportedFeature(inputName)) {
               throw new IOException("Unsupported Neuro model input feature: " + inputName);
            }
         }

         JsonObject normalization = root.has("normalization") && root.get("normalization").isJsonObject()
            ? root.getAsJsonObject("normalization")
            : new JsonObject();
         double[] mean = readVector(normalization.has("mean") ? normalization.getAsJsonArray("mean") : null, inputOrder.length, 0.0);
         double[] std = readVector(normalization.has("std") ? normalization.getAsJsonArray("std") : null, inputOrder.length, 1.0);
         JsonArray layersJson = root.getAsJsonArray("layers");
         if (layersJson != null && !layersJson.isEmpty()) {
            List<NeuroRuntimeModel.Layer> layers = new ArrayList<>(layersJson.size());
            int inputSize = inputOrder.length;

            for (JsonElement layerElement : layersJson) {
               if (!layerElement.isJsonObject()) {
                  throw new IOException("Neuro model layer must be a JSON object");
               }

               NeuroRuntimeModel.Layer layer = NeuroRuntimeModel.Layer.fromJson(layerElement.getAsJsonObject(), inputSize);
               inputSize = layer.outputSize();
               layers.add(layer);
            }

            if (inputSize != 1) {
               throw new IOException("Neuro reward model final layer must output exactly 1 value");
            } else {
               return new NeuroRuntimeModel.RewardModel(inputOrder, mean, std, layers);
            }
         } else {
            throw new IOException("Neuro model must contain at least one layer");
         }
      }

      private double score(double[] input) {
         double[] normalized = new double[input.length];

         for (int index = 0; index < input.length; index++) {
            double scale = Math.abs(this.std[index]) < 1.0E-9 ? 1.0 : this.std[index];
            normalized[index] = (input[index] - this.mean[index]) / scale;
         }

         double[] values = normalized;

         for (NeuroRuntimeModel.Layer layer : this.layers) {
            values = layer.forward(values);
         }

         return values[0];
      }

      private String[] inputOrder() {
         return this.inputOrder;
      }

      private static double[] readVector(JsonArray array, int expectedSize, double fallbackValue) throws IOException {
         double[] values = new double[expectedSize];
         if (array == null) {
            for (int index = 0; index < expectedSize; index++) {
               values[index] = fallbackValue;
            }

            return values;
         } else if (array.size() != expectedSize) {
            throw new IOException("Expected vector of size " + expectedSize + " but got " + array.size());
         } else {
            for (int index = 0; index < expectedSize; index++) {
               values[index] = array.get(index).getAsDouble();
            }

            return values;
         }
      }
   }

   static record Selection(double yawStep, double pitchStep, boolean modelApplied) {
      static NeuroRuntimeModel.Selection passThrough(double yawStep, double pitchStep, boolean modelApplied) {
         return new NeuroRuntimeModel.Selection(yawStep, pitchStep, modelApplied);
      }
   }
}
