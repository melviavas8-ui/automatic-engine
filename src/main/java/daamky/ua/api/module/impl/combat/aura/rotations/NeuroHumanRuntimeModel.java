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

final class NeuroHumanRuntimeModel {
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
      "aimOffsetLength"
   };
   private static final NeuroHumanRuntimeModel INSTANCE = new NeuroHumanRuntimeModel();
   private NeuroHumanRuntimeModel.HumanModel cachedModel;
   private Path cachedPath;
   private long cachedLastModified = Long.MIN_VALUE;
   private boolean loadFailed;

   private NeuroHumanRuntimeModel() {
   }

   static NeuroHumanRuntimeModel getInstance() {
      return INSTANCE;
   }

   NeuroRuntimeModel.Selection predict(AuraModule aura, NeuroRuntimeModel.InferenceContext context) {
      if (aura != null && aura.isNeuroHumanInferenceEnabled()) {
         NeuroHumanRuntimeModel.HumanModel model = this.loadModel(this.resolveModelPath(aura));
         if (model == null) {
            return NeuroRuntimeModel.Selection.passThrough(context.rawYawStep(), context.rawPitchStep(), false);
         } else {
            double[] prediction = model.predict(this.buildInputVector(model.inputOrder(), context));
            if (prediction.length >= 2 && Double.isFinite(prediction[0]) && Double.isFinite(prediction[1])) {
               double blend = aura.getNeuroHumanBlend().getValue();
               double yaw = this.lerp(context.rawYawStep(), prediction[0], blend);
               double pitch = this.lerp(context.rawPitchStep(), prediction[1], blend);
               yaw = this.preserveTurnResponse(yaw, context.rawYawStep(), context.yawDelta(), context.deltaLength());
               pitch = this.preserveTurnResponse(pitch, context.rawPitchStep(), context.pitchDelta(), context.deltaLength());
               yaw = this.clampToRemaining(yaw, context.yawDelta());
               pitch = this.clampToRemaining(pitch, context.pitchDelta());
               return NeuroRuntimeModel.Selection.passThrough(yaw, pitch, true);
            } else {
               return NeuroRuntimeModel.Selection.passThrough(context.rawYawStep(), context.rawPitchStep(), false);
            }
         }
      } else {
         return NeuroRuntimeModel.Selection.passThrough(context.rawYawStep(), context.rawPitchStep(), false);
      }
   }

   private Path resolveModelPath(AuraModule aura) {
      String configuredPath = aura.getNeuroHumanModelPath().getValue();
      Path path = configuredPath != null && !configuredPath.isBlank() ? Path.of(configuredPath) : Path.of("daamky", "models", "neuro_human_model.json");
      if (path.isAbsolute()) {
         return path;
      } else {
         MinecraftClient client = MinecraftClient.getInstance();
         return client != null && client.runDirectory != null ? client.runDirectory.toPath().resolve(path).normalize() : path.toAbsolutePath().normalize();
      }
   }

   private NeuroHumanRuntimeModel.HumanModel loadModel(Path path) {
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
                  NeuroHumanRuntimeModel.HumanModel var7;
                  try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                     JsonElement rootElement = JsonParser.parseReader(reader);
                     if (rootElement == null || !rootElement.isJsonObject()) {
                        throw new IOException("Neuro human model root must be a JSON object");
                     }

                     NeuroHumanRuntimeModel.HumanModel loaded = NeuroHumanRuntimeModel.HumanModel.fromJson(rootElement.getAsJsonObject());
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
               System.err.println("Failed to load Neuro human model from " + path + ": " + var10.getMessage());
               var10.printStackTrace();
            }

            return null;
         }
      }
   }

   private double[] buildInputVector(String[] inputOrder, NeuroRuntimeModel.InferenceContext context) {
      double[] values = new double[inputOrder.length];

      for (int index = 0; index < inputOrder.length; index++) {
         values[index] = this.featureValue(inputOrder[index], context);
      }

      return values;
   }

   private double featureValue(String name, NeuroRuntimeModel.InferenceContext context) {
      return switch (name) {
         case "yawDelta" -> context.yawDelta();
         case "pitchDelta" -> context.pitchDelta();
         case "deltaLength" -> context.deltaLength();
         case "distance" -> context.distance();
         case "distanceRatio" -> context.distanceRatio();
         case "targetMotion" -> context.targetMotion();
         case "targetSpeed" -> context.targetSpeed();
         case "yawVelocity" -> context.yawVelocity();
         case "pitchVelocity" -> context.pitchVelocity();
         case "attackWindow" -> context.attackWindow() ? 1.0 : 0.0;
         case "tickScale" -> context.tickScale();
         case "smoothness" -> context.smoothness();
         case "warmup" -> context.warmup();
         case "angleResponse" -> context.angleResponse();
         case "sampleShift" -> context.sampleShift();
         case "previousYawStep" -> context.previousYawStep();
         case "previousPitchStep" -> context.previousPitchStep();
         case "multiPointEnabled" -> context.multiPointEnabled();
         case "aimOffsetX" -> context.aimOffsetX();
         case "aimOffsetY" -> context.aimOffsetY();
         case "aimOffsetZ" -> context.aimOffsetZ();
         case "aimOffsetLength" -> context.aimOffsetLength();
         default -> 0.0;
      };
   }

   private boolean isSupportedFeature(String name) {
      return switch (name) {
         case "yawDelta", "pitchDelta", "deltaLength", "distance", "distanceRatio", "targetMotion", "targetSpeed", "yawVelocity", "pitchVelocity", "attackWindow", "tickScale", "smoothness", "warmup", "angleResponse", "sampleShift", "previousYawStep", "previousPitchStep", "multiPointEnabled", "aimOffsetX", "aimOffsetY", "aimOffsetZ", "aimOffsetLength" -> true;
         default -> false;
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

   private double preserveTurnResponse(double predictedStep, double rawStep, double remaining, double deltaLength) {
      double turnUrgency = MathHelper.clamp((deltaLength - 28.0) / 52.0, 0.0, 1.0);
      if (turnUrgency <= 0.0) {
         return predictedStep;
      } else if (Math.abs(rawStep) < 1.0E-5) {
         return predictedStep;
      } else if (Math.signum(predictedStep) != 0.0 && Math.signum(predictedStep) != Math.signum(rawStep)) {
         return this.clampToRemaining(this.lerp(predictedStep, rawStep, 0.55 + turnUrgency * 0.25), remaining);
      } else {
         double minRatio = 0.45 + turnUrgency * 0.35;
         double minStep = Math.abs(rawStep) * minRatio;
         if (Math.abs(predictedStep) >= minStep) {
            return predictedStep;
         } else {
            double boostedMagnitude = this.lerp(Math.abs(predictedStep), Math.abs(rawStep), 0.42 + turnUrgency * 0.3);
            double boostedStep = Math.copySign(boostedMagnitude, rawStep);
            return this.clampToRemaining(boostedStep, remaining);
         }
      }
   }

   private static final class HumanModel {
      private final String[] inputOrder;
      private final double[] mean;
      private final double[] std;
      private final List<NeuroHumanRuntimeModel.Layer> layers;

      private HumanModel(String[] inputOrder, double[] mean, double[] std, List<NeuroHumanRuntimeModel.Layer> layers) {
         this.inputOrder = inputOrder;
         this.mean = mean;
         this.std = std;
         this.layers = layers;
      }

      private static NeuroHumanRuntimeModel.HumanModel fromJson(JsonObject root) throws IOException {
         String modelType = root.has("modelType") ? root.get("modelType").getAsString() : "";
         if (!"human_mlp".equalsIgnoreCase(modelType)) {
            throw new IOException("Expected modelType=human_mlp but got " + modelType);
         } else {
            JsonArray inputOrderJson = root.has("inputOrder") && root.get("inputOrder").isJsonArray() ? root.getAsJsonArray("inputOrder") : null;
            String[] inputOrder = new String[inputOrderJson == null ? NeuroHumanRuntimeModel.LEGACY_INPUT_ORDER.length : inputOrderJson.size()];
            if (inputOrderJson == null) {
               System.arraycopy(NeuroHumanRuntimeModel.LEGACY_INPUT_ORDER, 0, inputOrder, 0, NeuroHumanRuntimeModel.LEGACY_INPUT_ORDER.length);
            } else {
               for (int index = 0; index < inputOrder.length; index++) {
                  inputOrder[index] = inputOrderJson.get(index).getAsString();
               }
            }

            NeuroHumanRuntimeModel runtime = NeuroHumanRuntimeModel.getInstance();

            for (String inputName : inputOrder) {
               if (!runtime.isSupportedFeature(inputName)) {
                  throw new IOException("Unsupported Neuro human input feature: " + inputName);
               }
            }

            JsonObject normalization = root.has("normalization") && root.get("normalization").isJsonObject()
               ? root.getAsJsonObject("normalization")
               : new JsonObject();
            double[] mean = readVector(normalization.has("mean") ? normalization.getAsJsonArray("mean") : null, inputOrder.length, 0.0);
            double[] std = readVector(normalization.has("std") ? normalization.getAsJsonArray("std") : null, inputOrder.length, 1.0);
            JsonArray layersJson = root.getAsJsonArray("layers");
            if (layersJson != null && !layersJson.isEmpty()) {
               List<NeuroHumanRuntimeModel.Layer> layers = new ArrayList<>(layersJson.size());
               int inputSize = inputOrder.length;

               for (JsonElement layerElement : layersJson) {
                  NeuroHumanRuntimeModel.Layer layer = NeuroHumanRuntimeModel.Layer.fromJson(layerElement.getAsJsonObject(), inputSize);
                  inputSize = layer.outputSize();
                  layers.add(layer);
               }

               if (inputSize != 2) {
                  throw new IOException("Neuro human model final layer must output 2 values");
               } else {
                  return new NeuroHumanRuntimeModel.HumanModel(inputOrder, mean, std, layers);
               }
            } else {
               throw new IOException("Neuro human model must contain layers");
            }
         }
      }

      private double[] predict(double[] input) {
         double[] normalized = new double[input.length];

         for (int index = 0; index < input.length; index++) {
            double scale = Math.abs(this.std[index]) < 1.0E-9 ? 1.0 : this.std[index];
            normalized[index] = (input[index] - this.mean[index]) / scale;
         }

         double[] values = normalized;

         for (NeuroHumanRuntimeModel.Layer layer : this.layers) {
            values = layer.forward(values);
         }

         return values;
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

   private static final class Layer {
      private final double[][] weights;
      private final double[] bias;
      private final String activation;

      private Layer(double[][] weights, double[] bias, String activation) {
         this.weights = weights;
         this.bias = bias;
         this.activation = activation;
      }

      private static NeuroHumanRuntimeModel.Layer fromJson(JsonObject root, int expectedInputSize) throws IOException {
         JsonArray weightsJson = root.getAsJsonArray("weights");
         JsonArray biasJson = root.getAsJsonArray("bias");
         if (weightsJson != null && biasJson != null) {
            int outputSize = weightsJson.size();
            if (outputSize != 0 && biasJson.size() == outputSize) {
               double[][] weights = new double[outputSize][expectedInputSize];

               for (int row = 0; row < outputSize; row++) {
                  JsonArray rowJson = weightsJson.get(row).getAsJsonArray();
                  if (rowJson.size() != expectedInputSize) {
                     throw new IOException("Neuro human model expected input size " + expectedInputSize + " but got " + rowJson.size());
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
               return new NeuroHumanRuntimeModel.Layer(weights, bias, activation);
            } else {
               throw new IOException("Neuro human model layer bias size mismatch");
            }
         } else {
            throw new IOException("Neuro human model layer is missing weights or bias");
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
}
