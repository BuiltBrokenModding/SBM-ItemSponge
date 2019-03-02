package com.builtbroken.itemsponge.init;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.CapabilityItemHandler;
import com.builtbroken.itemsponge.blocks.tiles.TileItemSponge;
import com.builtbroken.itemsponge.network.PacketStackSync;
import com.builtbroken.itemsponge.network.PacketTileSync;
import com.builtbroken.itemsponge.utils.ItemSpongeUtils;

/**
 * @author p455w0rd
 *
 */
public class ModNetworking {

	private static final SimpleNetworkWrapper INSTANCE = new SimpleNetworkWrapper(ModGlobals.MODID);
	private static int nextId = 0;

	public static SimpleNetworkWrapper getInstance() {
		return INSTANCE;
	}

	private static int getNextId() {
		int currentId = nextId;
		nextId++;
		return currentId;
	}

	public static void init() {
		getInstance().registerMessage(PacketTileSync.Handler.class, PacketTileSync.class, getNextId(), Side.CLIENT);
		getInstance().registerMessage(PacketStackSync.Handler.class, PacketStackSync.class, getNextId(), Side.CLIENT);
	}

	public static void sendTileSyncPacketToClient(TileItemSponge tile) {
		getInstance().sendToDimension(new PacketTileSync(tile.getPos(), tile.writeToNBT(tile.getUpdateTag())), tile.getWorld().provider.getDimension());
	}

	public static void sendStackSyncPacketToClient(EntityPlayerMP player, int slot, NBTTagCompound nbt) {
		ItemStack sponge = player.inventory.getStackInSlot(slot);
		if (ItemSpongeUtils.isSponge(sponge)) {
			if (sponge.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
				getInstance().sendTo(new PacketStackSync(nbt, slot), player);
			}
		}
	}
}
