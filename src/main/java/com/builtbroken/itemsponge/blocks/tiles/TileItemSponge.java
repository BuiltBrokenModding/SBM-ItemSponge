package com.builtbroken.itemsponge.blocks.tiles;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import com.builtbroken.itemsponge.init.ModGlobals.NBT;
import com.builtbroken.itemsponge.init.ModNetworking;
import com.builtbroken.itemsponge.inventories.ItemSpongeItemHandler;
import com.builtbroken.itemsponge.utils.ItemSpongeUtils;

/**
 * @author p455w0rd
 *
 */
public class TileItemSponge extends TileEntity implements ITickable {

	private ItemSpongeItemHandler itemHandler;

	public TileItemSponge() {
		itemHandler = new ItemSpongeItemHandler() {
			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				ItemStack returnStack = super.extractItem(slot, amount, simulate);
				if (!simulate && !returnStack.isEmpty() && !TileItemSponge.this.getWorld().isRemote) {
					ModNetworking.sendTileSyncPacketToClient(TileItemSponge.this);
				}
				return returnStack;
			}

			@Override
			@Nonnull
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				ItemStack returnStack = super.insertItem(slot, stack, simulate);
				if (!simulate && returnStack.getCount() != stack.getCount() && !TileItemSponge.this.getWorld().isRemote) {
					ModNetworking.sendTileSyncPacketToClient(TileItemSponge.this);
				}
				return returnStack;
			}
		};
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
		}
		return super.getCapability(capability, facing);
	}

	public void setAbsorbedStack(ItemStack stack) {
		ItemStack absorbedStack = stack.copy();
		absorbedStack.setCount(1);
		itemHandler.setAbsorbedStack(absorbedStack);
	}

	// Gets just the absorbed ItemStack (the ItemStack that is set, not the inventory)
	public ItemStack getAbsorbedStack() {
		return itemHandler.getAbsorbedStack();
	}

	// Gets the absorbed ItemStack from inventory (actual count)
	public ItemStack getAbsorbedStackWithCount() {
		return getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).getStackInSlot(0);
	}

	public boolean hasAbsorbedStack() {
		return !getAbsorbedStack().isEmpty();
	}

	public boolean isItemValid(ItemStack stack) {
		if (hasAbsorbedStack()) {
			ItemStack comparableStack = stack.copy();
			comparableStack.setCount(1);
			return ItemStack.areItemStacksEqual(getAbsorbedStack(), comparableStack);
		}
		return false;
	}

	@Override
	public void update() {
		if (getWorld() != null) {
			if (isServerSide()) {
				ItemSpongeUtils.absorbEntityItems(this);
			}
		}
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	@Nullable
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(getPos(), 255, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		if (compound.hasKey(NBT.INVENTORY_KEY, Constants.NBT.TAG_COMPOUND)) {
			NBTTagCompound serializedInv = compound.getCompoundTag(NBT.INVENTORY_KEY);
			itemHandler.deserializeNBT(serializedInv);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		if (hasAbsorbedStack()) {
			compound.setTag(NBT.INVENTORY_KEY, itemHandler.serializeNBT());
		}
		return super.writeToNBT(compound);
	}

	public boolean isServerSide() {
		return getWorld() != null && !getWorld().isRemote;
	}

}