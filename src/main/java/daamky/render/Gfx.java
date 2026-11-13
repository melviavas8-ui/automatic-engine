package daamky.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.texture.*;
import net.minecraft.client.gl.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.render.*;
import net.minecraft.client.render.*;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;


public final class Gfx {
    public static final VertexFormat FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;

    public static DrawContext ctx;

    public static ScreenRect scissor;

    public static final RenderPipeline RECT = gui("rect", false);
    public static final RenderPipeline MSDF = gui("msdf", true);
    public static final RenderPipeline SDF = gui("sdf", true);
    public static final RenderPipeline SDFGEN = RenderPipeline.builder()
            .withLocation(Identifier.of("daamky", "pipeline/sdfgen"))
            .withVertexShader(Identifier.of("daamky", "core/sdfgen"))
            .withFragmentShader(Identifier.of("daamky", "core/sdfgen"))
            .withSampler("Sampler0")
            .withVertexFormat(FORMAT, VertexFormat.DrawMode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    private Gfx() {}

    private static RenderPipeline gui(String n, boolean sampler) {
        var b = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(Identifier.of("daamky", "pipeline/" + n))
                .withVertexShader(Identifier.of("daamky", "core/" + n))
                .withFragmentShader(Identifier.of("daamky", "core/" + n))
                .withVertexFormat(FORMAT, VertexFormat.DrawMode.QUADS)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false);
        if (sampler) b = b.withSampler("Sampler0");
        return b.build();
    }


    public static final class Batch {
        private float[] d = new float[11 * 64];
        private int n;
        private float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        private final Matrix3x2f pose;
        private final Vector2f t = new Vector2f();

        public Batch(Matrix3x2f pose) { this.pose = pose; }

        public boolean isEmpty() { return n == 0; }

        public void v(float x, float y, float z, int color, float u, float v, int o1x, int o1y, int o2x, int o2y, float nx) {
            pose.transformPosition(x, y, t);
            if (d.length < (n + 1) * 11) d = java.util.Arrays.copyOf(d, d.length * 2);
            int i = n++ * 11;
            d[i] = t.x; d[i + 1] = t.y; d[i + 2] = z;
            d[i + 3] = Float.intBitsToFloat(color);
            d[i + 4] = u; d[i + 5] = v;
            d[i + 6] = o1x; d[i + 7] = o1y; d[i + 8] = o2x; d[i + 9] = o2y;
            d[i + 10] = nx;
            minX = Math.min(minX, t.x); maxX = Math.max(maxX, t.x);
            minY = Math.min(minY, t.y); maxY = Math.max(maxY, t.y);
        }

        public void submit(RenderPipeline pl, TextureSetup ts) {
            if (n == 0 || ctx == null) return;
            int x = (int) Math.floor(minX), y = (int) Math.floor(minY);
            ScreenRect b = new ScreenRect(x, y, (int) Math.ceil(maxX) - x + 1, (int) Math.ceil(maxY) - y + 1);
            ScreenRect sc = scissor;
            if (sc != null) {
                b = b.intersection(sc);
                if (b == null) return;
            }
            ctx.state.addSimpleElement(new State(pl, ts, sc, b, java.util.Arrays.copyOf(d, n * 11), n));
        }
    }

    private record State(RenderPipeline pipeline, TextureSetup textureSetup, ScreenRect scissorArea, ScreenRect bounds,
                         float[] d, int n) implements SimpleGuiElementRenderState {
        @Override
        public void setupVertices(VertexConsumer vc) {
            for (int k = 0; k < n; k++) {
                int i = k * 11;
                vc.vertex(d[i], d[i + 1], d[i + 2])
                        .color(Float.floatToRawIntBits(d[i + 3]))
                        .texture(d[i + 4], d[i + 5])
                        .overlay((int) d[i + 6], (int) d[i + 7])
                        .light((int) d[i + 8], (int) d[i + 9])
                        .normal(d[i + 10], 0f, 0f);
            }
        }
    }
}
