package daamky.ua.api.module.impl.visual.particles;

import net.minecraft.util.Identifier;
import daamky.ua.utils.render.color.ColorUtil;

final class ParticleConstants {
   static final String ROOT = "textures/features/particles/";
   static final int CLIENT_ACCENT = ColorUtil.rgba(127, 242, 255, 255);
   static final int MAX_PARTICLES = 1800;
   static final float LOCK_XZ_TRIGGER_YAW = -1337.1488F;
   static final String EVENT_WORLD = "World";
   static final String EVENT_ATTACKS = "Attacks";
   static final String TEXTURE_CROSS = "Cross";
   static final String TEXTURE_CROWN = "Crown";
   static final String TEXTURE_DOLLAR = "Dollar";
   static final String TEXTURE_HEART = "Heart";
   static final String TEXTURE_LIGHTNING = "Lightning";
   static final String TEXTURE_LINE = "Line";
   static final String TEXTURE_POINT = "Point";
   static final String TEXTURE_RHOMBUS = "Rhombus";
   static final String TEXTURE_SNOWFLAKE = "Snowflake";
   static final String TEXTURE_STAR = "Star";
   static final String TEXTURE_TRIANGLE = "Triangle";
   static final String RENDER_TEXTURES = "Textures";
   static final String RENDER_CUBES = "Cubes";
   static final String RENDER_TEXTURES_AND_CUBES = "Textures and Cubes";
   static final String RENDER_RANDOM = "Random";
   static final String RULE_NO_XZ = "No XZ Push";
   static final String RULE_RANDOM = "Random";
   static final String RULE_TO_SOURCE = "To Source";
   static final String RULE_FROM_SOURCE = "From Source";
   static final String RULE_CAMERA_LEFT = "Camera Left";
   static final String RULE_CAMERA_RIGHT = "Camera Right";
   static final String RULE_CAMERA_FORWARD = "Camera Forward";
   static final String RULE_CAMERA_BACK = "Camera Back";
   static final String RULE_NONE = "None";
   static final String RULE_TWIST_LEFT = "Twist Left";
   static final String RULE_TWIST_RIGHT = "Twist Right";
   static final String RULE_TWIST_RANDOM = "Twist Random";
   static final String RULE_ALTERNATE_SOURCE = "Alternate by Source";
   static final String COLLIDE_IGNORE = "Ignore";
   static final String COLLIDE_RICOCHET = "Ricochet";
   static final String COLLIDE_STRONG_RICOCHET = "Strong Ricochet";
   static final String COLLIDE_VERY_STRONG_RICOCHET = "Very Strong Ricochet";
   static final String COLLIDE_WEAK_RICOCHET = "Weak Ricochet";
   static final String COLLIDE_VERY_WEAK_RICOCHET = "Very Weak Ricochet";
   static final String COLLIDE_STUCK = "Stuck";
   static final String COLLIDE_FATAL = "Fatal";
   static final String COLLIDE_SLIDE = "Slide";
   static final String COLLIDE_SLIDE_SLOW = "Slide Slow";
   static final String[] EVENT_MODES = new String[]{"World", "Attacks"};
   static final String[] TEXTURE_MODES = new String[]{
      "Cross", "Crown", "Dollar", "Heart", "Lightning", "Line", "Point", "Rhombus", "Snowflake", "Star", "Triangle"
   };
   static final String[] RENDER_MODES = new String[]{"Textures", "Cubes", "Textures and Cubes", "Random"};
   static final String[] SPAWN_RULES = new String[]{
      "No XZ Push", "Random", "To Source", "From Source", "Camera Left", "Camera Right", "Camera Forward", "Camera Back"
   };
   static final String[] FORCE_RULES = new String[]{
      "None",
      "Camera Left",
      "Camera Right",
      "Camera Forward",
      "Camera Back",
      "Twist Left",
      "Twist Right",
      "Twist Random",
      "To Source",
      "From Source",
      "Alternate by Source"
   };
   static final String[] COLLISION_MODES = new String[]{
      "Ignore", "Ricochet", "Strong Ricochet", "Very Strong Ricochet", "Weak Ricochet", "Very Weak Ricochet", "Stuck", "Fatal", "Slide", "Slide Slow"
   };
   static final ParticleTexture[] TEXTURES = new ParticleTexture[]{
      particle("Cross", "cross.png"),
      particle("Crown", "crown.png"),
      particle("Dollar", "dollar.png"),
      particle("Heart", "heart.png"),
      particle("Lightning", "lighting.png"),
      particle("Line", "line.png"),
      particle("Point", "point.png"),
      particle("Rhombus", "rhombus.png"),
      particle("Snowflake", "snowflake.png"),
      particle("Star", "star.png"),
      particle("Triangle", "triangle.png")
   };

   private ParticleConstants() {
   }

   private static ParticleTexture particle(String mode, String path) {
      return new ParticleTexture(mode, Identifier.of("daamky", "textures/features/particles/" + path));
   }
}
