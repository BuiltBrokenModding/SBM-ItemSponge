package com.builtbroken.itemsponge.sponge;

import com.builtbroken.itemsponge.ConfigMain;
import com.builtbroken.itemsponge.ItemSpongeMod;
import com.builtbroken.itemsponge.NbtConsts;
import com.builtbroken.itemsponge.lib.Vector3;
import com.builtbroken.itemsponge.sponge.block.TileEntityItemSponge;
import com.builtbroken.itemsponge.sponge.item.ItemBlockItemSponge;
import com.builtbroken.itemsponge.sponge.item.SpongeInventory;
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
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author p455w0rd
 */
public class ItemSpongeUtils
{

    private static Map<NBTTagCompound, ItemStack> STACK_CACHE = new HashMap<NBTTagCompound, ItemStack>(); //TODO remove
    private static final HashMap<NBTTagCompound, ItemStack> EMPTY_MAP = new HashMap<NBTTagCompound, ItemStack>(); //TODO remove
    private static final List<EntityItem> EMPTY_ENTITY_LIST = new ArrayList<EntityItem>() //TODO remove
    {
        @Override
        public boolean add(EntityItem item)
        {
            return false;
        }
    };

    /**
     * Gets a list of valid Item Sponges with items set and adds applicable items in a players inventory to those Item Sponges
     */
    public static void absorbInventoryItems(EntityPlayer player)
    {
        if (player instanceof EntityPlayerMP)
        {
            final InventoryPlayer inv = player.inventory;
            for (Map.Entry<Integer, ItemStack> sponge : getSponges(player, true).entrySet())
            {
                for (int i = 0; i < inv.getSizeInventory(); i++) //TODO map entire inventory in 1 go
                {
                    final ItemStack stackInSlot = inv.getStackInSlot(i);
                    if (isItemValid(sponge.getValue(), stackInSlot) && sponge.getValue().hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null))
                    {
                        SpongeInventory itemHandler = (SpongeInventory) sponge.getValue().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                        if (itemHandler != null && itemHandler.isItemValid(0, stackInSlot))
                        {
                            ItemStack leftOver = itemHandler.insertItem(0, stackInSlot.copy(), false);
                            inv.setInventorySlotContents(i, leftOver);


                            sponge.getValue().getTagCompound().setTag(NbtConsts.INVENTORY_KEY, itemHandler.serializeNBT()); //TODO WHY?


                            ItemSpongeMod.sendStackSyncPacketToClient((EntityPlayerMP) player, sponge.getKey(), sponge.getValue().serializeNBT()); //TODO remove, let the player sync using share NBT
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
        if (getAbsorbedItemCount(sponge.getItem()) >= ConfigMain.maxItemsPerSponge)
        {
            return;
        }
        int numToAdd = 0;
        for (EntityItem entity : getItemsForCollection(sponge))
        {

            if (entity.getDistanceSq(sponge) <= 1d)
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
        for (EntityItem entity : getItemsForCollection(player))
        {
            if (entity.getDistanceSq(player) <= 2d) //TODO config for pickup radius
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
        SpongeInventory spongeInventory = getCap(tile);
        if (!hasRedstoneSignal(tile) && spongeInventory != null && !spongeInventory.isFull())
        {
            for (EntityItem entity : getItemsForCollection(tile))
            {
                if (entity.getDistanceSq(tile.getPos()) <= 1d)
                {
                    ItemStack leftOverStack = spongeInventory.insertItem(entity.getItem(), false);
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
                tile.getWorld().playSound(null, tile.getPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.1F, 0.5F * ((tile.getWorld().rand.nextFloat() - tile.getWorld().rand.nextFloat()) * 0.7F + 2F));
            }
        }
        return shouldUpdate;
    }

    /**
     * Get available and valid EntityItems near an Item Sponge TileEntity
     */
    private static List<EntityItem> getItemsForCollection(TileEntityItemSponge tile)
    {
        if (tile.hasAbsorbedStack() && tile.getWorld() != null)
        {
            return getItemsForCollection(tile.getWorld(), tile.getPos(), tile.getAbsorbedStack());
        }
        return EMPTY_ENTITY_LIST;
    }

    /**
     * Get available and valid EntityItems near a player who has an Item Sponge ItemStack in their inventory
     */
    private static LinkedList<EntityItem> getItemsForCollection(EntityPlayer player)
    {
        final LinkedList<EntityItem> entityItemList = new LinkedList();

        //Slot -> sponge stack
        for (Map.Entry<Integer, ItemStack> spongeSlotCombo : ItemSpongeUtils.getSponges(player, false).entrySet())
        {
            final ItemStack spongeStack = spongeSlotCombo.getValue();
            final SpongeInventory spongeInventory = getCap(spongeStack);
            if (spongeInventory != null && !spongeInventory.isFull())
            {
                ItemStack absorbedStack = getAbsorbedItem(spongeSlotCombo.getValue());
                for (EntityItem entity : getItemsForCollection(player.getEntityWorld(), player.getPosition(), absorbedStack))
                {
                    if (isItemValid(spongeSlotCombo.getValue(), entity.getItem()))
                    {
                        entityItemList.add(entity);
                    }
                }
            }
        }
        return entityItemList;
    }

    /**
     * Get available and valid EntityItems near an EntityItem version (dropped in world) of an ItemSponge ItemStack
     */
    private static List<EntityItem> getItemsForCollection(EntityItem itemSponge)
    {
        if (itemSponge.isEntityAlive() && itemSponge.ticksExisted > 0)
        {
            return getItemsForCollection(itemSponge.getEntityWorld(), itemSponge.getPosition(), getAbsorbedItem(itemSponge));
        }
        return EMPTY_ENTITY_LIST;
    }

    /**
     * Generic method for getting absorbable items near a specified {@link BlockPos position}
     */
    private static List<EntityItem> getItemsForCollection(World world, BlockPos vec, ItemStack comparableStack)
    {
        List<EntityItem> itemList = new ArrayList<EntityItem>();
        int radius = ConfigMain.MAX_DISTANCE;
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
            int currentCount = getAbsorbedItemCount(absorbedStack);

            absorbedStack = new ItemStack(getSerializedSpongeStack(sponge.getItem(), getAbsorbedItem(absorbedStack), currentCount + numToAdd)); ///TODO WTF

            sponge.setItem(absorbedStack);
            sponge.getEntityWorld().updateEntityWithOptionalForce(sponge, true);
        }
    }

    /**
     * Gets the speed at which items should be moved toward an ItemSponge depending on how full it is
     */
    public static double getSpeed(int amountStored)
    {
        //double percent = ((double) ConfigMain.maxItemsPerSponge - (double) amountStored) / ConfigMain.maxItemsPerSponge;
        //return percent < 0.1d ? 0.1d : percent;
        return 1; //TODO implement
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
    @Deprecated
    public static ItemStack getAbsorbedItem(ItemStack sponge) //TODO move to capability
    {
        if (sponge.getItem() == ItemSpongeMod.item && sponge.hasTagCompound() && sponge.getTagCompound().hasKey(NbtConsts.INVENTORY_KEY, Constants.NBT.TAG_COMPOUND))
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
            final ItemStack spongeItemHeld = getAbsorbedItem(sponge);
            return ItemStack.areItemsEqual(spongeItemHeld, stack) && ItemStack.areItemStackTagsEqual(spongeItemHeld, stack);
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
            final ItemStack spongeItemHeld = getAbsorbedItem(sponge);
            return ItemStack.areItemsEqual(spongeItemHeld, stack) && ItemStack.areItemStackTagsEqual(spongeItemHeld, stack);
        }
        return false;
    }

    public static boolean isItemValid(EntityItem sponge, EntityItem stack)
    {
        return isItemValid(sponge, stack.getItem());
    }

    /**
     * Gets all sponge items currently in the player's inventory
     *
     * @param player              - player to use for an inventory
     * @param absorbedItemSetOnly - only returns sponges that have an item set
     */
    private static Map<Integer, ItemStack> getSponges(EntityPlayer player, boolean absorbedItemSetOnly)
    {
        Map<Integer, ItemStack> spongeMap = new HashMap<>(); //TODO recycle maybe? also might want to turn into list? Could even return slots only?
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            ItemStack currentStack = inv.getStackInSlot(i);
            if (isSponge(currentStack) && (!absorbedItemSetOnly || hasAbsorbedItem(currentStack)))
            {
                spongeMap.put(i, currentStack);
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
        for (ItemStack currentStack : ConfigMain.getItemBlackList())
        {
            if (tmpStack.isItemEqual(currentStack))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isFull(ItemStack stack)
    {
        SpongeInventory spongeInventory = getCap(stack);
        if (spongeInventory != null)
        {
            return spongeInventory.isFull();
        }
        return false;
    }

    public static SpongeInventory getCap(ICapabilityProvider provider)
    {
        if (provider.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null))
        {
            IItemHandler handler = provider.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler instanceof SpongeInventory)
            {
                return (SpongeInventory) handler;
            }
        }
        return null;
    }
}
