package daamky.ua.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.player.AutoBuy;
import daamky.ua.utils.modules.autobuy.AuctionUtils;

@Mixin({HandledScreen.class})
public abstract class AuctionContainerMixin extends Screen {
   @Shadow
   protected int x;
   @Shadow
   protected int y;
   @Shadow
   protected int backgroundWidth;
   @Shadow
   protected int backgroundHeight;
   @Unique
   private ButtonWidget cataclysm$autoBuyBtn;
   @Unique
   private ButtonWidget cataclysm$modeBtn;
   @Unique
   private ButtonWidget cataclysm$autoParseBtn;
   @Unique
   private ButtonWidget cataclysm$parseNowBtn;
   @Unique
   private ButtonWidget cataclysm$discountBtn;
   @Unique
   private ButtonWidget cataclysm$refreshBtn;
   @Unique
   private ButtonWidget cataclysm$anarchyBtn;

   protected AuctionContainerMixin(Text title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void cataclysm$initAuctionButtons(CallbackInfo ci) {
      if ((Object)this instanceof GenericContainerScreen screen) {
         if (this.cataclysm$isAuction(screen)) {
            AutoBuy ab = AutoBuy.getInstance();
            if (ab != null) {
               int btnW = 110;
               int btnH = 20;
               int autoBuyX = this.x + this.backgroundWidth / 2 - btnW / 2;
               int autoBuyY = this.y - btnH - 5;
               if (autoBuyY < 4) {
                  autoBuyY = this.y + this.backgroundHeight + 4;
               }

               this.cataclysm$autoBuyBtn = ButtonWidget.builder(this.cataclysm$autoBuyLabel(ab), b -> {
                  ab.toggle();
                  b.setMessage(this.cataclysm$autoBuyLabel(ab));
               }).dimensions(autoBuyX, autoBuyY, btnW, btnH).build();
               this.addDrawableChild(this.cataclysm$autoBuyBtn);
               this.cataclysm$modeBtn = ButtonWidget.builder(this.cataclysm$modeLabel(ab), b -> {
                  ab.cycleServerMode();
                  b.setMessage(this.cataclysm$modeLabel(ab));
               }).dimensions(autoBuyX, autoBuyY - 24, btnW, btnH).build();
               this.addDrawableChild(this.cataclysm$modeBtn);
               int sideX = this.x - 100;
               if (sideX < 4) {
                  sideX = this.x + this.backgroundWidth + 4;
               }

               int sideY = this.y;
               int sideW = 95;
               this.cataclysm$autoParseBtn = ButtonWidget.builder(this.cataclysm$parseLabel(ab), b -> {
                  ab.toggleAutoParse();
                  b.setMessage(this.cataclysm$parseLabel(ab));
               }).dimensions(sideX, sideY, sideW, btnH).build();
               this.addDrawableChild(this.cataclysm$autoParseBtn);
               this.cataclysm$discountBtn = ButtonWidget.builder(this.cataclysm$discountLabel(ab), b -> {
                  int d = ab.getParseDiscountSetting().getValue().intValue() + 5;
                  if (d > 100) {
                     d = 5;
                  }

                  ab.getParseDiscountSetting().setValue(Double.valueOf((double)d));
                  b.setMessage(this.cataclysm$discountLabel(ab));
               }).dimensions(sideX, sideY + 24, sideW, btnH).build();
               this.addDrawableChild(this.cataclysm$discountBtn);
               this.cataclysm$refreshBtn = ButtonWidget.builder(this.cataclysm$refreshLabel(ab), b -> {
                  int v = ab.getUpdateDelaySetting().getValue().intValue() + 50;
                  if (v > 2000) {
                     v = 150;
                  }

                  ab.getUpdateDelaySetting().setValue(Double.valueOf((double)v));
                  b.setMessage(this.cataclysm$refreshLabel(ab));
               }).dimensions(sideX, sideY + 48, sideW, btnH).build();
               this.addDrawableChild(this.cataclysm$refreshBtn);
               this.cataclysm$parseNowBtn = ButtonWidget.builder(Text.literal("РџР°СЂСЃ СЃРµР№С‡Р°СЃ"), b -> ab.startParseNow())
                  .dimensions(sideX, sideY + 72, sideW, btnH)
                  .build();
               this.addDrawableChild(this.cataclysm$parseNowBtn);
               this.cataclysm$anarchyBtn = ButtonWidget.builder(this.cataclysm$anarchyLabel(ab), b -> {
                  if (ab.getServerMode().is("FunTime")) {
                     int min = ab.getAnarchyMinSecSetting().getValue().intValue() + 5;
                     int max = ab.getAnarchyMaxSecSetting().getValue().intValue() + 5;
                     if (min > 300) {
                        min = 90;
                        max = 120;
                     }

                     ab.getAnarchyMinSecSetting().setValue(Double.valueOf((double)min));
                     ab.getAnarchyMaxSecSetting().setValue(Double.valueOf((double)Math.max(min + 5, max)));
                  } else if (ab.getServerMode().is("SpookyTime")) {
                     ab.getSpWalkSetting().setValue(Boolean.valueOf(!ab.getSpWalkSetting().getValue()));
                  } else {
                     int v = ab.getUpdateDelaySetting().getValue().intValue() + 50;
                     if (v > 450) {
                        v = 100;
                     }

                     ab.getUpdateDelaySetting().setValue(Double.valueOf((double)v));
                  }

                  b.setMessage(this.cataclysm$anarchyLabel(ab));
               }).dimensions(sideX, sideY + 96, sideW, btnH).build();
               this.addDrawableChild(this.cataclysm$anarchyBtn);
            }
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void cataclysm$updateAuctionButtons(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if ((Object)this instanceof GenericContainerScreen screen) {
         boolean auction = this.cataclysm$isAuction(screen);
         AutoBuy ab = AutoBuy.getInstance();
         boolean show = auction && ab != null;
         this.updateBtn(this.cataclysm$autoBuyBtn, show, show ? this.cataclysm$autoBuyLabel(ab) : null);
         this.updateBtn(this.cataclysm$modeBtn, show, show ? this.cataclysm$modeLabel(ab) : null);
         this.updateBtn(this.cataclysm$autoParseBtn, show, show ? this.cataclysm$parseLabel(ab) : null);
         this.updateBtn(this.cataclysm$discountBtn, show, show ? this.cataclysm$discountLabel(ab) : null);
         this.updateBtn(this.cataclysm$refreshBtn, show, show ? this.cataclysm$refreshLabel(ab) : null);
         this.updateBtn(this.cataclysm$anarchyBtn, show, show ? this.cataclysm$anarchyLabel(ab) : null);
         if (this.cataclysm$parseNowBtn != null) {
            this.cataclysm$parseNowBtn.visible = show;
            this.cataclysm$parseNowBtn.active = show && ab != null && !ab.isParseRunning();
            if (show) {
               this.cataclysm$parseNowBtn.setMessage(Text.literal(ab.isParseRunning() ? "РџР°СЂСЃРёРЅРівЂ¦" : "РџР°СЂСЃ СЃРµР№С‡Р°СЃ"));
            }
         }
      }
   }

   @Unique
   private void updateBtn(ButtonWidget btn, boolean show, Text msg) {
      if (btn != null) {
         btn.visible = show;
         btn.active = show;
         if (show && msg != null) {
            btn.setMessage(msg);
         }
      }
   }

   @Unique
   private boolean cataclysm$isAuction(GenericContainerScreen screen) {
      String title = screen.getTitle().getString();
      if (!AuctionUtils.isAuctionTitle(title) && !AuctionUtils.isSearchTitle(title)) {
         if (title == null || !title.contains("РџРѕРёСЃРє") && !title.contains("Search") && !title.contains("РђСѓРєС†РёРѕРЅ")) {
            int n = Math.min(54, ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size());

            for (int i = 0; i < n; i++) {
               if (AuctionUtils.getPrice(((Slot)((GenericContainerScreenHandler)screen.getScreenHandler()).slots.get(i)).getStack()) > 0) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   @Unique
   private Text cataclysm$autoBuyLabel(AutoBuy ab) {
      return Text.literal("AutoBuy: " + (ab.isEnabled() ? "В§aON" : "В§cOFF"));
   }

   @Unique
   private Text cataclysm$modeLabel(AutoBuy ab) {
      return Text.literal("Mode: " + ab.getServerMode().getValue());
   }

   @Unique
   private Text cataclysm$parseLabel(AutoBuy ab) {
      return Text.literal("AutoParse: " + (ab.isAutoParseEnabled() ? "В§aON" : "В§cOFF"));
   }

   @Unique
   private Text cataclysm$discountLabel(AutoBuy ab) {
      return Text.literal("РЎРєРёРґРєР°: " + ab.getParseDiscountSetting().getValue().intValue() + "%");
   }

   @Unique
   private Text cataclysm$refreshLabel(AutoBuy ab) {
      return Text.literal("РћР±РЅРѕРІ: " + ab.getUpdateDelaySetting().getValue().intValue() + "ms");
   }

   @Unique
   private Text cataclysm$anarchyLabel(AutoBuy ab) {
      if (ab.getServerMode().is("FunTime")) {
         return Text.literal("FT /an: " + ab.getAnarchyMinSecSetting().getValue().intValue() + "-" + ab.getAnarchyMaxSecSetting().getValue().intValue() + "s");
      } else {
         return ab.getServerMode().is("SpookyTime")
            ? Text.literal("SP walk: " + (ab.getSpWalkSetting().getValue() ? "В§aON" : "В§cOFF") + " " + ab.getSpWalkBlocksSetting().getValue().intValue() + "b")
            : Text.literal("HW В· unit buy");
      }
   }
}

