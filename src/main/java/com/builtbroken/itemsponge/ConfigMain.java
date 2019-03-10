package com.builtbroken.itemsponge;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * @author p455w0rd
 */
@Config(modid = ItemSpongeMod.MODID, name = "sbm/itemsponge")
@Config.LangKey("config.itemsponge:main.title")
@Mod.EventBusSubscriber(modid = ItemSpongeMod.MODID)
public class ConfigMain
{
    @Config.Name("pull_distance")
    @Config.Comment("Distance that items can be pulled into the sponge.")
    @Config.LangKey("config.itemsponge:distance")
    @Config.RangeInt(min = 2, max = 100)
    public static int MAX_DISTANCE = 8;

    @Config.Name("slot_limit")
    @Config.Comment("Number of slots to allow for storage. 1 slot = 64 items max or 1 stack if item has a lower limit")
    @Config.LangKey("config.itemsponge:slot")
    @Config.RangeInt(min = 1, max = 1000)
    @Config.RequiresWorldRestart
    public static int SLOT_COUNT = Integer.MAX_VALUE;

    public static boolean REDUCE_SPEED = false;


    protected static String[] itemBlacklistArray = new String[0]; //TODO rewrite
    private static List<ItemStack> itemList = new ArrayList<ItemStack>(); //TODO rewrite

    public static List<ItemStack> getItemBlackList() //TODO rewrite
    {
        if (itemList.isEmpty())
        {
            for (String itemString : itemBlacklistArray)
            {
                String[] components = itemString.split(":");
                int damage = 0;
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(components[0] + ":" + components[1]));
                if (components.length == 3)
                {
                    damage = Integer.parseInt(components[2]);
                }
                if (item != null)
                {
                    itemList.add(new ItemStack(item, 1, damage));
                }
            }
        }
        return itemList;
    }

    @SubscribeEvent
    public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(ItemSpongeMod.MODID))
        {
            ConfigManager.sync(ItemSpongeMod.MODID, Config.Type.INSTANCE);
        }
    }
}
