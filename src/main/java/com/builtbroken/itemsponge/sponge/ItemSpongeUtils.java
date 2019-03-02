package com.builtbroken.itemsponge.sponge;

import com.builtbroken.itemsponge.ItemSpongeMod;
import com.builtbroken.itemsponge.ModConfig.Options;
import com.builtbroken.itemsponge.NbtConsts;
import com.builtbroken.itemsponge.lib.Vector3;
import com.builtbroken.itemsponge.sponge.item.ItemBlockItemSponge;
import com.builtbroken.itemsponge.sponge.block.TileEntityItemSponge;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author p455w0rd
 */
public class ItemSpongeUtils
{

    private static Map<NBTTagCompound, ItemStack> STACK_CACHE = new HashMap<NBTTagCompound, ItemStack>();
    private static final HashMap<NBTTagCompound, ItemStack> EMPTY_MAP = new HashMap<NBTTagCompound, ItemStack>();
    private static final List<EntityItem> EMPTY_ENTITY_LIST = new ArrayList<EntityItem>();

    /**
     * Gets a list of valid Item Sponges with items set and adds applicable items in a players inventory to those Item Sponges
     */
    public static void absorbInventoryItems(EntityPlayer player)
    {
        if (player instanceof EntityPlayerMP)
        {
            InventoryPlayer inv = player.inventory;
            for (Map.Entry<Integer, ItemStack> sponge : getAllItemSpongesWithAbsorbableItem(player).entrySet())
            {
                for (int i = 0; i < inv.getSizeInventory(); i++)
                {
                    ItemStack currentStack = inv.getStackInSlot(i);
                    if (isItemValid(sponge.getValue(), currentStack) && sponge.getValue().hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null))
                    {
                        ItemSpongeItemHandler itemHandler = (ItemSpongeItemHandler) sponge.getValue().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                        if (itemHandler != null && itemHandler.isItemValid(0, currentStack))
                        {
                            ItemStack leftOver = itemHandler.insertItem(0, currentStack.copy(), false);
                            inv.setInventorySlotContents(i, leftOver);
                            sponge.getValue().getTagCompound().setTag(NbtConsts.INVENTORY_KEY, itemHandler.serializeNBT());
                            ItemSpongeMod.sendStackSyncPacketToClient((EntityPlayerMP) player, sponge.getKey(), sponge.getValue().serializeNBT());
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds applicable item near an {@link EntityItem} version of an Item Sponge and adds them to the EntityItem's internal inventory
     */
    public static void absorbEntityItems(EntityItem sponge)
    {
        if (getAbsorbedItemCount(sponge.getItem()) >= Options.maxItemsPerSponge)
        {
            return;
        }
        int numToAdd = 0;
        for (EntityItem entity : getAbsorbableEntityItems(sponge))
        {
            if (getDistance(Vector3.fromEntityCenter(entity), Vector3.fromEntity(sponge)) <= 1d)
            {
                numToAdd += entity.getItem().getCount();
                entity.setDead();
                continue;
            }
            moveItemTowardPos(Vector3.fromEntityCenter(sponge), entity, getSpeed(getAbsorbedItemCount(entity.getItem())));
        }
        if (numToAdd > 0)
        {
            addToEntityItem(sponge, numToAdd);
        }
    }

    /**
     * Performs the update task on a player (searches inventory for valid Item Sponges, then searches inventory for valid items to be added to those sponges)
     */
    public static void absorbEntityItems(EntityPlayer player)
    {
        for (EntityItem entity : getAbsorbableEntityItems(player))
        {
            if (getDistance(Vector3.fromEntityCenter(entity), Vector3.fromEntity(player)) <= 2d)
            {
                entity.setNoPickupDelay();
            }
            moveItemTowardPos(Vector3.fromEntityCenter(player), entity, getSpeed(getAbsorbedItemCount(entity.getItem())));
        }
    }

    /**
     * Performs the update task on a TileEntity (searches a radius around the TileEntity for valid items to be added)
     */
    public static boolean absorbEntityItems(TileEntityItemSponge tile)
    {
        boolean shouldUpdate = false;
        IItemHandler inventory = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (!hasRedstoneSignal(tile) && inventory.getStackInSlot(0).getCount() < Options.maxItemsPerSponge)
        {
            Vector3 tilePos = Vector3.fromTileEntityCenter(tile);
            for (EntityItem entity : getAbsorbableEntityItems(tile))
            {
                Vector3 itemPos = Vector3.fromEntityCenter(entity);
                if (getDistance(itemPos, tilePos) <= 1d)
                {
                    ItemStack leftOverStack = inventory.insertItem(0, entity.getItem(), false);
                    if (leftOverStack.isEmpty())
                    {
                        entity.setDead();
                        shouldUpdate = true;
                    }
                    else if (leftOverStack.getCount() != entity.getItem().getCount())
                    {
                        entity.setItem(leftOverStack);
                        shouldUpdate = true;
                    }
                    else
                    {
                        moveItemTowardPos(tile, entity, getSpeed(getAbsorbedItemCount(tile)));
                    }
                }
                else
                {
                    moveItemTowardPos(tile, entity, getSpeed(getAbsorbedItemCount(tile)));
                }
            }
            if (shouldUpdate)
            {
                tile.getWorld().playSound(null, tilePos.toBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.1F, 0.5F * ((tile.getWorld().rand.nextFloat() - tile.getWorld().rand.nextFloat()) * 0.7F + 2F));
            }
        }
        return shouldUpdate;
    }

    /**
     * Get available and valid EntityItems near an Item Sponge TileEntity
     */
    private static List<EntityItem> getAbsorbableEntityItems(TileEntityItemSponge tile)
    {
        if (tile.hasAbsorbedStack() && tile.getWorld() != null)
        {
            return getAbsorbableEntityItems(tile.getWorld(), Vector3.fromTileEntity(tile).toBlockPos(), tile.getAbsorbedStack());
        }
        return EMPTY_ENTITY_LIST;
    }

    /**
     * Get available and valid EntityItems near a player who has an Item Sponge ItemStack in their inventory
     */
    private static List<EntityItem> getAbsorbableEntityItems(EntityPlayer player)
    {
        List<EntityItem> entityItemList = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> sponge : getAllItemSponges(player).entrySet())
        {
            if (sponge.getValue().hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null))
            {
                if (sponge.getValue().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).getStackInSlot(0).getCount() < Options.maxItemsPerSponge)
                {
                    ItemStack absorbedStack = getAbsorbedItem(sponge.getValue());
                    for (EntityItem entity : getAbsorbableEntityItems(player.getEntityWorld(), player.getPosition(), absorbedStack))
                    {
                        if (isItemValid(sponge.getValue(), entity.getItem()))
                        {
                            entityItemList.add(entity);
                        }
                    }
                }
            }
        }
        return entityItemList;
    }

    /**
     * Get available and valid EntityItems near an EntityItem version (dropped in world) of an ItemSponge ItemStack
     */
    private static List<EntityItem> getAbsorbableEntityItems(EntityItem itemSponge)
    {
        if (itemSponge.isEntityAlive() && itemSponge.ticksExisted > 0)
        {
            return getAbsorbableEntityItems(itemSponge.getEntityWorld(), itemSponge.getPosition(), getAbsorbedItem(itemSponge));
        }
        return EMPTY_ENTITY_LIST;
    }

    /**
     * Generic method for getting absorbable items near a specified {@link BlockPos position}
     */
    private static List<EntityItem> getAbsorbableEntityItems(World world, BlockPos vec, ItemStack comparableStack)
    {
        List<EntityItem> itemList = new ArrayList<EntityItem>();
        int radius = Options.maxDistance;
        for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(vec.getX() - radius, vec.getY() - radius, vec.getZ() - radius, vec.getX() + radius, vec.getY() + radius, vec.getZ() + radius)))
        {

            // Support Immersive Engineering/Demagnetize mod
            NBTTagCompound nbt = item.getEntityData();
            if (nbt.hasKey(NbtConsts.DEMAGNETIZE_KEY, Constants.NBT.TAG_BYTE) && nbt.getBoolean(NbtConsts.DEMAGNETIZE_KEY))
            {
                continue;
            }
            ItemStack compStack1 = comparableStack.copy();
            compStack1.setCount(1);
            ItemStack compStack2 = item.getItem().copy();
            compStack2.setCount(1);
            if (ItemStack.areItemStacksEqual(compStack1, compStack2))
            {
                itemList.add(item);
            }
        }
        return itemList;
    }

    /**
     * Creates a new Item Sponge {@link ItemStack} with updated absorbed item stack count via a serialized ItemHandler and updates the EntityItem as necessary
     */
    private static void addToEntityItem(EntityItem sponge, int numToAdd)
    {
        if (sponge.getEntityWorld() != null)
        {
            ItemStack absorbedStack = sponge.getItem().copy();
            int currentCount = getAbsorbedItemCount(sponge.getItem());
            absorbedStack = new ItemStack(getSerializedSpongeStack(sponge.getItem(), getAbsorbedItem(sponge.getItem()), currentCount + numToAdd));
            sponge.setItem(absorbedStack);
            sponge.getEntityWorld().updateEntityWithOptionalForce(sponge, true);
        }
    }

    /**
     * Gets the speed at which items should be moved toward an ItemSponge depending on how full it is
     */
    public static double getSpeed(int amountStored)
    {
        double percent = ((double) Options.maxItemsPerSponge - (double) amountStored) / Options.maxItemsPerSponge;
        return percent < 0.1d ? 0.1d : percent;
    }

    /**
     * Checks whether or not the {@link TileEntityItemSponge} has a redstoner signal
     */
    public static boolean hasRedstoneSignal(TileEntityItemSponge tile)
    {
        if (tile.getWorld() != null)
        {
            for (EnumFacing side : EnumFacing.VALUES)
            {
                if (tile.getWorld().isSidePowered(tile.getPos(), side))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the distance between 2 positions
     */
    private static double getDistance(Vector3 fromPos, Vector3 toPos)
    {
        return Math.sqrt(Math.pow(fromPos.x - toPos.x, 2) + Math.pow(fromPos.y - toPos.y, 2) + Math.pow(fromPos.z - toPos.z, 2));
    }

    private static void moveItemTowardPos(TileEntity tile, EntityItem item, double speed)
    {
        moveItemTowardPos(Vector3.fromTileEntityCenter(tile), item, speed);
    }

    /**
     * Moves the {@link net.minecraft.entity.item.EntityItem EntityItem} toward the given {@link Vector3 Vector}<br>
     * Used/Adapted from Botania
     */
    public static void moveItemTowardPos(Vector3 originalPosVector, EntityItem item, double speed)
    {
        Vector3 entityVector = Vector3.fromEntityCenter(item);
        Vector3 finalVector = originalPosVector.subtract(entityVector);

        if (finalVector.mag() > 1)
        {
            finalVector = finalVector.normalize();
        }

        item.motionX = finalVector.x * speed;
        item.motionY = finalVector.y * 0.45;
        item.motionZ = finalVector.z * speed;
    }

    /**
     * Sets {@link net.minecraft.tileentity.TileEntity TileEntity} NBT when the {@link net.minecraft.item.Item Item} version of the Item Sponge ({@link ItemBlockItemSponge ItemBlockItemSponge.class})
     * is placed into the world as a {@link Block}
     */
    public static boolean setTileNBT(World worldIn, @Nullable EntityPlayer player, BlockPos pos, ItemStack stack)
    {
        MinecraftServer minecraftserver = worldIn.getMinecraftServer();
        if (minecraftserver == null)
        {
            return false;
        }
        else
        {
            if (hasAbsorbedItem(stack))
            {
                TileEntity tileentity = worldIn.getTileEntity(pos);
                if (tileentity != null)
                {
                    if (!worldIn.isRemote && tileentity.onlyOpsCanSetNbt() && (player == null || !player.canUseCommandBlock()))
                    {
                        return false;
                    }
                    NBTTagCompound nbt = stack.getTagCompound();
                    nbt.setInteger("x", pos.getX());
                    nbt.setInteger("y", pos.getY());
                    nbt.setInteger("z", pos.getZ());
                    tileentity.readFromNBT(nbt);
                    tileentity.markDirty();
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Sets the {@link ItemStack absorbedItem} on the supplied {@link ItemStack sponge}<br>
     * Also caches the the instance of this object where the sponge only has a single absorbed item for recipe efficiency<br>
     * Will return a blank sponge stack if the supplied {@code absorbedItem} is {@link ItemStack#EMPTY ItemStack.EMPTY} (use for the reset recipe)
     */
    public static ItemStack setAbsorbedItem(@Nonnull ItemStack sponge, ItemStack absorbedItem)
    {
        if (sponge.isEmpty() || sponge.getItem() != ItemSpongeMod.item)
        {
            return ItemStack.EMPTY;
        }
        if (!sponge.hasTagCompound())
        {
            sponge.setTagCompound(new NBTTagCompound());
        }
        if (absorbedItem.getCount() > 1)
        {
            absorbedItem.setCount(1);
        }
        if (absorbedItem.isEmpty())
        {
            sponge = new ItemStack(ItemSpongeMod.item);
        }
        else
        {
            sponge = getCachedStack(getSerializedSpongeStack(sponge, absorbedItem, 1));
        }
        return sponge;
    }

    /**
     * Gets the absorbed item set on the Item Sponge {@link EntityItem}
     */
    public static ItemStack getAbsorbedItem(EntityItem spongeEntity)
    {
        return isSponge(spongeEntity) ? getAbsorbedItem(spongeEntity.getItem()) : ItemStack.EMPTY;
    }

    /**
     * Gets the absorbed item set on the Item Sponge {@link ItemStack}
     */
    public static ItemStack getAbsorbedItem(ItemStack sponge)
    {
        if (!sponge.isEmpty() && sponge.getItem() == ItemSpongeMod.item && sponge.hasTagCompound() && sponge.getTagCompound().hasKey(NbtConsts.INVENTORY_KEY, Constants.NBT.TAG_COMPOUND))
        {
            return getCachedStack(sponge.getTagCompound().getCompoundTag(NbtConsts.INVENTORY_KEY).getTagList(NbtConsts.ITEMS_KEY, Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0));
        }
        return ItemStack.EMPTY;
    }

    /**
     * Generates a {@link NBTTagCompound NBT} serialized {@link ItemStack} with inventory ready for deserialization by a {@link TileEntity}/{@link IItemHandler}
     */
    public static NBTTagCompound getSerializedSpongeStack(@Nonnull ItemStack sponge, @Nonnull ItemStack absorbedStack, int stackSize)
    {
        if (!sponge.hasTagCompound())
        {
            sponge.setTagCompound(new NBTTagCompound());
        }
        sponge.getTagCompound().setTag(NbtConsts.INVENTORY_KEY, createSerializedItemHandlerList(absorbedStack, stackSize));
        return sponge.writeToNBT(new NBTTagCompound());
    }

    /**
     * Generates a {@link NBTTagCompound NBT} serialized {@link IItemHandler} list from an {@link ItemStack} ready for deserialization by a {@link TileEntity}/{@link IItemHandler}
     */
    public static NBTTagCompound getSerializedItemHandlerFromSponge(@Nonnull ItemStack sponge)
    {
        if (sponge.hasTagCompound() && sponge.getTagCompound().hasKey(NbtConsts.INVENTORY_KEY, Constants.NBT.TAG_COMPOUND))
        {
            return sponge.getTagCompound().getCompoundTag(NbtConsts.INVENTORY_KEY);
        }
        return null;
    }

    /**
     * Creates a serialized {@link NBTTagCompound NBT} Item List valid for deserialization by an {@link IItemHandler} which implements {@link net.minecraftforge.common.util.INBTSerializable INBTSerializable}
     */
    public static NBTTagCompound createSerializedItemHandlerList(@Nonnull ItemStack absorbedStack, int stackSize)
    {
        NBTTagList nbtTagList = new NBTTagList();
        NBTTagCompound absorbedItemTag = new NBTTagCompound();
        absorbedItemTag.setInteger(NbtConsts.SLOT_KEY, 0);
        absorbedStack.setCount(1);
        absorbedStack.writeToNBT(absorbedItemTag);
        nbtTagList.appendTag(absorbedItemTag);
        NBTTagCompound invNBT = new NBTTagCompound();
        invNBT.setTag(NbtConsts.ITEMS_KEY, nbtTagList);
        invNBT.setInteger(NbtConsts.SIZE_KEY, 1);
        invNBT.setInteger(NbtConsts.REALSTACKSIZE_KEY, stackSize);
        return invNBT;
    }

    /**
     * Checks whether or not the Item Sponge {@link ItemStack} has an {@code absorbedItem} set
     */
    public static boolean hasAbsorbedItem(ItemStack sponge)
    {
        return isSponge(sponge) && !getAbsorbedItem(sponge).isEmpty();
    }

    /**
     * Checks whether or not the Item Sponge {@link EntityItem} has an {@code absorbedItem} set
     */
    public static boolean hasAbsorbedItem(EntityItem itemSpongeEntity)
    {
        return hasAbsorbedItem(itemSpongeEntity.getItem());
    }

    /**
     * Gets the amount of the {@code absorbedItem} contained on the Item Sponge {@link ItemStack}
     */
    public static int getAbsorbedItemCount(ItemStack sponge)
    {
        if (hasAbsorbedItem(sponge))
        {
            return getAbsorbedItemCount(sponge.getTagCompound());
        }
        return 0;
    }

    /**
     * Gets the amount of the {@code absorbedItem} contained on the Item Sponge {@link TileEntity}
     */
    private static int getAbsorbedItemCount(TileEntityItemSponge tile)
    {
        if (tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null))
        {
            return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).getStackInSlot(0).getCount();
        }
        return 0;
    }

    /**
     * Gets the amount of the {@code absorbedItem} contained on the Item Sponge {@link NBTTagCompound NBT}
     */
    private static int getAbsorbedItemCount(NBTTagCompound nbt)
    {
        if (nbt.hasKey(NbtConsts.INVENTORY_KEY, Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound invNBT = nbt.getCompoundTag(NbtConsts.INVENTORY_KEY);
            if (invNBT.hasKey(NbtConsts.REALSTACKSIZE_KEY, Constants.NBT.TAG_INT))
            {
                return invNBT.getInteger(NbtConsts.REALSTACKSIZE_KEY);
            }
        }
        return 0;
    }

    /**
     * Checks whether or not the supplied Item Sponge {@link ItemStack} has reached capacity
     */
    public static boolean isFull(ItemStack sponge)
    {
        return hasAbsorbedItem(sponge) && isFull(sponge.getTagCompound());
    }

    private static boolean isFull(NBTTagCompound nbt)
    {
        return getAbsorbedItemCount(nbt) == Options.maxItemsPerSponge;
    }

    // We cache up to 128 values...RAM is precious
    public static ItemStack getCachedStack(NBTTagCompound tag)
    {
        // Limit the cache to save RAM
        if (STACK_CACHE.keySet().size() >= 128)
        {
            STACK_CACHE = EMPTY_MAP;
        }
        for (NBTTagCompound stackNBT : STACK_CACHE.keySet())
        {
            if (stackNBT == tag)
            {
                return STACK_CACHE.get(stackNBT);
            }
        }
        STACK_CACHE.put(tag, new ItemStack(tag));
        return STACK_CACHE.get(tag);
    }

    /**
     * Checks whether or not the supplied {@link ItemStack} is an Item Sponge
     */
    public static boolean isSponge(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() == ItemSpongeMod.item;
    }

    /**
     * Checks whether or not the supplied {@link Entity} is an Item Sponge {@link EntityItem}
     */
    public static boolean isSponge(Entity entity)
    {
        return entity instanceof EntityItem && isSponge(((EntityItem) entity).getItem());
    }

    /**
     * Checks whether or not that supplied {@link ItemStack stack} is able to be absorbed by the supplied {@link ItemStack sponge}
     */
    public static boolean isItemValid(ItemStack sponge, ItemStack stack)
    {
        if (hasAbsorbedItem(sponge))
        {
            Pair<ItemStack, ItemStack> comparableStacks = getComparable(getAbsorbedItem(sponge), stack);
            return ItemStack.areItemStacksEqual(comparableStacks.getLeft(), comparableStacks.getRight());
        }
        return false;
    }

    /**
     * Checks whether or not the supplied {@link ItemStack} matches the {@code absorbable item} set on the Item Sponge
     */
    public static boolean isItemValid(EntityItem sponge, ItemStack stack)
    {
        if (hasAbsorbedItem(sponge))
        {
            Pair<ItemStack, ItemStack> comparableStacks = getComparable(getAbsorbedItem(sponge), stack);
            return ItemStack.areItemStacksEqual(comparableStacks.getLeft(), comparableStacks.getRight());
        }
        return false;
    }

    public static boolean isItemValid(EntityItem sponge, EntityItem stack)
    {
        return isItemValid(sponge, stack.getItem());
    }

    private static final Pair<ItemStack, ItemStack> getComparable(ItemStack stack1, ItemStack stack2)
    {
        ItemStack newStack1 = stack1.copy();
        ItemStack newStack2 = stack2.copy();
        newStack1.setCount(1);
        newStack2.setCount(1);
        return Pair.of(newStack1, newStack2);
    }

    /**
     * Gets all Item Sponges in a {@link EntityPlayer player}'s {@link InventoryPlayer inventory}
     * Also stores the slot number for each returned sponge
     */
    private static Map<Integer, ItemStack> getAllItemSponges(EntityPlayer player)
    {
        Map<Integer, ItemStack> spongeMap = new HashMap<>();
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            ItemStack currentStack = inv.getStackInSlot(i);
            if (isSponge(currentStack))
            {
                spongeMap.put(i, currentStack);
            }
        }
        return spongeMap;
    }

    /**
     * Gets all Item Sponges in a {@link EntityPlayer player}'s {@link InventoryPlayer inventory} which have an {@code absorbedItem} set
     * Also stores the slot number for each returned sponge
     */
    public static Map<Integer, ItemStack> getAllItemSpongesWithAbsorbableItem(EntityPlayer player)
    {
        Map<Integer, ItemStack> spongeMap = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> currentStack : getAllItemSponges(player).entrySet())
        {
            if (hasAbsorbedItem(currentStack.getValue()))
            {
                spongeMap.put(currentStack.getKey(), currentStack.getValue());
            }
        }
        return spongeMap;
    }

    /**
     * Checks whether or not the stack is blacklisted in the mod configs
     */
    public static boolean isItemBlacklisted(ItemStack stack)
    {
        ItemStack tmpStack = stack.copy();
        tmpStack.setCount(1);
        for (ItemStack currentStack : Options.getItemBlackList())
        {
            if (tmpStack.isItemEqual(currentStack))
            {
                return true;
            }
        }
        return false;
    }

    public static List<ItemStack> getAbsorbedItems(ItemStack sponge)
    {
        List<ItemStack> stackList = new ArrayList<>();
        if (hasAbsorbedItem(sponge))
        {
            ItemStack absorbedStack = getAbsorbedItem(sponge).copy();
            int totalAbsorbedItems = getAbsorbedItemCount(sponge);
            int maxStackSize = absorbedStack.getMaxStackSize();
            if (totalAbsorbedItems <= maxStackSize)
            {
                absorbedStack.setCount(totalAbsorbedItems);
                stackList.add(absorbedStack);
            }
            else
            {
                int fullStacks = totalAbsorbedItems / maxStackSize;
                int remainder = totalAbsorbedItems % maxStackSize;
                for (int i = 0; i < fullStacks; i++)
                {
                    ItemStack fullStack = absorbedStack.copy();
                    fullStack.setCount(maxStackSize);
                    stackList.add(fullStack);
                }
                if (remainder > 0)
                {
                    ItemStack remainderStack = absorbedStack.copy();
                    remainderStack.setCount(remainder);
                    stackList.add(remainderStack);
                }
            }
        }
        return stackList;
    }

}
