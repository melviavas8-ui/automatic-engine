package daamky.ua.api.module.impl.visual;

import net.minecraft.block.AbstractPlantBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.BushBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.MushroomPlantBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.ShortPlantBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.BlockTags;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.MultiModeSetting;

public class NoRender extends Module {
   private static NoRender instance;
   private final MultiModeSetting elements = this.register(
      new MultiModeSetting(
         "Elements",
         "Render elements to hide.",
         new String[]{
            "Fire",
            "Lava",
            "Hit Particles",
            "Bad Effects",
            "Block Overlay",
            "Plants",
            "Sky Background",
            "Fog",
            "Darkness",
            "Damage",
            "Nausea",
            "Scoreboard",
            "Bossbar",
            "Wither Hearts",
            "Totem Animation"
         },
         "Fire",
         "Bad Effects",
         "Block Overlay",
         "Darkness",
         "Damage",
         "Nausea",
         "Wither Hearts",
         "Totem Animation"
      )
   );
   private boolean plantsReloadState;

   public NoRender() {
      super("No Render", "Hides selected visual effects.", ModuleCategory.VISUAL);
      instance = this;
   }

   public static NoRender getInstance() {
      return instance;
   }

   public static boolean isActive(String element) {
      return instance != null && instance.isEnabled() && instance.elements.isSelected(element);
   }

   @Override
   protected void onEnable() {
      this.plantsReloadState = this.shouldHidePlants();
      if (this.plantsReloadState) {
         this.reloadWorldRenderer();
      }
   }

   @Override
   protected void onDisable() {
      boolean hadPlantsHidden = this.plantsReloadState;
      this.plantsReloadState = false;
      if (hadPlantsHidden) {
         this.reloadWorldRenderer();
      }
   }

   @Override
   public void onTick(MinecraftClient client) {
      boolean hidePlants = this.shouldHidePlants();
      if (hidePlants != this.plantsReloadState) {
         this.plantsReloadState = hidePlants;
         this.reloadWorldRenderer();
      }
   }

   public boolean shouldHidePlants() {
      return this.isEnabled() && this.elements.isSelected("Plants");
   }

   public boolean shouldHideSkyBackground() {
      return this.isEnabled() && this.elements.isSelected("Sky Background");
   }

   public boolean shouldHideHitParticles() {
      return this.isEnabled() && this.elements.isSelected("Hit Particles");
   }

   public boolean isPlantState(BlockState state) {
      return state == null
         ? false
         : state.getBlock() instanceof PlantBlock
            || state.getBlock() instanceof BushBlock
            || state.getBlock() instanceof AbstractPlantBlock
            || state.getBlock() instanceof CropBlock
            || state.getBlock() instanceof SaplingBlock
            || state.getBlock() instanceof MushroomPlantBlock
            || state.getBlock() instanceof SweetBerryBushBlock
            || state.getBlock() instanceof ShortPlantBlock
            || state.getBlock() instanceof TallPlantBlock
            || state.getBlock() instanceof FlowerBlock
            || state.getBlock() instanceof FlowerPotBlock
            || state.isIn(BlockTags.FLOWERS)
            || state.isIn(BlockTags.SAPLINGS);
   }

   private void reloadWorldRenderer() {
      if (mc.world != null && mc.worldRenderer != null) {
         mc.worldRenderer.reload();
      }
   }
}
