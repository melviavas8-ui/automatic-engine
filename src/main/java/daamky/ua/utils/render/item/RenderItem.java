package daamky.ua.utils.render.item;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ResourceReloader.Store;
import net.minecraft.util.Identifier;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class RenderItem {
   private static boolean reloadListenerRegistered;

   private RenderItem() {
   }

   public static void init() {
      registerReloadListener();
   }

   public static void beginFrame(DrawContext graphics) {
      renderer().beginFrame(graphics);
   }

   public static void flush() {
      renderer().flush();
   }

   public static void item(String id, float x, float y, float size) {
      item(stackOf(id), x, y, size, RenderItemOptions.defaults());
   }

   public static void item(String id, float x, float y, float size, RenderItemOptions options) {
      item(stackOf(id), x, y, size, options);
   }

   public static void item(ItemStack stack, float x, float y, float size) {
      item(stack, x, y, size, RenderItemOptions.defaults());
   }

   public static void item(ItemStack stack, float x, float y, float size, float alpha) {
      item(stack, x, y, size, RenderItemOptions.decorated(alpha));
   }

   public static void item(ItemStack stack, float x, float y, float size, RenderItemOptions options) {
      BuiltRenderItem item = new BuiltRenderItem(stack, x, y, size, options, 0);
      renderer().enqueue(item);
      drawDecorations(item);
   }

   public static void close() {
      CustomItemRenderer.closeInstance();
   }

   public static void beginGuiFrame() {
      renderer().beginGuiFrame();
   }

   public static void prepareBuffers() {
      renderer().prepareBuffers();
   }

   public static boolean isItemPipeline(RenderPipeline pipeline) {
      return renderer().isItemPipeline(pipeline);
   }

   public static void bindParams(RenderPass renderPass) {
      renderer().bindParams(renderPass);
   }

   public static void clearCaches() {
      renderer().clearCaches();
   }

   private static void drawDecorations(BuiltRenderItem item) {
      if (item != null && item.visible()) {
         ItemStack stack = item.stack();
         RenderItemOptions options = item.options();
         if (options.showDurability() && stack.isItemBarVisible()) {
            drawDurabilityBar(stack, item.x(), item.y(), item.size(), options.alpha());
         }

         if (options.showCount() && stack.getCount() > 1) {
            drawCount(stack.getCount(), item.x(), item.y(), item.size(), options.alpha());
         }
      }
   }

   private static void drawDurabilityBar(ItemStack stack, float x, float y, float size, float alpha) {
      float barWidth = Math.max(8.0F, size * 0.78F);
      float barHeight = Math.max(1.5F, size * 0.07F);
      float barX = x + (size - barWidth) * 0.5F;
      float barY = y + size - barHeight - Math.max(1.0F, size * 0.08F);
      float fill = Math.max(0.0F, Math.min(1.0F, (float)stack.getItemBarStep() / 13.0F));
      int barColor = stack.getItemBarColor();
      Render2D.rect(barX, barY, barWidth, barHeight, barHeight * 0.5F, ColorUtil.rgba(0, 0, 0, Math.round(150.0F * alpha)));
      Render2D.rect(
         barX,
         barY,
         Math.max(1.0F, barWidth * fill),
         barHeight,
         barHeight * 0.5F,
         ColorUtil.rgba(barColor >>> 16 & 0xFF, barColor >>> 8 & 0xFF, barColor & 0xFF, Math.round(240.0F * alpha))
      );
   }

   private static void drawCount(int count, float x, float y, float size, float alpha) {
      String text = Integer.toString(count);
      float textSize = Math.max(4.0F, Math.min(6.0F, size * 0.5F));
      float textWidth = Render2D.textWidth(FontType.SEMIBOLD, text, textSize);
      float textX = x + size - textWidth + size * 0.08F;
      float textY = y + size - textSize + size * 0.04F;
      float shadowOffset = Math.max(0.35F, Math.min(0.75F, size * 0.07F));
      int shadowAlpha = Math.round(170.0F * alpha);
      int textAlpha = Math.round(255.0F * alpha);
      Render2D.text(FontType.SEMIBOLD, text, textX + shadowOffset, textY + shadowOffset, textSize, ColorUtil.rgba(0, 0, 0, shadowAlpha));
      Render2D.text(FontType.SEMIBOLD, text, textX, textY, textSize, ColorUtil.rgba(255, 255, 255, textAlpha));
   }

   private static ItemStack stackOf(String rawId) {
      if (rawId != null && !rawId.isBlank()) {
         Identifier id = rawId.indexOf(58) >= 0 ? Identifier.tryParse(rawId.trim()) : Identifier.ofVanilla(rawId.trim());
         if (id == null) {
            return ItemStack.EMPTY;
         } else {
            Item item = (Item)Registries.ITEM.getOptionalValue(id).orElse(null);
            return item == null ? ItemStack.EMPTY : item.getDefaultStack();
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static CustomItemRenderer renderer() {
      return CustomItemRenderer.getInstance();
   }

   private static void registerReloadListener() {
      if (!reloadListenerRegistered) {
         Identifier id = Identifier.of("daamky", "render_item_cache");
         ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(id, new SimpleResourceReloader<Void>() {
            protected Void prepare(Store state) {
               return null;
            }

            protected void apply(Void prepared, Store state) {
               RenderItem.close();
            }
         });
         reloadListenerRegistered = true;
      }
   }
}
