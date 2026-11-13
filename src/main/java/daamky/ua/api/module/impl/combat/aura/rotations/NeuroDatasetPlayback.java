package daamky.ua.api.module.impl.combat.aura.rotations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class NeuroDatasetPlayback {
   private static final NeuroDatasetPlayback INSTANCE = new NeuroDatasetPlayback();
   private List<NeuroDatasetPlayback.DatasetSample> samples = new ArrayList<>();
   private int currentIndex = 0;
   private boolean loaded = false;
   private Path lastLoadedPath;

   private NeuroDatasetPlayback() {
   }

   public static NeuroDatasetPlayback getInstance() {
      return INSTANCE;
   }

   public NeuroDatasetPlayback.DatasetSample getNextSample() {
      if (this.loaded && !this.samples.isEmpty()) {
         NeuroDatasetPlayback.DatasetSample sample = this.samples.get(this.currentIndex);
         this.currentIndex = (this.currentIndex + 1) % this.samples.size();
         return sample;
      } else {
         return NeuroDatasetPlayback.DatasetSample.empty();
      }
   }

   public NeuroDatasetPlayback.DatasetSample findBestMatch(double yawDelta, double pitchDelta, double distance, boolean attackWindow) {
      if (this.loaded && !this.samples.isEmpty()) {
         NeuroDatasetPlayback.DatasetSample best = this.samples.get(0);
         double bestScore = Double.MAX_VALUE;

         for (NeuroDatasetPlayback.DatasetSample sample : this.samples) {
            double yawDiff = Math.abs(sample.yawDelta - yawDelta);
            double pitchDiff = Math.abs(sample.pitchDelta - pitchDelta);
            double distDiff = Math.abs(sample.distance - distance);
            boolean attackMatch = sample.attackWindow == attackWindow;
            double score = yawDiff + pitchDiff + distDiff * 0.1;
            if (!attackMatch) {
               score += 10.0;
            }

            if (score < bestScore) {
               bestScore = score;
               best = sample;
            }
         }

         return best;
      } else {
         return NeuroDatasetPlayback.DatasetSample.empty();
      }
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   public void reset() {
      this.currentIndex = 0;
   }

   public static class DatasetSample {
      public final double yawDelta;
      public final double pitchDelta;
      public final double deltaLength;
      public final double distance;
      public final double targetSpeed;
      public final boolean attackWindow;
      public final double smoothness;
      public final double speed;
      public final boolean multiPointEnabled;
      public final double aimOffsetX;
      public final double aimOffsetY;
      public final double aimOffsetZ;
      public final double appliedYawStep;
      public final double appliedPitchStep;

      private DatasetSample(
         double yawDelta,
         double pitchDelta,
         double deltaLength,
         double distance,
         double targetSpeed,
         boolean attackWindow,
         double smoothness,
         double speed,
         boolean multiPointEnabled,
         double aimOffsetX,
         double aimOffsetY,
         double aimOffsetZ,
         double appliedYawStep,
         double appliedPitchStep
      ) {
         this.yawDelta = yawDelta;
         this.pitchDelta = pitchDelta;
         this.deltaLength = deltaLength;
         this.distance = distance;
         this.targetSpeed = targetSpeed;
         this.attackWindow = attackWindow;
         this.smoothness = smoothness;
         this.speed = speed;
         this.multiPointEnabled = multiPointEnabled;
         this.aimOffsetX = aimOffsetX;
         this.aimOffsetY = aimOffsetY;
         this.aimOffsetZ = aimOffsetZ;
         this.appliedYawStep = appliedYawStep;
         this.appliedPitchStep = appliedPitchStep;
      }

      static NeuroDatasetPlayback.DatasetSample fromJson(JsonObject obj) {
         return new NeuroDatasetPlayback.DatasetSample(
            getDouble(obj, "yawDelta", 0.0),
            getDouble(obj, "pitchDelta", 0.0),
            getDouble(obj, "deltaLength", 0.0),
            getDouble(obj, "distance", 0.0),
            getDouble(obj, "targetSpeed", 0.0),
            getBoolean(obj, "attackWindow", false),
            getDouble(obj, "smoothness", 0.5),
            getDouble(obj, "speed", 1.0),
            getBoolean(obj, "multiPointEnabled", false),
            getDouble(obj, "aimOffsetX", 0.0),
            getDouble(obj, "aimOffsetY", 0.5),
            getDouble(obj, "aimOffsetZ", 0.0),
            getDouble(obj, "appliedYawStep", 0.0),
            getDouble(obj, "appliedPitchStep", 0.0)
         );
      }

      static NeuroDatasetPlayback.DatasetSample empty() {
         return new NeuroDatasetPlayback.DatasetSample(0.0, 0.0, 0.0, 0.0, 0.0, false, 0.5, 1.0, false, 0.0, 0.5, 0.0, 0.0, 0.0);
      }

      private static double getDouble(JsonObject obj, String key, double defaultValue) {
         if (obj.has(key)) {
            try {
               return obj.get(key).getAsDouble();
            } catch (Exception var5) {
               return defaultValue;
            }
         } else {
            return defaultValue;
         }
      }

      private static boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
         if (obj.has(key)) {
            try {
               JsonElement element = obj.get(key);
               if (element.isJsonPrimitive()) {
                  if (element.getAsJsonPrimitive().isBoolean()) {
                     return element.getAsBoolean();
                  }

                  if (element.getAsJsonPrimitive().isNumber()) {
                     return element.getAsDouble() > 0.5;
                  }
               }
            } catch (Exception var4) {
               return defaultValue;
            }
         }

         return defaultValue;
      }
   }
}
