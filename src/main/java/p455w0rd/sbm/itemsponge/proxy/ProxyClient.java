package p455w0rd.sbm.itemsponge.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.*;
import p455w0rd.sbm.itemsponge.init.ModBlocks;
import p455w0rd.sbm.itemsponge.init.ModEventsClient;

/**
 * @author p455w0rd
 *
 */
public class ProxyClient extends ProxyCommon {

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
		MinecraftForge.EVENT_BUS.register(ModEventsClient.class);
	}

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		ModBlocks.registerTESRs();
	}

	@Override
	public void postInit(FMLPostInitializationEvent event) {
		super.postInit(event);
	}

	@Override
	public EntityPlayer getPlayer() {
		return Minecraft.getMinecraft().player;
	}

	@Override
	public World getWorld() {
		return Minecraft.getMinecraft().world;
	}

}
