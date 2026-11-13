import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;






public class MsdfBaker {
    static final int CELL = 56, PAD = 8, BASE = 42;
    static final float SCALE = 40f;
    static final double RANGE = 8;
    static final int[] MASKS = {3, 6, 5};

    record Seg(double x0, double y0, double x1, double y1, int mask) {}
    record Glyph(int cp, Shape shape, double adv) {}

    public static void main(String[] a) throws Exception {
        Path in = Path.of(a[0]), out = Path.of(a[1]);
        Files.createDirectories(out);
        Map<String, Font> fonts = new TreeMap<>();
        try (var s = Files.list(in)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String fn = p.getFileName().toString(), l = fn.toLowerCase();
                if (!(l.endsWith(".ttf") || l.endsWith(".otf"))) continue;
                if (l.contains("variablefont") || l.contains("[wght]"))
                    System.out.println("[msdf] ВНИМАНИЕ: " + fn + " - вариативный шрифт, Java берёт его дефолтный (часто тонкий) экземпляр. Используй статические Regular/Bold.");
                fonts.put(l.substring(0, l.length() - 4).replaceAll("[^a-z0-9_\\-]", "_"),
                        Font.createFont(Font.TRUETYPE_FONT, p.toFile()));
            }
        }
        if (fonts.isEmpty()) {
            System.out.println("[msdf] fonts/ пуст -> fallback SansSerif Regular + Bold");
            fonts.put("default-regular", new Font(Font.SANS_SERIF, Font.PLAIN, 1));
            fonts.put("default-bold", new Font(Font.SANS_SERIF, Font.BOLD, 1));
        }
        List<String> names = new ArrayList<>();
        for (var e : fonts.entrySet()) { bakeFont(e.getKey(), e.getValue(), out); names.add(e.getKey()); }
        bakeGlyphs("icons", icons(), 0.9, 1.0, out);
        names.add("icons");
        StringBuilder idx = new StringBuilder("[");
        for (String n : names) { if (idx.length() > 1) idx.append(','); idx.append('"').append(n).append('"'); }
        Files.writeString(out.resolve("index.json"), idx.append(']').toString());
    }

    static void bakeFont(String name, Font base, Path out) throws Exception {
        Font f = base.deriveFont(SCALE);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        List<Integer> cps = new ArrayList<>();
        for (int c = 0x20; c < 0x7F; c++) cps.add(c);
        for (int c = 0xA0; c < 0x100; c++) cps.add(c);
        for (int c = 0x400; c < 0x500; c++) cps.add(c);
        cps.removeIf(c -> !f.canDisplay(c));
        List<Glyph> gl = new ArrayList<>();
        for (int cp : cps) {
            GlyphVector gv = f.createGlyphVector(frc, new String(Character.toChars(cp)));
            gl.add(new Glyph(cp, gv.getOutline(), gv.getGlyphMetrics(0).getAdvanceX() / SCALE));
        }
        LineMetrics lm = f.getLineMetrics("Hg", frc);
        bakeGlyphs(name, gl, lm.getAscent() / SCALE, (lm.getAscent() + lm.getDescent() + lm.getLeading()) / SCALE, out);
    }

    static void bakeGlyphs(String name, List<Glyph> gl, double ascent, double lineHeight, Path out) throws Exception {
        int n = gl.size(), cols = (int) Math.ceil(Math.sqrt(n)), rows = (n + cols - 1) / cols;
        int W = cols * CELL, H = rows * CELL;
        BufferedImage msdf = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) msdf.setRGB(x, y, 0xFF000000);
        StringBuilder js = new StringBuilder();
        java.io.ByteArrayOutputStream segBytes = new java.io.ByteArrayOutputStream();
        int segCount = 0;
        for (int i = 0; i < n; i++) {
            Glyph g = gl.get(i);
            Shape sh = g.shape();
            List<Seg> segs = sh.getBounds2D().isEmpty() ? List.of() : segments(sh);
            int segStart = segCount;
            for (Seg s : segs) {
                for (double v : new double[]{s.x0(), s.y0(), s.x1(), s.y1()}) {
                    int e = (int) Math.max(0, Math.min(65535, Math.round((v + 128.0) * 256.0)));
                    segBytes.write(e >> 8); segBytes.write(e & 255);
                }
                segCount++;
            }
            if (i > 0) js.append(',');
            js.append('"').append(g.cp()).append("\":[").append(i).append(',')
                    .append(String.format(Locale.ROOT, "%.5f", g.adv())).append(',').append(segStart).append(',').append(segs.size()).append(']');
            if (segs.isEmpty()) continue;
            int ox = (i % cols) * CELL, oy = (i / cols) * CELL;
            for (int y = 0; y < CELL; y++) for (int x = 0; x < CELL; x++) {
                double px = x + 0.5 - PAD, py = y + 0.5 - BASE;
                double[] d = {1e9, 1e9, 1e9};
                for (Seg s : segs) {
                    double dd = dist(px, py, s);
                    for (int c = 0; c < 3; c++) if ((s.mask >> c & 1) != 0 && dd < d[c]) d[c] = dd;
                }
                double sg = sh.contains(px, py) ? 1 : -1;
                msdf.setRGB(ox + x, oy + y, 0xFF000000 | enc(sg * d[0]) << 16 | enc(sg * d[1]) << 8 | enc(sg * d[2]));
            }
        }
        ImageIO.write(msdf, "png", out.resolve(name + "_msdf.png").toFile());
        Files.write(out.resolve(name + "_seg.bin"), segBytes.toByteArray());
        Files.writeString(out.resolve(name + ".json"), String.format(Locale.ROOT,
                "{\"cell\":%d,\"pad\":%d,\"base\":%d,\"scale\":%.1f,\"range\":%.1f,\"cols\":%d,\"w\":%d,\"h\":%d,\"ascent\":%.5f,\"lineHeight\":%.5f,\"glyphs\":{%s}}",
                CELL, PAD, BASE, SCALE, RANGE, cols, W, H, ascent, lineHeight, js));
        System.out.println("[msdf] baked " + name + " (" + n + " glyphs, " + W + "x" + H + ")");
    }

    static int enc(double signed) { return (int) Math.round(Math.max(0, Math.min(1, 0.5 + signed / RANGE)) * 255); }

    static double dist(double px, double py, Seg s) {
        double dx = s.x1 - s.x0, dy = s.y1 - s.y0, l2 = dx * dx + dy * dy;
        double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - s.x0) * dx + (py - s.y0) * dy) / l2));
        return Math.hypot(px - (s.x0 + t * dx), py - (s.y0 + t * dy));
    }

    static List<Seg> segments(Shape sh) {
        List<Seg> all = new ArrayList<>();
        List<double[]> pts = new ArrayList<>();
        PathIterator it = new FlatteningPathIterator(sh.getPathIterator(null), 0.02, 12);
        double[] c = new double[6];
        for (; !it.isDone(); it.next()) {
            int t = it.currentSegment(c);
            if (t == PathIterator.SEG_MOVETO) { contour(pts, all); pts = new ArrayList<>(); pts.add(new double[]{c[0], c[1]}); }
            else if (t == PathIterator.SEG_LINETO) pts.add(new double[]{c[0], c[1]});
            else if (t == PathIterator.SEG_CLOSE) { contour(pts, all); pts = new ArrayList<>(); }
        }
        contour(pts, all);
        return all;
    }

    static double d(double[] a, double[] b) { return Math.hypot(a[0] - b[0], a[1] - b[1]); }

    static void contour(List<double[]> p, List<Seg> out) {
        List<double[]> q = new ArrayList<>();
        for (double[] v : p) if (q.isEmpty() || d(q.get(q.size() - 1), v) > 1e-6) q.add(v);
        if (q.size() > 1 && d(q.get(0), q.get(q.size() - 1)) < 1e-6) q.remove(q.size() - 1);
        int n = q.size();
        if (n < 3) return;
        boolean[] corner = new boolean[n];
        int cc = 0, start = -1;
        for (int i = 0; i < n; i++) {
            double[] pr = q.get((i - 1 + n) % n), cu = q.get(i), nx = q.get((i + 1) % n);
            double a1 = Math.atan2(cu[1] - pr[1], cu[0] - pr[0]), a2 = Math.atan2(nx[1] - cu[1], nx[0] - cu[0]);
            double df = Math.abs(a2 - a1);
            if (df > Math.PI) df = 2 * Math.PI - df;
            if (df > 0.6) { corner[i] = true; cc++; if (start < 0) start = i; }
        }
        int[] col = new int[n];
        if (cc < 2) {
            for (int i = 0; i < n; i++) col[i] = Math.min(2, i * 3 / n);
        } else {
            int idx = -1;
            for (int k = 0; k < n; k++) {
                int i = (start + k) % n;
                if (corner[i]) idx++;
                int cl = idx % 3;
                if (idx == cc - 1 && cl == 0) cl = 1;
                col[i] = cl;
            }
        }
        for (int i = 0; i < n; i++) {
            double[] a = q.get(i), b = q.get((i + 1) % n);
            out.add(new Seg(a[0], a[1], b[0], b[1], MASKS[col[i]]));
        }
    }



    static Area disc(double cx, double cy, double r) { return new Area(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r)); }
    static Area ring(double cx, double cy, double r, double w) {
        Area a = disc(cx, cy, r + w / 2); a.subtract(disc(cx, cy, r - w / 2)); return a;
    }
    static Area ln(double x0, double y0, double x1, double y1, double w) {
        return new Area(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(new Line2D.Double(x0, y0, x1, y1)));
    }
    static Area stk(Shape s, double w) {
        return new Area(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(s));
    }
    static Area rr(double x, double y, double w, double h, double r) { return new Area(new RoundRectangle2D.Double(x, y, w, h, 2 * r, 2 * r)); }
    static Area rot(Area a, double deg, double cx, double cy) { return a.createTransformedArea(AffineTransform.getRotateInstance(Math.toRadians(deg), cx, cy)); }
    static Area add(Area... as) { Area r = new Area(); for (Area x : as) r.add(x); return r; }


    static void semi(Path2D p, double cx, double cy, double x0, double y0) {
        double r = Math.hypot(x0 - cx, y0 - cy), a0 = Math.atan2(y0 - cy, x0 - cx);
        for (int i = 1; i <= 32; i++) {
            double a = a0 + Math.PI * i / 32.0;
            p.lineTo(cx + r * Math.cos(a), cy + r * Math.sin(a));
        }
    }

    static List<Glyph> icons() {
        List<Area> L = new ArrayList<>();

        L.add(add(ring(.5, .5, .3, .09), ln(.5, .06, .5, .26, .09), ln(.5, .74, .5, .94, .09), ln(.06, .5, .26, .5, .09), ln(.74, .5, .94, .5, .09), disc(.5, .5, .06)));

        L.add(add(new Area(new Ellipse2D.Double(.1, .4, .58, .44)), disc(.72, .54, .17), disc(.66, .3, .1),
                stk(new QuadCurve2D.Double(.14, .68, .0, .74, .08, .92), .06)));

        Path2D lens = new Path2D.Double();
        lens.moveTo(.04, .5); lens.quadTo(.5, -.12, .96, .5); lens.quadTo(.5, 1.12, .04, .5); lens.closePath();
        L.add(add(stk(lens, .08), disc(.5, .5, .13), ln(.14, .9, .86, .1, .09)));

        Area sh = new Area(new Ellipse2D.Double(.14, .6, .72, .8)); sh.intersect(new Area(new Rectangle2D.Double(0, 0, 1, .97)));
        L.add(add(disc(.5, .27, .2), sh));

        L.add(add(ln(.18, .82, .62, .38, .13), ln(.82, .82, .38, .38, .13), disc(.7, .3, .17), rot(rr(.08, .1, .34, .18, .05), -45, .25, .19)));

        Area cf = add(rr(.04, .28, .92, .64, .09), rr(.04, .14, .42, .3, .06)); cf.subtract(disc(.5, .6, .17)); cf.add(disc(.5, .6, .08));
        L.add(cf);

        Area pal = new Area(new Ellipse2D.Double(.04, .1, .92, .8));
        for (double[] h : new double[][]{{.28, .5, .08}, {.46, .3, .08}, {.7, .34, .08}, {.78, .58, .08}, {.58, .78, .12}}) pal.subtract(disc(h[0], h[1], h[2]));
        L.add(pal);

        L.add(add(rr(.08, .18, .14, .14, .03), rr(.32, .2, .6, .1, .04), rr(.08, .43, .14, .14, .03), rr(.32, .45, .6, .1, .04), rr(.08, .68, .14, .14, .03), rr(.32, .7, .6, .1, .04)));

        Area pw = ring(.5, .55, .34, .11); pw.subtract(rr(.4, 0, .2, .45, 0));
        L.add(add(pw, ln(.5, .1, .5, .52, .11)));

        Area gear = disc(.5, .5, .31);
        for (int k = 0; k < 8; k++) gear.add(rot(rr(.41, .04, .18, .24, .04), k * 45, .5, .5));
        gear.subtract(disc(.5, .5, .15));
        L.add(gear);

        Path2D clip = new Path2D.Double();
        clip.moveTo(21.44, 11.05); clip.lineTo(12.25, 20.24);
        semi(clip, 8.005, 15.995, 12.25, 20.24);
        clip.lineTo(12.33, 3.18);
        semi(clip, 15.165, 6.01, 12.33, 3.18);
        clip.lineTo(9.41, 17.41);
        semi(clip, 7.995, 15.995, 9.41, 17.41);
        clip.lineTo(15.07, 6.1);
        clip.transform(AffineTransform.getScaleInstance(1 / 24.0, 1 / 24.0));
        L.add(stk(clip, .085));

        L.add(add(ring(.42, .42, .28, .1), ln(.64, .64, .92, .92, .13)));

        L.add(add(rr(.04, .28, .92, .64, .09), rr(.04, .14, .42, .3, .06)));

        L.add(add(ln(.18, .18, .82, .82, .13), ln(.82, .18, .18, .82, .13)));

        double ow = .062;
        Area[] orb = new Area[3];
        double[] angs = {-20, 40, 100};
        for (int k = 0; k < 3; k++)
            orb[k] = rot(stk(new Ellipse2D.Double(.08, .26, .84, .48), ow), angs[k], .5, .5);

        Area[] cut = new Area[3];
        double[][] half = {{0, 0, 1, .5}, {.5, 0, .5, 1}, {0, .5, 1, .5}};
        for (int k = 0; k < 3; k++) {
            Area gap = rot(stk(new Ellipse2D.Double(.08, .26, .84, .48), ow * 2.3), angs[(k + 1) % 3], .5, .5);
            gap.intersect(new Area(new Rectangle2D.Double(half[k][0], half[k][1], half[k][2], half[k][3])));
            Area o = new Area(orb[k]); o.subtract(gap); cut[k] = o;
        }
        Area atom = add(cut[0], cut[1], cut[2], ring(.5, .5, .11, .062));
        L.add(atom);

        Path2D ch = new Path2D.Double(); ch.moveTo(.14, .3); ch.lineTo(.86, .3); ch.lineTo(.5, .74); ch.closePath();
        L.add(add(new Area(ch), stk(ch, .1)));

        AffineTransform t = new AffineTransform();
        t.translate(0, -0.9 * SCALE);
        t.scale(SCALE, SCALE);
        List<Glyph> out = new ArrayList<>();
        for (int i = 0; i < L.size(); i++) out.add(new Glyph(0xE000 + i, t.createTransformedShape(L.get(i)), 1.0));
        return out;
    }
}
