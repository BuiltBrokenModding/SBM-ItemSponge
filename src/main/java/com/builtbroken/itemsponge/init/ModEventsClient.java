package com.builtbroken.itemsponge.init;

import com.builtbroken.itemsponge.client.render.ItemLayerWrapper;
import com.builtbroken.itemsponge.client.render.ItemSpongeItemRenderer;
import com.builtbroken.itemsponge.sponge.ItemBlockItemSponge;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-only events handler
 *
 * @author p455w0rd
 */
@SideOnly(Side.CLIENT)
public class ModEventsClient
{

    @SubscribeEvent
    public static void onModelRegistryReady(ModelRegistryEvent event)
    {
        ItemBlockItemSponge itemSpongeItem = (ItemBlockItemSponge) ModItems.ITEM_SPONGE_ITEMBLOCK;
        ModelLoader.setCustomModelResourceLocation(itemSpongeItem, 0, itemSpongeItem.getModelResource(false));
        ModelLoader.setCustomModelResourceLocation(itemSpongeItem, 1, itemSpongeItem.getModelResource(true));
        ModItems.ITEM_SPONGE_ITEMBLOCK.setTileEntityItemStackRenderer(new ItemSpongeItemRenderer());
    }

    @SubscribeEvent
    public static void initModels(ModelBakeEvent event)
    {
        ItemBlockItemSponge itemSpongeItem = (ItemBlockItemSponge) ModItems.ITEM_SPONGE_ITEMBLOCK;
        ModelLoader.setCustomModelResourceLocation(itemSpongeItem, 0, itemSpongeItem.getModelResource(false));
        ModelLoader.setCustomModelResourceLocation(itemSpongeItem, 1, itemSpongeItem.getModelResource(true));

        IBakedModel wtModel0 = event.getModelRegistry().getObject(itemSpongeItem.getModelResource(false));
        itemSpongeItem.setWrappedModel(new ItemLayerWrapper(wtModel0), false);
        if (itemSpongeItem.getTileEntityItemStackRenderer() instanceof ItemSpongeItemRenderer)
        {
            ItemSpongeItemRenderer renderer = (ItemSpongeItemRenderer) itemSpongeItem.getTileEntityItemStackRenderer();
            renderer.setModel(itemSpongeItem.getWrappedModel(false), false);
        }

        IBakedModel wtModel1 = event.getModelRegistry().getObject(itemSpongeItem.getModelResource(true));
        itemSpongeItem.setWrappedModel(new ItemLayerWrapper(wtModel1), true);
        if (itemSpongeItem.getTileEntityItemStackRenderer() instanceof ItemSpongeItemRenderer)
        {
            ItemSpongeItemRenderer renderer = (ItemSpongeItemRenderer) itemSpongeItem.getTileEntityItemStackRenderer();
            renderer.setModel(itemSpongeItem.getWrappedModel(true), true);
        }

        event.getModelRegistry().putObject(itemSpongeItem.getModelResource(false), itemSpongeItem.getWrappedModel(false));
        event.getModelRegistry().putObject(itemSpongeItem.getModelResource(true), itemSpongeItem.getWrappedModel(true));
    }

}
