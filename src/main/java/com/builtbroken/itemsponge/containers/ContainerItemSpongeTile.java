package com.builtbroken.itemsponge.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/**
 * @author p455w0rd
 */
public class ContainerItemSpongeTile extends Container
{

    @Override
    public boolean canInteractWith(EntityPlayer playerIn)
    {
        return true;
    }

}
