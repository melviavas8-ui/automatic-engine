package daamky.ua.screens.mainmenu;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;
import daamky.gui.Modules;
import daamky.render.Gfx;
import daamky.render.Icons;
import daamky.render.MsdfText;
import daamky.render.SdfRect;
import daamky.render.Txt;
import daamky.ua.api.theme.ClientTheme;
import daamky.ua.api.theme.ThemeManager;

/** Daamky's themed title screen. Uses the same SDF flow controls as DaamkyClickGui. */
public final class DaamkyMainMenuScreen extends Screen {
    private static final float DESIGN_W = 960.0F;
    private static final float DESIGN_H = 540.0F;
    private static final float BUTTON_X = 42.0F;
    private static final float BUTTON_W = 282.0F;
    private static final float BUTTON_H = 36.0F;
    private static final float BUTTON_GAP = 7.0F;

    private final List<MenuButton> buttons = new ArrayList<>();
    private final List<ThemeHit> themeHits = new ArrayList<>();
    private float[] hover = new float[5];
    private long lastFrameNanos;
    private float uiScale = 1.0F;
    private float offsetX;
    private float offsetY;
    private float themeX;
    private float themeY;
    private float themeW;
    private float themeH;
    private Matrix3x2f matrix = new Matrix3x2f();

    public DaamkyMainMenuScreen() {
        super(Text.literal("Daamky"));
    }

    @Override
    protected void init() {
        this.lastFrameNanos = System.nanoTime();
        this.rebuildButtons();
    }

    private void rebuildButtons() {
        this.buttons.clear();
        float y = 304.0F;
        this.buttons.add(new MenuButton("multi", "СЕТЕВАЯ ИГРА", "Подключиться к серверу", BUTTON_X, y, BUTTON_W, BUTTON_H, true));
        y += BUTTON_H + BUTTON_GAP;
        this.buttons.add(new MenuButton("single", "ОДИНОЧНАЯ ИГРА", "Выбрать или создать мир", BUTTON_X, y, BUTTON_W, BUTTON_H, false));
        y += BUTTON_H + BUTTON_GAP;
        this.buttons.add(new MenuButton("accounts", "АККАУНТ-МЕНЕДЖЕР", "Управление игровыми профилями", BUTTON_X, y, BUTTON_W, BUTTON_H, false));
        y += BUTTON_H + 10.0F;
        this.buttons.add(new MenuButton("options", "НАСТРОЙКИ", "", BUTTON_X, y, (BUTTON_W - 7.0F) * 0.5F, 30.0F, false));
        this.buttons.add(new MenuButton("quit", "ВЫХОД", "", BUTTON_X + (BUTTON_W + 7.0F) * 0.5F, y, (BUTTON_W - 7.0F) * 0.5F, 30.0F, false));
        this.hover = new float[this.buttons.size()];
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // The full-screen background is rendered in render() so it can share the menu's theme state.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (context == null) {
            return;
        }

        Modules.refreshFromTarget();
        Gfx.ctx = context;
        Gfx.scissor = null;
        MsdfText.msdfMode = Modules.useMsdf();
        MsdfText.baseWeight = Modules.weight();
        context.createNewRootLayer();

        this.updateGeometry();
        DaamkyMenuBackground.render(context, this.width, this.height);

        float delta = this.frameDeltaSeconds();
        float mouseXDesign = (mouseX - this.offsetX) / this.uiScale;
        float mouseYDesign = (mouseY - this.offsetY) / this.uiScale;

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(this.offsetX, this.offsetY);
        matrices.scale(this.uiScale, this.uiScale);
        this.matrix = new Matrix3x2f(matrices);

        this.drawAccountChip();
        this.drawThemeWidget(mouseXDesign, mouseYDesign);
        this.drawHero();
        this.drawButtons(mouseXDesign, mouseYDesign, delta);
        this.drawFooter();

        matrices.popMatrix();
    }

    private void updateGeometry() {
        this.uiScale = Math.max(0.1F, Math.min(1.2F, Math.min(this.width / DESIGN_W, this.height / DESIGN_H)));
        this.offsetX = (this.width - DESIGN_W * this.uiScale) * 0.5F;
        this.offsetY = (this.height - DESIGN_H * this.uiScale) * 0.5F;
    }

    private void drawAccountChip() {
        String username = this.client != null && this.client.getSession() != null
            ? this.client.getSession().getUsername()
            : "Player";
        if (username == null || username.isBlank()) {
            username = "Player";
        }

        float x = 32.0F, y = 24.0F, w = 216.0F, h = 38.0F;
        SdfRect.fill(this.matrix, x, y, w, h, 10.0F, 0xB909111D);
        SdfRect.border(this.matrix, x, y, w, h, 10.0F, 0.8F, 0x4AFFFFFF);
        SdfRect.flowGlow(this.matrix, x + 8.0F, y + 8.0F, 22.0F, 22.0F, 7.0F, 4.0F, 72);
        SdfRect.flow(this.matrix, x + 8.0F, y + 8.0F, 22.0F, 22.0F, 7.0F, 0.95F);
        Txt.icon(this.matrix, Icons.PLAYER, x + 12.0F, y + 12.0F, 14.0F, 0xFFFFFFFF);
        Txt.reg(this.matrix, "ТЕКУЩИЙ АККАУНТ", x + 39.0F, y + 7.0F, 6.2F, 0xFF9EADBF);
        Txt.bold(this.matrix, username, x + 39.0F, y + 19.0F, 9.0F, 0xFFF5F8FF);
        SdfRect.fill(this.matrix, x + w - 17.0F, y + 15.0F, 4.0F, 4.0F, 2.0F, 0xFF72DCA4);
    }

    private void drawThemeWidget(float mouseX, float mouseY) {
        List<ClientTheme> themes = ThemeManager.themes();
        ClientTheme current = ThemeManager.current();
        this.themeX = 706.0F;
        this.themeY = 24.0F;
        this.themeW = 222.0F;
        this.themeH = 62.0F;
        this.themeHits.clear();

        SdfRect.fill(this.matrix, themeX, themeY, themeW, themeH, 10.0F, 0xB909111D);
        SdfRect.border(this.matrix, themeX, themeY, themeW, themeH, 10.0F, 0.8F, 0x4AFFFFFF);
        Txt.reg(this.matrix, "ТЕМА И ИНТЕРФЕЙС", themeX + 12.0F, themeY + 10.0F, 6.5F, 0xFF9EADBF);
        Txt.bold(this.matrix, current.name(), themeX + 12.0F, themeY + 25.0F, 10.0F, 0xFFF4F6FF);

        LocalTime now = LocalTime.now();
        String time = String.format("%02d:%02d", now.getHour(), now.getMinute());
        float timeWidth = Txt.wBold(time, 15.0F);
        Txt.bold(this.matrix, time, themeX + themeW - timeWidth - 12.0F, themeY + 17.0F, 15.0F, 0xFFF8FAFF);

        float dotY = themeY + 48.0F;
        float dotX = themeX + 12.0F;
        float dotGap = 17.0F;
        int shown = Math.min(ThemeManager.themes().size(), 11);
        for (int i = 0; i < shown; i++) {
            ClientTheme theme = themes.get(i);
            float x = dotX + i * dotGap;
            int color = 0xFF000000 | theme.accent().getRGB() & 0x00FFFFFF;
            SdfRect.fill(this.matrix, x, dotY, 8.0F, 6.0F, 3.0F, color);
            if (theme.name().equalsIgnoreCase(current.name())) {
                SdfRect.border(this.matrix, x - 1.5F, dotY - 1.5F, 11.0F, 9.0F, 4.0F, 0.8F, 0xFFEAF0FF);
            }
            this.themeHits.add(new ThemeHit(x - 2.0F, dotY - 3.0F, 12.0F, 12.0F, theme.name()));
        }
    }

    private void drawHero() {
        String username = this.client != null && this.client.getSession() != null
            ? this.client.getSession().getUsername()
            : "PLAYER";
        if (username == null || username.isBlank()) {
            username = "PLAYER";
        }
        username = username.toUpperCase(java.util.Locale.ROOT);

        Txt.bold(this.matrix, "РАДЫ ВИДЕТЬ ТЕБЯ СНОВА, " + username, 42.0F, 158.0F, 7.0F, accentColor(220));
        Txt.bold(this.matrix, "Добро пожаловать в", 42.0F, 187.0F, 19.0F, 0xFFF3F6FC);
        Txt.bold(this.matrix, "Daamky Client", 42.0F, 216.0F, 27.0F, 0xFFFFFFFF);
        Txt.reg(this.matrix, "Твой клиент. Твои настройки. Твой стиль игры.", 43.0F, 249.0F, 8.2F, 0xFFB4C0D0);
        SdfRect.flow(this.matrix, 42.0F, 276.0F, 58.0F, 2.0F, 1.0F, 0.95F);
        Txt.reg(this.matrix, "БЫСТРЫЙ ДОСТУП", 111.0F, 273.0F, 6.1F, 0xFFAAB7C8);
    }

    private void drawButtons(float mouseX, float mouseY, float delta) {
        for (int i = 0; i < this.buttons.size(); i++) {
            MenuButton button = this.buttons.get(i);
            boolean hovered = contains(mouseX, mouseY, button.x, button.y, button.width, button.height);
            this.hover[i] = approach(this.hover[i], hovered ? 1.0F : 0.0F, delta * 8.0F);
            float hv = this.hover[i];
            float radius = button.height <= 30.0F ? 8.0F : 9.0F;

            SdfRect.fill(this.matrix, button.x, button.y, button.width, button.height, radius, button.primary ? 0xCC11182A : 0xB90A1220);
            if (button.primary || hv > 0.015F) {
                float fillAlpha = button.primary ? 0.94F : 0.08F + hv * 0.38F;
                int glowAlpha = button.primary ? 92 : Math.round(hv * 100.0F);
                SdfRect.flowGlow(this.matrix, button.x - 1.5F, button.y - 1.5F, button.width + 3.0F, button.height + 3.0F, radius + 1.5F, 5.0F, glowAlpha);
                SdfRect.flow(this.matrix, button.x, button.y, button.width, button.height, radius, fillAlpha);
            }
            SdfRect.border(this.matrix, button.x, button.y, button.width, button.height, radius, 0.8F,
                button.primary ? accentColor(220) : alphaColor(accentColor(190), Math.round(50.0F + hv * 130.0F)));

            int textColor = button.primary || hv > 0.2F ? 0xFFFFFFFF : 0xFFE4EAF4;
            float textSize = button.height <= 30.0F ? 7.2F : 8.4F;
            float textY = button.y + (button.height - textSize) * 0.5F - 0.5F;
            Txt.bold(this.matrix, button.label, button.x + 13.0F, textY, textSize, textColor);

            if (button.subtitle != null && !button.subtitle.isEmpty()) {
                Txt.reg(this.matrix, button.subtitle, button.x + 13.0F, button.y + button.height - 11.0F, 5.2F,
                    button.primary ? 0xDDF5F4FF : 0xFF98A7BC);
            }
            Txt.icon(this.matrix, Icons.CHEVRON, button.x + button.width - 22.0F, button.y + (button.height - 13.0F) * 0.5F, 13.0F,
                button.primary || hv > 0.35F ? 0xFFFFFFFF : 0xFF9BAAC0);
        }
    }

    private void drawFooter() {
        float y = DESIGN_H - 23.0F;
        Txt.reg(this.matrix, "DAAMKY CLIENT  •  1.0 RELEASE", 32.0F, y, 6.0F, 0xFF9BA8BA);
        String version = "MINECRAFT 1.21.11  /  FABRIC";
        float width = Txt.wReg(version, 6.0F);
        Txt.reg(this.matrix, version, DESIGN_W - width - 32.0F, y, 6.0F, 0xFF8997AA);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        this.updateGeometry();
        float mouseX = (float)((click.x() - this.offsetX) / this.uiScale);
        float mouseY = (float)((click.y() - this.offsetY) / this.uiScale);

        for (ThemeHit hit : this.themeHits) {
            if (contains(mouseX, mouseY, hit.x, hit.y, hit.width, hit.height)) {
                ThemeManager.select(hit.themeName);
                Modules.refreshFromTarget();
                return true;
            }
        }

        for (MenuButton button : this.buttons) {
            if (contains(mouseX, mouseY, button.x, button.y, button.width, button.height)) {
                this.activate(button.id);
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private void activate(String id) {
        switch (id) {
            case "multi" -> this.client.setScreen(new MultiplayerScreen(this));
            case "single" -> this.client.setScreen(new SelectWorldScreen(this));
            case "accounts" -> this.client.setScreen(new AltManagerScreen());
            case "options" -> this.client.setScreen(new OptionsScreen(this, this.client.options));
            case "quit" -> this.client.scheduleStop();
            default -> { }
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void close() {
        // The title screen remains open until the player selects an explicit action.
    }

    private float frameDeltaSeconds() {
        long now = System.nanoTime();
        float delta = this.lastFrameNanos == 0L ? 0.016F : (float)(now - this.lastFrameNanos) / 1.0E9F;
        this.lastFrameNanos = now;
        return Math.min(delta, 0.05F);
    }

    private static boolean contains(float mx, float my, float x, float y, float width, float height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    private static float approach(float current, float target, float amount) {
        return current < target ? Math.min(target, current + amount) : Math.max(target, current - amount);
    }

    private static int accentColor(int alpha) {
        return alphaColor(ThemeManager.current().accent().getRGB(), alpha);
    }

    private static int alphaColor(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private record MenuButton(String id, String label, String subtitle, float x, float y, float width, float height, boolean primary) {
    }

    private record ThemeHit(float x, float y, float width, float height, String themeName) {
    }

}
