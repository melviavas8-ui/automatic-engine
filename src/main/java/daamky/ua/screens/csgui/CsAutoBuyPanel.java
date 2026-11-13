package daamky.ua.screens.csgui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import daamky.ua.api.module.impl.player.AutoBuy;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.manager.Manager;
import daamky.ua.utils.modules.autobuy.AutoBuyItem;
import daamky.ua.utils.modules.autobuy.AutoBuyManager;
import daamky.ua.utils.modules.autobuy.catalog.AutoBuyItemCategory;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class CsAutoBuyPanel {
   private static final FontType FONT = FontType.SEMIBOLD;
   private static final FontType FONT_BOLD = FontType.BOLD;
   private static final String[] SERVERS = new String[]{"FunTime", "SpookyTime", "HolyWorld"};
   private static final String[] SERVER_SHORT = new String[]{"FT", "SP", "HW"};
   private static final String[] PAGE_LABELS = new String[]{"КАТАЛОГ", "ПОКУПКИ", "НАСТРОЙКИ"};
   private static final AutoBuyItemCategory[] CATS_FT = new AutoBuyItemCategory[]{
      null, AutoBuyItemCategory.KRUSH, AutoBuyItemCategory.SPHERES, AutoBuyItemCategory.TALISMANS, AutoBuyItemCategory.POTIONS, AutoBuyItemCategory.MISC
   };
   private static final String[] CAT_LABELS_FT = new String[]{"Все", "Круш", "Сферы", "Талис", "Зелья", "Разн"};
   private static final AutoBuyItemCategory[] CATS_HW = new AutoBuyItemCategory[]{null, AutoBuyItemCategory.HOLYWORLD};
   private static final String[] CAT_LABELS_HW = new String[]{"Все", "HW"};
   private String lastServerMode = "";
   private static final float CELL_BASE = 38.0F;
   private static final float CELL_GAP_BASE = 5.0F;
   private static final float RULE_H_BASE = 42.0F;
   private static final float RULE_GAP_BASE = 5.0F;
   private static final float ICON_BOX_BASE = 30.0F;
   private static final float R_BASE = 7.0F;
   private float s = 1.0F;
   private float viewX;
   private float viewY;
   private float viewW;
   private float viewH;
   private float scrollCat;
   private float targetScrollCat;
   private float scrollRules;
   private float targetScrollRules;
   private float scrollParse;
   private float targetScrollParse;
   private float catX;
   private float catY;
   private float catW;
   private float catH;
   private float rulesX;
   private float rulesY;
   private float rulesW;
   private float rulesH;
   private float parseX;
   private float parseY;
   private float parseW;
   private float parseH;
   private final List<float[]> serverHits = new ArrayList<>();
   private final List<float[]> catTabHits = new ArrayList<>();
   private final List<float[]> pageHits = new ArrayList<>();
   private final List<AutoBuyItem> catalogItems = new ArrayList<>();
   private final List<float[]> catalogBoxes = new ArrayList<>();
   private final List<AutoBuyItem> ruleItems = new ArrayList<>();
   private final List<float[]> ruleBoxes = new ArrayList<>();
   private float[] parseNowHit;
   private float[] autoParseHit;
   private float[] clearHit;
   private final List<float[]> settingHits = new ArrayList<>();
   private int selectedCatIndex = 0;
   private int selectedPage;
   private AutoBuyItem editingPriceItem;
   private String priceDraft = "";
   private float lastMx;
   private float lastMy;
   private float parseContentH = 320.0F;
   private float[] catBar;
   private float[] rulesBar;
   private float[] parseBar;
   private static final int BAR_NONE = 0;
   private static final int BAR_CAT = 1;
   private static final int BAR_RULES = 2;
   private static final int BAR_PARSE = 3;
   private int draggingBar = 0;
   private float barGrabOffset;
   private final List<float[]> stepperHits = new ArrayList<>();
   private static final int PRICE_MAX_DIGITS = 10;
   private List<AutoBuyItem> cachedCatalog;
   private String cachedCatalogKey = "";
   private long lastAnimationFrameNanos;
   private float animationDelta = 0.016666668F;
   private float panelAnimation;
   private float contentTransition;
   private float serverIndicatorX = Float.NaN;
   private float serverIndicatorW;
   private float pageIndicatorX = Float.NaN;
   private float pageIndicatorW;
   private final float[] categoryAnimations = new float[CATS_FT.length];
   private final IdentityHashMap<AutoBuyItem, Float> catalogHoverAnimations = new IdentityHashMap<>();
   private final IdentityHashMap<AutoBuyItem, Float> catalogEnabledAnimations = new IdentityHashMap<>();
   private final IdentityHashMap<AutoBuyItem, Float> ruleHoverAnimations = new IdentityHashMap<>();
   private float parseButtonAnimation;
   private float autoParseButtonAnimation;
   private float clearButtonAnimation;

   public CsAutoBuyPanel() {
   }

   private float u(float v) {
      return v * this.s;
   }

   private float cell() {
      return this.u(38.0F);
   }

   private float cellGap() {
      return this.u(5.0F);
   }

   private float ruleH() {
      return this.u(42.0F);
   }

   private float ruleGap() {
      return this.u(5.0F);
   }

   private float iconBox() {
      return this.u(30.0F);
   }

   private float r() {
      return this.u(7.0F);
   }

   public void reset() {
      this.scrollCat = this.targetScrollCat = 0.0F;
      this.scrollRules = this.targetScrollRules = 0.0F;
      this.scrollParse = this.targetScrollParse = 0.0F;
      this.editingPriceItem = null;
      this.priceDraft = "";
      this.draggingBar = 0;
      this.selectedCatIndex = 0;
      this.selectedPage = 0;
      this.resetAnimations();
   }

   public void onGuiClosed() {
      this.commitPriceEdit();
      this.draggingBar = 0;
   }

   public float getScroll() {
      return this.targetScrollCat;
   }

   public void setScroll(float value) {
      this.targetScrollCat = Math.max(0.0F, value);
      this.scrollCat = this.targetScrollCat;
   }

   public float getRulesScroll() {
      return this.targetScrollRules;
   }

   public void setRulesScroll(float value) {
      this.targetScrollRules = Math.max(0.0F, value);
      this.scrollRules = this.targetScrollRules;
   }

   public boolean mouseScrolled(double mx, double my, double amount) {
      float step = (float)amount * this.u(30.0F);
      if (hit(mx, my, this.catX, this.catY, this.catW, this.catH)) {
         this.targetScrollCat = Math.max(0.0F, this.targetScrollCat - step);
         return true;
      } else if (hit(mx, my, this.rulesX, this.rulesY, this.rulesW, this.rulesH)) {
         this.targetScrollRules = Math.max(0.0F, this.targetScrollRules - step);
         return true;
      } else if (hit(mx, my, this.parseX, this.parseY, this.parseW, this.parseH)) {
         this.targetScrollParse = Math.max(0.0F, this.targetScrollParse - step);
         return true;
      } else {
         return true;
      }
   }

   public boolean isSearchFocused() {
      return this.editingPriceItem != null;
   }

   public void render(DrawContext g, float x, float y, float w, float h, float alpha, float scale, int accent, int mx, int my) {
      this.s = Math.max(0.5F, Math.min(2.0F, scale));
      this.beginAnimationFrame();
      this.panelAnimation = this.animate(this.panelAnimation, 1.0F, 9.0F);
      this.contentTransition = this.animate(this.contentTransition, 1.0F, 13.0F);
      float entrance = easeOutCubic(this.panelAnimation);
      y += this.u(9.0F) * (1.0F - entrance);
      alpha *= entrance;
      this.viewX = x;
      this.viewY = y;
      this.viewW = w;
      this.viewH = h;
      this.lastMx = (float)mx;
      this.lastMy = (float)my;
      AutoBuyManager.get().ensureLoaded();
      int text = CsStyle.text(alpha);
      int dim = CsStyle.faint(alpha);
      int dim2 = CsStyle.muted(alpha);
      int accentMid = CsStyle.accentAlpha(accent, 120, alpha);
      int accentHard = CsStyle.accentAlpha(accent, 200, alpha);
      AutoBuy mod = module();
      String currentServer = mod != null ? mod.getServerMode().getValue() : "FunTime";
      boolean parseOn = mod != null && mod.isAutoParseEnabled();
      boolean parseRun = mod != null && mod.isParseRunning();
      if (!currentServer.equalsIgnoreCase(this.lastServerMode)) {
         this.contentTransition = 0.0F;
         boolean wasHw = this.lastServerMode.equalsIgnoreCase("HolyWorld");
         boolean isHw = currentServer.equalsIgnoreCase("HolyWorld");
         if (!this.lastServerMode.isEmpty() && wasHw != isHw) {
            this.selectedCatIndex = 0;
            this.scrollCat = this.targetScrollCat = 0.0F;
            this.scrollRules = this.targetScrollRules = 0.0F;
         }

         this.editingPriceItem = null;
         this.priceDraft = "";
         this.lastServerMode = currentServer;
      }

      AutoBuyItemCategory[] cats = catsFor(currentServer);
      String[] catLabels = catLabelsFor(currentServer);
      if (this.selectedCatIndex >= cats.length) {
         this.selectedCatIndex = 0;
      }

      float pad = this.u(8.0F);
      CsStyle.panel(x, y, w, h, 7.0F, alpha);
      float topH = this.u(38.0F);
      float topY = y + pad;
      float topX = x + pad;
      float topW = w - pad * 2.0F;
      float ambientPulse = 0.5F + 0.5F * (float)Math.sin((double)System.nanoTime() / 7.0E8);
      Render2D.blur(topX, topY, topW * 0.46F, topH, 8.0F, 14.0F, 1.0F, CsStyle.accentAlpha(accent, Math.round(24.0F + ambientPulse * 16.0F), alpha));
      CsStyle.card(topX, topY, topW, topH, 8.0F, alpha, true);
      float markSize = this.u(23.0F);
      float markX = topX + this.u(9.0F);
      float markY = topY + (topH - markSize) * 0.5F;
      CsStyle.glow(markX, markY, markSize, markSize, this.u(6.0F), accent, 1.15F, alpha);
      Render2D.rect(
         markX,
         markY,
         markSize,
         markSize,
         this.u(6.0F),
         CsStyle.accentAlpha(accent, 225, alpha),
         CsStyle.accentAlpha(accent, 190, alpha),
         CsStyle.accentAlpha(accent, 105, alpha),
         CsStyle.accentAlpha(accent, 135, alpha)
      );
      Render2D.outline(markX, markY, markSize, markSize, this.u(6.0F), 0.8F, CsStyle.accentAlpha(accent, 245, alpha));
      float markTextSize = this.u(10.5F);
      float markTextW = Render2D.textWidth(FONT_BOLD, "A", markTextSize);
      Render2D.text(
         FONT_BOLD, "A", markX + (markSize - markTextW) * 0.5F, this.centeredTextY(markY, markSize, markTextSize), markTextSize, CsStyle.white(alpha)
      );
      float titleX = markX + markSize + this.u(8.0F);
      float titleSize = this.u(10.5F);
      Render2D.text(FONT_BOLD, "AutoBuy", titleX, topY + this.u(7.0F), titleSize, text);
      Render2D.text(FONT, "AUCTION ASSISTANT", titleX, topY + this.u(21.0F), this.u(6.4F), dim);
      this.serverHits.clear();
      float pillX = topX + this.u(154.0F);
      float pillH = this.u(19.0F);
      float pillY = topY + (topH - pillH) * 0.5F;
      float[] serverWidths = new float[SERVERS.length];
      float serverTrackW = 0.0F;

      for (int i = 0; i < SERVERS.length; i++) {
         serverWidths[i] = Render2D.textWidth(FONT_BOLD, SERVER_SHORT[i], this.u(9.5F)) + this.u(17.0F);
         serverTrackW += serverWidths[i];
      }

      Render2D.rect(
         pillX,
         pillY,
         serverTrackW,
         pillH,
         pillH * 0.5F,
         CsStyle.surfaceDeep(168, alpha),
         CsStyle.surfaceDark(148, alpha),
         CsStyle.controlBottom(160, alpha),
         CsStyle.surfaceDeep(176, alpha)
      );
      Render2D.outline(pillX, pillY, serverTrackW, pillH, pillH * 0.5F, 0.65F, CsStyle.outline(70, alpha));
      float targetIndicatorX = pillX;
      float targetIndicatorW = serverWidths[0];

      for (int i = 0; i < SERVERS.length; i++) {
         if (SERVERS[i].equalsIgnoreCase(currentServer)) {
            targetIndicatorW = serverWidths[i];
            break;
         }

         targetIndicatorX += serverWidths[i];
      }

      if (Float.isNaN(this.serverIndicatorX)) {
         this.serverIndicatorX = targetIndicatorX;
         this.serverIndicatorW = targetIndicatorW;
      }

      this.serverIndicatorX = this.animate(this.serverIndicatorX, targetIndicatorX, 15.0F);
      this.serverIndicatorW = this.animate(this.serverIndicatorW, targetIndicatorW, 15.0F);
      CsStyle.glow(this.serverIndicatorX, pillY, this.serverIndicatorW, pillH, pillH * 0.5F, accent, 0.8F, alpha);
      Render2D.rect(
         this.serverIndicatorX,
         pillY,
         this.serverIndicatorW,
         pillH,
         pillH * 0.5F,
         CsStyle.accentAlpha(accent, 170, alpha),
         CsStyle.accentAlpha(accent, 145, alpha),
         CsStyle.accentAlpha(accent, 82, alpha),
         CsStyle.accentAlpha(accent, 105, alpha)
      );
      Render2D.outline(this.serverIndicatorX, pillY, this.serverIndicatorW, pillH, pillH * 0.5F, 0.75F, CsStyle.accentAlpha(accent, 205, alpha));
      float segmentX = pillX;

      for (int i = 0; i < SERVERS.length; i++) {
         float pw = serverWidths[i];
         boolean sel = SERVERS[i].equalsIgnoreCase(currentServer);
         boolean hov = hit((double)mx, (double)my, segmentX, pillY, pw, pillH);
         if (hov && !sel) {
            Render2D.rect(segmentX + this.u(1.0F), pillY + this.u(1.0F), pw - this.u(2.0F), pillH - this.u(2.0F), pillH * 0.45F, CsStyle.controlTop(92, alpha));
         }

         int pcol = sel ? CsStyle.white(alpha) : (hov ? text : dim2);
         float tw = Render2D.textWidth(FONT_BOLD, SERVER_SHORT[i], this.u(9.5F));
         Render2D.text(FONT_BOLD, SERVER_SHORT[i], segmentX + (pw - tw) * 0.5F, this.centeredTextY(pillY, pillH, this.u(9.5F)), this.u(9.5F), pcol);
         this.serverHits.add(new float[]{segmentX, pillY, pw, pillH, (float)i});
         segmentX += pw;
      }

      String status = parseRun ? "Парсинг…" : (parseOn ? "AutoParse ON" : "Готов");
      int stCol = parseRun ? CsStyle.warning(alpha) : (parseOn ? CsStyle.accentAlpha(accent, 245, alpha) : dim2);
      float stW = Render2D.textWidth(FONT, status, this.u(9.0F)) + this.u(26.0F);
      float stX = topX + topW - stW - this.u(9.0F);
      this.pageHits.clear();
      float pageX = segmentX + this.u(11.0F);
      float pageW = Math.max(this.u(126.0F), stX - pageX - this.u(8.0F));
      float pageH = this.u(19.0F);
      float pageY = pillY;
      float pageSegmentW = pageW / (float)PAGE_LABELS.length;
      Render2D.rect(
         pageX,
         pillY,
         pageW,
         pageH,
         pageH * 0.5F,
         CsStyle.surfaceDeep(158, alpha),
         CsStyle.surfaceDark(142, alpha),
         CsStyle.controlBottom(154, alpha),
         CsStyle.surfaceDeep(168, alpha)
      );
      Render2D.outline(pageX, pillY, pageW, pageH, pageH * 0.5F, 0.65F, CsStyle.outline(62, alpha));
      float targetPageX = pageX + (float)this.selectedPage * pageSegmentW;
      if (Float.isNaN(this.pageIndicatorX)) {
         this.pageIndicatorX = targetPageX;
         this.pageIndicatorW = pageSegmentW;
      }

      this.pageIndicatorX = this.animate(this.pageIndicatorX, targetPageX, 16.0F);
      this.pageIndicatorW = this.animate(this.pageIndicatorW, pageSegmentW, 16.0F);
      Render2D.rect(
         this.pageIndicatorX,
         pillY,
         this.pageIndicatorW,
         pageH,
         pageH * 0.5F,
         CsStyle.controlTop(190, alpha),
         CsStyle.accentAlpha(accent, 72, alpha),
         CsStyle.accentAlpha(accent, 46, alpha),
         CsStyle.controlBottom(185, alpha)
      );
      Render2D.outline(this.pageIndicatorX, pillY, this.pageIndicatorW, pageH, pageH * 0.5F, 0.7F, CsStyle.accentAlpha(accent, 135, alpha));

      for (int i = 0; i < PAGE_LABELS.length; i++) {
         float px = pageX + (float)i * pageSegmentW;
         boolean hovered = hit((double)mx, (double)my, px, pageY, pageSegmentW, pageH);
         float fontSize = this.u(6.8F);
         float labelW = Render2D.textWidth(FONT_BOLD, PAGE_LABELS[i], fontSize);
         int labelColor = i == this.selectedPage ? text : (hovered ? dim2 : dim);
         Render2D.text(FONT_BOLD, PAGE_LABELS[i], px + (pageSegmentW - labelW) * 0.5F, this.centeredTextY(pageY, pageH, fontSize), fontSize, labelColor);
         this.pageHits.add(new float[]{px, pageY, pageSegmentW, pageH, (float)i});
      }

      CsStyle.statusPill(stX, pillY, stW, pillH, stCol, alpha);
      float statusDotRadius = this.u(2.25F) * (1.0F + (parseRun ? ambientPulse * 0.22F : 0.0F));
      CsStyle.statusDot(stX + this.u(10.0F), pillY + pillH * 0.5F, statusDotRadius, stCol, alpha);
      Render2D.text(FONT, status, stX + this.u(17.0F), this.centeredTextY(pillY, pillH, this.u(9.0F)), this.u(9.0F), stCol);
      float bodyY = topY + topH + this.u(7.0F);
      float bodyH = h - (bodyY - y) - pad;
      float bodyX = x + pad;
      float bodyW = w - pad * 2.0F;
      boolean showCatalog = this.selectedPage == 0;
      boolean showRules = this.selectedPage == 1;
      boolean showSettings = this.selectedPage == 2;
      CsStyle.sectionCard(bodyX, bodyY, bodyW, bodyH, this.r(), accent, alpha);
      float hdrH = this.u(22.0F);
      int enCount = AutoBuyManager.get().enabledCount(currentServer);
      if (showCatalog) {
         this.drawColHeader("Каталог предметов", serverShort(currentServer), bodyX, bodyY, bodyW, hdrH, accent, alpha);
      } else if (showRules) {
         this.drawColHeader("Мои покупки", enCount + " выбрано", bodyX, bodyY, bodyW, hdrH, accent, alpha);
      } else {
         this.drawColHeader("Управление AutoBuy", currentServer, bodyX, bodyY, bodyW, hdrH, accent, alpha);
      }

      float tabY = bodyY + hdrH + this.u(2.0F);
      float tabH = this.u(16.0F);
      this.catTabHits.clear();
      float tabStartX = bodyX + this.u(8.0F);
      float tabRight = bodyX + bodyW - this.u(8.0F);
      float tabX = tabStartX;
      int tabRow = 0;
      if (showCatalog) {
         for (int i = 0; i < catLabels.length; i++) {
            float tw = Render2D.textWidth(FONT, catLabels[i], this.u(8.5F)) + this.u(12.0F);
            if (tabX > tabStartX && tabX + tw > tabRight) {
               tabRow++;
               tabX = tabStartX;
            }

            float currentTabY = tabY + (float)tabRow * (tabH + this.u(2.0F));
            boolean sel = this.selectedCatIndex == i;
            boolean hov = hit((double)mx, (double)my, tabX, currentTabY, tw, tabH);
            this.categoryAnimations[i] = this.animate(this.categoryAnimations[i], sel ? 1.0F : 0.0F, 18.0F);
            float tabAnimation = this.categoryAnimations[i];
            int idleFill = hov ? CsStyle.controlTop(112, alpha) : CsStyle.controlBottom(34, alpha);
            Render2D.rect(
               tabX,
               currentTabY,
               tw,
               tabH,
               this.u(4.0F),
               tabAnimation > 0.01F ? CsStyle.accentAlpha(accent, Math.round(38.0F + tabAnimation * 78.0F), alpha) : idleFill
            );
            if (tabAnimation > 0.01F) {
               Render2D.outline(tabX, currentTabY, tw, tabH, this.u(4.0F), 1.0F, CsStyle.accentAlpha(accent, Math.round(75.0F + tabAnimation * 95.0F), alpha));
               float underlineW = Math.max(this.u(5.0F), (tw - this.u(8.0F)) * tabAnimation);
               Render2D.rect(tabX + (tw - underlineW) * 0.5F, currentTabY + tabH - this.u(1.4F), underlineW, this.u(1.2F), this.u(0.6F), accentMid);
            } else if (hov) {
               Render2D.outline(tabX, currentTabY, tw, tabH, this.u(4.0F), 0.7F, CsStyle.outline(78, alpha));
            }

            Render2D.text(FONT, catLabels[i], tabX + this.u(6.0F), this.centeredTextY(currentTabY, tabH, this.u(8.5F)), this.u(8.5F), sel ? text : dim);
            this.catTabHits.add(new float[]{tabX, currentTabY, tw, tabH, (float)i});
            tabX += tw + this.u(4.0F);
         }
      }

      this.catX = bodyX;
      this.catY = tabY + (float)(tabRow + 1) * tabH + (float)tabRow * this.u(2.0F) + this.u(4.0F);
      this.catW = showCatalog ? bodyW : 0.0F;
      this.catH = bodyY + bodyH - this.catY - this.u(4.0F);
      this.rulesX = bodyX;
      this.rulesY = bodyY + hdrH + this.u(4.0F);
      this.rulesW = showRules ? bodyW : 0.0F;
      this.rulesH = bodyY + bodyH - this.rulesY - this.u(4.0F);
      this.parseX = bodyX;
      this.parseY = bodyY + hdrH + this.u(4.0F);
      this.parseW = showSettings ? bodyW : 0.0F;
      this.parseH = bodyY + bodyH - this.parseY - this.u(4.0F);
      this.catalogItems.clear();
      this.catalogBoxes.clear();
      if (showCatalog) {
         List<AutoBuyItem> catalog = this.filteredCatalog(currentServer);
         float barReserve = this.u(9.0F);
         float gridAvail = this.catW - this.u(8.0F) * 2.0F - barReserve;
         int cols = Math.max(1, (int)((gridAvail + this.cellGap()) / (this.cell() + this.cellGap())));
         float cellW = Math.max(this.u(16.0F), (gridAvail - this.cellGap() * (float)(cols - 1)) / (float)cols);
         int rows = (catalog.size() + cols - 1) / Math.max(1, cols);
         float contentCat = rows <= 0 ? 0.0F : (float)rows * cellW + (float)(rows - 1) * this.cellGap() + this.u(4.0F);
         float maxCat = Math.max(0.0F, contentCat - this.catH);
         this.targetScrollCat = Math.max(0.0F, Math.min(this.targetScrollCat, maxCat));
         this.scrollCat = this.scrollCat + (this.targetScrollCat - this.scrollCat) * 0.28F;
         Render2D.pushScissor(g, this.catX + 1.0F, this.catY, this.catW - 2.0F, this.catH);
         float cellStartX = this.catX + this.u(8.0F);
         float cellStartY = this.catY + this.u(2.0F) - this.scrollCat;
         float iconSize = Math.max(8.0F, cellW * 0.55F);
         AutoBuyItem hoveredCatalogItem = null;

         for (int i = 0; i < catalog.size(); i++) {
            AutoBuyItem item = catalog.get(i);
            int col = i % cols;
            int row = i / cols;
            float cx = cellStartX + (float)col * (cellW + this.cellGap());
            float cy = cellStartY + (float)row * (cellW + this.cellGap());
            if (!(cy + cellW < this.catY - 2.0F) && !(cy > this.catY + this.catH + 2.0F)) {
               boolean en = item.isEnabled();
               boolean hov = hit((double)mx, (double)my, cx, cy, cellW, cellW);
               if (hov) {
                  hoveredCatalogItem = item;
               }

               float hoverAnimation = this.animateItem(this.catalogHoverAnimations, item, hov, 20.0F);
               float enabledAnimation = this.animateItem(this.catalogEnabledAnimations, item, en, 16.0F);
               float revealAnimation = this.staggeredReveal(i, 0.018F);
               float grow = this.u(2.2F) * hoverAnimation;
               float drawX = cx - grow * 0.5F;
               float drawY = cy - grow * 0.65F + this.u(4.0F) * (1.0F - revealAnimation);
               float drawSize = cellW + grow;
               float cellAlpha = alpha * revealAnimation;
               int neutralTop = CsStyle.controlTop(Math.round(70.0F + hoverAnimation * 56.0F), cellAlpha);
               int neutralBottom = CsStyle.controlBottom(Math.round(82.0F + hoverAnimation * 60.0F), cellAlpha);
               Render2D.rect(drawX, drawY, drawSize, drawSize, 6.0F, neutralTop, neutralTop, neutralBottom, neutralBottom);
               if (enabledAnimation > 0.01F) {
                  CsStyle.glow(drawX, drawY, drawSize, drawSize, 6.0F, accent, 0.7F + hoverAnimation * 0.65F, cellAlpha * enabledAnimation);
                  int selectedTop = Math.round(58.0F + enabledAnimation * 35.0F + hoverAnimation * 24.0F);
                  int selectedBottom = Math.round(28.0F + enabledAnimation * 20.0F + hoverAnimation * 12.0F);
                  Render2D.rect(
                     drawX,
                     drawY,
                     drawSize,
                     drawSize,
                     6.0F,
                     CsStyle.accentAlpha(accent, selectedTop, cellAlpha * enabledAnimation),
                     CsStyle.accentAlpha(accent, selectedTop, cellAlpha * enabledAnimation),
                     CsStyle.accentAlpha(accent, selectedBottom, cellAlpha * enabledAnimation),
                     CsStyle.accentAlpha(accent, selectedBottom, cellAlpha * enabledAnimation)
                  );
                  Render2D.outline(
                     drawX,
                     drawY,
                     drawSize,
                     drawSize,
                     6.0F,
                     1.1F,
                     CsStyle.accentAlpha(accent, Math.round(95.0F + enabledAnimation * 110.0F), cellAlpha * enabledAnimation)
                  );
               } else if (hoverAnimation > 0.01F) {
                  Render2D.outline(drawX, drawY, drawSize, drawSize, 6.0F, 1.0F, CsStyle.outline(Math.round(62.0F + hoverAnimation * 62.0F), cellAlpha));
               }

               CsStyle.topHighlight(drawX, drawY, drawSize, 6.0F, Math.round((22.0F + enabledAnimation * 18.0F + hoverAnimation * 12.0F) * cellAlpha));
               float animatedIconSize = iconSize * (1.0F + hoverAnimation * 0.08F);
               this.drawIcon(g, item, drawX + (drawSize - animatedIconSize) * 0.5F, drawY + (drawSize - animatedIconSize) * 0.5F, animatedIconSize, cellAlpha);
               if (enabledAnimation > 0.05F) {
                  float tick = this.u(7.5F);
                  float tickX = drawX + drawSize - tick - this.u(1.5F);
                  float tickY = drawY + this.u(1.5F);
                  Render2D.rect(tickX, tickY, tick, tick, tick * 0.5F, CsStyle.accentAlpha(accent, 220, cellAlpha * enabledAnimation));
                  CsMenuAssets.iconCentered("✓", tickX + tick * 0.5F, tickY + tick * 0.5F, tick * 0.73F, CsStyle.white(cellAlpha * enabledAnimation));
               }

               if (cy >= this.catY - 1.0F && cy + cellW <= this.catY + this.catH + 1.0F) {
                  this.catalogItems.add(item);
                  this.catalogBoxes.add(new float[]{cx, cy, cellW, cellW});
               }
            }
         }

         Render2D.popScissor(g);
         if (hoveredCatalogItem != null) {
            this.drawCatalogTooltip(hoveredCatalogItem, (float)mx, (float)my, this.catX, this.catY, this.catW, this.catH, accent, alpha);
         }

         this.catBar = CsStyle.scrollbar(this.catX, this.catY, this.catW, this.catH, contentCat, this.scrollCat, accent, alpha);
         if (catalog.isEmpty()) {
            CsStyle.emptyState(this.catX, this.catY, this.catW, this.catH, "Пусто", "Смени вкладку категории", alpha);
         }
      } else {
         this.catBar = null;
      }

      this.ruleItems.clear();
      this.ruleBoxes.clear();
      List<AutoBuyItem> enabled = AutoBuyManager.get().enabledItems(currentServer);
      if (showRules) {
         int ruleColumns = 2;
         int ruleRows = (enabled.size() + ruleColumns - 1) / ruleColumns;
         float contentRules = enabled.isEmpty() ? 0.0F : (float)ruleRows * this.ruleH() + (float)Math.max(0, ruleRows - 1) * this.ruleGap() + this.u(4.0F);
         float maxRules = Math.max(0.0F, contentRules - this.rulesH);
         this.targetScrollRules = Math.max(0.0F, Math.min(this.targetScrollRules, maxRules));
         this.scrollRules = this.scrollRules + (this.targetScrollRules - this.scrollRules) * 0.28F;
         Render2D.pushScissor(g, this.rulesX + 1.0F, this.rulesY, this.rulesW - 2.0F, this.rulesH);
         float rulePadding = this.u(8.0F);
         float ruleColumnGap = this.u(7.0F);
         float ruleScrollReserve = this.u(9.0F);
         float ruleCardW = (this.rulesW - rulePadding * 2.0F - ruleColumnGap - ruleScrollReserve) * 0.5F;
         float ruleStartY = this.rulesY + this.u(2.0F) - this.scrollRules;

         for (int ruleIndex = 0; ruleIndex < enabled.size(); ruleIndex++) {
            AutoBuyItem item = enabled.get(ruleIndex);
            int ruleColumn = ruleIndex % ruleColumns;
            int ruleRow = ruleIndex / ruleColumns;
            float ry = ruleStartY + (float)ruleRow * (this.ruleH() + this.ruleGap());
            if (ry + this.ruleH() >= this.rulesY - 2.0F && ry <= this.rulesY + this.rulesH + 2.0F) {
               float rx = this.rulesX + rulePadding + (float)ruleColumn * (ruleCardW + ruleColumnGap);
               boolean hovx = hit((double)mx, (double)my, rx, ry, ruleCardW, this.ruleH());
               boolean editing = this.editingPriceItem == item;
               float hoverAnimationx = this.animateItem(this.ruleHoverAnimations, item, hovx || editing, 18.0F);
               float revealAnimationx = this.staggeredReveal(ruleIndex, 0.035F);
               float cardAlpha = alpha * revealAnimationx;
               float animatedRx = rx + this.u(7.0F) * (1.0F - revealAnimationx) - this.u(0.7F) * hoverAnimationx;
               float animatedRy = ry - this.u(0.8F) * hoverAnimationx;
               float animatedRw = ruleCardW + this.u(1.4F) * hoverAnimationx;
               if (editing) {
                  CsStyle.glow(animatedRx, animatedRy, animatedRw, this.ruleH(), 7.0F, accent, 0.65F + hoverAnimationx * 0.35F, cardAlpha);
               }

               CsStyle.card(animatedRx, animatedRy, animatedRw, this.ruleH(), 7.0F, cardAlpha, hoverAnimationx > 0.35F);
               Render2D.rect(
                  animatedRx + this.u(1.5F),
                  animatedRy + this.u(6.0F),
                  this.u(2.0F),
                  this.ruleH() - this.u(12.0F),
                  this.u(1.0F),
                  CsStyle.accentAlpha(accent, Math.round((editing ? 220.0F : 95.0F) + hoverAnimationx * 35.0F), cardAlpha)
               );
               float ibx = animatedRx + this.u(8.0F);
               float iby = animatedRy + (this.ruleH() - this.iconBox()) * 0.5F;
               CsStyle.inset(ibx, iby, this.iconBox(), this.iconBox(), 5.0F, cardAlpha);
               float ruleIcon = Math.max(8.0F, this.iconBox() * (0.55F + hoverAnimationx * 0.035F));
               this.drawIcon(g, item, ibx + (this.iconBox() - ruleIcon) * 0.5F, iby + (this.iconBox() - ruleIcon) * 0.5F, ruleIcon, cardAlpha);
               float tx = ibx + this.iconBox() + this.u(7.0F);
               Render2D.text(
                  FONT_BOLD,
                  clip(item.getName(), animatedRx + animatedRw - tx - this.u(8.0F), this.u(10.0F)),
                  tx,
                  animatedRy + this.u(7.0F),
                  this.u(10.0F),
                  ColorUtil.withAlpha(text, Math.round(255.0F * cardAlpha))
               );
               String label = "Цена";
               float fieldH = this.u(15.0F);
               float fieldY = animatedRy + this.u(21.0F);
               Render2D.text(FONT, label, tx, fieldY + (fieldH - this.u(8.5F)) * 0.5F, this.u(8.5F), ColorUtil.withAlpha(dim, Math.round(255.0F * cardAlpha)));
               float labelW = Render2D.textWidth(FONT, label, this.u(8.5F)) + this.u(6.0F);
               float fieldX = tx + labelW;
               float fieldW = Math.min(this.u(76.0F), animatedRx + animatedRw - fieldX - this.u(8.0F));
               CsStyle.inset(fieldX, fieldY, fieldW, fieldH, 4.0F, cardAlpha);
               if (editing) {
                  Render2D.outline(fieldX, fieldY, fieldW, fieldH, 4.0F, 1.1F, CsStyle.accentAlpha(accent, 215, cardAlpha));
                  float caretX = fieldX + this.u(5.0F) + Render2D.textWidth(FONT_BOLD, this.priceDraft, this.u(9.5F)) + this.u(1.0F);
                  if (System.currentTimeMillis() % 1000L > 500L && caretX < fieldX + fieldW - this.u(3.0F)) {
                     Render2D.rect(caretX, fieldY + this.u(3.0F), this.u(1.0F), fieldH - this.u(6.0F), 0.5F, CsStyle.accentAlpha(accent, 230, cardAlpha));
                  }
               }

               String shown = editing ? this.priceDraft : formatPrice(item.getBuyPrice());
               Render2D.text(
                  FONT_BOLD,
                  shown,
                  fieldX + this.u(5.0F),
                  fieldY + (fieldH - this.u(9.5F)) * 0.5F,
                  this.u(9.5F),
                  editing ? ColorUtil.withAlpha(text, Math.round(255.0F * cardAlpha)) : CsStyle.accentAlpha(accent, 235, cardAlpha)
               );
               if (ry >= this.rulesY - 1.0F && ry + this.ruleH() <= this.rulesY + this.rulesH + 1.0F) {
                  this.ruleItems.add(item);
                  this.ruleBoxes.add(new float[]{animatedRx, animatedRy, animatedRw, this.ruleH(), fieldX, fieldY, fieldW, fieldH});
               }
            }
         }

         Render2D.popScissor(g);
         this.rulesBar = CsStyle.scrollbar(this.rulesX, this.rulesY, this.rulesW, this.rulesH, contentRules, this.scrollRules, accent, alpha);
         if (enabled.isEmpty()) {
            CsStyle.emptyState(this.rulesX, this.rulesY, this.rulesW, this.rulesH, "Список пуст", "ЛКМ по иконке слева — добавить · ПКМ — убрать", alpha);
         }
      } else {
         this.rulesBar = null;
      }

      this.settingHits.clear();
      this.stepperHits.clear();
      this.parseNowHit = null;
      this.autoParseHit = null;
      this.clearHit = null;
      if (showSettings) {
         float contentParseH = this.parseContentH;
         float maxParse = Math.max(0.0F, contentParseH - this.parseH);
         this.targetScrollParse = Math.max(0.0F, Math.min(this.targetScrollParse, maxParse));
         this.scrollParse = this.scrollParse + (this.targetScrollParse - this.scrollParse) * 0.28F;
         Render2D.pushScissor(g, this.parseX + 1.0F, this.parseY, this.parseW - 2.0F, this.parseH);
         float btnH = this.u(18.0F);
         float btnGap = this.u(6.0F);
         float btnX = this.parseX + this.u(8.0F);
         float actionAreaW = this.parseW - this.u(8.0F) * 2.0F - this.u(9.0F);
         float btnW = (actionAreaW - btnGap * 2.0F) / 3.0F;
         float by = this.parseY + this.u(2.0F) - this.scrollParse;
         boolean hovP = hit((double)mx, (double)my, btnX, by, btnW, btnH);
         this.parseButtonAnimation = this.animate(this.parseButtonAnimation, hovP ? 1.0F : 0.0F, 18.0F);
         String pLabel = parseRun ? "Парсинг…" : "Парс сейчас";
         if (parseRun) {
            int warm = CsStyle.warning(alpha);
            drawGradientButton(btnX, by, btnW, btnH, warm, CsStyle.controlBottom(210, alpha), alpha);
            Render2D.outline(btnX, by, btnW, btnH, 6.0F, 0.9F, ColorUtil.withAlpha(warm, Math.round(210.0F * alpha)));
         } else {
            if (this.parseButtonAnimation > 0.01F) {
               CsStyle.glow(btnX, by, btnW, btnH, 6.0F, accent, 0.55F + this.parseButtonAnimation * 0.65F, alpha * this.parseButtonAnimation);
            }

            drawGradientButton(
               btnX,
               by,
               btnW,
               btnH,
               CsStyle.accentAlpha(accent, Math.round(125.0F + this.parseButtonAnimation * 48.0F), alpha),
               CsStyle.accentAlpha(accent, Math.round(62.0F + this.parseButtonAnimation * 38.0F), alpha),
               alpha
            );
            Render2D.outline(btnX, by, btnW, btnH, 6.0F, 0.9F, accentHard);
         }

         float plw = Render2D.textWidth(FONT_BOLD, pLabel, this.u(9.0F));
         Render2D.text(FONT_BOLD, pLabel, btnX + (btnW - plw) * 0.5F, this.btnTextY(by, btnH), this.u(9.0F), CsStyle.white(alpha));
         this.parseNowHit = this.clipToParse(new float[]{btnX, by, btnW, btnH});
         btnX += btnW + btnGap;
         boolean hovA = hit((double)mx, (double)my, btnX, by, btnW, btnH);
         this.autoParseButtonAnimation = this.animate(this.autoParseButtonAnimation, hovA ? 1.0F : 0.0F, 18.0F);
         String aLabel = "AutoParse";
         int okTop = CsStyle.accentAlpha(accent, Math.round(155.0F + this.autoParseButtonAnimation * 42.0F), alpha);
         int okBottom = CsStyle.accentAlpha(accent, Math.round(92.0F + this.autoParseButtonAnimation * 34.0F), alpha);
         int offTop = CsStyle.controlTop(Math.round(82.0F + this.autoParseButtonAnimation * 50.0F), alpha);
         int offBottom = CsStyle.controlBottom(Math.round(94.0F + this.autoParseButtonAnimation * 52.0F), alpha);
         drawGradientButton(btnX, by, btnW, btnH, parseOn ? okTop : offTop, parseOn ? okBottom : offBottom, alpha);
         Render2D.text(FONT_BOLD, aLabel, btnX + this.u(9.0F), this.btnTextY(by, btnH), this.u(9.0F), parseOn ? CsStyle.white(alpha) : dim2);
         CsStyle.switchTrack(btnX + btnW - this.u(29.0F), by + (btnH - this.u(11.0F)) * 0.5F, this.u(20.0F), this.u(11.0F), parseOn, hovA, accent, alpha);
         this.autoParseHit = this.clipToParse(new float[]{btnX, by, btnW, btnH});
         btnX += btnW + btnGap;
         boolean hovC = hit((double)mx, (double)my, btnX, by, btnW, btnH);
         this.clearButtonAnimation = this.animate(this.clearButtonAnimation, hovC ? 1.0F : 0.0F, 18.0F);
         drawGradientButton(
            btnX,
            by,
            btnW,
            btnH,
            CsStyle.controlTop(Math.round(76.0F + this.clearButtonAnimation * 56.0F), alpha),
            CsStyle.controlBottom(Math.round(88.0F + this.clearButtonAnimation * 58.0F), alpha),
            alpha
         );
         String cLabel = "Очистить список";
         float clw = Render2D.textWidth(FONT, cLabel, this.u(9.0F));
         float cIconX = btnX + (btnW - clw - this.u(12.0F)) * 0.5F;
         CsMenuAssets.icon(
            "×",
            cIconX,
            by + (btnH - this.u(9.0F)) * 0.5F,
            this.u(9.0F),
            this.clearButtonAnimation > 0.01F ? ColorUtil.withAlpha(CsStyle.danger(alpha), Math.round(160.0F + this.clearButtonAnimation * 95.0F)) : dim2
         );
         Render2D.text(FONT, cLabel, cIconX + this.u(12.0F), this.btnTextY(by, btnH), this.u(9.0F), this.clearButtonAnimation > 0.35F ? text : dim2);
         this.clearHit = this.clipToParse(new float[]{btnX, by, btnW, btnH});
         by += btnH + this.u(8.0F);
         btnX = this.parseX + this.u(8.0F);
         CsStyle.divider(btnX, by, actionAreaW, alpha);
         by += this.u(6.0F);
         if (mod != null) {
            float settingGap = this.u(6.0F);
            int settingIndex = 0;
            this.drawSettingGridRow(
               settingIndex++,
               btnX,
               by,
               actionAreaW,
               settingGap,
               "Обновление",
               mod.getUpdateDelaySetting().getValue().intValue() + " мс",
               "upd",
               frac(mod.getUpdateDelaySetting()),
               (float)mx,
               (float)my,
               alpha,
               text,
               dim,
               accent
            );
            this.drawSettingGridRow(
               settingIndex++,
               btnX,
               by,
               actionAreaW,
               settingGap,
               "Скидка парса",
               mod.getParseDiscountSetting().getValue().intValue() + "%",
               "disc",
               frac(mod.getParseDiscountSetting()),
               (float)mx,
               (float)my,
               alpha,
               text,
               dim,
               accent
            );
            this.drawSettingGridRow(
               settingIndex++,
               btnX,
               by,
               actionAreaW,
               settingGap,
               "Покупка кд",
               mod.getBuyDelaySetting().getValue().intValue() + " мс",
               "buy",
               frac(mod.getBuyDelaySetting()),
               (float)mx,
               (float)my,
               alpha,
               text,
               dim,
               accent
            );
            if (currentServer.equalsIgnoreCase("FunTime")) {
               this.drawSettingGridRow(
                  settingIndex++,
                  btnX,
                  by,
                  actionAreaW,
                  settingGap,
                  "FT анархия",
                  mod.getAnarchyMinSecSetting().getValue().intValue() + "–" + mod.getAnarchyMaxSecSetting().getValue().intValue() + "с",
                  "an",
                  frac(mod.getAnarchyMinSecSetting()),
                  (float)mx,
                  (float)my,
                  alpha,
                  text,
                  dim,
                  accent
               );
               this.drawSettingGridRow(
                  settingIndex++,
                  btnX,
                  by,
                  actionAreaW,
                  settingGap,
                  "Свап /an",
                  mod.getAnarchySwapSetting().getValue() ? "ON" : "OFF",
                  "answap",
                  -1.0F,
                  (float)mx,
                  (float)my,
                  alpha,
                  text,
                  dim,
                  accent
               );
            }

            if (currentServer.equalsIgnoreCase("SpookyTime")) {
               this.drawSettingGridRow(
                  settingIndex++,
                  btnX,
                  by,
                  actionAreaW,
                  settingGap,
                  "SP ходьба",
                  (mod.getSpWalkSetting().getValue() ? "ON " : "OFF ")
                     + mod.getSpWalkBlocksSetting().getValue().intValue()
                     + "б / "
                     + mod.getSpWalkIntervalSetting().getValue().intValue()
                     + "с",
                  "walk",
                  frac(mod.getSpWalkBlocksSetting()),
                  (float)mx,
                  (float)my,
                  alpha,
                  text,
                  dim,
                  accent
               );
            }

            if (currentServer.equalsIgnoreCase("HolyWorld")) {
               this.drawSettingGridRow(
                  settingIndex++,
                  btnX,
                  by,
                  actionAreaW,
                  settingGap,
                  "HW refresh",
                  Math.min(450, mod.getUpdateDelaySetting().getValue().intValue()) + " мс",
                  "upd",
                  frac(mod.getUpdateDelaySetting()),
                  (float)mx,
                  (float)my,
                  alpha,
                  text,
                  dim,
                  accent
               );
            }

            this.drawSettingGridRow(
               settingIndex++,
               btnX,
               by,
               actionAreaW,
               settingGap,
               "Авто /ah",
               mod.getAutoOpenAhSetting().getValue() ? "ON" : "OFF",
               "ah",
               -1.0F,
               (float)mx,
               (float)my,
               alpha,
               text,
               dim,
               accent
            );
            this.drawSettingGridRow(
               settingIndex++,
               btnX,
               by,
               actionAreaW,
               settingGap,
               "ReParse",
               mod.getReparseMinutesSetting().getValue().intValue() + " мин",
               "rep",
               frac(mod.getReparseMinutesSetting()),
               (float)mx,
               (float)my,
               alpha,
               text,
               dim,
               accent
            );
            by += (float)((settingIndex + 1) / 2) * this.u(34.0F);
         }

         if (parseRun && mod != null) {
            by += this.u(4.0F);
            String prog = "Парс " + (mod.getParseIndex() + 1) + "/" + Math.max(1, mod.getParseQueueSize());
            float pw = Render2D.textWidth(FONT_BOLD, prog, this.u(9.0F));
            Render2D.text(FONT_BOLD, prog, btnX + (actionAreaW - pw) * 0.5F, by, this.u(9.0F), CsStyle.warning(alpha));
            by += this.u(12.0F);
            String cur = clip(mod.getParseCurrentName(), actionAreaW, this.u(8.0F));
            float cw = Render2D.textWidth(FONT, cur, this.u(8.0F));
            Render2D.text(FONT, cur, btnX + (actionAreaW - cw) * 0.5F, by, this.u(8.0F), dim2);
            by += this.u(10.0F);
         }

         if (enabled.isEmpty()) {
            by += this.u(10.0F);
            String tip = clip("Включи предметы слева", actionAreaW, this.u(8.5F));
            float twx = Render2D.textWidth(FONT, tip, this.u(8.5F));
            Render2D.text(FONT, tip, btnX + (actionAreaW - twx) * 0.5F, by, this.u(8.5F), dim);
            by += this.u(10.0F);
         }

         this.parseContentH = by + this.scrollParse - this.parseY + this.u(4.0F);
         maxParse = Math.max(0.0F, this.parseContentH - this.parseH);
         this.targetScrollParse = Math.max(0.0F, Math.min(this.targetScrollParse, maxParse));
         Render2D.popScissor(g);
         this.parseBar = CsStyle.scrollbar(this.parseX, this.parseY, this.parseW, this.parseH, this.parseContentH, this.scrollParse, accent, alpha);
      } else {
         this.parseBar = null;
      }
   }

   private float btnTextY(float y, float h) {
      return this.centeredTextY(y, h, this.u(9.0F));
   }

   private float centeredTextY(float y, float h, float textSize) {
      return y + (h - textSize) * 0.5F + this.u(0.5F);
   }

   private float[] clipToParse(float[] box) {
      float top = Math.max(box[1], this.parseY);
      float bottom = Math.min(box[1] + box[3], this.parseY + this.parseH);
      if (bottom - top <= 1.0F) {
         return null;
      } else {
         box[1] = top;
         box[3] = bottom - top;
         return box;
      }
   }

   private void drawColHeader(String title, String badge, float x, float y, float w, float h, int accent, float alpha) {
      float tick = this.u(10.0F);
      Render2D.rect(
         x + this.u(8.0F),
         y + (h - tick) * 0.5F,
         this.u(2.0F),
         tick,
         this.u(1.0F),
         CsStyle.accentAlpha(accent, 235, alpha),
         CsStyle.accentAlpha(accent, 235, alpha),
         CsStyle.accentAlpha(accent, 80, alpha),
         CsStyle.accentAlpha(accent, 80, alpha)
      );
      float tSize = this.u(9.5F);
      Render2D.text(FONT_BOLD, title, x + this.u(15.0F), y + (h - tSize) * 0.5F + this.u(0.5F), tSize, CsStyle.text(alpha));
      if (badge != null && !badge.isEmpty()) {
         float bSize = this.u(8.0F);
         float bt = Render2D.textWidth(FONT_BOLD, badge, bSize);
         float bw = bt + this.u(10.0F);
         float bh = this.u(13.0F);
         float bx = x + w - bw - this.u(8.0F);
         float by = y + (h - bh) * 0.5F;
         Render2D.rect(bx, by, bw, bh, bh * 0.5F, CsStyle.accentAlpha(accent, 34, alpha));
         Render2D.outline(bx, by, bw, bh, bh * 0.5F, 0.7F, CsStyle.accentAlpha(accent, 95, alpha));
         Render2D.text(FONT_BOLD, badge, bx + (bw - bt) * 0.5F, by + (bh - bSize) * 0.5F + this.u(0.5F), bSize, CsStyle.accentAlpha(accent, 250, alpha));
      }

      CsStyle.divider(x + this.u(8.0F), y + h - 1.0F, w - this.u(16.0F), alpha);
   }

   private void drawSettingGridRow(
      int index,
      float x,
      float y,
      float totalWidth,
      float gap,
      String label,
      String value,
      String id,
      float fraction,
      float mx,
      float my,
      float alpha,
      int text,
      int dim,
      int accent
   ) {
      float width = (totalWidth - gap) * 0.5F;
      float itemX = x + (float)(index % 2) * (width + gap);
      float itemY = y + (float)(index / 2) * this.u(34.0F);
      this.drawSettingRow(itemX, itemY, width, label, value, id, fraction, mx, my, alpha, text, dim, accent);
   }

   private float drawSettingRow(
      float x, float y, float w, String label, String value, String id, float fraction, float mx, float my, float alpha, int text, int dim, int accent
   ) {
      float h = this.u(30.0F);
      boolean hov = hit((double)mx, (double)my, x, y, w, h) && my >= this.parseY && my <= this.parseY + this.parseH;
      int top = CsStyle.controlTop(hov ? 128 : 72, alpha);
      int bottom = CsStyle.controlBottom(hov ? 142 : 84, alpha);
      Render2D.rect(x, y, w, h, 6.0F, top, top, bottom, bottom);
      if (hov) {
         Render2D.outline(x, y, w, h, 6.0F, 0.9F, CsStyle.accentAlpha(accent, 110, alpha));
      }

      CsStyle.topHighlight(x, y, w, 6.0F, Math.round((float)(hov ? 30 : 18) * alpha));
      float labelSize = this.u(8.5F);
      float valueSize = this.u(9.5F);
      float textY = y + this.u(5.0F);
      Render2D.text(FONT, label, x + this.u(8.0F), textY + (valueSize - labelSize) * 0.5F, labelSize, dim);
      float vw = Render2D.textWidth(FONT_BOLD, value, valueSize);
      Render2D.text(FONT_BOLD, value, x + w - vw - this.u(8.0F), textY, valueSize, hov ? CsStyle.accentAlpha(accent, 250, alpha) : text);
      if (fraction >= 0.0F) {
         CsStyle.valueBar(x + this.u(8.0F), y + h - this.u(9.0F), w - this.u(16.0F), this.u(3.0F), fraction, accent, alpha);
      }

      if (hov) {
         float chip = this.u(9.0F);
         float chipY = y + this.u(3.0F);
         float minusX = x + w - this.u(8.0F) - vw - chip * 2.0F - this.u(6.0F);
         CsStyle.chip(minusX, chipY, chip, "−", true, accent, alpha);
         CsStyle.chip(minusX + chip + this.u(3.0F), chipY, chip, "+", true, accent, alpha);
         this.stepperHits.add(new float[]{minusX, chipY, chip, chip, settingId(id), -1.0F});
         this.stepperHits.add(new float[]{minusX + chip + this.u(3.0F), chipY, chip, chip, settingId(id), 1.0F});
      }

      float[] box = this.clipToParse(new float[]{x, y, w, h});
      if (box != null) {
         this.settingHits.add(new float[]{box[0], box[1], box[2], box[3], settingId(id)});
      }

      return y + h + this.u(4.0F);
   }

   private static float frac(NumberSetting setting) {
      if (setting == null) {
         return -1.0F;
      } else {
         double min = setting.getMin();
         double max = setting.getMax();
         return max <= min ? -1.0F : (float)Math.max(0.0, Math.min(1.0, (setting.getValue() - min) / (max - min)));
      }
   }

   private static void drawGradientButton(float x, float y, float w, float h, int top, int bottom, float alpha) {
      Render2D.rect(x, y, w, h, 6.0F, top, top, bottom, bottom);
      CsStyle.topHighlight(x, y, w, 6.0F, Math.round(34.0F * alpha));
   }

   private static float settingId(String id) {
      return switch (id) {
         case "upd" -> 1.0F;
         case "disc" -> 2.0F;
         case "buy" -> 3.0F;
         case "an" -> 4.0F;
         case "walk" -> 5.0F;
         case "ah" -> 6.0F;
         case "rep" -> 7.0F;
         case "answap" -> 8.0F;
         default -> 0.0F;
      };
   }

   private List<AutoBuyItem> filteredCatalog(String serverMode) {
      AutoBuyItemCategory[] cats = catsFor(serverMode);
      int idx = Math.max(0, Math.min(this.selectedCatIndex, cats.length - 1));
      String key = serverMode + "#" + idx;
      if (this.cachedCatalog != null && key.equals(this.cachedCatalogKey)) {
         return this.cachedCatalog;
      } else {
         AutoBuyItemCategory cat = cats[idx];
         this.cachedCatalog = cat == null ? AutoBuyManager.get().itemsForServer(serverMode) : AutoBuyManager.get().byCategoryForServer(cat, serverMode);
         this.cachedCatalogKey = key;
         return this.cachedCatalog;
      }
   }

   private static AutoBuyItemCategory[] catsFor(String serverMode) {
      return isHolyWorld(serverMode) ? CATS_HW : CATS_FT;
   }

   private static String[] catLabelsFor(String serverMode) {
      return isHolyWorld(serverMode) ? CAT_LABELS_HW : CAT_LABELS_FT;
   }

   private static boolean isHolyWorld(String serverMode) {
      return serverMode != null && serverMode.equalsIgnoreCase("HolyWorld");
   }

   private static String serverShort(String serverMode) {
      if (serverMode == null) {
         return "FT";
      } else if (serverMode.equalsIgnoreCase("HolyWorld")) {
         return "HW";
      } else {
         return serverMode.equalsIgnoreCase("SpookyTime") ? "SP" : "FT";
      }
   }

   private static String formatPrice(int price) {
      if (price >= 1000000) {
         return String.format(Locale.ROOT, "%.1fM", (double)price / 1000000.0);
      } else {
         return price >= 10000 ? String.format(Locale.ROOT, "%.1fK", (double)price / 1000.0) : String.valueOf(price);
      }
   }

   private void drawIcon(DrawContext g, AutoBuyItem item, float x, float y, float size, float alpha) {
      try {
         ItemStack stack = item.createIcon();
         if (g != null) {
            try {
               g.drawItem(stack, (int)x, (int)y);
               return;
            } catch (Throwable var9) {
            }
         }

         RenderItem.item(stack, x, y, size, alpha);
      } catch (Throwable var10) {
      }
   }

   private void drawCatalogTooltip(AutoBuyItem item, float mouseX, float mouseY, float areaX, float areaY, float areaW, float areaH, int accent, float alpha) {
      String price = "Лимит: " + formatPrice(item.getBuyPrice());
      float titleSize = this.u(9.0F);
      float detailSize = this.u(7.5F);
      float tooltipW = Math.max(
         this.u(92.0F), Math.max(Render2D.textWidth(FONT_BOLD, item.getName(), titleSize), Render2D.textWidth(FONT, price, detailSize)) + this.u(18.0F)
      );
      tooltipW = Math.min(tooltipW, Math.max(this.u(120.0F), areaW * 0.48F));
      float tooltipH = this.u(30.0F);
      float tooltipX = Math.max(areaX + this.u(4.0F), Math.min(mouseX + this.u(9.0F), areaX + areaW - tooltipW - this.u(4.0F)));
      float tooltipY = Math.max(areaY + this.u(4.0F), Math.min(mouseY + this.u(9.0F), areaY + areaH - tooltipH - this.u(4.0F)));
      CsStyle.glow(tooltipX, tooltipY, tooltipW, tooltipH, this.u(6.0F), accent, 0.65F, alpha);
      CsStyle.card(tooltipX, tooltipY, tooltipW, tooltipH, this.u(6.0F), alpha, true);
      Render2D.rect(
         tooltipX + this.u(5.0F), tooltipY + this.u(6.0F), this.u(2.0F), tooltipH - this.u(12.0F), this.u(1.0F), CsStyle.accentAlpha(accent, 220, alpha)
      );
      String title = clip(item.getName(), tooltipW - this.u(20.0F), titleSize);
      Render2D.text(FONT_BOLD, title, tooltipX + this.u(11.0F), tooltipY + this.u(5.0F), titleSize, CsStyle.text(alpha));
      Render2D.text(
         FONT,
         price,
         tooltipX + this.u(11.0F),
         tooltipY + this.u(17.0F),
         detailSize,
         item.isEnabled() ? CsStyle.accentAlpha(accent, 235, alpha) : CsStyle.muted(alpha)
      );
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (!(mx < (double)this.viewX) && !(mx > (double)(this.viewX + this.viewW)) && !(my < (double)this.viewY) && !(my > (double)(this.viewY + this.viewH))) {
         AutoBuy mod = module();
         if (btn != 0 || !this.grabBar(mx, my, this.catBar, 1) && !this.grabBar(mx, my, this.rulesBar, 2) && !this.grabBar(mx, my, this.parseBar, 3)) {
            boolean inCat = hit(mx, my, this.catX, this.catY, this.catW, this.catH);
            boolean inRules = hit(mx, my, this.rulesX, this.rulesY, this.rulesW, this.rulesH);
            boolean inParse = hit(mx, my, this.parseX, this.parseY, this.parseW, this.parseH);

            for (float[] t : this.serverHits) {
               if (btn == 0 && hit(mx, my, t[0], t[1], t[2], t[3]) && mod != null) {
                  String next = SERVERS[(int)t[4]];
                  String prev = mod.getServerMode().getValue();
                  if (!next.equalsIgnoreCase(prev)) {
                     mod.getServerMode().setValue(next);
                     boolean familyChanged = isHolyWorld(prev) != isHolyWorld(next);
                     if (familyChanged) {
                        this.selectedCatIndex = 0;
                        this.scrollCat = this.targetScrollCat = 0.0F;
                        this.scrollRules = this.targetScrollRules = 0.0F;
                     }

                     this.contentTransition = 0.0F;
                     this.lastServerMode = next;
                  }

                  return true;
               }
            }

            for (float[] tx : this.catTabHits) {
               if (btn == 0 && hit(mx, my, tx[0], tx[1], tx[2], tx[3])) {
                  this.selectedCatIndex = (int)tx[4];
                  this.targetScrollCat = this.scrollCat = 0.0F;
                  this.contentTransition = 0.0F;
                  return true;
               }
            }

            for (float[] page : this.pageHits) {
               if (btn == 0 && hit(mx, my, page[0], page[1], page[2], page[3])) {
                  int nextPage = (int)page[4];
                  if (this.selectedPage != nextPage) {
                     this.commitPriceEdit();
                     this.selectedPage = nextPage;
                     this.contentTransition = 0.0F;
                  }

                  return true;
               }
            }

            if (inParse
               && this.parseNowHit != null
               && btn == 0
               && hit(mx, my, this.parseNowHit[0], this.parseNowHit[1], this.parseNowHit[2], this.parseNowHit[3])) {
               if (mod != null && hasWorld()) {
                  mod.startParseNow();
               }

               return true;
            } else if (inParse
               && this.autoParseHit != null
               && btn == 0
               && hit(mx, my, this.autoParseHit[0], this.autoParseHit[1], this.autoParseHit[2], this.autoParseHit[3])) {
               if (mod != null) {
                  mod.toggleAutoParse();
               }

               return true;
            } else if (inParse && this.clearHit != null && btn == 0 && hit(mx, my, this.clearHit[0], this.clearHit[1], this.clearHit[2], this.clearHit[3])) {
               String mode = mod != null ? mod.getServerMode().getValue() : "FunTime";
               AutoBuyManager.get().disableAll(mode);
               this.editingPriceItem = null;
               this.priceDraft = "";
               return true;
            } else {
               if (mod != null && inParse) {
                  for (float[] txx : this.stepperHits) {
                     if (btn == 0 && hit(mx, my, txx[0], txx[1], txx[2], txx[3])) {
                        this.applySettingStep(mod, (int)txx[4], (int)txx[5]);
                        return true;
                     }
                  }

                  for (float[] txxx : this.settingHits) {
                     if (hit(mx, my, txxx[0], txxx[1], txxx[2], txxx[3])) {
                        int dir = btn == 0 ? 1 : (btn == 1 ? -1 : 0);
                        if (dir == 0) {
                           return true;
                        }

                        this.applySettingStep(mod, (int)txxx[4], dir);
                        return true;
                     }
                  }
               }

               if (inCat) {
                  for (int i = 0; i < this.catalogItems.size(); i++) {
                     float[] b = this.catalogBoxes.get(i);
                     if (hit(mx, my, b[0], b[1], b[2], b[3])) {
                        AutoBuyItem item = this.catalogItems.get(i);
                        if (btn == 0) {
                           if (!item.isEnabled()) {
                              AutoBuyManager.get().setEnabled(item, true);
                           }

                           return true;
                        }

                        if (btn == 1 && item.isEnabled()) {
                           AutoBuyManager.get().setEnabled(item, false);
                           if (this.editingPriceItem == item) {
                              this.editingPriceItem = null;
                              this.priceDraft = "";
                           }
                        }

                        return true;
                     }
                  }
               }

               if (inRules) {
                  for (int ix = 0; ix < this.ruleItems.size(); ix++) {
                     float[] b = this.ruleBoxes.get(ix);
                     AutoBuyItem itemx = this.ruleItems.get(ix);
                     if (hit(mx, my, b[4], b[5], b[6], b[7])) {
                        if (btn == 0) {
                           this.commitPriceEdit();
                           this.editingPriceItem = itemx;
                           this.priceDraft = String.valueOf(itemx.getBuyPrice());
                        }

                        return true;
                     }

                     if (hit(mx, my, b[0], b[1], b[2], b[3])) {
                        if (btn == 1) {
                           AutoBuyManager.get().setEnabled(itemx, false);
                           if (this.editingPriceItem == itemx) {
                              this.editingPriceItem = null;
                              this.priceDraft = "";
                           }

                           return true;
                        }

                        if (btn == 0) {
                           this.commitPriceEdit();
                           this.editingPriceItem = itemx;
                           this.priceDraft = String.valueOf(itemx.getBuyPrice());
                           return true;
                        }
                     }
                  }
               }

               if (btn == 0 && this.editingPriceItem != null) {
                  this.commitPriceEdit();
                  return true;
               } else {
                  return false;
               }
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean grabBar(double mx, double my, float[] bar, int id) {
      if (bar != null && hit(mx, my, bar[0] - 3.0F, bar[4], bar[2] + 6.0F, bar[5])) {
         this.draggingBar = id;
         this.barGrabOffset = (float)my - bar[4];
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseDragged(double mx, double my) {
      if (this.draggingBar == 0) {
         return false;
      } else {
         float[] bar = switch (this.draggingBar) {
            case 1 -> this.catBar;
            case 2 -> this.rulesBar;
            case 3 -> this.parseBar;
            default -> null;
         };
         if (bar == null) {
            this.draggingBar = 0;
            return false;
         } else {
            float travel = Math.max(1.0F, bar[3] - bar[5]);
            float progress = Math.max(0.0F, Math.min(1.0F, ((float)my - this.barGrabOffset - bar[1]) / travel));
            switch (this.draggingBar) {
               case 1: {
                  float max = Math.max(0.0F, this.contentFor(1) - this.catH);
                  this.targetScrollCat = this.scrollCat = progress * max;
                  break;
               }
               case 2: {
                  float max = Math.max(0.0F, this.contentFor(2) - this.rulesH);
                  this.targetScrollRules = this.scrollRules = progress * max;
                  break;
               }
               case 3: {
                  float max = Math.max(0.0F, this.parseContentH - this.parseH);
                  this.targetScrollParse = this.scrollParse = progress * max;
               }
            }

            return true;
         }
      }
   }

   private float contentFor(int id) {
      float[] bar = id == 1 ? this.catBar : this.rulesBar;
      float view = id == 1 ? this.catH : this.rulesH;
      return bar != null && !(bar[5] <= 0.0F) ? view * view / bar[5] : view;
   }

   private void applySettingStep(AutoBuy mod, int sid, int dir) {
      switch (sid) {
         case 1:
            mod.adjustUpdateDelay(dir * 50);
            break;
         case 2:
            mod.adjustParseDiscount(dir);
            break;
         case 3:
            int v = mod.getBuyDelaySetting().getValue().intValue() + dir * 10;
            mod.getBuyDelaySetting().setValue(Double.valueOf((double)Math.max(50, Math.min(5000, v))));
            break;
         case 4:
            int min = mod.getAnarchyMinSecSetting().getValue().intValue() + dir * 5;
            int max = mod.getAnarchyMaxSecSetting().getValue().intValue() + dir * 5;
            mod.getAnarchyMinSecSetting().setValue(Double.valueOf((double)Math.max(30, Math.min(600, min))));
            mod.getAnarchyMaxSecSetting()
               .setValue(Double.valueOf((double)Math.max(mod.getAnarchyMinSecSetting().getValue().intValue() + 5, Math.min(900, max))));
            break;
         case 5:
            if (dir > 0) {
               mod.getSpWalkSetting().setValue(Boolean.valueOf(!mod.getSpWalkSetting().getValue()));
            } else {
               int b = mod.getSpWalkBlocksSetting().getValue().intValue() - 1;
               if (b < 2) {
                  b = 15;
               }

               mod.getSpWalkBlocksSetting().setValue(Double.valueOf((double)b));
            }
            break;
         case 6:
            mod.getAutoOpenAhSetting().setValue(Boolean.valueOf(!mod.getAutoOpenAhSetting().getValue()));
            break;
         case 7:
            int r = mod.getReparseMinutesSetting().getValue().intValue() + dir * 5;
            mod.getReparseMinutesSetting().setValue(Double.valueOf((double)Math.max(0, Math.min(240, r))));
            break;
         case 8:
            mod.getAnarchySwapSetting().setValue(Boolean.valueOf(!mod.getAnarchySwapSetting().getValue()));
      }
   }

   public void mouseReleased(double mx, double my, int btn) {
      this.draggingBar = 0;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.editingPriceItem == null) {
         return false;
      } else if (keyCode == 256) {
         this.editingPriceItem = null;
         this.priceDraft = "";
         return true;
      } else if (keyCode == 257 || keyCode == 335) {
         this.commitPriceEdit();
         return true;
      } else if (keyCode == 259) {
         if (!this.priceDraft.isEmpty()) {
            this.priceDraft = this.priceDraft.substring(0, this.priceDraft.length() - 1);
         }

         return true;
      } else if (keyCode == 86 && (modifiers & 2) != 0) {
         this.appendDigits(clipboard());
         return true;
      } else {
         return true;
      }
   }

   private static String clipboard() {
      try {
         MinecraftClient client = MinecraftClient.getInstance();
         return client != null && client.keyboard != null ? client.keyboard.getClipboard() : "";
      } catch (Throwable var1) {
         return "";
      }
   }

   private void appendDigits(String source) {
      if (source != null) {
         for (int i = 0; i < source.length() && this.priceDraft.length() < 10; i++) {
            char c = source.charAt(i);
            if (c >= '0' && c <= '9') {
               this.priceDraft = this.priceDraft + c;
            }
         }
      }
   }

   public boolean charTyped(char codePoint, int modifiers) {
      if (this.editingPriceItem == null) {
         return false;
      } else {
         if (codePoint >= '0' && codePoint <= '9' && this.priceDraft.length() < 10) {
            this.priceDraft = this.priceDraft + codePoint;
         }

         return true;
      }
   }

   private void commitPriceEdit() {
      if (this.editingPriceItem != null) {
         String draft = this.priceDraft.trim();
         if (!draft.isEmpty()) {
            try {
               long parsed = Long.parseLong(draft);
               AutoBuyManager.get().setBuyPrice(this.editingPriceItem, (int)Math.max(0L, Math.min(2147483647L, parsed)));
            } catch (NumberFormatException var4) {
            }
         }

         this.editingPriceItem = null;
         this.priceDraft = "";
      }
   }

   private void resetAnimations() {
      this.lastAnimationFrameNanos = 0L;
      this.animationDelta = 0.016666668F;
      this.panelAnimation = 0.0F;
      this.contentTransition = 0.0F;
      this.serverIndicatorX = Float.NaN;
      this.serverIndicatorW = 0.0F;
      this.pageIndicatorX = Float.NaN;
      this.pageIndicatorW = 0.0F;
      Arrays.fill(this.categoryAnimations, 0.0F);
      this.catalogHoverAnimations.clear();
      this.catalogEnabledAnimations.clear();
      this.ruleHoverAnimations.clear();
      this.parseButtonAnimation = 0.0F;
      this.autoParseButtonAnimation = 0.0F;
      this.clearButtonAnimation = 0.0F;
   }

   private void beginAnimationFrame() {
      long now = System.nanoTime();
      if (this.lastAnimationFrameNanos == 0L) {
         this.animationDelta = 0.016666668F;
      } else {
         float elapsed = (float)(now - this.lastAnimationFrameNanos) / 1.0E9F;
         if (elapsed > 0.45F) {
            this.panelAnimation = 0.0F;
            this.contentTransition = 0.0F;
            this.serverIndicatorX = Float.NaN;
            this.pageIndicatorX = Float.NaN;
            Arrays.fill(this.categoryAnimations, 0.0F);
            this.catalogHoverAnimations.clear();
            this.catalogEnabledAnimations.clear();
            this.ruleHoverAnimations.clear();
            this.parseButtonAnimation = 0.0F;
            this.autoParseButtonAnimation = 0.0F;
            this.clearButtonAnimation = 0.0F;
            this.animationDelta = 0.016666668F;
         } else {
            this.animationDelta = Math.max(0.004166667F, Math.min(0.05F, elapsed));
         }
      }

      this.lastAnimationFrameNanos = now;
   }

   private float animate(float current, float target, float speed) {
      float factor = 1.0F - (float)Math.exp((double)(-Math.max(0.0F, speed) * this.animationDelta));
      float value = current + (target - current) * factor;
      return Math.abs(target - value) < 0.001F ? target : value;
   }

   private float animateItem(IdentityHashMap<AutoBuyItem, Float> animations, AutoBuyItem item, boolean active, float speed) {
      float value = this.animate(animations.getOrDefault(item, Float.valueOf(0.0F)), active ? 1.0F : 0.0F, speed);
      animations.put(item, value);
      return value;
   }

   private float staggeredReveal(int index, float delay) {
      float start = 0.12F + (float)Math.min(12, Math.max(0, index)) * delay;
      float revealState = Math.min(this.panelAnimation, this.contentTransition);
      float progress = (revealState - start) / Math.max(0.05F, 1.0F - start);
      return easeOutCubic(Math.max(0.0F, Math.min(1.0F, progress)));
   }

   private static float easeOutCubic(float value) {
      float clamped = Math.max(0.0F, Math.min(1.0F, value));
      float inverse = 1.0F - clamped;
      return 1.0F - inverse * inverse * inverse;
   }

   private static String clip(String text, float maxW, float size) {
      if (text == null) {
         return "";
      } else if (Render2D.textWidth(FONT, text, size) <= maxW) {
         return text;
      } else {
         String s = text;

         while (s.length() > 1 && Render2D.textWidth(FONT, s + "…", size) > maxW) {
            s = s.substring(0, s.length() - 1);
         }

         return s + "…";
      }
   }

   private static boolean hasWorld() {
      try {
         MinecraftClient client = MinecraftClient.getInstance();
         return client != null && client.world != null && client.player != null;
      } catch (Throwable var1) {
         return false;
      }
   }

   private static boolean hit(double mx, double my, float x, float y, float w, float h) {
      return mx >= (double)x && mx <= (double)(x + w) && my >= (double)y && my <= (double)(y + h);
   }

   private static AutoBuy module() {
      try {
         AutoBuy direct = AutoBuy.getInstance();
         if (direct != null) {
            return direct;
         } else {
            return Manager.getModules() == null ? null : Manager.getModules().getByType(AutoBuy.class).orElse(null);
         }
      } catch (Throwable var1) {
         return null;
      }
   }
}
