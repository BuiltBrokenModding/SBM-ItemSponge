package p455w0rd.sbm.itemsponge.init;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * @author p455w0rd
 *
 */
public class ModConfig extends Configuration {

	private static final ModConfig INSTANCE = new ModConfig(new File(ModGlobals.CONFIG_FILE));

	private ModConfig(File file) {
		super(file);
	}

	public static ModConfig getInstance() {
		return INSTANCE;
	}

	public void init() {
		load();
		sync();
	}

	public void sync() {
		Options.maxDistance = getInstance().getInt("MaxDistance", CATEGORY_GENERAL, 8, 1, 16, "Maximum distance (in blocks) an Item Sponge will search for absorbable items.");
		Options.maxItemsPerSponge = getInstance().getInt("MaxAmountPerSponge", CATEGORY_GENERAL, 2048, 16, Integer.MAX_VALUE, "Maximum number of items that can be stored in an Item Sponge");
		Options.itemBlacklistArray = getInstance().getStringList("ItemBlacklist", CATEGORY_GENERAL, new String[] {
				"appliedenergistics2:item.ItemCrystalSeed"
		}, "List of items not allowed to be crafted with the Item Sponge");
		if (getInstance().hasChanged()) {
			getInstance().save();
		}
	}

	public static class Options {

		public static int maxDistance = 8;
		public static int maxItemsPerSponge = Integer.MAX_VALUE;
		public static boolean getWeakerAsItFills = false;
		protected static String[] itemBlacklistArray = new String[0];
		private static List<ItemStack> itemList = new ArrayList<ItemStack>();

		public static List<ItemStack> getItemBlackList() {
			if (itemList.isEmpty()) {
				for (String itemString : itemBlacklistArray) {
					String[] components = itemString.split(":");
					int damage = 0;
					Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(components[0] + ":" + components[1]));
					if (components.length == 3) {
						damage = Integer.parseInt(components[2]);
					}
					if (item != null) {
						itemList.add(new ItemStack(item, 1, damage));
					}
				}
			}
			return itemList;
		}

	}

}
