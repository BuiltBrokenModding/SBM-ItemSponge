package com.builtbroken.itemsponge.sponge;

import com.builtbroken.itemsponge.ConfigMain;
import com.builtbroken.itemsponge.ItemSpongeMod;
import com.builtbroken.itemsponge.NbtConsts;
import com.builtbroken.itemsponge.sponge.block.TileEntityItemSponge;
import com.builtbroken.itemsponge.sponge.item.ItemBlockItemSponge;
import com.builtbroken.itemsponge.sponge.item.SpongeInventory;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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


    private static final List<SpongeInventory> temp_invs = new ArrayList(30);

    /**
     * Gets a list of valid Item Sponges with items set and adds applicable items in a players inventory to those Item Sponges
     */
    public static void handlePlayer(EntityPlayer player)
    {
        temp_invs.clear();

        //Find all sponges
        loop(player.inventory, (slot, stack) -> {
            SpongeInventory inventory = getCap(stack);
            if (inventory != null)
            {
                temp_invs.add(inventory);
            }
            return stack;
        });

        //Insert items, don't check if is valid as we do that in the inventory
        loop(player.inventory, (slot, stack) -> {
            for (SpongeInventory inventory : temp_invs)
            {
                stack = inventory.insertItem(stack, false);
                if (stack.isEmpty())
                {
                    return stack;
                }
            }
            return stack;
        });

        //Pickup items
        List<EntityItem> nearbyItems = getItemsForCollection(player.world, player.posX, player.posY + (player.height / 2), player.posZ);
        for(EntityItem entity : nearbyItems)
        {
            //Check if any of the sponges can take the item
            if(temp_invs.stream().allMatch(inv -> inv.insertItem(entity.getItem(), true).getCount() != entity.getItem().getCount()))
            {
                if (entity.getDistanceSq(player) <= 2d) //TODO config for pickup radius
                {
                    entity.setNoPickupDelay();
                }
                moveItemTowardPos(player, entity, getSpeed(0)); //TODO pass in item count
            }
        }


        //Update player
        player.inventoryContainer.detectAndSendChanges();
    }

    private static void loop(IInventory inventory, SlotConsumer consumer)
    {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++)
        {
            inventory.setInventorySlotContents(slot, consumer.apple(slot, inventory.getStackInSlot(slot)));
        }
    }

    @FunctionalInterface
    public static interface SlotConsumer
    {
        ItemStack apple(int slot, ItemStack contents);
    }

    /**
     * Adds applicable item near an {@link EntityItem} version of an Item Sponge and adds them to the EntityItem's internal inventory
     */
    public static void absorbEntityItems(EntityItem sponge)
    {
        final ItemStack spongeStack = sponge.getItem();
        final SpongeInventory inventory = getCap(spongeStack);
        if (inventory != null && !inventory.isFull())
        {
            //Loop all nearby entities
            for (EntityItem entity : getItemsForCollection(sponge))
            {
                //IF close enough pick up
                if (entity.getDistanceSq(sponge) <= 1d)
                {
                    //Insert into inventory
                    ItemStack itemStack = entity.getItem();
                    itemStack = inventory.insertItem(itemStack, false);

                    //Empty consume
                    if (itemStack.isEmpty())
                    {
                        entity.setDead();
                    }
                    //Update stack
                    else
                    {
                        entity.setItem(itemStack);
                    }
                }
                //If not close enough move
                else
                {
                    moveItemTowardPos(sponge, entity, getSpeed(inventory.getItemCount()));
                }
            }
        }
    }

    /**
     * Performs the update task on a TileEntity (searches a radius around the TileEntity for valid items to be added)
     */
    public static boolean absorbEntityItems(TileEntityItemSponge tile)
    {
        boolean shouldUpdate = false;
        final SpongeInventory spongeInventory = tile.getInventory();
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
                        moveItemTowardPos(tile, entity, getSpeed(spongeInventory.getItemCount()));
                    }
                }
                else
                {
                    moveItemTowardPos(tile, entity, getSpeed(spongeInventory.getItemCount()));
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
     * Get available and valid EntityItems near an EntityItem version (dropped in world) of an ItemSponge ItemStack
     */
    private static List<EntityItem> getItemsForCollection(EntityItem itemSponge)
    {
        if (itemSponge.isEntityAlive() && itemSponge.ticksExisted > 0)
        {
            return getItemsForCollection(itemSponge.getEntityWorld(), itemSponge.getPosition(), getAbsorbedItem(itemSponge.getItem()));
        }
        return EMPTY_ENTITY_LIST;
    }

    private static List<EntityItem> getItemsForCollection(World world, double x, double y, double z)
    {
        List<EntityItem> itemList = new ArrayList<EntityItem>();
        int radius = ConfigMain.MAX_DISTANCE;
        for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius)))
        {
            // Support Immersive Engineering/Demagnetize mod
            final NBTTagCompound nbt = item.getEntityData();
            if (!nbt.hasKey(NbtConsts.DEMAGNETIZE_KEY, Constants.NBT.TAG_BYTE) || !nbt.getBoolean(NbtConsts.DEMAGNETIZE_KEY))
            {
                itemList.add(item);
            }
        }
        return itemList;
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
        moveItemTowardPos(tile.getPos().getX() + 0.5, tile.getPos().getY() + 0.5, tile.getPos().getZ() + 0.5, item, speed);
    }

    public static void moveItemTowardPos(Entity entity, Entity item, double speed)
    {
        final Vec3d entityCenter = entity.getEntityBoundingBox().getCenter();
        moveItemTowardPos(entityCenter.x, entityCenter.y, entityCenter.z, item, speed);
    }

    /**
     * Movies items towards the target position
     *
     * @param x
     * @param y
     * @param z
     * @param item
     * @param speed
     */
    public static void moveItemTowardPos(double x, double y, double z, Entity item, double speed)
    {
        final Vec3d entityCenter = item.getEntityBoundingBox().getCenter();

        //Getting difference
        double deltaX = x - entityCenter.x;
        double deltaY = y - entityCenter.y;
        double deltaZ = z - entityCenter.z;

        //Distance
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        //Normalize
        deltaX = deltaX / distance;
        deltaY = deltaY / distance;
        deltaZ = deltaZ / distance;

        //Move
        item.motionX = deltaX * speed;
        item.motionY = deltaY * speed;
        item.motionZ = deltaZ * speed;
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
