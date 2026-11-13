package daamky.ua.utils.world;

import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BrushableBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.registry.Registries;

/** Which blocks BlockESP looks for, their names (settings) and colors (RGB). */
public final class BlockEspTypes {
   public static final int COUNT = 22;
   public static final String[] NAMES = new String[]{
      "Chest", "Trapped Chest", "Ender Chest", "Spawner", "Barrel", "Hopper", "Dispenser", "Dropper", "Furnace", "Shulker Box", "Decorated Pot",
      "Suspicious Sand/Gravel", "Coal Ore", "Iron Ore", "Gold Ore", "Copper Ore", "Lapis Ore", "Redstone Ore", "Diamond Ore", "Emerald Ore",
      "Quartz Ore", "Ancient Debris"
   };
   public static final int[] COLORS = new int[]{
      0xFFC254, 0x8F6D3E, 0x9931EE, 0xFFFFFF, 0xFAE13E, 0x3E89FA, 0x1B40FA, 0x0017FF, 0x737373, 0xF67B7B, 0xB97A57, 0xE3CB99,
      2368548, 14200728, 16766720, 13137226, 2647255, 14818075, 5629672, 2545261, 15787216, 9128501
   };

   private BlockEspTypes() {
   }

   /** Lazy holder: the registry is only touched when the first scan happens. */
   private static final class Holder {
      static final Reference2ByteOpenHashMap<Block> MAP = build();
      static final Predicate<BlockState> PREDICATE = state -> MAP.getByte(state.getBlock()) >= 0;

      private static Reference2ByteOpenHashMap<Block> build() {
         Reference2ByteOpenHashMap<Block> map = new Reference2ByteOpenHashMap<>();
         map.defaultReturnValue((byte)-1);
         map.put(Blocks.CHEST, (byte)0);
         map.put(Blocks.TRAPPED_CHEST, (byte)1);
         map.put(Blocks.ENDER_CHEST, (byte)2);
         map.put(Blocks.SPAWNER, (byte)3);
         map.put(Blocks.BARREL, (byte)4);
         map.put(Blocks.HOPPER, (byte)5);
         map.put(Blocks.DISPENSER, (byte)6);
         map.put(Blocks.DROPPER, (byte)7);
         map.put(Blocks.FURNACE, (byte)8);
         map.put(Blocks.DECORATED_POT, (byte)10);
         for (Block block : Registries.BLOCK) {
            if (block instanceof ShulkerBoxBlock) {
               map.put(block, (byte)9);
            } else if (block instanceof BrushableBlock) {
               map.put(block, (byte)11);
            }
         }

         map.put(Blocks.COAL_ORE, (byte)12);
         map.put(Blocks.DEEPSLATE_COAL_ORE, (byte)12);
         map.put(Blocks.IRON_ORE, (byte)13);
         map.put(Blocks.DEEPSLATE_IRON_ORE, (byte)13);
         map.put(Blocks.GOLD_ORE, (byte)14);
         map.put(Blocks.DEEPSLATE_GOLD_ORE, (byte)14);
         map.put(Blocks.NETHER_GOLD_ORE, (byte)14);
         map.put(Blocks.COPPER_ORE, (byte)15);
         map.put(Blocks.DEEPSLATE_COPPER_ORE, (byte)15);
         map.put(Blocks.LAPIS_ORE, (byte)16);
         map.put(Blocks.DEEPSLATE_LAPIS_ORE, (byte)16);
         map.put(Blocks.REDSTONE_ORE, (byte)17);
         map.put(Blocks.DEEPSLATE_REDSTONE_ORE, (byte)17);
         map.put(Blocks.DIAMOND_ORE, (byte)18);
         map.put(Blocks.DEEPSLATE_DIAMOND_ORE, (byte)18);
         map.put(Blocks.EMERALD_ORE, (byte)19);
         map.put(Blocks.DEEPSLATE_EMERALD_ORE, (byte)19);
         map.put(Blocks.NETHER_QUARTZ_ORE, (byte)20);
         map.put(Blocks.ANCIENT_DEBRIS, (byte)21);
         return map;
      }
   }

   public static int typeOf(BlockState state) {
      return Holder.MAP.getByte(state.getBlock());
   }

   public static Predicate<BlockState> predicate() {
      return Holder.PREDICATE;
   }
}
