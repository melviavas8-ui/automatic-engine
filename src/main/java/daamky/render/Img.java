package daamky.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;





public final class Img {
    private static final Map<String, int[]> SIZE = new HashMap<>();

    private Img() {}

    public static Identifier id(String path) { return Identifier.of("daamky", "textures/gui/" + path); }

    private static int[] size(String path) {
        return SIZE.computeIfAbsent(path, p -> {
            try (var in = MinecraftClient.getInstance().getResourceManager().open(id(p)); NativeImage ni = NativeImage.read(in)) {
                return new int[]{ni.getWidth(), ni.getHeight()};
            } catch (Exception e) {
                return new int[]{0, 0};
            }
        });
    }





    public static boolean draw(Matrix3x2f m, String path, float x, float y, float w, float h, int color) {
        var ctx = Gfx.ctx;
        if (ctx == null || Gfx.scissor != null) return false;
        int[] sz = size(path);
        int tw = sz[0], th = sz[1];
        if (tw <= 0 || th <= 0) return false;
        float k = Math.min(w / tw, h / th);
        float dx = x + (w - tw * k) / 2f, dy = y + (h - th * k) / 2f;
        var st = ctx.getMatrices();
        st.pushMatrix();
        st.set(m);
        st.translate(dx, dy);
        st.scale(k, k);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, id(path), 0, 0, 0f, 0f, tw, th, tw, th, tw, th, color);
        st.popMatrix();
        return true;
    }


    public static String forIcon(int cp) {
        return switch (cp) {
            case Icons.COMBAT -> "category/combat.png";
            case Icons.MOVEMENT -> "category/movement.png";
            case Icons.RENDER -> "category/render.png";
            case Icons.PLAYER -> "category/player.png";
            case Icons.UTIL -> "category/util.png";
            case Icons.CONFIGS -> "category/configs.png";
            case Icons.THEMES -> "category/themes.png";
            case Icons.ITEMS -> "category/autobuy_items.png";
            case Icons.POWER -> "logout.png";
            case Icons.GEAR -> "gear.png";
            case Icons.SEARCH -> "search.png";
            case Icons.FOLDER -> "folder.png";
            case Icons.CLOSE -> "close.png";
            case Icons.ATOM -> "atom64.png";
            default -> null;
        };
    }
}
