package com.builtbroken.itemsponge.client.render;

import static net.minecraft.util.EnumFacing.*;

import java.util.*;

import javax.annotation.Nonnull;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.pipeline.LightUtil;

/**
 * @author p455w0rd
 *
 */
public class StackRenderUtil {

	private static final Map<EnumFacing, Float> SIDE_ANGLES = new HashMap<EnumFacing, Float>();
	static {
		SIDE_ANGLES.put(UP, 90F);
		SIDE_ANGLES.put(DOWN, -90F);
		SIDE_ANGLES.put(NORTH, 0F);
		SIDE_ANGLES.put(EAST, 90F);
		SIDE_ANGLES.put(SOUTH, 180F);
		SIDE_ANGLES.put(WEST, -90F);
	}

	public static void renderStackOnSide(ItemStack stack, EnumFacing facing) {
		if (facing == UP || facing == DOWN) {
			renderIconVertical(stack, SIDE_ANGLES.get(facing));
			return;
		}
		renderIcon(stack, SIDE_ANGLES.get(facing));
	}

	// for EnumFacing.UP & EnumFacing.DOWN
	private static void renderIconVertical(ItemStack stack, float angle) {
		GlStateManager.pushMatrix();
		GlStateManager.rotate(90, 0, 0, 1);
		GlStateManager.translate(0, -1, 0);
		renderIcon(stack, angle);
		GlStateManager.translate(0, 1, 0);
		GlStateManager.popMatrix();
	}

	private static void renderIcon(ItemStack stack, float angle) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(.5f, .5f, .5f);
		GlStateManager.rotate(180, 0, 0, 1);
		GlStateManager.rotate(angle, 0, 1, 0);
		GlStateManager.translate(-.5f, -.5f, -.5f);
		GlStateManager.translate(0, 0, 0);
		GlStateManager.scale(0.0625F, 0.0625F, -.00001f);
		GlStateManager.translate(4, 4, 0);
		GlStateManager.scale(0.0625F * 8F, 0.0625F * 8F, 7);
		GlStateManager.disableLighting();
		GlStateManager.enablePolygonOffset();
		GlStateManager.doPolygonOffset(-1, -1);
		Minecraft mc = Minecraft.getMinecraft();
		RenderItem renderItem = mc.getRenderItem();
		renderItem.zLevel += 50.0F;
		TextureManager textureManager = mc.getTextureManager();
		IBakedModel model = mc.getRenderItem().getItemModelWithOverrides(stack, mc.world, mc.player);
		GlStateManager.pushMatrix();
		textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.translate(0, 0, 100.0F + renderItem.zLevel);
		GlStateManager.translate(8.0F, 8.0F, 0.0F);
		GlStateManager.scale(1.0F, -1.0F, 1.0F);
		GlStateManager.scale(16.0F, 16.0F, 16.0F);
		if (model.isGui3d()) {
			GlStateManager.enableLighting();
		}
		else {
			GlStateManager.disableLighting();
		}
		model = ForgeHooksClient.handleCameraTransforms(model, ItemCameraTransforms.TransformType.GUI, false);
		renderItem.renderItem(stack, model);
		GlStateManager.disableAlpha();
		GlStateManager.disableRescaleNormal();
		GlStateManager.popMatrix();
		textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
		renderItem.zLevel -= 50.0F;
		GlStateManager.enableAlpha();
		GlStateManager.disableBlend();
		GlStateManager.disablePolygonOffset();
		GlStateManager.popMatrix();
	}

	public static class RenderModel {

		public static void render(IBakedModel model, @Nonnull ItemStack stack) {
			render(model, -1, stack);
		}

		public static void render(IBakedModel model, int color) {
			render(model, color, ItemStack.EMPTY);
		}

		public static void render(IBakedModel model, int color, @Nonnull ItemStack stack) {
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder vertexbuffer = tessellator.getBuffer();
			vertexbuffer.begin(7, DefaultVertexFormats.ITEM);
			for (EnumFacing enumfacing : EnumFacing.values()) {
				renderQuads(vertexbuffer, model.getQuads((IBlockState) null, enumfacing, 0L), color, stack);
			}
			List<BakedQuad> tmpQuads = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(stack).getQuads(null, null, 0);
			renderQuads(vertexbuffer, tmpQuads, color, stack);
			tessellator.draw();
		}

		public static void renderQuads(BufferBuilder renderer, List<BakedQuad> quads, int color, ItemStack stack) {
			boolean flag = (color == -1) && (!stack.isEmpty());
			int i = 0;
			for (int j = quads.size(); i < j; i++) {
				BakedQuad bakedquad = quads.get(i);
				int k = color;
				if ((flag) && (bakedquad.hasTintIndex())) {
					ItemColors itemColors = Minecraft.getMinecraft().getItemColors();
					k = itemColors.colorMultiplier(stack, bakedquad.getTintIndex());
					if (EntityRenderer.anaglyphEnable) {
						k = TextureUtil.anaglyphColor(k);
					}
					k |= 0xFF000000;
				}
				LightUtil.renderQuadColor(renderer, bakedquad, k);
			}
		}
	}

}
