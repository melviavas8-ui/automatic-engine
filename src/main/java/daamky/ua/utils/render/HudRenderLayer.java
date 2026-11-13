package daamky.ua.utils.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.api.events.impl.DrawEvent;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.ui.Render2D;

public final class HudRenderLayer {
   private HudRenderLayer() {
   }

   public static boolean shouldRender(MinecraftClient client) {
      return client != null && client.player != null && client.world != null && client.getWindow() != null && !LoadingVisualGuard.shouldSuppressHud(client);
   }

   public static void renderGame(DrawContext graphics, float partialTick) {
      renderGame(MinecraftClient.getInstance(), graphics, partialTick);
   }

   public static void renderGame(MinecraftClient client, DrawContext graphics, float partialTick) {
      renderComposite(client, graphics, partialTick, DrawEvent.Layer.GAME);
   }

   public static void renderGameDrawEvents(DrawContext graphics, float partialTick) {
      renderDrawEvent(graphics, partialTick, DrawEvent.Layer.GAME);
   }

   public static void renderGameHud(MinecraftClient client, DrawContext graphics, float partialTick) {
      renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.GAME);
   }

   public static void renderScreenBackground(MinecraftClient client, DrawContext graphics, float partialTick) {
      if (shouldRender(client) && client.currentScreen != null) {
         renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.SCREEN_BACKGROUND);
      }
   }

   public static void renderChatOverlay(MinecraftClient client, DrawContext graphics, float partialTick) {
      renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.CHAT_OVERLAY);
   }

   public static void renderChatBackground(MinecraftClient client, DrawContext graphics, float partialTick) {
      renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.CHAT_BACKGROUND);
   }

   private static void renderComposite(MinecraftClient client, DrawContext graphics, float partialTick, DrawEvent.Layer hudLayer) {
      if (shouldRender(client)) {
         renderDrawEvent(graphics, partialTick, DrawEvent.Layer.GAME);
         renderHudOnly(client, graphics, partialTick, hudLayer);
      }
   }

   private static void renderHudOnly(MinecraftClient client, DrawContext graphics, float partialTick, DrawEvent.Layer layer) {
      if (shouldRender(client)) {
         Hud hud = Hud.getInstance();
         if (hud != null && hud.isEnabled()) {
            daamky.hud.DaamkyHud.render(graphics);
            graphics.createNewRootLayer();
         }
      }
   }

   private static void renderDrawEvent(DrawContext graphics, float partialTick, DrawEvent.Layer layer) {
      Render2D.beginFrame(graphics);
      Manager.postEvent(new DrawEvent(graphics, partialTick, layer));
      Render2D.flush();
      graphics.createNewRootLayer();
   }
}
