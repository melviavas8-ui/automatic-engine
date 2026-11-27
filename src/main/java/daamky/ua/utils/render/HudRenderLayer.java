package daamky.ua.utils.render;

import daamky.ua.api.events.impl.DrawEvent;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.ui.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class HudRenderLayer {
   private HudRenderLayer() {
   }

   public static boolean shouldRender(Minecraft client) {
      return client != null && client.player != null && client.level != null && client.getWindow() != null && !LoadingVisualGuard.shouldSuppressHud(client);
   }

   public static void renderGame(GuiGraphics graphics, float partialTick) {
      renderGame(Minecraft.getInstance(), graphics, partialTick);
   }

   public static void renderGame(Minecraft client, GuiGraphics graphics, float partialTick) {
      renderComposite(client, graphics, partialTick, DrawEvent.Layer.GAME);
   }

   public static void renderGameDrawEvents(GuiGraphics graphics, float partialTick) {
      renderDrawEvent(graphics, partialTick, DrawEvent.Layer.GAME);
   }

   public static void renderGameHud(Minecraft client, GuiGraphics graphics, float partialTick) {
      renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.GAME);
   }

   public static void renderScreenBackground(Minecraft client, GuiGraphics graphics, float partialTick) {
      if (shouldRender(client) && client.screen != null) {
         renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.SCREEN_BACKGROUND);
      }
   }

   public static void renderChatOverlay(Minecraft client, GuiGraphics graphics, float partialTick) {
      renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.CHAT_OVERLAY);
   }

   public static void renderChatBackground(Minecraft client, GuiGraphics graphics, float partialTick) {
      renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.CHAT_BACKGROUND);
   }

   private static void renderComposite(Minecraft client, GuiGraphics graphics, float partialTick, DrawEvent.Layer hudLayer) {
      if (shouldRender(client)) {
         renderDrawEvent(graphics, partialTick, DrawEvent.Layer.GAME);
         renderHudOnly(client, graphics, partialTick, hudLayer);
      }
   }

   private static void renderHudOnly(Minecraft client, GuiGraphics graphics, float partialTick, DrawEvent.Layer layer) {
      if (shouldRender(client)) {
         Hud hud = Hud.getInstance();
         if (hud != null && hud.isEnabled()) {
            daamky.hud.DaamkyHud.render(graphics);
            graphics.nextStratum();
         }
      }
   }

   private static void renderDrawEvent(GuiGraphics graphics, float partialTick, DrawEvent.Layer layer) {
      Render2D.beginFrame(graphics);
      Manager.postEvent(new DrawEvent(graphics, partialTick, layer));
      Render2D.flush();
      graphics.nextStratum();
   }
}
