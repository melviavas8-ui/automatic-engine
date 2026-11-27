package daamky.ua.newmenu.render.liqvid;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * 1.21.11: RenderSystem no longer exposes a CPU-side projection matrix, and the
 * vanilla GUI is rendered deferred. All LunaWare drawing is done with its own GL
 * shaders, so we keep our own ortho projection / model-view here instead of
 * touching the global RenderSystem state.
 */
public final class ScreenScale {
    private static float scale = 1.0F;
    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f modelView = new Matrix4f();
    private static boolean active;

    private ScreenScale() {
    }

    public static void begin(double renderScale) {
        scale = (float) renderScale;
        Window window = Minecraft.getInstance().getWindow();
        double width = window.getWidth() / renderScale;
        double height = window.getHeight() / renderScale;
        projection.identity().setOrtho(0.0F, (float) width, (float) height, 0.0F, -1000.0F, 1000.0F);
        modelView.identity();
        active = true;
    }

    public static void end() {
        active = false;
        scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static boolean isActive() {
        return active;
    }

    public static Matrix4f getProjectionMatrix() {
        return projection;
    }

    public static Matrix4f getModelViewMatrix() {
        return modelView;
    }

    public static float getScale() {
        return scale;
    }

    public static double getScaledMouseX() {
        return getScaledMouse()[0];
    }

    public static double getScaledMouseY() {
        return getScaledMouse()[1];
    }

    /** Window cursor position -> scaled GUI coordinates. */
    public static double[] getScaledMouse() {
        Window window = Minecraft.getInstance().getWindow();
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(window.handle(), xpos, ypos);
        return new double[] {
            xpos[0] * window.getGuiScaledWidth() / (double) window.getWidth(),
            ypos[0] * window.getGuiScaledHeight() / (double) window.getHeight()
        };
    }

    /** Scaled GUI mouse coords -> HUD ortho coords (ScreenScale.begin(2.0)). */
    public static float toHudX(double scaledMouseX) {
        Window window = Minecraft.getInstance().getWindow();
        return (float) (scaledMouseX * (window.getWidth() / 2.0) / window.getGuiScaledWidth());
    }

    public static float toHudY(double scaledMouseY) {
        Window window = Minecraft.getInstance().getWindow();
        return (float) (scaledMouseY * (window.getHeight() / 2.0) / window.getGuiScaledHeight());
    }

    public static float mouseHudX() {
        return toHudX(getScaledMouseX());
    }

    public static float mouseHudY() {
        return toHudY(getScaledMouseY());
    }

    public static float getRenderWidth() {
        return toHudX(Minecraft.getInstance().getWindow().getGuiScaledWidth());
    }

    public static float getRenderHeight() {
        return toHudY(Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }
}

