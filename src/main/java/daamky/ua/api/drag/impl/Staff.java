package daamky.ua.api.drag.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.utils.repository.staff.StaffUtils;

public final class Staff extends HudPanel {
   private static final float ROW_STEP = 12.0F;
   private static final float PANEL_ANIM = 0.24F;
   private static final float ROW_ANIM = 0.22F;
   private static final Comparator<Staff.RowState> ROW_STATE_COMPARATOR = Comparator.comparingDouble(Staff.RowState::offset);
   private final List<Staff.RowEntry> rowEntries = new ArrayList<>();
   private final List<Staff.StaffEntry> cachedStaffEntries = new ArrayList<>();
   private final List<Staff.RowState> rowStates = new ArrayList<>();
   private final Map<String, PlayerListEntry> tabPlayers = new HashMap<>();
   private final Map<String, PlayerEntity> worldPlayers = new HashMap<>();
   private final SmoothAnimation panelAnimation = new SmoothAnimation();
   private final SmoothAnimation iconAlphaAnimation = new SmoothAnimation();
   private boolean iconAlphaForward = true;

   public Staff() {
      super("staff", "Staff", 140.0F, 70.0F, 96.0F, 23.0F);
   }

   @Override
   public void render() {
      Staff.StaffState state = this.logics();
      if (state != null) {
         this.renderStaff(state);
      }
   }

   private Staff.StaffState logics() {
      List<Staff.StaffEntry> entries = this.staffEntries();
      boolean preview = entries.isEmpty() && this.editPreview();
      boolean targetVisible = !entries.isEmpty() || preview;
      this.drag.hitExpansion(0.0F, 17.0F, 0.0F, 0.0F);
      this.panelAnimation.update();
      this.panelAnimation.run(targetVisible ? 1.0 : 0.0, 0.24F, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
      this.iconAlphaAnimation.update();
      if (!this.iconAlphaAnimation.isAlive()) {
         this.iconAlphaAnimation.run(this.iconAlphaForward ? 1.0 : 0.0, 1.55, Easings.EXPO_IN_OUT);
         this.iconAlphaForward = !this.iconAlphaForward;
      }

      for (Staff.RowEntry entry : this.rowEntries) {
         entry.active = false;
         entry.alpha.update();
         entry.y.update();
      }

      int targetRows = 0;
      if (preview) {
         Staff.RowEntry entry = this.row(
            "__preview", new Staff.StaffEntry("Moderator", "Active", this.defaultSkin("Moderator"), ColorUtil.rgba(85, 255, 140, 235)), 0.0F
         );
         entry.active = true;
         entry.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
         entry.y.run(0.0, 0.22F, Easings.EXPO_OUT, true);
         targetRows = 1;
      } else {
         for (Staff.StaffEntry staff : entries) {
            float targetY = (float)targetRows * 12.0F;
            Staff.RowEntry entry = this.row(staff.name.toLowerCase(), staff, targetY);
            entry.active = true;
            entry.alpha.run(1.0, 0.22F, Easings.EXPO_OUT, true);
            entry.y.run((double)targetY, 0.22F, Easings.EXPO_OUT, true);
            targetRows++;
         }
      }

      for (Staff.RowEntry entry : this.rowEntries) {
         if (!entry.active) {
            entry.alpha.run(0.0, 0.22F, Easings.EXPO_IN, true);
         }
      }

      this.rowEntries.removeIf(entryx -> !entryx.active && entryx.alpha.get() <= 0.01F && !entryx.alpha.isAlive());
      float panelAlpha = this.panelAnimation.get();
      boolean visible = targetVisible || panelAlpha > 0.01F || !this.rowEntries.isEmpty();
      this.contentVisible(visible);
      if (!visible) {
         return null;
      } else {
         float width = 96.0F;

         for (Staff.RowEntry entryx : this.rowEntries) {
            if (entryx.alpha.get() > 0.01F || entryx.active) {
               width = Math.max(width, Render2D.textWidth(TEXT_FONT, entryx.name, 6.0F) + 46.0F);
            }
         }

         this.size(width, (float)Math.max(1, targetRows) * 11.0F + 10.0F);
         this.rowStates.clear();

         for (Staff.RowEntry entryxx : this.rowEntries) {
            float alpha = entryxx.alpha.get();
            if (alpha > 0.01F || entryxx.active) {
               this.rowStates.add(new Staff.RowState(entryxx.name, entryxx.status, entryxx.skin, entryxx.dotColor, entryxx.y.get(), alpha));
            }
         }

         if (this.rowStates.size() > 1) {
            this.rowStates.sort(ROW_STATE_COMPARATOR);
         }

         return new Staff.StaffState(
            this.rowStates, this.iconAlphaAnimation.get(), panelAlpha, this.drag.x(), this.drag.y(), this.drag.width(), this.drag.height()
         );
      }
   }

   private void renderStaff(Staff.StaffState state) {
      Hud hud = Hud.getInstance();
      int hudColor = hud != null ? hud.iconColor() : ColorUtil.rgba(247, 133, 255, 255);
      int fadedHudColor = ColorUtil.multAlpha(hudColor, state.alpha);
      int glowColor = ColorUtil.multAlpha(hudColor, 0.45F * state.alpha);
      float headerY = state.y - 17.0F;
      float headerHeight = 17.0F;
      float rowY = state.y + 5.0F;
      String headerText = "Staff List";
      float headerTextSize = 6.0F;
      float headerTextX = state.x + 10.0F;
      float headerTextY = headerY + (headerHeight - headerTextSize) * 0.5F - 0.5F;
      float headerTextWidth = Render2D.textWidth(TEXT_FONT, headerText, headerTextSize);
      float headerTextDotX = headerTextX + headerTextWidth + 3.0F;
      float iconSize = 8.0F;
      float iconWidth = Render2D.textWidth(FontType.MAINMENUSCREEN, "c", iconSize);
      float iconX = state.x + state.width - iconWidth - 8.0F;
      float iconY = headerY + (headerHeight - iconSize) * 0.5F + 1.0F;
      float glowSize = 22.0F;
      float glowX = iconX + iconWidth * 0.5F - glowSize * 0.5F;
      float glowY = headerY + (headerHeight - glowSize) * 0.5F;
      float sideRectY = headerY + (headerHeight - 9.0F) * 0.5F + 0.5F;
      float sideDotY = headerY + (headerHeight - 4.0F) * 0.5F + 0.5F;
      int glowAlpha = Math.round(128.0F * state.alpha);
      int glow = ColorUtil.rgba(247, 133, 255, glowAlpha);
      int purple = ColorUtil.rgba(247, 133, 255, Math.round(255.0F * state.alpha));
      int headerTextColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * state.alpha));
      int rowRectColor = ColorUtil.rgba(247, 133, 255, Math.round(255.0F * state.alpha));
      HudRenderCompat.background(
         Render2D.blurBuilder()
            .rectangle(state.x, headerY, state.width, headerHeight)
            .radius(4.0F, 4.0F, 2.0F, 2.0F)
            .blurRadius(15.0F)
            .smoothness(1.2F)
            .color(ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)))
            .build()
      );
      HudRenderCompat.background(
         Render2D.blurBuilder()
            .rectangle(state.x, state.y, state.width, state.height)
            .radius(2.0F, 2.0F, 4.0F, 4.0F)
            .blurRadius(15.0F)
            .smoothness(1.2F)
            .color(ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)))
            .build()
      );
      if (glowAlpha > 0) {
         HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", glowX - 13.0F, glowY + 0.5F, glowSize + 25.0F, glowSize, 0.0F, glowColor);
      }

      Render2D.rect(state.x, sideRectY, 2.5F, 9.0F, 0.0F, 2.0F, 2.0F, 0.0F, fadedHudColor);
      Render2D.rect(state.x + 3.5F, sideDotY, 4.0F, 4.0F, 4.0F, fadedHudColor);
      Render2D.rect(headerTextDotX, sideDotY, 4.0F, 4.0F, 4.0F, fadedHudColor);
      Render2D.text(TEXT_FONT, headerText, headerTextX, headerTextY, headerTextSize, headerTextColor);
      Render2D.text(FontType.MAINMENUSCREEN, "c", iconX, iconY, iconSize, fadedHudColor);

      for (Staff.RowState row : state.rows) {
         float rowAlpha = state.alpha * row.alpha;
         float currentY = rowY + row.offset;
         float headSize = 8.0F;
         float headX = state.x + 5.5F;
         float headY = currentY + 2.0F;
         float dotX = headX + headSize + 2.5F;
         float dotY = currentY + 4.5F;
         float nameX = dotX + 4.0F;
         float statusSize = 6.0F;
         float statusX = state.x + state.width - statusSize - 9.0F;
         float statusY = currentY + 3.0F;
         String visibleName = this.trimToWidth(row.name, TEXT_FONT, 6.0F, statusX - nameX - 6.0F);
         int leftDotColor = ColorUtil.rgba(128, 128, 128, Math.round(210.0F * rowAlpha));
         this.renderHead(row, headX, headY, headSize, rowAlpha);
         Render2D.rect(dotX, dotY, 3.2F, 3.2F, 3.2F, leftDotColor);
         Render2D.text(TEXT_FONT, visibleName, nameX, currentY + 1.5F, 6.0F, ColorUtil.multAlpha(TEXT_COLOR, rowAlpha));
         Render2D.rect(statusX, statusY, statusSize, statusSize, 2.0F, ColorUtil.multAlpha(row.dotColor, rowAlpha));
      }
   }

   private void renderHead(Staff.RowState row, float x, float y, float size, float alpha) {
      int color = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * alpha));
      String texture = row.skin != null && !row.skin.isBlank() ? row.skin : this.defaultSkin(row.name);
      boolean base = this.renderSkinPart(texture, x, y, size, 0.125F, 0.125F, 0.25F, 0.25F, color);
      boolean overlay = this.renderSkinPart(texture, x, y, size, 0.625F, 0.125F, 0.75F, 0.25F, color);
      if (!base && !overlay) {
         Render2D.image(texture, x, y, size, 2.0F, color);
      }
   }

   private boolean renderSkinPart(String texture, float x, float y, float size, float u0, float v0, float u1, float v1, int color) {
      if (texture != null && !texture.isBlank() && color >>> 24 != 0) {
         Render2D.imageUvNearest(texture, x, y, size, size, 2.0F, 1.0F, u0, v0, u1, v1, color);
         return true;
      } else {
         return false;
      }
   }

   private Staff.RowEntry row(String key, Staff.StaffEntry staff, float targetY) {
      for (Staff.RowEntry entry : this.rowEntries) {
         if (entry.key.equals(key)) {
            entry.name = staff.name;
            entry.status = staff.status;
            entry.skin = staff.skin;
            entry.dotColor = staff.dotColor;
            return entry;
         }
      }

      Staff.RowEntry entryx = new Staff.RowEntry(key, staff);
      entryx.alpha.set(0.0);
      entryx.y.set((double)(targetY + 4.0F));
      this.rowEntries.add(entryx);
      return entryx;
   }

   private List<Staff.StaffEntry> staffEntries() {
      this.cachedStaffEntries.clear();
      if (this.mc.world != null && this.mc.getNetworkHandler() != null) {
         this.tabPlayers.clear();

         for (PlayerListEntry info : this.mc.getNetworkHandler().getPlayerList()) {
            this.tabPlayers.put(StaffUtils.normalizeName(info.getProfile().name()).toLowerCase(), info);
         }

         this.worldPlayers.clear();

         for (PlayerEntity player : this.mc.world.getPlayers()) {
            this.worldPlayers.put(StaffUtils.normalizeName(player.getName().getString()).toLowerCase(), player);
         }

         for (String staff : StaffUtils.getStaffNames()) {
            String key = StaffUtils.normalizeName(staff).toLowerCase();
            if (!key.isEmpty()) {
               PlayerEntity player = this.worldPlayers.get(key);
               PlayerListEntry info = this.tabPlayers.get(key);
               if (player != null) {
                  this.cachedStaffEntries.add(new Staff.StaffEntry(staff, "Near", this.skin(player, info, staff), ColorUtil.rgba(255, 198, 76, 235)));
               } else if (info != null) {
                  this.cachedStaffEntries.add(new Staff.StaffEntry(staff, "Active", this.skin(null, info, staff), ColorUtil.rgba(85, 255, 140, 235)));
               } else {
                  this.cachedStaffEntries.add(new Staff.StaffEntry(staff, "Spec", this.defaultSkin(staff), ColorUtil.rgba(255, 92, 92, 235)));
               }
            }
         }

         return this.cachedStaffEntries;
      } else {
         return this.cachedStaffEntries;
      }
   }

   private String skin(PlayerEntity player, PlayerListEntry info, String name) {
      try {
         if (player instanceof AbstractClientPlayerEntity clientPlayer) {
            return clientPlayer.getSkin().body().texturePath().toString();
         }

         if (info != null) {
            return info.getSkinTextures().body().texturePath().toString();
         }
      } catch (RuntimeException var5) {
      }

      return this.defaultSkin(name);
   }

   private String defaultSkin(String name) {
      return ((name == null ? 0 : name.hashCode()) & 1) == 0
         ? "minecraft:textures/entity/player/wide/steve.png"
         : "minecraft:textures/entity/player/slim/alex.png";
   }

   private static final class RowEntry {
      private final String key;
      private final SmoothAnimation alpha = new SmoothAnimation();
      private final SmoothAnimation y = new SmoothAnimation();
      private String name;
      private String status;
      private String skin;
      private int dotColor;
      private boolean active;

      private RowEntry(String key, Staff.StaffEntry staff) {
         this.key = key;
         this.name = staff.name;
         this.status = staff.status;
         this.skin = staff.skin;
         this.dotColor = staff.dotColor;
      }
   }

   private static record RowState(String name, String status, String skin, int dotColor, float offset, float alpha) {
   }

   private static record StaffEntry(String name, String status, String skin, int dotColor) {
   }

   private static record StaffState(List<Staff.RowState> rows, float iconAlpha, float alpha, float x, float y, float width, float height) {
   }
}
