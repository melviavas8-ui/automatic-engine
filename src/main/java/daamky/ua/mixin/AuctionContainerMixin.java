package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.player.AutoBuy;
import daamky.ua.utils.modules.autobuy.AuctionUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;

@Mixin({AbstractContainerScreen.class})
public abstract class AuctionContainerMixin extends Screen {
   @Shadow
   protected int leftPos;
   @Shadow
   protected int topPos;
   @Shadow
   protected int imageWidth;
   @Shadow
   protected int imageHeight;
   @Unique
   private Button cataclysm$autoBuyBtn;
   @Unique
   private Button cataclysm$modeBtn;
   @Unique
   private Button cataclysm$autoParseBtn;
   @Unique
   private Button cataclysm$parseNowBtn;
   @Unique
   private Button cataclysm$discountBtn;
   @Unique
   private Button cataclysm$refreshBtn;
   @Unique
   private Button cataclysm$anarchyBtn;

   protected AuctionContainerMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void cataclysm$initAuctionButtons(CallbackInfo ci) {
      if ((Object)this instanceof ContainerScreen screen) {
         if (this.cataclysm$isAuction(screen)) {
            AutoBuy ab = AutoBuy.getInstance();
            if (ab != null) {
               int btnW = 110;
               int btnH = 20;
               int autoBuyX = this.leftPos + this.imageWidth / 2 - btnW / 2;
               int autoBuyY = this.topPos - btnH - 5;
               if (autoBuyY < 4) {
                  autoBuyY = this.topPos + this.imageHeight + 4;
               }

               this.cataclysm$autoBuyBtn = Button.builder(this.cataclysm$autoBuyLabel(ab), b -> {
                  ab.toggle();
                  b.setMessage(this.cataclysm$autoBuyLabel(ab));
               }).bounds(autoBuyX, autoBuyY, btnW, btnH).build();
               this.addRenderableWidget(this.cataclysm$autoBuyBtn);
               this.cataclysm$modeBtn = Button.builder(this.cataclysm$modeLabel(ab), b -> {
                  ab.cycleServerMode();
                  b.setMessage(this.cataclysm$modeLabel(ab));
               }).bounds(autoBuyX, autoBuyY - 24, btnW, btnH).build();
               this.addRenderableWidget(this.cataclysm$modeBtn);
               int sideX = this.leftPos - 100;
               if (sideX < 4) {
                  sideX = this.leftPos + this.imageWidth + 4;
               }

               int sideY = this.topPos;
               int sideW = 95;
               this.cataclysm$autoParseBtn = Button.builder(this.cataclysm$parseLabel(ab), b -> {
                  ab.toggleAutoParse();
                  b.setMessage(this.cataclysm$parseLabel(ab));
               }).bounds(sideX, sideY, sideW, btnH).build();
               this.addRenderableWidget(this.cataclysm$autoParseBtn);
               this.cataclysm$discountBtn = Button.builder(this.cataclysm$discountLabel(ab), b -> {
                  int d = ab.getParseDiscountSetting().getValue().intValue() + 5;
                  if (d > 100) {
                     d = 5;
                  }

                  ab.getParseDiscountSetting().setValue(Double.valueOf((double)d));
                  b.setMessage(this.cataclysm$discountLabel(ab));
               }).bounds(sideX, sideY + 24, sideW, btnH).build();
               this.addRenderableWidget(this.cataclysm$discountBtn);
               this.cataclysm$refreshBtn = Button.builder(this.cataclysm$refreshLabel(ab), b -> {
                  int v = ab.getUpdateDelaySetting().getValue().intValue() + 50;
                  if (v > 2000) {
                     v = 150;
                  }

                  ab.getUpdateDelaySetting().setValue(Double.valueOf((double)v));
                  b.setMessage(this.cataclysm$refreshLabel(ab));
               }).bounds(sideX, sideY + 48, sideW, btnH).build();
               this.addRenderableWidget(this.cataclysm$refreshBtn);
               this.cataclysm$parseNowBtn = Button.builder(Component.literal("РџР°СЂСЃ СЃРµР№С‡Р°СЃ"), b -> ab.startParseNow())
                  .bounds(sideX, sideY + 72, sideW, btnH)
                  .build();
               this.addRenderableWidget(this.cataclysm$parseNowBtn);
               this.cataclysm$anarchyBtn = Button.builder(this.cataclysm$anarchyLabel(ab), b -> {
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
               }).bounds(sideX, sideY + 96, sideW, btnH).build();
               this.addRenderableWidget(this.cataclysm$anarchyBtn);
            }
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void cataclysm$updateAuctionButtons(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if ((Object)this instanceof ContainerScreen screen) {
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
               this.cataclysm$parseNowBtn.setMessage(Component.literal(ab.isParseRunning() ? "РџР°СЂСЃРёРЅРівЂ¦" : "РџР°СЂСЃ СЃРµР№С‡Р°СЃ"));
            }
         }
      }
   }

   @Unique
   private void updateBtn(Button btn, boolean show, Component msg) {
      if (btn != null) {
         btn.visible = show;
         btn.active = show;
         if (show && msg != null) {
            btn.setMessage(msg);
         }
      }
   }

   @Unique
   private boolean cataclysm$isAuction(ContainerScreen screen) {
      String title = screen.getTitle().getString();
      if (!AuctionUtils.isAuctionTitle(title) && !AuctionUtils.isSearchTitle(title)) {
         if (title == null || !title.contains("РџРѕРёСЃРє") && !title.contains("Search") && !title.contains("РђСѓРєС†РёРѕРЅ")) {
            int n = Math.min(54, ((ChestMenu)screen.getMenu()).slots.size());

            for (int i = 0; i < n; i++) {
               if (AuctionUtils.getPrice(((Slot)((ChestMenu)screen.getMenu()).slots.get(i)).getItem()) > 0) {
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
   private Component cataclysm$autoBuyLabel(AutoBuy ab) {
      return Component.literal("AutoBuy: " + (ab.isEnabled() ? "В§aON" : "В§cOFF"));
   }

   @Unique
   private Component cataclysm$modeLabel(AutoBuy ab) {
      return Component.literal("Mode: " + ab.getServerMode().getValue());
   }

   @Unique
   private Component cataclysm$parseLabel(AutoBuy ab) {
      return Component.literal("AutoParse: " + (ab.isAutoParseEnabled() ? "В§aON" : "В§cOFF"));
   }

   @Unique
   private Component cataclysm$discountLabel(AutoBuy ab) {
      return Component.literal("РЎРєРёРґРєР°: " + ab.getParseDiscountSetting().getValue().intValue() + "%");
   }

   @Unique
   private Component cataclysm$refreshLabel(AutoBuy ab) {
      return Component.literal("РћР±РЅРѕРІ: " + ab.getUpdateDelaySetting().getValue().intValue() + "ms");
   }

   @Unique
   private Component cataclysm$anarchyLabel(AutoBuy ab) {
      if (ab.getServerMode().is("FunTime")) {
         return Component.literal("FT /an: " + ab.getAnarchyMinSecSetting().getValue().intValue() + "-" + ab.getAnarchyMaxSecSetting().getValue().intValue() + "s");
      } else {
         return ab.getServerMode().is("SpookyTime")
            ? Component.literal("SP walk: " + (ab.getSpWalkSetting().getValue() ? "В§aON" : "В§cOFF") + " " + ab.getSpWalkBlocksSetting().getValue().intValue() + "b")
            : Component.literal("HW В· unit buy");
      }
   }
}

