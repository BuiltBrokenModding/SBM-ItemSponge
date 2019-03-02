package com.builtbroken.itemsponge.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import com.builtbroken.itemsponge.ItemSponge;
import com.builtbroken.itemsponge.blocks.tiles.TileItemSponge;

/**
 * @author p455w0rd
 *
 */
public class PacketTileSync implements IMessage {

	public BlockPos pos;
	public NBTTagCompound inv;

	public PacketTileSync() {
	}

	public PacketTileSync(BlockPos pos, NBTTagCompound inv) {
		this.inv = inv;
		this.pos = pos;
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		inv = ByteBufUtils.readTag(buffer);
		pos = readPos(buffer);
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		ByteBufUtils.writeTag(buffer, inv);
		writePos(pos, buffer);
	}

	private BlockPos readPos(ByteBuf buffer) {
		return new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
	}

	private void writePos(BlockPos pos, ByteBuf buffer) {
		buffer.writeInt(pos.getX());
		buffer.writeInt(pos.getY());
		buffer.writeInt(pos.getZ());
	}

	public static class Handler implements IMessageHandler<PacketTileSync, IMessage> {

		@Override
		public IMessage onMessage(PacketTileSync message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
				World world = ItemSponge.PROXY.getWorld();
				TileEntity tile = world.getTileEntity(message.pos);
				if (tile != null && tile instanceof TileItemSponge) {
					tile.readFromNBT(message.inv);
				}
			});
			return null;
		}

	}

}