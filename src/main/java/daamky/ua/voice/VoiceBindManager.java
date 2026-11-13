package daamky.ua.voice;

import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.DataLine.Info;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import daamky.ua.api.config.ConfigManager;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleManager;

public final class VoiceBindManager {
   public static final boolean FEATURE_ENABLED = false;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final VoiceBindManager INSTANCE = new VoiceBindManager();
   private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000.0F, 16, 1, true, false);
   private static final int FRAME_BYTES = 640;
   private static final int PRE_ROLL_FRAMES = 9;
   private static final int MIN_UTTERANCE_BYTES = 8000;
   private static final int MAX_UTTERANCE_BYTES = 96000;
   private static final long COMMAND_WINDOW_MS = 3500L;
   private static final long TOGGLE_COOLDOWN_MS = 900L;
   private final AtomicLong captureGeneration = new AtomicLong();
   private volatile ModuleManager moduleManager;
   private volatile Path rootDirectory;
   private volatile List<VoiceBindManager.Template> wakeTemplates = List.of();
   private volatile Map<String, List<VoiceBindManager.Template>> moduleTemplates = Map.of();
   private volatile Map<String, Double> moduleThresholds = Map.of();
   private volatile boolean backgroundEnabled = true;
   private volatile boolean wakeWordRequired = true;
   private volatile double wakeThreshold = 1.08;
   private volatile boolean captureRunning;
   private volatile TargetDataLine captureLine;
   private volatile VoiceBindManager.RecordingRequest recordingRequest;
   private volatile String status = "Голосовой бинд готов";
   private volatile long lastToggleAt;
   private volatile VoiceBindManager.RecognitionStage recognitionStage = VoiceBindManager.RecognitionStage.WAKE_WORD;
   private volatile long commandDeadline;

   private VoiceBindManager() {
   }

   public static VoiceBindManager getInstance() {
      return INSTANCE;
   }

   public synchronized void init(ModuleManager manager) {
      this.moduleManager = manager;
      this.rootDirectory = ConfigManager.systemDirectory().resolve("voice-binds");

      try {
         Files.createDirectories(this.wakeDirectory());
         Files.createDirectories(this.modulesDirectory());
         this.backgroundEnabled = this.loadEnabled();
         this.wakeWordRequired = this.loadWakeWordRequired();
         this.reloadTemplates();
      } catch (IOException var3) {
         this.status = "Не удалось открыть папку голосовых биндов";
         LOGGER.error("Could not initialize voice binds", var3);
      }

      if (this.backgroundEnabled && this.hasRecognitionTemplates()) {
         this.ensureCaptureStarted();
      }
   }

   public boolean isBackgroundEnabled() {
      return this.backgroundEnabled;
   }

   public synchronized void setBackgroundEnabled(boolean enabled) {
      this.backgroundEnabled = enabled;
      this.saveEnabled();
      this.recognitionStage = VoiceBindManager.RecognitionStage.WAKE_WORD;
      this.commandDeadline = 0L;
      this.status = enabled ? "Фоновое распознавание включено" : "Фоновое распознавание выключено";
      if (enabled) {
         this.ensureCaptureStarted();
      } else if (this.recordingRequest == null) {
         this.stopCapture();
      }
   }

   public boolean isWakeWordRequired() {
      return this.wakeWordRequired;
   }

   public synchronized void setWakeWordRequired(boolean required) {
      this.wakeWordRequired = required;
      this.saveWakeWordRequired();
      this.recognitionStage = VoiceBindManager.RecognitionStage.WAKE_WORD;
      this.commandDeadline = 0L;
      this.status = required ? "Ключевое слово обязательно" : "Ключевое слово выключено — назовите модуль";
      if (this.backgroundEnabled && this.hasRecognitionTemplates()) {
         this.ensureCaptureStarted();
      } else if (this.recordingRequest == null) {
         this.stopCapture();
      }
   }

   public synchronized boolean beginRecording(VoiceBindManager.RecordTarget target, Module module) {
      if (target == VoiceBindManager.RecordTarget.MODULE_NAME && module == null) {
         return false;
      } else if (this.recordingRequest != null) {
         this.cancelRecording();
         return false;
      } else {
         String moduleKey = module == null ? "" : this.stableModuleKey(module);
         this.recordingRequest = new VoiceBindManager.RecordingRequest(target, moduleKey, System.currentTimeMillis());
         this.recognitionStage = VoiceBindManager.RecognitionStage.WAKE_WORD;
         this.status = target == VoiceBindManager.RecordTarget.WAKE_WORD ? "Говорите ключевое слово..." : "Произнесите название модуля...";
         this.ensureCaptureStarted();
         return true;
      }
   }

   public synchronized void cancelRecording() {
      this.recordingRequest = null;
      this.status = "Запись отменена";
      if (!this.backgroundEnabled) {
         this.stopCapture();
      }
   }

   public boolean isRecording() {
      return this.recordingRequest != null;
   }

   public String getStatus() {
      return this.status;
   }

   public int getWakeRecordingCount() {
      return this.wakeTemplates.size();
   }

   public int getModuleRecordingCount(Module module) {
      return module == null ? 0 : this.moduleTemplates.getOrDefault(this.stableModuleKey(module), List.of()).size();
   }

   public synchronized void resetWakeRecordings() {
      this.deleteWavFiles(this.wakeDirectory());
      this.reloadTemplatesQuietly();
      this.recognitionStage = VoiceBindManager.RecognitionStage.WAKE_WORD;
      this.status = "Записи ключевого слова удалены";
      if (this.wakeWordRequired && this.wakeTemplates.isEmpty() && this.recordingRequest == null) {
         this.stopCapture();
      }
   }

   public synchronized void resetModuleRecordings(Module module) {
      if (module != null) {
         this.deleteWavFiles(this.moduleDirectory(this.stableModuleKey(module)));
         this.reloadTemplatesQuietly();
         this.status = "Записи модуля удалены";
      }
   }

   private synchronized void ensureCaptureStarted() {
      if (!this.captureRunning && (this.backgroundEnabled || this.recordingRequest != null)) {
         this.captureRunning = true;
         long generation = this.captureGeneration.incrementAndGet();
         Thread thread = new Thread(() -> this.captureLoop(generation), "daamky Voice Bind");
         thread.setDaemon(true);
         thread.start();
      }
   }

   private synchronized void stopCapture() {
      this.captureGeneration.incrementAndGet();
      this.captureRunning = false;
      TargetDataLine line = this.captureLine;
      this.captureLine = null;
      if (line != null) {
         try {
            line.stop();
         } catch (Exception var3) {
         }

         line.close();
      }
   }

   private void captureLoop(long generation) {
      TargetDataLine line = null;

      try {
         Info info = new Info(TargetDataLine.class, AUDIO_FORMAT);
         line = (TargetDataLine)AudioSystem.getLine(info);
         line.open(AUDIO_FORMAT, 16000);
         line.start();
         this.captureLine = line;
         this.status = this.recordingRequest != null ? this.status : "Слушаю ключевое слово";
         this.captureFrames(line, generation);
      } catch (Exception var19) {
         this.status = "Микрофон недоступен: " + readableMessage(var19);
         this.recordingRequest = null;
         LOGGER.error("Voice bind microphone could not be opened", var19);
      } finally {
         if (line != null) {
            try {
               line.stop();
            } catch (Exception var18) {
            }

            line.close();
         }

         synchronized (this) {
            if (this.captureLine == line) {
               this.captureLine = null;
            }

            this.captureRunning = false;
         }

         if (this.shouldCapture()) {
            this.ensureCaptureStarted();
         }
      }
   }

   private void captureFrames(TargetDataLine line, long generation) {
      byte[] frame = new byte[640];
      ArrayDeque<byte[]> preRoll = new ArrayDeque<>();
      ByteArrayOutputStream utterance = new ByteArrayOutputStream();
      boolean speaking = false;
      int voicedFrames = 0;
      int silentFrames = 0;
      double noiseFloor = 0.006;

      while (this.captureGeneration.get() == generation && this.shouldCapture()) {
         int read = line.read(frame, 0, frame.length);
         if (read > 0) {
            byte[] current = read == frame.length ? (byte[])frame.clone() : copyOf(frame, read);
            double rms = rms(current);
            double speechThreshold = Math.max(0.0075, noiseFloor * 1.9);
            boolean voiced = rms >= speechThreshold;
            if (!speaking) {
               noiseFloor = clamp(noiseFloor * 0.985 + rms * 0.015, 0.0025, 0.03);
               preRoll.addLast(current);

               while (preRoll.size() > 9) {
                  preRoll.removeFirst();
               }

               voicedFrames = voiced ? voicedFrames + 1 : 0;
               if (voicedFrames >= 2) {
                  speaking = true;
                  silentFrames = 0;
                  utterance.reset();

                  for (byte[] previous : preRoll) {
                     utterance.writeBytes(previous);
                  }

                  preRoll.clear();
                  this.status = this.recordingRequest != null ? "Записываю..." : this.status;
               }
            } else {
               utterance.writeBytes(current);
               silentFrames = voiced ? 0 : silentFrames + 1;
               boolean longEnough = utterance.size() >= 8000;
               boolean complete = longEnough && silentFrames >= 9 || utterance.size() >= 96000;
               if (complete) {
                  this.processUtterance(utterance.toByteArray());
                  speaking = false;
                  voicedFrames = 0;
                  silentFrames = 0;
                  utterance.reset();
               }
            }

            VoiceBindManager.RecordingRequest request = this.recordingRequest;
            if (request != null && !speaking && System.currentTimeMillis() - request.startedAt() > 8000L) {
               this.recordingRequest = null;
               this.status = "Голос не обнаружен — попробуйте ещё раз";
            }
         }
      }
   }

   private boolean shouldCapture() {
      return this.recordingRequest != null || this.backgroundEnabled && this.hasRecognitionTemplates();
   }

   private boolean hasRecognitionTemplates() {
      return this.wakeWordRequired ? !this.wakeTemplates.isEmpty() : !this.moduleTemplates.isEmpty();
   }

   private void processUtterance(byte[] pcm) {
      VoiceBindManager.RecordingRequest request = this.recordingRequest;
      if (request != null) {
         this.saveRecording(request, pcm);
      } else if (this.backgroundEnabled && this.hasRecognitionTemplates()) {
         float[][] features = VoiceFeatureExtractor.extract(pcm);
         if (features.length >= 8) {
            if (!this.wakeWordRequired) {
               VoiceBindManager.ModuleMatch direct = this.matchModule(features);
               if (this.acceptModule(direct)) {
                  this.toggle(direct.module());
               } else {
                  this.status = "Команда не распознана";
               }
            } else {
               long now = System.currentTimeMillis();
               if (this.recognitionStage == VoiceBindManager.RecognitionStage.MODULE_NAME && now > this.commandDeadline) {
                  this.recognitionStage = VoiceBindManager.RecognitionStage.WAKE_WORD;
               }

               if (this.recognitionStage == VoiceBindManager.RecognitionStage.MODULE_NAME) {
                  VoiceBindManager.ModuleMatch match = this.matchModule(features);
                  if (this.acceptModule(match)) {
                     this.toggle(match.module());
                  } else {
                     this.status = "Команда не распознана";
                     this.recognitionStage = VoiceBindManager.RecognitionStage.WAKE_WORD;
                  }
               } else {
                  VoiceFeatureExtractor.SubsequenceMatch wakeMatch = this.matchWake(features);
                  boolean wakeAccepted = wakeMatch.score() <= this.wakeThreshold && wakeMatch.startFrame() >= 0 && wakeMatch.startFrame() <= 8;
                  LOGGER.info(
                     "[VoiceBind] wake score={} threshold={} start={} accepted={}",
                     new Object[]{
                        String.format(Locale.ROOT, "%.3f", wakeMatch.score()),
                        String.format(Locale.ROOT, "%.3f", this.wakeThreshold),
                        wakeMatch.startFrame(),
                        wakeAccepted
                     }
                  );
                  if (wakeAccepted) {
                     float[][] remainder = VoiceFeatureExtractor.renormalize(VoiceFeatureExtractor.slice(features, wakeMatch.endFrame() + 1));
                     if (remainder.length >= 8) {
                        VoiceBindManager.ModuleMatch immediate = this.matchModule(remainder);
                        if (this.acceptModule(immediate)) {
                           this.toggle(immediate.module());
                        } else {
                           this.status = "Команда после ключевого слова не распознана";
                           this.recognitionStage = VoiceBindManager.RecognitionStage.WAKE_WORD;
                        }
                     } else if (wakeMatch.score() > Math.max(0.9, this.wakeThreshold - 0.07)) {
                        this.recognitionStage = VoiceBindManager.RecognitionStage.WAKE_WORD;
                     } else {
                        this.recognitionStage = VoiceBindManager.RecognitionStage.MODULE_NAME;
                        this.commandDeadline = now + 3500L;
                        this.status = "Ключевое слово услышано — назовите модуль";
                     }
                  }
               }
            }
         }
      }
   }

   private VoiceFeatureExtractor.SubsequenceMatch matchWake(float[][] input) {
      VoiceFeatureExtractor.SubsequenceMatch best = VoiceFeatureExtractor.SubsequenceMatch.NONE;

      for (VoiceBindManager.Template template : this.wakeTemplates) {
         VoiceFeatureExtractor.SubsequenceMatch candidate = VoiceFeatureExtractor.subsequence(template.features(), input);
         if (candidate.score() < best.score()) {
            best = candidate;
         }
      }

      return best;
   }

   private VoiceBindManager.ModuleMatch matchModule(float[][] features) {
      ModuleManager manager = this.moduleManager;
      if (manager == null) {
         return new VoiceBindManager.ModuleMatch(null, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0, 0.0);
      } else {
         Module bestModule = null;
         double best = Double.POSITIVE_INFINITY;
         double second = Double.POSITIVE_INFINITY;
         int bestTemplateCount = 0;
         double bestThreshold = 0.0;

         for (Module module : manager.getModules()) {
            String moduleKey = this.stableModuleKey(module);
            List<VoiceBindManager.Template> templates = this.moduleTemplates.get(moduleKey);
            if (templates != null && !templates.isEmpty()) {
               List<Double> scores = new ArrayList<>(templates.size());

               for (VoiceBindManager.Template template : templates) {
                  scores.add(VoiceFeatureExtractor.distance(template.features(), features));
               }

               scores.sort(Double::compareTo);
               double moduleScore = scores.getFirst();
               if (scores.size() >= 2) {
                  moduleScore = moduleScore * 0.72 + scores.get(1) * 0.28;
               }

               if (moduleScore < best) {
                  second = best;
                  best = moduleScore;
                  bestModule = module;
                  bestTemplateCount = templates.size();
                  Double configuredThreshold = this.moduleThresholds.get(moduleKey);
                  bestThreshold = configuredThreshold == null ? this.calibratedThreshold(templates) : configuredThreshold;
               } else if (moduleScore < second) {
                  second = moduleScore;
               }
            }
         }

         return new VoiceBindManager.ModuleMatch(bestModule, best, second, bestTemplateCount, bestThreshold);
      }
   }

   private boolean acceptModule(VoiceBindManager.ModuleMatch match) {
      if (match != null && match.module() != null) {
         boolean hasAlternative = Double.isFinite(match.secondScore());
         double margin = hasAlternative ? match.secondScore() - match.score() : 0.0;
         double ratio = hasAlternative ? match.score() / Math.max(1.0E-4, match.secondScore()) : 1.0;
         boolean separated = hasAlternative ? ratio <= 0.965 || margin >= 0.075 : match.score() <= Math.min(0.88, match.threshold());
         double effectiveThreshold = match.templatesUsed() <= 1 ? Math.min(0.92, match.threshold()) : match.threshold();
         boolean accepted = match.score() <= effectiveThreshold && separated;
         LOGGER.info(
            "[VoiceBind] candidate={} score={} second={} samples={} threshold={} accepted={}",
            new Object[]{
               match.module().getName(),
               String.format(Locale.ROOT, "%.3f", match.score()),
               Double.isFinite(match.secondScore()) ? String.format(Locale.ROOT, "%.3f", match.secondScore()) : "none",
               match.templatesUsed(),
               String.format(Locale.ROOT, "%.3f", effectiveThreshold),
               accepted
            }
         );
         return accepted;
      } else {
         return false;
      }
   }

   private double calibratedThreshold(List<VoiceBindManager.Template> templates) {
      if (templates != null && templates.size() > 1) {
         List<Double> recordedVariation = new ArrayList<>(templates.size());

         for (int i = 0; i < templates.size(); i++) {
            List<Double> neighbours = new ArrayList<>(templates.size() - 1);

            for (int j = 0; j < templates.size(); j++) {
               if (i != j) {
                  neighbours.add(VoiceFeatureExtractor.distance(templates.get(i).features(), templates.get(j).features()));
               }
            }

            neighbours.sort(Double::compareTo);
            if (!neighbours.isEmpty()) {
               double variation = neighbours.getFirst();
               if (neighbours.size() >= 2) {
                  variation = variation * 0.72 + neighbours.get(1) * 0.28;
               }

               recordedVariation.add(variation);
            }
         }

         if (recordedVariation.isEmpty()) {
            return 1.12;
         } else {
            recordedVariation.sort(Double::compareTo);
            int percentileIndex = Math.min(recordedVariation.size() - 1, Math.round((float)(recordedVariation.size() - 1) * 0.85F));
            double learnedSpread = recordedVariation.get(percentileIndex);
            return clamp(learnedSpread + 0.18, 0.9, 1.15);
         }
      } else {
         return 1.12;
      }
   }

   private void toggle(Module module) {
      long now = System.currentTimeMillis();
      this.recognitionStage = VoiceBindManager.RecognitionStage.WAKE_WORD;
      this.commandDeadline = 0L;
      if (module != null && now - this.lastToggleAt >= 900L) {
         this.lastToggleAt = now;
         this.status = "Команда: " + module.getName();
         MinecraftClient minecraft = MinecraftClient.getInstance();
         if (minecraft != null) {
            minecraft.execute(module::toggleByVoice);
         }
      }
   }

   private synchronized void saveRecording(VoiceBindManager.RecordingRequest request, byte[] pcm) {
      if (this.recordingRequest == request) {
         this.recordingRequest = null;
         float[][] features = VoiceFeatureExtractor.extract(pcm);
         if (features.length < 8) {
            this.status = "Запись слишком короткая — повторите";
            if (!this.backgroundEnabled) {
               this.stopCapture();
            }
         } else {
            Path directory = request.target() == VoiceBindManager.RecordTarget.WAKE_WORD ? this.wakeDirectory() : this.moduleDirectory(request.moduleKey());

            try {
               Files.createDirectories(directory);
               Path output = directory.resolve("sample-" + System.currentTimeMillis() + ".wav");
               this.writeWav(output, pcm);
               this.reloadTemplates();
               this.status = request.target() == VoiceBindManager.RecordTarget.WAKE_WORD
                  ? "Вариант ключевого слова сохранён"
                  : "Вариант названия модуля сохранён";
            } catch (IOException var6) {
               this.status = "Не удалось сохранить запись";
               LOGGER.error("Could not save voice bind recording", var6);
            }

            if (this.backgroundEnabled) {
               this.ensureCaptureStarted();
            } else {
               this.stopCapture();
            }
         }
      }
   }

   private void reloadTemplatesQuietly() {
      try {
         this.reloadTemplates();
      } catch (IOException var2) {
         this.status = "Не удалось обновить голосовые записи";
         LOGGER.error("Could not reload voice bind templates", var2);
      }
   }

   private void reloadTemplates() throws IOException {
      List<VoiceBindManager.Template> wake = this.loadTemplates(this.wakeDirectory());
      Map<String, List<VoiceBindManager.Template>> modules = new HashMap<>();
      Map<String, Double> thresholds = new HashMap<>();
      Path modulesRoot = this.modulesDirectory();
      Files.createDirectories(modulesRoot);

      try (Stream<Path> directories = Files.list(modulesRoot)) {
         for (Path directory : directories.filter(x$0 -> Files.isDirectory(x$0)).toList()) {
            List<VoiceBindManager.Template> templates = this.loadTemplates(directory);
            if (!templates.isEmpty()) {
               String moduleKey = directory.getFileName().toString();
               modules.put(moduleKey, templates);
               thresholds.put(moduleKey, this.calibratedThreshold(templates));
            }
         }
      }

      this.wakeTemplates = List.copyOf(wake);
      this.wakeThreshold = this.calibratedWakeThreshold(wake);
      this.moduleTemplates = Map.copyOf(modules);
      this.moduleThresholds = Map.copyOf(thresholds);
   }

   private double calibratedWakeThreshold(List<VoiceBindManager.Template> templates) {
      if (templates != null && templates.size() > 1) {
         List<Double> distances = new ArrayList<>();

         for (int i = 0; i < templates.size(); i++) {
            double nearest = Double.POSITIVE_INFINITY;

            for (int j = 0; j < templates.size(); j++) {
               if (i != j) {
                  nearest = Math.min(nearest, VoiceFeatureExtractor.distance(templates.get(i).features(), templates.get(j).features()));
               }
            }

            if (Double.isFinite(nearest)) {
               distances.add(nearest);
            }
         }

         distances.sort(Double::compareTo);
         double typical = distances.get(Math.min(distances.size() - 1, Math.round((float)(distances.size() - 1) * 0.75F)));
         return clamp(typical + 0.12, 1.0, 1.14);
      } else {
         return 1.02;
      }
   }

   private List<VoiceBindManager.Template> loadTemplates(Path directory) throws IOException {
      Files.createDirectories(directory);
      List<VoiceBindManager.Template> result = new ArrayList<>();

      try (Stream<Path> files = Files.list(directory)) {
         for (Path file : files.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".wav"))
            .sorted(Comparator.comparing(Path::toString))
            .toList()) {
            try {
               byte[] pcm = this.readWav(file);
               float[][] features = VoiceFeatureExtractor.extract(pcm);
               if (features.length >= 8) {
                  result.add(new VoiceBindManager.Template(features));
               }
            } catch (Exception var9) {
               LOGGER.warn("Skipping invalid voice template {}", file, var9);
            }
         }
      }

      return result;
   }

   private void deleteWavFiles(Path directory) {
      if (directory != null && Files.isDirectory(directory)) {
         try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".wav")).toList()) {
               Files.deleteIfExists(file);
            }
         } catch (IOException var7) {
            this.status = "Не удалось удалить записи";
            LOGGER.error("Could not delete voice bind recordings from {}", directory, var7);
         }
      }
   }

   private byte[] readWav(Path file) throws Exception {
      byte[] var5;
      try (
         AudioInputStream source = AudioSystem.getAudioInputStream(file.toFile());
         AudioInputStream converted = AudioSystem.getAudioInputStream(AUDIO_FORMAT, source);
         ByteArrayOutputStream output = new ByteArrayOutputStream();
      ) {
         converted.transferTo(output);
         var5 = output.toByteArray();
      }

      return var5;
   }

   private void writeWav(Path file, byte[] pcm) throws IOException {
      try (
         ByteArrayInputStream bytes = new ByteArrayInputStream(pcm);
         AudioInputStream stream = new AudioInputStream(bytes, AUDIO_FORMAT, (long)pcm.length / 2L);
      ) {
         AudioSystem.write(stream, Type.WAVE, file.toFile());
      }
   }

   private boolean loadEnabled() {
      Path file = this.settingsFile();
      if (!Files.exists(file)) {
         return true;
      } else {
         try {
            return !Files.readString(file, StandardCharsets.UTF_8).trim().equalsIgnoreCase("false");
         } catch (IOException var3) {
            return true;
         }
      }
   }

   private void saveEnabled() {
      try {
         Files.createDirectories(this.rootDirectory);
         Files.writeString(
            this.settingsFile(),
            Boolean.toString(this.backgroundEnabled),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
         );
      } catch (IOException var2) {
         LOGGER.error("Could not save voice bind state", var2);
      }
   }

   private boolean loadWakeWordRequired() {
      Path file = this.wakeWordSettingFile();
      if (!Files.exists(file)) {
         return true;
      } else {
         try {
            return !Files.readString(file, StandardCharsets.UTF_8).trim().equalsIgnoreCase("false");
         } catch (IOException var3) {
            return true;
         }
      }
   }

   private void saveWakeWordRequired() {
      try {
         Files.createDirectories(this.rootDirectory);
         Files.writeString(
            this.wakeWordSettingFile(),
            Boolean.toString(this.wakeWordRequired),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
         );
      } catch (IOException var2) {
         LOGGER.error("Could not save voice wake-word state", var2);
      }
   }

   private Path wakeDirectory() {
      return this.rootDirectory.resolve("wake-word");
   }

   private Path modulesDirectory() {
      return this.rootDirectory.resolve("modules");
   }

   private Path moduleDirectory(String moduleKey) {
      return this.modulesDirectory().resolve(moduleKey);
   }

   private Path settingsFile() {
      return this.rootDirectory.resolve("enabled.txt");
   }

   private Path wakeWordSettingFile() {
      return this.rootDirectory.resolve("wake-word-required.txt");
   }

   private String stableModuleKey(Module module) {
      String source = module.getCategory().name() + ":" + module.getName();
      return Base64.getUrlEncoder().withoutPadding().encodeToString(source.getBytes(StandardCharsets.UTF_8));
   }

   private static double rms(byte[] pcm) {
      if (pcm.length < 2) {
         return 0.0;
      } else {
         double sum = 0.0;
         int samples = pcm.length / 2;

         for (int i = 0; i < samples; i++) {
            int low = pcm[i * 2] & 255;
            int high = pcm[i * 2 + 1];
            double sample = (double)((short)(low | high << 8)) / 32768.0;
            sum += sample * sample;
         }

         return Math.sqrt(sum / (double)samples);
      }
   }

   private static byte[] copyOf(byte[] source, int length) {
      byte[] result = new byte[length];
      System.arraycopy(source, 0, result, 0, length);
      return result;
   }

   private static double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   private static String readableMessage(Exception exception) {
      String message = exception.getMessage();
      return message != null && !message.isBlank() ? message : exception.getClass().getSimpleName();
   }

   private static record ModuleMatch(Module module, double score, double secondScore, int templatesUsed, double threshold) {
   }

   private static enum RecognitionStage {
      WAKE_WORD,
      MODULE_NAME;

      private RecognitionStage() {
      }
   }

   public static enum RecordTarget {
      WAKE_WORD,
      MODULE_NAME;

      private RecordTarget() {
      }
   }

   private static record RecordingRequest(VoiceBindManager.RecordTarget target, String moduleKey, long startedAt) {
   }

   private static record Template(float[][] features) {
   }
}
