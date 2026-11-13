package daamky.ua.api.drag.impl;

import java.util.ArrayList;
import java.util.List;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;

public final class Info extends HudPanel {
   private final List<String> rows = new ArrayList<>(3);
   private double lastX;
   private double lastZ;
   private long lastSampleMs;
   private double blocksPerSecond;

   public Info() {
      super("info", "Info", 10.0F, 70.0F, 92.0F, 50.0F);
   }

   @Override
   public void render() {
      Info.InfoState state = this.logics();
      if (state != null) {
         this.renderInfo(state);
      }
   }

   private Info.InfoState logics() {
      this.contentVisible(this.selected());
      if (this.mc.player == null) {
         return null;
      } else {
         this.updateSpeed();
         this.rows.clear();
         this.rows.add("FPS: " + this.mc.getCurrentFps());
         this.rows.add("XYZ: " + Math.round(this.mc.player.getX()) + " " + Math.round(this.mc.player.getY()) + " " + Math.round(this.mc.player.getZ()));
         this.rows.add("BPS: " + this.formatBps(this.blocksPerSecond));
         float width = 92.0F;

         for (String row : this.rows) {
            width = Math.max(width, Render2D.textWidth(TEXT_FONT, row, 6.0F) + 16.0F);
         }

         this.size(width, (float)this.rows.size() * 11.0F + 10.0F);
         return new Info.InfoState(this.rows, this.drag.x(), this.drag.y(), this.drag.width(), this.drag.height());
      }
   }

   private void renderInfo(Info.InfoState state) {
      float rowY = state.y + 5.0F;
      HudRenderCompat.background(state.x, state.y, state.width, state.height, 4.0F, 15.0F, 1.2F, ColorUtil.rgba(0, 0, 0, 255));

      for (String row : state.rows) {
         Render2D.text(TEXT_FONT, row, state.x + 8.0F, rowY + 1.8F, 6.0F, TEXT_COLOR);
         rowY += 11.0F;
      }
   }

   private void updateSpeed() {
      long now = System.currentTimeMillis();
      if (this.lastSampleMs == 0L) {
         this.lastSampleMs = now;
         this.lastX = this.mc.player.getX();
         this.lastZ = this.mc.player.getZ();
      } else {
         long deltaMs = now - this.lastSampleMs;
         if (deltaMs >= 200L) {
            double dx = this.mc.player.getX() - this.lastX;
            double dz = this.mc.player.getZ() - this.lastZ;
            double instant = Math.sqrt(dx * dx + dz * dz) / Math.max(0.001, (double)deltaMs / 1000.0);
            this.blocksPerSecond = this.blocksPerSecond * 0.65 + instant * 0.35;
            this.lastSampleMs = now;
            this.lastX = this.mc.player.getX();
            this.lastZ = this.mc.player.getZ();
         }
      }
   }

   private String formatBps(double value) {
      int scaled = Math.max(0, (int)Math.round(value * 100.0));
      int whole = scaled / 100;
      int fraction = scaled % 100;
      return whole + "." + twoDigits(fraction);
   }

   private static record InfoState(List<String> rows, float x, float y, float width, float height) {
   }
}
