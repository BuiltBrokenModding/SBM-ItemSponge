package com.builtbroken.itemsponge.sponge;

import com.builtbroken.itemsponge.ItemSpongeMod;
import com.builtbroken.itemsponge.sponge.item.SpongeInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

/**
 * Common events handler
 *
 * @author p455w0rd
 */
@EventBusSubscriber(modid = ItemSpongeMod.MODID)
public class EventHandler
{
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.player != null)
        {
            ItemSpongeUtils.handlePlayer(event.player);
        }
    }

    @SubscribeEvent
    public static void onWorldServerTick(TickEvent.WorldTickEvent event)
    {
        if (event.phase == Phase.START)
        {
            for (Entity entity : event.world.loadedEntityList)
            {
                if (entity instanceof EntityItem && entity.isEntityAlive() && entity.ticksExisted > 0 && ItemSpongeUtils.isSponge(entity))
                {
                    ItemSpongeUtils.absorbEntityItems((EntityItem) entity);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(ItemCraftedEvent event)
    {
        final World world = event.player.getEntityWorld();
        ItemStack result = event.crafting;
        if (!ItemSpongeUtils.hasAbsorbedItem(result))
        {
            int numCraftingItems = 0;
            IInventory inv = event.craftMatrix;
            ItemStack spongeStack = ItemStack.EMPTY;
            for (int i = 0; i < inv.getSizeInventory(); i++)
            {
                ItemStack currentStack = inv.getStackInSlot(i);
                if (!currentStack.isEmpty())
                {
                    numCraftingItems++;
                    if (ItemSpongeUtils.isSponge(currentStack) && ItemSpongeUtils.hasAbsorbedItem(currentStack))
                    {
                        spongeStack = currentStack.copy();
                    }
                }
            }
            if (numCraftingItems == 1 && !spongeStack.isEmpty())
            {
                final SpongeInventory spongeInventory = ItemSpongeUtils.getCap(spongeStack);
                for (int slot = 0; slot < spongeInventory.getSlots(); slot++)
                {
                    final ItemStack slotStack = spongeInventory.getStackInSlot(slot);
                    if (!slotStack.isEmpty())
                    {
                        event.player.inventory.placeItemBackInInventory(world, slotStack);
                    }
                }
            }
        }
    }

}
