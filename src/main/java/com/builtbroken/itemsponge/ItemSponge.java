package com.builtbroken.itemsponge;

import com.builtbroken.itemsponge.proxy.ProxyCommon;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = ItemSponge.MODID, name = ItemSponge.NAME, version = ItemSponge.VERSION, acceptedMinecraftVersions = "1.12")
public class ItemSponge
{

    public static final String MODID = "itemsponge";
    public static final String NAME = "Item Sponge";
    public static final String VERSION = "1.0.0";
    public static final String CONFIG_FILE = "config/ItemSponge.cfg";

    @Instance(MODID)
    public static ItemSponge INSTANCE;

    public static final String CLIENT_PROXY = "com.builtbroken.itemsponge.proxy.ProxyClient";
    public static final String SERVER_PROXY = "com.builtbroken.itemsponge.proxy.ProxyCommon";
    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = SERVER_PROXY)
    public static ProxyCommon PROXY;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        PROXY.preInit(e);
    }

    @EventHandler
    public void init(FMLInitializationEvent e)
    {
        PROXY.init(e);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e)
    {
        PROXY.postInit(e);
    }

}
