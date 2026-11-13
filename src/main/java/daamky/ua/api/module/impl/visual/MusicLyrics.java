package daamky.ua.api.module.impl.visual;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.visual.music.LrcParser;
import daamky.ua.api.module.impl.visual.music.LrcTrack;
import daamky.ua.api.module.impl.visual.music.LyricGlow;
import daamky.ua.api.module.impl.visual.music.LyricParticle3D;
import daamky.ua.api.module.impl.visual.music.OggStreamPlayer;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ButtonSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.pipeline.ClientPipelines;

/** Local OGG player with timestamped lyrics floating around the player in 3D. */
public final class MusicLyrics extends Module {
    private static final String NONE = "Не выбрано";
    private static final Identifier GLOW_TEXTURE = Identifier.of("daamky", "textures/particles/glow.png");
    private final Path musicDirectory = FabricLoader.getInstance().getGameDir().resolve("daamky").resolve("music");

    private final ModeSetting track = this.register(new ModeSetting("Track", "Выбери OGG-трек из папки daamky/music.", NONE, NONE));
    private final ButtonSetting refresh = this.register(new ButtonSetting("Обновить список", "Повторно просканировать папку с музыкой.", this::refreshTracks));
    private final ModeSetting animation = this.register(new ModeSetting("Animation", "Анимация появления и испарения строк.", "Vaporize", "Vaporize", "LyricFlow", "Typewriter", "PopScale", "KineticSlide", "Fade"));
    private final ColorSetting textColor = this.register(new ColorSetting("Text Color", "Цвет самого текста.", new Color(247, 250, 255, 255)));
    private final ModeSetting glowMode = this.register(new ModeSetting("Glow Mode", "Окраска свечения: один цвет, градиент слева направо или радужное.", "Gradient", "Custom", "Gradient", "Rainbow"));
    private final ColorSetting glowColor = this.register(new ColorSetting("Glow Color", "Основной цвет свечения.", new Color(120, 150, 255, 255)));
    private final ColorSetting glowColor2 = this.register(new ColorSetting("Glow Color 2", "Второй цвет градиента свечения.", new Color(255, 110, 200, 255)));
    private final NumberSetting glowIntensity = this.register(new NumberSetting("Glow Intensity", "Яркость свечения. 0 — выключено.", 1.2, 0.0, 3.0, 0.1));
    private final NumberSetting glowSize = this.register(new NumberSetting("Glow Size", "Радиус мягкого ореола вокруг текста.", 1.0, 0.4, 2.5, 0.1));
    private final NumberSetting glowCore = this.register(new NumberSetting("Glow Core", "Плотное свечение вплотную к буквам.", 1.0, 0.0, 2.0, 0.1));
    private final BooleanSetting glowPulse = this.register(new BooleanSetting("Glow Pulse", "Плавная пульсация свечения.", true));
    private final NumberSetting glowPulseSpeed = this.register(new NumberSetting("Pulse Speed", "Скорость пульсации свечения.", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting afterglow = this.register(new NumberSetting("Afterglow", "Сколько миллисекунд свечение остаётся после исчезновения строки.", 700.0, 0.0, 2000.0, 50.0));
    private final NumberSetting maxLines = this.register(new NumberSetting("Max Lines", "Максимум одновременно видимых строк.", 3.0, 1.0, 6.0, 1.0));
    private final NumberSetting minDistance = this.register(new NumberSetting("Min Distance", "Минимальная дистанция появления строк.", 2.0, 1.2, 4.0, 0.1));
    private final NumberSetting maxDistance = this.register(new NumberSetting("Max Distance", "Максимальная дистанция появления строк.", 4.5, 2.0, 7.0, 0.1));
    private final NumberSetting arcSpread = this.register(new NumberSetting("Arc Spread", "Угол разброса строк слева направо.", 70.0, 20.0, 85.0, 5.0));
    private final NumberSetting floatHeight = this.register(new NumberSetting("Float Height", "Высота вертикального движения текста.", 0.5, 0.1, 2.0, 0.1));

    private final OggStreamPlayer player = new OggStreamPlayer();
    private final LyricGlow glowStyle = new LyricGlow();
    private final List<LyricParticle3D> activeParticles = new ArrayList<>();
    private List<LrcTrack> lyrics = List.of();
    private long startedAtNanos;
    private long durationMillis;
    private int currentLine;
    private boolean validated;

    public MusicLyrics() {
        super("Music Lyrics", "Воспроизводит OGG и показывает синхронизированный текст вокруг игрока.", ModuleCategory.VISUAL);
        this.glowColor.visibleWhen(() -> !this.glowMode.is("Rainbow"));
        this.glowColor2.visibleWhen(() -> this.glowMode.is("Gradient"));
        this.glowPulseSpeed.visibleWhen(this.glowPulse::getValue);
        this.refreshTracks();
    }

    private void refreshTracks() {
        try {
            Files.createDirectories(this.musicDirectory);
            List<String> names = new ArrayList<>();
            names.add(NONE);
            try (var files = Files.list(this.musicDirectory)) {
                files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ogg"))
                    .map(path -> path.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(names::add);
            }
            this.track.replaceModes(names.toArray(String[]::new));
        } catch (IOException exception) {
            this.track.replaceModes(NONE);
            this.message("Не удалось прочитать папку daamky/music: " + safeMessage(exception));
        }
    }

    @Override
    protected void onEnable() {
        this.validated = false;
        this.currentLine = 0;
        this.lyrics = List.of();
        this.activeParticles.clear();
    }

    @Override
    protected void onDisable() {
        this.player.close();
        this.lyrics = List.of();
        this.activeParticles.clear();
        this.currentLine = 0;
        this.durationMillis = 0L;
        this.validated = false;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (!this.validated) {
            this.validated = true;
            if (!this.startSelectedTrack(client)) {
                this.setEnabled(false);
                return;
            }
        }

        this.player.tick();
        long elapsed = (System.nanoTime() - this.startedAtNanos) / 1_000_000L;
        this.spawnDueLyrics(elapsed);
        long now = System.currentTimeMillis();
        long afterglowMs = this.afterglow.getValue().longValue();
        this.activeParticles.removeIf(particle -> particle.isDead(now, afterglowMs));

        boolean durationReached = this.durationMillis > 0L && elapsed >= this.durationMillis;
        if (durationReached && this.player.isDrained()) this.setEnabled(false);
    }

    private boolean startSelectedTrack(MinecraftClient client) {
        String selected = this.track.getValue();
        if (selected == null || selected.isBlank() || NONE.equals(selected)) {
            this.message("Music Lyrics: сначала выбери трек в настройках.");
            return false;
        }
        if (!selected.toLowerCase(java.util.Locale.ROOT).endsWith(".ogg")) {
            this.message("Music Lyrics: поддерживаются только файлы .ogg.");
            return false;
        }
        Path audio = this.musicDirectory.resolve(selected).normalize();
        if (!audio.startsWith(this.musicDirectory) || !Files.isRegularFile(audio)
            || !audio.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ogg")) {
            this.message("Music Lyrics: выбранный OGG-файл не найден в daamky/music.");
            return false;
        }
        Path lrc = this.musicDirectory.resolve(stripExtension(audio.getFileName().toString()) + ".lrc");
        if (!Files.isRegularFile(lrc)) {
            this.message("Music Lyrics: рядом с треком не найден файл " + lrc.getFileName() + ".");
            return false;
        }
        try {
            this.lyrics = LrcParser.read(lrc);
            if (this.lyrics.isEmpty()) {
                this.message("Music Lyrics: в " + lrc.getFileName() + " нет строк с корректными тайм-кодами.");
                return false;
            }
            this.player.open(audio);
            this.startedAtNanos = System.nanoTime();
            this.durationMillis = this.player.getDurationMillis();
            this.currentLine = 0;
            this.activeParticles.clear();
            return true;
        } catch (Exception | LinkageError exception) {
            this.message("Music Lyrics: не удалось запустить трек: " + safeMessage(exception));
            this.player.close();
            return false;
        }
    }

    private void spawnDueLyrics(long elapsed) {
        if (mc.player == null || mc.gameRenderer == null) return;
        long now = System.currentTimeMillis();
        while (this.currentLine < this.lyrics.size()) {
            LrcTrack line = this.lyrics.get(this.currentLine);
            if (elapsed < line.timeMillis()) break;

            long nextStart = this.currentLine + 1 < this.lyrics.size()
                ? this.lyrics.get(this.currentLine + 1).timeMillis()
                : line.timeMillis() + 3500L;
            long lineDuration = Math.max(1400L, Math.min(5500L, nextStart - line.timeMillis()));
            long age = elapsed - line.timeMillis();
            if (age < lineDuration && !line.text().isBlank()) {
                Vec3d position = this.findSpawnPosition();
                if (position != null) {
                    int maxAllowed = (int) Math.round(this.maxLines.getValue());
                    while (this.countTextAlive(now) >= maxAllowed) {
                        boolean removed = false;
                        for (int i = 0; i < this.activeParticles.size(); i++) {
                            if (this.activeParticles.get(i).isTextAlive(now)) {
                                this.activeParticles.remove(i);
                                removed = true;
                                break;
                            }
                        }
                        if (!removed) break;
                    }
                    this.activeParticles.add(new LyricParticle3D(
                        line.text(), position, now - age, lineDuration, this.floatHeight.getFloat()
                    ));
                }
            }
            this.currentLine++;
        }
    }

    private int countTextAlive(long now) {
        int count = 0;
        for (LyricParticle3D particle : this.activeParticles) {
            if (particle.isTextAlive(now)) count++;
        }
        return count;
    }

    private void syncGlowStyle() {
        LyricGlow g = this.glowStyle;
        g.textColor = this.textColor.getValue().getRGB() & 0xFFFFFF;
        g.colorA = this.glowColor.getValue().getRGB() & 0xFFFFFF;
        g.colorB = this.glowColor2.getValue().getRGB() & 0xFFFFFF;
        g.rainbow = this.glowMode.is("Rainbow");
        g.gradient = this.glowMode.is("Gradient");
        g.intensity = this.glowIntensity.getFloat();
        g.size = this.glowSize.getFloat();
        g.core = this.glowCore.getFloat();
        g.pulse = this.glowPulse.getValue();
        g.pulseSpeed = this.glowPulseSpeed.getFloat();
        g.afterglowMs = this.afterglow.getValue().longValue();
    }

    /** Places lyric text in a forward-facing arc, matching the reference KineticLyrics layout. */
    private Vec3d findSpawnPosition() {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d eye = mc.player.getEyePos();
        double spread = this.arcSpread.getValue();
        double min = this.minDistance.getValue();
        double max = Math.max(min + 0.5, this.maxDistance.getValue());
        Vec3d best = null;
        double bestSeparation = -1.0;

        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = Math.toRadians(camera.getYaw() + ThreadLocalRandom.current().nextDouble(-spread, spread));
            double distance = ThreadLocalRandom.current().nextDouble(min, max);
            double yOffset = ThreadLocalRandom.current().nextDouble(-0.25, 0.55);
            Vec3d candidate = new Vec3d(
                eye.x - Math.sin(angle) * distance,
                eye.y + yOffset,
                eye.z + Math.cos(angle) * distance
            );

            if (this.activeParticles.isEmpty()) return candidate;
            double nearest = Double.MAX_VALUE;
            for (LyricParticle3D particle : this.activeParticles) {
                nearest = Math.min(nearest, candidate.distanceTo(particle.getBasePosition()));
            }
            if (nearest >= 1.35) return candidate;
            if (nearest > bestSeparation) {
                bestSeparation = nearest;
                best = candidate;
            }
        }
        return best;
    }

    @SubscribeEvent
    private void onWorldRender(WorldRenderEvent event) {
        if (!this.isEnabled() || this.activeParticles.isEmpty() || mc.player == null || mc.world == null || mc.gameRenderer == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        VertexConsumerProvider.Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
        MatrixStack matrices = event.getStack();
        long now = System.currentTimeMillis();

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(770, 771, 1, 0);
        GlStateManager._disableCull();
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(515);
        GlStateManager._depthMask(false);
        try {
            this.syncGlowStyle();
            String mode = this.animation.getValue();

            // Pass 1: soft glow sprites behind the text (flushed first so the text always lands on top).
            RenderLayer glowLayer = ClientPipelines.BLOOM_ESP.apply(GLOW_TEXTURE);
            VertexConsumer glowBuffer = consumers.getBuffer(glowLayer);
            for (LyricParticle3D particle : this.activeParticles) {
                particle.renderGlow(matrices, camera, mc.textRenderer, mode, glowBuffer, now, this.glowStyle);
            }
            consumers.draw(glowLayer);

            // Pass 2: the crisp text itself.
            for (LyricParticle3D particle : this.activeParticles) {
                particle.renderText(matrices, camera, mc.textRenderer, mode, consumers, now, this.glowStyle);
            }
            consumers.draw();
        } finally {
            GlStateManager._depthMask(true);
        }
    }

    private void message(String text) {
        if (mc.inGameHud != null) mc.inGameHud.getChatHud().addMessage(Text.literal("§b[Daamky] §f" + text));
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
