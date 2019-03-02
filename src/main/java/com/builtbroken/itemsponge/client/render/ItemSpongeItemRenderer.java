package com.builtbroken.itemsponge.client.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import com.builtbroken.itemsponge.client.render.StackRenderUtil.RenderModel;
import com.builtbroken.itemsponge.init.ModItems;
import com.builtbroken.itemsponge.utils.ItemSpongeUtils;

/**
 * @author p455w0rd
 *
 */
public class ItemSpongeItemRenderer extends TileEntityItemStackRenderer {

	public ItemLayerWrapper model0, model1;
	public static TransformType transformType;

	@Override
	public void renderByItem(ItemStack stack, float partialTicks) {
		if (stack == null || stack.isEmpty() || stack.getItem() != ModItems.ITEM_SPONGE_ITEMBLOCK || model0 == null || model1 == null) {
			return;
		}
		IBakedModel model = ItemSpongeUtils.hasAbsorbedItem(stack) ? model1 : model0;
		RenderModel.render(model, stack);
		ItemStack tmpStack = ItemSpongeUtils.getAbsorbedItem(stack);
		if (tmpStack.isEmpty()) {
			return;
		}
		GlStateManager.pushMatrix();
		GlStateManager.rotate(180, 0, 1, 0);
		GlStateManager.translate(-1, 0, -1);
		for (EnumFacing side : EnumFacing.VALUES) {
			StackRenderUtil.renderStackOnSide(tmpStack, side);
		}
		GlStateManager.translate(1, 0, 1);
		GlStateManager.popMatrix();
		GlStateManager.enableBlend();
	}

	public ItemSpongeItemRenderer setModel(ItemLayerWrapper wrappedModel, boolean hasItem) {
		if (!hasItem) {
			model0 = wrappedModel;
		}
		else {
			model1 = wrappedModel;
		}
		return this;
	}

}
