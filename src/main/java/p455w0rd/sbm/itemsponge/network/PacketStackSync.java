package p455w0rd.sbm.itemsponge.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import p455w0rd.sbm.itemsponge.ItemSponge;
import p455w0rd.sbm.itemsponge.utils.ItemSpongeUtils;

/**
 * @author p455w0rd
 *
 */
public class PacketStackSync implements IMessage {

	public NBTTagCompound inv;
	public int slot;

	public PacketStackSync() {
	}

	public PacketStackSync(NBTTagCompound inv, int slot) {
		this.inv = inv;
		this.slot = slot;
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		inv = ByteBufUtils.readTag(buffer);
		slot = buffer.readInt();
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		ByteBufUtils.writeTag(buffer, inv);
		buffer.writeInt(slot);
	}

	public static class Handler implements IMessageHandler<PacketStackSync, IMessage> {

		@Override
		public IMessage onMessage(PacketStackSync message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
				EntityPlayer player = ItemSponge.PROXY.getPlayer();
				InventoryPlayer playerInv = player.inventory;
				ItemStack sponge = playerInv.getStackInSlot(message.slot);
				if (ItemSpongeUtils.isSponge(sponge)) {
					sponge = new ItemStack(message.inv);
				}
			});
			return null;
		}

	}

}