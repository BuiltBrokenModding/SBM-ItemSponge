package com.builtbroken.itemsponge.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.*;
import com.builtbroken.itemsponge.init.*;

/**
 * @author p455w0rd
 *
 */
public class ProxyCommon {

	public void preInit(FMLPreInitializationEvent event) {
		ModBlocks.registersTiles();
		ModConfig.getInstance().init();
		ModNetworking.init();
	}

	public void init(FMLInitializationEvent event) {

	}

	public void postInit(FMLPostInitializationEvent event) {
		ModGuiHandler.register();
	}

	public EntityPlayer getPlayer() {
		return null;
	}

	public World getWorld() {
		return null;
	}

}
