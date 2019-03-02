package com.builtbroken.itemsponge.client;

import com.builtbroken.itemsponge.ProxyCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * @author p455w0rd
 */
public class ProxyClient extends ProxyCommon
{
    @Override
    public EntityPlayer getPlayer()
    {
        return Minecraft.getMinecraft().player;
    }

    @Override
    public World getWorld()
    {
        return Minecraft.getMinecraft().world;
    }
}
