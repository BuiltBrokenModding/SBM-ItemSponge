package com.builtbroken.itemsponge.proxy;

import com.builtbroken.itemsponge.init.ModBlocks;
import com.builtbroken.itemsponge.init.ModConfig;
import com.builtbroken.itemsponge.init.ModGuiHandler;
import com.builtbroken.itemsponge.init.ModNetworking;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * @author p455w0rd
 */
public class ProxyCommon
{

    public void preInit(FMLPreInitializationEvent event)
    {
        ModBlocks.registersTiles();
        ModConfig.getInstance().init();
        ModNetworking.init();
    }

    public void init(FMLInitializationEvent event)
    {

    }

    public void postInit(FMLPostInitializationEvent event)
    {
        ModGuiHandler.register();
    }

    public EntityPlayer getPlayer()
    {
        return null;
    }

    public World getWorld()
    {
        return null;
    }

}
