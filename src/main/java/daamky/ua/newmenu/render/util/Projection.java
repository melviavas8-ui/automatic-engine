package daamky.ua.newmenu.render.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector4f;

public final class Projection {
    private static final Matrix4f VIEW_PROJ = new Matrix4f();
    private static double camX, camY, camZ;
    private static boolean hasMatrices = false;

    public static float lastFov = 70.0F;

    private Projection() {}

    /**
     * Updates the exact view-projection matrix from WorldRenderer.render per frame.
     */
    public static void updateMatrices(Matrix4f positionMatrix, Matrix4f projectionMatrix, Vec3d camPos) {
        if (positionMatrix == null || projectionMatrix == null || camPos == null) return;
        VIEW_PROJ.set(projectionMatrix).mul(positionMatrix);
        camX = camPos.x;
        camY = camPos.y;
        camZ = camPos.z;
        hasMatrices = true;
    }

    /**
     * Projects a 3D world coordinate into 2D screen space.
     * Returns Vector2d(screenX, screenY), or null if behind camera.
     */
    public static Vector2d project(double wx, double wy, double wz) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return null;

        Vector4f clip;
        if (hasMatrices) {
            clip = new Vector4f(
                    (float) (wx - camX),
                    (float) (wy - camY),
                    (float) (wz - camZ),
                    1.0f
            );
            VIEW_PROJ.transform(clip);
        } else {
            // Fallback if matrices not yet captured in the first frame
            if (client.gameRenderer == null) return null;
            Camera camera = client.gameRenderer.getCamera();
            if (camera == null) return null;
            Vec3d camPos = camera.getPos();

            float fov = lastFov > 0.0F ? lastFov : (client.options != null && client.options.getFov() != null ? (float) client.options.getFov().getValue() : 70.0F);
            Matrix4f projMatrix = client.gameRenderer.getBasicProjectionMatrix(fov);
            Matrix4f viewMatrix = new Matrix4f().rotation(camera.getRotation().conjugate(new Quaternionf()));
            Matrix4f fallbackViewProj = new Matrix4f(projMatrix).mul(viewMatrix);

            clip = new Vector4f(
                    (float) (wx - camPos.x),
                    (float) (wy - camPos.y),
                    (float) (wz - camPos.z),
                    1.0f
            );
            fallbackViewProj.transform(clip);
        }

        if (clip.w <= 0.05f) {
            return null; // Behind camera
        }

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;

        int winW = client.getWindow().getScaledWidth();
        int winH = client.getWindow().getScaledHeight();

        double screenX = (ndcX * 0.5f + 0.5f) * winW;
        double screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * winH;

        if (screenX < -300 || screenX > winW + 300 || screenY < -300 || screenY > winH + 300) {
            return null;
        }

        return new Vector2d(screenX, screenY);
    }

    /**
     * Projects the exact top-center above an entity's head for perfectly centered nametag positioning.
     */
    public static Vector2d projectEntityTop(Entity entity, float tickDelta) {
        if (entity == null) return null;
        Vec3d pos = entity.getLerpedPos(tickDelta);
        double headY = pos.y + entity.getHeight() + 0.28;
        return project(pos.x, headY, pos.z);
    }
}
