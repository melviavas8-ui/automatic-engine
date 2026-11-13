package daamky.ua.api.module.impl.visual.music;

/** Per-frame glow/colour parameters shared between the Music Lyrics module and its particles. */
public final class LyricGlow {
    /** Opaque RGB of the text itself. */
    public int textColor = 0xF7FAFF;
    /** Primary glow colour (RGB). */
    public int colorA = 0x7896FF;
    /** Secondary glow colour (RGB), used by gradient mode. */
    public int colorB = 0xFF6EC8;
    public boolean gradient;
    public boolean rainbow;
    /** Overall brightness of the glow, 0..3. */
    public float intensity = 1.2F;
    /** Radius multiplier of the soft outer halo. */
    public float size = 1.0F;
    /** Strength of the tight halo hugging the letters, 0..2. */
    public float core = 1.0F;
    public boolean pulse = true;
    public float pulseSpeed = 1.0F;
    /** How long (ms) the halo keeps glowing after the text has disappeared. */
    public long afterglowMs = 700L;
}
