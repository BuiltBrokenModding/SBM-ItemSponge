package com.builtbroken.itemsponge;

import com.builtbroken.itemsponge.sponge.network.PacketStackSync;
import com.builtbroken.itemsponge.sponge.network.PacketTileSync;
import com.builtbroken.itemsponge.sponge.RecipeItemSponge;
import com.builtbroken.itemsponge.sponge.block.BlockItemSponge;
import com.builtbroken.itemsponge.sponge.item.ItemBlockItemSponge;
import com.builtbroken.itemsponge.sponge.block.TileEntityItemSponge;
import com.builtbroken.itemsponge.sponge.ItemSpongeUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.CapabilityItemHandler;

@Mod(modid = ItemSpongeMod.MODID, name = "[SBM] Item Sponge", version = ItemSpongeMod.VERSION)
@Mod.EventBusSubscriber()
public class ItemSpongeMod
{
    public static final String MODID = "itemsponge";

    public static final String MAJOR_VERSION = "@MAJOR@";
    public static final String MINOR_VERSION = "@MINOR@";
    public static final String REVISION_VERSION = "@REVIS@";
    public static final String BUILD_VERSION = "@BUILD@";
    public static final String MC_VERSION = "@MC@";
    public static final String VERSION = MC_VERSION + "-" + MAJOR_VERSION + "." + MINOR_VERSION + "." + REVISION_VERSION + "." + BUILD_VERSION;

    public static final String CONFIG_FILE = "config/ItemSponge.cfg";

    @Instance(MODID)
    public static ItemSpongeMod INSTANCE;

    @SidedProxy(clientSide = "com.builtbroken.itemsponge.client.ProxyClient", serverSide = "com.builtbroken.itemsponge.ProxyCommon")
    public static ProxyCommon PROXY;

    private static final SimpleNetworkWrapper NETWORK = new SimpleNetworkWrapper(MODID);

    public static Block block;
    public static Item item;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        NETWORK.registerMessage(PacketTileSync.Handler.class, PacketTileSync.class, 0, Side.CLIENT);
        NETWORK.registerMessage(PacketStackSync.Handler.class, PacketStackSync.class, 1, Side.CLIENT);
    }

    @SubscribeEvent
    public static void blockRegistry(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().register(block = new BlockItemSponge());
        GameRegistry.registerTileEntity(TileEntityItemSponge.class, block.getRegistryName());
    }

    @SubscribeEvent
    public static void itemRegistry(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().register(item = new ItemBlockItemSponge(block));
    }

    @SubscribeEvent
    public static void recipeRegistry(RegistryEvent.Register<IRecipe> event)
    {
        event.getRegistry().register(RecipeItemSponge.getInstance());
    }

    public static void sendTileSyncPacketToClient(TileEntityItemSponge tile)
    {
        NETWORK.sendToDimension(new PacketTileSync(tile.getPos(), tile.writeToNBT(tile.getUpdateTag())), tile.getWorld().provider.getDimension());
    }

    public static void sendStackSyncPacketToClient(EntityPlayerMP player, int slot, NBTTagCompound nbt)
    {
        ItemStack sponge = player.inventory.getStackInSlot(slot);
        if (ItemSpongeUtils.isSponge(sponge))
        {
            if (sponge.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null))
            {
                NETWORK.sendTo(new PacketStackSync(nbt, slot), player);
            }
        }
    }
}
