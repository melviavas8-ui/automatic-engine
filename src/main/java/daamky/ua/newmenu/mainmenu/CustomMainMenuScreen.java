package daamky.ua.newmenu.mainmenu;

import com.mojang.authlib.GameProfile;
import daamky.ua.newmenu.multibot.BotProxyConfig;
import daamky.ua.newmenu.render.Render2D;
import daamky.ua.newmenu.render.font.Fonts;
import daamky.ua.newmenu.render.font.MsdfFont;
import daamky.ua.newmenu.render.liqvid.BlurRenderer;
import daamky.ua.newmenu.render.util.RenderColor;
import daamky.ua.newmenu.render.util.ScissorStack;
import java.awt.Color;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public final class CustomMainMenuScreen extends Screen {

    public enum MenuState {
        MAIN,
        ACCOUNTS,
        PROXY,
        ADD_PROXY_MODAL
    }

    public enum ProxyFilterTab {
        ALL,
        SOCKS5,
        SAVED
    }

    private static final Identifier ICON_SINGLEPLAYER = Identifier.of("daamkymenu", "icons/singleplayer_download.png");
    private static final Identifier ICON_MULTIPLAYER = Identifier.of("daamkymenu", "icons/multiplayer_download.png");
    private static final Identifier ICON_SETTINGS = Identifier.of("daamkymenu", "icons/settings.png");
    private static final Identifier ICON_ACCOUNTS = Identifier.of("daamkymenu", "icons/profile.png");
    private static final Identifier ICON_ADD = Identifier.of("daamkymenu", "icons/add.png");
    private static final Identifier ICON_SPARKLES = Identifier.of("daamkymenu", "icons/sparkles.png");
    private static final Identifier ICON_TRASH = Identifier.of("daamkymenu", "icons/trash_preset.png");
    private static final Identifier ICON_SEARCH = Identifier.of("daamkymenu", "icons/search.png");
    private static final Identifier ICON_PROXY = Identifier.of("daamkymenu", "icons/zap.png");
    private static final Identifier ICON_WATERMARK_LOGO = Identifier.of("daamkymenu", "mainmenu/daamky-mark.png");

    private static final String[] MAIN_SUBTITLES = {
            "Твой следующий мир уже ждёт.",
            "Готов к новому приключению?",
            "Выбери мир и начинай исследовать.",
            "Собери свою историю в блоках."
    };

    // State machine & transitions
    private MenuState currentState = MenuState.MAIN;
    private MenuState targetState = MenuState.MAIN;
    private float transitionProgress = 1.0F; // 1.0 = fully in currentState

    private long screenOpenTime;
    private long lastFrameTime = System.currentTimeMillis();
    private int subtitleIndex = -1;
    private int previousSubtitleIndex = -1;
    private long subtitleSwitchAt;
    private long nextSubtitleSwitchAt;

    // Main Menu hover animations (MATERIAL ONLY - ZERO POSITIONAL MOVEMENT)
    private float hoverTopBadge = 0.0F;
    private float hoverSingleplayer = 0.0F;
    private float hoverMultiplayer = 0.0F;
    private float hoverBtnAccounts = 0.0F;
    private float hoverBtnSettings = 0.0F;
    private float hoverBtnProxy = 0.0F;
    private float hoverBtnExit = 0.0F;
    private float hoverWinClose = 0.0F;
    private float hoverWinMin = 0.0F;

    private boolean isExiting = false;
    private long exitTime = 0L;

    // Toast notification
    private String statusToast = "";
    private long statusToastTime = 0L;
    private int toastColor = 0xFF52D68A;

    // Account skin cache
    private final Map<String, Identifier> skinCache = new HashMap<>();

    // -------------------------------------------------------------
    // Accounts Screen State
    // -------------------------------------------------------------
    private String accountInput = "";
    private boolean accountInputFocused = false;
    private String accountSearch = "";
    private boolean accountSearchFocused = false;
    private String selectedAccount = "";
    private long lastAccountClickTime = 0L;
    private String lastClickedAccount = "";
    private float accountsScrollY = 0.0F;
    private float targetAccountsScrollY = 0.0F;

    private float microsoftBtnHover = 0.0F;
    private float createBtnHover = 0.0F;
    private float randomBtnHover = 0.0F;
    private float favoriteBtnHover = 0.0F;
    private float deleteBtnHover = 0.0F;
    private float loginAsBtnHover = 0.0F;
    private float deleteAllBtnHover = 0.0F;
    private float backBtnHover = 0.0F;

    // -------------------------------------------------------------
    // Proxy Screen State
    // -------------------------------------------------------------
    private ProxyFilterTab proxyFilterTab = ProxyFilterTab.ALL;
    private String proxySearch = "";
    private boolean proxySearchFocused = false;
    private float proxyScrollY = 0.0F;
    private float targetProxyScrollY = 0.0F;

    private float proxyAddBtnHover = 0.0F;
    private float proxyImportHover = 0.0F;
    private float proxyExportHover = 0.0F;
    private float proxyCheckAllHover = 0.0F;

    // Add Proxy Modal State
    private String modalCombinedInput = "";
    private boolean modalCombinedFocused = false;
    private BotProxyConfig.Type modalCombinedType = BotProxyConfig.Type.SOCKS5;

    private float modalSaveHover = 0.0F;
    private float modalCancelHover = 0.0F;

    public CustomMainMenuScreen() {
        super(Text.of("daamky"));
    }

    @Override
    protected void init() {
        super.init();
        screenOpenTime = System.currentTimeMillis();
        selectedAccount = AccountManager.getInstance().getActiveAccount();
        lastAccountClickTime = 0L;
        lastClickedAccount = "";
        isExiting = false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Handled by MenuBackgroundRenderer
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - lastFrameTime) / 1000.0F);
        lastFrameTime = now;

        // Smooth state transition lerp
        if (targetState != currentState) {
            transitionProgress = Math.max(0.0F, transitionProgress - dt * 9.0F);
            if (transitionProgress <= 0.01F) {
                currentState = targetState;
                transitionProgress = 0.0F;
            }
        } else {
            transitionProgress = Math.min(1.0F, transitionProgress + dt * 9.0F);
        }

        // Smooth scroll interpolation
        accountsScrollY += (targetAccountsScrollY - accountsScrollY) * Math.min(1.0F, dt * 14.0F);
        proxyScrollY += (targetProxyScrollY - proxyScrollY) * Math.min(1.0F, dt * 14.0F);

        // Exit scheduling
        if (isExiting && now - exitTime > 320L) {
            if (this.client != null) {
                this.client.scheduleStop();
            }
            return;
        }

        // 1. Draw universal blurred night village background with ambient particles
        MenuBackgroundRenderer.getInstance().render(context, this.width, this.height, 1.0F);

        // 2. Render active view based on state (Stable Geometry - Alpha fade only)
        float currentViewAlpha = easeOutQuart(transitionProgress);

        switch (currentState) {
            case MAIN -> renderMainMenuView(context, mouseX, mouseY, dt, currentViewAlpha);
            case ACCOUNTS -> renderAccountsView(context, mouseX, mouseY, dt, currentViewAlpha);
            case PROXY -> renderProxyView(context, mouseX, mouseY, dt, currentViewAlpha);
            case ADD_PROXY_MODAL -> {
                renderProxyView(context, mouseX, mouseY, dt, 0.35F);
                renderAddProxyModal(context, mouseX, mouseY, dt, currentViewAlpha);
            }
        }

        // 3. Render Status Toast Notification
        renderStatusToast(context);
    }

    // =========================================================================
    // 1. MAIN MENU VIEW
    // =========================================================================

    private void renderMainMenuView(DrawContext context, int mouseX, int mouseY, float dt, float alpha) {
        if (alpha <= 0.01F) return;

        long now = System.currentTimeMillis();
        float centerX = (float) Math.round(this.width * 0.5F);
        float baseCenterY = (float) Math.round(this.height * 0.44F);

        // Hover animation smoothing (Material properties only)
        hoverTopBadge = lerp(hoverTopBadge, isTopBadgeHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);
        hoverSingleplayer = lerp(hoverSingleplayer, isSingleplayerHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);
        hoverMultiplayer = lerp(hoverMultiplayer, isMultiplayerHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);
        hoverBtnAccounts = lerp(hoverBtnAccounts, isBtnAccountsHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);
        hoverBtnSettings = lerp(hoverBtnSettings, isBtnSettingsHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);
        hoverBtnProxy = lerp(hoverBtnProxy, isBtnProxyHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);
        hoverBtnExit = lerp(hoverBtnExit, isExitBtnHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);
        hoverWinClose = lerp(hoverWinClose, isWinCloseHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);
        hoverWinMin = lerp(hoverWinMin, isWinMinHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);

        // -------------------------------------------------------------
        // A. Top Identity: F Mark + Username Pill [Shonus ∨]
        // -------------------------------------------------------------
        float logoY = (float) Math.round(baseCenterY - 100.0F);
        drawWatermarkMark(context, centerX, logoY, alpha, now);

        String activeUser = AccountManager.getInstance().getActiveAccount();
        float badgeW = usernameBadgeWidth(activeUser);
        float badgeH = 20.0F;
        float badgeX = (float) Math.round(centerX - badgeW * 0.5F);
        float badgeY = (float) Math.round(logoY + 28.0F);
        drawUsernameBadge(context, activeUser, badgeX, badgeY, badgeW, badgeH, alpha, hoverTopBadge);

        // -------------------------------------------------------------
        // B. Title "daamky" + Subtitle (Normalized Vertical Rhythm)
        // -------------------------------------------------------------
        float titleY = (float) Math.round(badgeY + 34.0F);
        Fonts.SEMIBOLD.drawCenteredString(context, "daamky", centerX, titleY, 23.0F,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

        // Subtitles rotation with smooth cross-fade
        if (subtitleIndex < 0) {
            subtitleIndex = new Random().nextInt(MAIN_SUBTITLES.length);
            subtitleSwitchAt = now;
            nextSubtitleSwitchAt = now + 5400L;
        } else if (now >= nextSubtitleSwitchAt) {
            previousSubtitleIndex = subtitleIndex;
            int nextIndex = subtitleIndex;
            while (MAIN_SUBTITLES.length > 1 && nextIndex == subtitleIndex) {
                nextIndex = new Random().nextInt(MAIN_SUBTITLES.length);
            }
            subtitleIndex = nextIndex;
            subtitleSwitchAt = now;
            nextSubtitleSwitchAt = now + 5000L + new Random().nextInt(2500);
        }
        float subtitleBlend = Math.clamp((now - subtitleSwitchAt) / 450.0F, 0.0F, 1.0F);
        float subY = (float) Math.round(titleY + 29.0F);
        if (previousSubtitleIndex >= 0 && subtitleBlend < 1.0F) {
            Fonts.REGULAR.drawCenteredString(context, MAIN_SUBTITLES[previousSubtitleIndex], centerX,
                    subY, 9.5F,
                    RenderColor.multiplyAlpha(0xFFA0B2C6, alpha * (1.0F - subtitleBlend)));
        }
        Fonts.REGULAR.drawCenteredString(context, MAIN_SUBTITLES[subtitleIndex], centerX,
                subY, 9.5F,
                RenderColor.multiplyAlpha(0xFFA0B2C6, alpha * subtitleBlend));

        // -------------------------------------------------------------
        // C. Row 1: [▶ Одиночная]  [👥 Мультиплеер]
        // -------------------------------------------------------------
        float colW = 248.0F;
        float gap = 8.0F;
        float halfW = (float) Math.round((colW - gap) * 0.5F); // 120px
        float r1Y = (float) Math.round(subY + 25.0F);
        float btnH = 31.0F;

        float btnSingleX = (float) Math.round(centerX - colW * 0.5F);
        float btnMultiX = (float) Math.round(btnSingleX + halfW + gap);

        // Button 1: Singleplayer (GLASS_PRIMARY - Stronger material presence)
        drawGroupButton(context, btnSingleX, r1Y, halfW, btnH, 8.0F,
                hoverSingleplayer, alpha, GlassStyle.PRIMARY,
                ICON_SINGLEPLAYER, 100, 13.0F, "Одиночная",
                Fonts.MEDIUM, 10.5F, 7.0F, 0xFFCBD5E1, 0xFFE2E8F0);

        // Button 2: Multiplayer (GLASS_PRIMARY - Stronger material presence)
        drawGroupButton(context, btnMultiX, r1Y, halfW, btnH, 8.0F,
                hoverMultiplayer, alpha, GlassStyle.PRIMARY,
                ICON_MULTIPLAYER, 100, 13.0F, "Мультиплеер",
                Fonts.MEDIUM, 10.5F, 7.0F, 0xFFCBD5E1, 0xFFE2E8F0);

        // -------------------------------------------------------------
        // D. Row 2: Secondary Icon Buttons: [ 👤 ] [ ⚙ ] [ ⚡ ] [ 🚪 ]
        // (4 Identical 31x31 Utility Buttons, normalized optical icon size)
        // -------------------------------------------------------------
        float r2Y = (float) Math.round(r1Y + btnH + 8.0F);
        float iconBtnSize = 31.0F;
        float iconGap = 7.0F;

        float startRow2X = (float) Math.round(centerX - (iconBtnSize * 4.0F + iconGap * 3.0F) * 0.5F);

        // Button 1: Accounts (👤) - 13.5px optical normalization
        drawIconSquareButton(context, startRow2X, r2Y, iconBtnSize, iconBtnSize,
                ICON_ACCOUNTS, 128, 13.5F, hoverBtnAccounts, alpha, 0xFFCAD4DF);

        // Button 2: Options / Settings (⚙) - 13.0px optical normalization
        drawIconSquareButton(context, startRow2X + iconBtnSize + iconGap, r2Y, iconBtnSize, iconBtnSize,
                ICON_SETTINGS, 128, 13.0F, hoverBtnSettings, alpha, 0xFFCAD4DF);

        // Button 3: Proxy (⚡) - 13.0px optical normalization
        drawIconSquareButton(context, startRow2X + (iconBtnSize + iconGap) * 2.0F, r2Y, iconBtnSize, iconBtnSize,
                ICON_PROXY, 128, 13.0F, hoverBtnProxy, alpha, 0xFFCAD4DF);

        // Button 4: Exit Button (🚪) - 13.0px vector door logout icon with muted red
        drawExitSquareButton(context, startRow2X + (iconBtnSize + iconGap) * 3.0F, r2Y, iconBtnSize, iconBtnSize,
                hoverBtnExit, alpha);

        // -------------------------------------------------------------
        // E. Top-Right Window Controls: [ — ]  [ ✕ ]
        // -------------------------------------------------------------
        float winX = (float) Math.round(this.width - 48.0F);
        float winY = 12.0F;
        Fonts.REGULAR.drawString(context, "—", winX, winY, 11.0F,
                RenderColor.multiplyAlpha(RenderColor.interpolate(0x6094A3B8, 0xFFFFFFFF, hoverWinMin), alpha));
        Fonts.REGULAR.drawString(context, "✕", winX + 16.0F, winY, 11.0F,
                RenderColor.multiplyAlpha(RenderColor.interpolate(0x6094A3B8, 0xFFF87171, hoverWinClose), alpha));

        // -------------------------------------------------------------
        // F. Footer Text (Clean Centered, subtle & crisp typography)
        // -------------------------------------------------------------
        float footerY = (float) Math.round(this.height - 28.0F);
        Fonts.REGULAR.drawCenteredString(context, "Продукты daamky не связаны с Mojang.", centerX, footerY, 8.0F,
                RenderColor.multiplyAlpha(0x70FFFFFF, alpha));
        Fonts.REGULAR.drawCenteredString(context, "Политика конфиденциальности  ·  Условия использования", centerX, footerY + 11.0F, 7.8F,
                RenderColor.multiplyAlpha(0x50FFFFFF, alpha));
    }

    /**
     * Centralized Glass Hierarchy Presets:
     * PRIMARY: Strongest normal interactive glass body (Singleplayer, Multiplayer, Big Login actions)
     * SECONDARY: Balanced medium glass body (Utility buttons, Back, Shonus pill, inputs, sub-actions)
     * CARD: Subtle large-surface container glass (Cards, dialog backdrops, inactive grid items)
     * ACTIVE_CARD: Restrained cool cyan accent for active selections
     * DANGER: Muted crimson glass for exit / delete actions
     */
    public enum GlassStyle {
        PRIMARY(
                new Color(15, 18, 24, 130), new Color(20, 26, 36, 155),
                new Color(255, 255, 255, 38), new Color(255, 255, 255, 65),
                0.0018F, 0.0022F, 1.15F, 1.40F, 0.75F, 1.05F
        ),
        SECONDARY(
                new Color(13, 16, 21, 100), new Color(17, 22, 30, 125),
                new Color(255, 255, 255, 28), new Color(255, 255, 255, 50),
                0.0015F, 0.0019F, 0.95F, 1.20F, 0.60F, 0.85F
        ),
        CARD(
                new Color(11, 14, 18, 115), new Color(14, 18, 24, 135),
                new Color(255, 255, 255, 20), new Color(255, 255, 255, 35),
                0.0012F, 0.0015F, 0.80F, 1.00F, 0.45F, 0.65F
        ),
        ACTIVE_CARD(
                new Color(13, 18, 26, 125), new Color(16, 24, 34, 145),
                new Color(56, 189, 248, 65), new Color(56, 189, 248, 90),
                0.0018F, 0.0022F, 1.15F, 1.35F, 0.75F, 1.00F
        ),
        DANGER(
                new Color(24, 13, 16, 115), new Color(32, 16, 20, 145),
                new Color(244, 63, 94, 55), new Color(244, 63, 94, 85),
                0.0017F, 0.0021F, 1.10F, 1.30F, 0.70F, 0.95F
        );

        final Color idleTint, hoverTint, idleEdge, hoverEdge;
        final float idleDistort, hoverDistort, idleEdgeLight, hoverEdgeLight, idleShine, hoverShine;

        GlassStyle(Color idleTint, Color hoverTint, Color idleEdge, Color hoverEdge,
                   float idleDistort, float hoverDistort, float idleEdgeLight, float hoverEdgeLight,
                   float idleShine, float hoverShine) {
            this.idleTint = idleTint;
            this.hoverTint = hoverTint;
            this.idleEdge = idleEdge;
            this.hoverEdge = hoverEdge;
            this.idleDistort = idleDistort;
            this.hoverDistort = hoverDistort;
            this.idleEdgeLight = idleEdgeLight;
            this.hoverEdgeLight = hoverEdgeLight;
            this.idleShine = idleShine;
            this.hoverShine = hoverShine;
        }
    }

    private void drawGlassCard(DrawContext context, float x, float y, float w, float h, float radius, float alpha) {
        drawGlassCard(context, x, y, w, h, radius, alpha, GlassStyle.CARD);
    }

    private void drawGlassCard(DrawContext context, float x, float y, float w, float h, float radius, float alpha, GlassStyle style) {
        if (alpha <= 0.01F) return;
        BlurRenderer.drawLiquidGlass(
                context,
                Math.round(x), Math.round(y), Math.round(w), Math.round(h), radius,
                style.idleTint, style.idleEdge, alpha,
                style.idleDistort, style.idleEdgeLight, style.idleShine, 0.0F, 0.0F
        );
    }

    private void drawGlassButton(DrawContext context, float x, float y, float w, float h, float radius,
                                 float hover, float alpha, GlassStyle style) {
        if (alpha <= 0.01F) return;
        Color tint = interpolateColor(style.idleTint, style.hoverTint, hover);
        Color edge = interpolateColor(style.idleEdge, style.hoverEdge, hover);
        float distort = lerp(style.idleDistort, style.hoverDistort, hover);
        float edgeLight = lerp(style.idleEdgeLight, style.hoverEdgeLight, hover);
        float shine = lerp(style.idleShine, style.hoverShine, hover);

        BlurRenderer.drawLiquidGlass(
                context,
                Math.round(x), Math.round(y), Math.round(w), Math.round(h), radius,
                tint, edge, alpha,
                distort, edgeLight, shine, hover, 0.0F
        );
    }

    private static Color interpolateColor(Color c1, Color c2, float t) {
        float f = Math.clamp(t, 0.0F, 1.0F);
        int r = Math.round(lerp(c1.getRed(), c2.getRed(), f));
        int g = Math.round(lerp(c1.getGreen(), c2.getGreen(), f));
        int b = Math.round(lerp(c1.getBlue(), c2.getBlue(), f));
        int a = Math.round(lerp(c1.getAlpha(), c2.getAlpha(), f));
        return new Color(r, g, b, a);
    }

    /**
     * Shared optical text centering helper:
     * Derives exact mathematical horizontal center and baseline-adjusted vertical center once.
     */
    private void drawCenteredText(DrawContext context, MsdfFont font, String text,
                                  float x, float y, float w, float h,
                                  float fontSize, int color) {
        if (text == null || text.isEmpty() || (color >>> 24) == 0) return;
        float textW = font.getWidth(text, fontSize);
        float tx = (float) Math.round(x + (w - textW) * 0.5F);
        float ty = (float) Math.round(y + (h - fontSize) * 0.5F - fontSize * 0.08F);
        font.drawString(context, text, tx, ty, fontSize, color);
    }

    /**
     * Shared icon + label button renderer:
     * Centers [ icon ][ gap ][ label ] as ONE coherent horizontal content group with matching vertical center.
     */
    private void drawGroupButton(DrawContext context, float x, float y, float w, float h, float radius,
                                 float hover, float alpha, GlassStyle style,
                                 Identifier imageIcon, int iconTextureSize, float iconSize,
                                 String text, MsdfFont font, float fontSize, float gap,
                                 int idleIconCol, int idleTextCol) {
        if (alpha <= 0.01F) return;

        float rx = (float) Math.round(x);
        float ry = (float) Math.round(y);
        float rw = (float) Math.round(w);
        float rh = (float) Math.round(h);

        if (hover > 0.01F) {
            Render2D.drawShadow(context, rx, ry + 1.0F, rw, rh, radius, 5.0F,
                    RenderColor.multiplyAlpha(0x30000000, alpha * hover));
        }

        drawGlassButton(context, rx, ry, rw, rh, radius, hover, alpha, style);

        boolean hasIcon = imageIcon != null;
        float textW = (text != null && !text.isEmpty()) ? font.getWidth(text, fontSize) : 0.0F;
        float totalW = (hasIcon ? iconSize : 0.0F) + (hasIcon && textW > 0 ? gap : 0.0F) + textW;

        float startX = (float) Math.round(rx + (rw - totalW) * 0.5F);
        float centerY = (float) Math.round(ry + rh * 0.5F);

        int iconCol = RenderColor.interpolate(idleIconCol, 0xFFFFFFFF, hover);
        int textCol = RenderColor.interpolate(idleTextCol, 0xFFFFFFFF, hover);

        if (hasIcon) {
            float iconX = startX;
            float iconY = (float) Math.round(centerY - iconSize * 0.5F);
            drawMenuIcon(context, imageIcon, iconTextureSize, iconX, iconY, iconSize,
                    RenderColor.multiplyAlpha(iconCol, alpha));
        }

        if (text != null && !text.isEmpty()) {
            float textX = startX + (hasIcon ? iconSize + gap : 0.0F);
            float textY = (float) Math.round(centerY - fontSize * 0.5F - fontSize * 0.08F);
            font.drawString(context, text, textX, textY, fontSize,
                    RenderColor.multiplyAlpha(textCol, alpha));
        }
    }

    private void drawBackArrow(DrawContext context, float startX, float centerY, float width, int color) {
        float arrowLeft = (float) Math.round(startX);
        float arrowRight = (float) Math.round(startX + width);
        float arrowY = (float) Math.round(centerY);
        float headSize = 3.5F;
        float stroke = 1.25F;
        Render2D.drawLine(context, arrowLeft, arrowY, arrowRight, arrowY, stroke, color);
        Render2D.drawLine(context, arrowLeft, arrowY, arrowLeft + headSize, arrowY - headSize, stroke, color);
        Render2D.drawLine(context, arrowLeft, arrowY, arrowLeft + headSize, arrowY + headSize, stroke, color);
    }

    private void drawBackButton(DrawContext context, float backX, float backY, float backW, float backH,
                                float hoverBack, float alpha) {
        if (alpha <= 0.01F) return;

        float bx = (float) Math.round(backX);
        float by = (float) Math.round(backY);
        float bw = (float) Math.round(backW);
        float bh = (float) Math.round(backH);

        if (hoverBack > 0.01F) {
            Render2D.drawShadow(context, bx, by + 1.0F, bw, bh, 6.0F, 5.0F,
                    RenderColor.multiplyAlpha(0x30000000, alpha * hoverBack));
        }
        drawGlassButton(context, bx, by, bw, bh, 6.0F, hoverBack, alpha, GlassStyle.SECONDARY);

        float arrowW = 8.0F;
        float gap = 5.0F;
        float fontSize = 9.5F;
        float textW = Fonts.MEDIUM.getWidth("Назад", fontSize);
        float totalW = arrowW + gap + textW;

        float startX = (float) Math.round(bx + (bw - totalW) * 0.5F);
        float centerY = (float) Math.round(by + bh * 0.5F);
        float textY = (float) Math.round(centerY - fontSize * 0.5F - fontSize * 0.08F);

        int color = RenderColor.multiplyAlpha(RenderColor.interpolate(0xFFCBD5E1, 0xFFFFFFFF, hoverBack), alpha);

        drawBackArrow(context, startX, centerY, arrowW, color);
        Fonts.MEDIUM.drawString(context, "Назад", startX + arrowW + gap, textY, fontSize, color);
    }

    private void drawExitSquareButton(DrawContext context, float x, float y, float w, float h,
                                      float hover, float alpha) {
        float radius = 8.0F;
        float bx = (float) Math.round(x);
        float by = (float) Math.round(y);
        float bw = (float) Math.round(w);
        float bh = (float) Math.round(h);

        if (hover > 0.01F) {
            Render2D.drawShadow(context, bx, by + 1.0F, bw, bh, radius, 5.0F,
                    RenderColor.multiplyAlpha(0x35E11D48, alpha * hover));
        }

        GlassStyle style = hover > 0.01F ? GlassStyle.DANGER : GlassStyle.SECONDARY;
        drawGlassButton(context, bx, by, bw, bh, radius, hover, alpha, style);

        float contentCenterX = (float) Math.round(bx + bw * 0.5F);
        float contentCenterY = (float) Math.round(by + bh * 0.5F);
        int iconColor = RenderColor.interpolate(0xFFF87171, 0xFFFFAEB9, hover);
        drawExitDoorIcon(context, contentCenterX, contentCenterY, 13.0F,
                RenderColor.multiplyAlpha(iconColor, alpha));
    }

    /**
     * Draws a crisp vector Door with Exit Arrow icon.
     */
    private void drawExitDoorIcon(DrawContext context, float centerX, float centerY, float size, int color) {
        if (context == null || (color >>> 24) == 0) return;

        float halfS = size * 0.5F;
        float stroke = 1.25F;

        // Door frame (U-bracket on the left side)
        float doorLeft = (float) Math.round(centerX - halfS * 0.85F);
        float doorRight = (float) Math.round(centerX - halfS * 0.05F);
        float doorTop = (float) Math.round(centerY - halfS * 0.85F);
        float doorBottom = (float) Math.round(centerY + halfS * 0.85F);

        // Top horizontal lintel
        Render2D.drawLine(context, doorLeft, doorTop, doorRight, doorTop, stroke, color);
        // Left vertical door post
        Render2D.drawLine(context, doorLeft, doorTop, doorLeft, doorBottom, stroke, color);
        // Bottom horizontal sill
        Render2D.drawLine(context, doorLeft, doorBottom, doorRight, doorBottom, stroke, color);

        // Arrow pointing out to the right
        float arrowLeft = (float) Math.round(centerX - halfS * 0.30F);
        float arrowRight = (float) Math.round(centerX + halfS * 0.90F);
        float arrowY = (float) Math.round(centerY);

        // Arrow shaft
        Render2D.drawLine(context, arrowLeft, arrowY, arrowRight, arrowY, stroke, color);

        // Arrowhead (two clean diagonal lines pointing right)
        float headSize = size * 0.28F;
        Render2D.drawLine(context, arrowRight, arrowY, arrowRight - headSize, arrowY - headSize, stroke, color);
        Render2D.drawLine(context, arrowRight, arrowY, arrowRight - headSize, arrowY + headSize, stroke, color);
    }

    /**
     * Draws a crisp vector 5-point Star icon.
     */
    private void drawStarIcon(DrawContext context, float centerX, float centerY, float size, boolean filled, int color) {
        if (context == null || (color >>> 24) == 0) return;
        float rOuter = size * 0.5F;
        float rInner = rOuter * 0.40F;
        float stroke = 1.25F;

        float[] xp = new float[10];
        float[] yp = new float[10];

        for (int i = 0; i < 10; i++) {
            float r = (i % 2 == 0) ? rOuter : rInner;
            double angle = Math.toRadians(-90.0 + i * 36.0);
            xp[i] = centerX + (float) (Math.cos(angle) * r);
            yp[i] = centerY + (float) (Math.sin(angle) * r);
        }

        for (int i = 0; i < 10; i++) {
            int next = (i + 1) % 10;
            Render2D.drawLine(context, xp[i], yp[i], xp[next], yp[next], stroke, color);
        }

        if (filled) {
            float innerHalf = rInner * 0.85F;
            Render2D.drawRound(context, centerX - innerHalf, centerY - innerHalf, innerHalf * 2.0F, innerHalf * 2.0F, innerHalf, color);
        }
    }

    private void drawFavoriteGroupButton(DrawContext context, float x, float y, float w, float h, float radius,
                                         float hover, float alpha, GlassStyle style,
                                         boolean isFavorite,
                                         String text, MsdfFont font, float fontSize, float gap) {
        if (alpha <= 0.01F) return;

        float rx = (float) Math.round(x);
        float ry = (float) Math.round(y);
        float rw = (float) Math.round(w);
        float rh = (float) Math.round(h);

        if (hover > 0.01F) {
            Render2D.drawShadow(context, rx, ry + 1.0F, rw, rh, radius, 5.0F,
                    RenderColor.multiplyAlpha(0x30000000, alpha * hover));
        }

        drawGlassButton(context, rx, ry, rw, rh, radius, hover, alpha, style);

        float iconSize = 12.0F;
        float textW = (text != null && !text.isEmpty()) ? font.getWidth(text, fontSize) : 0.0F;
        float totalW = iconSize + gap + textW;

        float startX = (float) Math.round(rx + (rw - totalW) * 0.5F);
        float centerY = (float) Math.round(ry + rh * 0.5F);

        int iconIdle = isFavorite ? 0xFFF59E0B : 0xFFFFFFFF;
        int textIdle = isFavorite ? 0xFFFCD34D : 0xFFCAD4DF;
        int iconCol = RenderColor.interpolate(iconIdle, 0xFFFFFFFF, hover);
        int textCol = RenderColor.interpolate(textIdle, 0xFFFFFFFF, hover);

        float iconCenterX = (float) Math.round(startX + iconSize * 0.5F);
        drawStarIcon(context, iconCenterX, centerY, iconSize, isFavorite,
                RenderColor.multiplyAlpha(iconCol, alpha));

        if (text != null && !text.isEmpty()) {
            float textX = startX + iconSize + gap;
            float textY = (float) Math.round(centerY - fontSize * 0.5F - fontSize * 0.08F);
            font.drawString(context, text, textX, textY, fontSize,
                    RenderColor.multiplyAlpha(textCol, alpha));
        }
    }

    private void drawIconSquareButton(DrawContext context, float x, float y, float w, float h,
                                      Identifier imageIcon, int textureSize, float iconSize,
                                      float hover, float alpha, int iconColor) {
        float radius = 8.0F;
        float bx = (float) Math.round(x);
        float by = (float) Math.round(y);
        float bw = (float) Math.round(w);
        float bh = (float) Math.round(h);

        if (hover > 0.01F) {
            Render2D.drawShadow(context, bx, by + 1.0F, bw, bh, radius, 5.0F,
                    RenderColor.multiplyAlpha(0x30000000, alpha * hover));
        }

        drawGlassButton(context, bx, by, bw, bh, radius, hover, alpha, GlassStyle.SECONDARY);

        float contentCenterX = (float) Math.round(bx + bw * 0.5F);
        float contentCenterY = (float) Math.round(by + bh * 0.5F);
        int activeIconCol = RenderColor.interpolate(iconColor, 0xFFFFFFFF, hover);
        drawMenuIcon(context, imageIcon, textureSize,
                (float) Math.round(contentCenterX - iconSize * 0.5F),
                (float) Math.round(contentCenterY - iconSize * 0.5F),
                iconSize, RenderColor.multiplyAlpha(activeIconCol, alpha));
    }

    private void drawWatermarkMark(DrawContext context, float centerX, float y, float alpha, long now) {
        float size = 22.0F;
        float drawX = (float) Math.round(centerX - size * 0.5F);
        float drawY = (float) Math.round(y);

        if (context != null && context.getMatrices() != null) {
            Matrix3x2fStack stack = context.getMatrices();
            stack.pushMatrix();
            stack.translate(centerX, y + size * 0.5F);
            stack.rotate((float) Math.toRadians(-7.0f));
            stack.translate(-centerX, -(y + size * 0.5F));

            // Subtle steady cyan aura
            drawMenuIcon(context, ICON_WATERMARK_LOGO, 128, drawX - 1.0F,
                    drawY - 1.0F, size + 2.0F, RenderColor.multiplyAlpha(0x4038BDF8, alpha * 0.25F));
            drawMenuIcon(context, ICON_WATERMARK_LOGO, 128, drawX,
                    drawY, size, RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

            stack.popMatrix();
        } else {
            drawMenuIcon(context, ICON_WATERMARK_LOGO, 128, drawX,
                    drawY, size, RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
        }
    }

    private void drawUsernameBadge(DrawContext context, String username, float x, float y,
                                   float width, float height, float alpha, float hover) {
        if (alpha <= 0.01F) return;
        String label = username == null || username.isBlank() ? "Player" : username;
        float bx = (float) Math.round(x);
        float by = (float) Math.round(y);
        float bw = (float) Math.round(width);
        float bh = (float) Math.round(height);
        float radius = bh * 0.5F;

        drawGlassButton(context, bx, by, bw, bh, radius, hover, alpha, GlassStyle.SECONDARY);

        float fontSize = 9.5F;
        float textW = Fonts.MEDIUM.getWidth(label, fontSize);
        float chevW = 6.0F;
        float gap = 6.0F;
        float totalW = textW + gap + chevW;

        float startX = (float) Math.round(bx + (bw - totalW) * 0.5F);
        float centerY = (float) Math.round(by + bh * 0.5F);
        float textY = (float) Math.round(centerY - fontSize * 0.5F - fontSize * 0.08F);

        int textColor = RenderColor.multiplyAlpha(RenderColor.interpolate(0xFFE2E8F0, 0xFFFFFFFF, hover), alpha);
        int chevColor = RenderColor.multiplyAlpha(RenderColor.interpolate(0xFFA0B2C6, 0xFFE2E8F0, hover), alpha);

        Fonts.MEDIUM.drawString(context, label, startX, textY, fontSize, textColor);

        // Crisp vector dropdown chevron centered at centerY
        float cx = startX + textW + gap + chevW * 0.5F;
        Render2D.drawLine(context, cx - 2.5F, centerY - 1.5F, cx, centerY + 1.5F, 1.2F, chevColor);
        Render2D.drawLine(context, cx, centerY + 1.5F, cx + 2.5F, centerY - 1.5F, 1.2F, chevColor);
    }

    private float usernameBadgeWidth(String username) {
        String label = username == null || username.isBlank() ? "Player" : username;
        float fontSize = 9.5F;
        float textW = Fonts.MEDIUM.getWidth(label, fontSize);
        float chevW = 6.0F;
        float gap = 6.0F;
        return (float) Math.round(textW + gap + chevW + 20.0F);
    }

    private void drawMenuIcon(DrawContext context, Identifier icon, int textureSize,
                              float x, float y, float size, int color) {
        if (icon == null || textureSize <= 0 || (color >>> 24) == 0) return;
        int drawSize = Math.max(1, Math.round(size));
        int drawX = Math.round(x);
        int drawY = Math.round(y);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, drawX, drawY,
                0.0F, 0.0F, drawSize, drawSize, textureSize, textureSize, textureSize, textureSize, color);
    }

    private void drawBadge(DrawContext context, String text, float x, float y, int bg, int textColor, float h, float alpha) {
        float fontSize = 7.8F;
        float textW = Fonts.MEDIUM.getWidth(text, fontSize);
        float w = (float) Math.round(textW + 10.0F);
        float bx = (float) Math.round(x);
        float by = (float) Math.round(y);
        float bh = (float) Math.round(h);

        Render2D.drawRound(context, bx, by, w, bh, 4.0F, RenderColor.multiplyAlpha(bg, alpha));
        Render2D.drawRoundOutline(context, bx, by, w, bh, 4.0F, 1.0F, RenderColor.multiplyAlpha(0x18FFFFFF, alpha));
        drawCenteredText(context, Fonts.MEDIUM, text, bx, by, w, bh, fontSize, RenderColor.multiplyAlpha(textColor, alpha));
    }

    private String fitLabel(String text, float maxWidth, float size) {
        if (text == null || text.isEmpty() || Fonts.MEDIUM.getWidth(text, size) <= maxWidth) {
            return text == null ? "" : text;
        }
        String trimmed = text;
        while (!trimmed.isEmpty() && Fonts.MEDIUM.getWidth(trimmed + "…", size) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "…";
    }

    // =========================================================================
    // 2. ACCOUNTS VIEW
    // =========================================================================

    private void renderAccountsView(DrawContext context, int mouseX, int mouseY, float dt, float alpha) {
        if (alpha <= 0.01F) return;

        long now = System.currentTimeMillis();
        float centerX = (float) Math.round(this.width * 0.5F);
        float baseTopY = (float) Math.round(Math.max(16.0F, (this.height - 430.0F) * 0.5F));
        float cardW = 410.0F;
        float cardX = (float) Math.round(centerX - cardW * 0.5F);
        float gapX = 8.0F;
        float halfW = (float) Math.round((cardW - gapX) * 0.5F);

        // Back Button (Top Left) - Optically Centered with Vector Arrow
        backBtnHover = lerp(backBtnHover, isBackBtnHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);
        float backW = 74.0F;
        float backH = 24.0F;
        float backX = 24.0F;
        float backY = 24.0F;
        drawBackButton(context, backX, backY, backW, backH, backBtnHover, alpha);

        // Top identity: Watermark F mark + Active username pill
        String activeUser = AccountManager.getInstance().getActiveAccount();
        float badgeW = usernameBadgeWidth(activeUser);
        float badgeH = 20.0F;
        float badgeX = (float) Math.round(centerX - badgeW * 0.5F);
        float badgeY = (float) Math.round(baseTopY + 28.0F);
        drawWatermarkMark(context, centerX, baseTopY, alpha, now);
        drawUsernameBadge(context, activeUser, badgeX, badgeY, badgeW, badgeH, alpha, 0.0F);

        // Header Title: "Выберите аккаунт" (Normalized Vertical Rhythm)
        float titleY = (float) Math.round(badgeY + 34.0F);
        Fonts.SEMIBOLD.drawCenteredString(context, "Выберите аккаунт", centerX, titleY, 18.0F,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
        float subY = (float) Math.round(titleY + 24.0F);
        Fonts.REGULAR.drawCenteredString(context, "Войдите через Microsoft или выберите локальный профиль", centerX, subY, 9.8F,
                RenderColor.multiplyAlpha(0xFFAAB8C8, alpha));

        // -------------------------------------------------------------
        // 1. Microsoft Card (Top card - GLASS_CARD)
        // -------------------------------------------------------------
        float msY = (float) Math.round(subY + 20.0F);
        float msH = 92.0F;
        microsoftBtnHover = lerp(microsoftBtnHover, isMicrosoftBtnHovered(mouseX, mouseY, cardX, msY, cardW) ? 1.0F : 0.0F, dt * 10.0F);

        drawGlassCard(context, cardX, msY, cardW, msH, 8.0F, alpha, GlassStyle.CARD);

        // Microsoft 4-square logo (Red, Green, Blue, Yellow)
        float msLogoX = cardX + 14.0F;
        float msLogoY = msY + 14.0F;
        float sqSize = 6.0F;
        float sqGap = 2.0F;
        Render2D.drawRound(context, msLogoX, msLogoY, sqSize, sqSize, 1.0F, RenderColor.multiplyAlpha(0xFFF25022, alpha));
        Render2D.drawRound(context, msLogoX + sqSize + sqGap, msLogoY, sqSize, sqSize, 1.0F, RenderColor.multiplyAlpha(0xFF7FBA00, alpha));
        Render2D.drawRound(context, msLogoX, msLogoY + sqSize + sqGap, sqSize, sqSize, 1.0F, RenderColor.multiplyAlpha(0xFF00A4EF, alpha));
        Render2D.drawRound(context, msLogoX + sqSize + sqGap, msLogoY + sqSize + sqGap, sqSize, sqSize, 1.0F, RenderColor.multiplyAlpha(0xFFFFB900, alpha));

        // Microsoft Header Text
        Fonts.MEDIUM.drawString(context, "Microsoft", cardX + 34.0F, msY + 11.0F, 12.5F,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
        Fonts.REGULAR.drawString(context, "Лицензионный аккаунт", cardX + 34.0F, msY + 24.5F, 9.0F,
                RenderColor.multiplyAlpha(0xFFB4C0CE, alpha));

        // JAVA EDITION Tag Badge
        drawBadge(context, "JAVA EDITION", cardX + cardW - 74.0F, msY + 12.0F,
                0x18FFFFFF, 0xFF94A3B8, 14.0F, alpha);

        // "Войти через Microsoft" Button inside card (GLASS_PRIMARY - Distinct Interactive Hierarchy)
        float msBtnX = cardX + 14.0F;
        float msBtnY = msY + 44.0F;
        float msBtnW = cardW - 28.0F;
        float msBtnH = 32.0F;
        if (microsoftBtnHover > 0.01F) {
            Render2D.drawShadow(context, msBtnX, msBtnY + 1.0F, msBtnW, msBtnH, 6.0F, 6.0F,
                    RenderColor.multiplyAlpha(0x35000000, alpha * microsoftBtnHover));
        }
        drawGlassButton(context, msBtnX, msBtnY, msBtnW, msBtnH, 6.0F, microsoftBtnHover, alpha, GlassStyle.PRIMARY);

        // Mini MS logo inside button + Text (Single Centered Group)
        float msTextSize = 10.5F;
        float msTextW = Fonts.MEDIUM.getWidth("Войти через Microsoft", msTextSize);
        float btnSqSize = 4.5F;
        float btnSqGap = 1.5F;
        float btnLogoTotalW = btnSqSize * 2.0F + btnSqGap;
        float totalMsContentW = btnLogoTotalW + 8.0F + msTextW;
        float btnLogoX = (float) Math.round(msBtnX + (msBtnW - totalMsContentW) * 0.5F);
        float btnLogoY = (float) Math.round(msBtnY + (msBtnH - (btnSqSize * 2.0F + btnSqGap)) * 0.5F);

        Render2D.drawRound(context, btnLogoX, btnLogoY, btnSqSize, btnSqSize, 1.0F, RenderColor.multiplyAlpha(0xFFF25022, alpha));
        Render2D.drawRound(context, btnLogoX + btnSqSize + btnSqGap, btnLogoY, btnSqSize, btnSqSize, 1.0F, RenderColor.multiplyAlpha(0xFF7FBA00, alpha));
        Render2D.drawRound(context, btnLogoX, btnLogoY + btnSqSize + btnSqGap, btnSqSize, btnSqSize, 1.0F, RenderColor.multiplyAlpha(0xFF00A4EF, alpha));
        Render2D.drawRound(context, btnLogoX + btnSqSize + btnSqGap, btnLogoY + btnSqSize + btnSqGap, btnSqSize, btnSqSize, 1.0F, RenderColor.multiplyAlpha(0xFFFFB900, alpha));

        float msBtnTextY = (float) Math.round(msBtnY + (msBtnH - msTextSize) * 0.5F - msTextSize * 0.08F);
        Fonts.MEDIUM.drawString(context, "Войти через Microsoft", btnLogoX + btnLogoTotalW + 8.0F, msBtnTextY, msTextSize,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

        // -------------------------------------------------------------
        // 2. Section "ЛОКАЛЬНЫЕ И СОХРАНЁННЫЕ"
        // -------------------------------------------------------------
        float secY = msY + msH + 12.0F;
        Fonts.MEDIUM.drawString(context, "ЛОКАЛЬНЫЕ И СОХРАНЁННЫЕ", cardX + 2.0F, secY, 8.8F,
                RenderColor.multiplyAlpha(0xFF93A3B8, alpha));
        float secTextW = Fonts.MEDIUM.getWidth("ЛОКАЛЬНЫЕ И СОХРАНЁННЫЕ", 8.8F);
        Render2D.drawLine(context, cardX + secTextW + 8.0F, secY + 4.5F, cardX + cardW, secY + 4.5F, 1.0F,
                RenderColor.multiplyAlpha(0x18FFFFFF, alpha));

        // Input & Controls Bar: [Локальный ник] [Создать] [🔍 Поиск...] [🔀 Рандом]
        float inBarY = (float) Math.round(secY + 12.0F);
        float inH = 26.0F;

        // Nickname Input Box (GLASS_SECONDARY)
        float nickW = 112.0F;
        drawGlassCard(context, cardX, inBarY, nickW, inH, 6.0F, alpha, accountInputFocused ? GlassStyle.ACTIVE_CARD : GlassStyle.SECONDARY);
        float inTextY = (float) Math.round(inBarY + (inH - 9.5F) * 0.5F - 9.5F * 0.08F);
        if (accountInput.isEmpty() && !accountInputFocused) {
            Fonts.REGULAR.drawString(context, "Локальный ник", cardX + 8.0F, inTextY, 9.0F,
                    RenderColor.multiplyAlpha(0xFF8A9BA8, alpha));
        } else {
            String disp = accountInput + (accountInputFocused && (now / 500) % 2 == 0 ? "|" : "");
            Fonts.MEDIUM.drawString(context, disp, cardX + 8.0F, inTextY, 9.5F,
                    RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
        }

        // "+ Создать" Button (GLASS_SECONDARY)
        createBtnHover = lerp(createBtnHover, isCreateBtnHovered(mouseX, mouseY, cardX + nickW + 4.0F, inBarY) ? 1.0F : 0.0F, dt * 10.0F);
        float crtW = 72.0F;
        float crtX = cardX + nickW + 4.0F;
        drawGroupButton(context, crtX, inBarY, crtW, inH, 6.0F, createBtnHover, alpha, GlassStyle.SECONDARY,
                ICON_ADD, 128, 11.0F, "Создать", Fonts.MEDIUM, 9.0F, 5.0F, 0xFFCBD5E1, 0xFFFFFFFF);

        // Search Input Box (GLASS_SECONDARY)
        float srchW = 140.0F;
        float srchX = crtX + crtW + 6.0F;
        drawGlassCard(context, srchX, inBarY, srchW, inH, 6.0F, alpha, accountSearchFocused ? GlassStyle.ACTIVE_CARD : GlassStyle.SECONDARY);
        float srchTextY = (float) Math.round(inBarY + (inH - 9.0F) * 0.5F - 9.0F * 0.08F);
        if (accountSearch.isEmpty() && !accountSearchFocused) {
            drawMenuIcon(context, ICON_SEARCH, 128, srchX + 7.0F, (float) Math.round(inBarY + (inH - 12.0F) * 0.5F), 12.0F,
                    RenderColor.multiplyAlpha(0xFF8A9BA8, alpha));
            Fonts.REGULAR.drawString(context, "Поиск аккаунта", srchX + 23.0F, srchTextY, 8.8F,
                    RenderColor.multiplyAlpha(0xFF8A9BA8, alpha));
        } else {
            String disp = accountSearch + (accountSearchFocused && (now / 500) % 2 == 0 ? "|" : "");
            Fonts.MEDIUM.drawString(context, disp, srchX + 8.0F, srchTextY, 9.5F,
                    RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
        }

        // "🔀 Рандом" Button (GLASS_SECONDARY)
        randomBtnHover = lerp(randomBtnHover, isRandomBtnHovered(mouseX, mouseY, srchX + srchW + 4.0F, inBarY) ? 1.0F : 0.0F, dt * 10.0F);
        float rndW = 70.0F;
        float rndX = srchX + srchW + 4.0F;
        drawGroupButton(context, rndX, inBarY, rndW, inH, 6.0F, randomBtnHover, alpha, GlassStyle.SECONDARY,
                ICON_SPARKLES, 128, 11.0F, "Рандом", Fonts.MEDIUM, 8.8F, 5.0F, 0xFF38BDF8, 0xFFCAD4DF);

        // -------------------------------------------------------------
        // 3. Accounts Grid (2 Columns Scrollable)
        // -------------------------------------------------------------
        float gridY = (float) Math.round(inBarY + inH + 8.0F);
        float gridH = 106.0F;

        List<AccountManager.AccountEntry> allAccounts = AccountManager.getInstance().getAccountEntries();
        List<AccountManager.AccountEntry> filtered = new ArrayList<>();
        String searchLower = accountSearch.toLowerCase(Locale.ROOT).trim();
        for (AccountManager.AccountEntry e : allAccounts) {
            if (searchLower.isEmpty() || e.getNickname().toLowerCase(Locale.ROOT).contains(searchLower)) {
                filtered.add(e);
            }
        }

        float cardGridW = (float) Math.round((cardW - gapX) * 0.5F); // 201px
        float itemH = 48.0F;
        float gapY = 6.0F;

        int rowCount = (filtered.size() + 1) / 2;
        float totalContentH = rowCount * (itemH + gapY);
        float maxScroll = Math.max(0.0F, totalContentH - gridH);
        if (targetAccountsScrollY > maxScroll) targetAccountsScrollY = maxScroll;
        if (targetAccountsScrollY < 0.0F) targetAccountsScrollY = 0.0F;

        ScissorStack.push(cardX, gridY, cardW, gridH);
        try {
            for (int i = 0; i < filtered.size(); i++) {
                AccountManager.AccountEntry entry = filtered.get(i);
                int col = i % 2;
                int row = i / 2;

                float itmX = cardX + col * (cardGridW + gapX);
                float itmY = gridY + row * (itemH + gapY) - accountsScrollY;

                if (itmY + itemH < gridY || itmY > gridY + gridH) continue;

                boolean isActive = entry.getNickname().equalsIgnoreCase(AccountManager.getInstance().getActiveAccount());
                boolean isSelected = entry.getNickname().equalsIgnoreCase(selectedAccount);
                boolean hovered = mouseX >= itmX && mouseX <= itmX + cardGridW && mouseY >= itmY && mouseY <= itmY + itemH
                        && mouseY >= gridY && mouseY <= gridY + gridH;

                GlassStyle cardStyle = isActive ? GlassStyle.ACTIVE_CARD : (isSelected ? GlassStyle.SECONDARY : GlassStyle.CARD);
                drawGlassButton(context, itmX, itmY, cardGridW, itemH, 7.0F, hovered ? 1.0F : 0.0F, alpha, cardStyle);

                if (isSelected && !isActive) {
                    Render2D.drawRoundOutline(context, itmX, itmY, cardGridW, itemH, 7.0F, 1.0F,
                            RenderColor.multiplyAlpha(0x40FFFFFF, alpha));
                }

                // Player Head (Nearest filtered pixel-art)
                drawAccountHead(context, entry.getNickname(), itmX + 7.0F, itmY + 9.0F, 30.0F);

                // Date & Nickname (Proper alignment and typography)
                Fonts.REGULAR.drawString(context, entry.getDateAdded(), itmX + 42.0F, itmY + 9.0F, 7.8F,
                        RenderColor.multiplyAlpha(0xFF64748B, alpha));
                Fonts.MEDIUM.drawString(context, fitLabel(entry.getNickname(), cardGridW - 105.0F, 10.5F), itmX + 42.0F, itmY + 21.0F, 10.5F,
                        RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

                // Badges on right side
                if (isActive) {
                    drawBadge(context, "Активен", itmX + cardGridW - 58.0F, itmY + 16.0F,
                            0x2538BDF8, 0xFF38BDF8, 15.0F, alpha);
                } else {
                    // Star toggle - crisp resolution-independent vector star
                    float starX = itmX + cardGridW - 22.0F;
                    drawStarIcon(context, starX + 11.0F, itmY + 24.0F, 11.5F, entry.isFavorite(),
                            RenderColor.multiplyAlpha(entry.isFavorite() ? 0xFFF59E0B : 0x458A9BA8, alpha));
                }
            }
        } finally {
            ScissorStack.pop();
        }

        // -------------------------------------------------------------
        // 4. Action Buttons Bar
        // -------------------------------------------------------------
        float actY = (float) Math.round(gridY + gridH + 8.0F);

        // Big Primary Button [Войти как <SelectedAccount> ➔] - Strongest interactive hierarchy
        float loginH = 32.0F;
        loginAsBtnHover = lerp(loginAsBtnHover, isLoginAsBtnHovered(mouseX, mouseY, cardX, actY, cardW) ? 1.0F : 0.0F, dt * 10.0F);

        if (loginAsBtnHover > 0.01F) {
            Render2D.drawShadow(context, cardX, actY + 1.0F, cardW, loginH, 7.0F, 6.0F,
                    RenderColor.multiplyAlpha(0x35000000, alpha * loginAsBtnHover));
        }
        drawGlassButton(context, cardX, actY, cardW, loginH, 7.0F, loginAsBtnHover, alpha, GlassStyle.PRIMARY);

        String targetName = selectedAccount.isEmpty() ? AccountManager.getInstance().getActiveAccount() : selectedAccount;
        float logTextSize = 10.5F;
        drawCenteredText(context, Fonts.MEDIUM, "Войти как " + targetName + " ➔", cardX, actY, cardW, loginH, logTextSize,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

        // Secondary Row: [Добавить в избранное 🔖]   [Удалить аккаунт 🗑]
        float secBtnY = (float) Math.round(actY + loginH + 6.0F);
        float secBtnH = 26.0F;

        favoriteBtnHover = lerp(favoriteBtnHover, isFavoriteBtnHovered(mouseX, mouseY, cardX, secBtnY, halfW) ? 1.0F : 0.0F, dt * 10.0F);
        deleteBtnHover = lerp(deleteBtnHover, isDeleteBtnHovered(mouseX, mouseY, cardX + halfW + gapX, secBtnY, halfW) ? 1.0F : 0.0F, dt * 10.0F);

        // Favorite Button (GLASS_SECONDARY with dedicated vector star icon)
        boolean isFav = !selectedAccount.isEmpty() && AccountManager.getInstance().isFavorite(selectedAccount);
        String favText = isFav ? "Убрать из избранного" : "Добавить в избранное";
        drawFavoriteGroupButton(context, cardX, secBtnY, halfW, secBtnH, 6.0F, favoriteBtnHover, alpha, GlassStyle.SECONDARY,
                isFav, favText, Fonts.MEDIUM, 9.0F, 6.0F);

        // Delete Button (GLASS_DANGER on hover / subtle red accent)
        float delX = cardX + halfW + gapX;
        drawGroupButton(context, delX, secBtnY, halfW, secBtnH, 6.0F, deleteBtnHover, alpha, GlassStyle.DANGER,
                ICON_TRASH, 128, 11.0F, "Удалить аккаунт", Fonts.MEDIUM, 9.0F, 6.0F, 0xFFFF9298, 0xFFFFA6AB);

        // Bottom link: [Удалить найденные аккаунты]
        float delAllY = (float) Math.round(secBtnY + secBtnH + 8.0F);
        deleteAllBtnHover = lerp(deleteAllBtnHover, isDeleteAllBtnHovered(mouseX, mouseY, cardX, delAllY, cardW) ? 1.0F : 0.0F, dt * 10.0F);
        int delAllTextColor = RenderColor.interpolate(0xFF7D8E9E, 0xFFF87171, deleteAllBtnHover);
        drawCenteredText(context, Fonts.REGULAR, "Удалить найденные аккаунты", cardX, delAllY, cardW, 12.0F, 9.0F,
                RenderColor.multiplyAlpha(delAllTextColor, alpha));
    }

    // =========================================================================
    // 3. PROXY VIEW
    // =========================================================================

    private void renderProxyView(DrawContext context, int mouseX, int mouseY, float dt, float alpha) {
        if (alpha <= 0.01F) return;

        float centerX = (float) Math.round(this.width * 0.5F);
        float baseTopY = (float) Math.round(Math.max(16.0F, (this.height - 430.0F) * 0.5F));

        // Back Button (Top Left) - Optically Centered
        backBtnHover = lerp(backBtnHover, isBackBtnHovered(mouseX, mouseY) ? 1.0F : 0.0F, dt * 10.0F);
        float backW = 74.0F;
        float backH = 24.0F;
        float backX = 24.0F;
        float backY = 24.0F;
        drawBackButton(context, backX, backY, backW, backH, backBtnHover, alpha);

        // Top identity: Watermark F mark + Active username pill
        long now = System.currentTimeMillis();
        String activeUser = AccountManager.getInstance().getActiveAccount();
        float badgeW = usernameBadgeWidth(activeUser);
        float badgeH = 20.0F;
        float badgeX = (float) Math.round(centerX - badgeW * 0.5F);
        float badgeY = (float) Math.round(baseTopY + 28.0F);
        drawWatermarkMark(context, centerX, baseTopY, alpha, now);
        drawUsernameBadge(context, activeUser, badgeX, badgeY, badgeW, badgeH, alpha, 0.0F);

        // Header Title: "Управление Прокси"
        float titleY = (float) Math.round(badgeY + 34.0F);
        Fonts.SEMIBOLD.drawCenteredString(context, "Управление Прокси", centerX, titleY, 18.0F,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
        float subY = (float) Math.round(titleY + 24.0F);
        Fonts.REGULAR.drawCenteredString(context, "SOCKS5 и аутентификация для безопасного подключения", centerX, subY, 9.8F,
                RenderColor.multiplyAlpha(0xFFAAB8C8, alpha));

        // Main Glass Card (GLASS_CARD)
        float cardW = 430.0F;
        float cardH = 320.0F;
        float cardX = (float) Math.round(centerX - cardW * 0.5F);
        float cardY = (float) Math.round(subY + 20.0F);

        drawGlassCard(context, cardX, cardY, cardW, cardH, 10.0F, alpha, GlassStyle.CARD);

        // -------------------------------------------------------------
        // 1. Master Toggle Row: [ Switch ]  Использовать прокси
        // -------------------------------------------------------------
        float stY = cardY + 14.0F;
        boolean proxyEnabled = ProxyManager.getInstance().isProxyEnabled();
        ProxyManager.ProxyItem activeP = ProxyManager.getInstance().getActiveProxy();

        // Switch Toggle (Vector Pill)
        float swX = cardX + 16.0F;
        float swY = stY + 2.0F;
        float swW = 34.0F;
        float swH = 18.0F;
        int trackBg = proxyEnabled ? 0xFF22C55E : 0xFF2A3442;
        Render2D.drawRound(context, swX, swY, swW, swH, 9.0F, RenderColor.multiplyAlpha(trackBg, alpha));
        float thumbX = proxyEnabled ? swX + swW - 16.0F : swX + 2.0F;
        Render2D.drawRound(context, thumbX, swY + 2.0F, 14.0F, 14.0F, 7.0F, RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

        Fonts.MEDIUM.drawString(context, "Использовать прокси", swX + swW + 12.0F, stY, 11.0F,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
        String statusSub = proxyEnabled
                ? (activeP != null ? "Прокси включен (" + activeP.getType().name() + " активен)" : "Прокси включен")
                : "Прокси выключен (прямое подключение)";
        Fonts.REGULAR.drawString(context, statusSub, swX + swW + 12.0F, stY + 14.0F, 8.8F,
                RenderColor.multiplyAlpha(proxyEnabled ? 0xFF52D68A : 0xFF8A9BA8, alpha));

        // -------------------------------------------------------------
        // 2. Filter Pills Row: [Все (N)] [SOCKS5 (N)] [Сохранённые]      [+]
        // -------------------------------------------------------------
        float pillRowY = (float) Math.round(stY + 34.0F);
        float pillH = 24.0F;
        List<ProxyManager.ProxyItem> allProxies = ProxyManager.getInstance().getSavedProxies();
        long socksCount = allProxies.stream().filter(p -> p.getType() == BotProxyConfig.Type.SOCKS5).count();

        float p1W = 68.0F;
        float p1X = cardX + 16.0F;
        boolean p1Sel = (proxyFilterTab == ProxyFilterTab.ALL);
        drawCategoryPill(context, p1X, pillRowY, p1W, pillH, "Все (" + allProxies.size() + ")", p1Sel, mouseX, mouseY, alpha);

        float p2W = 84.0F;
        float p2X = p1X + p1W + 6.0F;
        boolean p2Sel = (proxyFilterTab == ProxyFilterTab.SOCKS5);
        drawCategoryPill(context, p2X, pillRowY, p2W, pillH, "SOCKS5 (" + socksCount + ")", p2Sel, mouseX, mouseY, alpha);

        float p3W = 92.0F;
        float p3X = p2X + p2W + 6.0F;
        boolean p3Sel = (proxyFilterTab == ProxyFilterTab.SAVED);
        drawCategoryPill(context, p3X, pillRowY, p3W, pillH, "Сохранённые", p3Sel, mouseX, mouseY, alpha);

        // Add Proxy "+" button on right (GLASS_SECONDARY)
        float addBtnW = 28.0F;
        float addBtnX = cardX + cardW - 16.0F - addBtnW;
        proxyAddBtnHover = lerp(proxyAddBtnHover, isProxyAddBtnHovered(mouseX, mouseY, addBtnX, pillRowY, addBtnW, pillH) ? 1.0F : 0.0F, dt * 10.0F);
        drawGlassButton(context, addBtnX, pillRowY, addBtnW, pillH, 5.0F, proxyAddBtnHover, alpha, GlassStyle.SECONDARY);
        drawMenuIcon(context, ICON_ADD, 128, (float) Math.round(addBtnX + (addBtnW - 12.0F) * 0.5F), (float) Math.round(pillRowY + (pillH - 12.0F) * 0.5F), 12.0F,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

        // -------------------------------------------------------------
        // 3. Search Bar: [ 🔍 Поиск прокси... ] (GLASS_SECONDARY)
        // -------------------------------------------------------------
        float srchY = (float) Math.round(pillRowY + pillH + 8.0F);
        float srchW = cardW - 32.0F;
        float srchH = 26.0F;
        float srchX = cardX + 16.0F;

        drawGlassCard(context, srchX, srchY, srchW, srchH, 6.0F, alpha, proxySearchFocused ? GlassStyle.ACTIVE_CARD : GlassStyle.SECONDARY);
        float pSrchTextY = (float) Math.round(srchY + (srchH - 9.0F) * 0.5F - 9.0F * 0.08F);
        if (proxySearch.isEmpty() && !proxySearchFocused) {
            drawMenuIcon(context, ICON_SEARCH, 128, srchX + 8.0F, (float) Math.round(srchY + (srchH - 12.0F) * 0.5F), 12.0F,
                    RenderColor.multiplyAlpha(0xFF8A9BA8, alpha));
            Fonts.REGULAR.drawString(context, "Поиск прокси...", srchX + 24.0F, pSrchTextY, 8.8F,
                    RenderColor.multiplyAlpha(0xFF8A9BA8, alpha));
        } else {
            String disp = proxySearch + (proxySearchFocused && (now / 500) % 2 == 0 ? "|" : "");
            Fonts.MEDIUM.drawString(context, disp, srchX + 8.0F, pSrchTextY, 9.5F,
                    RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
        }

        // -------------------------------------------------------------
        // 4. Proxy List (Scrollable)
        // -------------------------------------------------------------
        float listY = (float) Math.round(srchY + srchH + 8.0F);
        float listH = 138.0F;
        float listW = cardW - 32.0F;
        float listX = cardX + 16.0F;

        List<ProxyManager.ProxyItem> filtered = new ArrayList<>();
        String srchLower = proxySearch.toLowerCase(Locale.ROOT).trim();
        for (ProxyManager.ProxyItem p : allProxies) {
            if (proxyFilterTab == ProxyFilterTab.SOCKS5 && p.getType() != BotProxyConfig.Type.SOCKS5) continue;
            if (srchLower.isEmpty() || p.getDisplayAddress().toLowerCase(Locale.ROOT).contains(srchLower)
                    || p.getUsername().toLowerCase(Locale.ROOT).contains(srchLower)) {
                filtered.add(p);
            }
        }

        float itmH = 40.0F;
        float itmGap = 5.0F;
        float totalContentH = filtered.size() * (itmH + itmGap);
        float maxScroll = Math.max(0.0F, totalContentH - listH);
        if (targetProxyScrollY > maxScroll) targetProxyScrollY = maxScroll;
        if (targetProxyScrollY < 0.0F) targetProxyScrollY = 0.0F;

        if (filtered.isEmpty()) {
            drawCenteredText(context, Fonts.REGULAR, "Список прокси пуст", listX, listY, listW, listH, 10.5F,
                    RenderColor.multiplyAlpha(0xFF7E8F9F, alpha));
        } else {
            ScissorStack.push(listX, listY, listW, listH);
            try {
                for (int i = 0; i < filtered.size(); i++) {
                    ProxyManager.ProxyItem item = filtered.get(i);
                    float itmY = listY + i * (itmH + itmGap) - proxyScrollY;

                    if (itmY + itmH < listY || itmY > listY + listH) continue;

                    boolean isActive = (activeP != null && activeP.getId().equals(item.getId()));
                    boolean hov = mouseX >= listX && mouseX <= listX + listW && mouseY >= itmY && mouseY <= itmY + itmH
                            && mouseY >= listY && mouseY <= listY + listH;

                    GlassStyle itemStyle = isActive ? GlassStyle.ACTIVE_CARD : GlassStyle.SECONDARY;
                    drawGlassButton(context, listX, itmY, listW, itmH, 6.0F, hov ? 1.0F : 0.0F, alpha, itemStyle);

                    // Status Dot (Green if connected/active, Gray otherwise)
                    int dotCol = isActive ? 0xFF22C55E : 0xFF64748B;
                    Render2D.drawRound(context, listX + 10.0F, itmY + 16.0F, 7.0F, 7.0F, 3.5F,
                            RenderColor.multiplyAlpha(dotCol, alpha));

                    // IP & Port
                    Fonts.MEDIUM.drawString(context, item.getDisplayAddress(), listX + 26.0F, itmY + 9.0F, 10.5F,
                            RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

                    // Tags: SOCKS5 badge, user:pass badge
                    float tagX = listX + 26.0F;
                    float tagY = itmY + 23.0F;
                    drawBadge(context, item.getType().name(), tagX, tagY, 0x2038BDF8, 0xFF7DD3FC, 12.0F, alpha);
                    float tag1W = Fonts.MEDIUM.getWidth(item.getType().name(), 7.44F) + 10.0F;
                    String authLabel = item.hasAuth() ? "user:pass" : "no auth";
                    drawBadge(context, authLabel, tagX + tag1W + 4.0F, tagY, 0x15FFFFFF, 0xFF94A3B8, 12.0F, alpha);

                    // Ping info
                    if (item.getPingMs() > 0) {
                        int pingColor = item.getPingMs() < 80 ? 0xFF52D68A : (item.getPingMs() < 150 ? 0xFFFBBF24 : 0xFFF87171);
                        Fonts.REGULAR.drawString(context, item.getPingMs() + "ms", listX + listW - 90.0F, itmY + 15.0F, 9.0F,
                                RenderColor.multiplyAlpha(pingColor, alpha));
                    }

                    // Item Toggle Switch
                    float itmSwX = listX + listW - 54.0F;
                    float itmSwY = itmY + 13.0F;
                    float itmSwW = 26.0F;
                    float itmSwH = 14.0F;
                    int itmTrackBg = isActive ? 0xFF22C55E : 0xFF2A3442;
                    Render2D.drawRound(context, itmSwX, itmSwY, itmSwW, itmSwH, 7.0F, RenderColor.multiplyAlpha(itmTrackBg, alpha));
                    float itmThumbX = isActive ? itmSwX + itmSwW - 12.0F : itmSwX + 2.0F;
                    Render2D.drawRound(context, itmThumbX, itmSwY + 2.0F, 10.0F, 10.0F, 5.0F, RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

                    // Delete / Options "•••"
                    float optBtnX = listX + listW - 22.0F;
                    Fonts.MEDIUM.drawString(context, "•••", optBtnX, itmY + 12.0F, 11.0F,
                            RenderColor.multiplyAlpha(0xFF94A3B8, alpha));
                }
            } finally {
                ScissorStack.pop();
            }
        }

        // -------------------------------------------------------------
        // 5. Bottom Actions: [📥 Импорт из файла] [📤 Экспорт] [🗄 Проверить все прокси]
        // -------------------------------------------------------------
        float actRowY = (float) Math.round(cardY + cardH - 36.0F);
        float actBtnH = 24.0F;
        float actGap = 6.0F;
        float actBtnW = (float) Math.round((cardW - 32.0F - actGap * 2.0F) / 3.0F);

        // Button 1: Import
        float act1X = cardX + 16.0F;
        proxyImportHover = lerp(proxyImportHover, isProxyImportHovered(mouseX, mouseY, act1X, actRowY, actBtnW, actBtnH) ? 1.0F : 0.0F, dt * 10.0F);
        drawBottomActionCard(context, act1X, actRowY, actBtnW, actBtnH, "📥 Импорт из файла", proxyImportHover, alpha);

        // Button 2: Export
        float act2X = act1X + actBtnW + actGap;
        proxyExportHover = lerp(proxyExportHover, isProxyExportHovered(mouseX, mouseY, act2X, actRowY, actBtnW, actBtnH) ? 1.0F : 0.0F, dt * 10.0F);
        drawBottomActionCard(context, act2X, actRowY, actBtnW, actBtnH, "📤 Экспорт", proxyExportHover, alpha);

        // Button 3: Check all
        float act3X = act2X + actBtnW + actGap;
        proxyCheckAllHover = lerp(proxyCheckAllHover, isProxyCheckAllHovered(mouseX, mouseY, act3X, actRowY, actBtnW, actBtnH) ? 1.0F : 0.0F, dt * 10.0F);
        drawBottomActionCard(context, act3X, actRowY, actBtnW, actBtnH, "🗄 Проверить все", proxyCheckAllHover, alpha);
    }

    private void drawCategoryPill(DrawContext context, float x, float y, float w, float h,
                                  String label, boolean isSelected, int mouseX, int mouseY, float alpha) {
        boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        GlassStyle style = isSelected ? GlassStyle.ACTIVE_CARD : (hov ? GlassStyle.PRIMARY : GlassStyle.SECONDARY);
        drawGlassButton(context, x, y, w, h, 5.0F, hov ? 1.0F : 0.0F, alpha, style);

        float fontSize = 9.0F;
        int textColor = isSelected ? 0xFFFFFFFF : (hov ? 0xFFE2E8F0 : 0xFFCAD4DF);
        drawCenteredText(context, Fonts.MEDIUM, label, x, y, w, h, fontSize, RenderColor.multiplyAlpha(textColor, alpha));
    }

    private void drawBottomActionCard(DrawContext context, float x, float y, float w, float h,
                                      String label, float hover, float alpha) {
        drawGlassButton(context, x, y, w, h, 5.0F, hover, alpha, GlassStyle.SECONDARY);
        float fontSize = 8.8F;
        int textColor = RenderColor.interpolate(0xFFCAD4DF, 0xFFFFFFFF, hover);
        drawCenteredText(context, Fonts.MEDIUM, label, x, y, w, h, fontSize, RenderColor.multiplyAlpha(textColor, alpha));
    }

    // =========================================================================
    // 4. ADD PROXY MODAL DIALOG
    // =========================================================================

    private void renderAddProxyModal(DrawContext context, int mouseX, int mouseY, float dt, float alpha) {
        if (alpha <= 0.01F) return;

        Render2D.drawRound(context, 0, 0, this.width, this.height, 0.0F,
                RenderColor.multiplyAlpha(0x65000000, alpha));

        float modalW = 360.0F;
        float modalH = 220.0F;
        float modalX = (float) Math.round((this.width - modalW) * 0.5F);
        float modalY = (float) Math.round((this.height - modalH) * 0.5F);

        drawGlassCard(context, modalX, modalY, modalW, modalH, 10.0F, alpha, GlassStyle.CARD);

        Fonts.SEMIBOLD.drawString(context, "Добавить новый прокси", modalX + 16.0F, modalY + 14.0F, 12.5F,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

        // Format helper
        Fonts.REGULAR.drawString(context, "Формат: ip:port:user:pass или ip:port", modalX + 16.0F, modalY + 30.0F, 8.8F,
                RenderColor.multiplyAlpha(0xFF8A9BA8, alpha));

        // Input Field
        float inX = modalX + 16.0F;
        float inY = modalY + 48.0F;
        float inW = modalW - 32.0F;
        float inH = 32.0F;
        drawGlassCard(context, inX, inY, inW, inH, 6.0F, alpha, modalCombinedFocused ? GlassStyle.ACTIVE_CARD : GlassStyle.SECONDARY);

        long now = System.currentTimeMillis();
        float mInTextY = (float) Math.round(inY + (inH - 10.0F) * 0.5F - 10.0F * 0.08F);
        if (modalCombinedInput.isEmpty() && !modalCombinedFocused) {
            Fonts.REGULAR.drawString(context, "127.0.0.1:1080:login:password", inX + 8.0F, mInTextY, 9.8F,
                    RenderColor.multiplyAlpha(0xFF8A9BA8, alpha));
        } else {
            String disp = modalCombinedInput + (modalCombinedFocused && (now / 500) % 2 == 0 ? "|" : "");
            Fonts.MEDIUM.drawString(context, disp, inX + 8.0F, mInTextY, 10.0F,
                    RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
        }

        // Protocol Type badge (SOCKS5 only)
        float typeY = inY + inH + 12.0F;
        Fonts.MEDIUM.drawString(context, "Протокол:", inX, typeY + 4.0F, 9.5F,
                RenderColor.multiplyAlpha(0xFFCAD4DF, alpha));
        float s5X = inX + 70.0F;
        float s5W = 68.0F;
        float s5H = 20.0F;
        drawGlassCard(context, s5X, typeY, s5W, s5H, 5.0F, alpha, GlassStyle.ACTIVE_CARD);
        drawCenteredText(context, Fonts.MEDIUM, "SOCKS5", s5X, typeY, s5W, s5H, 9.0F,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));

        // Buttons: [Отмена]  [Сохранить и включить] - Optically Centered
        float btnY = modalY + modalH - 42.0F;
        float btnH = 28.0F;
        float canW = 100.0F;
        float savW = inW - canW - 8.0F;

        modalCancelHover = lerp(modalCancelHover, isModalCancelHovered(mouseX, mouseY, inX, btnY, canW, btnH) ? 1.0F : 0.0F, dt * 10.0F);
        modalSaveHover = lerp(modalSaveHover, isModalSaveHovered(mouseX, mouseY, inX + canW + 8.0F, btnY, savW, btnH) ? 1.0F : 0.0F, dt * 10.0F);

        drawGlassButton(context, inX, btnY, canW, btnH, 6.0F, modalCancelHover, alpha, GlassStyle.SECONDARY);
        drawCenteredText(context, Fonts.MEDIUM, "Отмена", inX, btnY, canW, btnH, 9.5F,
                RenderColor.multiplyAlpha(0xFFCAD4DF, alpha));

        float savX = inX + canW + 8.0F;
        drawGlassButton(context, savX, btnY, savW, btnH, 6.0F, modalSaveHover, alpha, GlassStyle.PRIMARY);
        drawCenteredText(context, Fonts.MEDIUM, "Сохранить и включить", savX, btnY, savW, btnH, 9.5F,
                RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
    }

    // =========================================================================
    // MOUSE & KEYBOARD INPUT HANDLING
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);

        // Handle Add Proxy Modal clicks
        if (currentState == MenuState.ADD_PROXY_MODAL && transitionProgress > 0.5F) {
            float modalW = 360.0F;
            float modalH = 220.0F;
            float modalX = (float) Math.round((this.width - modalW) * 0.5F);
            float modalY = (float) Math.round((this.height - modalH) * 0.5F);
            float inX = modalX + 16.0F;
            float inY = modalY + 48.0F;
            float inW = modalW - 32.0F;
            float inH = 32.0F;

            if (mouseX >= inX && mouseX <= inX + inW && mouseY >= inY && mouseY <= inY + inH) {
                modalCombinedFocused = true;
                return true;
            }
            modalCombinedFocused = false;

            float typeY = inY + inH + 12.0F;
            float s5X = inX + 70.0F;
            float s5W = 68.0F;
            float s5H = 20.0F;
            if (mouseX >= s5X && mouseX <= s5X + s5W && mouseY >= typeY && mouseY <= typeY + s5H) {
                modalCombinedType = BotProxyConfig.Type.SOCKS5;
                return true;
            }

            float btnY = modalY + modalH - 42.0F;
            float btnH = 28.0F;
            float canW = 100.0F;
            float savW = inW - canW - 8.0F;

            if (isModalCancelHovered(mouseX, mouseY, inX, btnY, canW, btnH)) {
                targetState = MenuState.PROXY;
                return true;
            }

            if (isModalSaveHovered(mouseX, mouseY, inX + canW + 8.0F, btnY, savW, btnH)) {
                submitModalProxy();
                return true;
            }
            return true;
        }

        // Handle Back Button in Accounts or Proxy view
        if ((currentState == MenuState.ACCOUNTS || currentState == MenuState.PROXY) && transitionProgress > 0.5F) {
            if (isBackBtnHovered(mouseX, mouseY)) {
                targetState = MenuState.MAIN;
                return true;
            }
        }

        // Handle Main Menu clicks
        if (currentState == MenuState.MAIN && transitionProgress > 0.5F) {
            if (isTopBadgeHovered(mouseX, mouseY)) {
                targetState = MenuState.ACCOUNTS;
                accountInputFocused = false;
                accountSearchFocused = false;
                return true;
            }
            if (isSingleplayerHovered(mouseX, mouseY)) {
                if (this.client != null) this.client.setScreen(new SelectWorldScreen(this));
                return true;
            }
            if (isMultiplayerHovered(mouseX, mouseY)) {
                if (this.client != null) this.client.setScreen(new MultiplayerScreen(this));
                return true;
            }
            if (isBtnAccountsHovered(mouseX, mouseY)) {
                targetState = MenuState.ACCOUNTS;
                accountInputFocused = false;
                accountSearchFocused = false;
                return true;
            }
            if (isBtnSettingsHovered(mouseX, mouseY)) {
                if (this.client != null) this.client.setScreen(new OptionsScreen(this, this.client.options));
                return true;
            }
            if (isBtnProxyHovered(mouseX, mouseY)) {
                targetState = MenuState.PROXY;
                proxySearchFocused = false;
                return true;
            }

            if (isExitBtnHovered(mouseX, mouseY)) {
                isExiting = true;
                exitTime = System.currentTimeMillis();
                showToast("Выход из игры...", 0xFFFF5C6C);
                return true;
            }

            if (isWinCloseHovered(mouseX, mouseY)) {
                isExiting = true;
                exitTime = System.currentTimeMillis();
                return true;
            }
            return true;
        }

        // Handle Accounts View clicks
        if (currentState == MenuState.ACCOUNTS && transitionProgress > 0.5F) {
            float centerX = (float) Math.round(this.width * 0.5F);
            float baseTopY = (float) Math.round(Math.max(16.0F, (this.height - 430.0F) * 0.5F));
            float cardW = 410.0F;
            float cardX = (float) Math.round(centerX - cardW * 0.5F);
            float gapX = 8.0F;
            float halfW = (float) Math.round((cardW - gapX) * 0.5F);

            // Microsoft Login button
            float msY = (float) Math.round(baseTopY + 28.0F + 34.0F + 24.0F + 20.0F);
            if (isMicrosoftBtnHovered(mouseX, mouseY, cardX, msY, cardW)) {
                return true;
            }

            // Inputs focus
            float secY = msY + 92.0F + 12.0F;
            float inBarY = (float) Math.round(secY + 12.0F);
            float nickW = 112.0F;
            float inH = 26.0F;

            if (mouseX >= cardX && mouseX <= cardX + nickW && mouseY >= inBarY && mouseY <= inBarY + inH) {
                accountInputFocused = true;
                accountSearchFocused = false;
                return true;
            }

            float crtW = 72.0F;
            float crtX = cardX + nickW + 4.0F;
            if (isCreateBtnHovered(mouseX, mouseY, crtX, inBarY)) {
                submitNewAccount();
                return true;
            }

            float srchW = 140.0F;
            float srchX = crtX + crtW + 6.0F;
            if (mouseX >= srchX && mouseX <= srchX + srchW && mouseY >= inBarY && mouseY <= inBarY + inH) {
                accountSearchFocused = true;
                accountInputFocused = false;
                return true;
            }

            float rndX = srchX + srchW + 4.0F;
            if (isRandomBtnHovered(mouseX, mouseY, rndX, inBarY)) {
                accountInput = AccountManager.getInstance().generateRandomNickname();
                accountInputFocused = true;
                accountSearchFocused = false;
                return true;
            }

            accountInputFocused = false;
            accountSearchFocused = false;

            // Grid items click
            float gridY = (float) Math.round(inBarY + inH + 8.0F);
            float gridH = 106.0F;
            if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= gridY && mouseY <= gridY + gridH) {
                List<AccountManager.AccountEntry> allAccounts = AccountManager.getInstance().getAccountEntries();
                List<AccountManager.AccountEntry> filtered = new ArrayList<>();
                String searchLower = accountSearch.toLowerCase(Locale.ROOT).trim();
                for (AccountManager.AccountEntry e : allAccounts) {
                    if (searchLower.isEmpty() || e.getNickname().toLowerCase(Locale.ROOT).contains(searchLower)) {
                        filtered.add(e);
                    }
                }

                float cardGridW = (float) Math.round((cardW - gapX) * 0.5F);
                float itemH = 48.0F;
                float gapY = 6.0F;

                for (int i = 0; i < filtered.size(); i++) {
                    AccountManager.AccountEntry entry = filtered.get(i);
                    int col = i % 2;
                    int row = i / 2;
                    float itmX = cardX + col * (cardGridW + gapX);
                    float itmY = gridY + row * (itemH + gapY) - accountsScrollY;

                    if (mouseX >= itmX && mouseX <= itmX + cardGridW && mouseY >= itmY && mouseY <= itmY + itemH) {
                        float starX = itmX + cardGridW - 24.0F;
                        if (mouseX >= starX && mouseX <= starX + 22.0F) {
                            AccountManager.getInstance().toggleFavorite(entry.getNickname());
                            return true;
                        }

                        long now = System.currentTimeMillis();
                        if (entry.getNickname().equalsIgnoreCase(lastClickedAccount) && (now - lastAccountClickTime) < 350L) {
                            // Double-click: activate account directly
                            selectedAccount = entry.getNickname();
                            AccountManager.getInstance().switchAccount(entry.getNickname());
                            lastAccountClickTime = 0L;
                            lastClickedAccount = "";
                        } else {
                            // Single-click: select account for bottom action buttons
                            selectedAccount = entry.getNickname();
                            lastAccountClickTime = now;
                            lastClickedAccount = entry.getNickname();
                        }
                        return true;
                    }
                }
            }

            // Action Buttons
            float actY = (float) Math.round(gridY + gridH + 8.0F);
            float loginH = 32.0F;
            if (isLoginAsBtnHovered(mouseX, mouseY, cardX, actY, cardW)) {
                if (!selectedAccount.isEmpty()) {
                    AccountManager.getInstance().switchAccount(selectedAccount);
                }
                return true;
            }

            float secBtnY = (float) Math.round(actY + loginH + 6.0F);
            if (isFavoriteBtnHovered(mouseX, mouseY, cardX, secBtnY, halfW)) {
                if (!selectedAccount.isEmpty()) {
                    AccountManager.getInstance().toggleFavorite(selectedAccount);
                }
                return true;
            }

            if (isDeleteBtnHovered(mouseX, mouseY, cardX + halfW + gapX, secBtnY, halfW)) {
                if (!selectedAccount.isEmpty()) {
                    AccountManager.getInstance().removeAccount(selectedAccount);
                    selectedAccount = AccountManager.getInstance().getActiveAccount();
                }
                return true;
            }

            float delAllY = (float) Math.round(secBtnY + 26.0F + 8.0F);
            if (isDeleteAllBtnHovered(mouseX, mouseY, cardX, delAllY, cardW)) {
                List<AccountManager.AccountEntry> allAccounts = AccountManager.getInstance().getAccountEntries();
                List<AccountManager.AccountEntry> toRemove = new ArrayList<>();
                String searchLower = accountSearch.toLowerCase(Locale.ROOT).trim();
                for (AccountManager.AccountEntry e : allAccounts) {
                    if (searchLower.isEmpty() || e.getNickname().toLowerCase(Locale.ROOT).contains(searchLower)) {
                        toRemove.add(e);
                    }
                }
                if (!toRemove.isEmpty()) {
                    for (AccountManager.AccountEntry e : toRemove) {
                        AccountManager.getInstance().removeAccount(e.getNickname());
                    }
                    selectedAccount = AccountManager.getInstance().getActiveAccount();
                }
                return true;
            }
            return true;
        }

        // Handle Proxy View clicks
        if (currentState == MenuState.PROXY && transitionProgress > 0.5F) {
            float centerX = (float) Math.round(this.width * 0.5F);
            float baseTopY = (float) Math.round(Math.max(16.0F, (this.height - 430.0F) * 0.5F));
            float cardW = 430.0F;
            float cardX = (float) Math.round(centerX - cardW * 0.5F);
            float cardY = (float) Math.round(baseTopY + 28.0F + 34.0F + 24.0F + 20.0F);

            // Master enable switch
            float stY = cardY + 14.0F;
            if (mouseX >= cardX + 16.0F && mouseX <= cardX + 220.0F && mouseY >= stY && mouseY <= stY + 24.0F) {
                boolean next = !ProxyManager.getInstance().isProxyEnabled();
                ProxyManager.getInstance().setProxyEnabled(next);
                showToast(next ? "Прокси включен 🟢" : "Прокси выключен ⚪", next ? 0xFF52D68A : 0xFF94A3B8);
                return true;
            }

            // Category Filter pills
            float pillRowY = (float) Math.round(stY + 34.0F);
            float p1W = 68.0F;
            float p1X = cardX + 16.0F;
            if (mouseX >= p1X && mouseX <= p1X + p1W && mouseY >= pillRowY && mouseY <= pillRowY + 24.0F) {
                proxyFilterTab = ProxyFilterTab.ALL;
                return true;
            }
            float p2W = 84.0F;
            float p2X = p1X + p1W + 6.0F;
            if (mouseX >= p2X && mouseX <= p2X + p2W && mouseY >= pillRowY && mouseY <= pillRowY + 24.0F) {
                proxyFilterTab = ProxyFilterTab.SOCKS5;
                return true;
            }
            float p3W = 92.0F;
            float p3X = p2X + p2W + 6.0F;
            if (mouseX >= p3X && mouseX <= p3X + p3W && mouseY >= pillRowY && mouseY <= pillRowY + 24.0F) {
                proxyFilterTab = ProxyFilterTab.SAVED;
                return true;
            }

            // Add Proxy "+" Button
            float addBtnW = 28.0F;
            float addBtnX = cardX + cardW - 16.0F - addBtnW;
            if (isProxyAddBtnHovered(mouseX, mouseY, addBtnX, pillRowY, addBtnW, 24.0F)) {
                targetState = MenuState.ADD_PROXY_MODAL;
                modalCombinedInput = "";
                modalCombinedFocused = true;
                return true;
            }

            // Search Bar Focus
            float srchY = (float) Math.round(pillRowY + 24.0F + 8.0F);
            float srchW = cardW - 32.0F;
            float srchH = 26.0F;
            float srchX = cardX + 16.0F;
            if (mouseX >= srchX && mouseX <= srchX + srchW && mouseY >= srchY && mouseY <= srchY + srchH) {
                proxySearchFocused = true;
                return true;
            }
            proxySearchFocused = false;

            // Proxy List Item click
            float listY = (float) Math.round(srchY + srchH + 8.0F);
            float listH = 138.0F;
            float listW = cardW - 32.0F;
            float listX = cardX + 16.0F;

            List<ProxyManager.ProxyItem> allProxies = ProxyManager.getInstance().getSavedProxies();
            List<ProxyManager.ProxyItem> filtered = new ArrayList<>();
            String srchLower = proxySearch.toLowerCase(Locale.ROOT).trim();
            for (ProxyManager.ProxyItem p : allProxies) {
                if (proxyFilterTab == ProxyFilterTab.SOCKS5 && p.getType() != BotProxyConfig.Type.SOCKS5) continue;
                if (srchLower.isEmpty() || p.getDisplayAddress().toLowerCase(Locale.ROOT).contains(srchLower)
                    || p.getUsername().toLowerCase(Locale.ROOT).contains(srchLower)) {
                    filtered.add(p);
                }
            }

            float itmH = 40.0F;
            float itmGap = 5.0F;
            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                for (int i = 0; i < filtered.size(); i++) {
                    ProxyManager.ProxyItem item = filtered.get(i);
                    float itmY = listY + i * (itmH + itmGap) - proxyScrollY;
                    if (mouseY >= itmY && mouseY <= itmY + itmH) {
                        float delX = listX + listW - 24.0F;
                        if (mouseX >= delX && mouseX <= delX + 22.0F) {
                            ProxyManager.getInstance().removeProxy(item);
                            showToast("Прокси удален", 0xFFFF7B88);
                            return true;
                        }
                        ProxyManager.getInstance().setActiveProxy(item);
                        ProxyManager.getInstance().setProxyEnabled(true);
                        showToast("Выбран: " + item.getDisplayAddress(), 0xFF52D68A);
                        return true;
                    }
                }
            }

            // Bottom Actions: Import, Export, Check All
            float actRowY = (float) Math.round(cardY + 320.0F - 36.0F);
            float actGap = 6.0F;
            float actBtnW = (float) Math.round((cardW - 32.0F - actGap * 2.0F) / 3.0F);
            float act1X = cardX + 16.0F;
            if (isProxyImportHovered(mouseX, mouseY, act1X, actRowY, actBtnW, 24.0F)) {
                showToast("Импорт из буфера...", 0xFF38BDF8);
                pasteClipboard(clip -> {
                    ProxyManager.ProxyItem parsed = ProxyManager.getInstance().parseCombined(clip, BotProxyConfig.Type.SOCKS5);
                    if (parsed != null) {
                        ProxyManager.getInstance().addProxy(parsed);
                        showToast("Прокси импортирован 🟢", 0xFF52D68A);
                    }
                });
                return true;
            }

            float act2X = act1X + actBtnW + actGap;
            if (isProxyExportHovered(mouseX, mouseY, act2X, actRowY, actBtnW, 24.0F)) {
                ProxyManager.ProxyItem active = ProxyManager.getInstance().getActiveProxy();
                if (active != null && this.client != null && this.client.keyboard != null) {
                    this.client.keyboard.setClipboard(active.getCombinedString());
                    showToast("Прокси скопирован в буфер 📋", 0xFF52D68A);
                } else {
                    showToast("Нет активного прокси", 0xFFFF5C6C);
                }
                return true;
            }

            float act3X = act2X + actBtnW + actGap;
            if (isProxyCheckAllHovered(mouseX, mouseY, act3X, actRowY, actBtnW, 24.0F)) {
                showToast("Проверка доступности всех прокси...", 0xFF38BDF8);
                for (ProxyManager.ProxyItem p : allProxies) {
                    p.testAsync(item -> {});
                }
                return true;
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void submitModalProxy() {
        if (modalCombinedInput.trim().isEmpty()) {
            showToast("Введите данные прокси", 0xFFFF5C6C);
            return;
        }
        ProxyManager.ProxyItem parsed = ProxyManager.getInstance().parseCombined(modalCombinedInput, modalCombinedType);
        if (parsed != null) {
            ProxyManager.getInstance().addProxy(parsed);
            ProxyManager.getInstance().setActiveProxy(parsed);
            ProxyManager.getInstance().setProxyEnabled(true);
            showToast("Прокси добавлен и активирован 🟢", 0xFF52D68A);
            targetState = MenuState.PROXY;
        } else {
            showToast("Неверный формат прокси", 0xFFFF5C6C);
        }
    }

    private void submitNewAccount() {
        if (accountInput.trim().isEmpty()) return;
        String clean = accountInput.trim();
        AccountManager.getInstance().addAccount(clean);
        AccountManager.getInstance().switchAccount(clean);
        selectedAccount = clean;
        accountInput = "";
        accountInputFocused = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentState == MenuState.ACCOUNTS) {
            targetAccountsScrollY -= (float) (verticalAmount * 24.0);
            return true;
        }
        if (currentState == MenuState.PROXY) {
            targetProxyScrollY -= (float) (verticalAmount * 24.0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (currentState == MenuState.ADD_PROXY_MODAL) {
                targetState = MenuState.PROXY;
                return true;
            }
            if (currentState != MenuState.MAIN) {
                targetState = MenuState.MAIN;
                return true;
            }
        }

        // Accounts input keys
        if (currentState == MenuState.ACCOUNTS) {
            if (accountInputFocused) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    submitNewAccount();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !accountInput.isEmpty()) {
                    accountInput = accountInput.substring(0, accountInput.length() - 1);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    pasteClipboard(text -> {
                        accountInput += text;
                        if (accountInput.length() > 16) accountInput = accountInput.substring(0, 16);
                    });
                    return true;
                }
            }
            if (accountSearchFocused) {
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !accountSearch.isEmpty()) {
                    accountSearch = accountSearch.substring(0, accountSearch.length() - 1);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    pasteClipboard(text -> accountSearch += text);
                    return true;
                }
            }
        }

        // Add Proxy Modal input keys
        if (currentState == MenuState.ADD_PROXY_MODAL) {
            if (modalCombinedFocused) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    submitModalProxy();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !modalCombinedInput.isEmpty()) {
                    modalCombinedInput = modalCombinedInput.substring(0, modalCombinedInput.length() - 1);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    pasteClipboard(text -> modalCombinedInput += text);
                    return true;
                }
            }
        }

        // Proxy search keys
        if (currentState == MenuState.PROXY && proxySearchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !proxySearch.isEmpty()) {
                proxySearch = proxySearch.substring(0, proxySearch.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                pasteClipboard(text -> proxySearch += text);
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr != 127) {
            if (currentState == MenuState.ACCOUNTS) {
                if (accountInputFocused && accountInput.length() < 16) {
                    accountInput += chr;
                    return true;
                }
                if (accountSearchFocused) {
                    accountSearch += chr;
                    return true;
                }
            }
            if (currentState == MenuState.ADD_PROXY_MODAL && modalCombinedFocused) {
                modalCombinedInput += chr;
                return true;
            }
            if (currentState == MenuState.PROXY && proxySearchFocused) {
                proxySearch += chr;
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    private void pasteClipboard(java.util.function.Consumer<String> consumer) {
        if (this.client != null && this.client.keyboard != null) {
            String clip = this.client.keyboard.getClipboard();
            if (clip != null && !clip.isBlank()) {
                consumer.accept(clip.trim());
            }
        }
    }

    // =========================================================================
    // HIT TARGET DETECTORS
    // =========================================================================

    private boolean isTopBadgeHovered(double mouseX, double mouseY) {
        float centerX = (float) Math.round(this.width * 0.5F);
        float baseCenterY = (float) Math.round(this.height * 0.44F);
        String activeUser = AccountManager.getInstance().getActiveAccount();
        float badgeW = usernameBadgeWidth(activeUser);
        float badgeH = 20.0F;
        float badgeX = (float) Math.round(centerX - badgeW * 0.5F);
        float badgeY = (float) Math.round(baseCenterY - 100.0F + 28.0F);
        return mouseX >= badgeX && mouseX <= badgeX + badgeW && mouseY >= badgeY && mouseY <= badgeY + badgeH;
    }

    private boolean isSingleplayerHovered(double mouseX, double mouseY) {
        float centerX = (float) Math.round(this.width * 0.5F);
        float baseCenterY = (float) Math.round(this.height * 0.44F);
        float colW = 248.0F;
        float gap = 8.0F;
        float halfW = (float) Math.round((colW - gap) * 0.5F);
        float btnSingleX = (float) Math.round(centerX - colW * 0.5F);
        float r1Y = (float) Math.round(baseCenterY - 100.0F + 28.0F + 34.0F + 29.0F + 25.0F);
        return mouseX >= btnSingleX && mouseX <= btnSingleX + halfW && mouseY >= r1Y && mouseY <= r1Y + 31.0F;
    }

    private boolean isMultiplayerHovered(double mouseX, double mouseY) {
        float centerX = (float) Math.round(this.width * 0.5F);
        float baseCenterY = (float) Math.round(this.height * 0.44F);
        float colW = 248.0F;
        float gap = 8.0F;
        float halfW = (float) Math.round((colW - gap) * 0.5F);
        float btnSingleX = (float) Math.round(centerX - colW * 0.5F);
        float btnMultiX = (float) Math.round(btnSingleX + halfW + gap);
        float r1Y = (float) Math.round(baseCenterY - 100.0F + 28.0F + 34.0F + 29.0F + 25.0F);
        return mouseX >= btnMultiX && mouseX <= btnMultiX + halfW && mouseY >= r1Y && mouseY <= r1Y + 31.0F;
    }

    private boolean isBtnAccountsHovered(double mouseX, double mouseY) {
        float centerX = (float) Math.round(this.width * 0.5F);
        float baseCenterY = (float) Math.round(this.height * 0.44F);
        float iconBtnSize = 31.0F;
        float iconGap = 7.0F;
        float startRow2X = (float) Math.round(centerX - (iconBtnSize * 4.0F + iconGap * 3.0F) * 0.5F);
        float r1Y = (float) Math.round(baseCenterY - 100.0F + 28.0F + 34.0F + 29.0F + 25.0F);
        float r2Y = (float) Math.round(r1Y + 31.0F + 8.0F);
        return mouseX >= startRow2X && mouseX <= startRow2X + iconBtnSize && mouseY >= r2Y && mouseY <= r2Y + iconBtnSize;
    }

    private boolean isBtnSettingsHovered(double mouseX, double mouseY) {
        float centerX = (float) Math.round(this.width * 0.5F);
        float baseCenterY = (float) Math.round(this.height * 0.44F);
        float iconBtnSize = 31.0F;
        float iconGap = 7.0F;
        float startRow2X = (float) Math.round(centerX - (iconBtnSize * 4.0F + iconGap * 3.0F) * 0.5F);
        float btnOptX = (float) Math.round(startRow2X + iconBtnSize + iconGap);
        float r1Y = (float) Math.round(baseCenterY - 100.0F + 28.0F + 34.0F + 29.0F + 25.0F);
        float r2Y = (float) Math.round(r1Y + 31.0F + 8.0F);
        return mouseX >= btnOptX && mouseX <= btnOptX + iconBtnSize && mouseY >= r2Y && mouseY <= r2Y + iconBtnSize;
    }

    private boolean isBtnProxyHovered(double mouseX, double mouseY) {
        float centerX = (float) Math.round(this.width * 0.5F);
        float baseCenterY = (float) Math.round(this.height * 0.44F);
        float iconBtnSize = 31.0F;
        float iconGap = 7.0F;
        float startRow2X = (float) Math.round(centerX - (iconBtnSize * 4.0F + iconGap * 3.0F) * 0.5F);
        float btnPrxX = (float) Math.round(startRow2X + (iconBtnSize + iconGap) * 2.0F);
        float r1Y = (float) Math.round(baseCenterY - 100.0F + 28.0F + 34.0F + 29.0F + 25.0F);
        float r2Y = (float) Math.round(r1Y + 31.0F + 8.0F);
        return mouseX >= btnPrxX && mouseX <= btnPrxX + iconBtnSize && mouseY >= r2Y && mouseY <= r2Y + iconBtnSize;
    }

    private boolean isExitBtnHovered(double mouseX, double mouseY) {
        float centerX = (float) Math.round(this.width * 0.5F);
        float baseCenterY = (float) Math.round(this.height * 0.44F);
        float iconBtnSize = 31.0F;
        float iconGap = 7.0F;
        float startRow2X = (float) Math.round(centerX - (iconBtnSize * 4.0F + iconGap * 3.0F) * 0.5F);
        float btnExitX = (float) Math.round(startRow2X + (iconBtnSize + iconGap) * 3.0F);
        float r1Y = (float) Math.round(baseCenterY - 100.0F + 28.0F + 34.0F + 29.0F + 25.0F);
        float r2Y = (float) Math.round(r1Y + 31.0F + 8.0F);
        return mouseX >= btnExitX && mouseX <= btnExitX + iconBtnSize && mouseY >= r2Y && mouseY <= r2Y + iconBtnSize;
    }

    private boolean isWinCloseHovered(double mouseX, double mouseY) {
        float winX = (float) Math.round(this.width - 48.0F + 16.0F);
        float winY = 12.0F;
        return mouseX >= winX - 4.0F && mouseX <= winX + 16.0F && mouseY >= winY - 4.0F && mouseY <= winY + 16.0F;
    }

    private boolean isWinMinHovered(double mouseX, double mouseY) {
        float winX = (float) Math.round(this.width - 48.0F);
        float winY = 12.0F;
        return mouseX >= winX - 4.0F && mouseX <= winX + 16.0F && mouseY >= winY - 4.0F && mouseY <= winY + 16.0F;
    }

    private boolean isBackBtnHovered(double mouseX, double mouseY) {
        return mouseX >= 24.0F && mouseX <= 24.0F + 74.0F && mouseY >= 24.0F && mouseY <= 24.0F + 24.0F;
    }

    private boolean isMicrosoftBtnHovered(double mouseX, double mouseY, float cardX, float msY, float cardW) {
        float msBtnX = cardX + 14.0F;
        float msBtnY = msY + 44.0F;
        float msBtnW = cardW - 28.0F;
        return mouseX >= msBtnX && mouseX <= msBtnX + msBtnW && mouseY >= msBtnY && mouseY <= msBtnY + 32.0F;
    }

    private boolean isCreateBtnHovered(double mouseX, double mouseY, float crtX, float inBarY) {
        return mouseX >= crtX && mouseX <= crtX + 72.0F && mouseY >= inBarY && mouseY <= inBarY + 26.0F;
    }

    private boolean isRandomBtnHovered(double mouseX, double mouseY, float rndX, float inBarY) {
        return mouseX >= rndX && mouseX <= rndX + 70.0F && mouseY >= inBarY && mouseY <= inBarY + 26.0F;
    }

    private boolean isFavoriteBtnHovered(double mouseX, double mouseY, float x, float y, float w) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 26.0F;
    }

    private boolean isDeleteBtnHovered(double mouseX, double mouseY, float x, float y, float w) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 26.0F;
    }

    private boolean isLoginAsBtnHovered(double mouseX, double mouseY, float x, float y, float w) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 32.0F;
    }

    private boolean isDeleteAllBtnHovered(double mouseX, double mouseY, float x, float y, float w) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y - 4.0F && mouseY <= y + 16.0F;
    }

    private boolean isProxyAddBtnHovered(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private boolean isProxyImportHovered(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private boolean isProxyExportHovered(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private boolean isProxyCheckAllHovered(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private boolean isModalCancelHovered(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private boolean isModalSaveHovered(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private void drawAccountHead(DrawContext context, String nickname, float x, float y, float size) {
        try {
            String clean = nickname == null || nickname.isBlank() ? "Player" : nickname;
            Identifier skinTexture = skinCache.get(clean);
            if (skinTexture == null) {
                UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + clean).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                boolean slim = (uuid.hashCode() & 1) == 1;
                skinTexture = Identifier.ofVanilla(slim ? "textures/entity/player/wide/alex.png" : "textures/entity/player/wide/steve.png");
                skinCache.put(clean, skinTexture);
            }

            float faceU = 8.0F / 64.0F;
            float faceV = 8.0F / 64.0F;
            float faceW = 8.0F / 64.0F;
            float faceH = 8.0F / 64.0F;
            Render2D.drawRoundTextureRegion(context, skinTexture, x, y, size, size, 6.0F,
                    0xFFFFFFFF, faceU, faceV, faceW, faceH);
            Render2D.drawRoundTextureRegion(context, skinTexture, x, y, size, size, 6.0F,
                    0xFFFFFFFF, 40.0F / 64.0F, 8.0F / 64.0F, faceW, faceH);
            Render2D.drawRoundOutline(context, x, y, size, size, 6.0F, 1.0F, 0x22FFFFFF);
        } catch (Throwable ignored) {
            Render2D.drawRound(context, x, y, size, size, 6.0F, 0x401E293B);
            Render2D.drawRoundOutline(context, x, y, size, size, 6.0F, 1.0F, 0x22FFFFFF);
        }
    }

    public void showToast(String message, int color) {
        this.statusToast = message;
        this.statusToastTime = System.currentTimeMillis();
        this.toastColor = color;
    }

    public void showToast(String message) {
        showToast(message, 0xFF52D68A);
    }

    private void renderStatusToast(DrawContext context) {
        if (statusToast.isEmpty()) return;
        long elapsed = System.currentTimeMillis() - statusToastTime;
        if (elapsed > 2800L) {
            statusToast = "";
            return;
        }

        float toastAlpha = 1.0F;
        if (elapsed > 2200L) {
            toastAlpha = 1.0F - (elapsed - 2200L) / 600.0F;
        }

        float tW = Fonts.MEDIUM.getWidth(statusToast, 10.5F) + 36.0F;
        float tH = 28.0F;
        float tX = (float) Math.round((this.width - tW) * 0.5F);
        float tY = (float) Math.round(this.height - 48.0F);

        BlurRenderer.drawLiquidGlass(context, tX, tY, tW, tH, 7.0F,
                new Color(12, 16, 24, 130), toastAlpha * 0.94F, 0.85F, 0.50F, 0.35F, 0.85F, 0.55F, true);
        Render2D.drawRound(context, tX, tY, tW, tH, 7.0F, RenderColor.multiplyAlpha(0x400C1018, toastAlpha));
        Render2D.drawRoundOutline(context, tX, tY, tW, tH, 7.0F, 1.0F,
                RenderColor.multiplyAlpha(0x22FFFFFF, toastAlpha));

        drawCenteredText(context, Fonts.MEDIUM, statusToast, tX, tY, tW, tH, 10.5F,
                RenderColor.multiplyAlpha(toastColor, toastAlpha));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.clamp(t, 0.0F, 1.0F);
    }

    private static float easeOutQuart(float x) {
        return 1.0F - (float) Math.pow(1.0 - x, 4.0);
    }
}
