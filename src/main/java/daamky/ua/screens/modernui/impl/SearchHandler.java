package daamky.ua.screens.modernui.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.module.impl.misc.ClientSounds;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.render.animation.modernfx.Decelerate;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.utils.sounds.SoundManager;

public final class SearchHandler {
   private static final float TEXT_SIZE = 7.0F;
   private final Decelerate focusAnimation = MathUtils.createAnimation(180, Direction.BACKWARDS);
   private boolean active;
   private String text = "";
   private int cursor;
   private float cursorBlink;
   private float focus;
   private float textScroll;
   private float targetTextScroll;
   private long lastUpdateMs = System.currentTimeMillis();

   public SearchHandler() {
   }

   public void setSearchActive(boolean active) {
      this.active = active;
      this.cursor = Math.max(0, Math.min(this.cursor, this.text.length()));
   }

   public void clearSearch() {
      this.text = "";
      this.cursor = 0;
      this.textScroll = 0.0F;
      this.targetTextScroll = 0.0F;
   }

   public void updateAnimations() {
      long now = System.currentTimeMillis();
      float delta = Math.min((float)(now - this.lastUpdateMs) / 1000.0F, 0.1F);
      this.lastUpdateMs = now;
      this.focusAnimation.setDirection(this.active ? Direction.FORWARDS : Direction.BACKWARDS);
      this.focus = MathUtils.clamp(this.focusAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      if (this.active) {
         this.cursorBlink += delta * 2.0F;
         if (this.cursorBlink > 1.0F) {
            this.cursorBlink--;
         }
      } else {
         this.targetTextScroll = 0.0F;
      }

      this.textScroll = this.textScroll + (this.targetTextScroll - this.textScroll) * MathUtils.clamp(delta * 12.0F, 0.0F, 1.0F);
   }

   public boolean handleSearchChar(CharInput event) {
      if (this.active && event.isValidChar()) {
         this.insert(event.asString());
         return true;
      } else {
         return false;
      }
   }

   public boolean handleSearchKey(KeyInput event) {
      if (!this.active) {
         return false;
      } else {
         boolean control = (event.modifiers() & 2) != 0;
         if (control && event.key() == 86) {
            this.paste();
            return true;
         } else if (control && event.key() == 88) {
            this.clear();
            return true;
         } else {
            switch (event.key()) {
               case 256:
                  this.setSearchActive(false);
                  return true;
               case 257:
               case 335:
                  return true;
               case 259:
                  if (this.cursor > 0) {
                     this.text = this.text.substring(0, this.cursor - 1) + this.text.substring(this.cursor);
                     this.cursor--;
                     this.playTypingSound();
                  }

                  return true;
               case 261:
                  if (this.cursor < this.text.length()) {
                     this.text = this.text.substring(0, this.cursor) + this.text.substring(this.cursor + 1);
                     this.playTypingSound();
                  }

                  return true;
               case 262:
                  this.cursor = Math.min(this.text.length(), this.cursor + 1);
                  return true;
               case 263:
                  this.cursor = Math.max(0, this.cursor - 1);
                  return true;
               case 268:
                  this.cursor = 0;
                  return true;
               case 269:
                  this.cursor = this.text.length();
                  return true;
               default:
                  return false;
            }
         }
      }
   }

   public void updateSearchTextScroll(float visibleWidth, float textWidth) {
      if (this.active && !(textWidth <= visibleWidth)) {
         String beforeCursor = this.text.substring(0, Math.max(0, Math.min(this.cursor, this.text.length())));
         float cursorX = Render2D.textWidth(FontType.SEMIBOLD, beforeCursor, 7.0F);
         if (cursorX - this.targetTextScroll > visibleWidth - 2.0F) {
            this.targetTextScroll = cursorX - visibleWidth + 2.0F;
         } else if (cursorX - this.targetTextScroll < 0.0F) {
            this.targetTextScroll = cursorX;
         }

         this.targetTextScroll = Math.max(0.0F, Math.min(this.targetTextScroll, Math.max(0.0F, textWidth - visibleWidth)));
      } else {
         this.targetTextScroll = 0.0F;
      }
   }

   public boolean isSearchActive() {
      return this.active;
   }

   public String getSearchText() {
      return this.text;
   }

   public int getSearchCursorPosition() {
      return this.cursor;
   }

   public float getSearchCursorBlink() {
      return this.cursorBlink;
   }

   public float getSearchFocusAnimation() {
      return this.focus;
   }

   public float getSearchTextScrollOffset() {
      return this.textScroll;
   }

   private void insert(String value) {
      if (value != null && !value.isEmpty()) {
         String cleaned = value.replace("\n", "").replace("\r", "").replace("\t", "");
         if (!cleaned.isEmpty()) {
            this.text = this.text.substring(0, this.cursor) + cleaned + this.text.substring(this.cursor);
            this.cursor = this.cursor + cleaned.length();
            this.playTypingSound();
         }
      }
   }

   private void paste() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      if (minecraft != null && minecraft.getWindow() != null) {
         this.insert(GLFW.glfwGetClipboardString(minecraft.getWindow().getHandle()));
      }
   }

   private void clear() {
      if (!this.text.isEmpty()) {
         this.text = "";
         this.cursor = 0;
         this.textScroll = 0.0F;
         this.targetTextScroll = 0.0F;
         this.playTypingSound();
      }
   }

   private void playTypingSound() {
      if (ClientSounds.allowSearchTypingSound()) {
         SoundManager.playSoundDirect(SoundManager.SEARCH_TYPING, 1.0F, 1.0F);
      }
   }
}
