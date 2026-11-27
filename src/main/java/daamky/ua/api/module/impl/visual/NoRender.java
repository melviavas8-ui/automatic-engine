package daamky.ua.api.module.impl.visual;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.MultiModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

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
   public void onTick(Minecraft client) {
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
         : state.getBlock() instanceof VegetationBlock
            || state.getBlock() instanceof BushBlock
            || state.getBlock() instanceof GrowingPlantBodyBlock
            || state.getBlock() instanceof CropBlock
            || state.getBlock() instanceof SaplingBlock
            || state.getBlock() instanceof MushroomBlock
            || state.getBlock() instanceof SweetBerryBushBlock
            || state.getBlock() instanceof TallGrassBlock
            || state.getBlock() instanceof DoublePlantBlock
            || state.getBlock() instanceof FlowerBlock
            || state.getBlock() instanceof FlowerPotBlock
            || state.is(BlockTags.FLOWERS)
            || state.is(BlockTags.SAPLINGS);
   }

   private void reloadWorldRenderer() {
      if (mc.level != null && mc.levelRenderer != null) {
         mc.levelRenderer.allChanged();
      }
   }
}
