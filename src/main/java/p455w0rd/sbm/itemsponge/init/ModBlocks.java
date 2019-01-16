package p455w0rd.sbm.itemsponge.init;

import net.minecraft.block.Block;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import p455w0rd.sbm.itemsponge.blocks.BlockItemSponge;
import p455w0rd.sbm.itemsponge.blocks.tiles.TileItemSponge;
import p455w0rd.sbm.itemsponge.client.render.ItemSpongeTileRenderer;

/**
 * @author p455w0rd
 *
 */
public class ModBlocks {

	public static final Block ITEM_SPONGE_BLOCK = new BlockItemSponge();

	public static void registersTiles() {
		GameRegistry.registerTileEntity(TileItemSponge.class, ITEM_SPONGE_BLOCK.getRegistryName());
	}

	@SideOnly(Side.CLIENT)
	public static void registerTESRs() {
		ClientRegistry.bindTileEntitySpecialRenderer(TileItemSponge.class, new ItemSpongeTileRenderer());
	}

}
