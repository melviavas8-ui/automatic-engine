package daamky.ua.api.drag.impl;

import java.util.List;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.misc.ServerHelper;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;

public final class Events extends HudPanel {
   public Events() {
      super("events", "Events", 140.0F, 200.0F, 104.0F, 23.0F);
   }

   @Override
   public void render() {
      Events.EventsState state = this.logics();
      if (state != null) {
         this.renderEvents(state);
      }
   }

   private Events.EventsState logics() {
      List<ServerHelper.HudEvent> events = this.events();
      boolean preview = events.isEmpty() && this.editPreview();
      boolean visible = !events.isEmpty() || preview;
      float alpha = this.contentAlpha(visible);
      if (alpha <= 0.0F) {
         return null;
      } else {
         int rows = preview ? 1 : Math.max(1, events.size());
         float width = 104.0F;
         if (preview) {
            width = Math.max(width, Render2D.textWidth(TEXT_FONT, "Example Event", 6.0F) + 68.0F);
         } else {
            for (ServerHelper.HudEvent event : events) {
               width = Math.max(width, Render2D.textWidth(TEXT_FONT, event.name(), 6.0F) + Render2D.textWidth(TEXT_FONT, event.timer(), 5.5F) + 38.0F);
            }
         }

         this.size(width, (float)rows * 13.0F + 10.0F);
         return new Events.EventsState(events, preview, alpha, this.drag.x(), this.drag.y(), this.drag.width(), this.drag.height());
      }
   }

   private void renderEvents(Events.EventsState state) {
      float rowY = state.y + 5.0F;
      HudRenderCompat.background(state.x, state.y, state.width, state.height, 4.0F, 15.0F, 1.2F, ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)));
      if (state.preview) {
         String name = "Example Event";
         String timer = "Start 1:30";
         String distanceText = this.distanceText(128.0);
         float timerWidth = Render2D.textWidth(TEXT_FONT, timer, 5.5F);
         String visibleName = this.trimToWidth(name + " " + distanceText, TEXT_FONT, 6.0F, state.width - timerWidth - 24.0F);
         Render2D.text(TEXT_FONT, visibleName, state.x + 8.0F, rowY + 1.8F, 6.0F, ColorUtil.multAlpha(TEXT_COLOR, state.alpha));
         Render2D.text(TEXT_FONT, timer, state.x + state.width - timerWidth - 8.0F, rowY + 2.1F, 5.5F, ColorUtil.multAlpha(MUTED_COLOR, state.alpha));
      } else {
         for (ServerHelper.HudEvent event : state.events) {
            String name = event.name();
            String timer = event.timer();
            String distanceText = this.distanceText(event.distance());
            float timerWidth = Render2D.textWidth(TEXT_FONT, timer, 5.5F);
            String visibleName = this.trimToWidth(name + " " + distanceText, TEXT_FONT, 6.0F, state.width - timerWidth - 24.0F);
            Render2D.text(TEXT_FONT, visibleName, state.x + 8.0F, rowY + 1.8F, 6.0F, ColorUtil.multAlpha(TEXT_COLOR, state.alpha));
            Render2D.text(TEXT_FONT, timer, state.x + state.width - timerWidth - 8.0F, rowY + 2.1F, 5.5F, ColorUtil.multAlpha(MUTED_COLOR, state.alpha));
            rowY += 13.0F;
         }
      }
   }

   private List<ServerHelper.HudEvent> events() {
      ModuleManager manager = Manager.getModules();
      return manager == null ? List.of() : manager.getByType(ServerHelper.class).map(ServerHelper::getHudEvents).orElse(List.of());
   }

   private String distanceText(double distance) {
      return !Double.isFinite(distance) ? "0m" : Math.max(0L, Math.round(distance)) + "m";
   }

   private static record EventsState(List<ServerHelper.HudEvent> events, boolean preview, float alpha, float x, float y, float width, float height) {
   }
}
