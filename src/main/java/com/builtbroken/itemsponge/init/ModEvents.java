package com.builtbroken.itemsponge.init;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import com.builtbroken.itemsponge.recipes.RecipeItemSponge;
import com.builtbroken.itemsponge.utils.ItemSpongeUtils;

/**
 * Common events handler
 * @author p455w0rd
 *
 */
@EventBusSubscriber(modid = ModGlobals.MODID)
public class ModEvents {

	@SubscribeEvent
	public static void onItemRegistryReady(RegistryEvent.Register<Item> event) {
		event.getRegistry().register(ModItems.ITEM_SPONGE_ITEMBLOCK);
	}

	@SubscribeEvent
	public static void onBlockRegistryReady(RegistryEvent.Register<Block> event) {
		event.getRegistry().register(ModBlocks.ITEM_SPONGE_BLOCK);
	}

	@SubscribeEvent
	public static void onRecipeRegistryReady(RegistryEvent.Register<IRecipe> event) {
		event.getRegistry().register(RecipeItemSponge.getInstance());
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.player != null) {
			if (!event.player.isSneaking()) {
				ItemSpongeUtils.absorbEntityItems(event.player);
			}
			if (event.player instanceof EntityPlayerMP) {
				ItemSpongeUtils.absorbInventoryItems(event.player);
			}
		}
	}

	@SubscribeEvent
	public static void onWorldServerTick(TickEvent.WorldTickEvent event) {
		if (event.phase == Phase.START) {
			for (Entity entity : event.world.loadedEntityList) {
				if (entity instanceof EntityItem && entity.isAddedToWorld() && ItemSpongeUtils.isSponge(entity)) {
					ItemSpongeUtils.absorbEntityItems((EntityItem) entity);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onItemCrafted(ItemCraftedEvent event) {
		ItemStack result = event.crafting;
		if (!ItemSpongeUtils.hasAbsorbedItem(result)) {
			int numCraftingItems = 0;
			IInventory inv = event.craftMatrix;
			ItemStack spongeStack = ItemStack.EMPTY;
			for (int i = 0; i < inv.getSizeInventory(); i++) {
				ItemStack currentStack = inv.getStackInSlot(i);
				if (!currentStack.isEmpty()) {
					numCraftingItems++;
					if (ItemSpongeUtils.isSponge(currentStack) && ItemSpongeUtils.hasAbsorbedItem(currentStack)) {
						spongeStack = currentStack.copy();
					}
				}
			}
			if (numCraftingItems == 1 && !spongeStack.isEmpty()) {
				for (ItemStack leftOverStack : ItemSpongeUtils.getAbsorbedItems(spongeStack)) {
					World world = event.player.getEntityWorld();
					event.player.inventory.placeItemBackInInventory(world, leftOverStack);
				}
			}
		}
	}

}
