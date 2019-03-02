package com.builtbroken.itemsponge.inventories;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandlerModifiable;
import com.builtbroken.itemsponge.init.ModConfig.Options;
import com.builtbroken.itemsponge.init.ModGlobals.NBT;
import com.builtbroken.itemsponge.utils.ItemSpongeUtils;

/**
 * @author p455w0rd
 *
 */
public class ItemSpongeItemHandler implements IItemHandlerModifiable, INBTSerializable<NBTTagCompound> {

	int realStackSize = 0;
	ItemStack absorbedStack = ItemStack.EMPTY;

	public void setAbsorbedStack(ItemStack stack) {
		absorbedStack = stack.copy();
		absorbedStack.setCount(1);
	}

	public ItemStack getAbsorbedStack() {
		return absorbedStack;
	}

	@Override
	public int getSlots() {
		return 1;
	}

	@Override
	@Nonnull
	public ItemStack getStackInSlot(int slot) {
		ItemStack realStack = getAbsorbedStack().copy();
		if (slot == 0 && !realStack.isEmpty()) {
			realStack.setCount(realStackSize);
			return realStack;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		if (isItemValid(slot, stack)) {
			realStackSize = stack.getCount();
		}
	}

	@Override
	@Nonnull
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		if (stack.isEmpty() || !isItemValid(slot, stack)) {
			return stack;
		}
		if (realStackSize < Options.maxItemsPerSponge) {
			int availableSpace = Options.maxItemsPerSponge - realStackSize;
			if (stack.getCount() <= availableSpace) {
				if (!simulate) {
					realStackSize += stack.getCount();
				}
				return ItemStack.EMPTY;
			}
			int leftOver = stack.getCount() - availableSpace;
			stack.setCount(leftOver);
			if (!simulate) {
				realStackSize = Options.maxItemsPerSponge;
			}
		}
		return stack;
	}

	@Override
	@Nonnull
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (slot != 0 || amount == 0) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = getStackInSlot(slot);
		if (!stack.isEmpty() && amount <= realStackSize) {
			ItemStack returnStack = stack.copy();
			returnStack.setCount(amount);
			if (!simulate) {
				realStackSize -= amount;
			}
			return returnStack;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot) {
		return slot == 0 ? Options.maxItemsPerSponge : 0;
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		ItemStack newStack1 = stack.copy();
		ItemStack newStack2 = getAbsorbedStack().copy();
		newStack1.setCount(1);
		newStack2.setCount(1);
		return Options.maxItemsPerSponge > realStackSize && slot == 0 && ItemStack.areItemStacksEqual(newStack1, newStack2);
	}

	// We maintain vanilla NBT structure just in case another mod might be expecting such serialization
	@Override
	public NBTTagCompound serializeNBT() {
		return ItemSpongeUtils.createSerializedItemHandlerList(getAbsorbedStack(), realStackSize);
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		if (nbt.hasKey(NBT.ITEMS_KEY) && nbt.hasKey(NBT.REALSTACKSIZE_KEY, Constants.NBT.TAG_INT)) {
			setAbsorbedStack(new ItemStack(nbt.getTagList(NBT.ITEMS_KEY, Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0)));
			realStackSize = nbt.getInteger(NBT.REALSTACKSIZE_KEY);
		}
	}

}