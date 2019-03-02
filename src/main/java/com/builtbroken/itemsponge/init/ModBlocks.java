package com.builtbroken.itemsponge.init;

import com.builtbroken.itemsponge.client.render.ItemSpongeTileRenderer;
import com.builtbroken.itemsponge.sponge.BlockItemSponge;
import com.builtbroken.itemsponge.sponge.TileEntityItemSponge;
import net.minecraft.block.Block;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author p455w0rd
 */
public class ModBlocks
{

    public static final Block ITEM_SPONGE_BLOCK = new BlockItemSponge();

    public static void registersTiles()
    {
        GameRegistry.registerTileEntity(TileEntityItemSponge.class, ITEM_SPONGE_BLOCK.getRegistryName());
    }

    @SideOnly(Side.CLIENT)
    public static void registerTESRs()
    {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityItemSponge.class, new ItemSpongeTileRenderer());
    }

}
