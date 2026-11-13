package daamky.ua.api.module.impl.combat.aura.attack;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import daamky.ua.api.module.impl.combat.AuraModule;

public class Pressing {
   private static final float DEFAULT_ATTACK_STRENGTH = 0.82F;
   private static final float MACE_ATTACK_STRENGTH = 0.25F;
   private static final long DEFAULT_MODE_DELAY_MS = 500L;
   private static final long MACE_MODE_DELAY_MS = 150L;
   private final int[] funTimeTicks = new int[]{10, 11, 10, 12};
   private final int[] spookyTicks = new int[]{10, 11, 12, 13, 12};
   private final int[] defaultTicks = new int[]{10, 11};
   private long lastClickTime = System.currentTimeMillis();

   public Pressing() {
   }

   public boolean isCooldownComplete(boolean dynamicCooldown, float ticks) {
      MinecraftClient client = MinecraftClient.getInstance();
      boolean mace = client != null && client.player != null && this.isHoldingMace(client);
      return this.isCooldownComplete(dynamicCooldown, ticks, mace ? 0.25F : 0.82F);
   }

   public boolean isCooldownComplete(boolean dynamicCooldown, float ticks, float minimumCooldown) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.player != null) {
         boolean mace = this.isHoldingMace(client);
         float cooldownTicks = ticks;
         float itemCooldownTicks = Math.max(1.0F, client.player.getAttackCooldownProgressPerTick());
         if (dynamicCooldown) {
            float tps = Math.max(1.0F, AuraModule.activeTps());
            float tpsFactor = 20.0F / tps;
            cooldownTicks = ticks * tpsFactor;
            itemCooldownTicks *= tpsFactor;
         }

         float requiredStrength = mace ? 0.25F : Math.max(0.0F, Math.min(1.0F, minimumCooldown));
         long delay = Math.max(0L, (long)Math.round(Math.max(0.0F, itemCooldownTicks * requiredStrength - cooldownTicks) * 50.0F));
         boolean cooldownReady = client.player.getAttackCooldownProgress(cooldownTicks) > requiredStrength;
         return cooldownReady && this.lastClickPassed() >= delay && this.hasModeTicksElapsedSinceLastClick(mace);
      } else {
         return false;
      }
   }

   public boolean hasTicksElapsedSinceLastClick(int ticks) {
      float tps = Math.max(1.0F, AuraModule.activeTps());
      return (float)this.lastClickPassed() >= (float)((long)ticks * 50L) * (20.0F / tps);
   }

   public long lastClickPassed() {
      return System.currentTimeMillis() - this.lastClickTime;
   }

   public void recalculate() {
      this.lastClickTime = System.currentTimeMillis();
   }

   private boolean hasModeTicksElapsedSinceLastClick(boolean mace) {
      return this.lastClickPassed() >= (mace ? 150L : 500L);
   }

   private boolean isHoldingMace(MinecraftClient client) {
      ItemStack mainHand = client.player.getMainHandStack();
      return mainHand.getItem().getTranslationKey().toLowerCase().contains("mace");
   }
}
