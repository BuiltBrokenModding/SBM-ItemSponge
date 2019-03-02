package com.builtbroken.itemsponge.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

/**
 * @author p455w0rd
 *
 */
public class GuiItemSpongeTile extends GuiContainer {

	public GuiItemSpongeTile(Container container) {
		super(container);
	}

	protected Container getContainer() {
		return inventorySlots;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
	}

}
