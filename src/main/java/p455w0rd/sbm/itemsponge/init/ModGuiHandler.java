package p455w0rd.sbm.itemsponge.init;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import p455w0rd.sbm.itemsponge.ItemSponge;
import p455w0rd.sbm.itemsponge.containers.ContainerItemSpongeItem;
import p455w0rd.sbm.itemsponge.containers.ContainerItemSpongeTile;

/**
 * @author p455w0rd
 *
 */
public class ModGuiHandler implements IGuiHandler {

	private static final ModGuiHandler INSTANCE = new ModGuiHandler();

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		switch (GuiType.values()[ID]) {
		case ITEMSPONGE_ITEM:
			return new ContainerItemSpongeItem();
		case ITEMSPONGE_TILE:
			return new ContainerItemSpongeTile();
		default:
			return null;
		}
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		switch (GuiType.values()[ID]) {
		case ITEMSPONGE_ITEM:

			break;
		case ITEMSPONGE_TILE:

			break;
		default:
			return null;
		}
		return null;
	}

	public static void register() {
		NetworkRegistry.INSTANCE.registerGuiHandler(ItemSponge.INSTANCE, INSTANCE);
	}

	public static void openGui(GuiType type, EntityPlayer playerIn, World worldIn, int x, int y, int z) {
		playerIn.openGui(ItemSponge.INSTANCE, type.ordinal(), worldIn, x, y, z);
	}

	public static enum GuiType {

			ITEMSPONGE_ITEM, ITEMSPONGE_TILE;

	}

}
