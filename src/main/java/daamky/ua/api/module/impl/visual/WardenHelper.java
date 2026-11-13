package daamky.ua.api.module.impl.visual;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.drag.impl.HudRenderCompat;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.DrawEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.visual.esp.EspGeometry;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public class WardenHelper extends Module {
   private static boolean active;
   private static final Map<BlockPos, WardenHelper.SavedTimer> timers = new HashMap<>();

   public WardenHelper() {
      super("WardenHelper", "", ModuleCategory.PLAYER);
   }

   public static Map<BlockPos, WardenHelper.SavedTimer> getTimers() {
      return Map.copyOf(timers);
   }

   @Override
   protected void onEnable() {
      active = true;
      timers.clear();
   }

   @Override
   protected void onDisable() {
      active = false;
      timers.clear();
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      updateTimers();
   }

   public static void updateTimers() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world != null && client.player != null) {
         long now = System.currentTimeMillis();

         for (Entity entity : client.world.getEntities()) {
            String text = timerText(entity);
            if (text.matches("\\d{2}:\\d{2}")) {
               Vec3d pos = entity.getEntityPos().add(0.0, 0.15, 0.0);
               timers.put(BlockPos.ofFloored(pos), new WardenHelper.SavedTimer(pos, parseSeconds(text), now));
            }
         }
      }
   }

   @SubscribeEvent
   private void onDraw(DrawEvent event) {
      if (mc.player != null && mc.world != null && mc.gameRenderer != null) {
         EspGeometry.ProjectionContext context = EspGeometry.createProjectionContext(mc, event.getPartialTicks());
         if (context != null) {
            long now = System.currentTimeMillis();
            Iterator<Entry<BlockPos, WardenHelper.SavedTimer>> iterator = timers.entrySet().iterator();

            while (iterator.hasNext()) {
               WardenHelper.SavedTimer timer = iterator.next().getValue();
               int left = timer.seconds - (int)((now - timer.seenAtMs) / 1000L);
               if (left <= 0) {
                  iterator.remove();
               } else {
                  EspGeometry.ScreenPoint screen = EspGeometry.projectWorldToScreenSpace(timer.pos, context);
                  if (screen != null) {
                     this.drawTimer(formatSeconds(left), (float)screen.x(), (float)screen.y());
                  }
               }
            }
         }
      }
   }

   public static boolean shouldHideVanillaTimer(Entity entity) {
      return active && timerText(entity).matches("\\d{2}:\\d{2}");
   }

   private void drawTimer(String text, float screenX, float screenY) {
      float textSize = 6.5F;
      float textWidth = Render2D.textWidth(FontType.SEMIBOLD, text, textSize);
      float x = screenX - textWidth / 2.0F - 5.0F;
      float y = screenY - 8.0F;
      float w = textWidth + 10.0F;
      float h = 13.0F;
      HudRenderCompat.background(
         Render2D.blurBuilder()
            .rectangle(x, y, w + 8.0F, h)
            .radius(2.0F, 7.0F, 2.0F, 7.0F)
            .blurRadius(15.0F)
            .smoothness(1.2F)
            .color(ColorUtil.rgba(0, 0, 0, 235))
            .build()
      );
      Render2D.text(FontType.SEMIBOLD, text, x + 7.5F, y + 3.0F, textSize, ColorUtil.rgba(255, 80, 80, 255));
   }

   private static String timerText(Entity entity) {
      Text component = entity instanceof TextDisplayEntity textDisplay ? textDisplay.getText() : null;
      if (component == null) {
         component = entity.getCustomName();
      }

      return component == null ? "" : component.getString().trim();
   }

   private static int parseSeconds(String text) {
      String[] parts = text.split(":");
      return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
   }

   private static String formatSeconds(int seconds) {
      return String.format("%02d:%02d", seconds / 60, seconds % 60);
   }

   public static record SavedTimer(Vec3d pos, int seconds, long seenAtMs) {
      public int leftSeconds() {
         return Math.max(0, this.seconds - (int)((System.currentTimeMillis() - this.seenAtMs) / 1000L));
      }
   }
}
