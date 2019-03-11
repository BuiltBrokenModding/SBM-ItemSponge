package com.builtbroken.itemsponge.sponge.item;

import com.builtbroken.itemsponge.ConfigMain;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

/**
 * @author p455w0rd
 */
public class SpongeInventory extends ItemStackHandler
{
    private ItemStack filterStack = ItemStack.EMPTY;
    private int currentCount = 0;

    public SpongeInventory()
    {
        super(ConfigMain.SLOT_COUNT);
    }

    public void setAbsorbedStack(ItemStack stack)
    {
        //clone stack
        filterStack = stack.copy();
        filterStack.setCount(1);
    }

    public ItemStack getAbsorbedStack()
    {
        return filterStack;
    }

    public int getItemCount()
    {
        return currentCount;
    }

    public boolean isFull()
    {
        return currentCount >= getMaxItemCount();
    }

    public int getMaxItemCount()
    {
        return getSlots() * getSlotLimit(0);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return filterStack.isEmpty() ? 64 : filterStack.getMaxStackSize();
    }

    @Override
    protected void onLoad()
    {
        currentCount = 0;
        for (int i = 0; i < getSlots(); i++)
        {
            currentCount += getStackInSlot(i).getCount();
        }
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack)
    {
        if (stack.isEmpty() || isItemValid(slot, stack))
        {
            super.setStackInSlot(slot, stack);
        }
    }

    @Nonnull
    public ItemStack insertItem(@Nonnull ItemStack stack, boolean simulate)
    {
        ItemStack consumeStack = stack.copy();
        for (int i = 0; i < getSlots(); i++) //TODO optimize to use a list of non-full slot
        {
            consumeStack = insertItem(i, consumeStack, simulate);
            if (consumeStack.isEmpty())
            {
                return consumeStack;
            }
        }
        return consumeStack;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate)
    {
        final ItemStack pre = getStackInSlot(slot);
        final ItemStack post = isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;

        if (!simulate && ItemStack.areItemsEqual(pre, post))
        {
            currentCount -= pre.getCount();
            currentCount += post.getCount();
        }
        return post;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (!simulate)
        {
            final ItemStack pre = getStackInSlot(slot);
            final ItemStack post = super.extractItem(slot, amount, false);
            if (ItemStack.areItemsEqual(pre, post))
            {
                currentCount -= pre.getCount();
                currentCount += post.getCount();
            }
        }
        return super.extractItem(slot, amount, true);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack)
    {
        //Ignore all if no filter is set
        if (filterStack == null || filterStack.isEmpty())
        {
            return false;
        }
        //Ignore if input is empty, is sponge, or has a sub inventory //TODO see if we should ignore most NBT
        if (stack.isEmpty() || stack.getItem() instanceof ItemBlockItemSponge || stack.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null))
        {
            return false;
        }
        //Match to item, damage, and NBT
        return stack.isItemEqual(filterStack) && ItemStack.areItemStackTagsEqual(stack, filterStack);
    }
}